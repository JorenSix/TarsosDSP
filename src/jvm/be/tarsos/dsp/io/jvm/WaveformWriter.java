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


package be.tarsos.dsp.io.jvm;

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

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

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
	 * @param fileName The name of the wav file to store.
	 */
	public WaveformWriter(final AudioFormat format,final String fileName){
		this.format=format;

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
	
	public WaveformWriter(final TarsosDSPAudioFormat format,final String fileName){
		this(JVMAudioInputStream.toAudioFormat(format),fileName);
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		this.byteOverlap = audioEvent.getOverlap() * format.getFrameSize();
		this.byteStepSize = audioEvent.getBufferSize() * format.getFrameSize() - byteOverlap;
		try {
			rawOutputStream.write(audioEvent.getByteBuffer(), byteOverlap, byteStepSize);
		} catch (IOException e) {
			LOG.severe(String.format("Failure while writing temporary file: %1s: %2s", rawOutputFile.getAbsolutePath(), e.getMessage()));
		}
		return true;
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
