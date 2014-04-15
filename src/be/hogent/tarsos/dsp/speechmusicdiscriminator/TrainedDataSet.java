package be.hogent.tarsos.dsp.speechmusicdiscriminator;


public class TrainedDataSet implements DataSet {
	
	private final String label;
	private final double[] means;
	private final double[] variances;
	private final String[] descriptions;
	
	public TrainedDataSet(String label,int numberOfFeatures){
		this.label = label;
		means = new double[numberOfFeatures];
		variances = new double[numberOfFeatures];
		descriptions = new String[numberOfFeatures];	
	}
	
	public void setFeature(int index,String description,double mean, double variance){
		descriptions[index]=description;
		means[index]=mean;
		variances[index]=variance;
	}

	@Override
	public String label() {
		return label;
	}

	@Override
	public double[] means() {
		return means;
	}

	@Override
	public double[] variances() {
		return variances;
	}

	@Override
	public String[] descriptions() {
		return descriptions;
	}
}
