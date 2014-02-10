/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -----------------------------------------------------------
*
*  TarsosDSP is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*  https://github.com/JorenSix/TarsosDSP
*  http://tarsos.0110.be/releases/TarsosDSP/
* 
*/
/* 
 * Copyright (c) 2006, Karl Helgason
 * 
 * 2007/1/8 modified by p.j.leonard
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package be.hogent.tarsos.dsp;

import be.hogent.tarsos.dsp.util.fft.FFT;

/**
 * Implementation of the Constant Q Transform.<br> References:
 * <p>
 * Judith C. Brown, <a
 * href="http://www.wellesley.edu/Physics/brown/pubs/cq1stPaper.pdf">
 * Calculation of a constant Q spectral transform</a>, J. Acoust. Soc. Am.,
 * 89(1): 425-434, 1991.
 * </p>
 * <p>
 * Judith C. Brown and Miller S. Puckette, <a
 * href="http://www.wellesley.edu/Physics/brown/pubs/effalgV92P2698-P2701.pdf"
 * >An efficient algorithm for the calculation of a constant Q transform</a>, J.
 * Acoust. Soc. Am., Vol. 92, No. 5, November 1992
 * </p>
 * <p>
 * Benjamin Blankertz, <a href=
 * "http://wwwmath1.uni-muenster.de/logik/org/staff/blankertz/constQ/constQ.pdf"
 * >The Constant Q Transform</a>
 * </p>
 * 
 * 
 * @author Joren Six
 * @author Karl Helgason
 * @author P.J Leonard
 */
public class ConstantQ implements AudioProcessor {


	/**
	 * The minimum frequency, in Hertz. The Constant-Q factors are calculated
	 * starting from this frequency.
	 */
	private final float minimumFrequency ;

	/**
	 * The maximum frequency in Hertz.
	 */
	private final float maximumFreqency;

	/**
	 * The length of the underlying FFT.
	 */
	private int fftLength;

	/**
	 * Lists the start of each frequency bin, in Hertz. 
	 */
	private final float[] frequencies;

	private final float[][] qKernel;

	private final int[][] qKernel_indexes;
	
	/**
	 * The array with constant q coefficients. If you for
	 * example are interested in coefficients between 256 and 1024 Hz
	 * (2^8 and 2^10 Hz) and you requested 12 bins per octave, you
	 * will need 12 bins/octave * 2 octaves * 2 entries/bin = 48
	 * places in the output buffer. The coefficient needs two entries
	 * in the output buffer since they are complex numbers.
	 */
	private final float[] coefficients;
	
	/**
	 * The output buffer with constant q magnitudes. If you for example are
	 * interested in coefficients between 256 and 1024 Hz (2^8 and 2^10 Hz) and
	 * you requested 12 bins per octave, you will need 12 bins/octave * 2
	 * octaves = 24 places in the output buffer.
	 */
	private final float[] magnitudes;
	
	/**
	 * The number of bins per octave.
	 */
	private int binsPerOctave;

	/**
	 * The underlying FFT object.
	 */
	private FFT fft;


	public ConstantQ(float sampleRate, float minFreq, float maxFreq,float binsPerOctave) {
		this(sampleRate,minFreq,maxFreq,binsPerOctave,0.001f,1.0f);
	}

	
	public ConstantQ(float sampleRate, float minFreq, float maxFreq,float binsPerOctave, float threshold,float spread) {
		this.minimumFrequency = minFreq;
		this.maximumFreqency = maxFreq;
		this.binsPerOctave = (int) binsPerOctave;
		
		// Calculate Constant Q		
		double q = 1.0 / (Math.pow(2, 1.0 / binsPerOctave) - 1.0) / spread;

		// Calculate number of output bins
		int numberOfBins = (int) Math.ceil(binsPerOctave * Math.log(maximumFreqency / minimumFrequency) / Math.log(2));
		
		// Initialize the coefficients array (complex number so 2 x number of bins)
		coefficients = new float[numberOfBins*2];
		
		// Initialize the magnitudes array
		magnitudes = new float[numberOfBins];

		
		// Calculate the minimum length of the FFT to support the minimum
		// frequency
		float calc_fftlen = (float) Math.ceil(q * sampleRate / minimumFrequency);
		
		// No need to use power of 2 FFT length.
		fftLength = (int) calc_fftlen; 
		
		//System.out.println(fftLength);
		//The FFT length needs to be a power of two for performance reasons:
		fftLength = (int) Math.pow(2, Math.ceil(Math.log(calc_fftlen) / Math.log(2)));
		
		// Create FFT object
		fft = new FFT(fftLength);
		qKernel = new float[numberOfBins][];
		qKernel_indexes = new int[numberOfBins][];
		frequencies = new float[numberOfBins];

		// Calculate Constant Q kernels
		 float[] temp = new float[fftLength*2];
		 float[] ctemp = new float[fftLength*2];
		 int[] cindexes = new int[fftLength];
		    for (int i = 0; i < numberOfBins; i++) {
		    	float[] sKernel = temp;
		    	// Calculate the frequency of current bin
		    	frequencies[i] = (float) (minimumFrequency * Math.pow(2, i/binsPerOctave ));
		    	
		    	// Calculate length of window
		    	int len = (int)Math.min(Math.ceil( q * sampleRate / frequencies[i]), fftLength);
		    	
		    	for (int j = 0; j < len; j++) {
		    		
		    		double window = -.5*Math.cos(2.*Math.PI*(double)j/(double)len)+.5;; // Hanning Window
		    		// double window = -.46*Math.cos(2.*Math.PI*(double)j/(double)len)+.54; // Hamming Window

		    		window /= len;
		    		
		    		// Calculate kernel
		    	    double x = 2*Math.PI * q * (double)j/(double)len;
		    		sKernel[j*2] = (float) (window * Math.cos(x));
		    		sKernel[j*2+1] = (float) (window * Math.sin(x));	
				}
		    	for (int j = len*2; j < fftLength*2; j++) {
		    		sKernel[j] = 0;
		    	}

			// Perform FFT on kernel
			fft.complexForwardTransform(sKernel);

			// Remove all zeros from kernel to improve performance
			float[] cKernel = ctemp;

			int k = 0;
			for (int j = 0, j2 = sKernel.length - 2; j < sKernel.length/2; j+=2,j2-=2)
	    	{
	    		double absval = Math.sqrt(sKernel[j]*sKernel[j] + sKernel[j+1]*sKernel[j+1]);
	    	    absval += Math.sqrt(sKernel[j2]*sKernel[j2] + sKernel[j2+1]*sKernel[j2+1]);	    	    
	    		if(absval > threshold)
	    		{
	    			cindexes[k] = j;
	    			cKernel[2*k] = sKernel[j] + sKernel[j2];
	    			cKernel[2*k + 1] = sKernel[j + 1] + sKernel[j2 + 1];
	    			k++;
	    		}	    		
	    	}

			sKernel = new float[k * 2];
			int[] indexes = new int[k];

			for (int j = 0; j < k * 2; j++)
				sKernel[j] = cKernel[j];
			for (int j = 0; j < k; j++)
				indexes[j] = cindexes[j];

			// Normalize fft output
			for (int j = 0; j < sKernel.length; j++)
				sKernel[j] /= fftLength;

			// Perform complex conjugate on sKernel
			for (int j = 1; j < sKernel.length; j += 2)
				sKernel[j] = -sKernel[j];
			
			for (int j = 0; j < sKernel.length; j ++)
				sKernel[j] = -sKernel[j];

			qKernel_indexes[i] = indexes;
			qKernel[i] = sKernel;
		}
	}

	/**
	 * Take an input buffer with audio and calculate the constant Q
	 * coefficients.
	 * 
	 * @param inputBuffer
	 *            The input buffer with audio.
	 * 
	 *            
	 */
	public void calculate(float[] inputBuffer) {
		fft.forwardTransform(inputBuffer);
		for (int i = 0; i < qKernel.length; i++) {
			float[] kernel = qKernel[i];
			int[] indexes = qKernel_indexes[i];
			float t_r = 0;
			float t_i = 0;
			for (int j = 0, l = 0; j < kernel.length; j += 2, l++) {
				int jj = indexes[l];
				float b_r = inputBuffer[jj];
				float b_i = inputBuffer[jj + 1];
				float k_r = kernel[j];
				float k_i = kernel[j + 1];
				// COMPLEX: T += B * K
				t_r += b_r * k_r - b_i * k_i;
				t_i += b_r * k_i + b_i * k_r;
			}
			coefficients[i * 2] = t_r;
			coefficients[i * 2 + 1] = t_i;
		}
	}
	
	/**
	 * Take an input buffer with audio and calculate the constant Q magnitudes.
	 * @param inputBuffer The input buffer with audio.
	 */
	public void calculateMagintudes(float[] inputBuffer) {
		calculate(inputBuffer);
		for(int i = 0 ; i < magnitudes.length ; i++){
			magnitudes[i] = (float) Math.sqrt(coefficients[i*2] * coefficients[i*2] + coefficients[i*2+1] * coefficients[i*2+1]); 
		}
	}
	

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audioBuffer = audioEvent.getFloatBuffer().clone();
		if(audioBuffer.length != getFFTlength()){
			throw new IllegalArgumentException(String.format("The length of the fft (%d) should be the same as the length of the audio buffer (%d)",getFFTlength(),audioBuffer.length));
		}
		calculateMagintudes(audioBuffer);
		return true;
	}

	@Override
	public void processingFinished() {
		// Do nothing.
	}
	
	//----GETTERS

	/**
	 * @return The list of starting frequencies for each band. In Hertz.
	 */
	public float[] getFreqencies() {
		return frequencies;
	}
	
	/**
	 * Returns the Constant Q magnitudes calculated for the previous audio
	 * buffer. Beware: the array is reused for performance reasons. If your need
	 * to cache your results, please copy the array.
	 * @return The output buffer with constant q magnitudes. If you for example are
	 * interested in coefficients between 256 and 1024 Hz (2^8 and 2^10 Hz) and
	 * you requested 12 bins per octave, you will need 12 bins/octave * 2
	 * octaves = 24 places in the output buffer.
	 */
	public float[] getMagnitudes() {
		return magnitudes;
	}

	
	/**
	 * Return the Constant Q coefficients calculated for the previous audio
	 * buffer. Beware: the array is reused for performance reasons. If your need
	 * to cache your results, please copy the array.
	 * 
	 * @return The array with constant q coefficients. If you for example are
	 *         interested in coefficients between 256 and 1024 Hz (2^8 and 2^10
	 *         Hz) and you requested 12 bins per octave, you will need 12
	 *         bins/octave * 2 octaves * 2 entries/bin = 48 places in the output
	 *         buffer. The coefficient needs two entries in the output buffer
	 *         since they are complex numbers.
	 */
	public float[] getCoefficients() {
		return coefficients;
	}

	/**
	 * @return The number of coefficients, output bands.
	 */
	public int getNumberOfOutputBands() {
		return frequencies.length;
	}

	/**
	 * @return The required length the FFT.
	 */
	public int getFFTlength() {
		return fftLength;
	}
	
	/**
	 * @return the number of bins every octave.
	 */
	public int getBinsPerOctave(){
		return binsPerOctave;
	}
}
