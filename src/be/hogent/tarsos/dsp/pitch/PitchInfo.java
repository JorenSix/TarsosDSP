package be.hogent.tarsos.dsp.pitch;

import be.hogent.tarsos.dsp.AudioEvent;

/**
 * A class with information about the result of a pitch detection on a block of
 * audio.
 * 
 * It contains:
 * 
 * <ul>
 * <li>The pitch in Hertz.</li>
 * <li>A probability (noisiness, (a)periodicity, salience, voicedness or clarity
 * measure) for the detected pitch. This is somewhat similar to the term voiced
 * which is used in speech recognition. This probability should be calculated
 * together with the pitch but is returned using a call to this method. So if
 * you want the probability of a buffer: first call getPitch(buffer) and then
 * getProbability().</li>
 * <li>A way to calculate the RMS of the signal.</li>
 * <li>A boolean that indicates if the algorithm thinks the signal is pitched or
 * not.</li>
 * </ul>
 * 
 * The separate pitched or unpitched boolean can coexist with a defined pitch.
 * E.g. if the algorithm detects 220Hz in a noisy signal it may respond with
 * 220Hz "unpitched".
 * 
 * 
 * @author Joren Six
 */
public class PitchInfo {
	
	/**
	 * The audio data encoded in floats from -1.0 to 1.0.
	 */
	private float[] floatBuffer;
	
	/**
	 * The pitch in Hertz.
	 */
	private float pitch;
	
	private float probability;
	
	private boolean pitched;
	
	
	public float getPitch() {
		return pitch;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public float getProbability() {
		return probability;
	}

	public void setProbability(float probability) {
		this.probability = probability;
	}

	public boolean isPitched() {
		return pitched;
	}

	public void setPitched(boolean pitched) {
		this.pitched = pitched;
	}

	public void setFloatBuffer(float[] floatBuffer) {
		this.floatBuffer = floatBuffer;
	}
	
	public float[] getFloatBuffer(){
		return floatBuffer;
	}
	
	/**
	 * Calculates and returns the root mean square of the signal. Please
	 * cache the result since it is calculated every time.
	 * @return The <a
	 *         href="http://en.wikipedia.org/wiki/Root_mean_square">RMS</a> of
	 *         the signal present in the current buffer.
	 */
	public double getRMS() {
		return AudioEvent.calculateRMS(floatBuffer);
	}
	
}
