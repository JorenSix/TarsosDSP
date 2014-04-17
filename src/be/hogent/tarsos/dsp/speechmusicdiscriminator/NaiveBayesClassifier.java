package be.hogent.tarsos.dsp.speechmusicdiscriminator;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a naive bayes classifier. It operates on the assumptions that
 * each measurement is independent and normally distributed.
 * @author Joren Six
 *
 */
public class NaiveBayesClassifier {
	private final List<DataSet> datasets;
	
	public NaiveBayesClassifier(List<DataSet> datasets){
		this.datasets = datasets;
	}
	
	/**
	 * Classifies the measurements.
	 * @param values a vector with the measurements.
	 * @return the class
	 */
	public String classify(double[] values){
		double[] probabilities = new double[datasets.size()];
		
		int index = 0;
		for(DataSet dataset:datasets){
			double[] means = dataset.means();
			double[] variances = dataset.variances();
			double probability = 1;
			for(int i = 0 ; i< values.length ; i++){
				probability *= probability(values[i], variances[i], means[i]);
			}
			probabilities[index] = probability;
			index++;
		}
		
		int maxIndex = -1;
		double maxValue = -100000.0;
		for(int i = 0 ; i< probabilities.length ; i++){
		
			if(probabilities[i] > maxValue){
				maxIndex = i;
				maxValue  = probabilities[i];
			}
		}
		if(maxIndex==-1){
			System.out.println("Did not find max");
		}
		//double probability = probabilities[maxIndex];
		//System.out.println(probability * values.length);
		return datasets.get(maxIndex).label();
	}
	
	public static double probability(double v,double variance, double mean){
		double factor = 1.0/Math.sqrt(2*Math.PI*variance);
		return factor * Math.exp(-Math.pow(v-mean,2)/(2*variance));
	}
	
	public static void main(String...strings){
		//see this example: 
		// http://www.ic.unicamp.br/~rocha/teaching/2011s2/mc906/aulas/naive-bayes-classifier.pdf
		
		TrainingDataSet males = new TrainingDataSet("Male",3);
		
		males.setDescription(0,"height");
		males.addDataPoint(0,6.0);
		males.addDataPoint(0,5.92);
		males.addDataPoint(0,5.58);
		males.addDataPoint(0,5.92);
		
		males.setDescription(1,"weight");
		males.addDataPoint(1,180.0);
		males.addDataPoint(1,190.0);
		males.addDataPoint(1,170.0);
		males.addDataPoint(1,165.0);
		
		males.setDescription(2,"footSize");
		males.addDataPoint(2,12.0);
		males.addDataPoint(2,11.0);
		males.addDataPoint(2,12.0);
		males.addDataPoint(2,10.0);
		
		TrainingDataSet females = new TrainingDataSet("female",3);
		
		females.setDescription(0,"height");
		females.addDataPoint(0,5.0);
		females.addDataPoint(0,5.5);
		females.addDataPoint(0,5.42);
		females.addDataPoint(0,5.75);
		
		females.setDescription(1,"weight");
		females.addDataPoint(1,100.0);
		females.addDataPoint(1,150.0);
		females.addDataPoint(1,130.0);
		females.addDataPoint(1,150.0);
		
		females.setDescription(2,"foot size");
		females.addDataPoint(2,6.0);
		females.addDataPoint(2,8.0);
		females.addDataPoint(2,7.0);
		females.addDataPoint(2,9.0);
		
		System.out.println(females.toString());
		System.out.println(males.toString());
		
		List<DataSet> dataSets = new ArrayList<DataSet>();
		dataSets.add(females);
		dataSets.add(males);
		
		NaiveBayesClassifier nbc = new NaiveBayesClassifier(dataSets);
		double[] values = {5.4,132,7.5};  
		System.out.println(nbc.classify(values));		
	}
}
