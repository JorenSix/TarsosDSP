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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;

public class ComplexOnsetTests {

	@Test
	public void testOnsets() throws UnsupportedAudioFileException, IOException{
		File audioFile = TestUtilities.onsetsAudioFile();
		String contents = TestUtilities.readFileFromJar("/be/tarsos/dsp/test/resources/NR45_expected_onsets_complex.txt");
		String[] onsetStrings = contents.split("\n");
		final double[] expectedOnsets = new double[onsetStrings.length];
		int i = 0;
		for(String onset : onsetStrings){
			expectedOnsets[i] = Double.parseDouble(onset);
			i++;
		}
		AudioDispatcher d = AudioDispatcherFactory.fromFile(audioFile, 512, 256);
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
