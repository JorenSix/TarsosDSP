package be.tarsos.dsp.wavelet.lift;

/**
 * <p>
 * HaarWavelet (flat LineWavelet) wavelet.
 * </p>
 * 
 * <p>
 * As with all Lifting scheme wavelet transform functions, the first stage of a
 * transform step is the split stage. The split step moves the even element to
 * the first half of an N element region and the odd elements to the second half
 * of the N element region.
 * </p>
 * 
 * <p>
 * The Lifting Scheme version of the HaarWavelet transform uses a wavelet
 * function (predict stage) that "predicts" that an odd element will have the
 * same value as it preceeding even element. Stated another way, the odd element
 * is "predicted" to be on a flat (zero slope LineWavelet) shared with the even
 * point. The difference between this "prediction" and the actual odd value
 * replaces the odd element.
 * </p>
 * 
 * <p>
 * The wavelet scaling function (a.k.a. smoothing function) used in the update
 * stage calculates the average between an even and an odd element.
 * </p>
 * 
 * <p>
 * The merge stage at the end of the inverse transform interleaves odd and even
 * elements from the two halves of the array (e.g., ordering them
 * even<sub>0</sub>, odd<sub>0</sub>, even<sub>1</sub>, odd<sub>1</sub>, ...)
 * </p>
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
public class HaarWavelet extends LiftingSchemeBaseWavelet {

	/**
	 * HaarWavelet predict step
	 */
	protected void predict(float[] vec, int N, int direction) {
		int half = N >> 1;

		for (int i = 0; i < half; i++) {
			float predictVal = vec[i];
			int j = i + half;

			if (direction == forward) {
				vec[j] = vec[j] - predictVal;
			} else if (direction == inverse) {
				vec[j] = vec[j] + predictVal;
			} else {
				System.out.println("HaarWavelet::predict: bad direction value");
			}
		}
	}

	public void forwardTransOne(float[] vec) {
		final int N = vec.length;

		split(vec, N);
		predict(vec, N, forward);
		update(vec, N, forward);

	} // forwardTrans

	/**
	 * <p>
	 * Update step of the HaarWavelet wavelet transform.
	 * </p>
	 * <p>
	 * The wavelet transform calculates a set of detail or difference
	 * coefficients in the predict step. These are stored in the upper half of
	 * the array. The update step calculates an average from the even-odd
	 * element pairs. The averages will replace the even elements in the lower
	 * half of the array.
	 * </p>
	 * <p>
	 * The HaarWavelet wavelet calculation used in the Lifting Scheme is
	 * </p>
	 * 
	 * <pre>
	 *        d<sub>j+1, i</sub> = odd<sub>j+1, i</sub> = odd<sub>j, i</sub> - even<sub>j, i</sub>
	 *        a<sub>j+1, i</sub> = even<sub>j, i</sub> = (even<sub>j, i</sub> + odd<sub>j, i</sub>)/2
	 * </pre>
	 * <p>
	 * Note that the Lifting Scheme uses an in-place algorithm. The odd elements
	 * have been replaced by the detail coefficients in the predict step. With a
	 * little algebra we can substitute the coefficient calculation into the
	 * average calculation, which gives us
	 * </p>
	 * 
	 * <pre>
	 *        a<sub>j+1, i</sub> = even<sub>j, i</sub> = even<sub>j, i</sub> + (odd<sub>j, i</sub>/2)
	 * </pre>
	 */
	protected void update(float[] vec, int N, int direction) {
		int half = N >> 1;

		for (int i = 0; i < half; i++) {
			int j = i + half;
			float updateVal = vec[j] / 2.0f;

			if (direction == forward) {
				vec[i] = vec[i] + updateVal;
			} else if (direction == inverse) {
				vec[i] = vec[i] - updateVal;
			} else {
				System.out.println("update: bad direction value");
			}
		}
	}

} // HaarWavelet
