package be.hogent.tarsos.dsp.test;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.hogent.tarsos.dsp.NewAudioDispatcher;
import be.hogent.tarsos.dsp.NewAudioPlayer;

public class AudioPlayerTest {
	
	@Test
	public void testAudioPlayer() throws UnsupportedAudioFileException, LineUnavailableException{
		float[] sine = TestUtilities.audioBufferSine();
		NewAudioDispatcher dispatcher = NewAudioDispatcher.fromFloatArray(sine, 44100, 1024, 512);
		dispatcher.addAudioProcessor(new NewAudioPlayer(dispatcher.getFormat()));	
		dispatcher.run();
	}

}
