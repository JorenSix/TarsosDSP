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
import be.tarsos.dsp.ConstantQ;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.util.PitchConverter;



public class ConstantQLayer implements Layer, Runnable{

	private TreeMap<Double, float[]> features;
	private final CoordinateSystem cs;
	private final File audioFile;
	

	private float maxSpectralEnergy = 0;
	private float minSpectralEnergy = 100000;
	private float[] binStartingPointsInCents;
	private float binWith;// in seconds
	private float binHeight;// in seconds

	/**
	 * The default minimum pitch, in absolute cents (+-66 Hz)
	 */
	private int minimumFrequencyInCents = 4000;
	/**
	 * The default maximum pitch, in absolute cents (+-4200 Hz)
	 */
	private int maximumFrequencyInCents = 10500;
	/**
	 * The default number of bins per octave.
	 */
	private int binsPerOctave = 48;

	/**
	 * The default increment in samples.
	 */
	private int increment;
	

	public ConstantQLayer(CoordinateSystem cs, File audioFile, int increment, int minFreqInCents,int maxFreqInCents, int binsPerOctave) {
		this.cs = cs;		
		this.audioFile = audioFile;		
		this.increment = increment;
		this.minimumFrequencyInCents = minFreqInCents;
		this.maximumFrequencyInCents = maxFreqInCents;
		new Thread(this, "Constant Q Initialization").start();
	}

	public void draw(Graphics2D graphics) {
		if(features != null){
			Map<Double, float[]> spectralInfoSubMap = features.subMap(cs.getMin(Axis.X) / 1000.0, cs.getMax(Axis.X) / 1000.0);
			
						
			double currentMaxSpectralEnergy = 0;
			for (Map.Entry<Double, float[]> column : spectralInfoSubMap.entrySet()) {
				float[] spectralEnergy = column.getValue();
				for (int i = 0; i < spectralEnergy.length; i++) {
					currentMaxSpectralEnergy = Math.max(currentMaxSpectralEnergy, spectralEnergy[i]);
				}
			}

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
						int greyValue = 255 - (int) (Math.log1p(spectralEnergy[i])
								/ Math.log1p(currentMaxSpectralEnergy) * 255);
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

	
	public void run() {
		try {
			
			float minimumFrequencyInHertz = (float) PitchConverter.absoluteCentToHertz(minimumFrequencyInCents);
			float maximumFrequencyInHertz = (float) PitchConverter.absoluteCentToHertz(maximumFrequencyInCents);

			final float sampleRate = AudioDispatcherFactory.fromFile(audioFile, 2048,0).getFormat().getFrameRate();
			
			final ConstantQ constantQ = new ConstantQ(sampleRate,minimumFrequencyInHertz,maximumFrequencyInHertz, binsPerOctave);

			binWith = increment	/ sampleRate;
			binHeight = 1200 / (float) binsPerOctave;

			float[] startingPointsInHertz = constantQ.getFreqencies();
			binStartingPointsInCents = new float[startingPointsInHertz.length];
			for (int i = 0; i < binStartingPointsInCents.length; i++) {
				binStartingPointsInCents[i] = (float) PitchConverter
						.hertzToAbsoluteCent(startingPointsInHertz[i]);
			}
			
			int size = constantQ.getFFTlength();
		
			AudioDispatcher	adp = AudioDispatcherFactory.fromFile(audioFile, size,size-increment);
			final double constantQLag = size / adp.getFormat().getSampleRate() - binWith / 2.0;// in seconds
			final TreeMap<Double, float[]> fe = new TreeMap<Double, float[]>();
			
			adp.addAudioProcessor(constantQ);
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
					ConstantQLayer.this.features = fe;
				}

				public boolean process(AudioEvent audioEvent) {
					fe.put(audioEvent.getTimeStamp() - constantQLag,
							constantQ.getMagnitudes().clone());
					return true;
				}
			});
			new Thread(adp,"Constant Q Calculation").start();
			
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e2){
			e2.printStackTrace();
		}
		
		
	}

	@Override
	public String getName() {
		return "Constant-Q Layer";
	}

}
