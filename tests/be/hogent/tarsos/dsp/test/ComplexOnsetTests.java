package be.hogent.tarsos.dsp.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.hogent.tarsos.dsp.onsets.OnsetHandler;

public class ComplexOnsetTests {

	@Test
	public void testOnsets() throws UnsupportedAudioFileException, IOException{
		File audioFile = TestUtilities.onsetsAudioFile();
		String contents = TestUtilities.readFileFromJar("/be/hogent/tarsos/dsp/test/resources/NR45_expected_onsets_complex.txt");
		String[] onsetStrings = contents.split("\n");
		final double[] expectedOnsets = new double[onsetStrings.length];
		int i = 0;
		for(String onset : onsetStrings){
			expectedOnsets[i] = Double.parseDouble(onset);
			i++;
		}
		AudioDispatcher d = AudioDispatcher.fromFile(audioFile, 512, 256);
		//use the same default params as aubio: 
		ComplexOnsetDetector cod = new ComplexOnsetDetector(512, 0.3,256.0/44100.0*4.0,-70);
		d.addAudioProcessor(cod);
		cod.setHandler(new OnsetHandler() {
			int i = 1;
			@Override
			public void handleOnset(double actualTime, double salience) {
				double expectedTime = expectedOnsets[i];
				System.out.println(actualTime);
				assertEquals("Onset time should be the expected value!",expectedTime,actualTime,0.017417);
				i++;
			}
		});
		d.run();
		
	}
}
