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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

/**
 * This AudioProcessor can be used to sync events with sound. It uses a pattern
 * described in JavaFX Special Effects Taking Java RIA to the Extreme with
 * Animation, Multimedia, and Game Element Chapter 9 page 185: <blockquote><i>
 * The variable LineWavelet is the Java Sound object that actually makes the sound. The
 * write method on LineWavelet is interesting because it blocks until it is ready for
 * more data. </i></blockquote> If this AudioProcessor chained with other
 * AudioProcessors the others should be able to operate in real time or process
 * the signal on a separate thread.
 * 
 * @author Joren Six
 */
public final class AudioPlayer implements AudioProcessor {
	

	/**
	 * The LineWavelet to send sound to. Is also used to keep everything in sync.
	 */
	private SourceDataLine line;

	
	private final AudioFormat format;

	/**
	 * Creates a new audio player.
	 * 
	 * @param format
	 *            The AudioFormat of the buffer.
	 * @throws LineUnavailableException
	 *             If no output LineWavelet is available.
	 */
	public AudioPlayer(final AudioFormat format)	throws LineUnavailableException {
		final DataLine.Info info = new DataLine.Info(SourceDataLine.class,format);
		this.format = format;
		line = (SourceDataLine) AudioSystem.getLine(info);
		line.open();
		line.start();
	}
	
	public AudioPlayer(final AudioFormat format, int bufferSize) throws LineUnavailableException {
		final DataLine.Info info = new DataLine.Info(SourceDataLine.class,format,bufferSize);
		this.format = format;
		line = (SourceDataLine) AudioSystem.getLine(info);
		line.open(format,bufferSize*2);
		System.out.println("Buffer size:" + line.getBufferSize());
		line.start();
	}
	
	public AudioPlayer(final TarsosDSPAudioFormat format, int bufferSize) throws LineUnavailableException {
		this(JVMAudioInputStream.toAudioFormat(format),bufferSize);
	}
	public AudioPlayer(final TarsosDSPAudioFormat format) throws LineUnavailableException {
		this(JVMAudioInputStream.toAudioFormat(format));
	}
	
	public long getMicroSecondPosition(){
		return line.getMicrosecondPosition();
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		int byteOverlap = audioEvent.getOverlap() * format.getFrameSize();
		int byteStepSize = audioEvent.getBufferSize() * format.getFrameSize() - byteOverlap;
		if(audioEvent.getTimeStamp() == 0){
			byteOverlap = 0;
			byteStepSize = audioEvent.getBufferSize() * format.getFrameSize();
		}
		// overlap in samples * nr of bytes / sample = bytes overlap
		
		/*
		if(byteStepSize < line.available()){
			System.out.println(line.available() + " Will not block " + line.getMicrosecondPosition());
		}else {
			System.out.println("Will block " + line.getMicrosecondPosition());
		}
		*/
		
		int bytesWritten = line.write(audioEvent.getByteBuffer(), byteOverlap, byteStepSize);
		if(bytesWritten != byteStepSize){
			System.err.println(String.format("Expected to write %d bytes but only wrote %d bytes",byteStepSize,bytesWritten));
		}
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see be.tarsos.util.RealTimeAudioProcessor.AudioProcessor#
	 * processingFinished()
	 */
	public void processingFinished() {
		// cleanup
		line.drain();//drain takes too long..
		line.stop();
		line.close();
	}
}
