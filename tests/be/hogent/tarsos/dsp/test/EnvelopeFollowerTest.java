/*
*
*  TarsosDSP is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
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
