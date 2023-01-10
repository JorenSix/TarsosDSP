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

package be.tarsos.dsp;

/**
 * Can be used to show the effect of bit depth modification in real-time.
 * It simply transforms every sample to the requested bit depth.
 * @author Joren Six
 */
public class BitDepthProcessor implements AudioProcessor {

	int bitDepth = 16;

	/**
	 * Set a new bit depth
	 * @param newBitDepth The new bit depth.
	 */
	public void setBitDepth(int newBitDepth){
		this.bitDepth = newBitDepth;
	}

	/**
	 * The current bit depth
	 * @return returns the current bit depth.
	 */
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
