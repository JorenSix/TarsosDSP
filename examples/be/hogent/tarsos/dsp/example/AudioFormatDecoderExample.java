package be.hogent.tarsos.dsp.example;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.ConstantQ;
import be.hogent.tarsos.dsp.DetermineDurationProcessor;
import be.hogent.tarsos.dsp.PipeDecoder;

public class AudioFormatDecoderExample {
	
	public static void main(String... args) throws UnsupportedAudioFileException{
		String resource = args[0];
		PipeDecoder decoder = new PipeDecoder();
		ConstantQ cteq = new ConstantQ(44100, 120, 10000, 12);
		
		AudioInputStream stream = decoder.getDecodedStream(resource, 44100);
		AudioDispatcher audioDispatcher = new AudioDispatcher(stream, cteq.getFFTlength(), 0);
		System.out.println(cteq.getFFTlength());
		//DetermineDurationProcessor ddp = new DetermineDurationProcessor();
		audioDispatcher.addAudioProcessor(cteq);
		//audioDispatcher.addAudioProcessor(ddp);
		audioDispatcher.run();
		System.out.println("Decoded resource '" + resource + "'. \n  It has a duration of " + 0 + " seconds ");
		System.exit(0);
	}
}
