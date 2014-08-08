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

package be.tarsos.dsp.test;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioFile;
import be.tarsos.dsp.AudioPlayer;

public class AudioPlayerTest {
	
	@Test
	public void testAudioPlayer() throws UnsupportedAudioFileException, LineUnavailableException{
		float[] sine = TestUtilities.audioBufferSine();
		AudioDispatcher dispatcher = AudioDispatcher.fromFloatArray(sine, 44100, 1024, 512);
		dispatcher.addAudioProcessor(new AudioPlayer(dispatcher.getFormat()));	
		dispatcher.run();
	}
	
	@Test
	public void testStreamAudioPlayer() throws UnsupportedAudioFileException, LineUnavailableException{
		AudioFile file = new AudioFile("http://mp3.streampower.be/stubru-high.mp3");
		AudioInputStream stream = file.getMonoStream(44100);
		AudioDispatcher d;
		d = new AudioDispatcher(stream, 1024, 0);
	    //d.addAudioProcessor(new HaarWaveletCoder());
	    //d.addAudioProcessor(new HaarWaveletDecoder());
	    d.addAudioProcessor(new AudioPlayer(d.getFormat()));
	    d.run();
	}

}
