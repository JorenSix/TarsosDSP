package be.tarsos.dsp.wavelet.lift;

/**
* 
* @author Ian Kaplan
*/
class PolynomialInterpolation {
	/** number of polynomial interpolation ponts */
	private final static int numPts = 4;

	/** Table for 4-point interpolation coefficients */
	private float fourPointTable[][];

	/** Table for 2-point interpolation coefficients */
	private float twoPointTable[][];

	/**
	 * <p>
	 * The polynomial interpolation algorithm assumes that the known points are
	 * located at x-coordinates 0, 1,.. N-1. An interpolated point is calculated
	 * at <b><i>x</i></b>, using N coefficients. The polynomial coefficients for
	 * the point <b><i>x</i></b> can be calculated staticly, using the Lagrange
	 * method.
	 * </p>
	 * 
	 * @param x
	 *            the x-coordinate of the interpolated point
	 * @param N
	 *            the number of polynomial points.
	 * @param c
	 *            an array for returning the coefficients
	 */
	private void lagrange(float x, int N, float c[]) {
		float num, denom;

		for (int i = 0; i < N; i++) {
			num = 1;
			denom = 1;
			for (int k = 0; k < N; k++) {
				if (i != k) {
					num = num * (x - k);
					denom = denom * (i - k);
				}
			} // for k
			c[i] = num / denom;
		} // for i
	} // lagrange

	/**
	 * <p>
	 * For a given N-point polynomial interpolation, fill the coefficient table,
	 * for points 0.5 ... (N-0.5).
	 * </p>
	 */
	private void fillTable(int N, float table[][]) {
		float x;
		float n = N;
		int i = 0;

		for (x = 0.5f; x < n; x = x + 1.0f) {
			lagrange(x, N, table[i]);
			i++;
		}
	} // fillTable

	/**
	 * <p>
	 * PolynomialWavelets constructor
	 * </p>
	 * <p>
	 * Build the 4-point and 2-point polynomial coefficient tables.
	 * </p>
	 */
	public PolynomialInterpolation() {

		// Fill in the 4-point polynomial interplation table
		// for the points 0.5, 1.5, 2.5, 3.5
		fourPointTable = new float[numPts][numPts];

		fillTable(numPts, fourPointTable);

		// Fill in the 2-point polynomial interpolation table
		// for 0.5 and 1.5
		twoPointTable = new float[2][2];

		fillTable(2, twoPointTable);
	} // PolynomialWavelets constructor

	/**
	 * Print an N x N table polynomial coefficient table
	 */
	private void printTable(float table[][], int N) {
		System.out.println(N + "-point interpolation table:");
		double x = 0.5;
		for (int i = 0; i < N; i++) {
			System.out.print(x + ": ");
			for (int j = 0; j < N; j++) {
				System.out.print(table[i][j]);
				if (j < N - 1)
					System.out.print(", ");
			}
			System.out.println();
			x = x + 1.0;
		}
	}

	/**
	 * Print the 4-point and 2-point polynomial coefficient tables.
	 */
	public void printTables() {
		printTable(fourPointTable, numPts);
		printTable(twoPointTable, 2);
	} // printTables

	/**
	 * <p>
	 * For the polynomial interpolation point x-coordinate <b><i>x</i></b>,
	 * return the associated polynomial interpolation coefficients.
	 * </p>
	 * 
	 * @param x
	 *            the x-coordinate for the interpolated pont
	 * @param n
	 *            the number of polynomial interpolation points
	 * @param c
	 *            an array to return the polynomial coefficients
	 */
	private void getCoef(float x, int n, float c[]) {
		float table[][] = null;

		int j = (int) x;
		if (j < 0 || j >= n) {
			System.out.println("PolynomialWavelets::getCoef: n = " + n
					+ ", bad x value");
		}

		if (n == numPts) {
			table = fourPointTable;
		} else if (n == 2) {
			table = twoPointTable;
			c[2] = 0.0f;
			c[3] = 0.0f;
		} else {
			System.out.println("PolynomialWavelets::getCoef: bad value for N");
		}

		if (table != null) {
			for (int i = 0; i < n; i++) {
				c[i] = table[j][i];
			}
		}
	} // getCoef

	/**
	 * <p>
	 * Given four points at the x,y coordinates {0,d<sub>0</sub>},
	 * {1,d<sub>1</sub>}, {2,d<sub>2</sub>}, {3,d<sub>3</sub>} return the
	 * y-coordinate value for the polynomial interpolated point at
	 * <b><i>x</i></b>.
	 * </p>
	 * 
	 * @param x
	 *            the x-coordinate for the point to be interpolated
	 * @param N
	 *            the number of interpolation points
	 * @param d
	 *            an array containing the y-coordinate values for the known
	 *            points (which are located at x-coordinates 0..N-1).
	 * @return the y-coordinate value for the polynomial interpolated point at
	 *         <b><i>x</i></b>.
	 */
	public float interpPoint(float x, int N, float d[]) {
		float c[] = new float[numPts];
		float point = 0;

		int n = numPts;
		if (N < numPts)
			n = N;

		getCoef(x, n, c);

		if (n == numPts) {
			point = c[0] * d[0] + c[1] * d[1] + c[2] * d[2] + c[3] * d[3];
		} else if (n == 2) {
			point = c[0] * d[0] + c[1] * d[1];
		}

		return point;
	} // interpPoint

} // PolynomialInterpolation
