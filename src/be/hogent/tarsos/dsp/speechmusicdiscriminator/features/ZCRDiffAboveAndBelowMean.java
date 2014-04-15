package be.hogent.tarsos.dsp.speechmusicdiscriminator.features;

import java.util.List;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.ClassificationLabel;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.TrainingDataSet;

public class ZCRDiffAboveAndBelowMean implements
		SpeechMusicDiscriminationFeature {

	@Override
	public double calculateFeature(AudioEvent e,
			List<Double> zeroCrossingRates, List<Double> energies) {
		double zeroCrossingRateMean = TrainingDataSet.mean(zeroCrossingRates);
		double zeroCrossingRateStandardDeviation = Math.sqrt(TrainingDataSet.varianceEstimatorForNormalDistribution(zeroCrossingRates));
		int aboveMeanZeroCrossingRate = 0;
		int belowMeanZeroCrossingRate = 0;
		for (double zeroCrossingRate : zeroCrossingRates) {
			if (zeroCrossingRate > (zeroCrossingRateMean + zeroCrossingRateStandardDeviation)) {
				aboveMeanZeroCrossingRate++;
			}
			if (zeroCrossingRate < (zeroCrossingRateMean - zeroCrossingRateStandardDeviation)) {
				belowMeanZeroCrossingRate++;
			}
		}
		return (belowMeanZeroCrossingRate - aboveMeanZeroCrossingRate)/(double) energies.size();
	}

	@Override
	public String name() {
		return "Difference between ZCR above and below a mean.";
	}

	@Override
	public double trainedMean(ClassificationLabel label) {
		double value = Double.MAX_VALUE;
		if(label== ClassificationLabel.MUSIC){
			value = -0.010935;
		}else if(label== ClassificationLabel.SPEECH){
			value = -0.052835;
		}
		return value;
	}

	@Override
	public double trainedVariance(ClassificationLabel label) {
		double value = Double.MAX_VALUE;
		if(label== ClassificationLabel.MUSIC){
			value = 0.002577;
		}else if(label== ClassificationLabel.SPEECH){
			value = 0.005349;
		}
		return value;
	}

}
