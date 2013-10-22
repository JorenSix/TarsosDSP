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

package be.hogent.tarsos.dsp.test;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.onsets.PercussionOnsetDetector;
import be.hogent.tarsos.dsp.onsets.PrintOnsetHandler;
import be.hogent.tarsos.dsp.util.Shared;

public class PercussionOnsetTest {
	

	public static void main(String[] args) throws LineUnavailableException, UnsupportedAudioFileException{
		
		float sampleRate = 44100;
		int bufferSize = 1024;
		int overlap = 0;
		
		//available mixers
		int index = 0;
		int selectedMixerIndex = 3;
		for(Mixer.Info mixer : AudioSystem.getMixerInfo()){
			System.out.println(index + ": " + Shared.toLocalString(mixer));
			index++;
		}
		Mixer.Info selectedMixer = AudioSystem.getMixerInfo()[selectedMixerIndex];
		System.out.println("Selected mixer: " + Shared.toLocalString(selectedMixer));
		
		//open a line
		final Mixer mixer = AudioSystem.getMixer(selectedMixer);		
		final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
		final DataLine.Info dataLineInfo = new DataLine.Info(
				TargetDataLine.class, format);
		TargetDataLine line;
		line = (TargetDataLine) mixer.getLine(dataLineInfo);
		final int numberOfSamples = bufferSize;
		line.open(format, numberOfSamples);
		line.start();		
		final AudioInputStream stream = new AudioInputStream(line);
		
		//create a new dispatcher
		AudioDispatcher dispatcher = new AudioDispatcher(stream, bufferSize, overlap);
		
		//add a processor, handle percussion event.
		dispatcher.addAudioProcessor(new PercussionOnsetDetector(sampleRate, bufferSize,overlap, new PrintOnsetHandler()));
		
		//run the dispatcher (on the same thread, use start() to run it on another thread). 
		dispatcher.run();
	}
	
	@Test
	public void testOnset() throws UnsupportedAudioFileException, IOException{
		/*
		String file = "/home/joren/Desktop/Fingerprinting/07. Pleasant Shadow Song_original.wav.semitone_up.wav";
		
		AudioFormat format = AudioSystem.getAudioInputStream(new File(file)).getFormat();

		AudioDispatcher dispatcher = AudioDispatcher.fromFile(new File(file),1024,0);
		dispatcher.addAudioProcessor(new PercussionOnsetDetector(format.getSampleRate(),1024, new PercussionHandler() {
			int i = 0 ;
			@Override
			public void handlePercussion(double timestamp) {
				System.out.println(i++ + "\t" + timestamp);
				
			}
		},44,4));
		dispatcher.run();
		*/
	}
	
}
