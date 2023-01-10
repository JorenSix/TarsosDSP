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

	/**
	 * Creates a new audio event with a certain audio format
	 * @param format The format to use to convert from bytes to floats.
	 */
	public AudioEvent(TarsosDSPAudioFormat format){
		this.format = format;
		this.converter = TarsosDSPAudioFloatConverter.getConverter(format);
		this.overlap = 0;
	}

	/**
	 * The audio sample rate in Hz
	 * @return  The audio sample rate in Hz
	 */
	public float getSampleRate(){
		return format.getSampleRate();
	}

	/**
	 * The size of the buffer in samples.
	 * @return The size of the buffer in samples.
	 */
	public int getBufferSize(){
		return getFloatBuffer().length;
	}
	
	/**
	 * The length of the stream, expressed in sample frames rather than byte
	 * @return  The length of the stream, expressed in sample frames rather than bytes
	 */
	public long getFrameLength(){
		return frameLength;
	}

	/**
	 * The overlap in samples between blocks of audio
	 * @return The overlap in samples between blocks of audio
	 */
	public int getOverlap(){
		return overlap;
	}

	/**
	 * Change the default overlap (in samples)
	 * @param newOverlap The new overlap between audio blocks.
	 */
	public void setOverlap(int newOverlap){
		overlap = newOverlap;
	}

	/**
	 * Change the number of bytes processed
	 *
	 * @param bytesProcessed The number of bytes processed.
	 */
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

	/**
	 * The timestamp at the end of the buffer (in seconds)
	 * @return The timestamp at the end of the buffer (in seconds)
	 */
	public double getEndTimeStamp(){
		return(bytesProcessed + bytesProcessing) / format.getFrameSize() / format.getSampleRate();
	}

	/**
	 * The number of samples processed.
	 * @return The number of samples processed.
	 */
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

	/**
	 * Set a new audio block.
	 * @param floatBuffer The audio block that is passed to the next processor.
	 */
	public void setFloatBuffer(float[] floatBuffer) {
		this.floatBuffer = floatBuffer;
	}

	/**
	 * The audio block in floats
	 * @return The float representation of the audio block.
	 */
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

	/**
	 * Set all sample values to zero.
	 */
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
	private static double soundPressureLevel(final float[] buffer) {
		double rms = calculateRMS(buffer);
		return linearToDecibel(rms);
	}
	
	/**
	 * Converts a linear to a dB value.
	 * 
	 * @param value
	 *            The value to convert.
	 * @return The converted value.
	 */
	private static double linearToDecibel(final double value) {
		return 20.0 * Math.log10(value);
	}

	/**
	 * Checks whether this block of audio is silent
	 * @param silenceThreshold the threshold in spl to use.
	 * @return True if SPL is below the threshold. False otherwise.
	 */
	public boolean isSilence(double silenceThreshold) {
		return soundPressureLevel(floatBuffer) < silenceThreshold;
	}

	/**
	 * The number of bytes being processed.
	 * @param bytesProcessing Sets the number of bytes being processed.
	 */
	public void setBytesProcessing(int bytesProcessing) {
		this.bytesProcessing = bytesProcessing;
		
	}
	
}
