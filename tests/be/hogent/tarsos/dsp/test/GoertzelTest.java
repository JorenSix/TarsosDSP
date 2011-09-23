package be.hogent.tarsos.dsp.test;

import org.junit.Test;

import be.hogent.tarsos.dsp.pitch.Goertzel;

public class GoertzelTest {
	
	public static float[] testAudioBufferSine(final double frequency,int size) {
		final double sampleRate = 44100.0;
		final double f0 = frequency;
		final double amplitudeF0 = 1.0;
		final float[] buffer = new float[size];
		for (int sample = 0; sample < buffer.length; sample++) {
			final double time = sample / sampleRate;
			buffer[sample] = (float) (amplitudeF0 * Math.sin(2 * Math.PI * f0
					* time));
		}
		return buffer;
	}
	
	@Test
	public void testDetection(){
		int size = 8820;
		Goertzel goertzel = new Goertzel(44100,size);
		int presentFrequency = 6000;
		
		float[] audio = testAudioBufferSine(presentFrequency, size);
		
		for(int i = 0 ; i < 100 ; i ++){
			System.out.println(audio[i]);
		}
		
		int stepsize = presentFrequency / 100;
		
		for(int i = 0 ; i < 200; i++){
			int guessFrequency = stepsize * i;
			double value = goertzel.process(audio,guessFrequency);
			System.out.println("Trying freq " + guessFrequency + " value: " + value + " actual frequency: " + presentFrequency);			
		}
	}

}
