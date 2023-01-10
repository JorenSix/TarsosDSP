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

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;

import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;

/**
 * Encapsulates an {@link AudioInputStream} to make it work with the core TarsosDSP library.
 * 
 * @author Joren Six
 *
 */
public class JVMAudioInputStream implements TarsosDSPAudioInputStream {
	
	private final AudioInputStream underlyingStream;
	private final TarsosDSPAudioFormat tarsosDSPAudioFormat;
	public JVMAudioInputStream(AudioInputStream stream){
		this.underlyingStream = stream;
		this.tarsosDSPAudioFormat = JVMAudioInputStream.toTarsosDSPFormat(stream.getFormat());
	}

	@Override
	public long skip(long bytesToSkip) throws IOException {
		return underlyingStream.skip(bytesToSkip);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return underlyingStream.read(b, off, len);
	}

	@Override
	public void close() throws IOException {
		underlyingStream.close();
	}

	@Override
	public long getFrameLength() {

		return underlyingStream.getFrameLength();
	}

	@Override
	public TarsosDSPAudioFormat getFormat() {
		return tarsosDSPAudioFormat;
	}
	
	/**
	 * Converts a {@link AudioFormat} to a {@link TarsosDSPAudioFormat}.
	 * 
	 * @param format
	 *            The {@link AudioFormat}
	 * @return A {@link TarsosDSPAudioFormat}
	 */
	public static TarsosDSPAudioFormat toTarsosDSPFormat(AudioFormat format) {
		boolean isSigned = format.getEncoding() == Encoding.PCM_SIGNED;
		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), isSigned, format.isBigEndian());
		return tarsosDSPFormat;
	}

	/**
	 * Converts a {@link TarsosDSPAudioFormat} to a {@link AudioFormat}.
	 * 
	 * @param format
	 *            The {@link TarsosDSPAudioFormat}
	 * @return A {@link AudioFormat}
	 */
	public static AudioFormat toAudioFormat(TarsosDSPAudioFormat format) {
		boolean isSigned = format.getEncoding() == TarsosDSPAudioFormat.Encoding.PCM_SIGNED;
		AudioFormat audioFormat = new AudioFormat(format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), isSigned, format.isBigEndian());
		return audioFormat;
	}
	
}
