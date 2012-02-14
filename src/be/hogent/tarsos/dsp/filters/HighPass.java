package be.hogent.tarsos.dsp.filters;

/**
 * A High pass IIR filter. Frequency defines the cutoff.
 * @author Joren Six
 */
public class HighPass extends IIRFilter{
	
	public HighPass(float freq, float sampleRate, int overlap) {
		super(freq, sampleRate, overlap);
	}

	protected void calcCoeff() 
	{
    float fracFreq = getFrequency()/getSampleRate();
	  float x = (float)Math.exp(-2 * Math.PI * fracFreq);
	  a = new float[] { (1+x)/2, -(1+x)/2 };
	  b = new float[] { x };
	}

}
