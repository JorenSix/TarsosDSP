package be.hogent.tarsos.dsp.synthesis;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;

public class NoiseGenerator implements AudioProcessor{
	
	private double gain;
	
	public NoiseGenerator(){
		this(1.0);
	}
	
	public NoiseGenerator(double gain){
		this.gain = gain;
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] buffer = audioEvent.getFloatBuffer();
		for(int i = 0 ; i < buffer.length ; i++){
			buffer[i] += (float) (Math.random() * gain);
		}
		return true;
	}

	@Override
	public void processingFinished() {
	}
	
	

}
