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
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.example.Shared;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;
import be.tarsos.dsp.onsets.PrintOnsetHandler;


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
		
		//open a LineWavelet
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
		JVMAudioInputStream inputStream = new JVMAudioInputStream(stream);
		//create a new dispatcher
		AudioDispatcher dispatcher = new AudioDispatcher(inputStream, bufferSize, overlap);
		
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
