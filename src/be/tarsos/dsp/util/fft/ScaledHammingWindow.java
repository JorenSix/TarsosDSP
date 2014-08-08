package be.hogent.tarsos.dsp.util.fft;

public class ScaledHammingWindow extends WindowFunction {

	@Override
	protected float value(int length, int index) {
		double scale = 1.0 / (double)length / 0.54;
		double factor = TWO_PI / (double)length;
		return (float) (scale * (25.0/46.0 - 21.0/46.0 * Math.cos(factor * index)));
	}

}
