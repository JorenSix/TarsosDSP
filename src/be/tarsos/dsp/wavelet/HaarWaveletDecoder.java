package be.tarsos.dsp.wavelet;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

public class HaarWaveletDecoder implements AudioProcessor{
	
	private final HaarWaveletTransform transform;
	
	public HaarWaveletDecoder(){
		transform = new HaarWaveletTransform();
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audioBuffer = audioEvent.getFloatBuffer();
		transform.inverseTransform(audioBuffer);
		return true;
	}

	@Override
	public void processingFinished() {
		
	}
	
}
