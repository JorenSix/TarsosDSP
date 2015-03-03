package be.tarsos.dsp.wavelet.lift;

/**
 * <p>
 * class LiftingSchemeBaseWavelet: base class for simple Lifting Scheme wavelets
 * using split, predict, update or update, predict, merge steps.
 * </p>
 * 
 * <p>
 * Simple lifting scheme wavelets consist of three steps, a split/merge step,
 * predict step and an update step:
 * </p>
 * <ul>
 * <li>
 * <p>
 * The split step divides the elements in an array so that the even elements are
 * in the first half and the odd elements are in the second half.
 * </p>
 * </li>
 * <li>
 * <p>
 * The merge step is the inverse of the split step. It takes two regions of an
 * array, an odd region and an even region and merges them into a new region
 * where an even element alternates with an odd element.
 * </p>
 * </li>
 * <li>
 * <p>
 * The predict step calculates the difference between an odd element and its
 * predicted value based on the even elements. The difference between the
 * predicted value and the actual value replaces the odd element.
 * </p>
 * </li>
 * <li>
 * <p>
 * The predict step operates on the odd elements. The update step operates on
 * the even element, replacing them with a difference between the predict value
 * and the actual odd element. The update step replaces each even element with
 * an average. The result of the update step becomes the input to the next
 * recursive step in the wavelet calculation.
 * </p>
 * </li>
 * 
 * </ul>
 * 
 * <p>
 * The split and merge methods are shared by all Lifting Scheme wavelet
 * algorithms. This base class provides the transform and inverse transform
 * methods (forwardTrans and inverseTrans). The predict and update methods are
 * abstract and are defined for a particular Lifting Scheme wavelet sub-class.
 * </p>
 * 
 * <p>
 * <b>References:</b>
 * </p>
 * 
 * <ul>
 * <li>
 * <a href="http://www.bearcave.com/misl/misl_tech/wavelets/lifting/index.html">
 * <i>The Wavelet Lifting Scheme</i></a> by Ian Kaplan, www.bearcave.com. This
 * is the parent web page for this Java source code.</li>
 * <li>
 * <i>Ripples in Mathematics: the Discrete Wavelet Transform</i> by Arne Jense
 * and Anders la Cour-Harbo, Springer, 2001</li>
 * <li>
 * <i>Building Your Own Wavelets at Home</i> in <a
 * href="http://www.multires.caltech.edu/teaching/courses/waveletcourse/">
 * Wavelets in Computer Graphics</a></li>
 * </ul>
 * 
 * <h4>
 * Copyright and Use</h4>
 * 
 * <p>
 * You may use this source code without limitation and without fee as long as
 * you include:
 * </p>
 * <blockquote> This software was written and is copyrighted by Ian Kaplan, Bear
 * Products International, www.bearcave.com, 2001. </blockquote>
 * <p>
 * This software is provided "as is", without any warrenty or claim as to its
 * usefulness. Anyone who uses this source code uses it at their own risk. Nor
 * is any support provided by Ian Kaplan and Bear Products International.
 * <p>
 * Please send any bug fixes or suggested source changes to:
 * 
 * <pre>
 *      iank@bearcave.com
 * </pre>
 * 
 * @author Ian Kaplan
 */
public abstract class LiftingSchemeBaseWavelet {

	/** "enumeration" for forward wavelet transform */
	protected final int forward = 1;
	/** "enumeration" for inverse wavelet transform */
	protected final int inverse = 2;

	/**
	 * Split the <i>vec</i> into even and odd elements, where the even elements
	 * are in the first half of the vector and the odd elements are in the
	 * second half.
	 */
	protected void split(float[] vec, int N) {

		int start = 1;
		int end = N - 1;

		while (start < end) {
			for (int i = start; i < end; i = i + 2) {
				float tmp = vec[i];
				vec[i] = vec[i + 1];
				vec[i + 1] = tmp;
			}
			start = start + 1;
			end = end - 1;
		}
	}

	/**
	 * Merge the odd elements from the second half of the N element region in
	 * the array with the even elements in the first half of the N element
	 * region. The result will be the combination of the odd and even elements
	 * in a region of length N.
	 */
	protected void merge(float[] vec, int N) {
		int half = N >> 1;
		int start = half - 1;
		int end = half;

		while (start > 0) {
			for (int i = start; i < end; i = i + 2) {
				float tmp = vec[i];
				vec[i] = vec[i + 1];
				vec[i + 1] = tmp;
			}
			start = start - 1;
			end = end + 1;
		}
	}

	/**
	 * Predict step, to be defined by the subclass
	 * 
	 * @param vec
	 *            input array
	 * @param N
	 *            size of region to act on (from 0..N-1)
	 * @param direction
	 *            forward or inverse transform
	 */
	protected abstract void predict(float[] vec, int N, int direction);

	/**
	 * Update step, to be defined by the subclass
	 * 
	 * @param vec
	 *            input array
	 * @param N
	 *            size of region to act on (from 0..N-1)
	 * @param direction
	 *            forward or inverse transform
	 */
	protected abstract void update(float[] vec, int N, int direction);

	/**
	 * <p>
	 * Simple wavelet Lifting Scheme forward transform
	 * </p>
	 * 
	 * <p>
	 * forwardTrans is passed an array of doubles. The array size must be a
	 * power of two. Lifting Scheme wavelet transforms are calculated in-place
	 * and the result is returned in the argument array.
	 * </p>
	 * 
	 * <p>
	 * The result of forwardTrans is a set of wavelet coefficients ordered by
	 * increasing frequency and an approximate average of the input data set in
	 * vec[0]. The coefficient bands follow this element in powers of two (e.g.,
	 * 1, 2, 4, 8...).
	 * </p>
	 * 
	 * @param vec
	 *            the vector
	 */
	public void forwardTrans(float[] vec) {
		final int N = vec.length;

		for (int n = N; n > 1; n = n >> 1) {
			split(vec, n);
			predict(vec, n, forward);
			update(vec, n, forward);
		}
	} // forwardTrans

	/**
	 * <p>
	 * Default two step Lifting Scheme inverse wavelet transform
	 * </p>
	 * 
	 * <p>
	 * inverseTrans is passed the result of an ordered wavelet transform,
	 * consisting of an average and a set of wavelet coefficients. The inverse
	 * transform is calculated in-place and the result is returned in the
	 * argument array.
	 * </p>
	 * 
	 * @param vec
	 *            the vector
	 */
	public void inverseTrans(float[] vec) {
		final int N = vec.length;

		for (int n = 2; n <= N; n = n << 1) {
			update(vec, n, inverse);
			predict(vec, n, inverse);
			merge(vec, n);
		}
	} // inverseTrans

} // LiftingSchemeBaseWavelet
