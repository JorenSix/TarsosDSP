package be.hogent.tarsos.dsp.test;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioPlayer;
import be.hogent.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.hogent.tarsos.dsp.resample.RateTransposer;

public class RateTransposerTest {
	@Test
	public void testTransposeSine() throws UnsupportedAudioFileException, LineUnavailableException, IOException{
		float[] audioBuffer = TestUtilities.audioBufferSine();
		double factor = 1.2;
		int sampleRate = 44100;
		WaveformSimilarityBasedOverlapAdd w = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(1.2, sampleRate));
		final AudioDispatcher d = AudioDispatcher.fromFloatArray(audioBuffer, sampleRate, w.getInputBufferSize(),w.getOverlap());
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
		AudioDispatcher d = AudioDispatcher.fromFile(TestUtilities.fluteFile(),w.getInputBufferSize(),w.getOverlap());
		AudioFormat f = new AudioFormat(sampleRate,16,1,true,false);
		w.setDispatcher(d);
		d.addAudioProcessor(w);
		d.addAudioProcessor(new RateTransposer(factor));
		d.addAudioProcessor(new AudioPlayer(f));
		d.run();
	}
}
