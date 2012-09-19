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
package be.hogent.tarsos.dsp.pitch;

import be.hogent.tarsos.dsp.AudioEvent;

/**
 * An interface to handle detected pitch.
 * 
 * @author Joren Six
 */
public interface PitchDetectionHandler {
	/**
	 * Handle a detected pitch.
	 * @param pitchDetectionResult 
	 * @param audioEvent
	 * 
	 */
	void handlePitch(PitchDetectionResult pitchDetectionResult,AudioEvent audioEvent);
}
