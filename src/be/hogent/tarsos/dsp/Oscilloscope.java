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

package be.hogent.tarsos.dsp;

/**
 * The oscilloscope generates a float array with 
 * array[i] an x coordinate in percentage
 * array[i+1] the value of the amplitude in audio buffer 
 * array[i+2] another x coordinate in percentage
 * array[i+3] the next amplitude in the audio buffer
 * 
 * The implementation is based on the one by Dan Ellis found at http://www.ee.columbia.edu/~dpwe/resources/Processing/
 * @author Dan Ellis
 * @author Joren Six
 *
 */
public class Oscilloscope implements AudioProcessor {
	public static interface OscilloscopeEventHandler{
		/**
		 * @param data The data contains a float array with: 
		 * array[i] an x coordinate in percentage
		 * array[i+1] the value of the amplitude in audio buffer 
		 * array[i+2] another x coordinate in percentage
		 * array[i+3] the next amplitude in the audio buffer
		 * @param event An audio Event.
		 */
		void handleEvent(float[] data, AudioEvent event);
	}
	float[] dataBuffer;
	private final OscilloscopeEventHandler handler;
	public Oscilloscope(OscilloscopeEventHandler handler){
		this.handler = handler;
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audioBuffer = audioEvent.getFloatBuffer();
		int offset = 0;
		float maxdx = 0;
		for (int i = 0; i < audioBuffer.length / 4; ++i) {
			float dx = audioBuffer[i + 1] - audioBuffer[i];
			if (dx > maxdx) {
				offset = i;
				maxdx = dx;
			}
		}
		
		float tbase = audioBuffer.length / 2;
		

		int length = Math.min((int) tbase, audioBuffer.length-offset);
		if(dataBuffer == null || dataBuffer.length != length * 4){
			dataBuffer = new float[length * 4];
		}
		
		int j = 0;
		for(int i = 0; i < length - 1; i++){
		    float x1 = i / tbase;
		    float x2 = i / tbase;
		    dataBuffer[j] = x1;
		    dataBuffer[j+1] = audioBuffer[i+offset];
		    dataBuffer[j+2] = x2;
		    dataBuffer[j+3] = audioBuffer[i+1+offset];
		    j = j + 4;
		}
		handler.handleEvent(dataBuffer, audioEvent);
		return true;
	}

	@Override
	public void processingFinished() {
	}

}
