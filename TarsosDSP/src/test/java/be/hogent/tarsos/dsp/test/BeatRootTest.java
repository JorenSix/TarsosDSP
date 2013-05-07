package be.hogent.tarsos.dsp.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.beatroot.BeatRootProcessor;
import be.hogent.tarsos.dsp.beatroot.BeatRootProcessor.OnsetHandler;

public class BeatRootTest {
	@Test
	public void testExpectedOnsets() throws UnsupportedAudioFileException, IOException{
		File audioFile = TestUtilities.onsetsAudioFile();
		String contents = TestUtilities.readFileFromJar("/be/hogent/tarsos/dsp/test/resources/NR45_expected_onsets.txt");
		String[] onsetStrings = contents.split("\n");
		final double[] expectedOnsets = new double[onsetStrings.length];
		int i = 0;
		for(String onset : onsetStrings){
			expectedOnsets[i] = Double.parseDouble(onset);
			i++;
		}
		
		AudioDispatcher d = AudioDispatcher.fromFile(audioFile, 2048, 2048-441);
		d.setZeroPad(true);
		BeatRootProcessor b = new BeatRootProcessor(d, 2048,441);
		b.setHandler(new OnsetHandler(){
			int i = 0;
			@Override
			public void handleOnset(double actualTime, double salience) {
				double expectedTime = expectedOnsets[i];
				assertEquals("Onset time should be the expected value!",expectedTime,actualTime,0.00001);
				i++;
			}
			
		});
		
		d.addAudioProcessor(b);
		d.run();
	}
}
