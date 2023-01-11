package be.tarsos.dsp.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.GeneralizedGoertzel;
import be.tarsos.dsp.pitch.Goertzel.FrequenciesDetectedHandler;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;

public class CrossCorrelation implements AudioProcessor{
	
	private final float[] zeroPaddedInvesedQuery;
	private final float[] zeroPaddedData;
	
	static interface CrossCorrelationHandler{
		void handleCrossCorrelation(float audioBufferTime,float maxTime,float value);
	}
	
	
	private final FFT fft;
	private final CrossCorrelationHandler handler;
	public CrossCorrelation(float[] query,CrossCorrelationHandler handler){
		zeroPaddedInvesedQuery = new float[query.length*2];
		zeroPaddedData= new float[query.length*2];
		int queryIndex = query.length-1;
		for(int i = query.length/2; i < query.length + query.length/2 ; i++){
			zeroPaddedInvesedQuery[i] = query[queryIndex];
			queryIndex--;
		}
		this.handler = handler;
		fft =  new FFT(zeroPaddedInvesedQuery.length,new HammingWindow());
		fft.forwardTransform(zeroPaddedInvesedQuery);
	}
	
	boolean prev;
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] fftData = audioEvent.getFloatBuffer().clone();
		
		Arrays.fill(zeroPaddedData, 0);
		System.arraycopy(fftData, 0, zeroPaddedData, fftData.length/2, fftData.length);
		
		fft.forwardTransform(zeroPaddedData);
	
		fft.multiply(zeroPaddedData, zeroPaddedInvesedQuery);
		fft.backwardsTransform(zeroPaddedData);
		float maxVal = -100000;
		int maxIndex =  0;
		for(int i = 0 ; i<zeroPaddedData.length ; i++){
			if(zeroPaddedData[i]> maxVal){
				maxVal = zeroPaddedData[i];
				maxIndex=i;
			}
		}
		
		float time = (float) (audioEvent.getTimeStamp() - audioEvent.getBufferSize()/audioEvent.getSampleRate() + maxIndex/2 /audioEvent.getSampleRate());
		handler.handleCrossCorrelation((float)audioEvent.getTimeStamp(), time, maxVal);
		return true;
	}
	@Override
	public void processingFinished() {
		
	}
	
	static float[] query;
	static float bufferTime;
	static float maxTime; 
	public static void main(String...strings) throws UnsupportedAudioFileException, IOException{
		AudioDispatcher q = AudioDispatcherFactory.fromFile(new File("/home/joren/Desktop/44kHz_1024_samples.wav"), 1024, 0);
		
		q.addAudioProcessor(new AudioProcessor() {
			
			@Override
			public void processingFinished() {
				// TODO Auto-generated method stub
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				query = audioEvent.getFloatBuffer().clone();
				return false;
			}
		});
		q.run();
		
		final List<Float> potentialMatch = new ArrayList<Float>();
		
		
		AudioDispatcher ref; 
		//ref = AudioDispatcherFactory.fromPipe("/home/joren/Desktop/sort/1044026.mp3",44100, 1024, 1024-128);
		//ref = AudioDispatcherFactory.fromPipe("/home/joren/Desktop/ref_other_new.wav",44100, 1024, 1024-128);
		//ref = AudioDispatcherFactory.fromPipe("/home/joren/Desktop/mixed_clip_09.wav",44100, 1024, 1024-128);
		ref = AudioDispatcherFactory.fromPipe("/home/joren/Recordings/Clip 12",44100, 1024, 1024-128);
		//ref = AudioDispatcherFactory.fromPipe("/home/joren/Desktop/clip_09_amplified.wav",44100, 1024, 1024-128);
		double[] frequencies = {697,941,1209};
		
		CrossCorrelation crosscorr = new CrossCorrelation(query,new CrossCorrelationHandler() {
			@Override
			public void handleCrossCorrelation(float audioBufferTime, float maxTime,
					float value) {
				if(value > 500){
				bufferTime = audioBufferTime;
				CrossCorrelation.maxTime = maxTime;
				//System.out.println(maxTime + " " + value);
				}
			}
		});
		GeneralizedGoertzel gengoe = new GeneralizedGoertzel(44100.0f, 1024, frequencies, new FrequenciesDetectedHandler() {
			@Override
			public void handleDetectedFrequencies(double time,double[] frequencies,
					double[] powers, double[] allFrequencies, double[] allPowers) {
				
				if(powers[0] > 3 && powers[1] > 3 && powers[2] > 3 && powers[0] + powers[1] + powers[2] > 20  ){
					if((float) time == bufferTime){
						//System.out.println(time +  " " + powers[0] + " " + powers[1] + " " + powers[2]);
						//System.out.println(maxTime);
						potentialMatch.add( maxTime);
					}
				}
			}
		});
		ref.addAudioProcessor(new SilenceDetector(-45, true));
		ref.addAudioProcessor(crosscorr);
		ref.addAudioProcessor(gengoe);
		ref.run();
		
		float maxMsDifference = 207.5f;
		float minDifference = 193.5f;
		float minI = -1000;
		for(int i = 0 ; i < potentialMatch.size();i++){
			if(minI < potentialMatch.get(i) ){
				float max = potentialMatch.get(i) + maxMsDifference/1000.0f;
				float min = potentialMatch.get(i) + minDifference/1000.0f;
				
				for(int j = i+1 ; j < potentialMatch.size(); j++ ){
					if( potentialMatch.get(j) >= min && potentialMatch.get(j) <= max){
						System.out.println("Match at: " +   potentialMatch.get(i));
						minI = max;
						break;
					}
				}
			}
		}
		
		
		
	}
}
