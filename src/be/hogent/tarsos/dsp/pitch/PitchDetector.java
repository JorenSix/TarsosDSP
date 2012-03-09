/*
*
*  TarsosDSP is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*
*/

package be.hogent.tarsos.dsp.pitch;

/**
 * A pitch detector is capable of analyzing a buffer with audio information
 * and return a pitch estimation in Hz.
 * 
 * @author Joren Six
 */
public interface PitchDetector {
	/**
	 * Analyzes a buffer with audio information and estimates a pitch in Hz.
	 * Currently this interface only allows one pitch per buffer.
	 * 
	 * @param audioBuffer
	 *            The buffer with audio information. The information in the
	 *            buffer is not modified so it can be (re)used for e.g. FFT
	 *            analysis.
	 * @return An estimation of the pitch in Hz or -1 if no pitch is detected or
	 *         present in the buffer.
	 */
	float getPitch(final float[] audioBuffer);
	
	/**
	 * Some algorithms can calculate a probability (noisiness, (a)periodicity,
	 * salience, voicedness or clarity measure) for the detected pitch. This is
	 * somewhat similar to the term voiced which is used in speech recognition.
	 * This probability should be calculated together with the pitch but is
	 * returned using a call to this method. So if you want the probability of a
	 * buffer: first call getPitch(buffer) and then getProbability().
	 * 
	 * 
	 * @return A probability
	 */
	float getProbability();

}
