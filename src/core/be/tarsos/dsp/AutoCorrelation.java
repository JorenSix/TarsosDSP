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

package be.tarsos.dsp;

public class AutoCorrelation implements AudioProcessor {

    private float result;
    
    public AutoCorrelation(){
    }
    
    @Override
    public boolean process(AudioEvent audioEvent) {
        float[] audioFloatbuffer = audioEvent.getFloatBuffer();
        
        result = 0;

        for (int i=0; i<audioFloatbuffer.length; i++){
        	result += ((float)Math.abs(audioFloatbuffer[i]))/(float)audioFloatbuffer.length;
        }
        return true;
    }
    
    @Override
    public void processingFinished() {
    }

    public float[] getValues() {
    	float[] returnValue = new float[1];
    	returnValue[0] = result;
        return returnValue;
    }
}
