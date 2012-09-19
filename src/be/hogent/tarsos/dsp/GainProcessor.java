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
 * With the gain processor it is possible to adapt the volume of the sound. With
 * a gain of 1, nothing happens. A gain greater than one is a volume increase a
 * gain between zero and one, exclusive, is a decrease. If you need to flip the
 * sign of the audio samples, you can by providing a gain of -1.0. but I have no
 * idea what you could gain by doing that (pathetic pun, I know).
 * 
 * @author Joren Six
 */
public class GainProcessor implements AudioProcessor {
	private double gain;
	
	public GainProcessor(double newGain) {
		setGain(newGain);
	}

	public void setGain(double newGain) {
		this.gain = newGain;
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audioFloatBuffer = audioEvent.getFloatBuffer();
		for (int i = audioEvent.getOverlap(); i < audioFloatBuffer.length ; i++) {
			float newValue = (float) (audioFloatBuffer[i] * gain);
			if(newValue > 1.0f) {
				newValue = 1.0f;
			} else if(newValue < -1.0f) {
				newValue = -1.0f;
			}
			audioFloatBuffer[i] = newValue;
		}
		return true;
	}
	
	@Override
	public void processingFinished() {
		// NOOP
	}
}
