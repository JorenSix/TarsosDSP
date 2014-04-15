package be.hogent.tarsos.dsp.speechmusicdiscriminator.features;

import java.util.List;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.ClassificationLabel;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.TrainingDataSet;

public class ZCRSkewness implements SpeechMusicDiscriminationFeature {

	@Override
	public double calculateFeature(AudioEvent e,
			List<Double> zeroCrossingRates, List<Double> energies) {
		double value = TrainingDataSet.skewness(zeroCrossingRates);
		return value;
	}

	@Override
	public String name() {
		return "ZCR Skewness or third moment";
	}
	
	@Override
	public double trainedMean(ClassificationLabel label) {
		double value = Double.MAX_VALUE;
		if(label== ClassificationLabel.MUSIC){
			value = 0.747878;
		}else if(label== ClassificationLabel.SPEECH){
			value = 1.642797;
		}
		return value;
	}

	@Override
	public double trainedVariance(ClassificationLabel label) {
		double value = Double.MAX_VALUE;
		if(label== ClassificationLabel.MUSIC){
			value = 0.790454;
		}else if(label== ClassificationLabel.SPEECH){
			value = 0.984599;
		}
		return value;
	}

}
