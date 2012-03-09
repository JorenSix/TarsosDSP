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

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioPlayer;

public class AudioPlayerTest {
	
	@Test
	public void testAudioPlayer() throws UnsupportedAudioFileException, LineUnavailableException{
		float[] sine = TestUtilities.audioBufferSine();
		AudioDispatcher dispatcher = AudioDispatcher.fromFloatArray(sine, 44100, 1024, 512);
		dispatcher.addAudioProcessor(new AudioPlayer(dispatcher.getFormat()));	
		dispatcher.run();
	}

}
