package be.tarsos.dsp.experimental;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sun.org.apache.xml.internal.resolver.helpers.FileURL;

import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;

public class OnsetTest {
	
	public static void main(String... args){
		String ref = "/media/data/datasets/Fingerprinting datasets/Datasets/small_dataset/reference/11266.mp3";
		String beats= "/media/data/datasets/Fingerprinting datasets/Datasets/small_dataset/reference/11266.csv";
		be.tarsos.dsp.AudioDispatcher adp = be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromPipe(ref, 44100, 256, 0);
		ComplexOnsetDetector cod = new ComplexOnsetDetector(256);
		final List<Double> onsetTimes = new ArrayList<Double>();
		adp.addAudioProcessor(cod);
		cod.setHandler(new OnsetHandler() {
			
			@Override
			public void handleOnset(double time, double salience) {
				onsetTimes.add(time);	
			}
		});
		adp.run();
		
		List<Double> beatTimes = new ArrayList<Double>();
		String[] data = readFile(beats).split("\n");
		for(String time : data){
			beatTimes.add(Double.parseDouble(time));
		}
		
		for(int i = 0 ; i < onsetTimes.size();i++){
			for(int j = i+1; j < onsetTimes.size() && onsetTimes.get(i) + 1.5 > onsetTimes.get(j);j++){
				System.out.println("diff:" +  (onsetTimes.get(j) - onsetTimes.get(i)) );
			}
		}
	}
	
	public static String readFile(final String name) {
		FileReader fileReader = null;
		final StringBuilder contents = new StringBuilder();
		try {
			final File file = new File(name);
			if (!file.exists()) {
				throw new IllegalArgumentException("File " + name + " does not exist");
			}
			fileReader = new FileReader(file);
			final BufferedReader reader = new BufferedReader(fileReader);
			String inputLine = reader.readLine();
			while (inputLine != null) {
				contents.append(inputLine).append("\n");
				inputLine = reader.readLine();
			}
			reader.close();
		} catch (final IOException i1) {
			//LOG.severe("Can't open file:" + name);
		}
		return contents.toString();
	}


}
