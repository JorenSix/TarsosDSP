package be.tarsos.dsp.test;

import static org.junit.Assert.*;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.GeneralizedGoertzel;
import be.tarsos.dsp.pitch.Goertzel.FrequenciesDetectedHandler;

public class GeneralizedGoertzelTest {
	
	private String symbolsAccumulator = "";
	
	//see https://en.wikipedia.org/wiki/Selcall
	@Test
	public void testSelCallDetection() throws UnsupportedAudioFileException, IOException{
		
		final double[] frequencies = {1981,1124,1197,1275,1358,1446,1540,1640,1747,1860,2400,930,2247,991,2110,1055};
		final String[] symbols = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
		
		FrequenciesDetectedHandler handler = new FrequenciesDetectedHandler() {
			String prevSymbol = "";
			
			@Override
			public void handleDetectedFrequencies(double time, double[] frequencies,
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
						//System.out.println(frequencies[maxIndex] +"\t" + powers[maxIndex]+ "\t" + symbol);
						symbolsAccumulator += symbol;
						//System.out.println(symbolsAccumulator);
					}
					prevSymbol = symbol;
				}
			}
		};
		
		
		int blockSize = 205;
		int sampleRate = 8000;
		AudioProcessor generalized = new GeneralizedGoertzel(sampleRate, blockSize,frequencies, handler);
		//AudioProcessor classic = new Goertzel(44100, 2048,frequenciesToDetect, handler);
		String source = TestUtilities.ccirFile().getAbsolutePath();
		AudioDispatcher ad = AudioDispatcherFactory.fromPipe(source, sampleRate, blockSize, 0);
		ad.addAudioProcessor(generalized);
		ad.run();
		
		assertEquals("The selCall decoded it",symbolsAccumulator,"042E1");
	}


}
