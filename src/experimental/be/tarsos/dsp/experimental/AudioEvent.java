package be.tarsos.dsp.experimental;
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
	
	/**
	 * The audio data encoded in floats from -1.0 to 1.0.
	 */
	private final float[] inputBuffer;
	
	/**
	 * The overlap in samples. 
	 */
	private int overlap;
	
	/**
	 * The number of bytes processed before this event. It can be used to calculate the time stamp for when this event started.
	 */
	private long bytesProcessed;
	
	
	public AudioEvent(TarsosDSPAudioFormat format,float[] floatBuffer){
		this.format = format;
		this.overlap = 0;
		this.inputBuffer = floatBuffer;
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
	 * @return The time stamp at the beginning of the event in seconds.
	 */
	public double getStartTimeStamp(){
		return bytesProcessed / format.getFrameSize() / format.getSampleRate();
	}
	
	public double getEndTimeStamp(){
		return(bytesProcessed / format.getFrameSize()  + inputBuffer.length) / format.getSampleRate();
	}
	
	public long getSamplesProcessed(){
		return bytesProcessed / format.getFrameSize();
	}

	
	public float[] getFloatBuffer(){
		return inputBuffer;
	}
	
	
	public boolean isSilence(double silenceThreshold) {
		return soundPressureLevel(inputBuffer) < silenceThreshold;
	}
	
	/**
	 * Calculates and returns the root mean square of the signal. Please
	 * cache the result since it is calculated every time.
	 * @return The <a
	 *         href="http://en.wikipedia.org/wiki/Root_mean_square">RMS</a> of
	 *         the signal present in the current buffer.
	 */
	public double getRMS() {
		return calculateRMS(inputBuffer);
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
}
