package be.hogent.tarsos.dsp.util;

import java.util.Arrays;

/**
 * Implements a moving mean adaptive threshold peak picker.
 * 
 * The implementation is a translation of peakpicker.c from Aubio, Copyright (C)
 * 2003-2009 Paul Brossier <piem@aubio.org>
 * 
 * @author Joren Six
 * @auhror Paul Brossiers
 */
public class PeakPicker {
	/** thresh: offset threshold [0.033 or 0.01] */
	private double threshold;
	/** win_post: median filter window length (causal part) [8] */
	private int win_post;
	/** pre: median filter window (anti-causal part) [post-1] */
	private int win_pre;
	
	/** biquad low pass filter */
	private BiQuadFilter biquad;
	
	/** original onsets */
	private float[] onset_keep;
	/** modified onsets */
	private float[] onset_proc;
	/** peak picked window [3] */
	private float[] onset_peek;
	/** scratch pad for biquad and median */
	private float[] scratch;
	
	private float lastPeekValue;
	
	/**
	 * Initializes a new moving mean adaptive threshold peak picker.
	 * 
	 * @param threshold
	 *            The threshold defines when a peak is selected. It should be
	 *            between zero and one, 0.3 is a reasonable value. If too many
	 *            peaks are detected go to 0.5 - 0.8.
	 */
	public PeakPicker(double threshold) {
		/* Low-pass filter cutoff [0.34, 1] */		
		biquad = new BiQuadFilter(0.1600,0.3200,0.1600,-0.5949,0.2348);
		this.threshold = threshold;
		win_post = 5;
		win_pre = 1;
		
		onset_keep = new float[win_post + win_pre +1];
		onset_proc =  new float[win_post + win_pre +1];
		scratch =  new float[win_post + win_pre +1];
		onset_peek = new float[3];		
	}
	
	/**
	 * Sets a new threshold.
	 * 
	 * @param threshold
	 *            The threshold defines when a peak is selected. It should be
	 *            between zero and one, 0.3 is a reasonable value. If too many
	 *            peaks are detected go to 0.5 - 0.8.
	 */
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	
	/**
	 * Modified version for real time, moving mean adaptive threshold this
	 * method is slightly more permissive than the off-line one, and yields to
	 * an increase of false positives.
	 * 
	 * @param onset
	 *            The new onset value.
	 * @return True if a peak is detected, false otherwise.
	 **/
	public boolean pickPeak(float onset) {
		float mean = 0.f;
		float median = 0.f;
		
		int length = win_post + win_pre + 1;
		
		
		/* store onset in onset_keep */
		/* shift all elements but last, then write last */
		/* for (i=0;i<channels;i++) { */
		for(int j=0;j<length-1;j++) {
			onset_keep[j] = onset_keep[j+1];
			onset_proc[j] = onset_keep[j];
		}
		onset_keep[length-1] = onset;
		onset_proc[length-1] = onset;
		
		/* filter onset_proc */
		/** \bug filtfilt calculated post+pre times, should be only once !? */
		biquad.doFiltering(onset_proc,scratch);

		/* calculate mean and median for onset_proc */
		
		/* copy to scratch */
		float sum = 0.0f;
		for (int j = 0; j < length; j++){
			scratch[j] = onset_proc[j];
			sum += scratch[j];
		}
		Arrays.sort(scratch);
		median = scratch[scratch.length/2];
		mean = sum/Float.valueOf(length);
				
		/* shift peek array */
		for (int j=0;j<3-1;j++){
			onset_peek[j] = onset_peek[j+1];
		}
		/* calculate new peek value */
		onset_peek[2] = (float) (onset_proc[win_post] - median - mean * threshold);
		
		boolean isPeak = isPeak(1);
		lastPeekValue = onset;
	
		return isPeak;
	}
	
	/**
	 * 
	 * @return The value of the last detected peak, or zero. 
	 */
	public float getLastPeekValue() {
		return lastPeekValue;
	}
	
	/**
	 * Returns true if the onset is a peak.
	 * 
	 * @param index
	 *            the index in onset_peak to check.
	 * @return True if the onset is a peak, false otherwise.
	 */
	private boolean isPeak(int index) {
		return (	onset_peek[index] > onset_peek[index - 1] &&
					onset_peek[index] > onset_peek[index + 1] && 
					onset_peek[index] > 0.);
	}
}
