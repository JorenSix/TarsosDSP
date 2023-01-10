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


package be.tarsos.dsp.pitch;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

/**
 * Is responsible to call a pitch estimation algorithm. It also calculates progress. 
 * The underlying pitch detection algorithm must implement the {@link PitchDetector} interface. 
 * @author Joren Six
 */
public class PitchProcessor implements AudioProcessor {
	
	/**
	 * A list of pitch estimation algorithms.
	 * @author Joren Six
	 */
	public enum PitchEstimationAlgorithm {
		/**
		 * See {@link Yin} for the implementation. Or see <a href=
		 * "http://recherche.ircam.fr/equipes/pcm/cheveign/ps/2002_JASA_YIN_proof.pdf"
		 * >the YIN article</a>.
		 */
		YIN,
		/**
		 * See {@link McLeodPitchMethod}. It is described in the article "<a
		 * href=
		 * "http://miracle.otago.ac.nz/postgrads/tartini/papers/A_Smarter_Way_to_Find_Pitch.pdf"
		 * >A Smarter Way to Find Pitch</a>".
		 */
		MPM,
		/**
		 * A YIN implementation with a faster  {@link FastYin} for the implementation. Or see <a href=
		 * "http://recherche.ircam.fr/equipes/pcm/cheveign/ps/2002_JASA_YIN_proof.pdf"
		 * >the YIN article</a>.
		 */
		FFT_YIN,
		/**
		 * An implementation of a dynamic wavelet pitch detection algorithm (See
		 * {@link DynamicWavelet}), described in a paper by Eric Larson and Ross
		 * Maddox 'Real-Time Time-Domain Pitch Tracking Using Wavelets'
		 */
		DYNAMIC_WAVELET,
		/**
		 * Returns the frequency of the FFT-bin with most energy.
		 */
		FFT_PITCH,
		/**
		 * A pitch extractor that extracts the Average Magnitude Difference
		 * (AMDF) from an audio buffer. This is a good measure of the Pitch (f0)
		 * of a signal.
		 */
		AMDF;
		
		/**
		 * Returns a new instance of a pitch detector object based on the provided values.
		 * @param sampleRate The sample rate of the audio buffer.
		 * @param bufferSize The size (in samples) of the audio buffer.
		 * @return A new pitch detector object.
		 */
		public PitchDetector getDetector(float sampleRate,int bufferSize){
			PitchDetector detector;
			if (this == MPM ) {
				detector = new McLeodPitchMethod(sampleRate, bufferSize);
			} else if(this == DYNAMIC_WAVELET ) {
				detector = new DynamicWavelet(sampleRate,bufferSize);
			} else if(this == FFT_YIN){
				detector = new FastYin(sampleRate, bufferSize);
			} else if(this==AMDF){
				detector = new AMDF(sampleRate, bufferSize);
			}  else {
				detector = new Yin(sampleRate, bufferSize);
			}
			return detector;
		}
		
	};
	
	/**
	 * The underlying pitch detector;
	 */
	private final PitchDetector detector;
	
	private final PitchDetectionHandler handler;
	
	/**
	 * Initialize a new pitch processor.
	 * 
	 * @param algorithm
	 *            An enum defining the algorithm.
	 * @param sampleRate
	 *            The sample rate of the buffer (Hz).
	 * @param bufferSize
	 *            The size of the buffer in samples.
	 * @param handler
	 *            The handler handles detected pitch.
	 */
	public PitchProcessor(PitchEstimationAlgorithm algorithm, float sampleRate,
			int bufferSize,
			PitchDetectionHandler handler) {
		detector = algorithm.getDetector(sampleRate, bufferSize);
		this.handler = handler;	
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audioFloatBuffer = audioEvent.getFloatBuffer();
		
		PitchDetectionResult result = detector.getPitch(audioFloatBuffer);
		
		
		handler.handlePitch(result,audioEvent);
		return true;
	}

	@Override
	public void processingFinished() {
	}

	
}
