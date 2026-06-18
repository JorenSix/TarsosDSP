/*
*      _______                       _____   _____ _____
*     |__   __|                     |  __ \ / ____|  __ \
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|
*
* -------------------------------------------------------------
*
* TarsosDSP is developed by Joren Six at IPEM, University Ghent
*
* -------------------------------------------------------------
*
*  Info: http://0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://0110.be/releases/TarsosDSP/
*
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
*
*/


package be.tarsos.dsp.test;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.jupiter.api.Test;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.EnvelopeFollower;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless smoke test for {@link EnvelopeFollower}: runs the follower over a
 * real flute buffer in-memory (no audio hardware) and asserts the envelope is
 * produced and stays within a sane range.
 */
public class EnvelopeFollowerTest {

	@Test
	public void testFollower() throws UnsupportedAudioFileException {
		final float[] sine = TestUtilities.audioBufferFlute();

		EnvelopeFollower follower = new EnvelopeFollower(44100);

		AudioDispatcher dispatcher = AudioDispatcherFactory.fromFloatArray(sine, 44100, 1024, 0);

		final float[] maxEnvelope = {0f};
		final int[] sampleCount = {0};

		dispatcher.addAudioProcessor(follower);
		dispatcher.addAudioProcessor(new AudioProcessor() {
			@Override
			public boolean process(AudioEvent audioEvent) {
				float[] buffer = audioEvent.getFloatBuffer();
				for (float v : buffer) {
					// The envelope is a non-negative magnitude.
					assertTrue(v >= 0f, "Envelope value should be non-negative");
					maxEnvelope[0] = Math.max(maxEnvelope[0], v);
					sampleCount[0]++;
				}
				return true;
			}

			@Override
			public void processingFinished() {

			}
		});
		dispatcher.run();

		assertTrue(sampleCount[0] > 0, "Envelope follower should have processed samples");
		assertTrue(maxEnvelope[0] > 0f, "A non-silent signal should yield a positive envelope");
	}

}
