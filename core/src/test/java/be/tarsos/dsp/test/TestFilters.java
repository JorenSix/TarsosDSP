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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.filters.HighPass;
import be.tarsos.dsp.filters.LowPassFS;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.jvm.WaveformWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestFilters {

	@Test
	public void testFilters() {
		//440Hz sample
		final float[] floatBuffer = TestUtilities.audioBufferSine();
		final TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(44100, 16, 1, true, false);
		final TarsosDSPAudioFloatConverter converter = TarsosDSPAudioFloatConverter.getConverter(format);
		final byte[] byteBuffer = new byte[floatBuffer.length * format.getFrameSize()];
		converter.toByteArray(floatBuffer, byteBuffer);
		final ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
		final AudioInputStream inputStream = new AudioInputStream(bais, JVMAudioInputStream.toAudioFormat(format), floatBuffer.length);
		JVMAudioInputStream jvmAudioInputStream = new JVMAudioInputStream(inputStream);
		final AudioDispatcher dispatcher = new AudioDispatcher(jvmAudioInputStream, 1024, 0);

		//Filter the signal away
		dispatcher.addAudioProcessor(new LowPassFS(110, 44100));
		dispatcher.addAudioProcessor(new HighPass(1760, 44100));

		//test if it is filtered away
		final SilenceDetector detector = new SilenceDetector(-25,false);
		dispatcher.addAudioProcessor(new AudioProcessor() {
			@Override
			public boolean process(AudioEvent audioEvent) {
				assertTrue(detector.isSilence(audioEvent.getFloatBuffer()));
				return true;
			}

			@Override
			public void processingFinished() {

			}
		});
		dispatcher.run();
	}

	@Test
	public void testFilterOnFile() throws UnsupportedAudioFileException, IOException {
		File testFile = TestUtilities.fluteFile();
		AudioFormat format = AudioSystem.getAudioFileFormat(testFile).getFormat();
		int stepSize = 2048;//samples
		int overlap = 0; //samples;
		float sampleRate = format.getSampleRate();
		float startFrequency = 200;
		float stopFrequency = 800;
		AudioInputStream inputStream = AudioSystem.getAudioInputStream(testFile);
		JVMAudioInputStream jvmAudioInputStream = new JVMAudioInputStream(inputStream);
		AudioDispatcher dispatcher = new AudioDispatcher(jvmAudioInputStream,stepSize,overlap);
		dispatcher.addAudioProcessor(new HighPass(startFrequency, sampleRate));
		dispatcher.addAudioProcessor(new LowPassFS(stopFrequency, sampleRate));
		dispatcher.addAudioProcessor(new WaveformWriter(format, "filtered.wav"));
		dispatcher.run();

		File f = new File("filtered.wav");
		f.delete();
	}

}
