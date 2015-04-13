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

package be.tarsos.dsp.example.spectrum;

import java.util.List;

import be.tarsos.dsp.SpectralPeakProcessor;
import be.tarsos.dsp.SpectralPeakProcessor.SpectralPeak;

public class SpectralInfo{
	private float[] magnitudes;
	private float[] frequencyEstimates;
	
	public SpectralInfo(float[] magnitudes, float[] frequencyEstimates){
		this.magnitudes = magnitudes;
		this.frequencyEstimates = frequencyEstimates;
	}
	
	public List<SpectralPeak> getPeakList(int medianFilterLength,float noiseFloorFactor,int numberOfPeaks,int minPeakDistanceInCents) {
		float[] noiseFloor = getNoiseFloor(medianFilterLength, noiseFloorFactor);
		List<Integer> localMaxima = SpectralPeakProcessor.findLocalMaxima(magnitudes, noiseFloor);
		return SpectralPeakProcessor.findPeaks(magnitudes, frequencyEstimates, localMaxima, numberOfPeaks,minPeakDistanceInCents);
	}

	public float[] getMagnitudes(){
		return magnitudes;
	}
	
	public float[] getNoiseFloor(int medianFilterLength,float noiseFloorFactor){
		return SpectralPeakProcessor.calculateNoiseFloor(magnitudes, medianFilterLength, noiseFloorFactor);
	}
}
