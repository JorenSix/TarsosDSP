package be.hogent.tarsos.dsp.example.spectrum;

import java.util.List;

import be.hogent.tarsos.dsp.SpectralPeakProcessor;
import be.hogent.tarsos.dsp.SpectralPeakProcessor.SpectralPeak;

public class SpectralInfo{
	private float[] magnitudes;
	private float[] frequencyEstimates;
	
	public SpectralInfo(float[] magnitudes, float[] frequencyEstimates){
		this.magnitudes = magnitudes;
		this.frequencyEstimates = frequencyEstimates;
	}
	

	public List<SpectralPeak> getPeakList(int medianFilterLength,float noiseFloorFactor,int numberOfPeaks) {
		float[] noiseFloor = getNoiseFloor(medianFilterLength, noiseFloorFactor);
		List<Integer> localMaxima = SpectralPeakProcessor.findLocalMaxima(magnitudes, noiseFloor);
		return SpectralPeakProcessor.findPeaks(magnitudes, frequencyEstimates, localMaxima, numberOfPeaks);
	}

	public float[] getMagnitudes(){
		return magnitudes;
	}
	
	public float[] getNoiseFloor(int medianFilterLength,float noiseFloorFactor){
		return SpectralPeakProcessor.calculateNoiseFloor(magnitudes, medianFilterLength, noiseFloorFactor);
	}
}