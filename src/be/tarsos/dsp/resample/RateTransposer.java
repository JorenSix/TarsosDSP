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
package be.hogent.tarsos.dsp.resample;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;


/**
 * Sample rate transposer. Changes sample rate by using  interpolation 
 * 
 * Together with the time stretcher this can be used for pitch shifting.
 * @author Joren Six
 */
public class RateTransposer implements AudioProcessor {

	private double factor;
    private Resampler r;
    
	/**
	 * Create a new sample rate transposer. The factor determines the new sample
	 * rate. E.g. 0.5 is half the sample rate, 1.0 does not change a thing and
	 * 2.0 doubles the samplerate. If the samples are played at the original
	 * speed the pitch doubles (0.5), does not change (1.0) or halves (0.5)
	 * respectively. Playback length follows the same rules, obviously.
	 * 
	 * @param factor
	 *            Determines the new sample rate. E.g. 0.5 is half the sample
	 *            rate, 1.0 does not change a thing and 2.0 doubles the sample
	 *            rate. If the samples are played at the original speed the
	 *            pitch doubles (0.5), does not change (1.0) or halves (0.5)
	 *            respectively. Playback length follows the same rules,
	 *            obviously.
	 */
	public RateTransposer(double factor){
		this.factor = factor;
		r= new Resampler(false,0.1,4.0);
	}
	
	public void setFactor(double tempo){
		this.factor = tempo;
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] src = audioEvent.getFloatBuffer();
		//Creation of float array in loop could be prevented if src.length is known beforehand...
		//Possible optimization is to instantiate it outside the loop and get a pointer to the 
		//array here, in the process method method.
		float[] out = new float[(int) (src.length * factor)];
		r.process(factor, src, 0, src.length, false, out, 0, out.length);
		//The size of the output buffer changes (according to factor). 
		audioEvent.setFloatBuffer(out);
		return true;
	}

	@Override
	public void processingFinished() {

	}

}
