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

import java.util.Arrays;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

public class Daubechies4WaveletCoder implements AudioProcessor {

	private final Daubechies4Wavelet transform;

	private int compression;

	public Daubechies4WaveletCoder() {
		this(16);
	}

	public Daubechies4WaveletCoder(int compression) {
		transform = new Daubechies4Wavelet();
		this.compression = compression;
	}

	@Override
	public boolean process(AudioEvent audioEvent) {

		float[] audioBuffer = audioEvent.getFloatBuffer();
		float[] sortBuffer = new float[audioBuffer.length];

		transform.forwardTrans(audioBuffer);

		for (int i = 0; i < sortBuffer.length; i++) {
			sortBuffer[i] = Math.abs(audioBuffer[i]);
		}
		Arrays.sort(sortBuffer);

		double threshold = sortBuffer[compression];

		for (int i = 0; i < audioBuffer.length; i++) {
			if (Math.abs(audioBuffer[i]) <= threshold) {
				audioBuffer[i] = 0;
			}
		}
		return true;
	}

	@Override
	public void processingFinished() {

	}

	public void setCompression(int compression) {
		this.compression = compression;
	}

	public int getCompression() {
		return this.compression;
	}

}
