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

package be.hogent.tarsos.dsp.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioPlayer;
import be.hogent.tarsos.dsp.WaveformWriter;
import be.hogent.tarsos.dsp.filters.HighPass;
import be.hogent.tarsos.dsp.filters.LowPassFS;
import be.hogent.tarsos.dsp.util.AudioFloatConverter;

public class TestFilters {

	public static float[] testAudioBufferSine() {
		final double sampleRate = 44100.0;
		final double f0 = 440.0;
		final double amplitudeF0 = 0.5;
		final double seconds = 2.0;
		final float[] buffer = new float[(int) (seconds * sampleRate)];
		for (int sample = 0; sample < buffer.length; sample++) {
			final double time = sample / sampleRate;
			buffer[sample] = (float) (amplitudeF0 * Math.sin(2 * Math.PI * f0
					* time));
		}
		return buffer;
	}

	public void testFilters() throws UnsupportedAudioFileException,
			LineUnavailableException {
		final float[] floatBuffer = testAudioBufferSine();
		final AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
		final AudioFloatConverter converter = AudioFloatConverter
				.getConverter(format);
		final byte[] byteBuffer = new byte[floatBuffer.length
				* format.getFrameSize()];
		assertEquals("Specified 16 bits so framesize should be 2.", 2,
				format.getFrameSize());
		converter.toByteArray(floatBuffer, byteBuffer);
		final ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
		final AudioInputStream inputStream = new AudioInputStream(bais, format,
				floatBuffer.length);
		final AudioDispatcher dispatcher = new AudioDispatcher(inputStream,
				1024, 0);
		dispatcher.addAudioProcessor(new LowPassFS(1000, 44100));
		dispatcher.addAudioProcessor(new HighPass(100, 44100));
		dispatcher.addAudioProcessor(new AudioPlayer(format));
		dispatcher.run();
	}

	@Test
	public void testFilterOnFile() throws UnsupportedAudioFileException,
			LineUnavailableException, IOException {
		File testFile = TestUtilities.fluteFile();
		
		AudioFormat format = AudioSystem.getAudioFileFormat(testFile).getFormat();
		int stepSize = 2048;//samples
		int overlap = 0; //samples;
		float sampleRate = format.getSampleRate();
		float startFrequency = 200;
		float stopFrequency = 800;
		AudioInputStream inputStream = AudioSystem.getAudioInputStream(testFile);
		AudioDispatcher dispatcher = new AudioDispatcher(inputStream,stepSize,overlap);
		dispatcher.addAudioProcessor(new HighPass(startFrequency, sampleRate));
		dispatcher.addAudioProcessor(new LowPassFS(stopFrequency, sampleRate));
		dispatcher.addAudioProcessor(new WaveformWriter(format, "filtered.wav"));
		dispatcher.run();
	}

}
