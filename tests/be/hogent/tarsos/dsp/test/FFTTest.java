/**
*
*  TarsosDSP is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*
**/
package be.hogent.tarsos.dsp.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;



import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.BlockingAudioPlayer;
import be.hogent.tarsos.dsp.FloatConverter;
import be.hogent.tarsos.dsp.util.AudioFloatConverter;
import be.hogent.tarsos.dsp.util.FFT;

public class FFTTest {
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
	
	@Test
	public void testForwardAndBackwardsFFT() throws UnsupportedAudioFileException, LineUnavailableException{		
		  final float[] floatBuffer = testAudioBufferSine();
	      final AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
	      final AudioFloatConverter converter =  AudioFloatConverter.getConverter(format);
	      final byte[] byteBuffer = new byte[floatBuffer.length * format.getFrameSize()];
	      assertEquals("Specified 16 bits so framesize should be 2.", 2, format.getFrameSize());
	      converter.toByteArray(floatBuffer, byteBuffer);
	      final ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
          final AudioInputStream inputStream = new AudioInputStream(bais, format,floatBuffer.length);
          final AudioDispatcher dispatcher = new AudioDispatcher(inputStream, 1024, 0);
          dispatcher.addAudioProcessor(new AudioProcessor() {
			private FFT  fft = new FFT(512);
			@Override
			public void processingFinished() {
			}
			
			@Override
			public boolean processOverlapping(float[] audioFloatBuffer,
					byte[] audioByteBuffer) {
				return processFull(audioFloatBuffer, audioByteBuffer);				
			}
			
			@Override
			public boolean processFull(float[] audioFloatBuffer, byte[] audioByteBuffer) {
				fft.forwardTransform(audioFloatBuffer);
				fft.backwardsTransform(audioFloatBuffer);
				return true;
			}
		});
        dispatcher.addAudioProcessor(new FloatConverter(format));
        dispatcher.addAudioProcessor(new BlockingAudioPlayer(format,1024, 0));
        dispatcher.run();
	}
}
