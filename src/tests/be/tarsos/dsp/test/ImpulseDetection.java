package be.tarsos.dsp.test;

import be.tarsos.dsp.resample.Resampler;
import be.tarsos.dsp.wavelet.lift.Daubechies4Wavelet;

public class ImpulseDetection {

	public static void main(String[] args) {
		
		double sampleRate = 2000.0;
		double frequency = 8.0;
		double amplitude = 0.8;
		
		double twoPiF = 2 * Math.PI * frequency;
		
		float[] data = new float[512];
		for(int sample = 0 ; sample < data.length ; sample++){
			double time = sample / sampleRate;
			data[sample] = (float) (amplitude * Math.sin(twoPiF * time));
		}
		
		for(int sample = 0 ; sample < data.length ; sample++){
			//System.out.println(data[sample]);
		}
		
		/*
		float amount = 10;
		for(int sample = 205 ; sample < 215 ; sample++){
			data[sample] = (sample-205)/amount;
		}
		*/
		data[62] = 1.0f;
		
		Daubechies4Wavelet dwt = new Daubechies4Wavelet();
		dwt.forwardTrans(data);
		
		float[] levelFive = mra(data,5);//31.25 - 62.5
		float[] levelFour = mra(data,4);//62.5-125
		float[] levelThree = mra(data,3);//125-250
		float[] levelTwo = mra(data,2);//250-500Hz
		float[] levelOne = mra(data,1);//500-100Hz
		
		
		dwt.inverseTrans(data);
		normalize(data);
		for(int i = 0 ; i < levelFive.length ; i++){
			System.out.println(i+";"+data[i]+";"+levelFive[i]+";"+levelFour[i]+";"+levelThree[i]+";"+levelTwo[i]+";"+levelOne[i]);
		}
		
		double maxValue = 0;
		int maxIndex = 0;
		for(int i = 0 ; i < levelOne.length ; i++){
			if(Math.abs(levelOne[i])>maxValue){
				maxIndex = i;
				maxValue = Math.abs(levelOne[i]);
			}
		}
		System.out.println("Anomaly at " + maxIndex);
	}
	
	
	
	private static float[] mra(float[] data,int level){
		int length = (int)Math.pow(2,level);
		int startIndex = (int) (data.length/Math.pow(2,level));
		int stopIndex = (int) (data.length/Math.pow(2,level-1));
		float[] part = new float[stopIndex-startIndex];
		int j = 0;
		for(int i = startIndex ; i < stopIndex ;i++){
			part[j] = -1 * data[i];
			j++;
		}
		normalize(part);
		
		float factor =  data.length/(float)part.length;
		Resampler r= new Resampler(false,factor,factor);
		float[] out = new float[(int) (part.length * factor)];
		r.process(factor, part, 0, part.length, false, out, 0, out.length);
		
		float [] mra = new float[data.length];
		j=0;
		for(int i = 0 ; i < out.length ; i++){
			if((i+length/2)%length==0){
				mra[i] = part[j];
				j++;
			}
		}
		return mra;
	}
	
	private static void normalize(float[] data){
		float maxValue = 0;
		for(int i = 0 ; i < data.length ;i++){
			maxValue = Math.max(Math.abs(data[i]),maxValue);
		}
		for(int i = 0 ; i < data.length ;i++){
			data[i]=data[i]/maxValue;
		}
	}
}
