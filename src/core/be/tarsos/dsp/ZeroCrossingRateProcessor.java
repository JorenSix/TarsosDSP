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
