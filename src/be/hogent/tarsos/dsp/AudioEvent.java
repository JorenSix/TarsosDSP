package be.hogent.tarsos.dsp;

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
	 * The check buffer is used to check whether data in floatBuffer has changed.
	 * It does this by storing and comparing samples.
	 */
	private float[] checkBuffer;
	
	/**
	 * The audio data encoded in bytes according to format.
	 */
	private byte[] byteBuffer;
	
	/**
	 * The overlap in samples. 
	 */
	private int overlap;
	
	
	
	public AudioEvent(AudioFormat format,int bufferSize,int overlap){
		this.format = format;
		this.converter = AudioFloatConverter.getConverter(format);
		this.overlap = overlap;
		setFloatBuffer(new float[bufferSize]);
		setByteBuffer(new byte[bufferSize * format.getFrameSize()]);
		fillCheckFloatBuffer();
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
		
	/**
	 * Create a new byte array with the audio data in bytes. The conversion is done using the AudioFloatConverter class. Pleas do not call this method too much
	 * @return a new byte array with the audio data in bytes.
	 */
	public byte[] getByteBuffer(){
		if(isDirty()){
			int length = getFloatBuffer().length * format.getFrameSize();
			if(byteBuffer.length != length){
				byteBuffer = new byte[length];
			}
			converter.toByteArray(getFloatBuffer(), byteBuffer);
			//mark this event as 'clean', isDirty() should return false.
			fillCheckFloatBuffer();
		}	
		return byteBuffer;
	}
	
	public void setByteBuffer(byte[] byteBuffer) {
		this.byteBuffer = byteBuffer;
	}
	
	public float[] getFloatBuffer(){
		return floatBuffer;
	}
	
	public void setFloatBuffer(float[] floatBuffer) {
		this.floatBuffer = floatBuffer;
	}
	
	/**
	 * When the float buffer is modified the audio is automatically marked as dirty. When a byteBuffer is requested it is then either created
	 * from the information in the floatBuffer, when dirty, or just returned,
	 * otherwise.
	 *
	 * @return true if the floatBuffer has changed, false otherwise.
	 */
	private boolean isDirty(){
		boolean dirty = false;
		int step = floatBuffer.length / checkBuffer.length;
		for(int i = 0 ; i < checkBuffer.length ; i++){
			if(checkBuffer[i] != floatBuffer[i*step]){
				dirty = true;
			}
		}
		return dirty;
	}
	
	/**
	 * Fill the check float buffer, used to mark this event as dirty automatically.
	 */
	private void fillCheckFloatBuffer(){
		checkBuffer = new float[20];
		int step = floatBuffer.length / checkBuffer.length;
		for(int i = 0 ; i < checkBuffer.length ; i++){
			checkBuffer[i] = floatBuffer[i*step];
		}
	}
}
