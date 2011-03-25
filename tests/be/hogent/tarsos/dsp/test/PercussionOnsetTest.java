package be.hogent.tarsos.dsp.test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.PercussionOnsetDetector;
import be.hogent.tarsos.dsp.PercussionOnsetDetector.PercussionHandler;

public class PercussionOnsetTest {
	

	public static void main(String[] args) throws LineUnavailableException, UnsupportedAudioFileException{
		
		float sampleRate = 44100;
		int bufferSize = 1024;
		int overlap = 0;
		
		//available mixers
		int index = 0;
		int selectedMixerIndex = 3;
		for(Mixer.Info mixer : AudioSystem.getMixerInfo()){
			System.out.println(index + ": " + mixer.toString());
			index++;
		}
		Mixer.Info selectedMixer = AudioSystem.getMixerInfo()[selectedMixerIndex];
		System.out.println("Selected mixer: " + selectedMixer.toString());
		
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
		dispatcher.addAudioProcessor(new PercussionOnsetDetector(sampleRate, bufferSize,
				overlap, new PercussionHandler() {
					@Override
					public void handlePercussion(double timestamp) {
						System.out.println(timestamp + "s");
					}
				}));
		
		//run the dispatcher (on the same thread, use start() to run it on another thread). 
		dispatcher.run();
	}
}
