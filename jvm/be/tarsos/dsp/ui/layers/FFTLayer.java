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
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;


public class FFTLayer implements Layer {
	
	private TreeMap<Double, float[]> features;
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
		this.features = new TreeMap<Double, float[]>();
		this.cs = cs;		
		this.audioFile = audioFile;		
		this.frameSize = frameSize;
		this.overlap = overlap;		
		initialise();
	}

	public void draw(Graphics2D graphics) {
		if(features != null){
			Map<Double, float[]> spectralInfoSubMap = features.subMap(
					cs.getMin(Axis.X) / 1000.0, cs.getMax(Axis.X) / 1000.0);
			for (Map.Entry<Double, float[]> column : spectralInfoSubMap.entrySet()) {
				double timeStart = column.getKey();// in seconds
				float[] spectralEnergy = column.getValue();// in cents
	
				// draw the pixels
				for (int i = 0; i < spectralEnergy.length; i++) {
					Color color = Color.black; 
					float centsStartingPoint = binStartingPointsInCents[i];
					// only draw the visible frequency range
					if (centsStartingPoint >= cs.getMin(Axis.Y)
							&& centsStartingPoint <= cs.getMax(Axis.Y)) {
						
						int greyValue = 255 - (int) (spectralEnergy[i]
								/ maxSpectralEnergy * 255);
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

	public void initialise() {
		
		try {
			AudioDispatcher adp = AudioDispatcherFactory.fromFile(audioFile, frameSize,overlap);
			float sampleRate = adp.getFormat().getSampleRate();
			final TreeMap<Double, float[]> fe = new TreeMap<Double, float[]>();
			binWith = increment / sampleRate;

			final FFT fft = new FFT(frameSize);
			
			binStartingPointsInCents = new float[frameSize];
			binHeightsInCents = new float[frameSize];
			for (int i = 1; i < frameSize; i++) {
				binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(fft.binToHz(i,sampleRate));
				binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i-1];
			}
			
			final double lag =  frameSize / sampleRate - binWith / 2.0;// in seconds
			
			adp.addAudioProcessor(new AudioProcessor() {

				public void processingFinished() {
					float minValue = 5 / 1000000.0f;
					for (float[] magnitudes : fe.values()) {
						for (int i = 0; i < magnitudes.length; i++) {
							magnitudes[i] = Math.max(minValue, magnitudes[i]);
							magnitudes[i] = (float) Math.log1p(magnitudes[i]);
							maxSpectralEnergy = Math.max(magnitudes[i],
									maxSpectralEnergy);
							minSpectralEnergy = Math.min(magnitudes[i],
									minSpectralEnergy);
						}
					}
					minSpectralEnergy = Math.abs(minSpectralEnergy);
					FFTLayer.this.features = fe;
				}

				public boolean process(AudioEvent audioEvent) {
					float[] buffer = audioEvent.getFloatBuffer().clone();
					float[] amplitudes = new float[buffer.length/2];
					fft.forwardTransform(buffer);
					fft.modulus(buffer, amplitudes);
					fe.put(audioEvent.getTimeStamp() - lag,amplitudes);
					return true;
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

}
