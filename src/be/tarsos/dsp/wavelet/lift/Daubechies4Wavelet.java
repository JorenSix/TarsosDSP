package be.tarsos.dsp.wavelet.lift;

/**
* 
* @author Ian Kaplan
*/
public class Daubechies4Wavelet extends LiftingSchemeBaseWavelet {
	final static float sqrt3 = (float) Math.sqrt(3);
	final static float sqrt2 = (float) Math.sqrt(2);

	protected void normalize(float[] S, int N, int direction) {
		int half = N >> 1;

		for (int n = 0; n < half; n++) {
			if (direction == forward) {
				S[n] = ((sqrt3 - 1.0f) / sqrt2) * S[n];
				S[n + half] = ((sqrt3 + 1.0f) / sqrt2) * S[n + half];
			} else if (direction == inverse) {
				S[n] = ((sqrt3 + 1.0f) / sqrt2) * S[n];
				S[n + half] = ((sqrt3 - 1.0f) / sqrt2) * S[n + half];
			} else {
				System.out
						.println("Daubechies4Wavelet::normalize: bad direction value");
				break;
			}
		}
	} // normalize

	protected void predict(float[] S, int N, int direction) {
		int half = N >> 1;

		if (direction == forward) {
			S[half] = S[half] - (sqrt3 / 4.0f) * S[0]
					- (((sqrt3 - 2) / 4.0f) * S[half - 1]);
		} else if (direction == inverse) {
			S[half] = S[half] + (sqrt3 / 4.0f) * S[0]
					+ (((sqrt3 - 2) / 4.0f) * S[half - 1]);
		} else {
			System.out
					.println("Daubechies4Wavelet::predict: bad direction value");
		}

		// predict, forward

		for (int n = 1; n < half; n++) {
			if (direction == forward) {
				S[half + n] = S[half + n] - (sqrt3 / 4.0f) * S[n]
						- (((sqrt3 - 2) / 4.0f) * S[n - 1]);
			} else if (direction == inverse) {
				S[half + n] = S[half + n] + (sqrt3 / 4.0f) * S[n]
						+ (((sqrt3 - 2) / 4.0f) * S[n - 1]);
			} else {
				break;
			}
		}

	} // predict

	protected void updateOne(float[] S, int N, int direction) {
		int half = N >> 1;

		for (int n = 0; n < half; n++) {
			float updateVal = sqrt3 * S[half + n];

			if (direction == forward) {
				S[n] = S[n] + updateVal;
			} else if (direction == inverse) {
				S[n] = S[n] - updateVal;
			} else {
				System.out
						.println("Daubechies4Wavelet::updateOne: bad direction value");
				break;
			}
		}
	} // updateOne

	protected void update(float[] S, int N, int direction) {
		int half = N >> 1;

		for (int n = 0; n < half - 1; n++) {
			if (direction == forward) {
				S[n] = S[n] - S[half + n + 1];
			} else if (direction == inverse) {
				S[n] = S[n] + S[half + n + 1];
			} else {
				System.out
						.println("Daubechies4Wavelet::update: bad direction value");
				break;
			}
		}

		if (direction == forward) {
			S[half - 1] = S[half - 1] - S[half];
		} else if (direction == inverse) {
			S[half - 1] = S[half - 1] + S[half];
		}
	} // update

	public void forwardTrans(float[] vec) {
		final int N = vec.length;

		for (int n = N; n > 1; n = n >> 1) {
			split(vec, n);
			updateOne(vec, n, forward); // update 1
			predict(vec, n, forward);
			update(vec, n, forward); // update 2
			normalize(vec, n, forward);
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
	 */
	public void inverseTrans(float[] vec) {
		final int N = vec.length;

		for (int n = 2; n <= N; n = n << 1) {
			normalize(vec, n, inverse);
			update(vec, n, inverse);
			predict(vec, n, inverse);
			updateOne(vec, n, inverse);
			merge(vec, n);
		}
	} // inverseTrans
}
