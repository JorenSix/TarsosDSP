package be.hogent.tarsos.dsp.speechmusicdiscriminator;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioFile;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.ZeroCrossingRateProcessor;

/**
 * 
 * 
 * https://github.com/bbcrd/bbc-vamp-plugins/blob/master/src/SpeechMusicSegmenter.cpp
 * 
 *  J. Saunders, "Real-time discrimination of broadcast speech/music,"
 * IEEE International Conference on Acoustics, Speech, and Signal Processing,
 * vol.2, pp.993-999, 7-10 May 1996
 * 
 * Measure the skewness of the distribution of zero-crossing rate across the audio file;
 * Find points at which this distribution changes drastically;
 *  For each candidate change point found, classify the corresponding segment as follows:
 *      Mean skewness > threshold: speech
 *      Mean skewness < threshold: music
 * If the segment has the same type with the previous one, merge it with
 * the previous one.
 * 
 * @author Chris Baume
 * @author Joren Six
 *
 */
public class OfflineSpeechMusicDiscriminator implements AudioProcessor {
	
	private final int resolution = 256;
	private final double changeThreshold = 0.08;
	private final double decisionThreshold = 0.270;
	private final double zeroCrossingMeanMargin = 14;
	private final ZeroCrossingRateProcessor zeroCrossingRateProcessor = new ZeroCrossingRateProcessor();
	
	private final List<Float> zeroCrossingRates;

	
	
	private int frameCounter;
	
	private String prevState = "";
	
	
	public OfflineSpeechMusicDiscriminator(){
		zeroCrossingRates = new ArrayList<Float>();
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		zeroCrossingRateProcessor.process(audioEvent);
			
		float zeroCrossingRate = zeroCrossingRateProcessor.getZeroCrossingRate();
		//store the current zero crossing rate
		zeroCrossingRates.add(zeroCrossingRate);
		frameCounter++;
		return true;
	}
	
	

	private void determineClassification() {
		List<Double> skewnessFunction = determineSkewnessFunction();
		double oldMean = 0.0;
		
		for (int n = 0; n < frameCounter / resolution ; n++) {
			 double mean = 0.0;
		     for (int i = 0; i < resolution; i++) {
		           mean += skewnessFunction.get(n * resolution + i);
		     }
		     mean /= (float)resolution;
		     System.out.println(mean);
		     if(n==0 || ( n>0 && Math.abs(mean-oldMean) > changeThreshold)){
		    	 if(mean < decisionThreshold){
		    		 if(!prevState.equalsIgnoreCase("Music")){
		    			 System.out.println("Music " + ((n * resolution + resolution/2.0) * 1024 / 22050.0));
		    		 }
		    		  prevState = "Music";
		    	 }else{
		    		 if(!prevState.equalsIgnoreCase("Speech")){
		    			 System.out.println("Speech " + ((n * resolution + resolution/2.0) * 1024 / 22050.0));
		    			 prevState = "Speech";
		    		 }
		    	 }
		     }
		     oldMean = mean;
		}
		
		for ( int n = 1; n < skewnessFunction.size(); n++) {
			//System.out.println(skewnessFunction.get(n));
		}
		
	}
	
	private List<Double> determineSkewnessFunction(){
		double meanZeroCrossingRate = 0.0;
		double meanThreashold = zeroCrossingMeanMargin/1000.0;
		List<Double> skewness = new ArrayList<Double>();
		for(int n = 0 ; n < frameCounter ; n++){
			int i = 0;
			meanZeroCrossingRate = 0.0;
			while(i < resolution && n+i < zeroCrossingRates.size()){
				meanZeroCrossingRate += zeroCrossingRates.get(n+i);
				i++;
			}
			meanZeroCrossingRate /= (float) resolution;
			
			i=0;
			int aboveMeanZCR=0;
			int belowMeanZCR=0;
			while (i < resolution && n+i < zeroCrossingRates.size()) {
		           if ( zeroCrossingRates.get(n+i) > (meanZeroCrossingRate + meanThreashold)){
		        	   aboveMeanZCR += 1;
		           }
		           if (zeroCrossingRates.get(n+i) < (meanZeroCrossingRate - meanThreashold)){
		        	   belowMeanZCR += 1;
		           }
		           i ++;
		    }
			double skewnessValue = belowMeanZCR - aboveMeanZCR;
	        skewnessValue /= (double) resolution;
	        skewness.add(skewnessValue);
		}
		return skewness;
	}

	@Override
	public void processingFinished() {
		determineClassification();
	}

	
	public static void main(String...args) throws UnsupportedAudioFileException, LineUnavailableException{
		AudioFile audioFile = new AudioFile("http://mp3.streampower.be/stubru-high.mp3");
		audioFile = new AudioFile("/home/joren/Desktop/music_speech.wav");
		AudioInputStream stream = audioFile.getMonoStream(22050);
		AudioDispatcher adp = new AudioDispatcher(stream,1024,0);
		adp.addAudioProcessor(new OfflineSpeechMusicDiscriminator());
		//adp.addAudioProcessor(new AudioPlayer(audioFile.getTargetFormat(44100)));
		adp.run();	
	}
}
