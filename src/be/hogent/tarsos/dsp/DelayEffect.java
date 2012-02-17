/**
*
*  TarsosDSP is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*
**/
package be.hogent.tarsos.dsp;

import javax.sound.sampled.AudioFormat;

import be.hogent.tarsos.dsp.util.AudioFloatConverter;

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
	private int writeIndex;
	private float decay;
	private int overlap;
	
	/**
	 * @param echoLength in seconds
	 * @param sampleRate the sample rate in Hz.
	 * @param decay The decay of the echo, a value between 0 and 1.
	 * @param overlap 
	 */
	public DelayEffect(double echoLength,double sampleRate,double decay, int overlap) {
		this.sampleRate = sampleRate;
		setDecay(decay);
		setEchoLength(echoLength);
		this.overlap = overlap;
	}
	
	public void setEchoLength(double newEchoLength){
		this.echoBuffer = new float[(int) (sampleRate * newEchoLength)];
	}
	
	public void setDecay(double newDecay){
		this.decay = (float) newDecay;
	}

	@Override
	public boolean processFull(float[] audioFloatBuffer, byte[] audioByteBuffer) {
		
		return true;
	}

	@Override
	public boolean processOverlapping(float[] audioFloatBuffer,
			byte[] audioByteBuffer) {
		
		
		for(int i = 0 ; i < audioFloatBuffer.length ; i++){
			echoBuffer[writeIndex] = audioFloatBuffer[i] + echoBuffer[writeIndex] * decay;
			writeIndex++;
			if(writeIndex == echoBuffer.length){
				writeIndex=0;
			}
		}
		
		int readIndex = writeIndex;
		
		for(int i = 0 ; i < audioFloatBuffer.length ; i++){
			audioFloatBuffer[i] = audioFloatBuffer[i] + echoBuffer[readIndex];
			readIndex++;
			if(readIndex == echoBuffer.length){
				readIndex=0;
			}
		}
		
		
		return true;
	}

	@Override
	public void processingFinished() {		
	}

}
