/*
 *  Copyright (c) 2007 - 2008 by Damien Di Fede <ddf@compartmental.net>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package be.hogent.tarsos.dsp.util.fft;

/**
 * A Window function represents a curve which is applied to a sample buffer to
 * reduce the introduction of spectral leakage in the Fourier transform.
 * 
 * <p>
 * <b>Windowing</b>
 * <p>
 * Windowing is the process of shaping the audio samples before transforming
 * them to the frequency domain. The Fourier Transform assumes the sample buffer
 * is is a repetitive signal, if a sample buffer is not truly periodic within
 * the measured interval sharp discontinuities may arise that can introduce
 * spectral leakage. Spectral leakage is the speading of signal energy across
 * multiple FFT bins. This "spreading" can drown out narrow band signals and
 * hinder detection.
 * <p>
 * A <a href="http://en.wikipedia.org/wiki/Window_function">windowing
 * function</a> attempts to reduce spectral leakage by attenuating the measured
 * sample buffer at its end points to eliminate discontinuities. If you call the
 * <code>window()</code> function with an appropriate WindowFunction, such as
 * <code>HammingWindow()</code>, the sample buffers passed to the object for
 * analysis will be shaped by the current window before being transformed. The
 * result of using a window is to reduce the leakage in the spectrum somewhat.
 * <p>
 * <code>WindowFunction</code> handles work associated with various window
 * functions such as the Hamming window. To create your own window function you
 * must extend <code>WindowFunction</code> and implement the
 * {@link #value(int, int) value} method which defines the shape of the window
 * at a given offset. <code>WindowFunction</code> will call this method to apply
 * the window to a sample buffer. The number passed to the method is an offset
 * within the length of the window curve.
 * 
 * @author Damien Di Fede
 * @author Corban Brook
 * 
 */
public abstract class WindowFunction {

	/** The float value of 2*PI. Provided as a convenience for subclasses. */
	protected static final float TWO_PI = (float) (2 * Math.PI);
	protected int length;

	public WindowFunction() {
	}

	/**
	 * Apply the window function to a sample buffer.
	 * 
	 * @param samples
	 *            a sample buffer
	 */
	public void apply(float[] samples) {
		this.length = samples.length;

		for (int n = 0; n < samples.length; n++) {
			samples[n] *= value(samples.length, n);
		}
	}

	/**
	 * Generates the curve of the window function.
	 * 
	 * @param length
	 *            the length of the window
	 * @return the shape of the window function
	 */
	public float[] generateCurve(int length) {
		float[] samples = new float[length];
		for (int n = 0; n < length; n++) {
			samples[n] = 1f * value(length, n);
		}
		return samples;
	}

	protected abstract float value(int length, int index);
}
