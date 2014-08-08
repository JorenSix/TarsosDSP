package be.hogent.tarsos.dsp.pitch;


/**
 * Implements a pitch tracker by simply locating the most 
 * salient frequency component in a signal. 
 * 
 * @author Joren Six
 */
public class FFTPitch implements PitchDetector {
	
	private final PitchDetectionResult result;
	public FFTPitch(int sampleRate,int bufferSize){
		result = new PitchDetectionResult();
	}

	@Override
	public PitchDetectionResult getPitch(float[] audioBuffer) {
		
		
		return result;
	}
	

}
