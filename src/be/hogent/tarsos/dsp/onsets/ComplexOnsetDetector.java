package be.hogent.tarsos.dsp.onsets;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.util.PeakPicker;
import be.hogent.tarsos.dsp.util.fft.FFT;
import be.hogent.tarsos.dsp.util.fft.HannWindow;

/**
 * A complex Domain Method onset detection function
 * 
 * Christopher Duxbury, Mike E. Davies, and Mark B. Sandler. Complex domain
 * onset detection for musical signals. In Proceedings of the Digital Audio
 * Effects Conference, DAFx-03, pages 90-93, London, UK, 2003
 * 
 * The implementation is a translation of onset.c from Aubio, Copyright (C)
 * 2003-2009 Paul Brossier <piem@aubio.org>
 * 
 * @author Joren Six
 * @author Paul Brossiers
 */
public class ComplexOnsetDetector implements AudioProcessor, OnsetDetector{
	
	
	/**
	 * The threshold to define silence, in dbSPL.
	 */
	private final double silenceThreshold;
	
	/**
	 * The minimum IOI (inter onset interval), in seconds.
	 */
	private final double minimumInterOnsetInterval;
	
	/**
	 * The last detected onset, in seconds.
	 */
	private double lastOnset;
	
	private final PeakPicker peakPicker;
	
	private OnsetHandler handler;
	
	
	/**
	 * To calculate the FFT.
	 */
	private final FFT fft;
	
	/**
	 * Previous phase vector, one frame behind
	 */
	private final float[] theta1;
	/**
	 * Previous phase vector, two frames behind
	 */
	private final float[] theta2;
	
	/**
	 * Previous norm (power, magnitude) vector
	 */
	private final float[] oldmag;
	
	/**
	 * Current onset detection measure vector 
	 */
	private final float[] dev1;
	
	/**
	 * 
	 * @param fftSize The size of the fft to take (e.g. 512)
	 * @param peakThreshold A threshold used for peak picking. Values between 0.1 and 0.8. Default is 0.3, if too many onsets are detected adjust to 0.4 or 0.5.
	 * @param silenceThreshold The threshold that defines when a buffer is silent. Default is -70dBSPL. -90 is also used.
	 * @param minimumInterOnsetInterval The minimum inter-onset-interval in seconds. When two onsets are detected within this interval the last one does not count. Default is 0.004 seconds.
	 */
	public ComplexOnsetDetector(int fftSize,double peakThreshold,double minimumInterOnsetInterval,double silenceThreshold){
		fft = new FFT(fftSize,new HannWindow());
		this.silenceThreshold = silenceThreshold;
		this.minimumInterOnsetInterval = minimumInterOnsetInterval;
		
		peakPicker = new PeakPicker(peakThreshold);
		
		int rsize = fftSize/2+1;
		oldmag = new float[rsize];
		dev1 = new float[rsize];
		theta1 = new float[rsize];
		theta2 = new float[rsize];
		
		handler = new PrintOnsetHandler();
	}
	
	public ComplexOnsetDetector(int fftSize){
		this(fftSize,0.3);
	}
	
	public ComplexOnsetDetector(int fftSize,double peakThreshold){
		this(fftSize,peakThreshold,0.03);
	}
	
	public ComplexOnsetDetector(int fftSize,double peakThreshold,double minimumInterOnsetInterval){
		this(fftSize,peakThreshold,minimumInterOnsetInterval,-70.0);
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		onsetDetection(audioEvent);
		return true;
	}
	
	
	private void onsetDetection(AudioEvent audioEvent){
		//calculate the complex fft (the magnitude and phase)
		float[] data = audioEvent.getFloatBuffer().clone();
		float[] power = new float[data.length/2];
		float[] phase = new float[data.length/2];
		fft.powerPhaseFFT(data, power, phase);
		
		float onsetValue = 0;
		
		for(int j = 0 ; j < power.length ; j++){
			//int imgIndex = (power.length - 1) * 2 - j;
			
			 // compute the predicted phase
			dev1[j] = 2.f * theta1[j] - theta2[j];
			
			// compute the euclidean distance in the complex domain
		    // sqrt ( r_1^2 + r_2^2 - 2 * r_1 * r_2 * \cos ( \phi_1 - \phi_2 ) )
			onsetValue += Math.sqrt(Math.abs(Math.pow(oldmag[j],2) + Math.pow(power[j],2) - 2. * oldmag[j] *power[j] * Math.cos(dev1[j] - phase[j])));
					
			/* swap old phase data (need to remember 2 frames behind)*/
			theta2[j] = theta1[j];
			theta1[j] = phase[j];
			
			/* swap old magnitude data (1 frame is enough) */
			oldmag[j]= power[j];
		}
		
		
		boolean isOnset = peakPicker.pickPeak(onsetValue);
		if(isOnset){
			if(audioEvent.isSilence(silenceThreshold)){
				isOnset = false;
			} else {				
				double delay = ((audioEvent.getOverlap()  * 4.3 ))/ audioEvent.getSampleRate(); 
				double onsetTime = audioEvent.getTimeStamp() - delay;
				if(onsetTime - lastOnset > minimumInterOnsetInterval){
					handler.handleOnset(onsetTime,peakPicker.getLastPeekValue());
					lastOnset = onsetTime;
				}
			}
		}
	}
	
	public void setHandler(OnsetHandler handler) {
		this.handler = handler;
	}	
	
	public void setThreshold(double threshold){
		this.peakPicker.setThreshold(threshold);
	}

	@Override
	public void processingFinished() {
		
	}
}
