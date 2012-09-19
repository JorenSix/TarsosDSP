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
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*  https://github.com/JorenSix/TarsosDSP
*  http://tarsos.0110.be/releases/TarsosDSP/
* 
*/

package be.hogent.tarsos.dsp.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioPlayer;
import be.hogent.tarsos.dsp.SilenceDetector;
import be.hogent.tarsos.dsp.util.AudioFloatConverter;

public class SilenceTest {
	
	@Test
	public void testSilenceDetector() throws UnsupportedAudioFileException, LineUnavailableException{
		final float[] floatSinBuffer = TestUtilities.audioBufferSine();
		final float[] floatSilenceBuffer = TestUtilities.audioBufferSilence();
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
        dispatcher.addAudioProcessor(new AudioPlayer(format));
        dispatcher.run();
	}

}
