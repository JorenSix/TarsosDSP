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

package be.tarsos.dsp.example.dissonance;

import be.tarsos.dsp.util.PitchConverter;

public class SensoryDissonanceResult {
	public final double hertzValue;
	public final double ratio;
	public final double dissonanceValue;
	
	public SensoryDissonanceResult(double ratio,double dissonanceValue, double hertzValue){
		this.ratio = ratio;
		this.dissonanceValue = dissonanceValue;
		this.hertzValue = hertzValue;
	}
	
	public double getdifferenceInCents(){
		return PitchConverter.ratioToCent(ratio);
	}
	
	public double getFrequencyInAbsoluteCents (){
		return PitchConverter.hertzToAbsoluteCent(hertzValue) +   getdifferenceInCents();
	}
	
	public double getFrequencyInHertz(){
		return PitchConverter.absoluteCentToHertz(getFrequencyInAbsoluteCents());
	}
}
