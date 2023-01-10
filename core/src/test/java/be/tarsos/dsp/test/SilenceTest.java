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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;


import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SilenceTest {
	
	@Test
	public void testSilenceDetector() {
		//4 seconds of 440Hz
		final float[] floatSinBuffer = TestUtilities.audioBufferSine();
		//half a second of silence
		final float[] floatSilenceBuffer = TestUtilities.audioBufferSilence();
		final float[] floatBuffer = new float[floatSinBuffer.length+(2 * floatSilenceBuffer.length)];
		int i = floatSilenceBuffer.length;
		for(;i<floatSilenceBuffer.length + floatSinBuffer.length;i++){
			floatBuffer[i]=floatSinBuffer[i-floatSilenceBuffer.length];
		}

		final AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
		final TarsosDSPAudioFloatConverter converter = TarsosDSPAudioFloatConverter
				.getConverter(JVMAudioInputStream.toTarsosDSPFormat(format));
		final byte[] byteBuffer = new byte[floatBuffer.length
				* format.getFrameSize()];
		converter.toByteArray(floatBuffer, byteBuffer);
		final ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
		final AudioInputStream inputStream = new AudioInputStream(bais, format,
				floatBuffer.length);
		JVMAudioInputStream stream = new JVMAudioInputStream(inputStream);
		final AudioDispatcher dispatcher = new AudioDispatcher(stream,
				512, 0);
		final Double[] startTime = {556.};
		//stops on silence
		final SilenceDetector detector = new SilenceDetector(-50,true);
        dispatcher.addAudioProcessor(detector);
		dispatcher.addAudioProcessor(new AudioProcessor() {
			@Override
			public boolean process(AudioEvent audioEvent) {
				//this code is not executed when the input is silent
				startTime[0] = Math.min(startTime[0] , audioEvent.getTimeStamp());
				return true;
			}

			@Override
			public void processingFinished() {

			}
		});
        dispatcher.run();

		assertEquals(floatSilenceBuffer.length/44100.0,startTime[0],0.01);
	}

}
