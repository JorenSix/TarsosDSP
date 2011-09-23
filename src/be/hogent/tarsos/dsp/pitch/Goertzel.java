package be.hogent.tarsos.dsp.pitch;

public class Goertzel {
	
	private final float sampleRate;
	
	private final int size;
	
	public Goertzel(final float audioSampleRate, final int bufferSize) {
		sampleRate = audioSampleRate;
		size = bufferSize;
	}
	
	
	public double process(final float[] audioBuffer,float frequency){
		double skn0,skn1,skn2;
		skn0 = skn1 = skn2 = 0;
		
		for(int i = 0 ; i < audioBuffer.length ; i++){
			skn2 = skn1;
			skn1 = skn0;
			skn0 = 2 * Math.cos(2 * Math.PI * frequency / sampleRate) * skn1 - skn2 + audioBuffer[i];
		}
		
		double wnk = Math.exp(-2 * Math.PI * frequency/sampleRate);
		
		return 20 * Math.log10(Math.abs(skn0 - wnk * skn1));		
	}
	
	

}
