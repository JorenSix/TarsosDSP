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

/*
*
*  I took Joren's Code and changed it so that 
*  it uses the FFT to calculate the difference function.
*  TarsosDSP is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*
*/

package be.tarsos.dsp.pitch;

import be.tarsos.dsp.util.fft.FloatFFT;

/**
 * An implementation of the YIN pitch tracking algorithm which uses an FFT to
 * calculate the difference function. This makes calculating the difference
 * function more performant. See <a href=
 * "http://recherche.ircam.fr/equipes/pcm/cheveign/ps/2002_JASA_YIN_proof.pdf"
 * >the YIN paper.</a> This implementation is done by <a href="mailto:matthias.mauch@elec.qmul.ac.uk">Matthias Mauch</a> and is
 * based on {@link Yin} which is based on the implementation found in <a
 * href="http://aubio.org">aubio</a> by Paul Brossier.
 * 
 * @author Matthias Mauch
 * @author Joren Six
 * @author Paul Brossier
 */
public final class FastYin implements PitchDetector {
	/**
	 * The default YIN threshold value. Should be around 0.10~0.15. See YIN
	 * paper for more information.
	 */
	private static final double DEFAULT_THRESHOLD = 0.20;

	/**
	 * The actual YIN threshold.
	 */
	private final double threshold;

	/**
	 * The audio sample rate. Most audio has a sample rate of 44.1kHz.
	 */
	private final float sampleRate;

	/**
	 * The buffer that stores the calculated values. It is exactly half the size
	 * of the input buffer.
	 */
	private final float[] yinBuffer;	
	
	/**
	 * The result of the pitch detection iteration.
	 */
	private final PitchDetectionResult result;
	
	//------------------------ FFT instance members
	
	/**
	 * Holds the FFT data, twice the length of the audio buffer.
	 */
	private final float[] audioBufferFFT;
	
	/**
	 * Half of the data, disguised as a convolution kernel.
	 */
	private final  float[] kernel;
	
	/**
	 * Buffer to allow convolution via complex multiplication. It calculates the auto correlation function (ACF).
	 */
	private final float[] yinStyleACF;
	
	/**
	 * An FFT object to quickly calculate the difference function.
	 */
	private final FloatFFT fft;

	/**
	 * Create a new pitch detector for a stream with the defined sample rate.
	 * Processes the audio in blocks of the defined size.
	 * 
	 * @param audioSampleRate
	 *            The sample rate of the audio stream. E.g. 44.1 kHz.
	 * @param bufferSize
	 *            The size of a buffer. E.g. 1024.
	 */
	public FastYin(final float audioSampleRate, final int bufferSize) {
		this(audioSampleRate, bufferSize, DEFAULT_THRESHOLD);
	}

	/**
	 * Create a new pitch detector for a stream with the defined sample rate.
	 * Processes the audio in blocks of the defined size.
	 * 
	 * @param audioSampleRate
	 *            The sample rate of the audio stream. E.g. 44.1 kHz.
	 * @param bufferSize
	 *            The size of a buffer. E.g. 1024.
	 * @param yinThreshold
	 *            The parameter that defines which peaks are kept as possible
	 *            pitch candidates. See the YIN paper for more details.
	 */
	public FastYin(final float audioSampleRate, final int bufferSize, final double yinThreshold) {
		this.sampleRate = audioSampleRate;
		this.threshold = yinThreshold;
		yinBuffer = new float[bufferSize / 2];
		//Initializations for FFT difference step
		audioBufferFFT = new float[2*bufferSize];
		kernel = new float[2*bufferSize];
		yinStyleACF = new float[2*bufferSize];
		fft = new FloatFFT(bufferSize);
		result = new PitchDetectionResult();
	}

	/**
	 * The main flow of the YIN algorithm. Returns a pitch value in Hz or -1 if
	 * no pitch is detected.
	 * 
	 * @return a pitch value in Hz or -1 if no pitch is detected.
	 */
	public PitchDetectionResult getPitch(final float[] audioBuffer) {

		final int tauEstimate;
		final float pitchInHertz;

		// step 2
		difference(audioBuffer);

		// step 3
		cumulativeMeanNormalizedDifference();

		// step 4
		tauEstimate = absoluteThreshold();

		// step 5
		if (tauEstimate != -1) {
			final float betterTau = parabolicInterpolation(tauEstimate);

			// step 6
			// TODO Implement optimization for the AUBIO_YIN algorithm.
			// 0.77% => 0.5% error rate,
			// using the data of the YIN paper
			// bestLocalEstimate()

			// conversion to Hz
			pitchInHertz = sampleRate / betterTau;
		} else{
			// no pitch found
			pitchInHertz = -1;
		}
		
		result.setPitch(pitchInHertz);

		return result;
	}

	/**
	 * Implements the difference function as described in step 2 of the YIN
	 * paper with an FFT to reduce the number of operations.
	 */
	private void difference(final float[] audioBuffer) {
		// POWER TERM CALCULATION
		// ... for the power terms in equation (7) in the Yin paper
		float[] powerTerms = new float[yinBuffer.length];
		for (int j = 0; j < yinBuffer.length; ++j) {
			powerTerms[0] += audioBuffer[j] * audioBuffer[j];
		}
		// now iteratively calculate all others (saves a few multiplications)
		for (int tau = 1; tau < yinBuffer.length; ++tau) {
			powerTerms[tau] = powerTerms[tau-1] - audioBuffer[tau-1] * audioBuffer[tau-1] + audioBuffer[tau+yinBuffer.length] * audioBuffer[tau+yinBuffer.length];  
		}

		// YIN-STYLE AUTOCORRELATION via FFT
		// 1. data
		for (int j = 0; j < audioBuffer.length; ++j) {
			audioBufferFFT[2*j] = audioBuffer[j];
			audioBufferFFT[2*j+1] = 0;
		}
		fft.complexForward(audioBufferFFT);
		
		// 2. half of the data, disguised as a convolution kernel
		for (int j = 0; j < yinBuffer.length; ++j) {
			kernel[2*j] = audioBuffer[(yinBuffer.length-1)-j];
			kernel[2*j+1] = 0;
			kernel[2*j+audioBuffer.length] = 0;
			kernel[2*j+audioBuffer.length+1] = 0;
		}
		fft.complexForward(kernel);

		// 3. convolution via complex multiplication
		for (int j = 0; j < audioBuffer.length; ++j) {
			yinStyleACF[2*j]   = audioBufferFFT[2*j]*kernel[2*j] - audioBufferFFT[2*j+1]*kernel[2*j+1]; // real
			yinStyleACF[2*j+1] = audioBufferFFT[2*j+1]*kernel[2*j] + audioBufferFFT[2*j]*kernel[2*j+1]; // imaginary
		}
		fft.complexInverse(yinStyleACF, true);
		
		// CALCULATION OF difference function
		// ... according to (7) in the Yin paper.
		for (int j = 0; j < yinBuffer.length; ++j) {
			// taking only the real part
			yinBuffer[j] = powerTerms[0] + powerTerms[j] - 2 * yinStyleACF[2 * (yinBuffer.length - 1 + j)];
		}
	}

	/**
	 * The cumulative mean normalized difference function as described in step 3
	 * of the YIN paper. <br>
	 * <code>
	 * yinBuffer[0] == yinBuffer[1] = 1
	 * </code>
	 */
	private void cumulativeMeanNormalizedDifference() {
		int tau;
		yinBuffer[0] = 1;
		float runningSum = 0;
		for (tau = 1; tau < yinBuffer.length; tau++) {
			runningSum += yinBuffer[tau];
			yinBuffer[tau] *= tau / runningSum;
		}
	}

	/**
	 * Implements step 4 of the AUBIO_YIN paper.
	 */
	private int absoluteThreshold() {
		// Uses another loop construct
		// than the AUBIO implementation
		int tau;
		// first two positions in yinBuffer are always 1
		// So start at the third (index 2)
		for (tau = 2; tau < yinBuffer.length; tau++) {
			if (yinBuffer[tau] < threshold) {
				while (tau + 1 < yinBuffer.length && yinBuffer[tau + 1] < yinBuffer[tau]) {
					tau++;
				}
				// found tau, exit loop and return
				// store the probability
				// From the YIN paper: The threshold determines the list of
				// candidates admitted to the set, and can be interpreted as the
				// proportion of aperiodic power tolerated
				// within a periodic signal.
				//
				// Since we want the periodicity and and not aperiodicity:
				// periodicity = 1 - aperiodicity
				result.setProbability(1 - yinBuffer[tau]);
				break;
			}
		}

		
		// if no pitch found, tau => -1
		if (tau == yinBuffer.length || yinBuffer[tau] >= threshold || result.getProbability() > 1.0) {
			tau = -1;
			result.setProbability(0);
			result.setPitched(false);	
		} else {
			result.setPitched(true);
		}

		return tau;
	}

	/**
	 * Implements step 5 of the AUBIO_YIN paper. It refines the estimated tau
	 * value using parabolic interpolation. This is needed to detect higher
	 * frequencies more precisely. See http://fizyka.umk.pl/nrbook/c10-2.pdf and
	 * for more background
	 * http://fedc.wiwi.hu-berlin.de/xplore/tutorials/xegbohtmlnode62.html
	 * 
	 * @param tauEstimate
	 *            The estimated tau value.
	 * @return A better, more precise tau value.
	 */
	private float parabolicInterpolation(final int tauEstimate) {
		final float betterTau;
		final int x0;
		final int x2;

		if (tauEstimate < 1) {
			x0 = tauEstimate;
		} else {
			x0 = tauEstimate - 1;
		}
		if (tauEstimate + 1 < yinBuffer.length) {
			x2 = tauEstimate + 1;
		} else {
			x2 = tauEstimate;
		}
		if (x0 == tauEstimate) {
			if (yinBuffer[tauEstimate] <= yinBuffer[x2]) {
				betterTau = tauEstimate;
			} else {
				betterTau = x2;
			}
		} else if (x2 == tauEstimate) {
			if (yinBuffer[tauEstimate] <= yinBuffer[x0]) {
				betterTau = tauEstimate;
			} else {
				betterTau = x0;
			}
		} else {
			float s0, s1, s2;
			s0 = yinBuffer[x0];
			s1 = yinBuffer[tauEstimate];
			s2 = yinBuffer[x2];
			// fixed AUBIO implementation, thanks to Karl Helgason:
			// (2.0f * s1 - s2 - s0) was incorrectly multiplied with -1
			betterTau = tauEstimate + (s2 - s0) / (2 * (2 * s1 - s2 - s0));
		}
		return betterTau;
	}
}
