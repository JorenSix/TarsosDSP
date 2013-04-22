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

package be.hogent.tarsos.dsp.effects;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;

/**
 * <p>
 * Adds a flanger effect to a signal. The implementation is done with a delay
 * buffer and an LFO in the form of a sine wave. It is probably the most
 * straightforward flanger implementation possible.
 * </p>
 * 
 * @author Joren Six
 */
public class FlangerEffect implements AudioProcessor {

	/**
	 * A simple delay buffer, it holds a number of samples determined by the
	 * maxFlangerLength and the sample rate.
	 */
	private float[] flangerBuffer;

	/**
	 * The position in the delay buffer to store the current sample.
	 */
	private int writePosition;

	/**
	 * Determines the factor of original signal that remains in the final mix.
	 * Dry should always equal 1-wet).
	 */
	private float dry;
	/**
	 * Determines the factor of flanged signal that is mixed in the final mix.
	 * Wet should always equal 1-dry.
	 */
	private float wet;
	/**
	 * The frequency for the LFO (sine).
	 */
	private double lfoFrequency;

	/**
	 * The sample rate is neede to calculate the length of the delay buffer.
	 */
	private double sampleRate;

	/**
	 * @param maxFlangerLength
	 *            in seconds
	 * @param wet
	 *            The 'wetness' of the flanging effect. A value between 0 and 1.
	 *            Zero meaning no flanging effect in the resulting signal, one
	 *            means total flanging effect and no original signal left. The
	 *            dryness of the signal is determined by dry = "1-wet".
	 * @param sampleRate
	 *            the sample rate in Hz.
	 * @param lfoFrequency
	 *            in Hertz
	 */
	public FlangerEffect(double maxFlangerLength, double wet,
			double sampleRate, double lfoFrequency) {
		flangerBuffer = new float[(int) (sampleRate * maxFlangerLength)];
		this.sampleRate = sampleRate;
		this.lfoFrequency = lfoFrequency;
		this.wet = (float) wet;
		this.dry = (float) (1 - wet);
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audioFloatBuffer = audioEvent.getFloatBuffer();
		int overlap = audioEvent.getOverlap();

		// Divide f by two, to counter rectifier below, which effectively
		// doubles the frequency
		double twoPIf = 2 * Math.PI * lfoFrequency / 2.0;
		double time = audioEvent.getTimeStamp();
		double timeStep = 1.0 / sampleRate;

		for (int i = overlap; i < audioFloatBuffer.length; i++) {

			// Calculate the LFO delay value with a sine wave:
			double lfoValue = flangerBuffer.length * Math.sin(twoPIf * time);
			// add a time step, each iteration
			time += timeStep;

			// Make the delay a positive integer
			int delay = (int) (Math.round(Math.abs(lfoValue)));
			
			// store the current sample in the delay buffer;
			if (writePosition >= flangerBuffer.length) {
				writePosition = 0;
			}
			flangerBuffer[writePosition] = audioFloatBuffer[i];

			// find out the position to read the delayed sample:
			int readPosition = writePosition - delay;
			if (readPosition < 0) {
				readPosition += flangerBuffer.length;
			}

			//increment the write position
			writePosition++;

			// Output is the input summed with the value at the delayed flanger
			// buffer
			audioFloatBuffer[i] = dry * audioFloatBuffer[i] + wet * flangerBuffer[readPosition];
		}
		return true;
	}

	@Override
	public void processingFinished() {
	}

	/**
	 * Set the new length of the delay line.
	 * 
	 * @param flangerLength
	 *            The new length of the delay line, in seconds.
	 */
	public void setFlangerLength(double flangerLength) {
		flangerBuffer = new float[(int) (sampleRate * flangerLength)];
	}

	/**
	 * Sets the frequency of the LFO (sine wave), in Hertz.
	 * 
	 * @param lfoFrequency
	 *            The new LFO frequency in Hertz.
	 */
	public void setLFOFrequency(double lfoFrequency) {
		this.lfoFrequency = lfoFrequency;
	}

	/**
	 * Sets the wetness and dryness of the effect. Should be a value between
	 * zero and one (inclusive), the dryness is determined by 1-wet.
	 * 
	 * @param wet
	 *            A value between zero and one (inclusive) that determines the
	 *            wet and dryness of the resulting mix.
	 */
	public void setWet(double wet) {
		this.wet = (float) wet;
		this.dry = (float) (1 - wet);
	}

	/**
	 * Sets the wetness and wetness of the effect. Should be a value between
	 * zero and one (inclusive), the wetness is determined by 1-dry.
	 * 
	 * @param dry
	 *            A value between zero and one (inclusive) that determines the
	 *            wet and dryness of the resulting mix.
	 */
	public void setDry(double dry) {
		this.dry = (float) dry;
		this.wet = (float) (1 - dry);
	}
}
