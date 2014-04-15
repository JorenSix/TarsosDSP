package be.hogent.tarsos.dsp.speechmusicdiscriminator.features;

import java.util.List;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.ClassificationLabel;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.TrainingDataSet;

/**
 * 
 * Counts the number of times the energy of a frame is below the mean frame
 * energy minus the standard deviation of the energy. From <a
 * href="http://www.ee.columbia.edu/~dpwe/papers/ScheiS97-mussp.pdf"
 * >CONSTRUCTION AND EVALUATION OF A ROBUST MULTIFEATURE SPEECH/MUSIC
 * DISCRIMINATOR - Eric Scheirer and Malcolm Slaney</a> 
 * 
 * <blockquote>Percentage
 * of “Low-Energy” Frames: The proportion of frames with RMS power less than 50%
 * of the mean RMS power within a one-second window. The energy distribution for
 * speech is more left-skewed than for music—there are more quiet frames—so this
 * measure will be higher for speech than for music [2]. 
 * </blockquote>
 * 
 * @author Joren Six
 * 
 */
public class EnergyBelowThreshold implements SpeechMusicDiscriminationFeature {

	@Override
	public double calculateFeature(AudioEvent e,List<Double> zeroCrossingRates, List<Double> energies) {
		double mean = TrainingDataSet.mean(energies);
		double stdDev = Math.sqrt(TrainingDataSet.varianceEstimatorForNormalDistribution(energies));
		double threshold = mean - stdDev;
		double energyBelowThreshold=0;
		for(int i = 0 ; i < energies.size() ; i++){
			if(energies.get(i) < threshold){
				energyBelowThreshold ++;
			}
		}
		return energyBelowThreshold / (double) energies.size();
	}

	@Override
	public String name() {
		return "Energy frames below a certain threshold.";
	}

	@Override
	public double trainedMean(ClassificationLabel label) {
		double value = Double.MAX_VALUE;
		if(label== ClassificationLabel.MUSIC){
			value = 0.140628;
		}else if(label== ClassificationLabel.SPEECH){
			value = 0.142575;
		}
		return value;
	}

	@Override
	public double trainedVariance(ClassificationLabel label) {
		double value = Double.MAX_VALUE;
		if(label== ClassificationLabel.MUSIC){
			value = 0.002488;
		}else if(label== ClassificationLabel.SPEECH){
			value = 0.011010;
		}
		return value;
	}
}
