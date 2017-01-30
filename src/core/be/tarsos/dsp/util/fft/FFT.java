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


package be.tarsos.dsp.util.fft;


/**
 * Wrapper for calling a hopefully Fast Fourier transform. Makes it easy to
 * switch FFT algorithm with minimal overhead.
 * Support for window functions is also present.
 * 
 * @author Joren Six
 */
public final class FFT {

	/**
	 * Forward FFT.
	 */
	private final FloatFFT fft;
	private final WindowFunction windowFunction;
	private final int fftSize;
	private final float[] window; 

	public FFT(final int size) {
		this(size,null);
	}
	

	
	/**
	 * Create a new fft of the specified size. Apply the specified window on the samples before a forward transform. 
	 * warning: the window is not applied in reverse when a backwards transform is requested.
	 * @param size The size of the fft.
	 * @param windowFunction Apply the specified window on the samples before a forward transform. 
	 * warning: the window is not applied in reverse when a backwards transform is requested.
	 */
	public FFT(final int size, final WindowFunction windowFunction){
		fft = new FloatFFT(size);
		fftSize = size;
		this.windowFunction = windowFunction;
		if(windowFunction==null)
			window = null;
		else
		   window = windowFunction.generateCurve(size);
	}

	/**
	 * Computes forward DFT.
	 * 
	 * @param data
	 *            data to transform.
	 */
	public void forwardTransform(final float[] data) {
		if(windowFunction!=null){
			for(int i = 0 ; i < data.length ; i++){
				data[i] = data[i] * window[i];
			}
			//windowFunction.apply(data);
		}
		fft.realForward(data);
	}
	
	public void complexForwardTransform(final float[] data) {
		if(windowFunction!=null){
			for(int i = 0 ; i < data.length ; i++){
				data[i] = data[i] * window[i];
			}
			//windowFunction.apply(data);
		}
		fft.complexForward(data);
	}

	/**
	 * Computes inverse DFT.
	 * Warning, does not reverse the window function.
	 * @param data
	 *            data to transform
	 */
	public void backwardsTransform(final float[] data) {
		fft.realInverse(data, true);
	}

	public double binToHz(final int binIndex, final float sampleRate) {
		return binIndex * sampleRate / (double) fftSize;
	}
	
	public int size(){
		return fftSize;
	}

	/**
	 * Returns the modulus of the element at index bufferCount. The modulus,
	 * magnitude or absolute value is (a²+b²) ^ 0.5 with a being the real part
	 * and b the imaginary part of a complex number.
	 * 
	 * @param data
	 *            The FFT transformed data.
	 * @param index
	 *            The index of the element.
	 * @return The modulus, magnitude or absolute value of the element at index
	 *         bufferCount
	 */
	public float modulus(final float[] data, final int index) {
		final int realIndex = 2 * index;
		final int imgIndex =  2 * index + 1;
		final float modulus = data[realIndex] * data[realIndex] + data[imgIndex] * data[imgIndex];
		return (float) Math.sqrt(modulus);
	}

	/**
	 * Calculates the the modulus for each element in data and stores the result
	 * in amplitudes.
	 * 
	 * @param data
	 *            The input data.
	 * @param amplitudes
	 *            The output modulus info or amplitude.
	 */
	public void modulus(final float[] data, final float[] amplitudes) {
		assert data.length / 2 == amplitudes.length;
		for (int i = 0; i < amplitudes.length; i++) {
			amplitudes[i] = modulus(data, i);
		}
	}
	
	/**
	 * Computes an FFT and converts the results to polar coordinates (power and
	 * phase). Both the power and phase arrays must be the same length, data
	 * should be double the length.
	 * 
	 * @param data
	 *            The input audio signal.
	 * @param power
	 *            The power (modulus) of the data.
	 * @param phase
	 *            The phase of the data
	 */
	public void powerPhaseFFT(float[] data,float[] power, float[] phase) {
		assert data.length / 2 == power.length;
		assert data.length / 2 == phase.length;
		if(windowFunction!=null){
			windowFunction.apply(data);
		}
		fft.realForward(data);
		powerAndPhaseFromFFT(data, power, phase);
	}
	
	
	/**
	 * Returns magnitude (or power) and phase for the FFT transformed data. 
	 * @param data The FFT transformed data.
	 * @param power The array where the magnitudes or powers are going to be stored. It is half the length of data (FFT size).
	 * @param phase The array where the phases are going to be stored. It is half the length of data (FFT size).
	 */
	public void powerAndPhaseFromFFT(float[] data,float[] power, float[] phase){
		phase[0] = (float) Math.PI;
		power[0] = -data[0];
		for (int i = 1; i < power.length; i++) {
			int realIndex = 2 * i;
			int imgIndex  = 2 * i + 1;
			power[i] = (float) Math.sqrt(data[realIndex] * data[realIndex] + data[imgIndex] * data[imgIndex]);
			phase[i] = (float) Math.atan2(data[imgIndex], data[realIndex]);
		}
	}
	
	public void powerPhaseFFTBeatRootOnset(float[] data,float[] power, float[] phase) {
		powerPhaseFFT(data, power, phase);
		power[0] = (float) Math.sqrt(data[0] * data[0] + data[1] * data[1]);
	}
	
	/**
	 * Multiplies to arrays containing imaginary numbers. The data in the first argument
	 * is modified! The real part is stored at <code>2*i</code>, the imaginary part <code>2*i+i</code>
	 * @param data The array with imaginary numbers that is modified.
	 * @param other The array with imaginary numbers that is not modified.
	 * Data and other need to be the same length.
	 */
	public void multiply(float[] data, float[] other){
		assert data.length == other.length;
		if(data.length!=other.length){
			throw new IllegalArgumentException("Both arrays with imaginary numbers shouldb e of equal length");
		}
		for (int i = 1; i < data.length-1; i+=2) {
			int realIndex = i;
			int imgIndex = i + 1;
			float tempReal = data[realIndex] * other[realIndex] + -1 * data[imgIndex] * other[imgIndex];
			float tempImg = data[realIndex] * other[imgIndex] + data[imgIndex] * other[realIndex];
			data[realIndex] = tempReal;
			data[imgIndex] = tempImg;
			//fix by perfecthu 
			//data[realIndex] = data[realIndex] * other[realIndex] + -1 * data[imgIndex] * other[imgIndex];
			//data[imgIndex] = data[realIndex] * other[imgIndex] + data[imgIndex] * other[realIndex];
			}
	}
}
