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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.util.AudioFloatConverter;


/**
 * This class plays a file and sends float arrays to registered AudioProcessor
 * implementors. This class can be used to feed FFT's, pitch detectors, audio players, ...
 * Using a (blocking) audio player it is even possible to synchronize execution of
 * AudioProcessors and sound. This behavior can be used for visualization.
 * @author Joren Six
 */
public final class AudioDispatcher implements Runnable {

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(AudioDispatcher.class.getName());

	/**
	 * The audio stream (in bytes), conversion to float happens at the last
	 * moment.
	 */
	private final AudioInputStream audioInputStream;

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
	private final AudioFloatConverter converter;
	
	private final AudioFormat format;

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
	 * Has this dispatcher a source? A dispatcher without source can be used for synthesis.
	 */
	private final boolean hasSource;
	
	
	private final AudioEvent audioEvent;
	
	/**
	 * If true the dispatcher stops dispatching audio.
	 */
	private boolean stopped;
	
	/**
	 * if zero pad is true then the first buffer is only filled up to  buffer size - hop size
	 * E.g. if the buffer is 2048 and the hop size is 48 then you get 2000x0 and 48 filled audio samples
	 */
	private boolean zeroPad;

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
	 * @throws UnsupportedAudioFileException
	 *             If an unsupported format is used.
	 */
	public AudioDispatcher(final AudioInputStream stream, final int audioBufferSize, final int bufferOverlap)
			throws UnsupportedAudioFileException {
		
		audioProcessors = new ArrayList<AudioProcessor>();
		audioInputStream = stream;

		format = audioInputStream.getFormat();
		audioEvent = new AudioEvent(format,audioInputStream.getFrameLength());
			
		setStepSizeAndOverlap(audioBufferSize, bufferOverlap);
		converter = AudioFloatConverter.getConverter(format);
		
		stopped = false;
		
		bytesToSkip = 0;
		bytesProcessed=0;
		
		hasSource = true;
	}
	
	public AudioDispatcher(final int audioBufferSize) {
		audioProcessors = new ArrayList<AudioProcessor>();
		audioInputStream = null;
		format = new AudioFormat(44100, 16, 1, true, false);
		audioEvent = new AudioEvent(format, 0);
		audioFloatBuffer = new float[audioBufferSize];
		
		floatOverlap = 0;
		floatStepSize = audioFloatBuffer.length - floatOverlap;
		audioByteBuffer = new byte[audioFloatBuffer.length * format.getFrameSize()];
		byteOverlap = floatOverlap * format.getFrameSize();
		byteStepSize = floatStepSize * format.getFrameSize();
		
		converter = AudioFloatConverter.getConverter(format);
		stopped = false;
		bytesToSkip = 0;
		bytesProcessed = 0;
		hasSource = false;
	}
	
	/**
	 * Returns the duration of the stream in seconds. If the length of the stream can not be determined (e.g. microphone input), it returns a negative number.
	 * @return The duration of the stream in seconds or a negative number.
	 */
	public double durationInSeconds(){
		return audioInputStream.getFrameLength() / format.getSampleRate();
	}
	
	/**
	 * Returns the length of the stream, expressed in sample frames rather than bytes. 
	 * @return The length of the stream, expressed in sample frames rather than bytes.
	 */
	public long durationInFrames(){
		return audioInputStream.getFrameLength() ;
	}
	
	
	
	
	/**
	 * Skip a number of seconds before processing the stream.
	 * @param seconds
	 */
	public void skip(double seconds){
		bytesToSkip = Math.round(seconds * format.getSampleRate() * format.getFrameSize()); 
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

		final AudioFormat format = audioInputStream.getFormat();
		audioByteBuffer = new byte[audioFloatBuffer.length * format.getFrameSize()];
		byteOverlap = floatOverlap * format.getFrameSize();
		byteStepSize = floatStepSize * format.getFrameSize();
	}
	
	/**
	 * if zero pad is true then the first buffer is only filled up to  buffer size - hop size
	 * E.g. if the buffer is 2048 and the hop size is 48 then you get 2000x0 and 48 filled audio samples
	 * @param zeroPad true if the buffer should be zeropadded, false otherwise.
	 */
	public void setZeroPad(boolean zeroPad){
		this.zeroPad = zeroPad;		
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
	 * Removes an AudioProcessor to the chain of processors and calls processingFinished.
	 * 
	 * @param audioProcessor
	 *            The AudioProcessor to add.
	 */
	public void removeAudioProcessor(final AudioProcessor audioProcessor) {
		audioProcessors.remove(audioProcessor);
		audioProcessor.processingFinished();
		LOG.fine("Remove an audioprocessor to the list of processors: " + audioProcessor.toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		if(hasSource){
			runSourcedDispatcher();
		}else{
			runNonSourcedDispatcher();
		}
	}
	
	private void runSourcedDispatcher(){
		try {
			int bytesRead = 0;
			
			if(bytesToSkip!=0){
				audioInputStream.skip(bytesToSkip);
				bytesProcessed += bytesToSkip;
			}
			if(zeroPad){
				bytesRead = slideBuffer();
			}else {
				bytesRead = processFirstBuffer();
			}

			// as long as the stream has not ended or the number of bytes
			// processed is smaller than the number of bytes to process: process
			// bytes.
		audioLoop:
			while (bytesRead != -1 && !stopped) {
				
				//Makes sure the right buffers are processed, they can be changed by audio processors.
				audioEvent.setOverlap(floatOverlap);
				audioEvent.setFloatBuffer(audioFloatBuffer);
				audioEvent.setBytesProcessed(bytesProcessed);
				
				for (final AudioProcessor processor : audioProcessors) {
					if(!processor.process(audioEvent)){
						break audioLoop;
					}
				}
								
				//Update the number of bytes processed;
				bytesProcessed += bytesRead;
					
				// Read, convert and process consecutive overlapping buffers.
				// Slide the buffer.
				bytesRead = slideBuffer();
			}

			// Notify all processors that no more data is available. 
			// when stop() is called processingFinished is called explicitly, no need to do this again.
			// The explicit call is to prevent timing issues.
			if(!stopped){
				stop();
			}
		} catch (final IOException e) {
			LOG.log(Level.SEVERE, "Error while reading data from audio stream.", e);
		}
	}
	
	private void runNonSourcedDispatcher(){
		audioFloatBuffer = new float[floatStepSize];
		audioEvent.setBytesProcessed(bytesProcessed);
		audioEvent.setFloatBuffer(audioFloatBuffer);
		audioLoop:
			while(!stopped){
				audioEvent.clearFloatBuffer();
				for (final AudioProcessor processor : audioProcessors) {
					if(!processor.process(audioEvent)){
						break audioLoop;
					}
				}
			}
	}
	
	private int processFirstBuffer() throws IOException{
		//the overlap for the first buffer is zero.
		audioEvent.setOverlap(0);
		audioEvent.setFloatBuffer(audioFloatBuffer);
		audioEvent.setBytesProcessed(bytesProcessed);
		
		// Read, convert and process the first full buffer.
		//Always read a full byte buffer!
		int bytesRead = 0;
		int currentBytesRead = 0;
		while(bytesRead != -1 && currentBytesRead<audioByteBuffer.length){
			bytesRead = audioInputStream.read(audioByteBuffer, currentBytesRead , audioByteBuffer.length - currentBytesRead);
			currentBytesRead += bytesRead;
		}
		bytesRead = currentBytesRead;
		
		if (bytesRead != -1 && !stopped) {
			converter.toFloatArray(audioByteBuffer, audioFloatBuffer);
			
			for (final AudioProcessor processor : audioProcessors) {
				if(!processor.process(audioEvent)){
					break;
				}
			}
			//Update the number of bytes processed;
			bytesProcessed += bytesRead;
			
			// Read, convert and process consecutive overlapping buffers.
			// Slide the buffer.
			bytesRead = slideBuffer();
		}
		return bytesRead;
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
	 * Slides a buffer with an floatOverlap and reads new data from the stream.
	 * to the correct place in the buffer. E.g. with a buffer size of 9 and
	 * floatOverlap of 3.
	 * 
	 * <pre>
	 *      | 0 | 1 | 3 | 3 | 4  | 5  | 6  | 7  | 8  |
	 *                        |
	 *                Slide (9 - 3 = 6)
	 *                        |
	 *                        v
	 *      | 6 | 7 | 8 | _ | _  | _  | _  | _  | _  |
	 *                        |
	 *        Fill from 3 to (3+6) exclusive
	 *                        |
	 *                        v
	 *      | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 |
	 * </pre>
	 * 
	 * @return The number of bytes read.
	 * @throws IOException
	 *             When something goes wrong while reading the stream. In
	 *             particular, an IOException is thrown if the input stream has
	 *             been closed.
	 */
	private int slideBuffer() throws IOException {
		assert floatOverlap < audioFloatBuffer.length;

		//Is array copy faster to shift an array? Probably..
		System.arraycopy(audioFloatBuffer, floatStepSize, audioFloatBuffer,0 ,floatOverlap);
		
		int bytesRead=0;
		
		//Check here if the dispatcher is stopped to prevent reading from a closed audio stream.
		if(stopped){
			bytesRead = -1;
		}else{
			int currentBytesRead = 0;
			//Always read a full byte buffer!
			while(bytesRead != -1 && currentBytesRead<byteStepSize){
				bytesRead = audioInputStream.read(audioByteBuffer, byteOverlap + currentBytesRead , byteStepSize - currentBytesRead);
				currentBytesRead += bytesRead;
			}
			bytesRead = currentBytesRead;
			converter.toFloatArray(audioByteBuffer, byteOverlap, audioFloatBuffer, floatOverlap, floatStepSize);
		}
		return bytesRead;
	}
	
	public AudioFormat getFormat(){
		return format;
	}

	/**
	 * Create a stream from a file and use that to create a new AudioDispatcher
	 * 
	 * @param audioFile
	 *            The file.
	 * @param size
	 *            The number of samples used in the buffer.
	 * @param overlap 
	 * @return A new audioprocessor.
	 * @throws UnsupportedAudioFileException
	 *             If the audio file is not supported.
	 * @throws IOException
	 *             When an error occurs reading the file.
	 */
	public static AudioDispatcher fromFile(final File audioFile, final int size,final int overlap)
			throws UnsupportedAudioFileException, IOException {
		final AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile);
		return new AudioDispatcher(stream, size, overlap);
	}
	
	
	/**
	 * Create a stream from an URL and use that to create a new AudioDispatcher
	 * 
	 * @param audioStream
	 *            The URL describing the stream..
	 * @param size
	 *            The number of samples used in the buffer.
	 * @param overlap 
	 * @return A new audio processor.
	 * @throws UnsupportedAudioFileException
	 *             If the audio file is not supported.
	 * @throws IOException
	 *             When an error occurs reading the file.
	 */
	public static AudioDispatcher fromURL(final URL audioStream, final int size,final int overlap)
	throws UnsupportedAudioFileException, IOException {
		final AudioInputStream stream = AudioSystem.getAudioInputStream(audioStream);
		return new AudioDispatcher(stream, size, overlap);
	}
	
	
	

	/**
	 * Create a stream from an array of bytes and use that to create a new
	 * AudioDispatcher.
	 * 
	 * @param byteArray
	 *            An array of bytes, containing audio information.
	 * @param audioFormat
	 *            The format of the audio represented using the bytes.
	 * @param audioBufferSize
	 *            The size of the buffer defines how much samples are processed
	 *            in one step. Common values are 1024,2048.
	 * @param bufferOverlap
	 *            How much consecutive buffers overlap (in samples). Half of the
	 *            AudioBufferSize is common.
	 * @return A new AudioDispatcher.
	 * @throws UnsupportedAudioFileException
	 *             If the audio format is not supported.
	 */
	public static AudioDispatcher fromByteArray(final byte[] byteArray, final AudioFormat audioFormat,
			final int audioBufferSize, final int bufferOverlap) throws UnsupportedAudioFileException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
		final long length = byteArray.length / audioFormat.getFrameSize();
		final AudioInputStream stream = new AudioInputStream(bais, audioFormat, length);
		return new AudioDispatcher(stream, audioBufferSize, bufferOverlap);
	}
	
	/**
	 * Create a new AudioDispatcher connected to the default microphone. The default is defined by the 
	 * Java runtime by calling <pre>AudioSystem.getTargetDataLine(format)</pre>. 
	 * The microphone must support the format: 44100Hz sample rate, 16bits mono, signed big endian.   
	 * @param audioBufferSize
	 *            The size of the buffer defines how much samples are processed
	 *            in one step. Common values are 1024,2048.
	 * @param bufferOverlap
	 *            How much consecutive buffers overlap (in samples). Half of the
	 *            AudioBufferSize is common.
	 * @return An audio dispatcher connected to the default microphone.
	 * @throws UnsupportedAudioFileException
	 * @throws LineUnavailableException
	 */
	public static AudioDispatcher fromDefaultMicrophone(final int audioBufferSize, final int bufferOverlap) throws UnsupportedAudioFileException, LineUnavailableException {
		final AudioFormat format = new AudioFormat(44100, 16, 1, true,true);
		TargetDataLine line =  AudioSystem.getTargetDataLine(format);
		line.open(format, audioBufferSize);
		line.start();
		AudioInputStream stream = new AudioInputStream(line);
		return new AudioDispatcher(stream, audioBufferSize, bufferOverlap);
	}
	
	/**
	 * Create a stream from an array of floats and use that to create a new
	 * AudioDispatcher.
	 * 
	 * @param floatArray
	 *            An array of floats, containing audio information.
	 * @param sampleRate 
	 * 			  The sample rate of the audio information contained in the buffer.
	 * @param audioBufferSize
	 *            The size of the buffer defines how much samples are processed
	 *            in one step. Common values are 1024,2048.
	 * @param bufferOverlap
	 *            How much consecutive buffers overlap (in samples). Half of the
	 *            AudioBufferSize is common.
	 * @return A new AudioDispatcher.
	 * @throws UnsupportedAudioFileException
	 *             If the audio format is not supported.
	 */
	public static AudioDispatcher fromFloatArray(final float[] floatArray, final int sampleRate, final int audioBufferSize, final int bufferOverlap) throws UnsupportedAudioFileException {
		final AudioFormat audioFormat = new AudioFormat(sampleRate, 16, 1, true, false);
		final AudioFloatConverter converter = AudioFloatConverter.getConverter(audioFormat);
		final byte[] byteArray = new byte[floatArray.length * audioFormat.getFrameSize()]; 
		converter.toByteArray(floatArray, byteArray);
		return AudioDispatcher.fromByteArray(byteArray, audioFormat, audioBufferSize, bufferOverlap);
	}
}
