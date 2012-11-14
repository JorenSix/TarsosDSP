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
 * Utility class to generate Dual-tone multi-frequency (DTMF) signaling tones.
 * This class also contains a list of valid DTMF frequencies and characters.
 * 
 * See the <a href="http://en.wikipedia.org/wiki/Dual-tone_multi-frequency_signaling"
 * >WikiPedia article on DTMF</a>.
 * 
 * @author Joren Six
 */
public class DTMF {

	/**
	 * The list of valid DTMF frequencies. See the <a
	 * href="http://en.wikipedia.org/wiki/Dual-tone_multi-frequency_signaling"
	 * >WikiPedia article on DTMF</a>.
	 */
	public static final double[] DTMF_FREQUENCIES = { 697, 770, 852, 941, 1209,
			1336, 1477, 1633 };

	/**
	 * The list of valid DTMF characters. See the <a
	 * href="http://en.wikipedia.org/wiki/Dual-tone_multi-frequency_signaling"
	 * >WikiPedia article on DTMF</a> for the relation between the characters
	 * and frequencies.
	 */
	public static final char[][] DTMF_CHARACTERS = { { '1', '2', '3', 'A' },
			{ '4', '5', '6', 'B' }, { '7', '8', '9', 'C' },
			{ '*', '0', '#', 'D' } };

	/**
	 * Generate a DTMF - tone for a valid DTMF character. 
	 * @param character a valid DTMF character (present in DTMF_CHARACTERS}
	 * @return a float buffer of predefined length (7168 samples) with the correct DTMF tone representing the character.
	 */
	public static float[] generateDTMFTone(char character){
		double firstFrequency = -1;
		double secondFrequency = -1;
		for(int row = 0 ; row < DTMF_CHARACTERS.length ; row++){
			for(int col = 0 ; col < DTMF_CHARACTERS[row].length ; col++){
				if(DTMF_CHARACTERS[row][col] == character){
					firstFrequency = DTMF_FREQUENCIES[row];
					secondFrequency = DTMF_FREQUENCIES[col + 4];
				}
			}
		}
		return DTMF.audioBufferDTMF(firstFrequency,secondFrequency,512*2*10);
	}
	
	/**
	 * Checks if the given character is present in DTMF_CHARACTERS.
	 * 
	 * @param character
	 *            the character to check.
	 * @return True if the given character is present in
	 *         DTMF_CHARACTERS, false otherwise.
	 */
	public static boolean isDTMFCharacter(char character){
		double firstFrequency = -1;
		double secondFrequency = -1;
		for(int row = 0 ; row < DTMF_CHARACTERS.length ; row++){
			for(int col = 0 ; col < DTMF_CHARACTERS[row].length ; col++){
				if(DTMF_CHARACTERS[row][col] == character){
					firstFrequency = DTMF_FREQUENCIES[row];
					secondFrequency = DTMF_FREQUENCIES[col + 4];
				}
			}
		}
		return (firstFrequency!=-1 && secondFrequency!=-1);
	}

	/**
	 * Creates an audio buffer in a float array of the defined size. The sample
	 * rate is 44100Hz by default. It mixes the two given frequencies with an
	 * amplitude of 0.5.
	 * 
	 * @param f0
	 *            The first fundamental frequency.
	 * @param f1
	 *            The second fundamental frequency.
	 * @param size
	 *            The size of the float array (sample rate is 44.1kHz).
	 * @return An array of the defined size.
	 */
	public static float[] audioBufferDTMF(final double f0, final double f1,
			int size) {
		final double sampleRate = 44100.0;
		final double amplitudeF0 = 0.4;
		final double amplitudeF1 = 0.4;
		final double twoPiF0 = 2 * Math.PI * f0;
		final double twoPiF1 = 2 * Math.PI * f1;
		final float[] buffer = new float[size];
		for (int sample = 0; sample < buffer.length; sample++) {
			final double time = sample / sampleRate;
			double f0Component = amplitudeF0 * Math.sin(twoPiF0 * time);
		    double f1Component = amplitudeF1 * Math.sin(twoPiF1 * time);
		    buffer[sample] = (float) (f0Component + f1Component);
		}
		return buffer;
	}
}
