package be.hogent.tarsos.dsp.speechmusicdiscriminator;


/**
 * The interface of a dataset to classify.
 * @author Joren Six
 *
 */
public interface DataSet {
	public String label();
	
	public double[] means();
	
	public double[] variances();
	
	public String[] descriptions();
	
}
