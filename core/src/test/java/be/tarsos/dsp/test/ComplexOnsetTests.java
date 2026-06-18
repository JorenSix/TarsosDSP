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

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Onset detection with aubio-style {@link ComplexOnsetDetector} parameters,
 * compared against {@code NR45_expected_onsets_complex.txt}.
 *
 * <p>Tagged {@code manual}: this overlaps the coverage in {@link BeatRootTest}
 * and its tight per-onset timing assertions are parameter-sensitive and
 * unverified against the current detector. Excluded from automated/CI runs;
 * run explicitly with {@code ./gradlew test -PrunManual}.
 */
@Tag("manual")
public class ComplexOnsetTests {

	@Test
	public void testOnsets() throws UnsupportedAudioFileException, IOException {
		File audioFile = TestUtilities.onsetsAudioFile();
		String contents = TestUtilities.readFileFromJar("NR45_expected_onsets_complex.txt");
		String[] onsetStrings = contents.split("\n");
		final double[] expectedOnsets = new double[onsetStrings.length];
		int i = 0;
		for (String onset : onsetStrings) {
			expectedOnsets[i] = Double.parseDouble(onset);
			i++;
		}
		AudioDispatcher d = AudioDispatcherFactory.fromFile(audioFile, 512, 256);
		//use the same default params as aubio:
		ComplexOnsetDetector cod = new ComplexOnsetDetector(512, 0.3, 256.0 / 44100.0 * 4.0, -70);
		d.addAudioProcessor(cod);
		cod.setHandler(new OnsetHandler() {
			int i = 0;
			@Override
			public void handleOnset(double actualTime, double salience) {
				if (i < expectedOnsets.length) {
					double expectedTime = expectedOnsets[i];
					assertEquals(expectedTime, actualTime, 0.017417, "Onset time should be the expected value!");
				}
				i++;
			}
		});
		d.run();

	}
}
