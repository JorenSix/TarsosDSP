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

package be.tarsos.dsp.wavelet;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

public class HaarWaveletFileWriter implements AudioProcessor {

	
	private final int compression;
	private FileOutputStream rawOutputStream;
	
	public HaarWaveletFileWriter(String fileName, int compression){
		this.compression = compression;
		try {
			this.rawOutputStream = new FileOutputStream(fileName);
		} catch (FileNotFoundException e) {
			this.rawOutputStream = null;
		}
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audioBuffer = audioEvent.getFloatBuffer();
		
		int placesWithZero = 0;
		int zeroCounter = 0;
		for(int i = 0 ; i < audioBuffer.length ; i++){
			if(audioBuffer[i]==0 && zeroCounter < compression){
				zeroCounter++;
				placesWithZero = placesWithZero | (1<<i);
			}
		}
		
		assert zeroCounter == compression;
				
		
		//16 bits little endian
		byte[] byteBuffer = new byte[(audioBuffer.length - compression) * 2];
		zeroCounter = 0;
		int bufferIndex = 0;
		for (int i = 0; i < byteBuffer.length; i++) {
			float value = audioBuffer[bufferIndex++];
			if(value == 0  && zeroCounter < compression ){
				zeroCounter++;
				i--;
			} else{
				int x = (int) (value * 32767.0);
				byteBuffer[i] = (byte) x;
				i++;
				byteBuffer[i] = (byte) (x >>> 8);	
			}
		}
		
		try {
			rawOutputStream.write(byteBuffer);
			rawOutputStream.write((byte) placesWithZero);
			rawOutputStream.write((byte) (placesWithZero>>>8));
			rawOutputStream.write((byte) (placesWithZero>>>16));
			rawOutputStream.write((byte) (placesWithZero>>>24));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return true;
	}

	@Override
	public void processingFinished() {
		try {
			rawOutputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
