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
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.layers.TooltipLayer.TooltipTextGenerator;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;


public class FFTLayer implements Layer, TooltipTextGenerator {
	
	private TreeMap<Double, FFTFrame> features;
	private final CoordinateSystem cs; 
	private final int frameSize;
	private final int overlap;
	private final File audioFile;
	
	
	private float binWith;// in seconds
	
	private float maxSpectralEnergy = 0;
	private float minSpectralEnergy = 100000;
	private float[] binStartingPointsInCents;
	private float[] binHeightsInCents;

	/**
	 * The default increment in samples.
	 */
	private int increment;

	public FFTLayer(CoordinateSystem cs, File audioFile , int frameSize, int overlap) {
		increment = frameSize - overlap;
		this.features = new TreeMap<Double, FFTFrame>();
		this.cs = cs;		
		this.audioFile = audioFile;		
		this.frameSize = frameSize;
		this.overlap = overlap;		
		initialise();
	}

	public void draw(Graphics2D graphics) {
		if(features != null){
			Map<Double, FFTFrame> spectralInfoSubMap = features.subMap(
					cs.getMin(Axis.X) / 1000.0, cs.getMax(Axis.X) / 1000.0);
			for (Map.Entry<Double, FFTFrame> frameEntry : spectralInfoSubMap.entrySet()) {
				double timeStart = frameEntry.getKey();// in seconds
				FFTFrame frame = frameEntry.getValue();// in cents
				
			
				// draw the pixels
				for (int i = 0; i < frame.magnitudes.length; i++) {
					Color color = Color.black;
					
					//actual energy at frame.frequencyEstimates[i];
					
					float centsStartingPoint = binStartingPointsInCents[i];
					// only draw the visible frequency range
					if (centsStartingPoint >= cs.getMin(Axis.Y)
							&& centsStartingPoint <= cs.getMax(Axis.Y)) {
						float factor = (frame.magnitudes[i] - frame.getMinMagnitude()) / (frame.getMaxMagnitude() - frame.getMinMagnitude());
						int greyValue = 255 - (int) ( factor* 255);
						greyValue = Math.max(0, greyValue);
						color = new Color(greyValue, greyValue, greyValue);
						graphics.setColor(color);
						graphics.fillRect((int) Math.round(timeStart * 1000),
								Math.round(centsStartingPoint),
								(int) Math.round(binWith * 1000),
								(int) Math.ceil(binHeightsInCents[i]));
					}
				}
			}
		}
	}
	
	private static class FFTFrame{
		
		private float[] magnitudes; 
		private float[] currentPhaseOffsets;
		private float[] previousPhaseOffsets;
		private FFT fft;
		private float[] frequencyEstimates;
		
		/**
		 * Cached calculations for the frequency calculation
		 */
		private final double dt;
		private final double cbin;
		private final double inv_2pi;
		private final double inv_deltat;
		private final double inv_2pideltat;
		private float sampleRate;
		
		private float minMagnitude;
		private float maxMagnitude;
		
		
	
		public FFTFrame(FFT fft, int bufferSize,int overlap, float sampleRate,float[] magnitudes, float[] currentPhaseOffsets,float[] previousPhaseOffsets){
			this.fft = fft;
			this.magnitudes = magnitudes;
			this.currentPhaseOffsets = currentPhaseOffsets;
			this.previousPhaseOffsets = previousPhaseOffsets;
			this.frequencyEstimates = new float[magnitudes.length];
			
			
			
			dt = (bufferSize - overlap) / (double) sampleRate;
			cbin = (double) (dt * sampleRate / (double) bufferSize);
			
			this.sampleRate = sampleRate;

			inv_2pi = (double) (1.0 / (2.0 * Math.PI));
			inv_deltat = (double) (1.0 / dt);
			inv_2pideltat = (double) (inv_deltat * inv_2pi);
			
			calculateFrequencyEstimates();
			convertMagnitudesToDecibel();
		}
		
		private void convertMagnitudesToDecibel(){
			float minValue = 5 / 1000000.0f;
			for (int i = 0; i < magnitudes.length; i++) {
				//if(magnitudes[i]==0){
				//	magnitudes[i]=minValue;
				//}
				double value = 1 + magnitudes[i];
				if(value <= 0){
					value = 1+minValue;
				}
				magnitudes[i] = (float) (Math.abs(20 * Math.log10(value)));
			}
		}
		
		/**
		 * For each bin, calculate a precise frequency estimate using phase offset.
		 */
		private void calculateFrequencyEstimates() {
			for(int i = 0;i < frequencyEstimates.length;i++){
				frequencyEstimates[i] = getFrequencyForBin(i);
			}
		}
		
		/*
		public float[] getFrequencyEstimates(){
			return frequencyEstimates;
		}
		*/
		
		public float calculateMinMagnitude(){
			float minMag = 4654654;
			for (int i = 0; i < magnitudes.length; i++) {
				minMag = Math.min(minMag, magnitudes[i]);
			}
			return minMag;
		}
		
		public float calculateMaxMagnitude(){
			float maxMag = -1654654;
			for (int i = 0; i < magnitudes.length; i++) {
				maxMag = Math.max(maxMag, magnitudes[i]);
			}
			return maxMag;
		}
		
		
		public float getMaxMagnitude() {
			return maxMagnitude;
		}

		public void setMaxMagnitude(float maxMagnitude) {
			this.maxMagnitude = maxMagnitude;
		}
		
		public float getMinMagnitude() {
			return minMagnitude;
		}

		public void setMinMagnitude(float minMagnitude) {
			this.minMagnitude = minMagnitude;
		}

		
		
		/**
		 * Calculates a frequency for a bin using phase info, if available.
		 * @param binIndex The FFT bin index.
		 * @return a frequency, in Hz, calculated using available phase info.
		 */
		private float getFrequencyForBin(int binIndex){
			final float frequencyInHertz;
			// use the phase delta information to get a more precise
			// frequency estimate
			// if the phase of the previous frame is available.
			// See
			// * Moore 1976
			// "The use of phase vocoder in computer music applications"
			// * Sethares et al. 2009 - Spectral Tools for Dynamic
			// Tonality and Audio Morphing
			// * Laroche and Dolson 1999
			if (previousPhaseOffsets != null) {
				float phaseDelta = currentPhaseOffsets[binIndex] - previousPhaseOffsets[binIndex];
				long k = Math.round(cbin * binIndex - inv_2pi * phaseDelta);
				frequencyInHertz = (float) (inv_2pideltat * phaseDelta  + inv_deltat * k);
			} else {
				frequencyInHertz = (float) fft.binToHz(binIndex, sampleRate);
			}
			return frequencyInHertz;
		}
		
	};

	public void initialise() {
		
		try {
			AudioDispatcher adp = AudioDispatcherFactory.fromFile(audioFile, frameSize,overlap);
			final float sampleRate = adp.getFormat().getSampleRate();
			final TreeMap<Double, FFTFrame> fe = new TreeMap<Double, FFTFrame>();
			binWith = increment / sampleRate;

			final FFT fft = new FFT(frameSize,new HammingWindow());
			
			binStartingPointsInCents = new float[frameSize];
			binHeightsInCents = new float[frameSize];
			for (int i = 1; i < frameSize; i++) {
				binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(fft.binToHz(i,sampleRate));
				binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i-1];
			}
			
			final double lag =  frameSize / sampleRate - binWith / 2.0;// in seconds
			
			adp.addAudioProcessor(new AudioProcessor() {

				float[] previousPhaseOffsets = null;
				
				public boolean process(AudioEvent audioEvent) {
					float[] buffer = audioEvent.getFloatBuffer().clone();
					float[] amplitudes = new float[buffer.length/2];
					float[] phases = new float[buffer.length/2];
									
					// Extract the power and phase data
					fft.powerPhaseFFT(buffer, amplitudes, phases);
					
					FFTFrame frame = new FFTFrame(fft, frameSize, overlap, sampleRate, amplitudes, phases, previousPhaseOffsets);
					previousPhaseOffsets = phases;
					
					fe.put(audioEvent.getTimeStamp() - lag,frame);
					return true;
				}
				
				public void processingFinished() {
					float decay = 0.99f;
					float ramp = 1.01f;
					for (FFTFrame frame : fe.values()) {
						
						maxSpectralEnergy = Math.max(frame.calculateMaxMagnitude(), maxSpectralEnergy);
						frame.setMaxMagnitude(maxSpectralEnergy);
						
						minSpectralEnergy = Math.min(frame.calculateMinMagnitude(), minSpectralEnergy);
						frame.setMinMagnitude(minSpectralEnergy);
						
						maxSpectralEnergy = maxSpectralEnergy * decay;
						minSpectralEnergy = minSpectralEnergy * ramp;
					}
					FFTLayer.this.features = fe;
				}
			});
			new Thread(adp,"Calculate FFT").start();
			
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e2){
			e2.printStackTrace();
		}		
		
	}

	@Override
	public String getName() {
		return "FFT Layer";
	}

	@Override
	public String generateTooltip(CoordinateSystem cs, Point2D point) {
		String tooltip = "";
		if(features!=null){
			double timestampInSeconds = point.getX()/1000.0;
			Map.Entry<Double,FFTFrame> ceilingEntry = features.ceilingEntry(timestampInSeconds);
			Map.Entry<Double,FFTFrame> floorEntry = features.floorEntry(timestampInSeconds);
			double diffToFloor = Math.abs(floorEntry.getKey() - timestampInSeconds);
			double diffToCeil = Math.abs(floorEntry.getKey() - timestampInSeconds);
			final Map.Entry<Double,FFTFrame> entry;
			if(diffToCeil > diffToFloor){
				entry = floorEntry;
			}else{
				entry = ceilingEntry; 
			}
			FFTFrame frame = entry.getValue();
			int binIndex=0;
			for(int i = 0 ; i < binStartingPointsInCents.length ; i++){
				if(binStartingPointsInCents[i] > point.getY() && binIndex == 0){
					binIndex = i-1;
				}
			}
			float frequency = frame.getFrequencyForBin(binIndex);
			
			
			//double binSize = binStartingPointsInCents[binIndex+1] - binStartingPointsInCents[binIndex];
			
			tooltip = String.format("Bin: %d  Estimated Frequency: %.02fHz  Time: %.03fs  ",binIndex,frequency,timestampInSeconds);
			
		}
		return tooltip;
	}

}
