package be.tarsos.dsp;

/**
 * Can be used to show the effect of bit depth modification in real-time.
 * It simply transforms every sample to the requested bit depth.
 * @author Joren Six
 */
public class BitDepthProcessor implements AudioProcessor {

	int bitDepth = 16;
			
	public void setBitDepth(int newBitDepth){
		this.bitDepth = newBitDepth;
	}
	
	public int getBitDepth(){
		return this.bitDepth;
	}
	
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] buffer = audioEvent.getFloatBuffer();
		//For e.g. a bith depth of 3, the factor is
		// 2^3 - 1 = 7
		float factor = (float) Math.pow(2, bitDepth)/2.0f - 1;
		
		for(int i = 0 ; i < buffer.length ; i++){
			//the float is scaled to the bith depth
			// e.g. if the bit depth is 3 and the value is 0.3:
			// ((int)(0.3 * 7)) / 7 = 0.28
			buffer[i]=((int) (buffer[i] * factor))/factor;
		}
		return true;
	}

	@Override
	public void processingFinished() {

	}	
}
