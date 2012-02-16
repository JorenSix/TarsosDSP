package be.hogent.tarsos.dsp;

/**
 * With the gain processor it is possible to adapt the volume of the sound. With
 * a gain of 1, nothing happens. A gain greater than one is a volume increase a
 * gain between zero and one, exclusive, is a decrease. If you need to flip the
 * sign of the audio samples, you can by providing a gain of -1.0. but I have no
 * idea what you could gain by doing that (pathetic pun, I know).
 * 
 * @author Joren Six
 */
public class GainProcessor implements AudioProcessor {
	private double gain;
	private final int overlap;// in samples

	public GainProcessor(double newGain, int overlap) {
		setGain(newGain);
		this.overlap = overlap;
	}

	public void setGain(double newGain) {
		this.gain = newGain;
	}

	@Override
	public boolean processFull(float[] audioFloatBuffer, byte[] audioByteBuffer) {
		for (int i = 0; i < audioFloatBuffer.length; i++) {
			audioFloatBuffer[i] = Math.min(1.0f,
					(float) (audioFloatBuffer[i] * gain));
		}
		return true;
	}

	@Override
	public boolean processOverlapping(float[] audioFloatBuffer,
			byte[] audioByteBuffer) {
		for (int i = overlap; i < audioFloatBuffer.length; i++) {
			audioFloatBuffer[i] = Math.min(1.0f,(float) (audioFloatBuffer[i] * gain));
		}
		return false;
	}

	@Override
	public void processingFinished() {
		// TODO Auto-generated method stub

	}

}
