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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;


/**
 * This class plays a file and sends float arrays to registered AudioProcessor
 * implementors. This class can be used to feed FFT's, pitch detectors, audio players, ...
 * Using a (blocking) audio player it is even possible to synchronize execution of
 * AudioProcessors and sound. This behavior can be used for visualization.
 * @author Joren Six
 */
public class AudioDispatcher implements Runnable {


	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(AudioDispatcher.class.getName());

	/**
	 * The audio stream (in bytes), conversion to float happens at the last
	 * moment.
	 */
	private final TarsosDSPAudioInputStream audioInputStream;

	/**
	 * This buffer is reused again and again to store audio data using the float
	 * data type.
	 */
	private float[] audioFloatBuffer;

	/**
	 * This buffer is reused again and again to store audio data using the byte
	 * data type.
	 */
	private byte[] audioByteBuffer;

	/**
	 * A list of registered audio processors. The audio processors are
	 * responsible for actually doing the digital signal processing
	 */
	private final List<AudioProcessor> audioProcessors;

	/**
	 * Converter converts an array of floats to an array of bytes (and vice
	 * versa).
	 */
	private final TarsosDSPAudioFloatConverter converter;
	
	private final TarsosDSPAudioFormat format;

	/**
	 * The floatOverlap: the number of elements that are copied in the buffer
	 * from the previous buffer. Overlap should be smaller (strict) than the
	 * buffer size and can be zero. Defined in number of samples.
	 */
	private int floatOverlap, floatStepSize;

	/**
	 * The overlap and stepsize defined not in samples but in bytes. So it
	 * depends on the bit depth. Since the int datatype is used only 8,16,24,...
	 * bits or 1,2,3,... bytes are supported.
	 */
	private int byteOverlap, byteStepSize;
	
	
	/**
	 * The number of bytes to skip before processing starts.
	 */
	private long bytesToSkip;
	
	/**
	 * Position in the stream in bytes. e.g. if 44100 bytes are processed and 16
	 * bits per frame are used then you are 0.5 seconds into the stream.
	 */
	private long bytesProcessed;
	
	
	/**
	 * The audio event that is send through the processing chain.
	 */
	private AudioEvent audioEvent;
	
	/**
	 * If true the dispatcher stops dispatching audio.
	 */
	private boolean stopped;
	
	/**
	 * If true then the first buffer is only filled up to buffer size - hop size
	 * E.g. if the buffer is 2048 and the hop size is 48 then you get 2000 times
	 * zero 0 and 48 actual audio samples. During the next iteration you get
	 * mostly zeros and 96 samples.
	 */
	private boolean zeroPadFirstBuffer;
	
	/**
	 * If true then the last buffer is zero padded. Otherwise the buffer is
	 * shortened to the remaining number of samples. If false then the audio
	 * processors must be prepared to handle shorter audio buffers.
	 */
	private boolean zeroPadLastBuffer;

	/**
	 * Create a new dispatcher from a stream.
	 * 
	 * @param stream
	 *            The stream to read data from.
	 * @param audioBufferSize
	 *            The size of the buffer defines how much samples are processed
	 *            in one step. Common values are 1024,2048.
	 * @param bufferOverlap
	 *            How much consecutive buffers overlap (in samples). Half of the
	 *            AudioBufferSize is common (512, 1024) for an FFT.
	 */
	public AudioDispatcher(final TarsosDSPAudioInputStream stream, final int audioBufferSize, final int bufferOverlap){
		// The copy on write list allows concurrent modification of the list while
		// it is iterated. A nice feature to have when adding AudioProcessors while
		// the AudioDispatcher is running.
		audioProcessors = new CopyOnWriteArrayList<AudioProcessor>();
		audioInputStream = stream;

		format = audioInputStream.getFormat();
		
			
		setStepSizeAndOverlap(audioBufferSize, bufferOverlap);
		
		audioEvent = new AudioEvent(format);
		audioEvent.setFloatBuffer(audioFloatBuffer);
		audioEvent.setOverlap(bufferOverlap);
		
		converter = TarsosDSPAudioFloatConverter.getConverter(format);
		
		stopped = false;
		
		bytesToSkip = 0;
		
		zeroPadLastBuffer = true;
	}	
	
	/**
	 * Skip a number of seconds before processing the stream.
	 * @param seconds The number of seconds to skip
	 */
	public void skip(double seconds){
		bytesToSkip = Math.round(seconds * format.getSampleRate()) * format.getFrameSize(); 
	}
	
	/**
	 * Set a new step size and overlap size. Both in number of samples. Watch
	 * out with this method: it should be called after a batch of samples is
	 * processed, not during.
	 * 
	 * @param audioBufferSize
	 *            The size of the buffer defines how much samples are processed
	 *            in one step. Common values are 1024,2048.
	 * @param bufferOverlap
	 *            How much consecutive buffers overlap (in samples). Half of the
	 *            AudioBufferSize is common (512, 1024) for an FFT.
	 */
	public void setStepSizeAndOverlap(final int audioBufferSize, final int bufferOverlap){
		audioFloatBuffer = new float[audioBufferSize];
		floatOverlap = bufferOverlap;
		floatStepSize = audioFloatBuffer.length - floatOverlap;

		audioByteBuffer = new byte[audioFloatBuffer.length * format.getFrameSize()];
		byteOverlap = floatOverlap * format.getFrameSize();
		byteStepSize = floatStepSize * format.getFrameSize();
	}
	
	/**
	 * if zero pad is true then the first buffer is only filled up to  buffer size - hop size
	 * E.g. if the buffer is 2048 and the hop size is 48 then you get 2000x0 and 48 filled audio samples
	 * @param zeroPadFirstBuffer true if the buffer should be zeroPadFirstBuffer, false otherwise.
	 */
	public void setZeroPadFirstBuffer(boolean zeroPadFirstBuffer){
		this.zeroPadFirstBuffer = zeroPadFirstBuffer;		
	}
	
	/**
	 * If zero pad last buffer is true then the last buffer is filled with zeros until the normal amount
	 * of elements are present in the buffer. Otherwise the buffer only contains the last elements and no zeros.
	 * By default it is set to true.
	 * 
	 * @param zeroPadLastBuffer A boolean to control whether the last buffer is zero-padded.
	 */
	public void setZeroPadLastBuffer(boolean zeroPadLastBuffer) {
		this.zeroPadLastBuffer = zeroPadLastBuffer;
	}
	

	/**
	 * Adds an AudioProcessor to the chain of processors.
	 * 
	 * @param audioProcessor
	 *            The AudioProcessor to add.
	 */
	public void addAudioProcessor(final AudioProcessor audioProcessor) {
		audioProcessors.add(audioProcessor);
		LOG.fine("Added an audioprocessor to the list of processors: " + audioProcessor.toString());
	}
	
	/**
	 * Removes an AudioProcessor to the chain of processors and calls its <code>processingFinished</code> method.
	 * 
	 * @param audioProcessor
	 *            The AudioProcessor to remove.
	 */
	public void removeAudioProcessor(final AudioProcessor audioProcessor) {
		audioProcessors.remove(audioProcessor);
		audioProcessor.processingFinished();
		LOG.fine("Remove an audioprocessor to the list of processors: " + audioProcessor.toString());
	}

	public void run() {
		
		int bytesRead = 0;
		
		if(bytesToSkip!=0){
			skipToStart();
		}
	
		//Read the first (and in some cases last) audio block.
		try {
			//needed to get correct time info when skipping first x seconds
			audioEvent.setBytesProcessed(bytesProcessed);
			bytesRead = readNextAudioBlock();
		} catch (IOException e) {
			String message="Error while reading audio input stream: " + e.getMessage();	
			LOG.warning(message);
			throw new Error(message);
		}

		// As long as the stream has not ended
		while (bytesRead != 0 && !stopped) {
			
			//Makes sure the right buffers are processed, they can be changed by audio processors.
			for (final AudioProcessor processor : audioProcessors) {
				if(!processor.process(audioEvent)){
					//skip to the next audio processors if false is returned.
					break;
				}	
			}
			
			if(!stopped){			
				//Update the number of bytes processed;
				bytesProcessed += bytesRead;
				audioEvent.setBytesProcessed(bytesProcessed);
					
				// Read, convert and process consecutive overlapping buffers.
				// Slide the buffer.
				try {
					bytesRead = readNextAudioBlock();
					audioEvent.setOverlap(floatOverlap);
				} catch (IOException e) {
					String message="Error while reading audio input stream: " + e.getMessage();	
					LOG.warning(message);
					throw new Error(message);
				}
			}
		}

		// Notify all processors that no more data is available. 
		// when stop() is called processingFinished is called explicitly, no need to do this again.
		// The explicit call is to prevent timing issues.
		if(!stopped){
			stop();
		}
	}
	
	
	private void skipToStart() {
		long skipped = 0l;
		try{
			skipped = audioInputStream.skip(bytesToSkip);
			if(skipped !=bytesToSkip){
				throw new IOException();
			}
			bytesProcessed += bytesToSkip;
		}catch(IOException e){
			String message=String.format("Did not skip the expected amount of bytes,  %d skipped, %d expected!", skipped,bytesToSkip);	
			LOG.warning(message);
			throw new Error(message);
		}
	}

	/**
	 * Stops dispatching audio data.
	 */
	public void stop() {
		stopped = true;
		for (final AudioProcessor processor : audioProcessors) {
			processor.processingFinished();
		}
		try {
			audioInputStream.close();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Closing audio stream error.", e);
		}
	}

	/**
	 * Reads the next audio block. It tries to read the number of bytes defined
	 * by the audio buffer size minus the overlap. If the expected number of
	 * bytes could not be read either the end of the stream is reached or
	 * something went wrong.
	 * 
	 * The behavior for the first and last buffer is defined by their corresponding the zero pad settings. The method also handles the case if
	 * the first buffer is also the last.
	 * 
	 * @return The number of bytes read.
	 * @throws IOException
	 *             When something goes wrong while reading the stream. In
	 *             particular, an IOException is thrown if the input stream has
	 *             been closed.
	 */
	private int readNextAudioBlock() throws IOException {
		assert floatOverlap < audioFloatBuffer.length;
		
		// Is this the first buffer?
		boolean isFirstBuffer = (bytesProcessed ==0 || bytesProcessed == bytesToSkip);
		
		final int offsetInBytes;
		
		final int offsetInSamples;
		
		final int bytesToRead;
		//Determine the amount of bytes to read from the stream
		if(isFirstBuffer && !zeroPadFirstBuffer){
			//If this is the first buffer and we do not want to zero pad the
			//first buffer then read a full buffer
			bytesToRead =  audioByteBuffer.length;
			// With an offset in bytes of zero;
			offsetInBytes = 0;
			offsetInSamples=0;
		}else{
			//In all other cases read the amount of bytes defined by the step size
			bytesToRead = byteStepSize;
			offsetInBytes = byteOverlap;
			offsetInSamples = floatOverlap;
		}
		
		//Shift the audio information using array copy since it is probably faster than manually shifting it.
		// No need to do this on the first buffer
		if(!isFirstBuffer && audioFloatBuffer.length == floatOverlap + floatStepSize ){
			System.arraycopy(audioFloatBuffer,floatStepSize, audioFloatBuffer,0 ,floatOverlap);
			/*
			for(int i = floatStepSize ; i < floatStepSize+floatOverlap ; i++){
				audioFloatBuffer[i-floatStepSize] = audioFloatBuffer[i];
			}*/
		}
		
		// Total amount of bytes read
		int totalBytesRead = 0;
		
		// The amount of bytes read from the stream during one iteration.
		int bytesRead=0;
		
		// Is the end of the stream reached?
		boolean endOfStream = false;
				
		// Always try to read the 'bytesToRead' amount of bytes.
		// unless the stream is closed (stopped is true) or no bytes could be read during one iteration 
		while(!stopped && !endOfStream && totalBytesRead<bytesToRead){
			try{
				bytesRead = audioInputStream.read(audioByteBuffer, offsetInBytes + totalBytesRead , bytesToRead - totalBytesRead);
			}catch(IndexOutOfBoundsException e){
				// The pipe decoder generates an out of bounds if end
				// of stream is reached. Ugly hack...
				bytesRead = -1;
			}
			if(bytesRead == -1){
				// The end of the stream is reached if the number of bytes read during this iteration equals -1
				endOfStream = true;
			}else{
				// Otherwise add the number of bytes read to the total 
				totalBytesRead += bytesRead;
			}
		}
		
		if(endOfStream){
			// Could not read a full buffer from the stream, there are two options:
			if(zeroPadLastBuffer){
				//Make sure the last buffer has the same length as all other buffers and pad with zeros
				for(int i = offsetInBytes + totalBytesRead; i < audioByteBuffer.length; i++){
					audioByteBuffer[i] = 0;
				}
				converter.toFloatArray(audioByteBuffer, offsetInBytes, audioFloatBuffer, offsetInSamples, floatStepSize);
			}else{
				// Send a smaller buffer through the chain.
				byte[] audioByteBufferContent = audioByteBuffer;
				audioByteBuffer = new byte[offsetInBytes + totalBytesRead];
				for(int i = 0 ; i < audioByteBuffer.length ; i++){
					audioByteBuffer[i] = audioByteBufferContent[i];
				}
				int totalSamplesRead = totalBytesRead/format.getFrameSize();
				audioFloatBuffer = new float[offsetInSamples + totalBytesRead/format.getFrameSize()];
				converter.toFloatArray(audioByteBuffer, offsetInBytes, audioFloatBuffer, offsetInSamples, totalSamplesRead);
				
				
			}			
		}else if(bytesToRead == totalBytesRead) {
			// The expected amount of bytes have been read from the stream.
			if(isFirstBuffer && !zeroPadFirstBuffer){
				converter.toFloatArray(audioByteBuffer, 0, audioFloatBuffer, 0, audioFloatBuffer.length);
			}else{
				converter.toFloatArray(audioByteBuffer, offsetInBytes, audioFloatBuffer, offsetInSamples, floatStepSize);
			}
		} else if(!stopped) {
			// If the end of the stream has not been reached and the number of bytes read is not the
			// expected amount of bytes, then we are in an invalid state; 
			throw new IOException(String.format("The end of the audio stream has not been reached and the number of bytes read (%d) is not equal "
					+ "to the expected amount of bytes(%d).", totalBytesRead,bytesToRead));
		}
		
		
		// Makes sure AudioEvent contains correct info.
		audioEvent.setFloatBuffer(audioFloatBuffer);
		audioEvent.setOverlap(offsetInSamples);
		
		return totalBytesRead; 
	}

	/**
	 * The current format used to convert floats to bytes and back.
	 * @return The current format used to convert floats to bytes and back.
	 */
	public TarsosDSPAudioFormat getFormat(){
		return format;
	}
	
	/**
	 * 
	 * @return The currently processed number of seconds.
	 */
	public float secondsProcessed(){
		return bytesProcessed / (format.getSampleSizeInBits() / 8) / format.getSampleRate() / format.getChannels() ;
	}

	/**
	 * Set a new audio buffer
	 * @param audioBuffer The audio buffer to use.
	 */
	public void setAudioFloatBuffer(float[] audioBuffer){
		audioFloatBuffer = audioBuffer;
	}
	/**
	 * @return True if the dispatcher is stopped or the end of stream has been reached.
	 */
	public boolean isStopped(){
		return stopped;
	}
	
}
