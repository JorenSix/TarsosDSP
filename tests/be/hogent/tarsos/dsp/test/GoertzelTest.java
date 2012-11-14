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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.pitch.DTMF;
import be.hogent.tarsos.dsp.pitch.Goertzel;
import be.hogent.tarsos.dsp.pitch.Goertzel.FrequenciesDetectedHandler;
import be.hogent.tarsos.dsp.util.AudioFloatConverter;

public class GoertzelTest {
	
	/**
	 * Generate a buffer with one sine wave.
	 * 
	 * @param f0
	 *            the frequency of the sine wave;
	 * @param size
	 *            the size of the buffer in samples.
	 * @return a buffer (float array) with audio information for the sine wave.
	 */
	public static float[] testAudioBufferSine(final double f0, int size) {
		final double sampleRate = 44100.0;
		final double amplitudeF0 = 1.0;
		final float[] buffer = new float[size];
		for (int sample = 0; sample < buffer.length; sample++) {
			final double time = sample / sampleRate;
			buffer[sample] = (float) (amplitudeF0 * Math.sin(2 * Math.PI * f0
					* time));
		}
		return buffer;
	}

	/**
	 * Append float buffers to form one big float buffer.
	 * 
	 * @param floatBuffers
	 *            The float buffers to append.
	 * @return An appended float buffer with all the information in the array of
	 *         buffers.
	 */
	public static float[] appendBuffers(final float[]...floatBuffers){
		int size = 0;
		for(int i = 0 ; i < floatBuffers.length ; i ++){
			size += floatBuffers[i].length;
		}
		final float[] floatBuffer = new float[size];
		int index = 0;
		for(int i = 0 ; i < floatBuffers.length; i ++){			
			for(int j = 0 ; j < floatBuffers[i].length ; j++){
				floatBuffer[index] = floatBuffers[i][j];
				index++;
			}
		}
		return floatBuffer;
	}
	
	
	/**
	 * Test detection of a simple sine wave (one frequency).
	 * @throws LineUnavailableException
	 * @throws UnsupportedAudioFileException
	 */
	@Test
	public void testDetection() throws LineUnavailableException, UnsupportedAudioFileException{
		
		final float[][] floatSinBuffers = {testAudioBufferSine(6000,10240),testAudioBufferSine(2000,10240),testAudioBufferSine(4000,10240)};
		final float[] floatBuffer = appendBuffers(floatSinBuffers);
		final AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
		final AudioFloatConverter converter = AudioFloatConverter.getConverter(format);
		final byte[] byteBuffer = new byte[floatBuffer.length * format.getFrameSize()];
		assertEquals("Specified 16 bits so framesize should be 2.", 2, format.getFrameSize());
		converter.toByteArray(floatBuffer, byteBuffer);
		final ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
		final AudioInputStream inputStream = new AudioInputStream(bais, format,floatBuffer.length);
		final AudioDispatcher dispatcher = new AudioDispatcher(inputStream, 1024, 0);
		
		double[] frequencies = {6000,3000,5000,5800,6500};
		
        dispatcher.addAudioProcessor(new Goertzel(44100,1024,frequencies,new FrequenciesDetectedHandler() {
			@Override
			public void handleDetectedFrequencies(final double[] frequencies, final double[] powers, final double[] allFrequencies, final double allPowers[]) {
				assertEquals("Should only detect 6000 Hz",(int)frequencies[0],6000);
			}
		}));
        //dispatcher.addAudioProcessor(new BlockingAudioPlayer(format,1024, 0));
        dispatcher.run();
	}
	
	/**
	 * Test detection of multiple frequencies.
	 * @throws LineUnavailableException
	 * @throws UnsupportedAudioFileException
	 */
	@Test
	public void testDTMF() throws LineUnavailableException, UnsupportedAudioFileException{
		
		final float[][] floatSinBuffers = {DTMF.generateDTMFTone('1'),DTMF.generateDTMFTone('2'),DTMF.generateDTMFTone('3'),DTMF.generateDTMFTone('4'),DTMF.generateDTMFTone('5'),DTMF.generateDTMFTone('6'),DTMF.generateDTMFTone('7'),DTMF.generateDTMFTone('8'),DTMF.generateDTMFTone('9')};
		final float[] floatBuffer = appendBuffers(floatSinBuffers);
		final int stepSize = 512;
		final AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
		final AudioFloatConverter converter = AudioFloatConverter.getConverter(format);
		final byte[] byteBuffer = new byte[floatBuffer.length * format.getFrameSize()];
		assertEquals("Specified 16 bits so framesize should be 2.", 2, format.getFrameSize());
		converter.toByteArray(floatBuffer, byteBuffer);
		final ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
		final AudioInputStream inputStream = new AudioInputStream(bais, format,floatBuffer.length);
		final AudioDispatcher dispatcher = new AudioDispatcher(inputStream, stepSize, 0);
		final StringBuilder data = new StringBuilder();
		dispatcher.addAudioProcessor(new Goertzel(44100, stepSize,
				DTMF.DTMF_FREQUENCIES, new FrequenciesDetectedHandler() {
					
					@Override
					public void handleDetectedFrequencies(
							final double[] frequencies, final double[] powers, final double[] allFrequencies, final double allPowers[]) {
						// assertEquals("Should detect 2 frequencies.",2,frequencies.length);
						assertEquals(
								"Number of frequencies should be the same as the number of powers.",
								frequencies.length, powers.length);
						if (frequencies.length == 2) {
							int rowIndex = -1;
							int colIndex = -1;
							for (int i = 0; i < 4; i++) {
								if (frequencies[0] == DTMF.DTMF_FREQUENCIES[i] || frequencies[1] == DTMF.DTMF_FREQUENCIES[i])
									rowIndex = i;
							}
							for (int i = 4; i < DTMF.DTMF_FREQUENCIES.length; i++) {
								if (frequencies[0] == DTMF.DTMF_FREQUENCIES[i] || frequencies[1] == DTMF.DTMF_FREQUENCIES[i])
									colIndex = i-4;
							}
							if(rowIndex>=0 && colIndex>=0){
								char character = DTMF.DTMF_CHARACTERS[rowIndex][colIndex];
								if(data.length()==0 || character != data.charAt(data.length()-1)){
									data.append(character);
								}
							}
						}
					}
				}));
		//dispatcher.addAudioProcessor(new BlockingAudioPlayer(format, stepSize, 0));
		dispatcher.run();
		assertEquals("Decoded string should be 123456789", "123456789", data.toString());
		assertEquals("Length should be 9", 9, data.length());
		
	}

}
