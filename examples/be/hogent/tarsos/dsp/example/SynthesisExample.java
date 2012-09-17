package be.hogent.tarsos.dsp.example;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioPlayer;
import be.hogent.tarsos.dsp.synthesis.NoiseGenerator;
import be.hogent.tarsos.dsp.synthesis.SineGenerator;

public class SynthesisExample {
	
	public static void main(String... args) throws LineUnavailableException{
		AudioDispatcher dispatcher = new AudioDispatcher(1024);
		dispatcher.addAudioProcessor(new SineGenerator(0.2,440));
		dispatcher.addAudioProcessor(new SineGenerator(0.1,880));
		dispatcher.addAudioProcessor(new SineGenerator(0.05,220));
		dispatcher.addAudioProcessor(new NoiseGenerator(0.003));
		dispatcher.addAudioProcessor(new AudioPlayer( new AudioFormat(44100, 16, 1, true, false)));
		dispatcher.run();
		
	}

}
