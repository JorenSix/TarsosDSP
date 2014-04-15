package be.hogent.tarsos.dsp.speechmusicdiscriminator.features;

import java.util.List;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.ClassificationLabel;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.TrainingDataSet;

public class ZCRAboveThreshold implements SpeechMusicDiscriminationFeature {
		
	@Override
	public double trainedMean(ClassificationLabel label) {
		double value = Double.MAX_VALUE;
		if(label== ClassificationLabel.MUSIC){
			value = 1.762295;
		}else if(label== ClassificationLabel.SPEECH){
			value = 3.918486;
		}
		return value;
	}
	
	@Override
	public double trainedVariance(ClassificationLabel label) {
		double value = Double.MAX_VALUE;
		if(label== ClassificationLabel.MUSIC){
			value = 3.940683;
		}else if(label== ClassificationLabel.SPEECH){
			value = 9.416086;
		}
		return value;
	}
	
	@Override
	public String name() {
		return "ZCR above a threshold";
	}
	
	@Override
	public double calculateFeature(AudioEvent e,List<Double> zeroCrossingRates,List<Double> energies) {
		double zeroCrossingRateMean = TrainingDataSet.mean(zeroCrossingRates);
		double zeroCrossingRateStandardDeviation = Math.sqrt(TrainingDataSet.varianceEstimatorForNormalDistribution(zeroCrossingRates));
		double aboveMeanZeroCrossingRateThreshold = 0;
		double zeroCrossingRateThreshold = zeroCrossingRateMean + 3 * zeroCrossingRateStandardDeviation;
		for (double zeroCrossingRate : zeroCrossingRates) {
			if (zeroCrossingRate > zeroCrossingRateThreshold) {
				aboveMeanZeroCrossingRateThreshold++;
			}
		}
		return aboveMeanZeroCrossingRateThreshold;
	}
}