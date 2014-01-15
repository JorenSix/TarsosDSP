package be.hogent.tarsos.dsp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.ByteOrder;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * <p>
 * Decode audio files to PCM, mono, 16bits per sample, at any sample rate using
 * an external program. By default avconv, provided by libav is used. Other
 * command line programs that are able to decode audio and pipe binary PCM
 * samples to STDOUT are possible as well (ffmpeg, mplayer). On Debian: <code>apt-get install libav-tools</code>
 * </p>
 * <p>
 * This adds support for a lot of audio formats and video container formats with
 * relatively little effort. Depending on the program used also http streams,
 * rtpm streams, ... are supported as well.
 * </p>
 * <p>
 * To see which audio decoders are supported, check
 * </p>
 * <code><pre>avconv -decoders | grep -E "^A" | sort
avconv version 9.8, Copyright (c) 2000-2013 the Libav developers
  built on Aug 26 2013 09:52:20 with gcc 4.4.3 (Ubuntu 4.4.3-4ubuntu5.1)
A... 8svx_exp             8SVX exponential
A... 8svx_fib             8SVX fibonacci
A... aac                  AAC (Advanced Audio Coding)
A... aac_latm             AAC LATM (Advanced Audio Coding LATM syntax)
A... ac3                  ATSC A/52A (AC-3)
A... adpcm_4xm            ADPCM 4X Movie
...
</pre></code>
 * 
 * @author Joren Six
 */
public class PipeDecoder {
	
	private final static Logger LOG = Logger.getLogger(PipeDecoder.class.getName());
	private final String pipeEnvironment;
	private final String pipeArgument;
	private final String pipeCommand;
	private final File pipeLogFile;
	private final int pipeBuffer;
	
	public PipeDecoder(){
		//Use sensible defaults depending on the platform
		if(System.getProperty("os.name").indexOf("indows") > 0 ){
			pipeEnvironment = "cmd.exe";
			pipeArgument = "/C";
		}else{
			pipeEnvironment = "/bin/bash";
			pipeArgument = "-c";
		}
		pipeCommand = "avconv -i \"%resource%\" -vn -ar %sample_rate% -ac %channels% -sample_fmt s16 -f wav pipe:1";
		pipeLogFile = new File("decoder_log.txt");
		pipeBuffer = 10000;
	}
	
	public PipeDecoder(String pipeEnvironment,String pipeArgument,String pipeCommand,String pipeLogFile,int pipeBuffer){
		this.pipeEnvironment = "/bin/bash";
		this.pipeArgument = "-c";
		this.pipeCommand = pipeCommand;
		this.pipeLogFile = new File(pipeLogFile);
		this.pipeBuffer = pipeBuffer;
	}

	
	public AudioInputStream getDecodedStream(final String resource,final int targetSampleRate) {
		
		AudioFormat audioFormat = getTargetAudioFormat(targetSampleRate);
		
		try {
			String command = pipeCommand;
			command = command.replace("%resource%", resource);
			command = command.replace("%sample_rate%", String.valueOf(targetSampleRate));
			command = command.replace("%channels%", "1");
			
			ProcessBuilder pb = new ProcessBuilder(pipeEnvironment, pipeArgument , command);
		
			pb.redirectError(Redirect.appendTo(pipeLogFile));
		
			LOG.fine("Starting piped decoding process for " + resource );
			final Process process = pb.start();
			
			InputStream stdOut = new BufferedInputStream(process.getInputStream(), pipeBuffer);
						
			final AudioInputStream audioStream = new AudioInputStream(stdOut, audioFormat, AudioSystem.NOT_SPECIFIED);
			
			new Thread(new Runnable(){

				@Override
				public void run() {
					try {
						process.waitFor();
						LOG.fine("Finished piped decoding process");
					} catch (InterruptedException e) {
						LOG.severe("Interrupted while waiting for sub process exit.");
						e.printStackTrace();
					}
				}},"Decoding Pipe").start();
			return audioStream;
		} catch (IOException e) {
			LOG.warning("IO exception while decoding audio via sub process." + e.getMessage() );
			e.printStackTrace();
		}
		return null;
		
	}
	
	/**
	 * Constructs the target audio format. The audio format is one channel
	 * signed PCM of a given sample rate.
	 * 
	 * @param targetSampleRate
	 *            The sample rate to convert to.
	 * @return The audio format after conversion.
	 */
	public AudioFormat getTargetAudioFormat(int targetSampleRate) {
		AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 
	        		targetSampleRate, 
	        		2 * 8, 
	        		1, 
	        		2 * 1, 
	        		targetSampleRate, 
	                ByteOrder.BIG_ENDIAN.equals(ByteOrder.nativeOrder()));
		 return audioFormat;
	}

}
