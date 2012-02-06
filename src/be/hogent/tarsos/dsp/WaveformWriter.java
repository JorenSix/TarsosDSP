package be.hogent.tarsos.dsp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * <p>
 * Writes a WAV-file to disk. It stores the bytes to a raw file and when the
 * processingFinished method is called it prepends the raw file with a header to
 * make it a legal WAV-file.
 * </p>
 * 
 * <p>
 * Writing a RAW file first and then a header is needed because the header
 * contains fields for the size of the file, which is unknown beforehand. See
 * Subchunk2Size and ChunkSize on this <a
 * href="https://ccrma.stanford.edu/courses/422/projects/WaveFormat/">wav file
 * reference</a>.
 * </p>
 * 
 * @author Joren Six
 */
public class WaveformWriter implements AudioProcessor {
	private final AudioFormat format;
	private final File rawOutputFile;
	private final String fileName;
	private FileOutputStream rawOutputStream;
	
	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(WaveformWriter.class.getName());
	
	/**
	 * The overlap and step size defined not in samples but in bytes. So it
	 * depends on the bit depth. Since the integer data type is used only
	 * 8,16,24,... bits or 1,2,3,... bytes are supported.
	 */
	private int byteOverlap, byteStepSize;
	
	/**
	 * Initialize the writer.
	 * @param format The format of the received bytes.
	 * @param bufferSize The size of a received buffer (in samples, not bytes).
	 * @param overlap The overlap of two consecutive buffers (in samples, not bytes).
	 * @param fileName The name of the wav file to store.
	 */
	public WaveformWriter(final AudioFormat format, final int bufferSize, final int overlap,final String fileName){
		this.format=format;
		this.byteOverlap = overlap * format.getFrameSize();
		this.byteStepSize = bufferSize * format.getFrameSize() - byteOverlap;
		this.fileName = fileName;
		//a temporary raw file with a random prefix
		this.rawOutputFile = new File(System.getProperty("java.io.tmpdir"), new Random().nextInt() + "out.raw");
		try {
			this.rawOutputStream = new FileOutputStream(rawOutputFile);
		} catch (FileNotFoundException e) {
			//It should always be possible to write to a temporary file.
			String message;
			message = String.format("Could not write to the temporary RAW file %1s: %2s", rawOutputFile.getAbsolutePath(), e.getMessage());
			LOG.severe(message);
		}	
	}
	
	@Override
	public boolean processFull(float[] audioFloatBuffer, byte[] audioByteBuffer) {
		writeData(audioByteBuffer, 0, byteStepSize);
		return true;
	}

	@Override
	public boolean processOverlapping(float[] audioFloatBuffer,
			byte[] audioByteBuffer) {
		writeData(audioByteBuffer, byteOverlap, byteStepSize);
		return true;
	}
	
	public void setStepSizeAndOverlap(final int audioBufferSize, final int bufferOverlap){
		this.byteOverlap = bufferOverlap * format.getFrameSize();
		this.byteStepSize = audioBufferSize * format.getFrameSize() - byteOverlap;
	}
	
	private void writeData(byte[] audioByteBuffer,int offset,int length){
		try {
			rawOutputStream.write(audioByteBuffer, byteOverlap, byteStepSize);
		} catch (IOException e) {
			
		}
	}

	@Override
	public void processingFinished() {
		File out = new File(fileName);
		try {
			//stream the raw file
			final FileInputStream inputStream = new FileInputStream(rawOutputFile);
			long lengthInSamples = rawOutputFile.length() / format.getFrameSize();
			final AudioInputStream audioInputStream;
			//create an audio stream form the raw file in the specified format
			audioInputStream = new AudioInputStream(inputStream, format,lengthInSamples);
			//stream this to the out file
			final FileOutputStream fos = new FileOutputStream(out);
			//stream all the bytes to the output stream
			AudioSystem.write(audioInputStream,AudioFileFormat.Type.WAVE,fos);
			//cleanup
			fos.close();
			audioInputStream.close();
			inputStream.close();
			rawOutputStream.close();
			rawOutputFile.delete();			
		} catch (IOException e) {
			String message;
			message = String.format("Error writing the WAV file %1s: %2s", out.getAbsolutePath(), e.getMessage());
			LOG.severe(message);
		}
	}

}
