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

package be.tarsos.dsp.pitch;


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
