package be.tarsos.dsp.wavelet;

import java.util.Arrays;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

public class HaarWaveletCoder implements AudioProcessor{
	
	private final HaarWaveletTransform transform;
	
	private int compression;
	
	public HaarWaveletCoder(){
		this(16);
	}
	
	public HaarWaveletCoder(int compression){
		transform = new HaarWaveletTransform();
		this.compression = compression;
	}
	

	@Override
	public boolean process(AudioEvent audioEvent) {

		float[] audioBuffer = audioEvent.getFloatBuffer();
		float[] sortBuffer = new float[audioBuffer.length];
		transform.transform(audioEvent.getFloatBuffer());

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
	
	public void setCompression(int compression){
		this.compression = compression;
	}
	
	public int getCompression(){
		return this.compression;
	}

}
