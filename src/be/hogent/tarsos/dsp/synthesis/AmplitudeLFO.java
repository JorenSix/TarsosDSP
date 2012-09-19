package be.hogent.tarsos.dsp.synthesis;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;

public class AmplitudeLFO implements AudioProcessor {
	
	private double frequency;
	private double scaleParameter;
	private double phase;
	
	public AmplitudeLFO(){
		this(1.5,0.75);
	}
	
	public AmplitudeLFO(double frequency, double scaleParameter){
		this.frequency = frequency;
		this.scaleParameter = scaleParameter;
		phase = 0;
	}
	

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] buffer = audioEvent.getFloatBuffer();
		double sampleRate = audioEvent.getSampleRate();
		double twoPiF = 2 * Math.PI * frequency;
		double time = 0;
		for(int i = 0 ; i < buffer.length ; i++){
			time = i / sampleRate;
			float gain =  (float) (scaleParameter * Math.sin(twoPiF * time + phase));
			buffer[i] = gain * buffer[i];
		}
		phase = twoPiF * buffer.length / sampleRate + phase;
		return true;
	}

	@Override
	public void processingFinished() {
	}

}
