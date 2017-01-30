package be.tarsos.dsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;

/**
 * <p>
 * This class implements a spectral peak follower as described in Sethares et
 * al. 2009 - Spectral Tools for Dynamic Tonality and Audio Morphing - section
 * "Analysis-Resynthessis". It calculates a noise floor and picks spectral peaks
 * rising above a calculated noise floor with a certain factor. The noise floor
 * is determined using a simple median filter.
 * </p>
 * <p>
 * Parts of the code is modified from <a
 * href="http://www.dynamictonality.com/spectools.htm">the code accompanying
 * "Spectral Tools for Dynamic Tonality and Audio Morphing"</a>.
 * </p>
 * <p>
 * To get the spectral peaks from an audio frame, call <code>getPeakList</code>
 * <code><pre> 
AudioDispatcher dispatcher = new AudioDispatcher(stream, fftsize, overlap);
dispatcher.addAudioProcessor(spectralPeakFollower);
dispatcher.addAudioProcessor(new AudioProcessor() {

  public void processingFinished() {
  }
  
  public boolean process(AudioEvent audioEvent) {
    spectralPeakFollower.getPeakList();
    // do something with the list...
    return true;
  }
});
dispatcher.run();
</pre></code>
 * 
 * @author Joren Six
 * @author William A. Sethares
 * @author Andrew J. Milne
 * @author Stefan Tiedje
 * @author Anthony Prechtl
 * @author James Plamondon
 * 
 */
public class SpectralPeakFollower implements AudioProcessor {

	/**
	 * The length of the median filter used to determine the noise floor.
	 */
	private final int medianFilterLength;
	/**
	 * The noise floor is multiplied with this factor to determine if the
	 * information is either noise or an interesting spectral peak.
	 */
	private final float noiseFloorFactor;
	
	private float maxMagnitude=0.0000001f;
	/**
	 * The amount of spectral peaks returned, can be less.
	 */
	private final int spectralPeaks;

	private final int sampleRate;

	/**
	 * The time difference between two frames in seconds
	 */
	private final double dt;
	private final double cbin;
	private final double inv_2pi;
	private final double inv_deltat;
	private final double inv_2pideltat;
	
	private final List<SpectralPeak> spectralPeakList;

	/**
	 * The fft object used to calculate phase and magnitudes.
	 */
	private final FFT fft;

	/**
	 * The pahse info of the current frame.
	 */
	private final float[] phase;

	/**
	 * The magnitudes in the current frame.
	 */
	private final float[] magnitude;
	
	/**
	 * The calculated noise floor for the current frame.
	 */
	private final float[] noisefloor;

	/**
	 * The phase information of the previous frame, or null.
	 */
	private float[] previousPhase;

	public SpectralPeakFollower(int bufferSize, int overlap, int sampleRate) {
		this(bufferSize, overlap, sampleRate, 35, 7,2.5f);
	}

	public SpectralPeakFollower(int bufferSize, int overlap, int sampleRate,
			int medianFilterLength, int spectralPeaks, float noiseFloorFactor) {
		fft = new FFT(bufferSize, new HammingWindow());

		magnitude = new float[bufferSize / 2];
		phase = new float[bufferSize / 2];

		noisefloor = new float[bufferSize / 2];

		dt = (bufferSize - overlap) / (double) sampleRate;
		cbin = (double) (dt * sampleRate / (double) bufferSize);

		inv_2pi = (double) (1.0 / (2.0 * Math.PI));
		inv_deltat = (double) (1.0 / dt);
		inv_2pideltat = (double) (inv_deltat * inv_2pi);

		this.medianFilterLength = medianFilterLength;
		this.spectralPeaks = spectralPeaks;
		this.noiseFloorFactor = noiseFloorFactor;
		this.sampleRate = sampleRate;
		
		spectralPeakList = new ArrayList<SpectralPeakFollower.SpectralPeak>();
	}

	private void calculateFFT(float[] audio) {
		// Clone to prevent overwriting audio data
		float[] fftData = audio.clone();
		// Extract the power and phase data
		fft.powerPhaseFFT(fftData, magnitude, phase);
		
		for(int i = 0;i<magnitude.length;i++){
			maxMagnitude = Math.max(maxMagnitude, magnitude[i]);
		}
		
		//log10 of the normalized value
		for(int i = 1;i<magnitude.length;i++){
			magnitude[i] = (float) (10 * Math.log10(magnitude[i]/maxMagnitude));
		}
	}
	
	public static class SpectralPeak{
		private final float frequencyInHertz;
		private final float magnitude;
		private final float referenceFrequency;
		private final int bin;
		/**
		 * Timestamp in fractional seconds
		 */
		private final float timeStamp;
		
		public SpectralPeak(float timeStamp,float frequencyInHertz, float magnitude,float referenceFrequency,int bin){
			this.frequencyInHertz = frequencyInHertz;
			this.magnitude = magnitude;
			this.referenceFrequency = referenceFrequency;
			this.timeStamp = timeStamp;
			this.bin = bin;
		}
		
		public float getRelativeFrequencyInCents(){
			float refInCents = (float) PitchConverter.hertzToAbsoluteCent(referenceFrequency);
			float valueInCents = (float) PitchConverter.hertzToAbsoluteCent(frequencyInHertz);
			return valueInCents - refInCents;
		}
		
		public float getTimeStamp(){
			return timeStamp;
		}
		
		public float getMagnitude(){
			return magnitude;
		}
		
		public float getFrequencyInHertz(){
			return frequencyInHertz;
		}
		
		public String toString(){
			return String.format("%.2f %.2f %.2f", frequencyInHertz,getRelativeFrequencyInCents(),magnitude);
		}

		public int getBin() {
			return bin;
		}
	}

	private void calculateNoiseFloor() {
		double[] noiseFloorBuffer;
		

		for (int i = 0; i < magnitude.length; i++) {
			noiseFloorBuffer = new double[medianFilterLength];
			
			int index = 0;
			for (int j = i - medianFilterLength/2; j <= i + medianFilterLength/2 && index < noiseFloorBuffer.length; j++) {
			  if(j >= 0 && j < magnitude.length){
				noiseFloorBuffer[index] = magnitude[j];
			  }  
			  index++;
			}
	
			// calculate the noise floor value.
			noisefloor[i] = (float) (median(noiseFloorBuffer) * (1.0/noiseFloorFactor)) ;
		}
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audio = audioEvent.getFloatBuffer();

		spectralPeakList.clear();

		// extract magnitudes, and phase
		calculateFFT(audio);

		int framesize = magnitude.length;

		// determine the noise floor
		calculateNoiseFloor();

		int nump = 0;// number of currently selected peaks
		float[] peakMagnitudes = new float[framesize];
		float[] magnitudesRatioToNoiseFloor = new float[framesize];
		float maxMagnitude = 0;
		int maxMagnitudeIndex = 0;
		float referenceFrequency=0;
		for (int i = 1; i < framesize - 1; i++) {
			boolean largerThanPrevious = (magnitude[i - 1] < magnitude[i]);
			boolean largerThanNext = (magnitude[i] > magnitude[i + 1]);
			boolean largerThanNoiseFloor = (magnitude[i] >  noisefloor[i]);
			if (largerThanPrevious && largerThanNext && largerThanNoiseFloor) {
				peakMagnitudes[i] = magnitude[i];
				magnitudesRatioToNoiseFloor[i] = 1 - (1000.0f + noisefloor[i])/(1000.0f + magnitude[i]);
				nump = nump + 1;
			}
			if(Math.abs(magnitude[i]) > Math.abs(maxMagnitude)){
				maxMagnitude = magnitude[i];
				maxMagnitudeIndex = i;
			}
		}

		// If there are peaks and the audio buffer does not contain silence
		if (nump > 0 && !audioEvent.isSilence(-75)) {
			//the frequency of the bin with the highest magnitude
			referenceFrequency =  getFrequencyForBin(maxMagnitudeIndex);
			
			//float[] magintudesClone = peakMagnitudes.clone();
			//Arrays.sort(magintudesClone);
			//float peakthresh = magintudesClone[magintudesClone.length - nump];
			//if (nump > spectralPeaks) {
			//	peakthresh = magintudesClone[framesize - spectralPeaks];
			//}
			
			float[] magnitudesRatioToNoiseFloorClone = magnitudesRatioToNoiseFloor.clone();
			Arrays.sort(magnitudesRatioToNoiseFloorClone);
			float ratioThresh = magnitudesRatioToNoiseFloorClone[magnitudesRatioToNoiseFloor.length - nump];
			if (nump > spectralPeaks) {
				ratioThresh = magnitudesRatioToNoiseFloorClone[framesize - spectralPeaks];
			}

			for (int i = 1; i < framesize - 1; i++) {
				// for each local peak where the threshold is larger than the
				// defined one
				//if ((peakMagnitudes[i] != 0) & (magnitude[i] >= peakthresh)) {
				if ((peakMagnitudes[i] != 0) & (magnitudesRatioToNoiseFloor[i] >= ratioThresh)) {
					final float frequencyInHertz= getFrequencyForBin(i);
					//ignore frequencies lower than 30Hz
					if(frequencyInHertz > 30 && referenceFrequency > 30){
						float binMagnitude = magnitude[i]/maxMagnitude;
						SpectralPeak peak = new SpectralPeak((float)audioEvent.getTimeStamp(),frequencyInHertz, binMagnitude, referenceFrequency,i);
						spectralPeakList.add(peak);
					}
				}
			}
		}
		previousPhase = phase.clone();

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
		if (previousPhase != null) {
			float phaseDelta = phase[binIndex] - previousPhase[binIndex];
			long k = Math.round(cbin * binIndex - inv_2pi * phaseDelta);
			frequencyInHertz = (float) (inv_2pideltat * phaseDelta  + inv_deltat * k);
		} else {
			frequencyInHertz = (float) fft.binToHz(binIndex, sampleRate);
		}
		return frequencyInHertz;
	}

	@Override
	public void processingFinished() {

	}
	
	public List<SpectralPeak> getPeakList(){
		return new ArrayList<SpectralPeak>(spectralPeakList);
	}
	
	
	public static final float median(double[] arr){
		return percentile(arr, 0.5);
	}
	
	/**
	*  Returns the p-th percentile of values in an array. You can use this
	*  function to establish a threshold of acceptance. For example, you can
	*  decide to examine candidates who score above the 90th percentile (0.9).
	*  The elements of the input array are modified (sorted) by this method.
	*
	*  @param   arr  An array of sample data values that define relative standing.
	*                The contents of the input array are sorted by this method.
	*  @param   p    The percentile value in the range 0..1, inclusive.
	*  @return  The p-th percentile of values in an array.  If p is not a multiple
	*           of 1/(n - 1), this method interpolates to determine the value at
	*           the p-th percentile.
	**/
	public static final float percentile( double[] arr, double p ) {
		
		if (p < 0 || p > 1)
			throw new IllegalArgumentException("Percentile out of range.");
		
		//	Sort the array in ascending order.
		Arrays.sort(arr);
		
		//	Calculate the percentile.
		double t = p*(arr.length - 1);
		int i = (int)t;
		
		return (float) ((i + 1 - t)*arr[i] + (t - i)*arr[i + 1]);
	}

	public float[] getSpectrum() {
		return magnitude;
	}
	
	public float[] getNoiseFloor() {
		return noisefloor;
	}

}
