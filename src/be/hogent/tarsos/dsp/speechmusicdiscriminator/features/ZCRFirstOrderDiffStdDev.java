package be.hogent.tarsos.dsp.speechmusicdiscriminator.features;

import java.util.ArrayList;
import java.util.List;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.ClassificationLabel;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.TrainingDataSet;

public class ZCRFirstOrderDiffStdDev implements SpeechMusicDiscriminationFeature{

	@Override
	public double calculateFeature(AudioEvent e,
			List<Double> zeroCrossingRates, List<Double> energies) {
		
		List<Double> firstOrderDifferenceOfZeroCrossingRate = new ArrayList<Double>();
		
		for(int i = 1 ; i < zeroCrossingRates.size() ; i++ ){
			firstOrderDifferenceOfZeroCrossingRate.add(zeroCrossingRates.get(i-1) - zeroCrossingRates.get(i));
		}
		return  Math.sqrt((TrainingDataSet.varianceEstimatorForNormalDistribution(firstOrderDifferenceOfZeroCrossingRate)));
	}

	@Override
	public String name() {
		return "Standard deviation of the first order differential of the ZCR";
	}

	@Override
	public double trainedMean(ClassificationLabel label) {
		double value = Double.MAX_VALUE;
		if(label== ClassificationLabel.MUSIC){
			value = 0.030913;
		}else if(label== ClassificationLabel.SPEECH){
			value = 0.051953;
		}
		return value;
	}

	@Override
	public double trainedVariance(ClassificationLabel label) {
		double value = Double.MAX_VALUE;
		if(label== ClassificationLabel.MUSIC){
			value = 0.000155;
		}else if(label== ClassificationLabel.SPEECH){
			value = 0.000247;
		}
		return value;
	}
}
