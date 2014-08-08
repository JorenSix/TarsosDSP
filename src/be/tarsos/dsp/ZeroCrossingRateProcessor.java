package be.tarsos.dsp;

/**
 * Calculates the zero crossing rate for a frame.
 * @author Joren Six
 *
 */
public class ZeroCrossingRateProcessor implements AudioProcessor{

	private float zeroCrossingRate = 0;
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] buffer = audioEvent.getFloatBuffer();
		
		int numberOfZeroCrossings = 0;
		for(int i = 1 ; i < buffer.length ; i++){
			if(buffer[i] * buffer[i-1] < 0){
				numberOfZeroCrossings++;
			}
		}
		
		zeroCrossingRate = numberOfZeroCrossings / (float) (buffer.length - 1);
		
		return true;
	}
	
	public float getZeroCrossingRate(){
		return zeroCrossingRate;
	}

	@Override
	public void processingFinished() {
	}

}
