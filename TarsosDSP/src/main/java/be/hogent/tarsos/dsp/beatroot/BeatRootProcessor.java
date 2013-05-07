package be.hogent.tarsos.dsp.beatroot;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;

public class BeatRootProcessor implements AudioProcessor {
	/** RMS amplitude of the current frame. */
	private double frameRMS;
	
	/** The number of overlapping frames of audio data which have been read. */
	private int frameCount;

	/** Long term average frame energy (in frequency domain representation). */
	private double ltAverage;


	/** The window function for the STFT, currently a Hamming window. */
	private double[] window;

	/** The real part of the data for the in-place FFT computation.
	 *  Since input data is real, this initially contains the input data. */
	private double[] reBuffer;

	/** The imaginary part of the data for the in-place FFT computation.
	 *  Since input data is real, this initially contains zeros. */
	private double[] imBuffer;

	/** Spectral flux onset detection function, indexed by frame. */
	private double[] spectralFlux;
	
	/** A mapping function for mapping FFT bins to final frequency bins.
	 *  The mapping is linear (1-1) until the resolution reaches 2 points per
	 *  semitone, then logarithmic with a semitone resolution.  e.g. for
	 *  44.1kHz sampling rate and fftSize of 2048 (46ms), bin spacing is
	 *  21.5Hz, which is mapped linearly for bins 0-34 (0 to 732Hz), and
	 *  logarithmically for the remaining bins (midi notes 79 to 127, bins 35 to
	 *  83), where all energy above note 127 is mapped into the final bin. */
	private int[] freqMap;

	/** The number of entries in <code>freqMap</code>. Note that the length of
	 *  the array is greater, because its size is not known at creation time. */
	private int freqMapSize;

	/** The magnitude spectrum of the most recent frame.
	 *  Used for calculating the spectral flux. */
	private double[] prevFrame;
	
	/** The magnitude spectrum of the current frame. */
	private double[] newFrame;

	/** The magnitude spectra of all frames, used for plotting the spectrogram. */
	private double[][] frames;
	
	/** The RMS energy of all frames. */
	private double[] energy;
	
	/** Spacing of audio frames in samples (see <code>hopTime</code>) */
	protected int hopSize;

	/** The size of an FFT frame in samples (see <code>fftTime</code>) */
	protected int fftSize;

	
	/** Total number of audio frames if known, or -1 for live or compressed input. */
	private int totalFrames;
	
	/** RMS frame energy below this value results in the frame being set to zero,
	 *  so that normalisation does not have undesired side-effects. */
	public static double silenceThreshold = 0.0004;
	
	/** For dynamic range compression, this value is added to the log magnitude
	 *  in each frequency bin and any remaining negative values are then set to zero.
	 */
	public static double rangeThreshold = 10;
	
	/** Determines method of normalisation. Values can be:<ul>
	 *  <li>0: no normalisation</li>
	 *  <li>1: normalisation by current frame energy</li>
	 *  <li>2: normalisation by exponential average of frame energy</li>
	 *  </ul>
	 */
	public static int normaliseMode = 2;
	
	/** Ratio between rate of sampling the signal energy (for the amplitude envelope) and the hop size */
	public static int energyOversampleFactor = 2;
	
	private OnsetHandler handler;
	
	double hopTime;
	
	public BeatRootProcessor(AudioDispatcher d,int fftSize, int hopSize){
		this.hopSize = hopSize; 
		this.hopTime = hopSize/d.getFormat().getSampleRate();
		//no overlap
		totalFrames = (int)(d.durationInFrames() / hopSize) + 4;
		energy = new double[totalFrames*energyOversampleFactor];
		spectralFlux = new double[totalFrames];
		this.fftSize = fftSize;
		reBuffer = new double[fftSize];
		imBuffer = new double[fftSize];
		prevFrame = new double[fftSize];
		window = FFT.makeWindow(FFT.HAMMING, fftSize, fftSize);
		for (int i=0; i < fftSize; i++)
			window[i] *= Math.sqrt(fftSize);
		
		makeFreqMap(fftSize, d.getFormat().getSampleRate());
		newFrame = new double[freqMapSize];
		frames = new double[totalFrames][freqMapSize];
		
		handler = new PrintOnsetHandler();
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		frameRMS = audioEvent.getRMS()/2.0;
		float[] audioBuffer = audioEvent.getFloatBuffer();
		for (int i = 0; i < fftSize; i++) {
			reBuffer[i] = window[i] * audioBuffer[i];
		}
		Arrays.fill(imBuffer, 0);
		FFT.magnitudePhaseFFT(reBuffer, imBuffer);
		Arrays.fill(newFrame, 0);
		double flux = 0;
		for (int i = 0; i <= fftSize/2; i++) {
			if (reBuffer[i] > prevFrame[i])
				flux += reBuffer[i] - prevFrame[i];
			newFrame[freqMap[i]] += reBuffer[i];
		}
		spectralFlux[frameCount] = flux;
		for (int i = 0; i < freqMapSize; i++)
			frames[frameCount][i] = newFrame[i];
	
		int sz = (fftSize - hopSize) / energyOversampleFactor;
		int index = hopSize; 
		for (int j = 0; j < energyOversampleFactor; j++) {
			double newEnergy = 0;
			for (int i = 0; i < sz; i++) {
				newEnergy += audioBuffer[index] * audioBuffer[index];
				if (++index == fftSize)
					index = 0;				
			}
			energy[frameCount * energyOversampleFactor + j] =
					newEnergy / sz <= 1e-6? 0: Math.log(newEnergy / sz) + 13.816;
		}
		double decay = frameCount >= 200? 0.99:
					(frameCount < 100? 0: (frameCount - 100) / 100.0);
		if (ltAverage == 0)
			ltAverage = frameRMS;
		else
			ltAverage = ltAverage * decay + frameRMS * (1.0 - decay);
		if (frameRMS <= silenceThreshold)
			for (int i = 0; i < freqMapSize; i++)
				frames[frameCount][i] = 0;
		else {
			if (normaliseMode == 1)
				for (int i = 0; i < freqMapSize; i++)
					frames[frameCount][i] /= frameRMS;
			else if (normaliseMode == 2)
				for (int i = 0; i < freqMapSize; i++)
					frames[frameCount][i] /= ltAverage;
			for (int i = 0; i < freqMapSize; i++) {
				frames[frameCount][i] = Math.log(frames[frameCount][i]) + rangeThreshold;
				if (frames[frameCount][i] < 0)
					frames[frameCount][i] = 0;
			}
		}

		double[] tmp = prevFrame;
		prevFrame = reBuffer;
		reBuffer = tmp;
		frameCount++;
		return true;
	}
	
	/** Creates a map of FFT frequency bins to comparison bins.
	 *  Where the spacing of FFT bins is less than 0.5 semitones, the mapping is
	 *  one to one. Where the spacing is greater than 0.5 semitones, the FFT
	 *  energy is mapped into semitone-wide bins. No scaling is performed; that
	 *  is the energy is summed into the comparison bins. See also
	 *  processFrame()
	 */
	protected void makeFreqMap(int fftSize, float sampleRate) {
		freqMap = new int[fftSize/2+1];
		double binWidth = sampleRate / fftSize;
		int crossoverBin = (int)(2 / (Math.pow(2, 1/12.0) - 1));
		int crossoverMidi = (int)Math.round(Math.log(crossoverBin*binWidth/440)/
														Math.log(2) * 12 + 69);
		// freq = 440 * Math.pow(2, (midi-69)/12.0) / binWidth;
		int i = 0;
		while (i <= crossoverBin)
			freqMap[i++] = i;
		while (i <= fftSize/2) {
			double midi = Math.log(i*binWidth/440) / Math.log(2) * 12 + 69;
			if (midi > 127)
				midi = 127;
			freqMap[i++] = crossoverBin + (int)Math.round(midi) - crossoverMidi;
		}
		freqMapSize = freqMap[i-1] + 1;
	} // makeFreqMap()
	
	
	public void findOnsets(double p1, double p2){
		LinkedList<Integer> peaks = Peaks.findPeaks(spectralFlux, (int)Math.round(0.06 / hopTime), p1, p2, true);
		Iterator<Integer> it = peaks.iterator();
		double minSalience = Peaks.min(spectralFlux);
		for (int i = 0; i < peaks.size(); i++) {
			int index = it.next();
			double time  = index * hopTime;
			double salience = spectralFlux[index] - minSalience;
			handler.handleOnset(time,salience);
		}
	}
	
	public static interface OnsetHandler{
		public void handleOnset(double time, double salience);
	}
	
	public void setHandler(OnsetHandler handler) {
		this.handler = handler;
	}
	
	public class PrintOnsetHandler implements OnsetHandler{
		@Override
		public void handleOnset(double time, double salience) {
			System.out.println(String.format("%0.4f;%.4f", time,salience));	
		}		
	}
	

	@Override
	public void processingFinished() {
		double p1 = 0.35;
		double p2 = 0.84;
		Peaks.normalise(spectralFlux);
		findOnsets(p1, p2);

	}

}
