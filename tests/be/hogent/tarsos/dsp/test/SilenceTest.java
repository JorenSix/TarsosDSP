package be.hogent.tarsos.dsp.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.BlockingAudioPlayer;
import be.hogent.tarsos.dsp.SilenceDetector;
import be.hogent.tarsos.dsp.util.AudioFloatConverter;

public class SilenceTest {
	
	/**
	 * Constructs and returns a buffer of a two seconds long pure sine of 440Hz
	 * sampled at 44.1kHz.
	 * 
	 * @return A buffer of a two seconds long pure sine (440Hz) sampled at
	 *         44.1kHz.
	 */
	public static float[] testAudioBufferSine() {
		final double sampleRate = 44100.0;
		final double f0 = 440.0;
		final double amplitudeF0 = 0.5;
		final double seconds = 2.0;
		final float[] buffer = new float[(int) (seconds * sampleRate)];
		for (int sample = 0; sample < buffer.length; sample++) {
			final double time = sample / sampleRate;
			buffer[sample] = (float) (amplitudeF0 * Math.sin(2 * Math.PI * f0 * time));
		}
		return buffer;
	}
	
	public static float[] testAudioBufferSilence() {
		final double sampleRate = 44100.0;
		final double seconds = 0.5;
		final float[] buffer = new float[(int) (seconds * sampleRate)];
		return buffer;
	}
	
	@Test
	public void testSilenceDetector() throws UnsupportedAudioFileException, LineUnavailableException{
		final float[] floatSinBuffer = testAudioBufferSine();
		final float[] floatSilenceBuffer = testAudioBufferSilence();
		final float[] floatBuffer = new float[floatSinBuffer.length+(2 * floatSilenceBuffer.length)];
		int i = floatSilenceBuffer.length;
		for(;i<floatSilenceBuffer.length + floatSinBuffer.length;i++){
			floatBuffer[i]=floatSinBuffer[i-floatSilenceBuffer.length];
		}
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
		
        dispatcher.addAudioProcessor(new SilenceDetector());
        dispatcher.addAudioProcessor(new BlockingAudioPlayer(format,1024, 0));
        dispatcher.run();
	}

}
