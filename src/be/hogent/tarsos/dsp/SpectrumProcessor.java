package be.hogent.tarsos.dsp;

import be.hogent.tarsos.dsp.util.fft.FFT;
import be.hogent.tarsos.dsp.util.fft.HammingWindow;

/**
 * <p>
 * This class extract spectral information and calculates the presice locations for frequency bins.
 * </p>
 * <p>
 * 
 * @author Joren Six
 * 
 */
public class SpectrumProcessor implements AudioProcessor {	

	private final int sampleRate;

	/**
	 * The fft object used to calculate phase and magnitudes.
	 */
	private final FFT fft;

	/**
	 * The pahse info of the current frame.
	 */
	private final float[] phase;
	
	/**
	 * The phase information of the previous frame, or null.
	 */
	private float[] previousPhase;
	

	/**
	 * The magnitudes in the current frame.
	 */
	private final float[] magnitude;
	
	/**
	 * The calculated frequencies for each bin, calculated using the phase.
	 */
	private final float[] frequencies;
	
	/**
	 * The time difference between two frames in seconds
	 */
	private final double dt;
	private final double cbin;
	private final double inv_2pi;
	private final double inv_deltat;
	private final double inv_2pideltat;

	public SpectrumProcessor(int bufferSize, int overlap, int sampleRate) {
		fft = new FFT(bufferSize, new HammingWindow());

		magnitude = new float[bufferSize / 2];
		phase = new float[bufferSize / 2];
		frequencies = new float[bufferSize / 2];
		
		dt = (bufferSize - overlap) / (double) sampleRate;
		cbin = (double) (dt * sampleRate / (double) bufferSize);
		inv_2pi = (double) (1.0 / (2.0 * Math.PI));
		inv_deltat = (double) (1.0 / dt);
		inv_2pideltat = (double) (inv_deltat * inv_2pi);
		this.sampleRate = sampleRate;
	}

	private void calculateFFT(float[] audio) {
		// Clone to prevent overwriting audio data
		float[] fftData = audio.clone();
		// Extract the power and phase data
		fft.powerPhaseFFT(fftData, magnitude, phase);
	}
	
	
	
	public boolean process(float[] audio){
		// extract magnitudes, and phase
		calculateFFT(audio);

		//calculate precise frequency component
		for (int i = 0; i < frequencies.length; i++) {
			frequencies[i] = getFrequencyForBin(i);
		}
		
		//store the current phase info
		previousPhase = phase.clone();
				
		return true;
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audio = audioEvent.getFloatBuffer();
		process(audio);
		return true;
	}
	
	private float getFrequencyForBin(int binIndex){
		final float frequencyInHertz;
		// use the phase delta information to get a more precise
		// frequency estimate
		// if the phase of the previous frame is available.
		// See
		// * Moore 1976
		// "The use of phase vocoder in computer music applications"
		// * Sethares et al. 2009 - Spectral Tools for Dynamic
		// Tonality and Audio Morphing
		// * Laroche and Dolson 1999
		if (previousPhase == null) {
			frequencyInHertz = (float) fft.binToHz(binIndex, sampleRate);
		} else {
			float phaseDelta = phase[binIndex] - previousPhase[binIndex];
			long k = Math.round(cbin * binIndex - inv_2pi * phaseDelta);
			frequencyInHertz = (float) (inv_2pideltat * phaseDelta  + inv_deltat * k);
		}
		return frequencyInHertz;
	}

	@Override
	public void processingFinished() {

	}

	public float[] getMagnitudes() {
		return magnitude;
	}
	
	public float[] getFrequencies(){
		return frequencies;
	}
}
