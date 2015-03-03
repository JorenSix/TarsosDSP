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

package be.tarsos.dsp.example.dissonance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SensoryDissonanceCurve {
	
	private final double range;
	private final double increment = 0.005;
	
	private final List<SensoryDissonanceResult> curve;
	
	private final double dstar = 0.24;
	private final double s1= 0.0207;
	private final double s2=18.96;
	private final double c1=5;
	private final double c2=-5;
	private final double a1 = -3.51;
	private final double a2 = -5.75;
	
	public SensoryDissonanceCurve(){
		this(2.3);
	}
	
	/**
	 * Calcultate the dissonance curve until this factor. 2.3 means 
	 * one octave and one sixth of an octave. 
	 * @param range
	 */
	private SensoryDissonanceCurve(double range){
		this.range = range;
		this.curve = new ArrayList<SensoryDissonanceResult>();
	}
	
	
	public List<SensoryDissonanceResult> calculate(List<Double> frequencies,List<Double> amplitudes){
		curve.clear();
		curve.add(new SensoryDissonanceResult(1.0, 0.0, frequencies.get(0)));
		for(double alpha = 1 + increment; alpha<=range; alpha += increment ){
			List<Double> f = new ArrayList<Double>(frequencies);
			List<Double> a = new ArrayList<Double>(amplitudes);
			for(Double frequency:frequencies){
				f.add(alpha*frequency);
			}
			for(Double amplitude:amplitudes){
				a.add(amplitude);
			}
			double d = dissonanceMeasure(f, a);
			
			curve.add(new SensoryDissonanceResult(alpha, d, frequencies.get(0)));
		}
		return curve;
	}
	
	public List<SensoryDissonanceResult> valleys(List<SensoryDissonanceResult> results){
		List<SensoryDissonanceResult> valleys = new ArrayList<SensoryDissonanceResult>();
		for(int i = 1 ; i < results.size()-1; i++){
			SensoryDissonanceResult prevResult = results.get(i-1); 
			SensoryDissonanceResult currentResult = results.get(i);
			SensoryDissonanceResult nextResult = results.get(i+1);
			if(currentResult.dissonanceValue < nextResult.dissonanceValue && currentResult.dissonanceValue < prevResult.dissonanceValue){
				double rightSlope =  (nextResult.dissonanceValue - currentResult.dissonanceValue)/increment;
				double leftSlope =  (prevResult.dissonanceValue - currentResult.dissonanceValue)/increment;				
				if(rightSlope > 0.15 && leftSlope > 0.15){
					valleys.add(currentResult);
				}
			}
		}
		return valleys;
	}
	
	public double dissonanceMeasure(final List<Double> frequencies , List<Double> amplitudes){
		
		int n = frequencies.size();
		double d = 0;
		
		Collections.sort(frequencies);
		
		//create a list with indexes
		final Integer[] indexes = new Integer[frequencies.size()];
		for(int i = 0;i<frequencies.size() ; i++){
			indexes[i] = i;
		}
		//sort the indexes according to ascending frequency
		Arrays.sort(indexes, new Comparator<Integer>() {
			@Override 
			public int compare(final Integer o1, final Integer o2) {
				return Double.compare(frequencies.get(o1), frequencies.get(o2));
			}
		});
		
		List<Double> sortedFrequencies = new ArrayList<Double>(frequencies.size());
		List<Double> sortedAmplitudes = new ArrayList<Double>(frequencies.size());
		
		//create sorted frequency and amplitude lists
		for(int i = 0;i<frequencies.size() ; i++){
			sortedFrequencies.add(frequencies.get(indexes[i]));
			sortedAmplitudes.add(amplitudes.get(indexes[i]));
		}
		
		for(int i = 2;i<n ; i++){
			List<Double> fmin = new ArrayList<Double>(sortedFrequencies.subList(0, n - i + 1));
			for(int j = 0 ; j < fmin.size() ; j++){
				fmin.set(j, dstar / (s1*fmin.get(j)+s2));
			}
			
			List<Double> fdiff = new ArrayList<Double>();
			for(int j = i-1 ; j < sortedFrequencies.size(); j++){
				fdiff.add(sortedFrequencies.get(j) - sortedFrequencies.get(j-i+1));
			}
			
			List<Double> a = new ArrayList<Double>();
			for(int j = i - 1 ; j < sortedFrequencies.size() ; j++){
				a.add(sortedAmplitudes.get(j));
			}
			
			for(int j = 0 ; j < a.size() ; j++){
				double element= c1 * Math.exp(a1*fmin.get(j)*fdiff.get(j));
				double other = c2 * Math.exp(a2*fmin.get(j)*fdiff.get(j));
				d += a.get(j)* (element + other);
			}
			
		}
		return d;
	}
	
	
	public static void main(String...strings){
		
		List<Double> frequencies = new ArrayList<Double>();
		frequencies.add(440.0*1);
		frequencies.add(440.0*2);
		frequencies.add(440.0*3);
		frequencies.add(440.0*4);
		frequencies.add(440.0*5);
		//frequencies.add(440.0*6);
		//frequencies.add(440.0*7);
		//frequencies.add(440.0*8);
		
		List<Double> amplitudes = new ArrayList<Double>();
		amplitudes.add(Math.pow(0.88, 0));
		amplitudes.add(Math.pow(0.88, 1));
		amplitudes.add(Math.pow(0.88, 2));
		amplitudes.add(Math.pow(0.88, 3));
		amplitudes.add(Math.pow(0.88, 4));
		//amplitudes.add(Math.pow(0.88, 5));
		//amplitudes.add(Math.pow(0.88, 6));
		//amplitudes.add(Math.pow(0.88, 7));
		
		SensoryDissonanceCurve sdc = new SensoryDissonanceCurve();
		
		System.out.println("Expect  0.0049863 is " + sdc.dissonanceMeasure(frequencies,amplitudes));
		
		sdc.calculate(frequencies, amplitudes);
	}
    
}
