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

package be.hogent.tarsos.dsp.filters;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;

/**
 * An Infinite Impulse Response, or IIR, filter is a filter that uses a set of
 * coefficients and previous filtered values to filter a stream of audio. It is
 * an efficient way to do digital filtering. IIRFilter is a general IIRFilter
 * that simply applies the filter designated by the filter coefficients so that
 * sub-classes only have to dictate what the values of those coefficients are by
 * defining the <code>calcCoeff()</code> function. When filling the coefficient
 * arrays, be aware that <code>b[0]</code> corresponds to
 * <code>b<sub>1</sub></code>.
 * 
 * @author Damien Di Fede
 * @author Joren Six
 * 
 */
public abstract class IIRFilter implements AudioProcessor {
	
	/** The b coefficients. */
	protected float[] b;

	/** The a coefficients. */
	protected float[] a;

	/**
	 * The input values to the left of the output value currently being
	 * calculated.
	 */
	protected float[] in;
	
	/** The previous output values. */
	protected float[] out;

	private final float frequency;
	
	private final float sampleRate;


	/**
	 * Constructs an IIRFilter with the given cutoff frequency that will be used
	 * to filter audio recorded at <code>sampleRate</code>.
	 * 
	 * @param freq
	 *            the cutoff frequency
	 * @param sampleRate
	 *            the sample rate of audio to be filtered
	 */
	public IIRFilter(float freq, float sampleRate) {
		this.sampleRate = sampleRate;
		this.frequency = freq;	
		calcCoeff();
		in = new float[a.length];
		out = new float[b.length];
	}

	/**
	 * Returns the cutoff frequency (in Hz).
	 * 
	 * @return the current cutoff frequency (in Hz).
	 */
	protected final float getFrequency() {
		return frequency;
	}
	
	protected final float getSampleRate(){
		return sampleRate;
	}

	/**
	 * Calculates the coefficients of the filter using the current cutoff
	 * frequency. To make your own IIRFilters, you must extend IIRFilter and
	 * implement this function. The frequency is expressed as a fraction of the
	 * sample rate. When filling the coefficient arrays, be aware that
	 * <code>b[0]</code> corresponds to the coefficient
	 * <code>b<sub>1</sub></code>.
	 * 
	 */
	protected abstract void calcCoeff() ;

	
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audioFloatBuffer = audioEvent.getFloatBuffer();
		
		for (int i = audioEvent.getOverlap(); i < audioFloatBuffer.length; i++) {
			//shift the in array
			System.arraycopy(in, 0, in, 1, in.length - 1);
			in[0] = audioFloatBuffer[i];
	
			//calculate y based on a and b coefficients
			//and in and out.
			float y = 0;
			for(int j = 0 ; j < a.length ; j++){
				y += a[j] * in[j];
			}			
			for(int j = 0 ; j < b.length ; j++){
				y += b[j] * out[j];
			}
			//shift the out array
			System.arraycopy(out, 0, out, 1, out.length - 1);
			out[0] = y;
			
			audioFloatBuffer[i] = y;
		} 
		return true;
	}
	

	@Override
	public void processingFinished() {
		
	}
}
