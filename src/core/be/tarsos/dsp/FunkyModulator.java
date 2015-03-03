package be.tarsos.dsp;

import javax.sound.sampled.LineUnavailableException;

import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;

public class FunkyModulator implements AudioProcessor{
	
	private final FFT fft;
	private final int size;
	private final float[] currentMagnitudes;
	private final float[] currentPhase;
	private final float[] currentFrequencies;
	
	private  float[] previousPhase;
	
	private final double pitchShiftRatio = 1.1f;
	private final float[] inverseWindow;
	
	/**
	 * Cached calculations for the frequency calculation
	 */
	private final double timeDelta;
	private final double binSizeInHz;
	private final double binSizeInHzTimesTimeDelta;
	private final double oneOverTwoPi;
	private final double oneOverTimeDelta;
	private final double oneOVerTwoPiTimeDelta;
	private final double sampleRate;
	
	public FunkyModulator(int size,double sampleRate){
		fft = new FFT(size, new HammingWindow());
		
		this.size = size;
		currentMagnitudes = new float[size/2];
		currentFrequencies= new float[size/2];
		currentPhase= new float[size/2];
		previousPhase = null;
		this.sampleRate = sampleRate;
		
		inverseWindow = new HammingWindow().generateCurve(size);
		for(int i = 0 ; i < inverseWindow.length ; i++){
			inverseWindow[i] = 1.0f/inverseWindow[i];
		}
		
		//time jumps in seconds, from one analysis frame to the other
		timeDelta = (size ) / sampleRate;
		//bin size in Hz times the timeDelta
		binSizeInHzTimesTimeDelta = (double) (timeDelta * sampleRate / (double) size);
		binSizeInHz = (double) (sampleRate / (double) size);

		oneOverTwoPi = (double) (1.0 / (2.0 * Math.PI));
		oneOverTimeDelta = (double) (1.0 / timeDelta);
		oneOVerTwoPiTimeDelta = (double) (oneOverTimeDelta * oneOverTwoPi);
	}
	
	/**
	 * Calculates a frequency for a bin using phase info, if available.
	 * @param binIndex The FFT bin index.
	 * @return a frequency, in Hz, calculated using available phase info.
	 */
	private float getFrequencyForBin(int binIndex) {
		final float frequencyInHertz;
		// use the phase delta information to get a more precise
		// frequency estimate
		// if the phase of the previous frame is available.
		// See
		// * Moore 1976
		// "The use of phase vocoder in computer music applications"
		// * Sethares et al. 2009 - Spectral Tools for Dynamic
		// Tonality and Audio Morphing
		// * Laroche and Dolson 1999
		if (previousPhase!=null) {
			float phaseDelta =  previousPhase[binIndex] - currentPhase[binIndex];
			long k = Math.round(binSizeInHzTimesTimeDelta * binIndex - oneOverTwoPi * phaseDelta);
			frequencyInHertz = (float) (oneOVerTwoPiTimeDelta * phaseDelta  + oneOverTimeDelta * k);
		} else {
			frequencyInHertz = (float) fft.binToHz(binIndex, (float)sampleRate);
		}
		return frequencyInHertz;
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		//see http://downloads.dspdimension.com/smbPitchShift.cpp
			
		///Analysis****
		float[] fftData = audioEvent.getFloatBuffer().clone();
		
		//Fourier transform the audio 
		fft.forwardTransform(fftData);
		//Calculate the magnitudes and phase information. 
		fft.powerAndPhaseFromFFT(fftData, currentMagnitudes, currentPhase);
		
		
		for(int i = 0 ; i < size/2 ; i++){
			currentFrequencies[i]=getFrequencyForBin(i);
		}
		
		///Processing****
		///actual pitch shifting
		float[] newMagnitudes = new float[size/2];
		float[] newFrequencies = new float[size/2];
		for(int i =0 ; i < size/2 ; i++){
			int index = (int)(i * pitchShiftRatio);
			if(index < size/2){
				newMagnitudes[index] = currentMagnitudes[i];
				newFrequencies[index] = (float) (currentFrequencies[i]*pitchShiftRatio);
			}
		}
		
		///Synthesis****
		float[] newFFTData = new float[size];
		for(int i =0 ; i < size/2 ; i++){
			
			/* subtract bin mid frequency */
			float differenceFromMidFrequency = newFrequencies[i] - (float)(i*binSizeInHz);
			
			/* get bin deviation from freq deviation */
			float frequencyDeviation = (float)(differenceFromMidFrequency/binSizeInHz);
			
			
			float phase =  (float) (2* Math.PI * frequencyDeviation);
			
			newFFTData[2*i] = (float) (newMagnitudes[i] * Math.cos(phase));
			newFFTData[2*i+1] = (float) (newMagnitudes[i]* Math.sin(phase));
		}
		
		
		fft.backwardsTransform(newFFTData);
		for(int i = 0 ; i < newFFTData.length ; i ++){
			newFFTData[i] = newFFTData[i] * inverseWindow[i] * 0.05f;
		}
		
		audioEvent.clearFloatBuffer();
		audioEvent.setFloatBuffer(newFFTData);
		return true;
	}

	@Override
	public void processingFinished() {
		
	}
	
	public static void main(String... args) throws LineUnavailableException{
		AudioDispatcher adp = AudioDispatcherFactory.fromPipe("/home/joren/Desktop/02 My Ship.mp3", 16000, 2048*20,0);
		TarsosDSPAudioFormat format = adp.getFormat();
		adp.addAudioProcessor(new FunkyModulator(4096*20, 16000));
		adp.addAudioProcessor(new AudioPlayer(JVMAudioInputStream.toAudioFormat(format)));
		adp.run();
	}

}
