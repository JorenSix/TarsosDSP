package be.tarsos.dsp;


import be.tarsos.dsp.util.fft.FFT;

/**
 * This is a translation of code by Stephan M. Bernsee. See the following explanation on this code:
 * <a href="http://www.dspdimension.com/admin/pitch-shifting-using-the-ft/">Pitch shifting using the STFT</a>.
 * 
 * @author Joren Six
 * @author Stephan M. Bernsee
 */
public class PitchShifter implements AudioProcessor{
	
	private final FFT fft;
	private final int size;
	private final float[] currentMagnitudes;
	private final float[] currentPhase;
	private final float[] currentFrequencies;
	private final float[] outputAccumulator;
	private final float[] summedPhase;
	
	private  float[] previousPhase;
	
	private double pitchShiftRatio = 0;

	private final double sampleRate;
	
	private long osamp;
	
	private double excpt;
	
	public PitchShifter(double factor, double sampleRate, int size, int overlap){

		
		pitchShiftRatio = factor;
		this.size = size;
		this.sampleRate = sampleRate;
		//this.d = d;
		
		osamp=size/(size-overlap);
		
		this.excpt = 2.*Math.PI*(double)(size-overlap)/(double)size;
		
		fft = new FFT(size);
		
		currentMagnitudes = new float[size/2];
		currentFrequencies = new float[size/2];
		currentPhase = new float[size/2];
		
		previousPhase = new float[size/2];
		summedPhase = new float[size/2];
		outputAccumulator = new float[size*2];
	}
	
	public void setPitchShiftFactor(float newPitchShiftFactor){
		this.pitchShiftRatio = newPitchShiftFactor;
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		//see http://downloads.dspdimension.com/smbPitchShift.cpp
			
		/* ***************** ANALYSIS ******************* */		
		float[] fftData = audioEvent.getFloatBuffer().clone();
		
		for(int i = 0 ; i<size ; i++){
			float window = (float) (-.5*Math.cos(2.*Math.PI*(double)i/(double)size)+.5);
			fftData[i] = window * fftData[i];
		}
		//Fourier transform the audio 
		fft.forwardTransform(fftData);
		//Calculate the magnitudes and phase information. 
		fft.powerAndPhaseFromFFT(fftData, currentMagnitudes, currentPhase);
		
		float freqPerBin  = (float) (sampleRate/(float)size);	// distance in Hz between FFT bins
		
		for(int i = 0 ; i < size/2 ; i++){
			
			float phase = currentPhase[i];
			
			/* compute phase difference */
			double tmp = phase - previousPhase[i];
			previousPhase[i] = phase;

			/* subtract expected phase difference */
			tmp -= (double)i*excpt;

			/* map delta phase into +/- Pi interval */
			long qpd = (long) (tmp/Math.PI);
			if (qpd >= 0) 
				qpd += qpd&1;
			else 
				qpd -= qpd&1;
			tmp -= Math.PI*(double)qpd;

			/* get deviation from bin frequency from the +/- Pi interval */
			tmp = osamp*tmp/(2.*Math.PI);

			/* compute the k-th partials' true frequency */
			tmp = (double)i*freqPerBin + tmp*freqPerBin;			

			/* store magnitude and true frequency in analysis arrays */
			currentFrequencies[i] = (float) tmp;
		}
		
		/* ***************** PROCESSING ******************* */
		/* this does the actual pitch shifting */
		float[] newMagnitudes = new float[size/2];
		float[] newFrequencies = new float[size/2];
		
		for(int i = 0 ; i < size/2 ; i++){
			int index = (int)(i * pitchShiftRatio);
			if(index < size/2){
				newMagnitudes[index] += currentMagnitudes[i];
				newFrequencies[index] = (float) (currentFrequencies[i]*pitchShiftRatio);
			}
		}
		
		///Synthesis****
		float[] newFFTData = new float[size];
		
		for(int i =0 ; i < size/2 ; i++){
			
			float magn = newMagnitudes[i];
			double tmp = newFrequencies[i];

			/* subtract bin mid frequency */
			tmp -= (double)i*freqPerBin;

			/* get bin deviation from freq deviation */
			tmp /= freqPerBin;

			/* take osamp into account */
			tmp = 2.*Math.PI*tmp/osamp;

			/* add the overlap phase advance back in */
			tmp += (double)i*excpt;

			/* accumulate delta phase to get bin phase */
			summedPhase[i] += tmp;
			float phase = summedPhase[i];

			/* get real and imag part and re-interleave */
			newFFTData[2*i] = (float) (magn * Math.cos(phase));
			newFFTData[2*i+1] = (float) (magn* Math.sin(phase));
		}
		
		/* zero negative frequencies */
		for (int i = size/2+2;  i < size; i++){ 
			newFFTData[i] = 0.f;
		}
		
		fft.backwardsTransform(newFFTData);
		for(int i = 0 ; i < newFFTData.length ; i ++){
			float window = (float) (-.5*Math.cos(2.*Math.PI*(double)i/(double)size)+.5);
			//outputAccumulator[i] += 2000*window*newFFTData[i]/(float) (size*osamp);
			outputAccumulator[i] += window*newFFTData[i]/(float) osamp;
			if(outputAccumulator[i] > 1.0 ||  outputAccumulator[i] < -1.0 ){
				System.err.println("Clipping!");
			}
		}
		
		int stepSize = (int) (size/osamp);
		
		
		
		//Arrays.fill(audioBuffer, 0);
		System.arraycopy(outputAccumulator, stepSize, outputAccumulator, 0, size);
		
		float[] audioBuffer = new float[audioEvent.getFloatBuffer().length];
		audioEvent.setFloatBuffer(audioBuffer);
		System.arraycopy(outputAccumulator, 0, audioBuffer,size-stepSize, stepSize);
		
		return true;
	}

	@Override
	public void processingFinished() {
		
	}
}
