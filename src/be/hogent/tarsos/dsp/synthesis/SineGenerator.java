package be.hogent.tarsos.dsp.synthesis;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;

public class SineGenerator implements AudioProcessor{
	
	private double gain;
	private double frequency;
	private double phase;
	
	public SineGenerator(){
		this(1.0,440);
	}
	
	public SineGenerator(double gain,double frequency){
		this.gain = gain;
		this.frequency = frequency;
		this.phase = 0;
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] buffer = audioEvent.getFloatBuffer();
		double sampleRate = audioEvent.getSampleRate();
		double twoPiF = 2 * Math.PI * frequency;
		double time = 0  ;
		for(int i = 0 ; i < buffer.length ; i++){
			time = i / sampleRate;
			buffer[i] += (float) (gain * Math.sin(twoPiF * time + phase));
		}
		phase = time;
		return true;
	}

	@Override
	public void processingFinished() {
	}
	
	

}
