package be.hogent.tarsos.dsp.example;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.DetermineDurationProcessor;
import be.hogent.tarsos.dsp.PipeDecoder;

/**
 * Shows how to use the PipeDecoder to decode about any audio format.
 * 
 * @author Joren Six
 */
public class AudioFormatDecoderExample {
	
	public static void main(String... args) throws UnsupportedAudioFileException{
		if(args.length!=1){
			System.err.println("Pleas provide only one argument: an audio resource to decode. \nThe method also supports streaming over http.");
		}else{
			SharedCommandLineUtilities.printPrefix();
			SharedCommandLineUtilities.printLine();
			String resource = args[0];
			PipeDecoder decoder = new PipeDecoder();
			AudioInputStream stream = decoder.getDecodedStream(resource, 44100);
			AudioDispatcher audioDispatcher = new AudioDispatcher(stream, 2048, 0);
			DetermineDurationProcessor ddp = new DetermineDurationProcessor();
			audioDispatcher.addAudioProcessor(ddp);
			audioDispatcher.run();
			System.out.println("Decoded resource '" + resource + "'. \n  It has a duration of " + ddp.getDurationInSeconds() + " seconds ");
			System.exit(0);
		}
	}
}
