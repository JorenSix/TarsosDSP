package be.hogent.tarsos.dsp.speechmusicdiscriminator.features;

import java.util.List;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.ClassificationLabel;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.TrainingDataSet;

/**
 * More variability in energy is expected in speech signals.
 * @author Joren Six
 */
public class EnergyStandardDeviation implements
		SpeechMusicDiscriminationFeature {

	@Override
	public double calculateFeature(AudioEvent e,
			List<Double> zeroCrossingRates, List<Double> energies) {
		return Math.sqrt(TrainingDataSet.varianceEstimatorForNormalDistribution(energies));
	}

	@Override
	public String name() {
		return "Standard deviation of energy of frames";
	}

	@Override
	public double trainedMean(ClassificationLabel label) {
		double value = Double.MAX_VALUE;
		if(label== ClassificationLabel.MUSIC){
			value = 0.029204;
		}else if(label== ClassificationLabel.SPEECH){
			value = 0.036072;
		}
		return value;
	}

	@Override
	public double trainedVariance(ClassificationLabel label) {
		double value = Double.MAX_VALUE;
		if(label== ClassificationLabel.MUSIC){
			value = 0.000298;
		}else if(label== ClassificationLabel.SPEECH){
			value = 0.000525;
		}
		return value;
	}

}
