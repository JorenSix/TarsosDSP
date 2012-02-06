package be.hogent.tarsos.dsp.filters;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.BlockingAudioPlayer;
import be.hogent.tarsos.dsp.WaveformWriter;
import be.hogent.tarsos.dsp.util.AudioFloatConverter;

/**
 *
 * <p>
 * An overlap-add technique based on waveform similarity (WSOLA) for high
 * quality time-scale modification of speech
 * </p>
 * <p>
 * A concept of waveform similarity for tackling the problem of time-scale
 * modification of speech is proposed. It is worked out in the context of
 * short-time Fourier transform representations. The resulting WSOLA
 * (waveform-similarity-based synchronized overlap-add) algorithm produces
 * high-quality speech output, is algorithmically and computationally efficient
 * and robust, and allows for online processing with arbitrary time-scaling
 * factors that may be specified in a time-varying fashion and can be chosen
 * over a wide continuous range of values.
 * </p>
 * <p>
 * Inspired by the work soundtouch by Olli Parviainen,
 * http://www.surina.net/soundtouch, especially the TDStrech.cpp file.
 * </p>
 * @author Joren Six
 * @author Olli Parviainen
 */
public class WaveformSimilarityBasedOverlapAdd implements AudioProcessor {
	
	private int seekWindowLength;
	private int seekLength;
	private int overlapLength;
	
	private float[] pMidBuffer;	
	private float[] pRefMidBuffer;
	private float[] outputFloatBuffer;
	private byte[] outputByteBuffer;
	
	private int intskip;
	private int sampleReq; 
	
	private double tempo;
	private AudioFormat format;
	private BlockingAudioPlayer outputProcessor;
	private AudioFloatConverter converter;
	public AudioDispatcher dispatcher;
	
	private Parameters newParameters;
	
	public WaveformSimilarityBasedOverlapAdd(AudioFormat format,Parameters  params){
		this.format = format;
				
		converter = AudioFloatConverter.getConverter(format);
		
			try {
				outputProcessor = new BlockingAudioPlayer(format, getOutputBufferSize(),0);
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			setParameters(params);
		applyNewParameters();

	}
	
	public void setParameters(Parameters params){
		newParameters = params;
	}
	
	private void applyNewParameters(){
		Parameters params = newParameters;
		int oldOverlapLength = overlapLength;
		overlapLength = (int) ((params.getSampleRate() * params.getOverlapMs())/1000);
		seekWindowLength = (int) ((params.getSampleRate() * params.getSequenceMs())/1000);
		seekLength = (int) ((params.getSampleRate() *  params.getSeekWindowMs())/1000);
		
		tempo = params.getTempo();
		
		if(overlapLength > oldOverlapLength * 8){
			pMidBuffer = new float[overlapLength * 8]; //overlapLengthx2?
			pRefMidBuffer = new float[overlapLength * 8];//overlapLengthx2?
		}
		
		double nominalSkip = tempo * (seekWindowLength - overlapLength);
		intskip = (int) (nominalSkip + 0.5);
		
		sampleReq = Math.max(intskip + overlapLength, seekWindowLength) + seekLength;
		
		outputFloatBuffer= new float[getOutputBufferSize()];
		outputByteBuffer = new byte [getOutputBufferSize() * format.getFrameSize()];
		newParameters = null;
		
		outputProcessor.setStepSizeAndOverlap(getOutputBufferSize(), 0);
	}
	
	public int getInputBufferSize(){
		return sampleReq;
	}
	
	public int getOverlap(){
		return sampleReq-intskip;
	}
	
	public int getOutputBufferSize(){
		return seekWindowLength - overlapLength;
	}
	
	
	/**
	 * Overlaps the sample in output with the samples in input.
	 * @param output The output buffer.
	 * @param input The input buffer.
	 */
	private void overlap(final float[] output, int outputOffset, float[] input,int inputOffset){
		for(int i = 0 ; i < overlapLength ; i++){
			int itemp = overlapLength - i;
			output[i + outputOffset] = (input[i + inputOffset] * i + pMidBuffer[i] * itemp ) / overlapLength;  
		}
	}
	
	
	/**
	 * Seeks for the optimal overlap-mixing position.
	 * 
	 * The best position is determined as the position where the two overlapped
	 * sample sequences are 'most alike', in terms of the highest
	 * cross-correlation value over the overlapping period
	 * 
	 * @param inputBuffer The input buffer
	 * @param postion The position where to start the seek operation, in the input buffer. 
	 * @return The best position.
	 */
	private int seekBestOverlapPosition(float[] inputBuffer, int postion) {
		int bestOffset;
		double bestCorrelation, currentCorrelation;
		int tempOffset;

		int comparePosition;

		// Slopes the amplitude of the 'midBuffer' samples
		precalcCorrReferenceMono();

		bestCorrelation = -10;
		bestOffset = 0;

		// Scans for the best correlation value by testing each possible
		// position
		// over the permitted range.
		for (tempOffset = 0; tempOffset < seekLength; tempOffset++) {

			comparePosition = postion + tempOffset;

			// Calculates correlation value for the mixing position
			// corresponding
			// to 'tempOffset'
			currentCorrelation = (double) calcCrossCorr(pRefMidBuffer, inputBuffer,comparePosition);
			// heuristic rule to slightly favor values close to mid of the
			// range
			double tmp = (double) (2 * tempOffset - seekLength) / seekLength;
			currentCorrelation = ((currentCorrelation + 0.1) * (1.0 - 0.25 * tmp * tmp));

			// Checks for the highest correlation value
			if (currentCorrelation > bestCorrelation) {
				bestCorrelation = currentCorrelation;
				bestOffset = tempOffset;
			}
		}

		return bestOffset;

	}
	
	/**
	* Slopes the amplitude of the 'midBuffer' samples so that cross correlation
	* is faster to calculate. Why is this faster?
	*/
	void precalcCorrReferenceMono()
	{
	    for (int i = 0; i < overlapLength; i++){
	    	float temp = i * (overlapLength - i);
	        pRefMidBuffer[i] = pMidBuffer[i] * temp;
	    }
	}	

	
	double calcCrossCorr(float[] mixingPos, float[] compare, int offset){
		double corr = 0;
	    double norm = 0;
	    for (int i = 1; i < overlapLength; i ++){
	        corr += mixingPos[i] * compare[i + offset];
	        norm += mixingPos[i] * mixingPos[i];
	    }
	    // To avoid division by zero.
	    if (norm < 1e-8){
	    	norm = 1.0;    
	    }
	    return corr / Math.pow(norm,0.5);
	}
	

	@Override
	public boolean processFull(float[] audioFloatBuffer, byte[] audioByteBuffer) {
		//Search for the best overlapping position.
		int offset =  seekBestOverlapPosition(audioFloatBuffer,0);
		
		// Mix the samples in the 'inputBuffer' at position of 'offset' with the 
        // samples in 'midBuffer' using sliding overlapping
        // ... first partially overlap with the end of the previous sequence
        // (that's in 'midBuffer')
		overlap(outputFloatBuffer,0,audioFloatBuffer,offset);
			
		//copy sequence samples from input to output			
		int sequenceLength = seekWindowLength - 2 * overlapLength;
		System.arraycopy(audioFloatBuffer, offset + overlapLength, outputFloatBuffer, overlapLength, sequenceLength);
		
	     // Copies the end of the current sequence from 'inputBuffer' to 
        // 'midBuffer' for being mixed with the beginning of the next 
        // processing sequence and so on
		System.arraycopy(audioFloatBuffer, offset + sequenceLength + overlapLength, pMidBuffer, 0, overlapLength);
		
		converter.toByteArray(outputFloatBuffer, outputByteBuffer);
		outputProcessor.processFull(outputFloatBuffer, outputByteBuffer);
		
		if(newParameters!=null){
			applyNewParameters();
			dispatcher.setStepSizeAndOverlap(getInputBufferSize(),getOverlap());
		}
		return true;
	}

	@Override
	public boolean processOverlapping(float[] audioFloatBuffer,
			byte[] audioByteBuffer) {
		return processFull(audioFloatBuffer, audioByteBuffer);
	}


	@Override
	public void processingFinished() {
		outputProcessor.processingFinished();
	}
	
	/**
	 * An object to encapsulate some of the parameters for
	 *         WSOLA, together with a couple of practical helper functions.
	 * 
	 * @author Joren Six
	 */
	public static class Parameters {
		private final int sequenceMs;
		private final int seekWindowMs;
		private final int overlapMs;
		
		private final double tempo;
		private final double sampleRate;
		
		/**
		 * @param newSequenceMs
		 *            Length of a single processing sequence, in milliseconds.
		 *            This determines to how long sequences the original sound
		 *            is chopped in the time-stretch algorithm.
		 * 
		 *            The larger this value is, the lesser sequences are used in
		 *            processing. In principle a bigger value sounds better when
		 *            slowing down tempo, but worse when increasing tempo and
		 *            vice versa.
		 * 
		 *            Increasing this value reduces computational burden & vice
		 *            versa.
		 * @param newSeekWindowMs
		 *            Seeking window length in milliseconds for algorithm that
		 *            finds the best possible overlapping location. This
		 *            determines from how wide window the algorithm may look for
		 *            an optimal joining location when mixing the sound
		 *            sequences back together.
		 * 
		 *            The bigger this window setting is, the higher the
		 *            possibility to find a better mixing position will become,
		 *            but at the same time large values may cause a "drifting"
		 *            artifact because consequent sequences will be taken at
		 *            more uneven intervals.
		 * 
		 *            If there's a disturbing artifact that sounds as if a
		 *            constant frequency was drifting around, try reducing this
		 *            setting.
		 * 
		 *            Increasing this value increases computational burden &
		 *            vice versa.
		 * @param newOverlapMs
		 *            Overlap length in milliseconds. When the chopped sound
		 *            sequences are mixed back together, to form a continuous
		 *            sound stream, this parameter defines over how long period
		 *            the two consecutive sequences are let to overlap each
		 *            other.
		 * 
		 *            This shouldn't be that critical parameter. If you reduce
		 *            the DEFAULT_SEQUENCE_MS setting by a large amount, you
		 *            might wish to try a smaller value on this.
		 * 
		 *            Increasing this value increases computational burden &
		 *            vice versa.
		 */
		public Parameters(double tempo, double sampleRate, int newSequenceMs, int newSeekWindowMs, int newOverlapMs) {
			this.tempo = tempo;
			this.sampleRate = sampleRate;
			this.overlapMs = newOverlapMs;
			this.seekWindowMs = newSeekWindowMs;
			this.sequenceMs = newSequenceMs;
		}
		
		public static Parameters speechDefaults(double tempo, double sampleRate){
			int sequenceMs = 40;
			int seekWindowMs = 15;
			int overlapMs = 12;
			return new Parameters(tempo,sampleRate,sequenceMs, seekWindowMs,overlapMs);
		}
		
		public static Parameters musicDefaults(double tempo, double sampleRate){
			int sequenceMs = 82;
			int seekWindowMs =  28;
			int overlapMs = 12;
			return new Parameters(tempo,sampleRate,sequenceMs, seekWindowMs,overlapMs);
		}
		
		public static Parameters slowdownDefaults(double tempo, double sampleRate){
			int sequenceMs = 90;
			int seekWindowMs =  30;
			int overlapMs = 18;
			return new Parameters(tempo,sampleRate,sequenceMs, seekWindowMs,overlapMs);
		}
		
		public static Parameters automaticDefaults(double tempo, double sampleRate){
			double tempoLow = 0.5; // -50% speed
			double tempoHigh = 2.0; // +100% speed
			
			double sequenceMsLow = 125; //ms
			double sequenceMsHigh = 50; //ms
			double sequenceK = ((sequenceMsHigh - sequenceMsLow) / (tempoHigh - tempoLow));
			double sequenceC = sequenceMsLow - sequenceK * tempoLow;
			
			double seekLow = 25;// ms
			double seekHigh = 15;// ms
			double seekK =((seekHigh - seekLow) / (tempoHigh-tempoLow));
			double seekC = seekLow - seekK * seekLow;
			
			int sequenceMs = (int) (sequenceC + sequenceK * tempo + 0.5);
			int seekWindowMs =  (int) (seekC + seekK * tempo + 0.5);
			int overlapMs = 12;
			return new Parameters(tempo,sampleRate,sequenceMs, seekWindowMs,overlapMs);
		}

		public double getOverlapMs() {
			return overlapMs;
		}

		public double getSequenceMs() {
			return sequenceMs;
		}

		public double getSeekWindowMs() {
			return seekWindowMs;
		}
		
		public double getSampleRate() {
			return sampleRate;
		}
		
		public double getTempo(){
			return tempo;
		}
	}
	
	
}
