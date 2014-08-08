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
