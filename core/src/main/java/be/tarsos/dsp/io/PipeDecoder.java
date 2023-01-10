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

package be.tarsos.dsp.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteOrder;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.tarsos.dsp.util.FFMPEGDownloader;

/**
 * <p>
 * Decode audio files to PCM, mono, 16bits per sample, at any sample rate using
 * an external program. By default ffmpeg is used. Other
 * command Line  programs that are able to decode audio and pipe binary PCM
 * samples to STDOUT are possible as well (avconv, mplayer). 
 * To install ffmpeg on Debian: <code>apt-get install ffmpeg</code>.
 * </p>
 * <p>
 * This adds support for a lot of audio formats and video container formats with
 * relatively little effort. Depending on the program used also http streams,
 * rtpm streams, ... are supported as well.
 * </p>
 * <p>
 * To see which audio decoders are supported, check
 * </p>
 * <code>ffmpeg -decoders | grep -E "^A" | sort
avconv version 9.8, Copyright (c) 2000-2013 the Libav developers
  built on Aug 26 2013 09:52:20 with gcc 4.4.3 (Ubuntu 4.4.3-4ubuntu5.1)
A... 8svx_exp             8SVX exponential
A... 8svx_fib             8SVX fibonacci
A... aac                  AAC (Advanced Audio Coding)
A... aac_latm             AAC LATM (Advanced Audio Coding LATM syntax)
A... ac3                  ATSC A/52A (AC-3)
A... adpcm_4xm            ADPCM 4X Movie
...
</code>
 * 
 * @author Joren Six
 */
public class PipeDecoder {
	
	private final static Logger LOG = Logger.getLogger(PipeDecoder.class.getName());
	private final String pipeEnvironment;
	private final String pipeArgument;
	private final String pipeCommand;
	private final int pipeBuffer;

	private boolean printErrorstream = false;

	private String decoderBinaryAbsolutePath;
	
	public PipeDecoder(){
		pipeBuffer = 10000;

		//Use sensible defaults depending on the platform
		if(System.getProperty("os.name").indexOf("indows") > 0 ){
			pipeEnvironment = "cmd.exe";
			pipeArgument = "/C";
		}else if(new File("/bin/bash").exists()){
			pipeEnvironment = "/bin/bash";
			pipeArgument = "-c";
		}else if (new File("/system/bin/sh").exists()){
			//probably we are on android here
			pipeEnvironment = "/system/bin/sh";
			pipeArgument = "-c";
		}else{
			LOG.severe("Coud not find a command line environment (cmd.exe or /bin/bash)");
			throw new Error("Decoding via a pipe will not work: Coud not find a command line environment (cmd.exe or /bin/bash)");
		}
		
		String path = System.getenv("PATH");
		String arguments = " -ss %input_seeking%  %number_of_seconds% -i \"%resource%\" -vn -ar %sample_rate% -ac %channels% -sample_fmt s16 -f s16le pipe:1";
		if(isAvailable("ffmpeg")){
			LOG.info("found ffmpeg on the path (" + path + "). Will use ffmpeg for decoding media files.");
			pipeCommand = "ffmpeg" + arguments;	
		} else {
			if(isAndroid()) {
				String tempDirectory = System.getProperty("java.io.tmpdir");
				printErrorstream=true;
				File f = new File(tempDirectory, "ffmpeg");
				if (f.exists() && f.length() > 1000000 && f.canExecute()) {
					decoderBinaryAbsolutePath = f.getAbsolutePath();
				} else {
					LOG.severe("Could not find an ffmpeg binary for your Android system. Did you forget calling: 'new AndroidFFMPEGLocator(this);' ?");
					LOG.severe("Tried to unpack a statically compiled ffmpeg binary for your architecture to: " + f.getAbsolutePath());
				}
			}else{
				LOG.warning("Dit not find ffmpeg or avconv on your path(" + path + "), will try to download it automatically.");
				FFMPEGDownloader downloader = new FFMPEGDownloader();
				decoderBinaryAbsolutePath = downloader.ffmpegBinary();
				if(decoderBinaryAbsolutePath==null){
					LOG.severe("Could not download an ffmpeg binary automatically for your system.");
				}
			}
			if(decoderBinaryAbsolutePath == null){
				pipeCommand = "false";
				throw new Error("Decoding via a pipe will not work: Could not find an ffmpeg binary for your system");
			}else{
				pipeCommand = '"' + decoderBinaryAbsolutePath + '"' + arguments;
			}
		}
	}
	
	private boolean isAvailable(String command){
		try{
			Runtime.getRuntime().exec(command + " -version");
			return true;
		}catch (Exception e){
			return false;
		}	
	}
	
	public PipeDecoder(String pipeEnvironment,String pipeArgument,String pipeCommand,String pipeLogFile,int pipeBuffer){
		this.pipeEnvironment = pipeEnvironment;
		this.pipeArgument = pipeArgument;
		this.pipeCommand = pipeCommand;
		this.pipeBuffer = pipeBuffer;
	}

	
	public InputStream getDecodedStream(final String resource,final int targetSampleRate,final double timeOffset, double numberOfSeconds) {
		
		try {
			String command = pipeCommand;
			command = command.replace("%input_seeking%",String.valueOf(timeOffset));
			//defines the number of seconds to process
			// -t 10.000 e.g. specifies to process ten seconds 
			// from the specified time offset (which is often zero).
			if(numberOfSeconds>0){
				command = command.replace("%number_of_seconds%","-t " + String.valueOf(numberOfSeconds));
			} else {
				command = command.replace("%number_of_seconds%","");
			}
			command = command.replace("%resource%", resource);
			command = command.replace("%sample_rate%", String.valueOf(targetSampleRate));
			command = command.replace("%channels%","1");
			
			ProcessBuilder pb;
			pb= new ProcessBuilder(pipeEnvironment, pipeArgument , command);

			LOG.info("Starting piped decoding process for " + resource);
			LOG.info(" with command: " + command);
			final Process process = pb.start();
			
			final InputStream stdOut = new BufferedInputStream(process.getInputStream(), pipeBuffer){
				@Override
				public void close() throws IOException{
					super.close();
					// try to destroy the ffmpeg command after close
					process.destroy();
				}
			};

			if(printErrorstream) {
				//print to log if requested
				new ErrorStreamGobbler(process.getErrorStream(),LOG).start();
			}else{
				//makes sure the error stream is handled
				//fix by SalomonBrys
				//see https://github.com/JorenSix/TarsosDSP/pull/212
				new ErrorStreamIgnorer(process.getErrorStream()).start();
			}
			
			new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						process.waitFor();
						LOG.info("Finished piped decoding process");
					} catch (InterruptedException e) {
						LOG.severe("Interrupted while waiting for decoding sub process exit.");
						e.printStackTrace();
					}
				}},"Decoding Pipe").start();
			return stdOut;
		} catch (IOException e) {
			LOG.warning("IO exception while decoding audio via sub process." + e.getMessage() );
			e.printStackTrace();
		}
		return null;
	}
	
	public double getDuration(final String resource) {
		double duration = -1;
		try {
			//use " for windows compatibility!
			String command = "ffmpeg -i \"%resource%\"";
			
			command = command.replace("%resource%", resource);
					
			ProcessBuilder pb;
			pb = new ProcessBuilder(pipeEnvironment, pipeArgument , command);

			LOG.info("Starting duration command for " + resource);
			LOG.fine(" with command: " + command);
			final Process process = pb.start();
			
			final InputStream stdOut = new BufferedInputStream(process.getInputStream(), pipeBuffer){
				@Override
				public void close() throws IOException{
					super.close();
					// try to destroy the ffmpeg command after close
					process.destroy();
				}
			};
			
			ErrorStreamStringGlobber essg = new ErrorStreamStringGlobber(process.getErrorStream());
			essg.start();
			
			new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						process.waitFor();
						LOG.info("Finished piped decoding process");
					} catch (InterruptedException e) {
						LOG.severe("Interrupted while waiting for decoding sub process exit.");
						e.printStackTrace();
					}
				}},"Decoding Pipe").run();
			
			String stdError = essg.getErrorStreamAsString();
			Pattern regex = Pattern.compile(".*\\s.*Duration:\\s+(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d), .*", Pattern.DOTALL | Pattern.MULTILINE);
			Matcher regexMatcher = regex.matcher(stdError);
			if (regexMatcher.find()) {
				duration = Integer.valueOf(regexMatcher.group(1)) * 3600+ 
				Integer.valueOf(regexMatcher.group(2)) * 60+
				Integer.valueOf(regexMatcher.group(3)) * 1 +
				 Double.valueOf("." + regexMatcher.group(4) );
			}
		} catch (IOException e) {
			LOG.warning("IO exception while decoding audio via sub process." + e.getMessage() );
			e.printStackTrace();
		}
		return duration;
	}

	public void printBinaryInfo(){
		try {
			Process p = Runtime.getRuntime().exec(decoderBinaryAbsolutePath);
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String line = null;
			while ((line = input.readLine()) != null) {
				System.out.println(line);
			}
			input.close();
			//int exitVal = 
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Constructs the target audio format. The audio format is one channel
	 * signed PCM of a given sample rate.
	 * 
	 * @param targetSampleRate
	 *            The sample rate to convert to.
	 * @return The audio format after conversion.
	 */
	public static TarsosDSPAudioFormat getTargetAudioFormat(int targetSampleRate) {
		TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(TarsosDSPAudioFormat.Encoding.PCM_SIGNED, 
	        		targetSampleRate, 
	        		2 * 8, 
	        		1, 
	        		2 * 1, 
	        		targetSampleRate, 
	                ByteOrder.BIG_ENDIAN.equals(ByteOrder.nativeOrder()));
		 return audioFormat;
	}


	private boolean isAndroid(){
		try {
			// This class is only available on android
			Class.forName("android.app.Activity");
			System.out.println("Running on Android!");
			return true;
		} catch(ClassNotFoundException e) {
			//the class is not found when running JVM
			return false;
		}
	}

	private static class ErrorStreamIgnorer extends Thread {
		private final InputStream is;

		private ErrorStreamIgnorer(InputStream is) {
			this.is = is;
		}

		@Override
		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null) {}
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	private class ErrorStreamGobbler extends Thread {
		private final InputStream is;
		private final Logger logger;

		private ErrorStreamGobbler(InputStream is, Logger logger) {
			this.is = is;
			this.logger = logger;
		}

		@Override
		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null) {
					logger.info(line);
				}
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
	
	private class ErrorStreamStringGlobber extends Thread {
		private final InputStream is;
		private final StringBuilder sb;

		private ErrorStreamStringGlobber(InputStream is) {
			this.is = is;
			this.sb = new StringBuilder();
		}

		@Override
		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		
		public String getErrorStreamAsString(){
			return sb.toString();
		}
	}
}
