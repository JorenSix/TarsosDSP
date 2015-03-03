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
import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.util.PitchConverter;

public class SpectrumLayer implements Layer  {
	
	private float[] spectrum;
	private List<Integer> peaksInBins;

	private float multiplier = 10;
	private int sampleRate;
	private int fftSize;
	private CoordinateSystem cs;
	private final Color color;
	
	public SpectrumLayer(CoordinateSystem cs, int fftSize, int sampleRate,Color color){
		this.cs = cs;
		this.sampleRate = sampleRate;
		this.fftSize = fftSize;
		this.color= color;
		peaksInBins = new ArrayList<Integer>();
	}

	@Override
	public void draw(Graphics2D graphics) {
		if(spectrum!=null){
			graphics.setColor(color);
			int prevFreqInCents = 0;
			int prevMagnitude = 0;
						
			for(int i = 1 ; i < spectrum.length ; i++){
				float hertzValue =  (i* sampleRate) / (float) fftSize;
				int frequencyInCents = (int) Math.round(PitchConverter.hertzToAbsoluteCent(hertzValue));
				int magintude = Math.round(spectrum[i] * multiplier);
				if(cs.getMin(Axis.X) < frequencyInCents && frequencyInCents < cs.getMax(Axis.X)){
					graphics.drawLine(prevFreqInCents, prevMagnitude,frequencyInCents, magintude);
					prevFreqInCents = frequencyInCents;
					prevMagnitude = magintude;
				}
			}
			
			int markerWidth = Math.round(LayerUtilities.pixelsToUnits(graphics, 7, true));
			int markerheight = Math.round(LayerUtilities.pixelsToUnits(graphics, 7, false));
			graphics.setColor(Color.blue);
			for(int i = 0 ; i < peaksInBins.size() ; i++){
				int bin =  peaksInBins.get(i) ;
				float hertzValue =  (bin * sampleRate) / (float) fftSize;
				int frequencyInCents = (int) Math.round(PitchConverter.hertzToAbsoluteCent(hertzValue) - markerWidth/2.0f );
				if(cs.getMin(Axis.X) < frequencyInCents && frequencyInCents < cs.getMax(Axis.X)){
					int magintude = Math.round(spectrum[bin] * multiplier - markerheight/2.0f);
					graphics.drawOval(Math.round(frequencyInCents), magintude, markerWidth, markerheight);
				}
			}
		}
	}

	@Override
	public String getName() {
		return "Spectrum";
	}

	public void setSpectrum(float[] spectrum) {
		this.spectrum = spectrum;
	}

	public void setPeak(int binIndex) {
		peaksInBins.add(binIndex);
	}
	
	public void clearPeaks(){
		peaksInBins.clear();
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	public void setFFTSize(int fftSize) {
		this.fftSize = fftSize;
	}
}
