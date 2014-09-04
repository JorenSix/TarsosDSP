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

package be.tarsos.dsp.wavelet.lift;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

public class Daubechies4WaveletDecoder implements AudioProcessor {

	private final Daubechies4Wavelet transform;

	public Daubechies4WaveletDecoder() {
		transform = new Daubechies4Wavelet();
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audioBuffer = audioEvent.getFloatBuffer();
		transform.inverseTrans(audioBuffer);
		return true;
	}

	@Override
	public void processingFinished() {

	}

}
