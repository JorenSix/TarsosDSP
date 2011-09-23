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
import be.hogent.tarsos.dsp.pitch.Goertzel;
import be.hogent.tarsos.dsp.pitch.Goertzel.FrequencyDetectedHandler;
import be.hogent.tarsos.dsp.util.AudioFloatConverter;

public class GoertzelTest {
	
	public static float[] testAudioBufferSine(final double frequency,int size) {
		final double sampleRate = 44100.0;
		final double f0 = frequency;
		final double amplitudeF0 = 1.0;
		final float[] buffer = new float[size];
		for (int sample = 0; sample < buffer.length; sample++) {
			final double time = sample / sampleRate;
			buffer[sample] = (float) (amplitudeF0 * Math.sin(2 * Math.PI * f0
					* time));
		}
		return buffer;
	}
	
	@Test
	public void testDetection() throws LineUnavailableException, UnsupportedAudioFileException{
		
		final float[][] floatSinBuffer = {testAudioBufferSine(6000,10240),testAudioBufferSine(5000,10240),testAudioBufferSine(4000,10240)};

		int size = 0;
		for(int i = 0 ; i < floatSinBuffer.length ; i ++){
			size += floatSinBuffer[i].length;
		}
		
		
		final float[] floatBuffer = new float[size];
		
		
		for(int i = 0 ; i < floatSinBuffer.length ; i ++){
			int index = 0;
			for(int j = 0 ; j < floatSinBuffer[i].length ; j++){
				floatBuffer[index] = floatSinBuffer[i][j];
				index++;
			}
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
		
		float[] frequencies = {6000,4000,3000};
		
        dispatcher.addAudioProcessor(new Goertzel(44100,1024,frequencies,new FrequencyDetectedHandler() {
			@Override
			public void detectedFrequency(double frequency) {
				System.out.println(frequency + " detected");
			}
		}));
        dispatcher.addAudioProcessor(new BlockingAudioPlayer(format,1024, 0));
        dispatcher.run();
	}

}
