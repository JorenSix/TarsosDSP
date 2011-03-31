package be.hogent.tarsos.dsp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
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
	private final float[] audioFloatBuffer;

	/**
	 * This buffer is reused again and again to store audio data using the byte
	 * data type.
	 */
	private final byte[] audioByteBuffer;

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

	/**
	 * The floatOverlap: the number of elements that are copied in the buffer
	 * from the previous buffer. Overlap should be smaller (strict) than the
	 * buffer size and can be zero. Defined in number of samples.
	 */
	private final int floatOverlap, floatStepSize;

	/**
	 * The overlap and stepsize defined not in samples but in bytes. So it
	 * depends on the bit depth. Since the int datatype is used only 8,16,24,...
	 * bits or 1,2,3,... bytes are supported.
	 */
	private final int byteOverlap, byteStepSize;
	
	
	/**
	 * If true the dispatcher stops dispatching audio.
	 */
	private boolean stopped;

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

		final AudioFormat format = audioInputStream.getFormat();

		converter = AudioFloatConverter.getConverter(format);

		audioFloatBuffer = new float[audioBufferSize];
		floatOverlap = bufferOverlap;
		floatStepSize = audioFloatBuffer.length - floatOverlap;

		audioByteBuffer = new byte[audioFloatBuffer.length * format.getFrameSize()];
		byteOverlap = floatOverlap * format.getFrameSize();
		byteStepSize = floatStepSize * format.getFrameSize();
		
		stopped = false;
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
		try {
			int bytesRead;

			// Read, convert and process the first full buffer.
			bytesRead = audioInputStream.read(audioByteBuffer);

			if (bytesRead != -1 && !stopped) {
				converter.toFloatArray(audioByteBuffer, audioFloatBuffer);
				for (final AudioProcessor processor : audioProcessors) {
					if(!processor.processFull(audioFloatBuffer, audioByteBuffer)){
						break;
					}
				}
				// Read, convert and process consecutive overlapping buffers.
				// Slide the buffer.
				bytesRead = slideBuffer();
			}

			// as long as the stream has not ended or the number of bytes
			// processed is smaller than the number of bytes to process: process
			// bytes.
			while (bytesRead != -1 && !stopped) {
				for (final AudioProcessor processor : audioProcessors) {
					if(!processor.processOverlapping(audioFloatBuffer, audioByteBuffer)){
						break;
					}
				}
				bytesRead = slideBuffer();
			}

			// Notify all processors that no more data is available. 
			// when stop() is called processingFinished is called explicitly, no need to do this again.
			// The explicit call is to prevent timing issues.
			if(!stopped){
				for (final AudioProcessor processor : audioProcessors) {
					processor.processingFinished();
				}
				audioInputStream.close();
			}
		} catch (final IOException e) {
			LOG.log(Level.SEVERE, "Error while reading data from audio stream.", e);
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

		for (int i = 0; i < floatOverlap; i++) {
			audioFloatBuffer[i] = audioFloatBuffer[i + floatStepSize];
		}

		final int bytesRead = audioInputStream.read(audioByteBuffer, byteOverlap, byteStepSize);
		converter.toFloatArray(audioByteBuffer, byteOverlap, audioFloatBuffer, floatOverlap, floatStepSize);

		return bytesRead;
	}

	/**
	 * Create a stream from a file and use that to create a new AudioDispatcher
	 * 
	 * @param audioFile
	 *            The file.
	 * @param size
	 *            The number of samples used in the buffer.
	 * @return A new audioprocessor.
	 * @throws UnsupportedAudioFileException
	 *             If the audio file is not supported.
	 * @throws IOException
	 *             When an error occurs reading the file.
	 */
	public static AudioDispatcher fromFile(final File audioFile, final int size)
			throws UnsupportedAudioFileException, IOException {
		final AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile);
		return new AudioDispatcher(stream, size, 0);
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
}
