package be.tarsos.dsp.test;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.PitchShifter;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;

public class PitchShifterTest {
	
	@Test
	public void testPitchShiftSine() throws UnsupportedAudioFileException, LineUnavailableException, IOException{
		float[] audioBuffer = TestUtilities.audioBufferSine();
		double factor = 1.35;
		int sampleRate = 44100;
	
		final AudioDispatcher d = AudioDispatcherFactory.fromFloatArray(audioBuffer, sampleRate, 1024,1024-32);
		d.setZeroPadLastBuffer(true);
		PitchShifter w = new PitchShifter(factor,sampleRate,1024,1024-32);
		AudioFormat f = new AudioFormat(sampleRate,16,1,true,false);
		d.addAudioProcessor(w);
		
		d.addAudioProcessor(new AudioPlayer(f));
		d.run();
	}

}
