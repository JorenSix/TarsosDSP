/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -----------------------------------------------------------
*
*  TarsosDSP is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*  https://github.com/JorenSix/TarsosDSP
*  http://tarsos.0110.be/releases/TarsosDSP/
* 
*/
package be.hogent.tarsos.dsp;

/**
 * Simply stops the audio processing 
 * pipeline if the stop time is reached.
 * @author Joren Six
 *
 */
public class StopAudioProcessor implements AudioProcessor {

	private final double stopTime;
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
}
