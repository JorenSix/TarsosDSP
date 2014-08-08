package be.tarsos.dsp.wavelet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

public class HaarWaveletFileReader implements AudioProcessor {

	private final int compression;
	private FileInputStream rawInputStream;
	
	public HaarWaveletFileReader(String fileName, int compression){
		this.compression = compression;
		try {
			this.rawInputStream = new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			this.rawInputStream = null;
		}
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		
		float[] audioBuffer = new float[32];
		
		byte[] byteBuffer = new byte[(32-compression)*2];
		int placesWithZero = 0;
		try {
			rawInputStream.read(byteBuffer);
			placesWithZero += rawInputStream.read();
			placesWithZero += (rawInputStream.read()<<8);
			placesWithZero += (rawInputStream.read()<<16);
			placesWithZero += (rawInputStream.read()<<24);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		int byteBufferIndex = 0;
		for(int i = 0 ; i < audioBuffer.length ; i++){
			if((placesWithZero & (1<<i)) != 1<<i){
				int x = byteBuffer[byteBufferIndex]  & 0xFF ;
				byteBufferIndex++;
				int y = byteBuffer[byteBufferIndex] << 8 ;
				byteBufferIndex++;
				x = x | y;
				float value = x / 32767.0f;
				audioBuffer[i] = value;
			}
		}
		audioEvent.setFloatBuffer(audioBuffer);
		
		
		boolean more=true;
		try {
			more = rawInputStream.available() > 0;
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		return more;
	}

	@Override
	public void processingFinished() {
		try {
			rawInputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
