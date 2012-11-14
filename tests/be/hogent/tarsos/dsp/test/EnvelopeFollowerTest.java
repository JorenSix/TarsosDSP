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
import be.hogent.tarsos.dsp.EnvelopeFollower;

public class EnvelopeFollowerTest {
	
	@Test
	public void testFollower() throws UnsupportedAudioFileException{
		final float[] sine = TestUtilities.audioBufferFlute();

		EnvelopeFollower follower = new EnvelopeFollower(44100);
		
		AudioDispatcher dispatcher = AudioDispatcher.fromFloatArray(sine, 44100, 1024, 0);
		
		
		dispatcher.addAudioProcessor(follower);
		dispatcher.addAudioProcessor(new AudioProcessor() {
			int counter = 0;
			@Override
			public boolean process(AudioEvent audioEvent) {
				float buffer[] = audioEvent.getFloatBuffer();
				for(int i = 0 ; i < buffer.length ; i++){
					System.out.println(buffer[i] + ";" + sine[counter++]);
				}
				return true;
			}
			
			@Override
			public void processingFinished() {
				// TODO Auto-generated method stub
				
			}
		});
		dispatcher.run();
	}

}
