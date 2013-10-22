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
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://tarsos.0110.be/releases/TarsosDSP/
*  
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
* 
*/

package be.hogent.tarsos.dsp.onsets;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.util.fft.FFT;

/**
 * <p>
 * Estimates the locations of percussive onsets using a simple method described
 * in <a
 * href="http://arrow.dit.ie/cgi/viewcontent.cgi?article=1018&context=argcon"
 * >"Drum Source Separation using Percussive Feature Detection and Spectral
 * Modulation"</a> by Dan Barry, Derry Fitzgerald, Eugene Coyle and Bob Lawlor,
 * ISSC 2005.
 * </p>
 * <p>
 * Implementation based on a <a href=
 * "http://vamp-plugins.org/code-doc/PercussionOnsetDetector_8cpp-source.html"
 * >VAMP plugin example</a> by Chris Cannam at Queen Mary, London:
 * 
 * <pre>
 *  Centre for Digital Music, Queen Mary, University of London.
 *  Copyright 2006 Chris Cannam.
 *    
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use, copy,
 *  modify, merge, publish, distribute, sublicense, and/or sell copies
 *  of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *  
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR
 *  ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 *  CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 *  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *  
 *  Except as contained in this notice, the names of the Centre for
 *  Digital Music; Queen Mary, University of London; and Chris Cannam
 *  shall not be used in advertising or otherwise to promote the sale,
 *  use or other dealings in this Software without prior written
 *  authorization.
 * </pre>
 * 
 * </p>
 * 
 * @author Joren Six
 * @author Chris Cannam
 */
public class PercussionOnsetDetector implements AudioProcessor, OnsetDetector {

	public static final double DEFAULT_THRESHOLD = 8;
	
	public static final double DEFAULT_SENSITIVITY = 20;

	private final FFT fft;

	private final float[] priorMagnitudes;
	private final float[] currentMagnitudes;

	private float dfMinus1, dfMinus2;

	private OnsetHandler handler;

	private final float sampleRate;//samples per second (Hz)
	private long processedSamples;//in samples
	
	/**
	 * Sensitivity of peak detector applied to broadband detection function (%).
	 * In [0-100].
	 */
	private final double sensitivity;
	
	/**
	 * Energy rise within a frequency bin necessary to count toward broadband
	 * total (dB). In [0-20].
	 * 
	 */
	private final double threshold;

	/**
	 * Create a new percussion onset detector. With a default sensitivity and threshold.
	 * 
	 * @param sampleRate
	 *            The sample rate in Hz (used to calculate timestamps)
	 * @param bufferSize
	 *            The size of the buffer in samples.
	 * @param bufferOverlap
	 *            The overlap of buffers in samples.
	 * @param handler
	 *            An interface implementor to handle percussion onset events.
	 */
	public PercussionOnsetDetector(float sampleRate, int bufferSize,
			int bufferOverlap, OnsetHandler handler) {
		this(sampleRate, bufferSize, handler,
				DEFAULT_SENSITIVITY, DEFAULT_THRESHOLD);
	}

	/**
	 * Create a new percussion onset detector.
	 * 
	 * @param sampleRate
	 *            The sample rate in Hz (used to calculate timestamps)
	 * @param bufferSize
	 *            The size of the buffer in samples.
	 * @param handler
	 *            An interface implementor to handle percussion onset events.
	 * @param sensitivity
	 *            Sensitivity of the peak detector applied to broadband
	 *            detection function (%). In [0-100].
	 * @param threshold
	 *            Energy rise within a frequency bin necessary to count toward
	 *            broadband total (dB). In [0-20].
	 */
	public PercussionOnsetDetector(float sampleRate, int bufferSize, OnsetHandler handler, double sensitivity, double threshold) {
		fft = new FFT(bufferSize / 2);
		this.threshold = threshold;
		this.sensitivity = sensitivity;
		priorMagnitudes = new float[bufferSize / 2];
		currentMagnitudes = new float[bufferSize / 2];
		this.handler = handler;
		this.sampleRate = sampleRate;
		
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audioFloatBuffer = audioEvent.getFloatBuffer();
		this.processedSamples += audioFloatBuffer.length;
		this.processedSamples -= audioEvent.getOverlap();

		fft.forwardTransform(audioFloatBuffer);
		fft.modulus(audioFloatBuffer, currentMagnitudes);
		int binsOverThreshold = 0;
		for (int i = 0; i < currentMagnitudes.length; i++) {
			if (priorMagnitudes[i] > 0.f) {
				double diff = 10 * Math.log10(currentMagnitudes[i]
						/ priorMagnitudes[i]);
				if (diff >= threshold) {
					binsOverThreshold++;
				}
			}
			priorMagnitudes[i] = currentMagnitudes[i];
		}

		if (dfMinus2 < dfMinus1
				&& dfMinus1 >= binsOverThreshold
				&& dfMinus1 > ((100 - sensitivity) * audioFloatBuffer.length) / 200) {
			float timeStamp = processedSamples / sampleRate;
			handler.handleOnset(timeStamp,-1);
		}

		dfMinus2 = dfMinus1;
		dfMinus1 = binsOverThreshold;

		return true;
	}

	@Override
	public void processingFinished() {
	}

	@Override
	public void setHandler(OnsetHandler handler) {
		this.handler = handler;
	}
}
