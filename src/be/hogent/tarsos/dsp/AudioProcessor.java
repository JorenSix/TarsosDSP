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
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://tarsos.0110.be/releases/TarsosDSP/
*  
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
* 
*/

package be.hogent.tarsos.dsp;

/**
 * <p>
 * AudioProcessors are responsible for actual digital signal processing. The
 * interface is simple: a process method that works on an AudioEvent object. 
 * The AudioEvent contains a buffer with some floats and the same information in
 * raw bytes.
 * </p> 
 * <p>
 * AudioProcessors are meant to be chained e.g. execute an effect and
 * then play the sound. The chain of audio processor can be interrupted by returning
 * false in the process methods.
 * </p>
 * @author Joren Six
 */
public interface AudioProcessor {

	/**
	 * Process the audio event. Do the actual signal processing on an
	 * (optionally) overlapping buffer.
	 * 
	 * @param audioEvent
	 *            The audio event that contains audio data.
	 * @return False if the chain needs to stop here, true otherwise. This can
	 *         be used to implement e.g. a silence detector.
	 */
    boolean process(AudioEvent audioEvent);

    /**
     * Notify the AudioProcessor that no more data is available and processing
     * has finished. Can be used to deallocate resources or cleanup.
     */
    void processingFinished();
}
