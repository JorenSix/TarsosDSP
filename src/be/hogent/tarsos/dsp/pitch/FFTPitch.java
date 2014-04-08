package be.hogent.tarsos.dsp.pitch;

import be.hogent.tarsos.dsp.SpectrumProcessor;

/**
 * Implements a pitch tracker by simply locating the most 
 * salient frequency component in a signal. 
 * 
 * @author Joren Six
 */
public class FFTPitch implements PitchDetector {
	
	private final SpectrumProcessor spectrumExtractor; 
	private final PitchDetectionResult result;
	public FFTPitch(int sampleRate,int bufferSize){
		spectrumExtractor = new SpectrumProcessor(bufferSize, 0, sampleRate);
		result = new PitchDetectionResult();
	}

	@Override
	public PitchDetectionResult getPitch(float[] audioBuffer) {
		spectrumExtractor.process(audioBuffer);
		
		float[] magnitudes = spectrumExtractor.getMagnitudes();
		float[] frequencies = spectrumExtractor.getFrequencies();
		
		float maxMagnitude = -5000;
		float pitch = 0;
		for(int i = 4 ; i < magnitudes.length-4;i++){
			if(magnitudes[i] > maxMagnitude){
				pitch = frequencies[i];
				maxMagnitude = magnitudes[i];
			}
		}
		result.setPitch(pitch);
		
		return result;
	}
	

}
