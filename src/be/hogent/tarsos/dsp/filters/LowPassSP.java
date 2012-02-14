/**
*
*  TarsosDSP is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*
**/
package be.hogent.tarsos.dsp.filters;

/**
 * Single pass low pass filter.
 * @author Joren Six
 */
public class LowPassSP extends IIRFilter {

	public LowPassSP(float freq, float sampleRate, int overlap) {
		super(freq, sampleRate, overlap);
	}

	@Override
	protected void calcCoeff() {
		float fracFreq = getFrequency() / getSampleRate();
		float x = (float) Math.exp(-2 * Math.PI * fracFreq);
		a = new float[] { 1 - x };
		b = new float[] { x };
	}

}
