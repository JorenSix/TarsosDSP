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

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.DetermineDurationProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

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
			AudioDispatcher audioDispatcher = AudioDispatcherFactory.fromPipe(resource, 44100, 2048, 0);
			DetermineDurationProcessor ddp = new DetermineDurationProcessor();
			audioDispatcher.addAudioProcessor(ddp);
			audioDispatcher.run();
			System.out.println("Decoded resource '" + resource + "'. \n  It has a duration of " + ddp.getDurationInSeconds() + " seconds ");
			System.exit(0);
		}
	}
}
