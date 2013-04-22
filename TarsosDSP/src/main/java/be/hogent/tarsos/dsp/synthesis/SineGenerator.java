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
		double time = 0;
		for(int i = 0 ; i < buffer.length ; i++){
			time = i / sampleRate;
			buffer[i] += (float) (gain * Math.sin(twoPiF * time + phase));
		}
		phase = twoPiF * buffer.length / sampleRate + phase;
		return true;
	}

	@Override
	public void processingFinished() {
	}
}
