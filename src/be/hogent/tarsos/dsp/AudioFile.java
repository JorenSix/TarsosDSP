package be.hogent.tarsos.dsp;

import javax.sound.sampled.AudioInputStream;

import be.hogent.tarsos.dsp.util.AudioResourceUtils;


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
public class AudioFile {
	
	//private final static Logger LOG = Logger.getLogger(AudioFile.class.getName());
	
	private final String resource;
	private static PipeDecoder pipeDecoder = new PipeDecoder();
	
	public static void setDecoder(PipeDecoder decoder){
		pipeDecoder = decoder;
	}
	
	private final PipeDecoder decoder;
	public AudioFile(String resource){
		this.resource = AudioResourceUtils.sanitizeResource(resource);
		decoder = pipeDecoder;
	}
	
	/**
	 * Return a one channel, signed PCM stream of audio of a defined sample rate. 
	 * @param targetSampleRate The target sample stream.
	 * @return An audio stream which can be used to read samples from.
	 */
	public AudioInputStream getMonoStream(int targetSampleRate){
		AudioInputStream stream = null;
		stream = decoder.getDecodedStream(resource, targetSampleRate);
		return stream;
	}
}
