/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -------------------------------------------------------------
*
* TarsosDSP is developed by Joren Six at IPEM, University Ghent
*  
* -------------------------------------------------------------
*
*  Info: http://0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://0110.be/releases/TarsosDSP/
*  
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
* 
*/

/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.dsp.example;



public class KernelDensityEstimate {
	protected final double[] accumulator;
	protected final Kernel kernel;
	private double sum;
	
	public KernelDensityEstimate(final Kernel kernel, final int size) {
		accumulator = new double[size];
		sum = 0;
		this.kernel = kernel;
		if (kernel.size() > accumulator.length) { 
			throw new IllegalArgumentException("The kernel size should be smaller than the acummulator size.");
		}
	}
	
	public KernelDensityEstimate(final Kernel kernel, double[] accumulator) {
		this.accumulator = accumulator;
		this.kernel = kernel;
		if (kernel.size() > accumulator.length) { 
			throw new IllegalArgumentException("The kernel size should be smaller than the acummulator size.");
		}
		calculateSumFreq();
	}

	/**
	 * Add the kernel to an accumulator for each value.
	 * 
	 * When a kernel with a width of 7 is added at 1 cents it has influence on
	 * the bins from 1200 - 7 * 10 + 1 to 1 + 7 * 10 so from 1131 to 71. To make
	 * the modulo calculation easy 1200 is added to each value: -69 % 1200 is
	 * -69, (-69 + 1200) % 1200 is the expected 1131. If you know what I mean.
	 * This algorithm computes O(width * n) sums with n the number of
	 * annotations and width the number of bins affected, rather efficient.
	 * 
	 * @param value
	 *            The value to add.
	 */
	public void add(double value) {
		int accumulatorSize = accumulator.length;
		int calculationAria = kernel.size() / 2;
		int start = (int) (value + accumulatorSize - calculationAria);
		int stop = (int) (value + accumulatorSize + calculationAria);
		if (kernel.size() % 2 != 0)
			stop++;
		for (int i = start; i < stop; i++) {
			double kernelValue = kernel.value(i - start);
			accumulator[i % accumulatorSize] += kernelValue;
			sum += kernelValue;
		}
	}
	
	/**
	 * Remove a value from the kde, removes a kernel at the specified position.
	 * @param value The value to remove.
	 */
	public void remove(double value) {
		int accumulatorSize = accumulator.length;
		int calculationAria = kernel.size() / 2;
		int start = (int) (value + accumulatorSize - calculationAria);
		int stop = (int) (value + accumulatorSize + calculationAria);
		if (kernel.size() % 2 != 0)
			stop++;

		for (int i = start; i < stop; i++) {
			double kernelValue = kernel.value(i - start);
			accumulator[i % accumulatorSize] -= kernelValue;
			sum -= kernelValue;
		}
	}
	
	/**
	 * Shift the accumulator x positions.
	 * @param shift The number of positions the accumulator should be shifted.
	 */
	public void shift(int shift){
		double[] newValues = new double[size()];
		for(int index = 0 ; index < size() ; index++){
			newValues[index] = accumulator[(index + shift) % size()];
		}
		for(int index = 0 ; index < size() ; index++){
			accumulator[index] = newValues[index];
		}
	}

	/**
	 * Returns the current estimate.
	 * 
	 * @return The current estimate. To prevent unauthorized modification a
	 *         clone of the array is returned. Please cache appropriately.
	 */
	public double[] getEstimate() {
		return accumulator;
	}
	
	/**
	 * Map the kernel density estimate to another size. E.g. a KDE with 4 values
	 * mapped to two is done by iterating the 4 elements and adding them on
	 * modulo 2 places. Here 1 + 4 = 5, 2 + 9 = 11
	 * 
	 * <pre>
	 * (1 2 4 9).map(2) = (5 11) 
	 * </pre>
	 * @param size The new size for the KDE.
	 * @return A new KDE with the contents of the original mapped to the new size.
	 */
	public KernelDensityEstimate map(int size){
		KernelDensityEstimate newKDE = new KernelDensityEstimate(kernel,size);
		for(int index = 0 ; index < size() ; index++){
			newKDE.accumulator[index % size] += accumulator[index];
		}
		newKDE.calculateSumFreq();
		return newKDE;
	}

	/**
	 * Return the value for the accumulator at a certain index.
	 * 
	 * @param index
	 *            The index.
	 * @return The value for the accumulator at a certain index.
	 */
	public double getValue(final int index) {
		return accumulator[index];
	}

	/**
	 * @return The size of the accumulator.
	 */
	public int size() {
		return accumulator.length;
	}

	/**
	 * Returns the sum of all estimates in the accumulator.
	 * 
	 * @return The total sum of all estimates.
	 */
	public double getSumFreq() {
		return sum;
	}
	
	/**
	 * Calculates the sum of all estimates in the accummulator. Should be called after each update.
	 */
	private void calculateSumFreq(){
		sum = 0;
		for (int i = 0; i < accumulator.length; i++) {
			sum += accumulator[i];
		}
	}

	/**
	 * Sets the maximum value in accumulator to 1.0
	 */
	public void normalize() {
		normalize(1.0);
	}
	
	/**
	 * Sets a new maximum bin value.
	 * @param newMaxvalue The new maximum bin value.
	 */
	public void normalize(double newMaxvalue){
		double maxElement = getMaxElement();
		double scaleFactor = newMaxvalue / getMaxElement();
		if (maxElement > 0) {
			for (int i = 0; i < size(); i++) {
				accumulator[i] = accumulator[i] * scaleFactor;
			}
		}
		calculateSumFreq();
	}
	
	/**
	 * @return the maximum element in the accumulator;
	 */
	public double getMaxElement() {
		double maxElement = 0.0;
		for (int i = 0; i < size(); i++) {
			maxElement = Math.max(accumulator[i], maxElement);
		}
		return maxElement;
	}
	
	/**
	 * Sets the area under the curve to 1.0. 
	 * In essence every value is divided by getSumFreq(). 
	 * As per definition of a probability density function.
	 */
	public void pdfify() {
		double sumFreq = this.getSumFreq();
		if(sumFreq != 0.0){
			for (int i = 0; i < accumulator.length; i++) {
				accumulator[i] = accumulator[i]/sumFreq;
			}
		}
		//reset sum freq
		calculateSumFreq();
		assert getSumFreq() == 1.0;
	}
	
	/**
	 * Clears the data in the accumulator.
	 */
	public void clear(){
		for (int i = 0; i < accumulator.length; i++) {
			accumulator[i] = 0;
		}
		//reset sum freq
		calculateSumFreq();
		assert getSumFreq() == 0.0;
	}
		
	/**
	 * Takes the maximum of the value in the accumulator for two kde's.
	 * @param other The other kde of the same size.
	 */
	public void max(KernelDensityEstimate other){
		assert other.size() == size() : "The kde size should be the same!";
		for (int i = 0; i < accumulator.length; i++) {
			accumulator[i] = Math.max(accumulator[i], other.accumulator[i]);
		}
		calculateSumFreq();
	}
	
	/**
	 * Adds a KDE to this accumulator
	 * @param other The other KDE of the same size.
	 */
	public void add(KernelDensityEstimate other){
		assert other.size() == size() : "The kde size should be the same!";
		for (int i = 0; i < accumulator.length; i++) {
			accumulator[i] += other.accumulator[i];
		}
		calculateSumFreq();
	}

	/**
	 * <p>
	 * Calculate a correlation with another KernelDensityEstimate. The index of
	 * the other estimates are shifted by a number which can be zero (or
	 * positive or negative). Beware: the index wraps around the edges.
	 * </p>
	 * <p>
	 * This and the other KernelDensityEstimate should have the same size.
	 * </p>
	 * 
	 * @param other
	 *            The other estimate.
	 * @param positionsToShiftOther
	 *            The number of positions to shift the estimate.
	 * @return A value between 0 and 1 representing how similar both estimates
	 *         are. 1 means total correlation, 0 no correlation.
	 */
	public double correlation(final KernelDensityEstimate other,
			final int positionsToShiftOther) {
		assert other.size() == size() : "The kde size should be the same!";
		double correlation;
		double matchingArea = 0.0;
		double biggestKDEArea = Math.max(getSumFreq(), other.getSumFreq());
		//an if, else to prevent modulo calculation
		if(positionsToShiftOther == 0){
			for (int i = 0; i < accumulator.length; i++) {
				matchingArea += Math.min(accumulator[i],other.accumulator[i]);
			}
		}else{
			for (int i = 0; i < accumulator.length; i++) {
				int otherIndex = (i + positionsToShiftOther) % other.size();
				matchingArea += Math.min(accumulator[i],other.accumulator[otherIndex]);
			}
		}
		if (matchingArea == 0.0) {
			correlation = 0.0;
		} else {
			correlation = matchingArea / biggestKDEArea;
		}
		return correlation;
	}

	/**
	 * Calculates how much the other KernelDensityEstimate needs to be shifted
	 * for optimal correlation.
	 * 
	 * @param other
	 *            The other KernelDensityEstimate.
	 * @return A number between 0 (inclusive) and the size of the
	 *         KernelDensityEstimate (exclusive) which represents how much the
	 *         other KernelDensityEstimate needs to be shifted for optimal
	 *         correlation.
	 */
	public int shiftForOptimalCorrelation(final KernelDensityEstimate other) {
		int optimalShift = 0; // displacement with best correlation
		double maximumCorrelation = -1; // best found correlation

		for (int shift = 0; shift < size(); shift++) {
			final double currentCorrelation = correlation(other, shift);
			if (maximumCorrelation < currentCorrelation) {
				maximumCorrelation = currentCorrelation;
				optimalShift = shift;
			}
		}
		return optimalShift;
	}
	
	
	/**
	 * Calculates the optimal correlation between two Kernel Density Estimates
	 * by shifting and searching for optimal correlation.
	 * 
	 * @param other
	 *            The other KernelDensityEstimate.
	 * @return A value between 0 and 1 representing how similar both estimates
	 *         are. 1 means total correlation, 0 no correlation.
	 */
	public double optimalCorrelation(final KernelDensityEstimate other) {
		int shift = shiftForOptimalCorrelation(other);
		return correlation(other, shift);
	}
	


	/**
	 * Defines a kernel. It has a size and cached values for each index.
	 * 
	 * @author Joren Six
	 */
	public static interface Kernel {
		/**
		 * Fetch the value for the kernel at a certain index.
		 * 
		 * @param kernelIndex
		 *            The index of the previously computed value.
		 * @return The cached value for a certain index.
		 */
		double value(final int kernelIndex);

		/**
		 * The size of the kernel.
		 * 
		 * @return The size of the kernel.
		 */
		int size();
	}	

	/**
	 * A Gaussian kernel function.
	 * 
	 * @author Joren Six
	 */
	public static class GaussianKernel implements Kernel {

		private final double kernel[];

		/**
		 * Construct a kernel with a defined width.
		 * 
		 * @param kernelWidth
		 *            The width of the kernel.
		 */
		public GaussianKernel(final double kernelWidth) {
			double calculationAria = 5 * kernelWidth;// Aria, not area
			double halfWidth = kernelWidth / 2.0;

			// Compute a kernel: a lookup table with e.g. a Gaussian curve
			kernel = new double[(int) calculationAria * 2 + 1];
			double difference = -calculationAria;
			for (int i = 0; i < kernel.length; i++) {
				double power = Math.pow(difference / halfWidth, 2.0);
				kernel[i] = Math.pow(Math.E, -0.5 * power);
				difference++;
			}
		}

		
		public double value(int index) {
			return kernel[index];
		}

		
		public int size() {
			return kernel.length;
		}
	}

	/**
	 * A rectangular kernel function.
	 */
	public static class RectangularKernel implements Kernel {

		private final double kernel[];

		public RectangularKernel(final double kernelWidth) {
			// Compute a kernel: a lookup table with a width
			kernel = new double[(int) kernelWidth];
			for (int i = 0; i < kernel.length; i++) {
				kernel[i] = 1.0;
			}
		}

		
		public double value(int index) {
			return kernel[index];
		}

		
		public int size() {
			return kernel.length;
		}
	}
	
	
	/**
	 * Calculates the optimal correlation between two Kernel Density Estimates
	 * by shifting and searching for optimal correlation.
	 * @param correlationMeasure 
	 * 
	 * @param other
	 *            The other KernelDensityEstimate.
	 * @return A value between 0 and 1 representing how similar both estimates
	 *         are. 1 means total correlation, 0 no correlation.
	 */
	public double optimalCorrelation(final KDECorrelation correlationMeasure, final KernelDensityEstimate other) {
		int shift = shiftForOptimalCorrelation(correlationMeasure,other);
		return correlationMeasure.correlation(this,other, shift);
	}
	
	/**
	 * Calculates how much the other KernelDensityEstimate needs to be shifted
	 * for optimal correlation.
	 * @param correlationMeasure 
	 * 
	 * @param other
	 *            The other KernelDensityEstimate.
	 * @return A number between 0 (inclusive) and the size of the
	 *         KernelDensityEstimate (exclusive) which represents how much the
	 *         other KernelDensityEstimate needs to be shifted for optimal
	 *         correlation.
	 */
	public int shiftForOptimalCorrelation(final KDECorrelation correlationMeasure, final KernelDensityEstimate other) {
		int optimalShift = 0; // displacement with best correlation
		double maximumCorrelation = -1; // best found correlation

		for (int shift = 0; shift < size(); shift++) {
			final double currentCorrelation = correlationMeasure.correlation(this,other, shift);
			if (maximumCorrelation < currentCorrelation) {
				maximumCorrelation = currentCorrelation;
				optimalShift = shift;
			}
		}
		return optimalShift;
	}
	
	public static interface KDECorrelation{
		public double correlation(KernelDensityEstimate first,KernelDensityEstimate other, int shift);
	} 
	
	public static class Overlap implements KDECorrelation{
		public double correlation(KernelDensityEstimate first,KernelDensityEstimate other, int shift) {
			double correlation;
			int matchingArea = 0;
			for (int i = 0; i < first.size(); i++) {
				int otherIndex = (other.size() + i + shift) % other.size();
				matchingArea += Math.min(first.getValue(i),other.getValue(otherIndex));
			}
			double biggestKDEArea = Math.max(first.getSumFreq(), other.getSumFreq());
			correlation = matchingArea / biggestKDEArea;
			return correlation;
		}
	}
	
	public static class Cosine implements KDECorrelation{
		public double correlation(KernelDensityEstimate first,KernelDensityEstimate other, int shift) {
			double correlation;
			double innerProduct = 0;
			double firstSquaredSum = 0;
			double otherSquaredSum = 0;
			for (int i = 0; i < first.size(); i++) {
				int otherIndex = (other.size() + i + shift) % other.size();
				double firstValue = first.getValue(i);
				double otherValue = other.getValue(otherIndex);
				innerProduct += firstValue * otherValue;
				firstSquaredSum += firstValue * firstValue;
				otherSquaredSum += otherValue * otherValue;
			}
			correlation = innerProduct / ( Math.pow(firstSquaredSum, 0.5) * Math.pow(otherSquaredSum, 0.5));
			return correlation;
		}
	}
}
