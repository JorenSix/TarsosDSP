/*
*
*  TarsosDSP is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*
*/

package be.hogent.tarsos.dsp.pitch;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;


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
		 * Maddox <a href= http://online.physics.uiuc
		 * .edu/courses/phys498pom/NSF_REU_Reports/2005_reu/Real
		 * -Time_Time-Domain_Pitch_Tracking_Using_Wavelets.pdf">"Real-Time
		 * Time-Domain Pitch Tracking Using Wavelets</a>
		 */
		DYNAMIC_WAVELET,
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
			} else {
				detector = new Yin(sampleRate, bufferSize);
			}
			return detector;
		}
		
	};
	
	/**
	 * An interface to handle detected pitch.
	 * 
	 * @author Joren Six
	 */
	public interface DetectedPitchHandler {
		/**
		 * Handle a detected pitch.
		 * 
		 * @param pitch
		 *            The pitch in Hz. -1 is returned if no pitch is detected.
		 * @param probability
		 *            The probability (a value between 0 and 1) represents how
		 *            periodic the signal is.
		 * @param timeStamp
		 *            A time stamp associated with the detection.
		 * @param progress
		 *            If the length of the stream is known beforehand (a file) a
		 *            progress indication is possible. It is a percentage. If a
		 *            stream is analyzed a negative value is returned.
		 */
		void handlePitch(float pitch, float probability, float timeStamp,
				float progress);
	}
	
	/**
	 * The underlying pitch detector;
	 */
	private final PitchDetector detector;
	
	private final DetectedPitchHandler handler;
	
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
			DetectedPitchHandler handler) {
		detector = algorithm.getDetector(sampleRate, bufferSize);
		this.handler = handler;	
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audioFloatBuffer = audioEvent.getFloatBuffer();
		
		float pitch = detector.getPitch(audioFloatBuffer);
		float probability = detector.getProbability();
		float timeStamp = (float) audioEvent.getTimeStamp();
		float progress = (float) audioEvent.getProgress();
		
		handler.handlePitch(pitch, probability, timeStamp, progress);
		return true;
	}

	@Override
	public void processingFinished() {
	}

	
}
