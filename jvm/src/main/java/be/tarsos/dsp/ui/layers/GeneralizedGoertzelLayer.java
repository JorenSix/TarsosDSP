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

package be.tarsos.dsp.ui.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.StopAudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.GeneralizedGoertzel;
import be.tarsos.dsp.pitch.Goertzel.FrequenciesDetectedHandler;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.util.PitchConverter;



public class GeneralizedGoertzelLayer implements Layer{

	private TreeMap<Double, double[]> features;
	private final CoordinateSystem cs;
	private final File audioFile;
	

	private double maxSpectralEnergy = 0;
	private double minSpectralEnergy = 100000;
	private float[] binStartingPointsInCents;
	private float binWith;// in seconds
	private float binHeight;// in cents

	

	public GeneralizedGoertzelLayer(CoordinateSystem cs, File audioFile, int binHeightInCents) {
		this.cs = cs;		
		this.audioFile = audioFile;
	}

	public void draw(Graphics2D graphics) {
		
		calculateFeatures();
		
		if(features != null){
			Map<Double, double[]> spectralInfoSubMap = features.subMap(
					cs.getMin(Axis.X) / 1000.0, cs.getMax(Axis.X) / 1000.0);
			for (Map.Entry<Double, double[]> column : spectralInfoSubMap.entrySet()) {
				double timeStart = column.getKey();// in seconds
				double[] spectralEnergy = column.getValue();// in cents
	
				// draw the pixels
				for (int i = 0; i < spectralEnergy.length; i++) {
					Color color = Color.black;
					float centsStartingPoint = binStartingPointsInCents[i];
					// only draw the visible frequency range
					if (centsStartingPoint >= cs.getMin(Axis.Y)
							&& centsStartingPoint <= cs.getMax(Axis.Y)) {
						
						double factor = spectralEnergy[i] / maxSpectralEnergy;
						int greyValue = 255 - (int) (factor * 255) ;
						greyValue = Math.max(0, greyValue);
						color = new Color(greyValue, greyValue, greyValue);
						graphics.setColor(color);
						graphics.fillRect((int) Math.round(timeStart * 1000),
								Math.round(centsStartingPoint),
								(int) Math.round(binWith * 1000),
								(int) Math.ceil(binHeight));
					}
				}
			}
		}
	}
	
	

	
	public void calculateFeatures() {
		try {
			//maxSpectralEnergy = 0;
			//minSpectralEnergy = 100000;
			int blockSize = 8000;
			int overlap = 7500;
			AudioDispatcher adp =  AudioDispatcherFactory.fromFile(audioFile, blockSize,overlap);
			adp.skip(Math.max(0, cs.getMin(Axis.X)/1000.0));
			adp.addAudioProcessor(new StopAudioProcessor(cs.getMax(Axis.X)/1000.0));
			
			final float sampleRate = adp.getFormat().getFrameRate();
			
			double lowFrequencyInCents = cs.getMin(Axis.Y);
			double highFrequencyInCents = cs.getMax(Axis.Y);
			
			int steps = 50; // 100 steps;
			double stepInCents = (highFrequencyInCents - lowFrequencyInCents) / (float) steps;
			

			binWith = (blockSize - overlap)	/  sampleRate;
			binHeight = (float) stepInCents;
			double[] frequencies = new double[steps];
			binStartingPointsInCents = new float[steps];
			for(int i = 0 ; i< steps ; i++){
				double valueInCents = i * stepInCents + lowFrequencyInCents;
				frequencies[i] = PitchConverter.absoluteCentToHertz(valueInCents);
				binStartingPointsInCents[i]=(float)valueInCents;
			}
			
			final TreeMap<Double, double[]> fe = new TreeMap<Double, double[]>();
			
			FrequenciesDetectedHandler handler= new FrequenciesDetectedHandler(){
				int i = 0;
				@Override
				public void handleDetectedFrequencies(double time, double[] frequencies,
						double[] powers, double[] allFrequencies,
						double[] allPowers) {
						
						double timeStamp = (Math.max(0, cs.getMin(Axis.X)/1000.0)) + i * binWith;
						i++;
						fe.put(timeStamp,allPowers.clone());
				}};
				
			final GeneralizedGoertzel goertzel = new GeneralizedGoertzel(sampleRate,blockSize,frequencies,handler);
			adp.addAudioProcessor(goertzel);
			adp.run();
			
			for (double[] magnitudes : fe.values()) {
				for (int i = 0; i < magnitudes.length; i++) {
					if(magnitudes[i]==0){
						magnitudes[i] = 1.0/(float)1e10;
					}
					//to dB
					magnitudes[i] = 20 * Math.log(1+Math.abs(magnitudes[i]))/Math.log(10);
					
					maxSpectralEnergy = Math.max(magnitudes[i],maxSpectralEnergy);
					minSpectralEnergy = Math.min(magnitudes[i],minSpectralEnergy);
				}
			}
			minSpectralEnergy = Math.abs(minSpectralEnergy);
			this.features = fe;
			
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e2){
			e2.printStackTrace();
		}
		
		
	}

	@Override
	public String getName() {
		return "Generalized Goertzel Layer";
	}

}
