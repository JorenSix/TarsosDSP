package be.hogent.tarsos.dsp.speechmusicdiscriminator;

import java.util.ArrayList;
import java.util.List;

public class TrainingDataSet implements DataSet {
	
	private final String label;
	private final int numberOfFeatures;
	private final String[] descriptions;
	private final List<List<Double>> dataPointList;
	
	public TrainingDataSet(String label,int numberOfFeatures){
		this.label = label;
		this.numberOfFeatures = numberOfFeatures;
		descriptions = new String[numberOfFeatures];
		dataPointList = new ArrayList<List<Double>>();
		for(int i = 0 ; i < numberOfFeatures ; i++){
			dataPointList.add(new ArrayList<Double>());
		}
	}
	
	public void setDataPoints(int index,String description, List<Double> dataPoints){
		dataPointList.set(index, dataPoints);
		descriptions[index]=description;
	}
	
	public void addDataPoint(int index, double dataPoint){
		dataPointList.get(index).add(dataPoint);
	}

	@Override
	public String label() {
		return label;
	}

	@Override
	public double[] means() {
		double[] means = new double[numberOfFeatures];
		for(int i = 0 ; i < numberOfFeatures ; i++){
			means[i] = mean(dataPointList.get(i));
		}
		return means;
	}

	@Override
	public double[] variances() {
		double[] variances = new double[numberOfFeatures];
		for(int i = 0 ; i < numberOfFeatures ; i++){
			variances[i] = varianceEstimatorForNormalDistribution(dataPointList.get(i));
		}
		return variances;
	}

	@Override
	public String[] descriptions() {
		return descriptions;
	}
	
	
	public static double mean(List<Double> dataset){
		double sum = 0;
		for(Double dataPoint:dataset){
			sum += dataPoint;
		}
		if(dataset.size()==0){
			System.out.println("hmm");
		}
		return sum / (double) dataset.size();
	}
	
	/**
	 * An estimator for the variance of a normal distribution.
	 * See here:http://magician.ucsd.edu/essentials/WebBookse68.html
	 * @param dataset
	 * @return an estimation of the variance.
	 */
	public static double varianceEstimatorForNormalDistribution(List<Double> dataset){
		double mean = mean(dataset);
		double sum = 0;
		for(Double dataPoint:dataset){
			double term = dataPoint - mean;
			sum += (term * term);
		}
		return  sum / (double) (dataset.size()-1.0);
	}
	
	/**
	 * Third central moment
	 * @param dataset
	 * @return the skewness or third cental moment
	 */
	public static double skewness(List<Double> dataset){
		double mean = mean(dataset);
		double sum = 0;
		double squaredSum = 0;
		for(Double dataPoint:dataset){
			double term = dataPoint - mean;
			squaredSum += (term * term);
			sum += (term * term * term);
		}
		return  (sum / (float) dataset.size()) / (Math.pow(squaredSum/ (float) dataset.size(), 1.5));
	}

	public void setDescription(int index, String description) {
		descriptions[index] = description;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(label).append("\n");
		double[] variances = variances();
		double[] means = means();
		for(int i = 0 ; i < numberOfFeatures ; i++){
			sb.append("\t").append(descriptions[i]);
			sb.append("\n");
			
			sb.append("\t\t").append("Mean: ");
			sb.append(String.format("%.6f", means[i]));
			sb.append("\n");
			
			//sb.append("\t\t").append("Number of points: ");
			//sb.append(dataPointList.get(i).size());
			//sb.append("\n");
			
			sb.append("\t\t").append("Estimated variance: ");
			sb.append(String.format("%.6f", variances[i]));
			sb.append("\n");
			
			//sb.append("\t\t").append("Estimated standard deviation: ");
			//sb.append(String.format("%.6f", Math.sqrt(variances[i])));
			//sb.append("\n");
		}
		return sb.toString();
	}
}
