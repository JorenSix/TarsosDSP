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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.PipeDecoder;
import be.tarsos.dsp.io.PipedAudioStream;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;

/**
 * The Factory creates {@link AudioDispatcher} objects from various sources: the
 * configured default microphone, PCM wav files or PCM samples piped from a
 * sub-process. It depends on the javax.sound.* packages and does not work on Android.
 * 
 * @author Joren Six
 * @see AudioDispatcher
 */
public class AudioDispatcherFactory {

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
	 * @throws LineUnavailableException
	 */
	public static AudioDispatcher fromDefaultMicrophone(final int audioBufferSize, final int bufferOverlap) throws LineUnavailableException {
		return fromDefaultMicrophone(44100, audioBufferSize, bufferOverlap);
	}
	
	/**
	 * Create a new AudioDispatcher connected to the default microphone. The default is defined by the 
	 * Java runtime by calling <pre>AudioSystem.getTargetDataLine(format)</pre>. 
	 * The microphone must support the format of the requested sample rate, 16bits mono, signed big endian.   
	 * @param sampleRate
	 * 			The <b>requested</b> sample rate must be supported by the capture device. Nonstandard sample 
	 * 			rates can be problematic!
	 * @param audioBufferSize
	 *            The size of the buffer defines how much samples are processed
	 *            in one step. Common values are 1024,2048.
	 * @param bufferOverlap
	 *            How much consecutive buffers overlap (in samples). Half of the
	 *            AudioBufferSize is common.
	 * @return An audio dispatcher connected to the default microphone.
	 * @throws LineUnavailableException
	 */
	public static AudioDispatcher fromDefaultMicrophone(final int sampleRate,final int audioBufferSize, final int bufferOverlap) throws LineUnavailableException {
		final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true,true);
		TargetDataLine line =  AudioSystem.getTargetDataLine(format);
		line.open(format, audioBufferSize);
		line.start();
		AudioInputStream stream = new AudioInputStream(line);
		TarsosDSPAudioInputStream audioStream = new JVMAudioInputStream(stream);
		return new AudioDispatcher(audioStream,audioBufferSize,bufferOverlap);
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
		TarsosDSPAudioInputStream audioStream = new JVMAudioInputStream(stream);
		return new AudioDispatcher(audioStream, audioBufferSize, bufferOverlap);
	}
	
	
	/**
	 * Create a stream from an URL and use that to create a new AudioDispatcher
	 * 
	 * @param audioURL
	 *            The URL describing the stream..
	 * @param audioBufferSize
	 *            The number of samples used in the buffer.
	 * @param bufferOverlap 
	 * @return A new audio processor.
	 * @throws UnsupportedAudioFileException
	 *             If the audio file is not supported.
	 * @throws IOException
	 *             When an error occurs reading the file.
	 */
	public static AudioDispatcher fromURL(final URL audioURL, final int audioBufferSize,final int bufferOverlap)
	throws UnsupportedAudioFileException, IOException {
		final AudioInputStream stream = AudioSystem.getAudioInputStream(audioURL);
		TarsosDSPAudioInputStream audioStream = new JVMAudioInputStream(stream);
		return new AudioDispatcher(audioStream, audioBufferSize, bufferOverlap);
	}
	
	/**
	 * Create a stream from a piped sub process and use that to create a new
	 * {@link AudioDispatcher} The sub-process writes a WAV-header and
	 * PCM-samples to standard out. The header is ignored and the PCM samples
	 * are are captured and interpreted. Examples of executables that can
	 * convert audio in any format and write to stdout are ffmpeg and avconv.
	 * 
	 * @param source
	 *            The file or stream to capture.
	 * @param targetSampleRate
	 *            The target sample rate.
	 * @param audioBufferSize
	 *            The number of samples used in the buffer.
	 * @param bufferOverlap
	 * @return A new audioprocessor.
	 */
	public static AudioDispatcher fromPipe(final String source,final int targetSampleRate, final int audioBufferSize,final int bufferOverlap){
		return fromPipe(source, targetSampleRate, audioBufferSize, bufferOverlap,0);
	}
	
	/**
	 * Create a stream from a piped sub process and use that to create a new
	 * {@link AudioDispatcher} The sub-process writes a WAV-header and
	 * PCM-samples to standard out. The header is ignored and the PCM samples
	 * are are captured and interpreted. Examples of executables that can
	 * convert audio in any format and write to stdout are ffmpeg and avconv.
	 * 
	 * @param source
	 *            The file or stream to capture.
	 * @param targetSampleRate
	 *            The target sample rate.
	 * @param audioBufferSize
	 *            The number of samples used in the buffer.
	 * @param bufferOverlap
	 * @param startTimeOffset 
	 * 			  Number of seconds to skip
	 * @return A new audioprocessor.
	 */
	public static AudioDispatcher fromPipe(final String source,final int targetSampleRate, final int audioBufferSize,final int bufferOverlap,final double startTimeOffset){
		if(new File(source).exists()&&new File(source).isFile() && new File(source).canRead()){
			PipedAudioStream f = new PipedAudioStream(source);
			TarsosDSPAudioInputStream audioStream = f.getMonoStream(targetSampleRate,startTimeOffset);
			return new AudioDispatcher(audioStream, audioBufferSize, bufferOverlap);
		}else{
			throw new IllegalArgumentException("The file " + source + " is not a readable file. Does it exist?");
		}
	}
	
	/**
	 * Create a stream from a piped sub process and use that to create a new
	 * {@link AudioDispatcher} The sub-process writes a WAV-header and
	 * PCM-samples to standard out. The header is ignored and the PCM samples
	 * are are captured and interpreted. Examples of executables that can
	 * convert audio in any format and write to stdout are ffmpeg and avconv.
	 * 
	 * @param source
	 *            The file or stream to capture.
	 * @param targetSampleRate
	 *            The target sample rate.
	 * @param audioBufferSize
	 *            The number of samples used in the buffer.
	 * @param bufferOverlap
	 * @param startTimeOffset 
	 * 			  Number of seconds to skip
	 * @param numberOfSeconds
	 * 			  Number of seconds to pipe
	 * @return A new audioprocessor.
	 */
	public static AudioDispatcher fromPipe(final String source,final int targetSampleRate, final int audioBufferSize,final int bufferOverlap,final double startTimeOffset,final double numberOfSeconds){
		if(new File(source).exists()&&new File(source).isFile() && new File(source).canRead()){
			PipedAudioStream f = new PipedAudioStream(source);
			TarsosDSPAudioInputStream audioStream = f.getMonoStream(targetSampleRate,startTimeOffset,numberOfSeconds);
			return new AudioDispatcher(audioStream, audioBufferSize, bufferOverlap);
		}else{
			throw new IllegalArgumentException("The file " + source + " is not a readable file. Does it exist?");
		}
	}
	
	
	/**
	 * Create a stream from a file and use that to create a new AudioDispatcher
	 * 
	 * @param audioFile
	 *            The file.
	 * @param audioBufferSize
	 *            The number of samples used in the buffer.
	 * @param bufferOverlap 
	 * @return A new audioprocessor.
	 * @throws UnsupportedAudioFileException
	 *             If the audio file is not supported.
	 * @throws IOException
	 *             When an error occurs reading the file.
	 */
	public static AudioDispatcher fromFile(final File audioFile, final int audioBufferSize,final int bufferOverlap)
			throws UnsupportedAudioFileException, IOException {
		final AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile);
		TarsosDSPAudioInputStream audioStream = new JVMAudioInputStream(stream);
		return new AudioDispatcher(audioStream, audioBufferSize, bufferOverlap);
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
		final TarsosDSPAudioFloatConverter converter = TarsosDSPAudioFloatConverter.getConverter(JVMAudioInputStream.toTarsosDSPFormat(audioFormat));
		final byte[] byteArray = new byte[floatArray.length * audioFormat.getFrameSize()]; 
		converter.toByteArray(floatArray, byteArray);
		return AudioDispatcherFactory.fromByteArray(byteArray, audioFormat, audioBufferSize, bufferOverlap);
	}
	
	
}
