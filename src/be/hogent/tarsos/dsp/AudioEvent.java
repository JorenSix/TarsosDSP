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

import java.util.Arrays;

import javax.sound.sampled.AudioFormat;

import be.hogent.tarsos.dsp.util.AudioFloatConverter;

/**
 * An audio event flows through the processing pipeline. The object is reused for performance reasons.
 * The arrays with audio information are also reused, so watch out when using the buffer getter and setters. 
 * 
 * @author Joren Six
 */
public class AudioEvent {
	/**
	 * The format specifies a particular arrangement of data in a sound stream. 
	 */
	private final AudioFormat format;
	
	private final AudioFloatConverter converter;
	
	/**
	 * The audio data encoded in floats from -1.0 to 1.0.
	 */
	private float[] floatBuffer;
	
	/**
	 * The audio data encoded in bytes according to format.
	 */
	private byte[] byteBuffer;
	
	/**
	 * The overlap in samples. 
	 */
	private int overlap;
	
	/**
	 * The length of the stream, expressed in sample frames rather than bytes
	 */
	private long frameLength;
	
	/**
	 * The number of bytes processed before this event. It can be used to calculate the time stamp for when this event started.
	 */
	private long bytesProcessed;
	
	
	public AudioEvent(AudioFormat format,long frameLength){
		this.format = format;
		this.converter = AudioFloatConverter.getConverter(format);
		this.overlap = 0;
		this.frameLength = frameLength;
	}
	
	public float getSampleRate(){
		return format.getSampleRate();
	}
	
	public int getBufferSize(){
		return getFloatBuffer().length;
	}
	
	public int getOverlap(){
		return overlap;
	}
	
	public void setOverlap(int newOverlap){
		overlap = newOverlap;
	}
	
	public void setBytesProcessed(long bytesProcessed){
		this.bytesProcessed = bytesProcessed;		
	}
	
	/**
	 * Calculates and returns the time stamp at the beginning of this audio event.
	 * @return The time stamp at the beginning of the event.
	 */
	public double getTimeStamp(){
		return bytesProcessed / format.getFrameSize() / format.getSampleRate();
	}
	
	public long getSamplesProcessed(){
		return bytesProcessed / format.getFrameSize();
	}

	/**
	 * Calculate the progress in percentage of the total number of frames.
	 * 
	 * @return a percentage of processed frames or a negative number if the
	 *         number of frames is not known beforehand.
	 */
	public double getProgress(){
		return bytesProcessed / format.getFrameSize() / (double) frameLength;
	}
	
	/**
	 * Return a byte array with the audio data in bytes.
	 *  A conversion is done from float, cache accordingly on the other side...
	 * 
	 * @return a byte array with the audio data in bytes.
	 */
	public byte[] getByteBuffer(){
		int length = getFloatBuffer().length * format.getFrameSize();
		if(byteBuffer == null || byteBuffer.length != length){
			byteBuffer = new byte[length];
		}
		converter.toByteArray(getFloatBuffer(), byteBuffer);
		return byteBuffer;
	}
	
	public void setFloatBuffer(float[] floatBuffer) {
		this.floatBuffer = floatBuffer;
	}
	
	public float[] getFloatBuffer(){
		return floatBuffer;
	}
	
	/**
	 * Calculates and returns the root mean square of the signal. Please
	 * cache the result since it is calculated every time.
	 * @return The <a
	 *         href="http://en.wikipedia.org/wiki/Root_mean_square">RMS</a> of
	 *         the signal present in the current buffer.
	 */
	public double getRMS() {
		return calculateRMS(floatBuffer);
	}
	
	/**
	 * Calculates and returns the root mean square of the signal. Please
	 * cache the result since it is calculated every time.
	 * @param floatBuffer The audio buffer to calculate the RMS for.
	 * @return The <a
	 *         href="http://en.wikipedia.org/wiki/Root_mean_square">RMS</a> of
	 *         the signal present in the current buffer.
	 */
	public static double calculateRMS(float[] floatBuffer){
		double rms = 0.0;
		for (int i = 0; i < floatBuffer.length; i++) {
			rms =+ floatBuffer[i] * floatBuffer[i];
		}
		rms = rms / Double.valueOf(floatBuffer.length);
		rms = Math.sqrt(rms);
		return rms;
	}

	public void clearFloatBuffer() {
		Arrays.fill(floatBuffer, 0);
	}
	
}
