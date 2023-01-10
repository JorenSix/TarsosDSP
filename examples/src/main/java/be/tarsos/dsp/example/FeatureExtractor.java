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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.beatroot.BeatRootOnsetEventHandler;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
/**
 * Provides support for different types of command LineWavelet audio feature extraction.
 * @author Joren Six
 */
public class FeatureExtractor {
	
	private final List<FeatureExtractorApp> featureExtractors;
	
	public FeatureExtractor(String...arguments){
		//Create a list of feature extractors
		featureExtractors = new ArrayList<FeatureExtractorApp>();
		featureExtractors.add(new SoundPressureLevelExtractor());
		featureExtractors.add(new PitchExtractor());
		featureExtractors.add(new RootMeanSquareExtractor());
		featureExtractors.add(new OnsetExtractor());
		featureExtractors.add(new BeatExtractor());
		
		checkArgumentsAndRun(arguments);
	}
	
	private void checkArgumentsAndRun(String... arguments){
		if(arguments.length == 0){
			printError();
		} else {
			String subCommand = arguments[0].toLowerCase();
			FeatureExtractorApp appToExecute = null;
			for(FeatureExtractorApp app : featureExtractors){
	    		if(subCommand.equalsIgnoreCase(app.name())){
	    			appToExecute = app;	
	    		}
	    	}
			if(appToExecute == null){
				printError();
			}else{
				try {
					if(!appToExecute.run(arguments)){
						printHelp(appToExecute);
					}
				} catch (UnsupportedAudioFileException e) {
					printHelp(appToExecute);
					SharedCommandLineUtilities.printLine();
					System.err.println("Error:");
					System.err.println("\tThe audio file is not supported!");
				} catch (IOException e) {
					printHelp(appToExecute);
					SharedCommandLineUtilities.printLine();
					System.err.println("Current error:");
					System.err.println("\tIO error, maybe the audio file is not found or not supported!");
				}
			}
		}
	}
	
	private final void printError(){
		SharedCommandLineUtilities.printPrefix();
		System.err.println("Name:");
		System.err.println("\tTarsosDSP feature extractor");
		SharedCommandLineUtilities.printLine();
		System.err.println("Synopsis:");
		System.err.println("\tjava -jar FeatureExtractor.jar SUB_COMMAND [options...]");
		SharedCommandLineUtilities.printLine();
		System.err.println("Description:");
		System.err.println("\t Extracts features from an audio file, SUB_COMMAND needs\n\tto be one of the following:");
		for (FeatureExtractorApp app : featureExtractors) {
			System.err.println("\t\t" + app.name());
		}
    }
	
	private final void printHelp(FeatureExtractorApp appToExecute){
		SharedCommandLineUtilities.printPrefix();
		System.err.println("Name:");
		System.err.println("\tTarsosDSP " + appToExecute.name() + " feature extractor");
		SharedCommandLineUtilities.printLine();
		System.err.println("Synopsis:");
		System.err.println("\tjava -jar FeatureExtractor.jar " + appToExecute.name() + " " + appToExecute.synopsis());
		SharedCommandLineUtilities.printLine();
		System.err.println("Description:");
		System.err.println(appToExecute.description());
    }

	/**
	 * @param arguments
	 */
	public static void main(String... arguments) {
		new FeatureExtractor(arguments);
	}
	
	private interface FeatureExtractorApp{
		String name();
		String description();
		String synopsis();
		boolean run(String... args) throws UnsupportedAudioFileException, IOException;
		
	}
	
	private class RootMeanSquareExtractor implements FeatureExtractorApp{

		@Override
		public String name() {
			return "rms";
		}

		@Override
		public String description() {
			return "\tCalculates the root mean square of an audio signal for each \n\tblock of 2048 samples. The output gives you a timestamp and the RMS value,\n\tSeparated by a semicolon.\n\n\t\n\ninput.wav: a\treadable audio file.";
		}

		@Override
		public String synopsis() {
			return "input.wav";
		}

		@Override
		public boolean run(String... args) throws UnsupportedAudioFileException, IOException {
			if(args.length!=2){
				return false;
			}
				
			String inputFile = args[1];
			File audioFile = new File(inputFile);
			int size = 2048;
			int overlap = 0;
			AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, size, overlap);
			
			dispatcher.addAudioProcessor(new AudioProcessor() {
				@Override
				public void processingFinished() {
				}
				
				@Override
				public boolean process(AudioEvent audioEvent) {
					System.out.println(audioEvent.getTimeStamp() + "," + audioEvent.getRMS());
					return true;
				}
			});
			dispatcher.run();
			return true;
		}
	}
	
	private class SoundPressureLevelExtractor implements FeatureExtractorApp{

		@Override
		public String name() {
			return "sound_pressure_level";
		}

		@Override
		public String description() {
			return "\tCalculates a sound pressure level in dB for each\n\tblock of 2048 samples.The output gives you a timestamp and a value in dBSPL.\n\tSeparated by a semicolon.\n\n\t\n\nWith input.wav\ta readable audio file.";
		}

		@Override
		public String synopsis() {
			return "input.wav";
		}

		@Override
		public boolean run(String... args) throws UnsupportedAudioFileException, IOException {
			if(args.length!=2){
				return false;
			}
				
			String inputFile = args[1];
			File audioFile = new File(inputFile);
			int size = 2048;
			int overlap = 0;
			final SilenceDetector silenceDetecor = new SilenceDetector();		
			AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, size, overlap);
			dispatcher.addAudioProcessor(silenceDetecor);
			dispatcher.addAudioProcessor(new AudioProcessor() {
				@Override
				public void processingFinished() {
				}
				
				@Override
				public boolean process(AudioEvent audioEvent) {
					System.out.println(audioEvent.getTimeStamp() + "," + silenceDetecor.currentSPL());
					return true;
				}
			});
			dispatcher.run();
			return true;
		}
	}
	
	private class PitchExtractor implements FeatureExtractorApp, PitchDetectionHandler{

		@Override
		public String name() {
			return "pitch";
		}

		@Override
		public String description() {
			String descr = "\tCalculates pitch in Hz for each block of 2048 samples. \n\tThe output is a semicolon separated list of a timestamp, frequency in hertz and \n\ta probability which describes how pitched the sound is at the given time. ";
			descr += "\n\n\tinput.wav\t\ta readable wav file.";
			descr += "\n\t--detector DETECTOR\tdefaults to FFT_YIN or one of these:\n\t\t\t\t";
			for(PitchEstimationAlgorithm algo : PitchEstimationAlgorithm.values()){
				descr += algo.name() + "\n\t\t\t\t";
			}
			return descr;
		}

		@Override
		public String synopsis() {
			String helpString = "[--detector DETECTOR] input.wav";			
			return helpString;
		}

		@Override
		public boolean run(String... args) throws UnsupportedAudioFileException, IOException {
			PitchEstimationAlgorithm algo = PitchEstimationAlgorithm.FFT_YIN;
			String inputFile = args[1];
			
			if(args.length == 1 || args.length == 3){
				return false;
			}else if(args.length==4 && !args[1].equalsIgnoreCase("--detector")){
				return false;
			}else if(args.length==4 && args[1].equalsIgnoreCase("--detector")){
				try{
					algo = PitchEstimationAlgorithm.valueOf(args[2].toUpperCase());
					inputFile = args[3];
				}catch(IllegalArgumentException e){
					//if enum value string is not recognized
					return false;
				}
			}
			File audioFile = new File(inputFile);
			float samplerate = AudioSystem.getAudioFileFormat(audioFile).getFormat().getSampleRate();
			int size = 1024;
			int overlap = 0;
			AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, size, overlap);
			dispatcher.addAudioProcessor(new PitchProcessor(algo, samplerate, size, this));
			dispatcher.run();
			return true;
		}

		@Override
		public void handlePitch(PitchDetectionResult pitchDetectionResult,
				AudioEvent audioEvent) {
			double timeStamp = audioEvent.getTimeStamp();
			float pitch = pitchDetectionResult.getPitch();
			float probability = pitchDetectionResult.getProbability();
			System.out.println(timeStamp+","+pitch+","+probability);
		}
	}
	
	private class OnsetExtractor implements FeatureExtractorApp, OnsetHandler{

		@Override
		public String name() {
			return "onset";
		}

		@Override
		public String description() {
			String descr = "\tCalculates onsets using a complex domain onset detector. " +
					"\n\tThe output is a semicolon separated list of a timestamp, and a salliance. ";
			descr += "\n\n\tinput.wav\t\ta readable wav file.";
			descr += "";
			return descr;
		}

		@Override
		public String synopsis() {
			String helpString = "input.wav";			
			return helpString;
		}

		@Override
		public boolean run(String... args) throws UnsupportedAudioFileException, IOException {
			String inputFile = args[1];
			File audioFile = new File(inputFile);
			int size = 512;
			int overlap = 256;
			AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(audioFile.getAbsolutePath(),44100, size, overlap);
			ComplexOnsetDetector detector = new ComplexOnsetDetector(size,0.7,0.1);
			detector.setHandler(this);
			dispatcher.addAudioProcessor(detector);
			
			dispatcher.run();
			return true;
		}

		@Override
		public void handleOnset(double time, double salience) {
			System.out.println(time + "," + salience);
		}
	}
	
	private class BeatExtractor implements FeatureExtractorApp, OnsetHandler{

		@Override
		public String name() {
			return "beat";
		}

		@Override
		public String description() {
			String descr = "\tCalculates onsets using a complex domain onset detector. " +
					"\n\tThe output is a semicolon separated list of a timestamp, and a salliance. ";
			descr += "\n\n\tinput.wav\t\ta readable wav file.";
			descr += "";
			return descr;
		}

		@Override
		public String synopsis() {
			String helpString = "input.wav";			
			return helpString;
		}

		@Override
		public boolean run(String... args) throws UnsupportedAudioFileException, IOException {
			String inputFile = args[1];
			File audioFile = new File(inputFile);
			int size = 512;
			int overlap = 256;
			AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, size, overlap);
			
			ComplexOnsetDetector detector = new ComplexOnsetDetector(size);
			BeatRootOnsetEventHandler handler = new BeatRootOnsetEventHandler();
			detector.setHandler(handler);
			
			dispatcher.addAudioProcessor(detector);
			dispatcher.run();
			
			handler.trackBeats(this);
			
			return true;
		}
		
		@Override
		public void handleOnset(double time, double salience) {
			System.out.println(time);
		}
	}
}
