package be.tarsos.dsp.pitch;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.pitch.Goertzel.FrequenciesDetectedHandler;
import be.tarsos.dsp.util.Complex;
import be.tarsos.dsp.util.fft.HammingWindow;
import be.tarsos.dsp.util.fft.WindowFunction;

/**
 *  See "Goertzel algorithm generalized to non-integer multiples of fundamental frequency" by Petr Sysel and Pavel Rajmic
 * 
 * 
 * @author Joren Six
 *
 */
public class GeneralizedGoertzel implements AudioProcessor{
	
	/**
	 * A list of frequencies to detect.
	 */
	private final double[] frequenciesToDetect;
	
	private final double[] indvec;
	
	/**
	 * Cached cosine calculations for each frequency to detect.
	 */
	private final double[] precalculatedCosines;
	/**
	 * Cached wnk calculations for each frequency to detect.
	 */
	private final double[] precalculatedWnk;
	/**
	 * A calculated power for each frequency to detect. This array is reused for
	 * performance reasons.
	 */
	private final double[] calculatedPowers;
	private final Complex[] calculatedComplex;

	private final FrequenciesDetectedHandler handler;


	/**
	 * Create a new Generalized Goertzel processor.
	 * @param audioSampleRate The sample rate of the audio in Hz.
	 * @param bufferSize the size of the buffer.
	 * @param frequencies The list of frequencies to detect (in Hz).
	 * @param handler The handler used to handle the detected frequencies.
	 */
	public GeneralizedGoertzel(final float audioSampleRate, final int bufferSize,
			double[] frequencies, FrequenciesDetectedHandler handler){
		frequenciesToDetect = frequencies;
		
		indvec = new double[frequenciesToDetect.length];
		for (int j = 0; j < frequenciesToDetect.length; j++) {
			indvec[j] = frequenciesToDetect[j]/(audioSampleRate/(float)bufferSize);
		}
		
		
		precalculatedCosines = new double[frequencies.length];
		precalculatedWnk = new double[frequencies.length];
		this.handler = handler;

		calculatedPowers = new double[frequencies.length];
		calculatedComplex = new Complex[frequencies.length];

		for (int i = 0; i < frequenciesToDetect.length; i++) {
			precalculatedCosines[i] = 2 * Math.cos(2 * Math.PI
					* frequenciesToDetect[i] / audioSampleRate);
			precalculatedWnk[i] = Math.exp(-2 * Math.PI
					* frequenciesToDetect[i] / audioSampleRate);
		}
		
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		
		float[] x = audioEvent.getFloatBuffer();
		WindowFunction f  = new HammingWindow();
		f.apply(x);
		for (int j = 0; j < frequenciesToDetect.length; j++) {
			double pik_term = 2 * Math.PI * indvec[j]/(float) audioEvent.getBufferSize(); 
			double cos_pik_term2 = Math.cos(pik_term) * 2;
			Complex cc = new Complex(0,-1*pik_term).exp();
			double s0=0;
			double s1=0;
			double s2=0;
			
			for(int i = 0 ; i < audioEvent.getBufferSize() ; i++ ){
				s0 = x[i]+cos_pik_term2*s1-s2;
				s2=s1;
				s1=s0;
			}
			s0 = cos_pik_term2 * s1 - s2;
			calculatedComplex[j] = cc.times(new Complex(-s1,0)).plus(new Complex(s0,0));
			calculatedPowers[j] = calculatedComplex[j].mod();
		}
		
		handler.handleDetectedFrequencies(audioEvent.getTimeStamp(),frequenciesToDetect.clone(), calculatedPowers.clone(),
				frequenciesToDetect.clone(), calculatedPowers.clone());
		
		return true;
	}


	@Override
	public void processingFinished() {
		
	}

}
