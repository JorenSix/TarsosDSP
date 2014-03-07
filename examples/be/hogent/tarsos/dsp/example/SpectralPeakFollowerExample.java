package be.hogent.tarsos.dsp.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioFile;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.SpectralPeakFollower;
import be.hogent.tarsos.dsp.SpectralPeakFollower.SpectralPeak;
import be.hogent.tarsos.dsp.util.PitchConverter;

public class SpectralPeakFollowerExample {
	
	
	
	private final int sampleRate = 44100;
	private final int fftsize = 32768/2;
	private final int overlap = fftsize/2;//50% overlap
	private final int noiseFloorMedianFilterLenth = fftsize/117;//35
	private final float noiseFloorFactor = 1.5f;
	private final String fileName;
	private final int numberOfSpectralPeaks;
	
	/**
	 * Peaks are only allowed between the highest peak minus 2400 and plus 7200 cents (8 octaves in total)
	 */
	private final int minimumCentsBelowHighest = 2400;//2 octaves
	private final int maximumCentsAboveHighest = 7200;//6 octaves

	
	
	private final List<List<SpectralPeak>> peakListList = new ArrayList<List<SpectralPeak>>();
	
	
	public SpectralPeakFollowerExample(String fileName,int numberOfSpectralPeaks){
		this.fileName = fileName;
		this.numberOfSpectralPeaks = numberOfSpectralPeaks;
		
	}
	
	private void extractPeakListList() throws UnsupportedAudioFileException{
		AudioFile f = new AudioFile(fileName);
		AudioInputStream stream = f.getMonoStream(sampleRate);
		
		final SpectralPeakFollower spectralPeakFollower = new SpectralPeakFollower(fftsize, overlap, sampleRate,noiseFloorMedianFilterLenth,numberOfSpectralPeaks,noiseFloorFactor);
		AudioDispatcher dispatcher = new AudioDispatcher(stream, fftsize, overlap);
		dispatcher.addAudioProcessor(spectralPeakFollower);
		dispatcher.addAudioProcessor(new AudioProcessor() {
			@Override
			public void processingFinished() {
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				peakListList.add(spectralPeakFollower.getPeakList());
				return true;
			}
		});
		dispatcher.run();
	}
	
	private void processPeakListList(){
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
