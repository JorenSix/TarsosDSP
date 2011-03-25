package be.hogent.tarsos.dsp;

import be.hogent.tarsos.dsp.pitch.McLeodPitchMethod;
import be.hogent.tarsos.dsp.pitch.PitchDetector;
import be.hogent.tarsos.dsp.pitch.Yin;


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
		MPM
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
	final PitchDetector detector;
	final long lengthInSamples;
	final int overlap;
	final float sampleRate;
	final DetectedPitchHandler handler;
	long processedSamples;
	
	/**
	 * Initialize a new pitch processor.
	 * 
	 * @param algorithm
	 *            An enum defining the algorithm.
	 * @param sampleRate
	 *            The sample rate of the buffer (Hz).
	 * @param bufferSize
	 *            The size of the buffer in samples.
	 * @param bufferOverlap
	 *            The size of the overlap between two consecutive buffers (in
	 *            samples).
	 * @param totalLengthInSamples
	 *            The total length of the stream (in samples).
	 * @param handler
	 *            The handler handles detected pitch.
	 */
	public PitchProcessor(PitchEstimationAlgorithm algorithm, float sampleRate,
			int bufferSize, int bufferOverlap, long totalLengthInSamples,
			DetectedPitchHandler handler) {
		this.sampleRate = sampleRate;
		lengthInSamples = totalLengthInSamples;
		processedSamples = 0;
		this.handler = handler;
		overlap = bufferOverlap;
		if (PitchEstimationAlgorithm.MPM == algorithm) {
			detector = new McLeodPitchMethod(sampleRate, bufferSize);
		} else {
			detector = new Yin(sampleRate, bufferSize);
		}
	}

	@Override
	public boolean processFull(float[] audioFloatBuffer, byte[] audioByteBuffer) {
		this.processedSamples += audioFloatBuffer.length;
		float pitch = detector.getPitch(audioFloatBuffer);
		float probability = detector.getProbability();
		float timeStamp = processedSamples / sampleRate;
		float progress = processedSamples / (float) lengthInSamples;
		
		handler.handlePitch(pitch, probability, timeStamp, progress);
		return true;
	}

	@Override
	public boolean processOverlapping(float[] audioFloatBuffer,
			byte[] audioByteBuffer) {
		this.processedSamples -= overlap;
		return processFull(audioFloatBuffer,audioByteBuffer);

	}

	@Override
	public void processingFinished() {
	}
}
