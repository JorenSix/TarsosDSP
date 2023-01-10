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
 * Simply stops the audio processing 
 * pipeline if the stop time is reached.
 * @author Joren Six
 *
 */
public class StopAudioProcessor implements AudioProcessor {

	private double stopTime;
	public StopAudioProcessor(double stopTime){
		this.stopTime = stopTime;
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		return audioEvent.getTimeStamp() <= stopTime;
	}

	@Override
	public void processingFinished() {
		
	}

	public double getStopAt() {
		return stopTime;
	}

	public void setStopTime(double stopTime) {
		this.stopTime = stopTime;		
	}
}
