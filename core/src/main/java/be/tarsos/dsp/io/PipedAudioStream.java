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

import java.io.InputStream;

import be.tarsos.dsp.util.AudioResourceUtils;


/**
 * An audio file can be used to convert and read from. It uses libAV to convert
 * about any audio format to a one channel PCM stream of a chosen sample rate. There is 
 * support for movie files as well, the first audio channel is then used as input.
 * The resource is either a local file or a type of stream supported by libAV (e.g. HTTP streams);
 * 
 * For a list of audio decoders the following command is practical:
 * <pre>
avconv -decoders | grep -E "^A" | sort
 

A... 8svx_exp             8SVX exponential
A... 8svx_fib             8SVX fibonacci
A... aac                  AAC (Advanced Audio Coding)
A... aac_latm             AAC LATM (Advanced Audio Coding LATM syntax)
...
 * </pre>
 */
public class PipedAudioStream {
	
	//private final static Logger LOG = Logger.getLogger(PipedAudioStream.class.getName());
	
	private final String resource;
	private static PipeDecoder pipeDecoder = new PipeDecoder();
	
	public static void setDecoder(PipeDecoder decoder){
		pipeDecoder = decoder;
	}
	
	private final PipeDecoder decoder;
	public PipedAudioStream(String resource){
		this.resource = AudioResourceUtils.sanitizeResource(resource);
		decoder = pipeDecoder;
	}
	
	/**
	 * Return a one channel, signed PCM stream of audio of a defined sample rate. 
	 * @param targetSampleRate The target sample stream.
	 * @param startTimeOffset The start time offset.
	 * @return An audio stream which can be used to read samples from.
	 */
	public TarsosDSPAudioInputStream getMonoStream(int targetSampleRate,double startTimeOffset){
		return getMonoStream(targetSampleRate, startTimeOffset,-1);
	}
	
	private TarsosDSPAudioFormat getTargetFormat(int targetSampleRate){
		return new TarsosDSPAudioFormat(targetSampleRate, 16, 1, true, false);
	}


	/**
	 * Return a one channel, signed PCM stream of audio of a defined sample rate. 
	 * @param targetSampleRate The target sample stream.
	 * @param startTimeOffset The start time offset.
	 * @param numberOfSeconds the number of seconds to pipe. If negative the stream is processed until end of stream.
	 * @return An audio stream which can be used to read samples from.
	 */
	public TarsosDSPAudioInputStream getMonoStream(int targetSampleRate, double startTimeOffset,
			double numberOfSeconds) {
		InputStream stream = null;
		stream = decoder.getDecodedStream(resource, targetSampleRate,startTimeOffset,numberOfSeconds);
		return new UniversalAudioInputStream(stream, getTargetFormat(targetSampleRate));
	}
}
