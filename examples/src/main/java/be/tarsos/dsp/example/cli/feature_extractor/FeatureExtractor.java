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

package be.tarsos.dsp.example.cli.feature_extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.example.TarsosDSPExampleStarter;
import be.tarsos.dsp.example.cli.SharedCommandLineUtilities;

/**
 * Provides support for different types of command LineWavelet audio feature extraction.
 * @author Joren Six
 */
public class FeatureExtractor {

	public static class FeatureExtractorStarter extends TarsosDSPExampleStarter{

		@Override
		public String name() {
			return "feature_extractor";
		}

		@Override
		public void start(String... args) {
			FeatureExtractor.main(args);
		}

		public String description(){
			return "Extract features from audio.";
		}

		public boolean hasGUI(){
			return false;
		}
	}
	
	private final List<FeatureExtractorApp> featureExtractors;
	
	public FeatureExtractor(String...arguments){
		//Create a list of feature extractors
		featureExtractors = new ArrayList<>();
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
		System.err.println("\tjava -jar examples.jar feature_extractor SUB_COMMAND [options...]");
		SharedCommandLineUtilities.printLine();
		System.err.println("Description:");
		System.err.println("\t Extracts features from an audio file, SUB_COMMAND needs\n\tto be one of the following:");
		for (FeatureExtractorApp app : featureExtractors) {
			System.err.println("\t\t" + app.name());
		}
    }
	
	private void printHelp(FeatureExtractorApp appToExecute){
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

}
