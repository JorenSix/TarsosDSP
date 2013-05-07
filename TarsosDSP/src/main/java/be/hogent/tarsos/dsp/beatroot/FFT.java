/*
	Copyright (C) 2001, 2006 by Simon Dixon

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License along
	with this program (the file gpl.txt); if not, download it from
	http://www.gnu.org/licenses/gpl.txt or write to the
	Free Software Foundation, Inc.,
	51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package be.hogent.tarsos.dsp.beatroot;

/** Class for computing a windowed fast Fourier transform.
 *  Implements some of the window functions for the STFT from
 *  Harris (1978), Proc. IEEE, 66, 1, 51-83.
 */
public class FFT {

	/** used in {@link FFT#fft(double[], double[], int)} to specify
	 *  a forward Fourier transform */
	public static final int FORWARD = -1;
	/** used in {@link FFT#fft(double[], double[], int)} to specify
	 *  an inverse Fourier transform */
	public static final int REVERSE = 1;
	/** used in {@link FFT#makeWindow(int,int,int)} to specify a
	 *  rectangular window function */
	public static final int RECT = 0;
	/** used in {@link FFT#makeWindow(int,int,int)} to specify a
	 *  Hamming window function */
	public static final int HAMMING = 1;
	/** used in {@link FFT#makeWindow(int,int,int)} to specify a
	 *  61-dB 3-sample Blackman-Harris window function */
	public static final int BH3 = 2;
	/** used in {@link FFT#makeWindow(int,int,int)} to specify a
	 *  74-dB 4-sample Blackman-Harris window function */
	public static final int BH4 = 3;
	/** used in {@link FFT#makeWindow(int,int,int)} to specify a
	 *  minimum 3-sample Blackman-Harris window function */
	public static final int BH3MIN = 4;
	/** used in {@link FFT#makeWindow(int,int,int)} to specify a
	 *  minimum 4-sample Blackman-Harris window function */
	public static final int BH4MIN = 5;
	/** used in {@link FFT#makeWindow(int,int,int)} to specify a
	 *  Gaussian window function */
	public static final int GAUSS = 6; 
	static final double twoPI = 2 * Math.PI;

	/** The FFT method. Calculation is inline, for complex data stored
	 *  in 2 separate arrays. Length of input data must be a power of two.
	 *  @param re        the real part of the complex input and output data
	 *  @param im        the imaginary part of the complex input and output data
	 *  @param direction the direction of the Fourier transform (FORWARD or
	 *  REVERSE)
	 *  @throws IllegalArgumentException if the length of the input data is
	 *  not a power of 2
	 */
	public static void fft(double re[], double im[], int direction) {
		int n = re.length;
		int bits = (int)Math.rint(Math.log(n) / Math.log(2));
		if (n != (1 << bits))
			throw new IllegalArgumentException("FFT data must be power of 2");
		int localN;
		int j = 0;
		for (int i = 0; i < n-1; i++) {
			if (i < j) {
				double temp = re[j];
				re[j] = re[i];
				re[i] = temp;
				temp = im[j];
				im[j] = im[i];
				im[i] = temp;
			}
			int k = n / 2;
			while ((k >= 1) &&  (k - 1 < j)) {
				j = j - k;
				k = k / 2;
			}
			j = j + k;
		}
		for(int m = 1; m <= bits; m++) {
			localN = 1 << m;
			double Wjk_r = 1;
			double Wjk_i = 0;
			double theta = twoPI / localN;
			double Wj_r = Math.cos(theta);
			double Wj_i = direction * Math.sin(theta);
			int nby2 = localN / 2;
			for (j = 0; j < nby2; j++) {
				for (int k = j; k < n; k += localN) {
					int id = k + nby2;
					double tempr = Wjk_r * re[id] - Wjk_i * im[id];
					double tempi = Wjk_r * im[id] + Wjk_i * re[id];
					re[id] = re[k] - tempr;
					im[id] = im[k] - tempi;
					re[k] += tempr;
					im[k] += tempi;
				}
				double wtemp = Wjk_r;
				Wjk_r = Wj_r * Wjk_r  - Wj_i * Wjk_i;
				Wjk_i = Wj_r * Wjk_i  + Wj_i * wtemp;
			}
		}
	} // fft()

	/** Computes the power spectrum of a real sequence (in place).
	 *  @param re the real input and output data; length must be a power of 2
	 */
	public static void powerFFT(double[] re) {
		double[] im = new double[re.length];
		fft(re, im, FORWARD);
		for (int i = 0; i < re.length; i++)
			re[i] = re[i] * re[i] + im[i] * im[i];
	} // powerFFT()	

	/** Converts a real power sequence from to magnitude representation,
	 *  by computing the square root of each value.
	 *  @param re the real input (power) and output (magnitude) data; length
	 *  must be a power of 2
	 */
	public static void toMagnitude(double[] re) {
		for (int i = 0; i < re.length; i++)
			re[i] = Math.sqrt(re[i]);
	} // toMagnitude()
	
	/** Computes the magnitude spectrum of a real sequence (in place).
	 *  @param re the real input and output data; length must be a power of 2
	 */
	public static void magnitudeFFT(double[] re) {
		powerFFT(re);
		toMagnitude(re);
	} // magnitudeFFT()

	/** Computes a complex (or real if im[] == {0,...}) FFT and converts
	 *  the results to polar coordinates (power and phase). Both arrays
	 *  must be the same length, which is a power of 2.
	 *  @param re the real part of the input data and the power of the output
	 *  data
	 *  @param im the imaginary part of the input data and the phase of the
	 *  output data
	 */
	public static void powerPhaseFFT(double[] re, double[] im) {
		fft(re, im, FORWARD);
		for (int i = 0; i < re.length; i++) {
			double pow = re[i] * re[i] + im[i] * im[i];
			im[i] = Math.atan2(im[i], re[i]);
			re[i] = pow;
		}
	} // powerPhaseFFT()
	
	/** Inline computation of the inverse FFT given spectral input data
	 *  in polar coordinates (power and phase).
	 *  Both arrays must be the same length, which is a power of 2.
	 *  @param pow the power of the spectral input data (and real part of the
	 *  output data)
	 *  @param ph the phase of the spectral input data (and the imaginary part
	 *  of the output data)
	 */
	public static void powerPhaseIFFT(double[] pow, double[] ph) {
		toMagnitude(pow);
		for (int i = 0; i < pow.length; i++) {
			double re = pow[i] * Math.cos(ph[i]);
			ph[i] = pow[i] * Math.sin(ph[i]);
			pow[i] = re;
		}
		fft(pow, ph, REVERSE);
	} // powerPhaseIFFT()
	
	/** Computes a complex (or real if im[] == {0,...}) FFT and converts
	 *  the results to polar coordinates (magnitude and phase). Both arrays
	 *  must be the same length, which is a power of 2.
	 *  @param re the real part of the input data and the magnitude of the
	 *  output data
	 *  @param im the imaginary part of the input data and the phase of the
	 *  output data
	 */
	public static void magnitudePhaseFFT(double[] re, double[] im) {
		powerPhaseFFT(re, im);
		toMagnitude(re);
	} // magnitudePhaseFFT()


	/** Fill an array with the values of a standard Hamming window function
	 *  @param data the array to be filled
	 *  @param size the number of non zero values; if the array is larger than
	 *  this, it is zero-padded symmetrically at both ends 
	 */
	static void hamming(double[] data, int size) {
		int start = (data.length - size) / 2;
		int stop = (data.length + size) / 2;
		double scale = 1.0 / (double)size / 0.54;
		double factor = twoPI / (double)size;
		for (int i = 0; start < stop; start++, i++)
			data[i] = scale * (25.0/46.0 - 21.0/46.0 * Math.cos(factor * i));
	} // hamming()

	/** Fill an array with the values of a minimum 4-sample Blackman-Harris
	 *  window function
	 *  @param data the array to be filled
	 *  @param size the number of non zero values; if the array is larger than
	 *  this, it is zero-padded symmetrically at both ends 
	 */
	static void blackmanHarris4sMin(double[] data, int size) {
		int start = (data.length - size) / 2;
		int stop = (data.length + size) / 2;
		double scale = 1.0 / (double)size / 0.36;
		for (int i = 0; start < stop; start++, i++)
			data[i] = scale * ( 0.35875 -
								0.48829 * Math.cos(twoPI * i / size) +
								0.14128 * Math.cos(2 * twoPI * i / size) -
								0.01168 * Math.cos(3 * twoPI * i / size));
	} // blackmanHarris4sMin()

	/** Fill an array with the values of a 74-dB 4-sample Blackman-Harris
	 *  window function
	 *  @param data the array to be filled
	 *  @param size the number of non zero values; if the array is larger than
	 *  this, it is zero-padded symmetrically at both ends 
	 */
	static void blackmanHarris4s(double[] data, int size) {
		int start = (data.length - size) / 2;
		int stop = (data.length + size) / 2;
		double scale = 1.0 / (double)size / 0.4;
		for (int i = 0; start < stop; start++, i++)
			data[i] = scale * ( 0.40217 -
								0.49703 * Math.cos(twoPI * i / size) +
								0.09392 * Math.cos(2 * twoPI * i / size) -
								0.00183 * Math.cos(3 * twoPI * i / size));
	} // blackmanHarris4s()

	/** Fill an array with the values of a minimum 3-sample Blackman-Harris
	 *  window function
	 *  @param data the array to be filled
	 *  @param size the number of non zero values; if the array is larger than
	 *  this, it is zero-padded symmetrically at both ends 
	 */
	static void blackmanHarris3sMin(double[] data, int size) {
		int start = (data.length - size) / 2;
		int stop = (data.length + size) / 2;
		double scale = 1.0 / (double) size / 0.42;
		for (int i = 0; start < stop; start++, i++)
			data[i] = scale * ( 0.42323 -
								0.49755 * Math.cos(twoPI * i / size) +
								0.07922 * Math.cos(2 * twoPI * i / size));
	} // blackmanHarris3sMin()

	/** Fill an array with the values of a 61-dB 3-sample Blackman-Harris
	 *  window function
	 *  @param data the array to be filled
	 *  @param size the number of non zero values; if the array is larger than
	 *  this, it is zero-padded symmetrically at both ends 
	 */
	static void blackmanHarris3s(double[] data, int size) {
		int start = (data.length - size) / 2;
		int stop = (data.length + size) / 2;
		double scale = 1.0 / (double) size / 0.45;
		for (int i = 0; start < stop; start++, i++)
			data[i] = scale * ( 0.44959 -
								0.49364 * Math.cos(twoPI * i / size) +
								0.05677 * Math.cos(2 * twoPI * i / size));
	} // blackmanHarris3s()

	/** Fill an array with the values of a Gaussian window function
	 *  @param data the array to be filled
	 *  @param size the number of non zero values; if the array is larger than
	 *  this, it is zero-padded symmetrically at both ends 
	 */
	static void gauss(double[] data, int size) { // ?? between 61/3 and 74/4 BHW
		int start = (data.length - size) / 2;
		int stop = (data.length + size) / 2;
		double delta = 5.0 / size;
		double x = (1 - size) / 2.0 * delta;
		double c = -Math.PI * Math.exp(1.0) / 10.0;
		double sum = 0;
		for (int i = start; i < stop; i++) {
			data[i] = Math.exp(c * x * x);
			x += delta;
			sum += data[i];
		}
		for (int i = start; i < stop; i++)
			data[i] /= sum;
	} // gauss()

	/** Fill an array with the values of a rectangular window function
	 *  @param data the array to be filled
	 *  @param size the number of non zero values; if the array is larger than
	 *  this, it is zero-padded symmetrically at both ends 
	 */
	static void rectangle(double[] data, int size) {
		int start = (data.length - size) / 2;
		int stop = (data.length + size) / 2;
		for (int i = start; i < stop; i++)
			data[i] = 1.0 / (double) size;
	} // rectangle()

	/** Returns an array of values of a normalised smooth window function,
	 *  as used for performing a short time Fourier transform (STFT).
	 *  All functions are normalised by length and coherent gain.
	 *  More information on characteristics of these functions can be found
	 *  in F.J. Harris (1978), On the Use of Windows for Harmonic Analysis
	 *  with the Discrete Fourier Transform, <em>Proceedings of the IEEE</em>,
	 *  66, 1, 51-83.
	 *  @param choice  the choice of window function, one of the constants
	 *  defined above
	 *  @param size    the size of the returned array
	 *  @param support the number of non-zero values in the array
	 *  @return the array containing the values of the window function
	 */
	public static double[] makeWindow(int choice, int size, int support) {
		double[] data = new double[size];
		if (support > size)
			support = size;
		switch (choice) {
			case RECT:		rectangle(data, support);			break;
			case HAMMING:	hamming(data, support);				break;
			case BH3:		blackmanHarris3s(data, support);	break;
			case BH4:		blackmanHarris4s(data, support);	break;
			case BH3MIN:	blackmanHarris3sMin(data, support);	break;
			case BH4MIN:	blackmanHarris4sMin(data, support);	break;
			case GAUSS:		gauss(data, support);				break;
			default:		rectangle(data, support);			break;
		}
		return data;
	} // makeWindow()

	/** Applies a window function to an array of data, storing the result in
	 *  the data array.
	 *  Performs a dot product of the data and window arrays. 
	 *  @param data   the array of input data, also used for output
	 *  @param window the values of the window function to be applied to data
	 */
	public static void applyWindow(double[] data, double[] window) {
		for (int i = 0; i < data.length; i++)
			data[i] *= window[i];
	} // applyWindow()

	/** Unit test of the FFT class.
	 *  Performs a forward and inverse FFT on a 1MB array of random values
	 *  and checks how closely the values are preserved.
	 *  @param args ignored
	 */
	public static void main(String[] args) {
		final int SZ = 1024 * 1024;
		double[] r1 = new double[SZ];
		double[] i1 = new double[SZ];
		double[] r2 = new double[SZ];
		double[] i2 = new double[SZ];
		for (int j = 0; j < SZ; j++) {
			r1[j] = r2[j] = Math.random();
			i1[j] = i2[j] = Math.random();
		}
		System.out.println("start");
		fft(r2, i2, FORWARD);
		System.out.println("reverse");
		fft(r2, i2, REVERSE);
		System.out.println("result");
		double err = 0;
		for (int j = 0; j < SZ; j++)
			err += Math.abs(r1[j] - r2[j] / SZ) + Math.abs(i1[j] - i2[j] / SZ);
		System.out.printf( "Err: %12.10f   Av: %12.10f\n", err, err / SZ);
	} // main()

} // class FFT
