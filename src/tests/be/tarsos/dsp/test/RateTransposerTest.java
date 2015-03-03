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

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.resample.RateTransposer;

public class RateTransposerTest {
	@Test
	public void testTransposeSine() throws UnsupportedAudioFileException, LineUnavailableException, IOException{
		float[] audioBuffer = TestUtilities.audioBufferSine();
		double factor = 1.2;
		int sampleRate = 44100;
		WaveformSimilarityBasedOverlapAdd w = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(1.2, sampleRate));
		final AudioDispatcher d = AudioDispatcherFactory.fromFloatArray(audioBuffer, sampleRate, w.getInputBufferSize(),w.getOverlap());
		AudioFormat f = new AudioFormat(sampleRate,16,1,true,false);
		w.setDispatcher(d);
		d.addAudioProcessor(w);
		d.addAudioProcessor(new RateTransposer(factor));
		d.addAudioProcessor(new AudioPlayer(f));
		d.run();
	}
	
	@Test
	public void testTransposeFlute() throws LineUnavailableException, UnsupportedAudioFileException, IOException{
		double factor = 1.2;
		int sampleRate = 44100;
		WaveformSimilarityBasedOverlapAdd w = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(1.2, sampleRate));
		AudioDispatcher d = AudioDispatcherFactory.fromFile(TestUtilities.fluteFile(),w.getInputBufferSize(),w.getOverlap());
		AudioFormat f = new AudioFormat(sampleRate,16,1,true,false);
		w.setDispatcher(d);
		d.addAudioProcessor(w);
		d.addAudioProcessor(new RateTransposer(factor));
		d.addAudioProcessor(new AudioPlayer(f));
		d.run();
	}
}
