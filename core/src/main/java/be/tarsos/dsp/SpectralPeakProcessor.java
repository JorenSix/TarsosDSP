/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -------------------------------------------------------------
*
* TarsosDSP is developed by Joren Six at IPEM, University Ghent
*  
* -------------------------------------------------------------
*
*  Info: http://0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://0110.be/releases/TarsosDSP/
*  
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
* 
*/

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
 * <code>
AudioDispatcher dispatcher = new AudioDispatcher(stream, fftsize, overlap);
dispatcher.addAudioProcessor(spectralPeakFollower);
dispatcher.addAudioProcessor(new AudioProcessor() {

  public void processingFinished() {
  }
  
  public boolean process(AudioEvent audioEvent) {
    float[] noiseFloor = SpectralPeakProcessor.calculateNoiseFloor(spectralPeakFollower.getMagnitudes(), medianFilterLength, noiseFloorFactor);
	List localMaxima = SpectralPeakProcessor.findLocalMaxima(spectralPeakFollower.getMagnitudes(), noiseFloor);
	List list = SpectralPeakProcessor.findPeaks(spectralPeakFollower.getMagnitudes(), spectralPeakFollower.getFrequencyEstimates(), localMaxima, numberOfPeaks);
    // do something with the list...
    return true;
  }
});
dispatcher.run();
</code>
 * 
 * @author Joren Six
 * @author William A. Sethares
 * @author Andrew J. Milne
 * @author Stefan Tiedje
 * @author Anthony Prechtl
 * @author James Plamondon
 * 
 */
public class SpectralPeakProcessor implements AudioProcessor {

	/**
	 * The sample rate of the signal.
	 */
	private final int sampleRate;
	
	/**
	 * Cached calculations for the frequency calculation
	 */
	private final double dt;
	private final double cbin;
	private final double inv_2pi;
	private final double inv_deltat;
	private final double inv_2pideltat;

	/**
	 * The fft object used to calculate phase and magnitudes.
	 */
	private final FFT fft;

	/**
	 * The pahse info of the current frame.
	 */
	private final float[] currentPhaseOffsets;

	/**
	 * The magnitudes in the current frame.
	 */
	private final float[] magnitudes;
	
	/**
	 * Detailed frequency estimates for each bin, using phase info
	 */
	private final float[] frequencyEstimates;
	
	/**
	 * The phase information of the previous frame, or null.
	 */
	private float[] previousPhaseOffsets;

	

	public SpectralPeakProcessor(int bufferSize, int overlap, int sampleRate) {
		fft = new FFT(bufferSize, new HammingWindow());

		magnitudes = new float[bufferSize / 2];
		currentPhaseOffsets = new float[bufferSize / 2];
		frequencyEstimates = new float[bufferSize / 2];

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
		fft.powerPhaseFFT(fftData, magnitudes, currentPhaseOffsets);
	}
	
	private void normalizeMagintudes(){
		float maxMagnitude = (float) -1e6;
		for(int i = 0;i<magnitudes.length;i++){
			maxMagnitude = Math.max(maxMagnitude, magnitudes[i]);
		}
		
		//log10 of the normalized value
		//adding 75 makes sure the value is above zero, a bit ugly though...
		for(int i = 1;i<magnitudes.length;i++){
			magnitudes[i] = (float) (10 * Math.log10(magnitudes[i]/maxMagnitude)) + 75;
		}
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audio = audioEvent.getFloatBuffer();
	
		// 1. Extract magnitudes, and phase using an FFT.
		calculateFFT(audio);
		
		// 2. Estimate a detailed frequency for each bin.
		calculateFrequencyEstimates();
		
		// 3. Normalize the each magnitude.
		normalizeMagintudes();
		
		// 4. Store the current phase so it can be used for the next frequency estimates block. 
		previousPhaseOffsets = currentPhaseOffsets.clone();
	
		return true;		
	}
	
	@Override
	public void processingFinished() {
	}
	
	
	/**
	 * For each bin, calculate a precise frequency estimate using phase offset.
	 */
	private void calculateFrequencyEstimates() {
		for(int i = 0;i < frequencyEstimates.length;i++){
			frequencyEstimates[i] = getFrequencyForBin(i);
		}
	}
	
	/**
	 * @return the magnitudes.
	 */
	public float[] getMagnitudes() {
		return magnitudes.clone();
	}
	
	/**
	 * @return the precise frequency for each bin.
	 */
	public float[] getFrequencyEstimates(){
		return frequencyEstimates.clone();
	}
	
	/**
	 * Calculates a frequency for a bin using phase info, if available.
	 * @param binIndex The FFT bin index.
	 * @return a frequency, in Hz, calculated using available phase info.
	 */
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
		if (previousPhaseOffsets != null) {
			float phaseDelta = currentPhaseOffsets[binIndex] - previousPhaseOffsets[binIndex];
			long k = Math.round(cbin * binIndex - inv_2pi * phaseDelta);
			frequencyInHertz = (float) (inv_2pideltat * phaseDelta  + inv_deltat * k);
		} else {
			frequencyInHertz = (float) fft.binToHz(binIndex, sampleRate);
		}
		return frequencyInHertz;
	}
	
	/**
	 * Calculate a noise floor for an array of magnitudes.
	 * @param magnitudes The magnitudes of the current frame.
	 * @param medianFilterLength The length of the median filter used to determine the noise floor.
	 * @param noiseFloorFactor The noise floor is multiplied with this factor to determine if the
	 * information is either noise or an interesting spectral peak.
	 * @return a float array representing the noise floor.
	 */
	public static float[] calculateNoiseFloor(float[] magnitudes, int medianFilterLength, float noiseFloorFactor) {
		double[] noiseFloorBuffer;
		float[] noisefloor = new float[magnitudes.length];
		
		float median = (float) median(magnitudes.clone());
		
		// Naive median filter implementation.
		// For each element take a median of surrounding values (noiseFloorBuffer) 
		// Store the median as the noise floor.
		for (int i = 0; i < magnitudes.length; i++) {
			noiseFloorBuffer = new double[medianFilterLength];
			int index = 0;
			for (int j = i - medianFilterLength/2; j <= i + medianFilterLength/2 && index < noiseFloorBuffer.length; j++) {
			  if(j >= 0 && j < magnitudes.length){
				noiseFloorBuffer[index] = magnitudes[j];
			  } else{
				noiseFloorBuffer[index] = median;
			  } 
			  index++;
			}		
			// calculate the noise floor value.
			noisefloor[i] = (float) (median(noiseFloorBuffer) * (noiseFloorFactor)) ;
		}
		
		float rampLength = 12.0f;
		for(int i = 0 ; i <= rampLength ; i++){
			//ramp
			float ramp = 1.0f;
			ramp = (float) (-1 * (Math.log(i/rampLength))) + 1.0f;
			noisefloor[i] = ramp * noisefloor[i];
		}
		
		return noisefloor;
	}
	
	/**
	 * Finds the local magintude maxima and stores them in the given list.
	 * @param magnitudes The magnitudes.
	 * @param noisefloor The noise floor.
	 * @return a list of local maxima.
	 */
	public static List<Integer> findLocalMaxima(float[] magnitudes,float[] noisefloor){
		List<Integer> localMaximaIndexes = new ArrayList<Integer>();
		for (int i = 1; i < magnitudes.length - 1; i++) {
			boolean largerThanPrevious = (magnitudes[i - 1] < magnitudes[i]);
			boolean largerThanNext = (magnitudes[i] > magnitudes[i + 1]);
			boolean largerThanNoiseFloor = (magnitudes[i] >  noisefloor[i]);
			if (largerThanPrevious && largerThanNext && largerThanNoiseFloor) {
				localMaximaIndexes.add(i);
			}
		}
		return localMaximaIndexes;
	}
	
	/**
	 * @param magnitudes the magnitudes.
	 * @return the index for the maximum magnitude.
	 */
	private static int findMaxMagnitudeIndex(float[] magnitudes){
		int maxMagnitudeIndex = 0;
		float maxMagnitude = (float) -1e6;
		for (int i = 1; i < magnitudes.length - 1; i++) {
			if(magnitudes[i] > maxMagnitude){
				maxMagnitude = magnitudes[i];
				maxMagnitudeIndex = i;
			}
		}
		return maxMagnitudeIndex;
	}
	
	/**
	 * 
	 * @param magnitudes the magnitudes..
	 * @param frequencyEstimates The frequency estimates for each bin.
	 * @param localMaximaIndexes The indexes of the local maxima.
	 * @param numberOfPeaks The requested number of peaks.
	 * @param minDistanceInCents The minimum distance in cents between the peaks
	 * @return A list with spectral peaks.
	 */
	public static List<SpectralPeak> findPeaks(float[] magnitudes, float[] frequencyEstimates, List<Integer> localMaximaIndexes, int numberOfPeaks, int minDistanceInCents){
		int maxMagnitudeIndex = findMaxMagnitudeIndex(magnitudes);		
		List<SpectralPeak> spectralPeakList = new ArrayList<SpectralPeak>();
		
		if(localMaximaIndexes.size()==0)
			return spectralPeakList;
		
		float referenceFrequency=0;
		//the frequency of the bin with the highest magnitude
		referenceFrequency =  frequencyEstimates[maxMagnitudeIndex];
		
		//remove frequency estimates below zero
		for(int i = 0 ; i < localMaximaIndexes.size() ; i++){
			if(frequencyEstimates[localMaximaIndexes.get(i)] < 0 ){
				localMaximaIndexes.remove(i);
				frequencyEstimates[localMaximaIndexes.get(i)]=1;//Hz
				i--;
			}
		}
		
		//filter the local maxima indexes, remove peaks that are too close to each other
		//assumes that localmaximaIndexes is sorted from lowest to higest index
		for(int i = 1 ; i < localMaximaIndexes.size() ; i++){
			double centCurrent = PitchConverter.hertzToAbsoluteCent(frequencyEstimates[localMaximaIndexes.get(i)]);
			double centPrev = PitchConverter.hertzToAbsoluteCent(frequencyEstimates[localMaximaIndexes.get(i-1)]);
			double centDelta = centCurrent - centPrev;
			if(centDelta  < minDistanceInCents ){
				if(magnitudes[localMaximaIndexes.get(i)] > magnitudes[localMaximaIndexes.get(i-1)]){
					localMaximaIndexes.remove(i-1);
				}else{
					localMaximaIndexes.remove(i);
				}
				i--;
			}
		}
		
		// Retrieve the maximum values for the indexes
		float[] maxMagnitudes = new float[localMaximaIndexes.size()];
		for(int i = 0 ; i < localMaximaIndexes.size() ; i++){
			maxMagnitudes[i] = magnitudes[localMaximaIndexes.get(i)];
		}
		// Sort the magnitudes in ascending order
		Arrays.sort(maxMagnitudes);
		
		// Find the threshold, the first value or somewhere in the array.
		float peakthresh = maxMagnitudes[0];
		if (maxMagnitudes.length > numberOfPeaks) {
			peakthresh = maxMagnitudes[maxMagnitudes.length - numberOfPeaks];
		}
		
		//store the peaks
		for(Integer i : localMaximaIndexes){
			if(magnitudes[i]>= peakthresh){
				final float frequencyInHertz= frequencyEstimates[i];
				//ignore frequencies lower than 30Hz
				float binMagnitude = magnitudes[i];
				SpectralPeak peak = new SpectralPeak(0,frequencyInHertz, binMagnitude, referenceFrequency,i);
				spectralPeakList.add(peak);
			}
		}
		return spectralPeakList;
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
	
	public static double median(float[] m) {
//		Sort the array in ascending order.
		Arrays.sort(m);
	    int middle = m.length/2;
	    if (m.length%2 == 1) {
	        return m[middle];
	    } else {
	        return (m[middle-1] + m[middle]) / 2.0;
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
			if(referenceFrequency > 0 && frequencyInHertz > 0){
				float refInCents = (float) PitchConverter.hertzToAbsoluteCent(referenceFrequency);
				float valueInCents = (float) PitchConverter.hertzToAbsoluteCent(frequencyInHertz);
				return valueInCents - refInCents;
			}else{
				return 0;
			}
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
		
		public float getRefFrequencyInHertz(){
			return referenceFrequency;
		}
		
		public String toString(){
			return String.format("%.2f %.2f %.2f", frequencyInHertz,getRelativeFrequencyInCents(),magnitude);
		}

		public int getBin() {
			return bin;
		}
	}


}
