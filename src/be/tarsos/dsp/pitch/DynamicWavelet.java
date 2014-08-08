/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -----------------------------------------------------------
*
*  TarsosDSP is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://tarsos.0110.be/releases/TarsosDSP/
*  
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
* 
*/
package be.hogent.tarsos.dsp.pitch;

import java.util.Arrays;

/* dywapitchtrack.c

Dynamic Wavelet Algorithm Pitch Tracking library
Released under the MIT open source licence
 
Copyright (c) 2010 Antoine Schmitt

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

/**
 * <p>
 * The pitch is the main frequency of the waveform (the 'note' being played or
 * sung). It is expressed as a float in Hz.
 * </p>
 * Unlike the human ear, pitch detection is difficult to achieve for computers.
 * Many algorithms have been designed and experimented, but there is no 'best'
 * algorithm. They all depend on the context and the tradeoffs acceptable in
 * terms of speed and latency. The context includes the quality and 'cleanness'
 * of the audio : obviously polyphonic sounds (multiple instruments playing
 * different notes at the same time) are extremely difficult to track,
 * percussive or noisy audio has no pitch, most real-life audio have some noisy
 * moments, some instruments have a lot of harmonics, etc... </p>
 * <p>
 * The dywapitchtrack is based on a custom-tailored algorithm which is of very
 * high quality: both very accurate (precision < 0.05 semitones), very low
 * latency (< 23 ms) and very low error rate. It has been thoroughly tested on
 * human voice.
 * </p>
 * <p>
 * It can best be described as a dynamic wavelet algorithm (dywa):
 * </p>
 * <p>
 * The heart of the algorithm is a very powerful wavelet algorithm, described in
 * a paper by Eric Larson and Ross Maddox <a href= http://online.physics.uiuc
 * .edu/courses/phys498pom/NSF_REU_Reports/2005_reu/Real
 * -Time_Time-Domain_Pitch_Tracking_Using_Wavelets.pdf">"Real-Time Time-Domain
 * Pitch Tracking Using Wavelets</a>
 * </p>
 * 
 * @author Antoine Schmitt
 * @author Joren Six
 * 
 */
public class DynamicWavelet implements PitchDetector{
	
	// algorithm parameters
	private final int maxFLWTlevels = 6;
	private final double maxF = 3000.;
	private final int differenceLevelsN = 3;
	private final double maximaThresholdRatio = 0.75;
	
	/**
	 * The result of the pitch detection iteration.
	 */
	private final PitchDetectionResult result;
	
	private final float sampleRate;
	
	int[] distances; 
	int[] mins;
	int[] maxs;
	
	public DynamicWavelet(float sampleRate,int bufferSize){
		this.sampleRate = sampleRate;
		
		distances = new int[bufferSize];
		mins = new int[bufferSize];
		maxs = new int[bufferSize];
		result = new PitchDetectionResult();
	}
	
	@Override
	public PitchDetectionResult getPitch(float[] audioBuffer) {
		float pitchF = -1.0f;
		
		int curSamNb = audioBuffer.length;
	
		int nbMins;
		int nbMaxs;
		
		//check if the buffer size changed
		if(distances.length == audioBuffer.length){
			//if not fill the arrays with zero
			Arrays.fill(distances,0);
			Arrays.fill(mins,0);
			Arrays.fill(maxs,0);
		} else {
			//otherwise create new ones 
			distances = new int[audioBuffer.length];
			mins = new int[audioBuffer.length];
			maxs = new int[audioBuffer.length];
		}
		
		double ampltitudeThreshold;  
		double theDC = 0.0;
		
		
		//compute ampltitudeThreshold and theDC
		//first compute the DC and maxAMplitude
		double maxValue = 0.0;
		double minValue = 0.0;
		for (int i = 0; i < audioBuffer.length;i++) {
			double sample = audioBuffer[i];
			theDC = theDC + sample;
			maxValue = Math.max(maxValue, sample);
			minValue = Math.min(sample, minValue);
		}
		theDC = theDC/audioBuffer.length;
		maxValue = maxValue - theDC;
		minValue = minValue - theDC;
		double amplitudeMax = (maxValue > -minValue ? maxValue : -minValue);
		
		ampltitudeThreshold = amplitudeMax*maximaThresholdRatio;
		
		// levels, start without downsampling..
		int curLevel = 0;
		double curModeDistance = -1.;
		int delta;
		
		//TODO: refactor to make this more java, break it up in methods, remove the wile and branching statements...
		
		search:
		while(true){
			delta = (int) (sampleRate / (Math.pow(2, curLevel)*maxF));
			if (curSamNb < 2)
				break search;
			
			// compute the first maximums and minumums after zero-crossing
			// store if greater than the min threshold
			// and if at a greater distance than delta
			double dv, previousDV = -1000;
			
			nbMins = nbMaxs = 0;   
			int lastMinIndex = -1000000;
			int lastmaxIndex = -1000000;
			boolean findMax = false;
			boolean findMin = false;
			for (int i = 2; i < curSamNb; i++) {
				double si = audioBuffer[i] - theDC;
				double si1 = audioBuffer[i-1] - theDC;
				
				if(si1 <= 0 && si > 0) findMax = true;
				if(si1 >= 0 && si < 0) findMin = true;
				
				// min or max ?
				dv = si - si1;
				
				if (previousDV > -1000) {
					if (findMin && previousDV < 0 && dv >= 0) { 
					
						// minimum
						if (Math.abs(si) >= ampltitudeThreshold) {
							if (i > lastMinIndex + delta) {
								mins[nbMins++] = i;
								lastMinIndex = i;
								findMin = false;
							} 
						} 
					}
					
					if (findMax  && previousDV > 0 && dv <= 0) {
						// maximum
						if (Math.abs(si) >= ampltitudeThreshold) {
							if (i > lastmaxIndex + delta) {
								maxs[nbMaxs++] = i;
								lastmaxIndex = i;
								findMax = false;
							}
						}
					}
				}
				previousDV = dv;
			}
			
			if (nbMins == 0 && nbMaxs == 0) {
				// no best distance !
				//asLog("dywapitch no mins nor maxs, exiting\n");
				
				// if DEBUGG then put "no mins nor maxs, exiting"
				break search;
			}
			
			int d;
			Arrays.fill(distances, 0);
			for (int i = 0 ; i < nbMins ; i++) {
				for (int j = 1; j < differenceLevelsN; j++) {
					if (i+j < nbMins) {
						d = Math.abs(mins[i] - mins[i+j]);
						//asLog("dywapitch i=%ld j=%ld d=%ld\n", i, j, d);
						distances[d] = distances[d] + 1;
					}
				}
			}
			
			int bestDistance = -1;
			int bestValue = -1;
			for (int i = 0; i< curSamNb; i++) {
				int summed = 0;
				for (int j = -delta ; j <= delta ; j++) {
					if (i+j >=0 && i+j < curSamNb)
						summed += distances[i+j];
				}
				//asLog("dywapitch i=%ld summed=%ld bestDistance=%ld\n", i, summed, bestDistance);
				if (summed == bestValue) {
					if (i == 2*bestDistance)
						bestDistance = i;
					
				} else if (summed > bestValue) {
					bestValue = summed;
					bestDistance = i;
				}
			}
			
			// averaging
			double distAvg = 0.0;
			double nbDists = 0;
			for (int j = -delta ; j <= delta ; j++) {
				if (bestDistance+j >=0 && bestDistance+j < audioBuffer.length) {
					int nbDist = distances[bestDistance+j];
					if (nbDist > 0) {
						nbDists += nbDist;
						distAvg += (bestDistance+j)*nbDist;
					}
				}
			}
			
			// this is our mode distance !
			distAvg /= nbDists;
			//asLog("dywapitch distAvg=%f\n", distAvg);

			// continue the levels ?
			if (curModeDistance > -1.) {
				double similarity = Math.abs(distAvg*2 - curModeDistance);
				if (similarity <= 2*delta) {
					//if DEBUGG then put "similarity="&similarity&&"delta="&delta&&"ok"
	 				//asLog("dywapitch similarity=%f OK !\n", similarity);
					// two consecutive similar mode distances : ok !
					pitchF = (float) (sampleRate/(Math.pow(2,curLevel-1)*curModeDistance));
					break search;
				}
				//if DEBUGG then put "similarity="&similarity&&"delta="&delta&&"not"
			}
			
			// not similar, continue next level
			curModeDistance = distAvg;
			
						
			curLevel = curLevel + 1;
			if (curLevel >= maxFLWTlevels) {
				// put "max levels reached, exiting"
	 			//asLog("dywapitch max levels reached, exiting\n");
				break search;
			}
			
			// downsample
			if (curSamNb < 2) {
	 			//asLog("dywapitch not enough samples, exiting\n");
				break search;
			}
			//do not modify original audio buffer, make a copy buffer, if
			//downsampling is needed (only once).
			float[] newAudioBuffer = audioBuffer;
			if(curSamNb == distances.length){
				newAudioBuffer = new float[curSamNb/2];
			}
			for (int i = 0; i < curSamNb/2; i++) {
				newAudioBuffer[i] = (audioBuffer[2*i] + audioBuffer[2*i + 1])/2.0f;				
			}
			audioBuffer = newAudioBuffer;
			curSamNb /= 2;
		}		
		
		result.setPitch(pitchF);
		result.setPitched(-1!=pitchF);
		result.setProbability(-1);
		
		return result;
	}
}
