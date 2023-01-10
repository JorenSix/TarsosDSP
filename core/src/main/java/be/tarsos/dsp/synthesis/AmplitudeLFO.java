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

package be.tarsos.dsp.synthesis;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

/**
 * A low frequency oscillator to change the amplitude of a signal.
 */
public class AmplitudeLFO implements AudioProcessor {
	
	private double frequency;
	private double scaleParameter;
	private double phase;

	/**
	 * Create a new low frequency oscillator with a default frequency (1.5Hz and scale 0.75)
	 */
	public AmplitudeLFO(){
		this(1.5,0.75);
	}

	/**
	 * Create a new low frequency oscillator
	 * @param frequency The frequency in Hz
	 * @param scaleParameter The scale between 0 and 1 to modify the amplitude.
	 */
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
