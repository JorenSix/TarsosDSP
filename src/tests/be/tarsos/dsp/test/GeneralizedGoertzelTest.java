package be.tarsos.dsp.test;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.GeneralizedGoertzel;
import be.tarsos.dsp.pitch.Goertzel.FrequenciesDetectedHandler;

public class GeneralizedGoertzelTest {
	
	public static void main(String... args) throws UnsupportedAudioFileException, IOException{
		
		selCall();
		
		double[] frequenciesToDetect = new double[100];
		for(int i = 0 ; i < frequenciesToDetect.length ; i++ ){
			frequenciesToDetect[i] = 396 + i;
		}
		FrequenciesDetectedHandler handler = new FrequenciesDetectedHandler() {
			
			@Override
			public void handleDetectedFrequencies(double[] frequencies,
					double[] powers, double[] allFrequencies, double[] allPowers) {
				int maxIndex = 0;
				double maxPower = 0;
				for(int i = 0 ; i < frequencies.length;i++){
					if(powers[i] > maxPower){
						maxPower = powers[i];
						maxIndex= i;
					}
				}
				System.out.println(frequencies[maxIndex] +"\t" + powers[maxIndex]);
			}
		};
		
	
		
		int blockSize = 4096;
		AudioProcessor generalized = new GeneralizedGoertzel(44100, blockSize,frequenciesToDetect, handler);
		//AudioProcessor classic = new Goertzel(44100, 2048,frequenciesToDetect, handler);
		AudioDispatcher ad = AudioDispatcherFactory.fromFile(new File("/home/joren/Desktop/440Hz-44.1kHz.wav"), blockSize, 0);
		ad = AudioDispatcherFactory.fromFile(new File("/home/joren/Desktop/chirp-44.1kHz_10min.wav"), blockSize, 0);
		
		
		//ad.addAudioProcessor(classic);
		ad.addAudioProcessor(generalized);
		long prev = System.currentTimeMillis();
		//ad.run();
		long diff = System.currentTimeMillis() - prev;
		System.out.println(diff);
	}
	
	private static void selCall() throws UnsupportedAudioFileException, IOException{
		
		final double[] frequencies = {1981,1124,1197,1275,1358,1446,1540,1640,1747,1860,2400,930,2247,991,2110,1055};
		final String[] symbols = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
		
		FrequenciesDetectedHandler handler = new FrequenciesDetectedHandler() {
			String prevSymbol = "";
			@Override
			public void handleDetectedFrequencies(double[] frequencies,
					double[] powers, double[] allFrequencies, double[] allPowers) {
				int maxIndex = 0;
				double maxPower = 0;
				for(int i = 0 ; i < frequencies.length;i++){
					if(powers[i] > maxPower){
						maxPower = powers[i];
						maxIndex= i;
					}
				}
				if( maxPower > 20 ){
					String symbol = symbols[maxIndex];
					if(! symbol.equalsIgnoreCase(prevSymbol)){
						System.out.println(frequencies[maxIndex] +"\t" + powers[maxIndex]+ "\t" + symbol);
					}
					prevSymbol = symbol;
				}
			}
		};
		
		
		int blockSize = 205;
		int sampleRate = 8000;
		AudioProcessor generalized = new GeneralizedGoertzel(sampleRate, blockSize,frequencies, handler);
		//AudioProcessor classic = new Goertzel(44100, 2048,frequenciesToDetect, handler);
		String source = "/home/joren/Desktop/CCIR_04221.ogg";
		AudioDispatcher ad = AudioDispatcherFactory.fromPipe(source, sampleRate, blockSize, 0);
		ad.addAudioProcessor(generalized);
		ad.run();
	}


}
