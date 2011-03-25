package be.hogent.tarsos.dsp.filters;

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
