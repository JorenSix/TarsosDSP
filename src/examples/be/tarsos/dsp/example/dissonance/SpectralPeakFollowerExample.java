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

package be.tarsos.dsp.example.dissonance;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SpectralPeakProcessor;
import be.tarsos.dsp.io.PipedAudioStream;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;

public class SpectralPeakFollowerExample {	
	private final int sampleRate = 44100;
	private final int fftsize = 32768/2;
	private final int overlap = fftsize/2;//50% overlap
	//private final int noiseFloorMedianFilterLenth = fftsize/117;//35
	//private final float noiseFloorFactor = 1.5f;
	private final String fileName;
	//private final int numberOfSpectralPeaks;
	
	/**
	 * Peaks are only allowed between the highest peak minus 2400 and plus 7200 cents (8 octaves in total)
	 */
	//private final int minimumCentsBelowHighest = 2400;//2 octaves
	//private final int maximumCentsAboveHighest = 7200;//6 octaves

	
	
	private final List<float[]> magnitudesList = new ArrayList<float[]>();
	private final List<float[]> frequencyEstimatesList = new ArrayList<float[]>();
	
	
	public SpectralPeakFollowerExample(String fileName,int numberOfSpectralPeaks){
		this.fileName = fileName;
		//this.numberOfSpectralPeaks = numberOfSpectralPeaks;
		
	}
	
	private void extractPeakListList() throws UnsupportedAudioFileException{
		PipedAudioStream f = new PipedAudioStream(fileName);
		TarsosDSPAudioInputStream stream = f.getMonoStream(sampleRate,0);
		
		final SpectralPeakProcessor spectralPeakFollower = new SpectralPeakProcessor(fftsize, overlap, sampleRate);
		AudioDispatcher dispatcher = new AudioDispatcher(stream, fftsize, overlap);
		dispatcher.addAudioProcessor(spectralPeakFollower);
		dispatcher.addAudioProcessor(new AudioProcessor() {
			@Override
			public void processingFinished() {
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				magnitudesList.add(spectralPeakFollower.getMagnitudes());
				frequencyEstimatesList.add(spectralPeakFollower.getFrequencyEstimates());
				return true;
			}
		});
		dispatcher.run();
	}
	
	private void processPeakListList(){
		/*
		KernelDensityEstimate kde = new KernelDensityEstimate(new KernelDensityEstimate.GaussianKernel(30), minimumCentsBelowHighest + maximumCentsAboveHighest);
		for(List<SpectralPeak> spectralPeakList : peakListList){
			for(SpectralPeak spectralPeak : spectralPeakList){
				//avoids peaks more than 2 octaves below the highest peak
				if(
						spectralPeak.getRelativeFrequencyInCents() + minimumCentsBelowHighest >= 0
						&&
						spectralPeak.getRelativeFrequencyInCents() <= maximumCentsAboveHighest 
				){
					//String point = String.format("%.2f;%.2f", spectralPeak.getRelativeFrequencyInCents(),spectralPeak.getMagnitude());
					for(int i = 0 ; i < Math.log(spectralPeak.getMagnitude()* 100) ; i++){
						kde.add(spectralPeak.getRelativeFrequencyInCents() + minimumCentsBelowHighest);
					}
				}
			}
		}
		
		kde.normalize(100.0);
		double[] estimate = kde.getEstimate();
		
		//all peaks are within the largest 5 percent of the estimate
		double noiseThreshold = SpectralPeakFollower.percentile(estimate.clone(),0.95);
						
		List<Double> peakValues = new ArrayList<Double>();
		for(int i  = 1 ; i < estimate.length - 1 ;i++){
			boolean largerThanPrev = estimate[i] > estimate[i-1];
			boolean largerThanNext = estimate[i] > estimate[i+1];
			boolean largerThanNoise = estimate[i] >= noiseThreshold;
			if(largerThanNoise && largerThanNext && largerThanPrev){
				peakValues.add(estimate[i]);
			}
		}
		
		double threshold = noiseThreshold;
		if(peakValues.size()>numberOfSpectralPeaks){
			Collections.sort(peakValues);
			threshold = peakValues.get(peakValues.size()-numberOfSpectralPeaks);
		}
		
		
		System.out.println("Cent Amplitude ratio");
		for(int i  = 1 ; i < estimate.length - 1 ;i++){
			boolean largerThanPrev = estimate[i] > estimate[i-1];
			boolean largerThanNext = estimate[i] > estimate[i+1];
			boolean largerThanThreshold = estimate[i] >= threshold;
			if(largerThanThreshold && largerThanNext && largerThanPrev){
				int cents = i - minimumCentsBelowHighest;
				double ratio = 0;
				if(cents != 0){
					ratio = PitchConverter.centToRatio(cents);
				}
				System.out.println(String.format("%d %.2f %.2f",cents, estimate[i],ratio));
			}
		}
		*/
	}
	
	public static void main(String... args) throws UnsupportedAudioFileException {		
		String fileName = args[0];
		
		final int numberOfSpectralPeaks;
		if(args.length == 2){
			numberOfSpectralPeaks = Integer.parseInt(args[1]);
		}else{
			numberOfSpectralPeaks = 10;
		}
		
		SpectralPeakFollowerExample spfe = new SpectralPeakFollowerExample(fileName,numberOfSpectralPeaks);
		spfe.extractPeakListList();
		spfe.processPeakListList();
	}
}
