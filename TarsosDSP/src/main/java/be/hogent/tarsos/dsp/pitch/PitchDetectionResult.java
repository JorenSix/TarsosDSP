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
package be.hogent.tarsos.dsp.pitch;


/**
 * A class with information about the result of a pitch detection on a block of
 * audio.
 * 
 * It contains:
 * 
 * <ul>
 * <li>The pitch in Hertz.</li>
 * <li>A probability (noisiness, (a)periodicity, salience, voicedness or clarity
 * measure) for the detected pitch. This is somewhat similar to the term voiced
 * which is used in speech recognition. This probability should be calculated
 * together with the pitch. The exact meaning of the value depends on the detector used.</li>
 * <li>A way to calculate the RMS of the signal.</li>
 * <li>A boolean that indicates if the algorithm thinks the signal is pitched or
 * not.</li>
 * </ul>
 * 
 * The separate pitched or unpitched boolean can coexist with a defined pitch.
 * E.g. if the algorithm detects 220Hz in a noisy signal it may respond with
 * 220Hz "unpitched".
 * 
 * <p>
 * For performance reasons the object is reused. Please create a copy of the object
 * if you want to use it on an other thread.
 * 
 * 
 * @author Joren Six
 */
public class PitchDetectionResult {	
	/**
	 * The pitch in Hertz.
	 */
	private float pitch;
	
	private float probability;
	
	private boolean pitched;
	
	public PitchDetectionResult(){
		pitch = -1;
		probability = -1;
		pitched = false;
	}
	
	/**
	 * A copy constructor. Since PitchDetectionResult objects are reused for performance reasons, creating a copy can be practical.
	 * @param other
	 */
	public PitchDetectionResult(PitchDetectionResult other){
		this.pitch = other.pitch;
		this.probability = other.probability;
		this.pitched = other.pitched;
	}
		 
	
	/**
	 * @return The pitch in Hertz.
	 */
	public float getPitch() {
		return pitch;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public PitchDetectionResult clone(){
		return new PitchDetectionResult(this);
	}

	/**
	 * @return A probability (noisiness, (a)periodicity, salience, voicedness or
	 *         clarity measure) for the detected pitch. This is somewhat similar
	 *         to the term voiced which is used in speech recognition. This
	 *         probability should be calculated together with the pitch. The
	 *         exact meaning of the value depends on the detector used.
	 */
	public float getProbability() {
		return probability;
	}

	public void setProbability(float probability) {
		this.probability = probability;
	}

	/**
	 * @return Whether the algorithm thinks the block of audio is pitched. Keep
	 *         in mind that an algorithm can come up with a best guess for a
	 *         pitch even when isPitched() is false.
	 */
	public boolean isPitched() {
		return pitched;
	}

	public void setPitched(boolean pitched) {
		this.pitched = pitched;
	}	
}
