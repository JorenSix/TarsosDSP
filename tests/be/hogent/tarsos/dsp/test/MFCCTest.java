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

import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.mfcc.MFCC;

public class MFCCTest {
	
	@Test
	public void MFCCForSineTest() throws UnsupportedAudioFileException{
		int sampleRate = 44100;
		int bufferSize = 1024;
		int bufferOverlap = 0;
		final float[] floatBuffer = TestUtilities.audioBufferSine();
		final AudioDispatcher dispatcher = AudioDispatcher.fromFloatArray(floatBuffer, sampleRate, bufferSize, bufferOverlap);
		final MFCC mfcc = new MFCC(bufferSize, sampleRate, bufferOverlap);
		dispatcher.addAudioProcessor(mfcc);
		dispatcher.addAudioProcessor(new AudioProcessor() {
			
			@Override
			public void processingFinished() {
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				System.out.println(mfcc.getMFCC()[0]);
				return true;
			}
		});
		dispatcher.run();
	}

}
