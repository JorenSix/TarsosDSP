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


package be.tarsos.dsp;

import java.util.Arrays;

import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

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
	private final TarsosDSPAudioFormat format;
	
	private final TarsosDSPAudioFloatConverter converter;
	
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

	private int bytesProcessing;
	
	
	public AudioEvent(TarsosDSPAudioFormat format){
		this.format = format;
		this.converter = TarsosDSPAudioFloatConverter.getConverter(format);
		this.overlap = 0;
	}
	
	public float getSampleRate(){
		return format.getSampleRate();
	}
	
	public int getBufferSize(){
		return getFloatBuffer().length;
	}
	
	/**
	 * @return  The length of the stream, expressed in sample frames rather than bytes
	 */
	public long getFrameLength(){
		return frameLength;
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
	 * @return The time stamp at the beginning of the event in seconds.
	 */
	public double getTimeStamp(){
		return bytesProcessed / format.getFrameSize() / format.getSampleRate();
	}
	
	public double getEndTimeStamp(){
		return(bytesProcessed + bytesProcessing) / format.getFrameSize() / format.getSampleRate();
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
	 * Returns the dBSPL for a buffer.
	 * 
	 * @return The dBSPL level for the buffer.
	 */
	public double getdBSPL() {
		return soundPressureLevel(floatBuffer);
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
			rms += floatBuffer[i] * floatBuffer[i];
		}
		rms = rms / Double.valueOf(floatBuffer.length);
		rms = Math.sqrt(rms);
		return rms;
	}

	public void clearFloatBuffer() {
		Arrays.fill(floatBuffer, 0);
	}

		/**
	 * Returns the dBSPL for a buffer.
	 * 
	 * @param buffer
	 *            The buffer with audio information.
	 * @return The dBSPL level for the buffer.
	 */
	private double soundPressureLevel(final float[] buffer) {
		double value = Math.pow(localEnergy(buffer), 0.5);
		value = value / buffer.length;
		return linearToDecibel(value);
	}
	
	/**
	 * Calculates the local (linear) energy of an audio buffer.
	 * 
	 * @param buffer
	 *            The audio buffer.
	 * @return The local (linear) energy of an audio buffer.
	 */
	private double localEnergy(final float[] buffer) {
		double power = 0.0D;
		for (float element : buffer) {
			power += element * element;
		}
		return power;
	}
	
	/**
	 * Converts a linear to a dB value.
	 * 
	 * @param value
	 *            The value to convert.
	 * @return The converted value.
	 */
	private double linearToDecibel(final double value) {
		return 20.0 * Math.log10(value);
	}

	public boolean isSilence(double silenceThreshold) {
		return soundPressureLevel(floatBuffer) < silenceThreshold;
	}

	public void setBytesProcessing(int bytesProcessing) {
		this.bytesProcessing = bytesProcessing;
		
	}
	
}
