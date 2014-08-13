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

public class DetermineDurationProcessor implements AudioProcessor {
	
	long durationInSamples;
	float sampleRate;
	AudioEvent lastEvent;

	@Override
	public boolean process(AudioEvent audioEvent) {
		lastEvent = audioEvent;
		return true;
	}
	
	public double getDurationInSeconds(){
		return durationInSamples/sampleRate;
	}
	
	public double getDurationInSamples(){
		return durationInSamples;
	}

	@Override
	public void processingFinished() {
		sampleRate = lastEvent.getSampleRate();
		durationInSamples = lastEvent.getSamplesProcessed() + lastEvent.getFloatBuffer().length;
	}
}
