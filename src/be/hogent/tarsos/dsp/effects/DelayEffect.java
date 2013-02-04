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

package be.hogent.tarsos.dsp.effects;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;


/**
 * <p>
 * Adds an echo effect to the signal.
 * </p>
 * 
 * @author Joren Six
 */
public class DelayEffect implements AudioProcessor {
	
	private double sampleRate;
	private float[] echoBuffer;//in seconds
	private int position;
	private float decay;
	
	private double newEchoLength;
	
	/**
	 * @param echoLength in seconds
	 * @param sampleRate the sample rate in Hz.
	 * @param decay The decay of the echo, a value between 0 and 1. 1 meaning no decay, 0 means immediate decay (not echo effect).
	 */
	public DelayEffect(double echoLength,double decay,double sampleRate) {
		this.sampleRate = sampleRate;
		setDecay(decay);
		setEchoLength(echoLength);
		applyNewEchoLength();	
	}
	
	/**
	 * @param newEchoLength A new echo buffer length in seconds.
	 */
	public void setEchoLength(double newEchoLength){
		this.newEchoLength = newEchoLength;
	}
	
	private void applyNewEchoLength(){
		if(newEchoLength != -1){
			
			//create a new buffer with the information of the previous buffer
			float[] newEchoBuffer = new float[(int) (sampleRate * newEchoLength)];
			if(echoBuffer != null){
				for(int i = 0 ; i < newEchoBuffer.length; i++){
					if(position >= echoBuffer.length){
						position = 0;
					}
					newEchoBuffer[i] = echoBuffer[position];
					position++;
				}
			}
			this.echoBuffer = newEchoBuffer;
			newEchoLength = -1;
		}
	}
	
	/**
	 * A decay, should be a value between zero and one.
	 * @param newDecay the new decay (preferably between zero and one).
	 */
	public void setDecay(double newDecay){
		this.decay = (float) newDecay;
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audioFloatBuffer = audioEvent.getFloatBuffer();
		int overlap = audioEvent.getOverlap();
			
		for(int i = overlap ; i < audioFloatBuffer.length ; i++){
			if(position >= echoBuffer.length){
				position = 0;
			}
			
			//output is the input added with the decayed echo 		
			audioFloatBuffer[i] = audioFloatBuffer[i] + echoBuffer[position] * decay;
			//store the sample in the buffer;
			echoBuffer[position] = audioFloatBuffer[i];
			
			position++;
		}
		
		applyNewEchoLength();
		
		return true;
	}

	@Override
	public void processingFinished() {		
	}
}
