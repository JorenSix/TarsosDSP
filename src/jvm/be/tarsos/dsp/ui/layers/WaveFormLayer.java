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

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;


public class WaveFormLayer implements Layer {

	private final Color waveFormColor;
	private final CoordinateSystem cs; 
	private final File audioFile;
	
	private float[] samples;
	private float sampleRate;
	

	public WaveFormLayer(CoordinateSystem cs,File audioFile) {
		waveFormColor = Color.black;
		this.cs = cs;
		this.audioFile = audioFile;
		initialise();
	}

	public void draw(Graphics2D graphics) {
		graphics.setColor(waveFormColor);
		this.drawWaveForm(graphics);
	}

	private void drawWaveForm(Graphics2D graphics) {
		final int waveFormXMin = (int) cs.getMin(Axis.X);
		final int waveFormXMax = (int) cs.getMax(Axis.X);
		graphics.setColor(Color.GRAY);
		graphics.drawLine(waveFormXMin, 0, waveFormXMax,0);
		graphics.setColor(Color.BLACK);
		if (samples != null && samples.length > 0) {
			//graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		
			final int waveFormHeightInUnits = (int) cs.getDelta(Axis.Y);
			final float lengthInMs = samples.length/sampleRate*1000;
			final int amountOfSamples = samples.length;
			
			float sampleCalculateFactor = amountOfSamples / lengthInMs;
			
			int amplitudeFactor = waveFormHeightInUnits / 2;
			
			//every millisecond:
			int step = 1;
			
			for (int i = Math.max(0, waveFormXMin); i < Math.min(waveFormXMax, lengthInMs); i+= step) {
				int index = (int) (i * sampleCalculateFactor);
				if (index < samples.length) {
					graphics.drawLine(i, 0, i,(int) (samples[index] * amplitudeFactor));
				}
			}
			//graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
		}
	}

	public void initialise() {
		try {			
			AudioDispatcher adp;
			//max 20min
			adp = AudioDispatcherFactory.fromFile(audioFile,44100*60*20, 0);
			adp.setZeroPadLastBuffer(false);
			
			sampleRate = adp.getFormat().getSampleRate();
			adp.addAudioProcessor(new AudioProcessor() {
				public void processingFinished() {
				}
				public boolean process(AudioEvent audioEvent) {
					float[] audioFloatBuffer = audioEvent.getFloatBuffer();
					WaveFormLayer.this.samples = audioFloatBuffer.clone();
					return true;
				}
			});
			new Thread(adp).start();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}

	@Override
	public String getName() {
		return "Waveform layer";
	}
}
