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

package be.tarsos.dsp.resample;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

/**
 * Currently not working sample rate transposer, works only for integer factors. 
 * Changes sample rate by using linear interpolation.
 * 
 * Together with the time stretcher this can be used for pitch shifting.
 * @author Joren Six
 * @author Olli Parviainen
 */
public class SoundTouchRateTransposer implements AudioProcessor {

	private double rate;
	int slopeCount;
    double prevSample;
    private AudioDispatcher dispatcher;

    public void setDispatcher(AudioDispatcher newDispatcher){
		this.dispatcher = newDispatcher;
	}
    
	public SoundTouchRateTransposer(double d){
		this.rate = d;
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		int i, used;
		float[] src = audioEvent.getFloatBuffer();
		float[] dest = new float[(int) Math.round(audioEvent.getBufferSize() / rate)];
	    used = 0;
	    i = 0;

	    // Process the last sample saved from the previous call first...
	    while (slopeCount <= 1.0f) {
	        dest[i] = (float)((1.0f - slopeCount) * prevSample + slopeCount * src[0]);
	        i++;
	        slopeCount += rate;
	    }
	    slopeCount -= 1.0f;
	    end:
        while(true){
            while (slopeCount > 1.0f) {
                slopeCount -= 1.0f;
                used++;
                if (used >= src.length - 1) 
                	break end;
            }
            if(i < dest.length){
            	dest[i] = (float)((1.0f - slopeCount) * src[used] + slopeCount * src[used + 1]);
            }
            i++;
            slopeCount += rate;
        }
	    
	    //Store the last sample for the next round
	    prevSample = src[src.length - 1];
	    dispatcher.setStepSizeAndOverlap(dest.length, 0);
	    audioEvent.setFloatBuffer(dest);
		return true;
	}

	@Override
	public void processingFinished() {

	}

}
