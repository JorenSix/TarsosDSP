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

package be.tarsos.dsp.example;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.effects.DelayEffect;
import be.tarsos.dsp.filters.LowPassFS;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.synthesis.AmplitudeLFO;
import be.tarsos.dsp.synthesis.NoiseGenerator;
import be.tarsos.dsp.synthesis.SineGenerator;

/**
 * Shows how a synthesizer can be constructed using some simple ugen blocks.
 * @author Joren Six
 */
public class SynthesisExample {
	
	public static void main(String... args) throws LineUnavailableException{
		AudioDispatcher dispatcher = new AudioDispatcher(1024);
		dispatcher.addAudioProcessor(new NoiseGenerator(0.2));
		dispatcher.addAudioProcessor(new LowPassFS(1000,44100));
		dispatcher.addAudioProcessor(new LowPassFS(1000,44100));
		dispatcher.addAudioProcessor(new LowPassFS(1000,44100));
		dispatcher.addAudioProcessor(new SineGenerator(0.05,220));
		dispatcher.addAudioProcessor(new AmplitudeLFO(10,0.9));
		dispatcher.addAudioProcessor(new SineGenerator(0.2,440));
		dispatcher.addAudioProcessor(new SineGenerator(0.1,880));
		dispatcher.addAudioProcessor(new DelayEffect(1.5, 0.4, 44100));
		dispatcher.addAudioProcessor(new AmplitudeLFO());
		dispatcher.addAudioProcessor(new SineGenerator(0.05,1760));
		dispatcher.addAudioProcessor(new SineGenerator(0.01,2460));
		dispatcher.addAudioProcessor(new DelayEffect(0.757, 0.4, 44100));
		dispatcher.addAudioProcessor(new AudioPlayer( new AudioFormat(44100, 16, 1, true, false)));
		dispatcher.run();
	}
}
