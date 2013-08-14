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

import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.mfcc.MFCC;

public class MFCCTest {
	
//	private static int counter = 0;
	
	@Test
	public void MFCCForSineTest() throws UnsupportedAudioFileException{
		int sampleRate = 44100;
		int bufferSize = 1024;
		int bufferOverlap = 128;
		final float[] floatBuffer = TestUtilities.audioBufferSine();
		final AudioDispatcher dispatcher = AudioDispatcher.fromFloatArray(floatBuffer, sampleRate, bufferSize, bufferOverlap);
		final MFCC mfcc = new MFCC(bufferSize, sampleRate, 40, 50, 300, 3000);
		dispatcher.addAudioProcessor(mfcc);
		dispatcher.addAudioProcessor(new AudioProcessor() {
			
			@Override
			public void processingFinished() {
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				return true;
			}
		});
		dispatcher.run();
	}

}
