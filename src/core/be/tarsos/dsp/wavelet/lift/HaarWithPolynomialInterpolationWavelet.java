package be.tarsos.dsp.wavelet.lift;

/**
 * <p>
 * HaarWavelet transform extended with a polynomial interpolation step
 * </p>
 * <p>
 * This wavelet transform extends the HaarWavelet transform with a polynomial
 * wavelet function.
 * </p>
 * <p>
 * The polynomial wavelet uses 4-point polynomial interpolation to "predict" an
 * odd point from four even point values.
 * </p>
 * <p>
 * This class extends the HaarWavelet transform with an interpolation stage
 * which follows the predict and update stages of the HaarWavelet transform. The
 * predict value is calculated from the even points, which in this case are the
 * smoothed values calculated by the scaling function (e.g., the averages of the
 * even and odd values).
 * </p>
 * 
 * <p>
 * The predict value is subtracted from the current odd value, which is the
 * result of the HaarWavelet wavelet function (e.g., the difference between the
 * odd value and the even value). This tends to result in large odd values after
 * the interpolation stage, which is a weakness in this algorithm.
 * </p>
 * 
 * <p>
 * This algorithm was suggested by Wim Sweldens' tutorial <i>Building Your Own
 * Wavelets at Home</i>.
 * </p>
 * 
 * </p>
 * 
 * <pre>
 *   <a href="http://www.bearcave.com/misl/misl_tech/wavelets/lifting/index.html">
 *   http://www.bearcave.com/misl/misl_tech/wavelets/lifting/index.html</a>
 * </pre>
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
public class HaarWithPolynomialInterpolationWavelet extends HaarWavelet {
	final static int numPts = 4;
	private PolynomialInterpolation fourPt;

	/**
	 * HaarWithPolynomialInterpolationWavelet class constructor
	 */
	public HaarWithPolynomialInterpolationWavelet() {
		fourPt = new PolynomialInterpolation();
	}

	/**
	 * <p>
	 * Copy four points or <i>N</i> (which ever is less) data points from
	 * <i>vec</i> into <i>d</i> These points are the "known" points used in the
	 * polynomial interpolation.
	 * </p>
	 * 
	 * @param vec
	 *            the input data set on which the wavelet is calculated
	 * @param d
	 *            an array into which <i>N</i> data points, starting at
	 *            <i>start</i> are copied.
	 * @param N
	 *            the number of polynomial interpolation points
	 * @param start
	 *            the index in <i>vec</i> from which copying starts
	 */
	private void fill(float vec[], float d[], int N, int start) {
		int n = numPts;
		if (n > N)
			n = N;
		int end = start + n;
		int j = 0;

		for (int i = start; i < end; i++) {
			d[j] = vec[i];
			j++;
		}
	} // fill

	/**
	 * <p>
	 * Predict an odd point from the even points, using 4-point polynomial
	 * interpolation.
	 * </p>
	 * <p>
	 * The four points used in the polynomial interpolation are the even points.
	 * We pretend that these four points are located at the x-coordinates
	 * 0,1,2,3. The first odd point interpolated will be located between the
	 * first and second even point, at 0.5. The next N-3 points are located at
	 * 1.5 (in the middle of the four points). The last two points are located
	 * at 2.5 and 3.5. For complete documentation see
	 * </p>
	 * 
	 * <pre>
	 *   <a href="http://www.bearcave.com/misl/misl_tech/wavelets/lifting/index.html">
	 *   http://www.bearcave.com/misl/misl_tech/wavelets/lifting/index.html</a>
	 * </pre>
	 * 
	 * <p>
	 * The difference between the predicted (interpolated) value and the actual
	 * odd value replaces the odd value in the forward transform.
	 * </p>
	 * 
	 * <p>
	 * As the recursive steps proceed, N will eventually be 4 and then 2. When N
	 * = 4, linear interpolation is used. When N = 2, HaarWavelet interpolation
	 * is used (the prediction for the odd value is that it is equal to the even
	 * value).
	 * </p>
	 * 
	 * @param vec
	 *            the input data on which the forward or inverse transform is
	 *            calculated.
	 * @param N
	 *            the area of vec over which the transform is calculated
	 * @param direction
	 *            forward or inverse transform
	 */
	protected void interp(float[] vec, int N, int direction) {
		int half = N >> 1;
		float d[] = new float[numPts];

		// int k = 42;

		for (int i = 0; i < half; i++) {
			float predictVal;

			if (i == 0) {
				if (half == 1) {
					// e.g., N == 2, and we use HaarWavelet interpolation
					predictVal = vec[0];
				} else {
					fill(vec, d, N, 0);
					predictVal = fourPt.interpPoint(0.5f, half, d);
				}
			} else if (i == 1) {
				predictVal = fourPt.interpPoint(1.5f, half, d);
			} else if (i == half - 2) {
				predictVal = fourPt.interpPoint(2.5f, half, d);
			} else if (i == half - 1) {
				predictVal = fourPt.interpPoint(3.5f, half, d);
			} else {
				fill(vec, d, N, i - 1);
				predictVal = fourPt.interpPoint(1.5f, half, d);
			}

			int j = i + half;
			if (direction == forward) {
				vec[j] = vec[j] - predictVal;
			} else if (direction == inverse) {
				vec[j] = vec[j] + predictVal;
			} else {
				System.out
						.println("PolynomialWavelets::predict: bad direction value");
			}
		}
	} // interp

	/**
	 * <p>
	 * HaarWavelet transform extened with polynomial interpolation forward
	 * transform.
	 * </p>
	 * <p>
	 * This version of the forwardTrans function overrides the function in the
	 * LiftingSchemeBaseWavelet base class. This function introduces an extra
	 * polynomial interpolation stage at the end of the transform.
	 * </p>
	 */
	public void forwardTrans(float[] vec) {
		final int N = vec.length;

		for (int n = N; n > 1; n = n >> 1) {
			split(vec, n);
			predict(vec, n, forward);
			update(vec, n, forward);
			interp(vec, n, forward);
		} // for
	} // forwardTrans

	/**
	 * <p>
	 * HaarWavelet transform extened with polynomial interpolation inverse
	 * transform.
	 * </p>
	 * <p>
	 * This version of the inverseTrans function overrides the function in the
	 * LiftingSchemeBaseWavelet base class. This function introduces an inverse
	 * polynomial interpolation stage at the start of the inverse transform.
	 * </p>
	 */
	public void inverseTrans(float[] vec) {
		final int N = vec.length;

		for (int n = 2; n <= N; n = n << 1) {
			interp(vec, n, inverse);
			update(vec, n, inverse);
			predict(vec, n, inverse);
			merge(vec, n);
		}
	} // inverseTrans

} // HaarWithPolynomialInterpolationWavelet

