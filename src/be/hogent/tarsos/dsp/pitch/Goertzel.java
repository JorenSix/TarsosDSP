package be.hogent.tarsos.dsp.pitch;

import be.hogent.tarsos.dsp.AudioProcessor;

public class Goertzel implements AudioProcessor{
	
	private final float sampleRate;
	
	private final int size;
	
	private final float[] frequenciesToDetect;
	private final double[] precalculatedCosines;
	private final double[] precalculatedWnk;
	
	private final FrequencyDetectedHandler handler;
	
	public Goertzel(final float audioSampleRate, final int bufferSize,float[] frequencies,FrequencyDetectedHandler handler) {
		sampleRate = audioSampleRate;
		size = bufferSize;
		frequenciesToDetect = frequencies;
		precalculatedCosines = new double[frequencies.length];
		precalculatedWnk = new double[frequencies.length];
		this.handler = handler;
		
		for(int i = 0 ; i < frequenciesToDetect.length ; i ++){
			precalculatedCosines[i] = 2 * Math.cos(2 * Math.PI * frequenciesToDetect[i] / sampleRate);
			precalculatedWnk[i] =  Math.exp(-2 * Math.PI * frequenciesToDetect[i]/sampleRate);
		}		
	}
	

	
	public static interface FrequencyDetectedHandler{
		void detectedFrequency(double frequency);
	}


	@Override
	public boolean processFull(float[] audioFloatBuffer, byte[] audioByteBuffer) {
		double skn0,skn1,skn2;
		
		
		for(int j = 0 ; j < frequenciesToDetect.length; j++){
			skn0 = skn1 = skn2 = 0;
			for(int i = 0 ; i < audioFloatBuffer.length ; i++){
				skn2 = skn1;
				skn1 = skn0;
				skn0 = precalculatedCosines[j]  * skn1 - skn2 + audioFloatBuffer[i];
			}	
			double wnk = precalculatedWnk[j];
			double power = 20 * Math.log10(Math.abs(skn0 - wnk * skn1));
			if(power > 50){
				//frequency detected!
				handler.detectedFrequency(frequenciesToDetect[j]);
				//break for (only one frequency can be detected).
				break;
			}
		}
		return false;
	}


	@Override
	public boolean processOverlapping(float[] audioFloatBuffer,
			byte[] audioByteBuffer) {
		processFull(audioFloatBuffer, audioByteBuffer);
		return false;
	}


	@Override
	public void processingFinished() {		
	}
	
	

}
