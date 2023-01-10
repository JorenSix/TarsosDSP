package be.tarsos.dsp.wavelet.lift;

/**
 * <p>
 * Line (with slope) wavelet
 * </p>
 *
 * <p>
 * The wavelet Lifting Scheme "LineWavelet" wavelet approximates the data set
 * using a LineWavelet with with slope (in contrast to the HaarWavelet wavelet
 * where a LineWavelet has zero slope is used to approximate the data).
 * </p>
 *
 * <p>
 * The predict stage of the LineWavelet wavelet "predicts" that an odd point
 * will lie midway between its two neighboring even points. That is, that the
 * odd point will lie on a LineWavelet between the two adjacent even points. The
 * difference between this "prediction" and the actual odd value replaces the
 * odd element.
 * </p>
 *
 * <p>
 * The update stage calculates the average of the odd and even element pairs,
 * although the method is indirect, since the predict phase has over written the
 * odd value.
 * </p>
 *
 * <b>
 * Copyright and Use</b>
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
public class LineWavelet extends LiftingSchemeBaseWavelet {

	/**
	 * <p>
	 * Calculate an extra "even" value for the LineWavelet wavelet algorithm at
	 * the end of the data series. Here we pretend that the last two values in
	 * the data series are at the x-axis coordinates 0 and 1, respectively. We
	 * then need to calculate the y-axis value at the x-axis coordinate 2. This
	 * point lies on a LineWavelet running through the points at 0 and 1.
	 * </p>
	 * <p>
	 * Given two points, x 1 , y 1  and x 2 ,
	 * y 2 , where
	 * </p>
	 *
	 * <pre>
	 *         x 1  = 0
	 *         x 2  = 1
	 * </pre>
	 * <p>
	 * calculate the point on the LineWavelet at x 3 , y 3 ,
	 * where
	 * </p>
	 *
	 * <pre>
	 *         x 3  = 2
	 * </pre>
	 * <p>
	 * The "two-point equation" for a LineWavelet given x 1 ,
	 * y 1  and x 2 , y 2  is
	 * </p>
	 *
	 * <pre>
	 *      .          y 2  - y 1 
	 *      (y - y 1 ) = -------- (x - x 1 )
	 *      .          x 2  - x 1 
	 * </pre>
	 * <p>
	 * Solving for y
	 * </p>
	 *
	 * <pre>
	 *      .    y 2  - y 1 
	 *      y = -------- (x - x 1 ) + y 1 
	 *      .    x 2  - x 1 
	 * </pre>
	 * <p>
	 * Since x 1  = 0 and x 2  = 1
	 * </p>
	 *
	 * <pre>
	 *      .    y 2  - y 1 
	 *      y = -------- (x - 0) + y 1 
	 *      .    1 - 0
	 * </pre>
	 * <p>
	 * or
	 * </p>
	 *
	 * <pre>
	 *      y = (y 2  - y 1 )*x + y 1 
	 * </pre>
	 * <p>
	 * We're calculating the value at x 3  = 2, so
	 * </p>
	 *
	 * <pre>
	 *      y = 2*y 2  - 2*y 1  + y 1 
	 * </pre>
	 * <p>
	 * or
	 * </p>
	 *
	 * <pre>
	 *      y = 2*y 2  - y 1 
	 * </pre>
	 */
	private float new_y(float y1, float y2) {
		float y = 2 * y2 - y1;
		return y;
	}

	/**
	 * <p>
	 * Predict phase of LineWavelet Lifting Scheme wavelet
	 * </p>
	 *
	 * <p>
	 * The predict step attempts to "predict" the value of an odd element from
	 * the even elements. The difference between the prediction and the actual
	 * element is stored as a wavelet coefficient.
	 * </p>
	 * <p>
	 * The "predict" step takes place after the split step. The split step will
	 * move the odd elements (b j ) to the second half of the array,
	 * leaving the even elements (a i ) in the first half
	 * </p>
	 *
	 * <pre>
	 *     a 0 , a 1 , a 1 , a 3 , b 0 , b 1 , b 2 , b 2 ,
	 * </pre>
	 * <p>
	 * The predict step of the LineWavelet wavelet "predicts" that the odd
	 * element will be on a LineWavelet between two even elements.
	 * </p>
	 *
	 * <pre>
	 *     b j+1,i  = b j,i  - (a j,i  + a j,i+1 )/2
	 * </pre>
	 * <p>
	 * Note that when we get to the end of the data series the odd element is
	 * the last element in the data series (remember, wavelet algorithms work on
	 * data series with 2<sup>n</sup> elements). Here we "predict" that the odd
	 * element will be on a LineWavelet that runs through the last two even
	 * elements. This can be calculated by assuming that the last two even
	 * elements are located at x-axis coordinates 0 and 1, respectively. The odd
	 * element will be at 2. The <i>new_y()</i> function is called to do this
	 * simple calculation.
	 * </p>
	 */
	protected void predict(float[] vec, int N, int direction) {
		int half = N >> 1;
		float predictVal;

		for (int i = 0; i < half; i++) {
			int j = i + half;
			if (i < half - 1) {
				predictVal = (vec[i] + vec[i + 1]) / 2;
			} else if (N == 2) {
				predictVal = vec[0];
			} else {
				// calculate the last "odd" prediction
				predictVal = new_y(vec[i - 1], vec[i]);
			}

			if (direction == forward) {
				vec[j] = vec[j] - predictVal;
			} else if (direction == inverse) {
				vec[j] = vec[j] + predictVal;
			} else {
				System.out.println("predictline::predict: bad direction value");
			}
		}
	} // predict

	/**
	 * <p>
	 * The predict phase works on the odd elements in the second half of the
	 * array. The update phase works on the even elements in the first half of
	 * the array. The update phase attempts to preserve the average. After the
	 * update phase is completed the average of the even elements should be
	 * approximately the same as the average of the input data set from the
	 * previous iteration. The result of the update phase becomes the input for
	 * the next iteration.
	 * </p>
	 * <p>
	 * In a HaarWavelet wavelet the average that replaces the even element is
	 * calculated as the average of the even element and its associated odd
	 * element (e.g., its odd neighbor before the split). This is not possible
	 * in the LineWavelet wavelet since the odd element has been replaced by the
	 * difference between the odd element and the mid-point of its two even
	 * neighbors. As a result, the odd element cannot be recovered.
	 * </p>
	 * <p>
	 * The value that is added to the even element to preserve the average is
	 * calculated by the equation shown below. This equation is given in Wim
	 * Sweldens' journal articles and his tutorial (<i>Building Your Own
	 * Wavelets at Home</i>) and in <i>Ripples in Mathematics</i>. A somewhat
	 * more complete derivation of this equation is provided in <i>Ripples in
	 * Mathematics</i> by A. Jensen and A. la Cour-Harbo, Springer, 2001.
	 * </p>
	 * <p>
	 * The equation used to calculate the average is shown below for a given
	 * iteratin <i>i</i>. Note that the predict phase has already completed, so
	 * the odd values belong to iteration <i>i+1</i>.
	 * </p>
	 *
	 * <pre>
	 *   even i+1,j  = even i,j  op (odd i+1,k-1  + odd i+1,k )/4
	 * </pre>
	 * <p>
	 * There is an edge problem here, when i = 0 and k = N/2 (e.g., there is no
	 * k-1 element). We assume that the odd i+1,k-1  is the same as
	 * odd k . So for the first element this becomes
	 *
	 * <pre>
	 *       (2 * odd k )/4
	 * </pre>
	 * <p>
	 * or
	 * </p>
	 *
	 * <pre>
	 *       odd k /2
	 * </pre>
	 */
	protected void update(float[] vec, int N, int direction) {
		int half = N >> 1;

		for (int i = 0; i < half; i++) {
			int j = i + half;
			float val;

			if (i == 0) {
				val = vec[j] / 2.0f;
			} else {
				val = (vec[j - 1] + vec[j]) / 4.0f;
			}
			if (direction == forward) {
				vec[i] = vec[i] + val;
			} else if (direction == inverse) {
				vec[i] = vec[i] - val;
			} else {
				System.out.println("update: bad direction value");
			}
		} // for
	}

} // LineWavelet
