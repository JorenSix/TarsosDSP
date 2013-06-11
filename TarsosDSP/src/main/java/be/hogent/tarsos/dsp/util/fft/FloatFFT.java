/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -----------------------------------------------------------
*
*  TarsosDSP is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://tarsos.0110.be/releases/TarsosDSP/
*  
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
* 
*/

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is JTransforms.
 *
 * The Initial Developer of the Original Code is
 * Piotr Wendykier, Emory University.
 * Portions created by the Initial Developer are Copyright (C) 2007-2009
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package be.hogent.tarsos.dsp.util.fft;

import java.util.concurrent.Future;

import be.hogent.tarsos.dsp.util.ConcurrencyUtils;



/**
 * Computes 1D Discrete Fourier Transform (DFT) of complex and real, single
 * precision data. The size of the data can be an arbitrary number. This is a
 * parallel implementation of split-radix and mixed-radix algorithms optimized
 * for SMP systems. <br>
 * <br>
 * This code is derived from General Purpose FFT Package written by Takuya Ooura
 * (http://www.kurims.kyoto-u.ac.jp/~ooura/fft.html) and from JFFTPack written
 * by Baoshe Zhang (http://jfftpack.sourceforge.net/)
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public strictfp class FloatFFT {

    private static enum Plans {
        SPLIT_RADIX, MIXED_RADIX, BLUESTEIN
    }

    private int n;

    private int nBluestein;

    private int[] ip;

    private float[] w;

    private int nw;

    private int nc;

    private float[] wtable;

    private float[] wtable_r;

    private float[] bk1;

    private float[] bk2;

    private Plans plan;

    private static final int[] factors = { 4, 2, 3, 5 };

    private static final float PI = 3.14159265358979311599796346854418516f;

    private static final float TWO_PI = 6.28318530717958623199592693708837032f;

    /**
     * Creates new instance of FloatFFT.
     * 
     * @param n
     *            size of data
     */
    public FloatFFT(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be greater than 0");
        }
        this.n = n;

        if (!ConcurrencyUtils.isPowerOf2(n)) {
            if (getReminder(n, factors) >= 211) {
                plan = Plans.BLUESTEIN;
                nBluestein = ConcurrencyUtils.nextPow2(n * 2 - 1);
                bk1 = new float[2 * nBluestein];
                bk2 = new float[2 * nBluestein];
                this.ip = new int[2 + (int) Math.ceil(2 + (1 << (int) (Math.log(nBluestein + 0.5) / Math.log(2)) / 2))];
                this.w = new float[nBluestein];
                int twon = 2 * nBluestein;
                nw = ip[0];
                if (twon > (nw << 2)) {
                    nw = twon >> 2;
                    makewt(nw);
                }
                nc = ip[1];
                if (nBluestein > (nc << 2)) {
                    nc = nBluestein >> 2;
                    makect(nc, w, nw);
                }
                bluesteini();
            } else {
                plan = Plans.MIXED_RADIX;
                wtable = new float[4 * n + 15];
                wtable_r = new float[2 * n + 15];
                cffti();
                rffti();
            }
        } else {
            plan = Plans.SPLIT_RADIX;
            this.ip = new int[2 + (int) Math.ceil(2 + (1 << (int) (Math.log(n + 0.5) / Math.log(2)) / 2))];
            this.w = new float[n];
            int twon = 2 * n;
            nw = ip[0];
            if (twon > (nw << 2)) {
                nw = twon >> 2;
                makewt(nw);
            }
            nc = ip[1];
            if (n > (nc << 2)) {
                nc = n >> 2;
                makect(nc, w, nw);
            }
        }
    }

    /**
     * Computes 1D forward DFT of complex data leaving the result in
     * <code>a</code>. Complex number is stored as two float values in
     * sequence: the real and imaginary part, i.e. the size of the input array
     * must be greater or equal 2*n. The physical layout of the input data has
     * to be as follows:<br>
     * 
     * <pre>
     * a[2*k] = Re[k], 
     * a[2*k+1] = Im[k], 0&lt;=k&lt;n
     * </pre>
     * 
     * @param a
     *            data to transform
     */
    public void complexForward(float[] a) {
        complexForward(a, 0);
    }

    /**
     * Computes 1D forward DFT of complex data leaving the result in
     * <code>a</code>. Complex number is stored as two float values in
     * sequence: the real and imaginary part, i.e. the size of the input array
     * must be greater or equal 2*n. The physical layout of the input data has
     * to be as follows:<br>
     * 
     * <pre>
     * a[offa+2*k] = Re[k], 
     * a[offa+2*k+1] = Im[k], 0&lt;=k&lt;n
     * </pre>
     * 
     * @param a
     *            data to transform
     * @param offa
     *            index of the first element in array <code>a</code>
     */
    public void complexForward(float[] a, int offa) {
        if (n == 1)
            return;
        switch (plan) {
        case SPLIT_RADIX:
            cftbsub(2 * n, a, offa, ip, nw, w);
            break;
        case MIXED_RADIX:
            cfftf(a, offa, -1);
            break;
        case BLUESTEIN:
            bluestein_complex(a, offa, -1);
            break;
        }
    }

    /**
     * Computes 1D inverse DFT of complex data leaving the result in
     * <code>a</code>. Complex number is stored as two float values in
     * sequence: the real and imaginary part, i.e. the size of the input array
     * must be greater or equal 2*n. The physical layout of the input data has
     * to be as follows:<br>
     * 
     * <pre>
     * a[2*k] = Re[k], 
     * a[2*k+1] = Im[k], 0&lt;=k&lt;n
     * </pre>
     * 
     * @param a
     *            data to transform
     * @param scale
     *            if true then scaling is performed
     */
    public void complexInverse(float[] a, boolean scale) {
        complexInverse(a, 0, scale);
    }

    /**
     * Computes 1D inverse DFT of complex data leaving the result in
     * <code>a</code>. Complex number is stored as two float values in
     * sequence: the real and imaginary part, i.e. the size of the input array
     * must be greater or equal 2*n. The physical layout of the input data has
     * to be as follows:<br>
     * 
     * <pre>
     * a[offa+2*k] = Re[k], 
     * a[offa+2*k+1] = Im[k], 0&lt;=k&lt;n
     * </pre>
     * 
     * @param a
     *            data to transform
     * @param offa
     *            index of the first element in array <code>a</code>
     * @param scale
     *            if true then scaling is performed
     */
    public void complexInverse(float[] a, int offa, boolean scale) {
        if (n == 1)
            return;
        switch (plan) {
        case SPLIT_RADIX:
            cftfsub(2 * n, a, offa, ip, nw, w);
            break;
        case MIXED_RADIX:
            cfftf(a, offa, +1);
            break;
        case BLUESTEIN:
            bluestein_complex(a, offa, 1);
            break;
        }
        if (scale) {
            scale(n, a, offa, true);
        }
    }

    /**
     * Computes 1D forward DFT of real data leaving the result in <code>a</code>
     * . The physical layout of the output data is as follows:<br>
     * 
     * if n is even then
     * 
     * <pre>
     * a[2*k] = Re[k], 0&lt;=k&lt;n/2
     * a[2*k+1] = Im[k], 0&lt;k&lt;n/2
     * a[1] = Re[n/2]
     * </pre>
     * 
     * if n is odd then
     * 
     * <pre>
     * a[2*k] = Re[k], 0&lt;=k&lt;(n+1)/2
     * a[2*k+1] = Im[k], 0&lt;k&lt;(n-1)/2
     * a[1] = Im[(n-1)/2]
     * </pre>
     * 
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * forward transform, use <code>realForwardFull</code>. To get back the
     * original data, use <code>realInverse</code> on the output of this method.
     * 
     * @param a
     *            data to transform
     */
    public void realForward(float[] a) {
        realForward(a, 0);
    }

    /**
     * Computes 1D forward DFT of real data leaving the result in <code>a</code>
     * . The physical layout of the output data is as follows:<br>
     * 
     * if n is even then
     * 
     * <pre>
     * a[offa+2*k] = Re[k], 0&lt;=k&lt;n/2
     * a[offa+2*k+1] = Im[k], 0&lt;k&lt;n/2
     * a[offa+1] = Re[n/2]
     * </pre>
     * 
     * if n is odd then
     * 
     * <pre>
     * a[offa+2*k] = Re[k], 0&lt;=k&lt;(n+1)/2
     * a[offa+2*k+1] = Im[k], 0&lt;k&lt;(n-1)/2
     * a[offa+1] = Im[(n-1)/2]
     * </pre>
     * 
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * forward transform, use <code>realForwardFull</code>. To get back the
     * original data, use <code>realInverse</code> on the output of this method.
     * 
     * @param a
     *            data to transform
     * @param offa
     *            index of the first element in array <code>a</code>
     */
    public void realForward(float[] a, int offa) {
        if (n == 1)
            return;

        switch (plan) {
        case SPLIT_RADIX:
            float xi;

            if (n > 4) {
                cftfsub(n, a, offa, ip, nw, w);
                rftfsub(n, a, offa, nc, w, nw);
            } else if (n == 4) {
                cftx020(a, offa);
            }
            xi = a[offa] - a[offa + 1];
            a[offa] += a[offa + 1];
            a[offa + 1] = xi;
            break;
        case MIXED_RADIX:
            rfftf(a, offa);
            for (int k = n - 1; k >= 2; k--) {
                int idx = offa + k;
                float tmp = a[idx];
                a[idx] = a[idx - 1];
                a[idx - 1] = tmp;
            }
            break;
        case BLUESTEIN:
            bluestein_real_forward(a, offa);
            break;
        }
    }

    /**
     * Computes 1D forward DFT of real data leaving the result in <code>a</code>
     * . This method computes the full real forward transform, i.e. you will get
     * the same result as from <code>complexForward</code> called with all
     * imaginary parts equal 0. Because the result is stored in <code>a</code>,
     * the size of the input array must greater or equal 2*n, with only the
     * first n elements filled with real data. To get back the original data,
     * use <code>complexInverse</code> on the output of this method.
     * 
     * @param a
     *            data to transform
     */
    public void realForwardFull(float[] a) {
        realForwardFull(a, 0);
    }

    /**
     * Computes 1D forward DFT of real data leaving the result in <code>a</code>
     * . This method computes the full real forward transform, i.e. you will get
     * the same result as from <code>complexForward</code> called with all
     * imaginary part equal 0. Because the result is stored in <code>a</code>,
     * the size of the input array must greater or equal 2*n, with only the
     * first n elements filled with real data. To get back the original data,
     * use <code>complexInverse</code> on the output of this method.
     * 
     * @param a
     *            data to transform
     * @param offa
     *            index of the first element in array <code>a</code>
     */
    public void realForwardFull(final float[] a, final int offa) {

        final int twon = 2 * n;
        switch (plan) {
        case SPLIT_RADIX:
            realForward(a, offa);
            int nthreads = ConcurrencyUtils.getNumberOfThreads();
            if ((nthreads > 1) && (n / 2 > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
                Future<?>[] futures = new Future[nthreads];
                int k = n / 2 / nthreads;
                for (int i = 0; i < nthreads; i++) {
                    final int firstIdx = i * k;
                    final int lastIdx = (i == (nthreads - 1)) ? n / 2 : firstIdx + k;
                    futures[i] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            int idx1, idx2;
                            for (int k = firstIdx; k < lastIdx; k++) {
                                idx1 = 2 * k;
                                idx2 = offa + ((twon - idx1) % twon);
                                a[idx2] = a[offa + idx1];
                                a[idx2 + 1] = -a[offa + idx1 + 1];
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);
            } else {
                int idx1, idx2;
                for (int k = 0; k < n / 2; k++) {
                    idx1 = 2 * k;
                    idx2 = offa + ((twon - idx1) % twon);
                    a[idx2] = a[offa + idx1];
                    a[idx2 + 1] = -a[offa + idx1 + 1];
                }
            }
            a[offa + n] = -a[offa + 1];
            a[offa + 1] = 0;
            break;
        case MIXED_RADIX:
            rfftf(a, offa);
            int m;
            if (n % 2 == 0) {
                m = n / 2;
            } else {
                m = (n + 1) / 2;
            }
            for (int k = 1; k < m; k++) {
                int idx1 = offa + twon - 2 * k;
                int idx2 = offa + 2 * k;
                a[idx1 + 1] = -a[idx2];
                a[idx1] = a[idx2 - 1];
            }
            for (int k = 1; k < n; k++) {
                int idx = offa + n - k;
                float tmp = a[idx + 1];
                a[idx + 1] = a[idx];
                a[idx] = tmp;
            }
            a[offa + 1] = 0;
            break;
        case BLUESTEIN:
            bluestein_real_full(a, offa, -1);
            break;
        }
    }

    /**
     * Computes 1D inverse DFT of real data leaving the result in <code>a</code>
     * . The physical layout of the input data has to be as follows:<br>
     * 
     * if n is even then
     * 
     * <pre>
     * a[2*k] = Re[k], 0&lt;=k&lt;n/2
     * a[2*k+1] = Im[k], 0&lt;k&lt;n/2
     * a[1] = Re[n/2]
     * </pre>
     * 
     * if n is odd then
     * 
     * <pre>
     * a[2*k] = Re[k], 0&lt;=k&lt;(n+1)/2
     * a[2*k+1] = Im[k], 0&lt;k&lt;(n-1)/2
     * a[1] = Im[(n-1)/2]
     * </pre>
     * 
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * inverse transform, use <code>realInverseFull</code>.
     * 
     * @param a
     *            data to transform
     * 
     * @param scale
     *            if true then scaling is performed
     * 
     */
    public void realInverse(float[] a, boolean scale) {
        realInverse(a, 0, scale);
    }

    /**
     * Computes 1D inverse DFT of real data leaving the result in <code>a</code>
     * . The physical layout of the input data has to be as follows:<br>
     * 
     * if n is even then
     * 
     * <pre>
     * a[offa+2*k] = Re[k], 0&lt;=k&lt;n/2
     * a[offa+2*k+1] = Im[k], 0&lt;k&lt;n/2
     * a[offa+1] = Re[n/2]
     * </pre>
     * 
     * if n is odd then
     * 
     * <pre>
     * a[offa+2*k] = Re[k], 0&lt;=k&lt;(n+1)/2
     * a[offa+2*k+1] = Im[k], 0&lt;k&lt;(n-1)/2
     * a[offa+1] = Im[(n-1)/2]
     * </pre>
     * 
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * inverse transform, use <code>realInverseFull</code>.
     * 
     * @param a
     *            data to transform
     * @param offa
     *            index of the first element in array <code>a</code>
     * @param scale
     *            if true then scaling is performed
     * 
     */
    public void realInverse(float[] a, int offa, boolean scale) {
        if (n == 1)
            return;
        switch (plan) {
        case SPLIT_RADIX:
            a[offa + 1] = (float)(0.5 * (a[offa] - a[offa + 1]));
            a[offa] -= a[offa + 1];
            if (n > 4) {
                rftfsub(n, a, offa, nc, w, nw);
                cftbsub(n, a, offa, ip, nw, w);
            } else if (n == 4) {
                cftxc020(a, offa);
            }
            if (scale) {
                scale(n / 2, a, offa, false);
            }
            break;
        case MIXED_RADIX:
            for (int k = 2; k < n; k++) {
                int idx = offa + k;
                float tmp = a[idx - 1];
                a[idx - 1] = a[idx];
                a[idx] = tmp;
            }
            rfftb(a, offa);
            if (scale) {
                scale(n, a, offa, false);
            }
            break;
        case BLUESTEIN:
            bluestein_real_inverse(a, offa);
            if (scale) {
                scale(n, a, offa, false);
            }
            break;
        }

    }

    /**
     * Computes 1D inverse DFT of real data leaving the result in <code>a</code>
     * . This method computes the full real inverse transform, i.e. you will get
     * the same result as from <code>complexInverse</code> called with all
     * imaginary part equal 0. Because the result is stored in <code>a</code>,
     * the size of the input array must greater or equal 2*n, with only the
     * first n elements filled with real data.
     * 
     * @param a
     *            data to transform
     * @param scale
     *            if true then scaling is performed
     */
    public void realInverseFull(float[] a, boolean scale) {
        realInverseFull(a, 0, scale);
    }

    /**
     * Computes 1D inverse DFT of real data leaving the result in <code>a</code>
     * . This method computes the full real inverse transform, i.e. you will get
     * the same result as from <code>complexInverse</code> called with all
     * imaginary part equal 0. Because the result is stored in <code>a</code>,
     * the size of the input array must greater or equal 2*n, with only the
     * first n elements filled with real data.
     * 
     * @param a
     *            data to transform
     * @param offa
     *            index of the first element in array <code>a</code>
     * @param scale
     *            if true then scaling is performed
     */
    public void realInverseFull(final float[] a, final int offa, boolean scale) {
        final int twon = 2 * n;
        switch (plan) {
        case SPLIT_RADIX:
            realInverse2(a, offa, scale);
            int nthreads = ConcurrencyUtils.getNumberOfThreads();
            if ((nthreads > 1) && (n / 2 > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
                Future<?>[] futures = new Future[nthreads];
                int k = n / 2 / nthreads;
                for (int i = 0; i < nthreads; i++) {
                    final int firstIdx = i * k;
                    final int lastIdx = (i == (nthreads - 1)) ? n / 2 : firstIdx + k;
                    futures[i] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            int idx1, idx2;
                            for (int k = firstIdx; k < lastIdx; k++) {
                                idx1 = 2 * k;
                                idx2 = offa + ((twon - idx1) % twon);
                                a[idx2] = a[offa + idx1];
                                a[idx2 + 1] = -a[offa + idx1 + 1];
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);
            } else {
                int idx1, idx2;
                for (int k = 0; k < n / 2; k++) {
                    idx1 = 2 * k;
                    idx2 = offa + ((twon - idx1) % twon);
                    a[idx2] = a[offa + idx1];
                    a[idx2 + 1] = -a[offa + idx1 + 1];
                }
            }
            a[offa + n] = -a[offa + 1];
            a[offa + 1] = 0;
            break;
        case MIXED_RADIX:
            rfftf(a, offa);
            if (scale) {
                scale(n, a, offa, false);
            }
            int m;
            if (n % 2 == 0) {
                m = n / 2;
            } else {
                m = (n + 1) / 2;
            }
            for (int k = 1; k < m; k++) {
                int idx1 = offa + 2 * k;
                int idx2 = offa + twon - 2 * k;
                a[idx1] = -a[idx1];
                a[idx2 + 1] = -a[idx1];
                a[idx2] = a[idx1 - 1];
            }
            for (int k = 1; k < n; k++) {
                int idx = offa + n - k;
                float tmp = a[idx + 1];
                a[idx + 1] = a[idx];
                a[idx] = tmp;
            }
            a[offa + 1] = 0;
            break;
        case BLUESTEIN:
            bluestein_real_full(a, offa, 1);
            if (scale) {
                scale(n, a, offa, true);
            }
            break;
        }
    }

    protected void realInverse2(float[] a, int offa, boolean scale) {
        if (n == 1)
            return;
        switch (plan) {
        case SPLIT_RADIX:
            float xi;

            if (n > 4) {
                cftfsub(n, a, offa, ip, nw, w);
                rftbsub(n, a, offa, nc, w, nw);
            } else if (n == 4) {
                cftbsub(n, a, offa, ip, nw, w);
            }
            xi = a[offa] - a[offa + 1];
            a[offa] += a[offa + 1];
            a[offa + 1] = xi;
            if (scale) {
                scale(n, a, offa, false);
            }
            break;
        case MIXED_RADIX:
            rfftf(a, offa);
            for (int k = n - 1; k >= 2; k--) {
                int idx = offa + k;
                float tmp = a[idx];
                a[idx] = a[idx - 1];
                a[idx - 1] = tmp;
            }
            if (scale) {
                scale(n, a, offa, false);
            }
            int m;
            if (n % 2 == 0) {
                m = n / 2;
                for (int i = 1; i < m; i++) {
                    int idx = offa + 2 * i + 1;
                    a[idx] = -a[idx];
                }
            } else {
                m = (n - 1) / 2;
                for (int i = 0; i < m; i++) {
                    int idx = offa + 2 * i + 1;
                    a[idx] = -a[idx];
                }
            }
            break;
        case BLUESTEIN:
            bluestein_real_inverse2(a, offa);
            if (scale) {
                scale(n, a, offa, false);
            }
            break;
        }
    }

    private static int getReminder(int n, int factors[]) {
        int reminder = n;

        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive integer");
        }

        for (int i = 0; i < factors.length && reminder != 1; i++) {
            int factor = factors[i];
            while ((reminder % factor) == 0) {
                reminder /= factor;
            }
        }
        return reminder;
    }

    /* -------- initializing routines -------- */

    /*---------------------------------------------------------
       cffti: initialization of Complex FFT
      --------------------------------------------------------*/

    void cffti(int n, int offw) {
        if (n == 1)
            return;

        final int twon = 2 * n;
        final int fourn = 4 * n;
        float argh;
        int idot, ntry = 0, i, j;
        float argld;
        int i1, k1, l1, l2, ib;
        float fi;
        int ld, ii, nf, ip, nl, nq, nr;
        float arg;
        int ido, ipm;

        nl = n;
        nf = 0;
        j = 0;

        factorize_loop: while (true) {
            j++;
            if (j <= 4)
                ntry = factors[j - 1];
            else
                ntry += 2;
            do {
                nq = nl / ntry;
                nr = nl - ntry * nq;
                if (nr != 0)
                    continue factorize_loop;
                nf++;
                wtable[offw + nf + 1 + fourn] = ntry;
                nl = nq;
                if (ntry == 2 && nf != 1) {
                    for (i = 2; i <= nf; i++) {
                        ib = nf - i + 2;
                        int idx = ib + fourn;
                        wtable[offw + idx + 1] = wtable[offw + idx];
                    }
                    wtable[offw + 2 + fourn] = 2;
                }
            } while (nl != 1);
            break factorize_loop;
        }
        wtable[offw + fourn] = n;
        wtable[offw + 1 + fourn] = nf;
        argh = TWO_PI / (float) n;
        i = 1;
        l1 = 1;
        for (k1 = 1; k1 <= nf; k1++) {
            ip = (int) wtable[offw + k1 + 1 + fourn];
            ld = 0;
            l2 = l1 * ip;
            ido = n / l2;
            idot = ido + ido + 2;
            ipm = ip - 1;
            for (j = 1; j <= ipm; j++) {
                i1 = i;
                wtable[offw + i - 1 + twon] = 1;
                wtable[offw + i + twon] = 0;
                ld += l1;
                fi = 0;
                argld = ld * argh;
                for (ii = 4; ii <= idot; ii += 2) {
                    i += 2;
                    fi += 1;
                    arg = fi * argld;
                    int idx = i + twon;
                    wtable[offw + idx - 1] = (float)Math.cos(arg);
                    wtable[offw + idx] = (float)Math.sin(arg);
                }
                if (ip > 5) {
                    int idx1 = i1 + twon;
                    int idx2 = i + twon;
                    wtable[offw + idx1 - 1] = wtable[offw + idx2 - 1];
                    wtable[offw + idx1] = wtable[offw + idx2];
                }
            }
            l1 = l2;
        }

    }

    void cffti() {
        if (n == 1)
            return;

        final int twon = 2 * n;
        final int fourn = 4 * n;
        float argh;
        int idot, ntry = 0, i, j;
        float argld;
        int i1, k1, l1, l2, ib;
        float fi;
        int ld, ii, nf, ip, nl, nq, nr;
        float arg;
        int ido, ipm;

        nl = n;
        nf = 0;
        j = 0;

        factorize_loop: while (true) {
            j++;
            if (j <= 4)
                ntry = factors[j - 1];
            else
                ntry += 2;
            do {
                nq = nl / ntry;
                nr = nl - ntry * nq;
                if (nr != 0)
                    continue factorize_loop;
                nf++;
                wtable[nf + 1 + fourn] = ntry;
                nl = nq;
                if (ntry == 2 && nf != 1) {
                    for (i = 2; i <= nf; i++) {
                        ib = nf - i + 2;
                        int idx = ib + fourn;
                        wtable[idx + 1] = wtable[idx];
                    }
                    wtable[2 + fourn] = 2;
                }
            } while (nl != 1);
            break factorize_loop;
        }
        wtable[fourn] = n;
        wtable[1 + fourn] = nf;
        argh = TWO_PI / (float) n;
        i = 1;
        l1 = 1;
        for (k1 = 1; k1 <= nf; k1++) {
            ip = (int) wtable[k1 + 1 + fourn];
            ld = 0;
            l2 = l1 * ip;
            ido = n / l2;
            idot = ido + ido + 2;
            ipm = ip - 1;
            for (j = 1; j <= ipm; j++) {
                i1 = i;
                wtable[i - 1 + twon] = 1;
                wtable[i + twon] = 0;
                ld += l1;
                fi = 0;
                argld = ld * argh;
                for (ii = 4; ii <= idot; ii += 2) {
                    i += 2;
                    fi += 1;
                    arg = fi * argld;
                    int idx = i + twon;
                    wtable[idx - 1] = (float)Math.cos(arg);
                    wtable[idx] = (float)Math.sin(arg);
                }
                if (ip > 5) {
                    int idx1 = i1 + twon;
                    int idx2 = i + twon;
                    wtable[idx1 - 1] = wtable[idx2 - 1];
                    wtable[idx1] = wtable[idx2];
                }
            }
            l1 = l2;
        }

    }

    void rffti() {

        if (n == 1)
            return;
        final int twon = 2 * n;
        float argh;
        int ntry = 0, i, j;
        float argld;
        int k1, l1, l2, ib;
        float fi;
        int ld, ii, nf, ip, nl, is, nq, nr;
        float arg;
        int ido, ipm;
        int nfm1;

        nl = n;
        nf = 0;
        j = 0;

        factorize_loop: while (true) {
            ++j;
            if (j <= 4)
                ntry = factors[j - 1];
            else
                ntry += 2;
            do {
                nq = nl / ntry;
                nr = nl - ntry * nq;
                if (nr != 0)
                    continue factorize_loop;
                ++nf;
                wtable_r[nf + 1 + twon] = ntry;

                nl = nq;
                if (ntry == 2 && nf != 1) {
                    for (i = 2; i <= nf; i++) {
                        ib = nf - i + 2;
                        int idx = ib + twon;
                        wtable_r[idx + 1] = wtable_r[idx];
                    }
                    wtable_r[2 + twon] = 2;
                }
            } while (nl != 1);
            break factorize_loop;
        }
        wtable_r[twon] = n;
        wtable_r[1 + twon] = nf;
        argh = TWO_PI / (float) (n);
        is = 0;
        nfm1 = nf - 1;
        l1 = 1;
        if (nfm1 == 0)
            return;
        for (k1 = 1; k1 <= nfm1; k1++) {
            ip = (int) wtable_r[k1 + 1 + twon];
            ld = 0;
            l2 = l1 * ip;
            ido = n / l2;
            ipm = ip - 1;
            for (j = 1; j <= ipm; ++j) {
                ld += l1;
                i = is;
                argld = (float) ld * argh;

                fi = 0;
                for (ii = 3; ii <= ido; ii += 2) {
                    i += 2;
                    fi += 1;
                    arg = fi * argld;
                    int idx = i + n;
                    wtable_r[idx - 2] = (float)Math.cos(arg);
                    wtable_r[idx - 1] = (float)Math.sin(arg);
                }
                is += ido;
            }
            l1 = l2;
        }
    }

    private void bluesteini() {
        int k = 0;
        float arg;
        float pi_n = PI / n;
        bk1[0] = 1;
        bk1[1] = 0;
        for (int i = 1; i < n; i++) {
            k += 2 * i - 1;
            if (k >= 2 * n)
                k -= 2 * n;
            arg = pi_n * k;
            bk1[2 * i] = (float)Math.cos(arg);
            bk1[2 * i + 1] = (float)Math.sin(arg);
        }
        float scale = (float)(1.0 / nBluestein);
        bk2[0] = bk1[0] * scale;
        bk2[1] = bk1[1] * scale;
        for (int i = 2; i < 2 * n; i += 2) {
            bk2[i] = bk1[i] * scale;
            bk2[i + 1] = bk1[i + 1] * scale;
            bk2[2 * nBluestein - i] = bk2[i];
            bk2[2 * nBluestein - i + 1] = bk2[i + 1];
        }
        cftbsub(2 * nBluestein, bk2, 0, ip, nw, w);
    }

    private void makewt(int nw) {
        int j, nwh, nw0, nw1;
        float delta, wn4r, wk1r, wk1i, wk3r, wk3i;
        float delta2, deltaj, deltaj3;

        ip[0] = nw;
        ip[1] = 1;
        if (nw > 2) {
            nwh = nw >> 1;
            delta = (float)(0.785398163397448278999490867136046290 / nwh);
            delta2 = delta * 2;
            wn4r = (float)Math.cos(delta * nwh);
            w[0] = 1;
            w[1] = wn4r;
            if (nwh == 4) {
                w[2] = (float)Math.cos(delta2);
                w[3] = (float)Math.sin(delta2);
            } else if (nwh > 4) {
                makeipt(nw);
                w[2] = (float)(0.5 / Math.cos(delta2));
                w[3] = (float)(0.5 / Math.cos(delta * 6));
                for (j = 4; j < nwh; j += 4) {
                    deltaj = delta * j;
                    deltaj3 = 3 * deltaj;
                    w[j] = (float)Math.cos(deltaj);
                    w[j + 1] = (float)Math.sin(deltaj);
                    w[j + 2] = (float)Math.cos(deltaj3);
                    w[j + 3] = (float)-Math.sin(deltaj3);
                }
            }
            nw0 = 0;
            while (nwh > 2) {
                nw1 = nw0 + nwh;
                nwh >>= 1;
                w[nw1] = 1;
                w[nw1 + 1] = wn4r;
                if (nwh == 4) {
                    wk1r = w[nw0 + 4];
                    wk1i = w[nw0 + 5];
                    w[nw1 + 2] = wk1r;
                    w[nw1 + 3] = wk1i;
                } else if (nwh > 4) {
                    wk1r = w[nw0 + 4];
                    wk3r = w[nw0 + 6];
                    w[nw1 + 2] = (float)(0.5 / wk1r);
                    w[nw1 + 3] = (float)(0.5 / wk3r);
                    for (j = 4; j < nwh; j += 4) {
                        int idx1 = nw0 + 2 * j;
                        int idx2 = nw1 + j;
                        wk1r = w[idx1];
                        wk1i = w[idx1 + 1];
                        wk3r = w[idx1 + 2];
                        wk3i = w[idx1 + 3];
                        w[idx2] = wk1r;
                        w[idx2 + 1] = wk1i;
                        w[idx2 + 2] = wk3r;
                        w[idx2 + 3] = wk3i;
                    }
                }
                nw0 = nw1;
            }
        }
    }

    private void makeipt(int nw) {
        int j, l, m, m2, p, q;

        ip[2] = 0;
        ip[3] = 16;
        m = 2;
        for (l = nw; l > 32; l >>= 2) {
            m2 = m << 1;
            q = m2 << 3;
            for (j = m; j < m2; j++) {
                p = ip[j] << 2;
                ip[m + j] = p;
                ip[m2 + j] = p + q;
            }
            m = m2;
        }
    }

    private void makect(int nc, float[] c, int startc) {
        int j, nch;
        float delta, deltaj;

        ip[1] = nc;
        if (nc > 1) {
            nch = nc >> 1;
            delta = (float)(0.785398163397448278999490867136046290 / nch);
            c[startc] = (float)Math.cos(delta * nch);
            c[startc + nch] = (float)(0.5 * c[startc]);
            for (j = 1; j < nch; j++) {
                deltaj = delta * j;
                c[startc + j] =  (float)(0.5 * Math.cos(deltaj));
                c[startc + nc - j] = (float)(0.5 * Math.sin(deltaj));
            }
        }
    }

    private void bluestein_complex(final float[] a, final int offa, final int isign) {
        final float[] ak = new float[2 * nBluestein];
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
            nthreads = 2;
            if ((nthreads >= 4) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads())) {
                nthreads = 4;
            }
            Future<?>[] futures = new Future[nthreads];
            int k = n / nthreads;
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = i * k;
                final int lastIdx = (i == (nthreads - 1)) ? n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        if (isign > 0) {
                            for (int i = firstIdx; i < lastIdx; i++) {
                                int idx1 = 2 * i;
                                int idx2 = idx1 + 1;
                                int idx3 = offa + idx1;
                                int idx4 = offa + idx2;
                                ak[idx1] = a[idx3] * bk1[idx1] - a[idx4] * bk1[idx2];
                                ak[idx2] = a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1];
                            }
                        } else {
                            for (int i = firstIdx; i < lastIdx; i++) {
                                int idx1 = 2 * i;
                                int idx2 = idx1 + 1;
                                int idx3 = offa + idx1;
                                int idx4 = offa + idx2;
                                ak[idx1] = a[idx3] * bk1[idx1] + a[idx4] * bk1[idx2];
                                ak[idx2] = -a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            k = nBluestein / nthreads;
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = i * k;
                final int lastIdx = (i == (nthreads - 1)) ? nBluestein : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        if (isign > 0) {
                            for (int i = firstIdx; i < lastIdx; i++) {
                                int idx1 = 2 * i;
                                int idx2 = idx1 + 1;
                                float im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                                ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                                ak[idx2] = im;
                            }
                        } else {
                            for (int i = firstIdx; i < lastIdx; i++) {
                                int idx1 = 2 * i;
                                int idx2 = idx1 + 1;
                                float im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                                ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2];
                                ak[idx2] = im;
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            cftfsub(2 * nBluestein, ak, 0, ip, nw, w);

            k = n / nthreads;
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = i * k;
                final int lastIdx = (i == (nthreads - 1)) ? n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        if (isign > 0) {
                            for (int i = firstIdx; i < lastIdx; i++) {
                                int idx1 = 2 * i;
                                int idx2 = idx1 + 1;
                                int idx3 = offa + idx1;
                                int idx4 = offa + idx2;
                                a[idx3] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
                                a[idx4] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                            }
                        } else {
                            for (int i = firstIdx; i < lastIdx; i++) {
                                int idx1 = 2 * i;
                                int idx2 = idx1 + 1;
                                int idx3 = offa + idx1;
                                int idx4 = offa + idx2;
                                a[idx3] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2];
                                a[idx4] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {
            if (isign > 0) {
                for (int i = 0; i < n; i++) {
                    int idx1 = 2 * i;
                    int idx2 = idx1 + 1;
                    int idx3 = offa + idx1;
                    int idx4 = offa + idx2;
                    ak[idx1] = a[idx3] * bk1[idx1] - a[idx4] * bk1[idx2];
                    ak[idx2] = a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1];
                }
            } else {
                for (int i = 0; i < n; i++) {
                    int idx1 = 2 * i;
                    int idx2 = idx1 + 1;
                    int idx3 = offa + idx1;
                    int idx4 = offa + idx2;
                    ak[idx1] = a[idx3] * bk1[idx1] + a[idx4] * bk1[idx2];
                    ak[idx2] = -a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1];
                }
            }

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            if (isign > 0) {
                for (int i = 0; i < nBluestein; i++) {
                    int idx1 = 2 * i;
                    int idx2 = idx1 + 1;
                    float im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                    ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                    ak[idx2] = im;
                }
            } else {
                for (int i = 0; i < nBluestein; i++) {
                    int idx1 = 2 * i;
                    int idx2 = idx1 + 1;
                    float im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                    ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2];
                    ak[idx2] = im;
                }
            }

            cftfsub(2 * nBluestein, ak, 0, ip, nw, w);
            if (isign > 0) {
                for (int i = 0; i < n; i++) {
                    int idx1 = 2 * i;
                    int idx2 = idx1 + 1;
                    int idx3 = offa + idx1;
                    int idx4 = offa + idx2;
                    a[idx3] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
                    a[idx4] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                }
            } else {
                for (int i = 0; i < n; i++) {
                    int idx1 = 2 * i;
                    int idx2 = idx1 + 1;
                    int idx3 = offa + idx1;
                    int idx4 = offa + idx2;
                    a[idx3] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2];
                    a[idx4] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                }
            }
        }
    }

    private void bluestein_real_full(final float[] a, final int offa, final int isign) {
        final float[] ak = new float[2 * nBluestein];
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
            nthreads = 2;
            if ((nthreads >= 4) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads())) {
                nthreads = 4;
            }
            Future<?>[] futures = new Future[nthreads];
            int k = n / nthreads;
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = i * k;
                final int lastIdx = (i == (nthreads - 1)) ? n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        if (isign > 0) {
                            for (int i = firstIdx; i < lastIdx; i++) {
                                int idx1 = 2 * i;
                                int idx2 = idx1 + 1;
                                int idx3 = offa + i;
                                ak[idx1] = a[idx3] * bk1[idx1];
                                ak[idx2] = a[idx3] * bk1[idx2];
                            }
                        } else {
                            for (int i = firstIdx; i < lastIdx; i++) {
                                int idx1 = 2 * i;
                                int idx2 = idx1 + 1;
                                int idx3 = offa + i;
                                ak[idx1] = a[idx3] * bk1[idx1];
                                ak[idx2] = -a[idx3] * bk1[idx2];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            k = nBluestein / nthreads;
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = i * k;
                final int lastIdx = (i == (nthreads - 1)) ? nBluestein : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        if (isign > 0) {
                            for (int i = firstIdx; i < lastIdx; i++) {
                                int idx1 = 2 * i;
                                int idx2 = idx1 + 1;
                                float im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                                ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                                ak[idx2] = im;
                            }
                        } else {
                            for (int i = firstIdx; i < lastIdx; i++) {
                                int idx1 = 2 * i;
                                int idx2 = idx1 + 1;
                                float im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                                ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2];
                                ak[idx2] = im;
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            cftfsub(2 * nBluestein, ak, 0, ip, nw, w);

            k = n / nthreads;
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = i * k;
                final int lastIdx = (i == (nthreads - 1)) ? n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        if (isign > 0) {
                            for (int i = firstIdx; i < lastIdx; i++) {
                                int idx1 = 2 * i;
                                int idx2 = idx1 + 1;
                                a[offa + idx1] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
                                a[offa + idx2] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                            }
                        } else {
                            for (int i = firstIdx; i < lastIdx; i++) {
                                int idx1 = 2 * i;
                                int idx2 = idx1 + 1;
                                a[offa + idx1] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2];
                                a[offa + idx2] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {
            if (isign > 0) {
                for (int i = 0; i < n; i++) {
                    int idx1 = 2 * i;
                    int idx2 = idx1 + 1;
                    int idx3 = offa + i;
                    ak[idx1] = a[idx3] * bk1[idx1];
                    ak[idx2] = a[idx3] * bk1[idx2];
                }
            } else {
                for (int i = 0; i < n; i++) {
                    int idx1 = 2 * i;
                    int idx2 = idx1 + 1;
                    int idx3 = offa + i;
                    ak[idx1] = a[idx3] * bk1[idx1];
                    ak[idx2] = -a[idx3] * bk1[idx2];
                }
            }

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            if (isign > 0) {
                for (int i = 0; i < nBluestein; i++) {
                    int idx1 = 2 * i;
                    int idx2 = idx1 + 1;
                    float im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                    ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                    ak[idx2] = im;
                }
            } else {
                for (int i = 0; i < nBluestein; i++) {
                    int idx1 = 2 * i;
                    int idx2 = idx1 + 1;
                    float im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                    ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2];
                    ak[idx2] = im;
                }
            }

            cftfsub(2 * nBluestein, ak, 0, ip, nw, w);

            if (isign > 0) {
                for (int i = 0; i < n; i++) {
                    int idx1 = 2 * i;
                    int idx2 = idx1 + 1;
                    a[offa + idx1] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
                    a[offa + idx2] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                }
            } else {
                for (int i = 0; i < n; i++) {
                    int idx1 = 2 * i;
                    int idx2 = idx1 + 1;
                    a[offa + idx1] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2];
                    a[offa + idx2] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                }
            }
        }
    }

    private void bluestein_real_forward(final float[] a, final int offa) {
        final float[] ak = new float[2 * nBluestein];
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
            nthreads = 2;
            if ((nthreads >= 4) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads())) {
                nthreads = 4;
            }
            Future<?>[] futures = new Future[nthreads];
            int k = n / nthreads;
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = i * k;
                final int lastIdx = (i == (nthreads - 1)) ? n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        for (int i = firstIdx; i < lastIdx; i++) {
                            int idx1 = 2 * i;
                            int idx2 = idx1 + 1;
                            int idx3 = offa + i;
                            ak[idx1] = a[idx3] * bk1[idx1];
                            ak[idx2] = -a[idx3] * bk1[idx2];
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            k = nBluestein / nthreads;
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = i * k;
                final int lastIdx = (i == (nthreads - 1)) ? nBluestein : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        for (int i = firstIdx; i < lastIdx; i++) {
                            int idx1 = 2 * i;
                            int idx2 = idx1 + 1;
                            float im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                            ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2];
                            ak[idx2] = im;
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

        } else {

            for (int i = 0; i < n; i++) {
                int idx1 = 2 * i;
                int idx2 = idx1 + 1;
                int idx3 = offa + i;
                ak[idx1] = a[idx3] * bk1[idx1];
                ak[idx2] = -a[idx3] * bk1[idx2];
            }

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            for (int i = 0; i < nBluestein; i++) {
                int idx1 = 2 * i;
                int idx2 = idx1 + 1;
                float im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2];
                ak[idx2] = im;
            }
        }

        cftfsub(2 * nBluestein, ak, 0, ip, nw, w);

        if (n % 2 == 0) {
            a[offa] = bk1[0] * ak[0] + bk1[1] * ak[1];
            a[offa + 1] = bk1[n] * ak[n] + bk1[n + 1] * ak[n + 1];
            for (int i = 1; i < n / 2; i++) {
                int idx1 = 2 * i;
                int idx2 = idx1 + 1;
                a[offa + idx1] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2];
                a[offa + idx2] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
            }
        } else {
            a[offa] = bk1[0] * ak[0] + bk1[1] * ak[1];
            a[offa + 1] = -bk1[n] * ak[n - 1] + bk1[n - 1] * ak[n];
            for (int i = 1; i < (n - 1) / 2; i++) {
                int idx1 = 2 * i;
                int idx2 = idx1 + 1;
                a[offa + idx1] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2];
                a[offa + idx2] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
            }
            a[offa + n - 1] = bk1[n - 1] * ak[n - 1] + bk1[n] * ak[n];
        }

    }

    private void bluestein_real_inverse(final float[] a, final int offa) {
        final float[] ak = new float[2 * nBluestein];
        if (n % 2 == 0) {
            ak[0] = a[offa] * bk1[0];
            ak[1] = a[offa] * bk1[1];

            for (int i = 1; i < n / 2; i++) {
                int idx1 = 2 * i;
                int idx2 = idx1 + 1;
                int idx3 = offa + idx1;
                int idx4 = offa + idx2;
                ak[idx1] = a[idx3] * bk1[idx1] - a[idx4] * bk1[idx2];
                ak[idx2] = a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1];
            }

            ak[n] = a[offa + 1] * bk1[n];
            ak[n + 1] = a[offa + 1] * bk1[n + 1];

            for (int i = n / 2 + 1; i < n; i++) {
                int idx1 = 2 * i;
                int idx2 = idx1 + 1;
                int idx3 = offa + 2 * n - idx1;
                int idx4 = idx3 + 1;
                ak[idx1] = a[idx3] * bk1[idx1] + a[idx4] * bk1[idx2];
                ak[idx2] = a[idx3] * bk1[idx2] - a[idx4] * bk1[idx1];
            }

        } else {
            ak[0] = a[offa] * bk1[0];
            ak[1] = a[offa] * bk1[1];

            for (int i = 1; i < (n - 1) / 2; i++) {
                int idx1 = 2 * i;
                int idx2 = idx1 + 1;
                int idx3 = offa + idx1;
                int idx4 = offa + idx2;
                ak[idx1] = a[idx3] * bk1[idx1] - a[idx4] * bk1[idx2];
                ak[idx2] = a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1];
            }

            ak[n - 1] = a[offa + n - 1] * bk1[n - 1] - a[offa + 1] * bk1[n];
            ak[n] = a[offa + n - 1] * bk1[n] + a[offa + 1] * bk1[n - 1];

            ak[n + 1] = a[offa + n - 1] * bk1[n + 1] + a[offa + 1] * bk1[n + 2];
            ak[n + 2] = a[offa + n - 1] * bk1[n + 2] - a[offa + 1] * bk1[n + 1];

            for (int i = (n - 1) / 2 + 2; i < n; i++) {
                int idx1 = 2 * i;
                int idx2 = idx1 + 1;
                int idx3 = offa + 2 * n - idx1;
                int idx4 = idx3 + 1;
                ak[idx1] = a[idx3] * bk1[idx1] + a[idx4] * bk1[idx2];
                ak[idx2] = a[idx3] * bk1[idx2] - a[idx4] * bk1[idx1];
            }
        }

        cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
            nthreads = 2;
            if ((nthreads >= 4) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads())) {
                nthreads = 4;
            }
            Future<?>[] futures = new Future[nthreads];
            int k = nBluestein / nthreads;
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = i * k;
                final int lastIdx = (i == (nthreads - 1)) ? nBluestein : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        for (int i = firstIdx; i < lastIdx; i++) {
                            int idx1 = 2 * i;
                            int idx2 = idx1 + 1;
                            float im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                            ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                            ak[idx2] = im;
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            cftfsub(2 * nBluestein, ak, 0, ip, nw, w);

            k = n / nthreads;
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = i * k;
                final int lastIdx = (i == (nthreads - 1)) ? n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        for (int i = firstIdx; i < lastIdx; i++) {
                            int idx1 = 2 * i;
                            int idx2 = idx1 + 1;
                            a[offa + i] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

        } else {

            for (int i = 0; i < nBluestein; i++) {
                int idx1 = 2 * i;
                int idx2 = idx1 + 1;
                float im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                ak[idx2] = im;
            }

            cftfsub(2 * nBluestein, ak, 0, ip, nw, w);

            for (int i = 0; i < n; i++) {
                int idx1 = 2 * i;
                int idx2 = idx1 + 1;
                a[offa + i] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
            }
        }
    }

    private void bluestein_real_inverse2(final float[] a, final int offa) {
        final float[] ak = new float[2 * nBluestein];
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
            nthreads = 2;
            if ((nthreads >= 4) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads())) {
                nthreads = 4;
            }
            Future<?>[] futures = new Future[nthreads];
            int k = n / nthreads;
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = i * k;
                final int lastIdx = (i == (nthreads - 1)) ? n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        for (int i = firstIdx; i < lastIdx; i++) {
                            int idx1 = 2 * i;
                            int idx2 = idx1 + 1;
                            int idx3 = offa + i;
                            ak[idx1] = a[idx3] * bk1[idx1];
                            ak[idx2] = a[idx3] * bk1[idx2];
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            k = nBluestein / nthreads;
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = i * k;
                final int lastIdx = (i == (nthreads - 1)) ? nBluestein : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        for (int i = firstIdx; i < lastIdx; i++) {
                            int idx1 = 2 * i;
                            int idx2 = idx1 + 1;
                            float im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                            ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                            ak[idx2] = im;
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

        } else {

            for (int i = 0; i < n; i++) {
                int idx1 = 2 * i;
                int idx2 = idx1 + 1;
                int idx3 = offa + i;
                ak[idx1] = a[idx3] * bk1[idx1];
                ak[idx2] = a[idx3] * bk1[idx2];
            }

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            for (int i = 0; i < nBluestein; i++) {
                int idx1 = 2 * i;
                int idx2 = idx1 + 1;
                float im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                ak[idx2] = im;
            }
        }

        cftfsub(2 * nBluestein, ak, 0, ip, nw, w);

        if (n % 2 == 0) {
            a[offa] = bk1[0] * ak[0] - bk1[1] * ak[1];
            a[offa + 1] = bk1[n] * ak[n] - bk1[n + 1] * ak[n + 1];
            for (int i = 1; i < n / 2; i++) {
                int idx1 = 2 * i;
                int idx2 = idx1 + 1;
                a[offa + idx1] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
                a[offa + idx2] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
            }
        } else {
            a[offa] = bk1[0] * ak[0] - bk1[1] * ak[1];
            a[offa + 1] = bk1[n] * ak[n - 1] + bk1[n - 1] * ak[n];
            for (int i = 1; i < (n - 1) / 2; i++) {
                int idx1 = 2 * i;
                int idx2 = idx1 + 1;
                a[offa + idx1] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
                a[offa + idx2] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
            }
            a[offa + n - 1] = bk1[n - 1] * ak[n - 1] - bk1[n] * ak[n];
        }
    }

    /*---------------------------------------------------------
       rfftf1: further processing of Real forward FFT
      --------------------------------------------------------*/
    void rfftf(final float a[], final int offa) {
        if (n == 1)
            return;
        int l1, l2, na, kh, nf, ip, iw, ido, idl1;

        final float[] ch = new float[n];
        final int twon = 2 * n;
        nf = (int) wtable_r[1 + twon];
        na = 1;
        l2 = n;
        iw = twon - 1;
        for (int k1 = 1; k1 <= nf; ++k1) {
            kh = nf - k1;
            ip = (int) wtable_r[kh + 2 + twon];
            l1 = l2 / ip;
            ido = n / l2;
            idl1 = ido * l1;
            iw -= (ip - 1) * ido;
            na = 1 - na;
            switch (ip) {
            case 2:
                if (na == 0) {
                    radf2(ido, l1, a, offa, ch, 0, iw);
                } else {
                    radf2(ido, l1, ch, 0, a, offa, iw);
                }
                break;
            case 3:
                if (na == 0) {
                    radf3(ido, l1, a, offa, ch, 0, iw);
                } else {
                    radf3(ido, l1, ch, 0, a, offa, iw);
                }
                break;
            case 4:
                if (na == 0) {
                    radf4(ido, l1, a, offa, ch, 0, iw);
                } else {
                    radf4(ido, l1, ch, 0, a, offa, iw);
                }
                break;
            case 5:
                if (na == 0) {
                    radf5(ido, l1, a, offa, ch, 0, iw);
                } else {
                    radf5(ido, l1, ch, 0, a, offa, iw);
                }
                break;
            default:
                if (ido == 1)
                    na = 1 - na;
                if (na == 0) {
                    radfg(ido, ip, l1, idl1, a, offa, ch, 0, iw);
                    na = 1;
                } else {
                    radfg(ido, ip, l1, idl1, ch, 0, a, offa, iw);
                    na = 0;
                }
                break;
            }
            l2 = l1;
        }
        if (na == 1)
            return;
        System.arraycopy(ch, 0, a, offa, n);
    }

    /*---------------------------------------------------------
       rfftb1: further processing of Real backward FFT
      --------------------------------------------------------*/
    void rfftb(final float a[], final int offa) {
        if (n == 1)
            return;
        int l1, l2, na, nf, ip, iw, ido, idl1;

        float[] ch = new float[n];
        final int twon = 2 * n;
        nf = (int) wtable_r[1 + twon];
        na = 0;
        l1 = 1;
        iw = n;
        for (int k1 = 1; k1 <= nf; k1++) {
            ip = (int) wtable_r[k1 + 1 + twon];
            l2 = ip * l1;
            ido = n / l2;
            idl1 = ido * l1;
            switch (ip) {
            case 2:
                if (na == 0) {
                    radb2(ido, l1, a, offa, ch, 0, iw);
                } else {
                    radb2(ido, l1, ch, 0, a, offa, iw);
                }
                na = 1 - na;
                break;
            case 3:
                if (na == 0) {
                    radb3(ido, l1, a, offa, ch, 0, iw);
                } else {
                    radb3(ido, l1, ch, 0, a, offa, iw);
                }
                na = 1 - na;
                break;
            case 4:
                if (na == 0) {
                    radb4(ido, l1, a, offa, ch, 0, iw);
                } else {
                    radb4(ido, l1, ch, 0, a, offa, iw);
                }
                na = 1 - na;
                break;
            case 5:
                if (na == 0) {
                    radb5(ido, l1, a, offa, ch, 0, iw);
                } else {
                    radb5(ido, l1, ch, 0, a, offa, iw);
                }
                na = 1 - na;
                break;
            default:
                if (na == 0) {
                    radbg(ido, ip, l1, idl1, a, offa, ch, 0, iw);
                } else {
                    radbg(ido, ip, l1, idl1, ch, 0, a, offa, iw);
                }
                if (ido == 1)
                    na = 1 - na;
                break;
            }
            l1 = l2;
            iw += (ip - 1) * ido;
        }
        if (na == 0)
            return;
        System.arraycopy(ch, 0, a, offa, n);
    }

    /*-------------------------------------------------
       radf2: Real FFT's forward processing of factor 2
      -------------------------------------------------*/
    void radf2(final int ido, final int l1, final float in[], final int in_off, final float out[], final int out_off, final int offset) {
        int i, ic, idx0, idx1, idx2, idx3, idx4;
        float t1i, t1r, w1r, w1i;
        int iw1;
        iw1 = offset;
        idx0 = l1 * ido;
        idx1 = 2 * ido;
        for (int k = 0; k < l1; k++) {
            int oidx1 = out_off + k * idx1;
            int oidx2 = oidx1 + idx1 - 1;
            int iidx1 = in_off + k * ido;
            int iidx2 = iidx1 + idx0;

            float i1r = in[iidx1];
            float i2r = in[iidx2];

            out[oidx1] = i1r + i2r;
            out[oidx2] = i1r - i2r;
        }
        if (ido < 2)
            return;
        if (ido != 2) {
            for (int k = 0; k < l1; k++) {
                idx1 = k * ido;
                idx2 = 2 * idx1;
                idx3 = idx2 + ido;
                idx4 = idx1 + idx0;
                for (i = 2; i < ido; i += 2) {
                    ic = ido - i;
                    int widx1 = i - 1 + iw1;
                    int oidx1 = out_off + i + idx2;
                    int oidx2 = out_off + ic + idx3;
                    int iidx1 = in_off + i + idx1;
                    int iidx2 = in_off + i + idx4;

                    float a1i = in[iidx1 - 1];
                    float a1r = in[iidx1];
                    float a2i = in[iidx2 - 1];
                    float a2r = in[iidx2];

                    w1r = wtable_r[widx1 - 1];
                    w1i = wtable_r[widx1];

                    t1r = w1r * a2i + w1i * a2r;
                    t1i = w1r * a2r - w1i * a2i;

                    out[oidx1] = a1r + t1i;
                    out[oidx1 - 1] = a1i + t1r;

                    out[oidx2] = t1i - a1r;
                    out[oidx2 - 1] = a1i - t1r;
                }
            }
            if (ido % 2 == 1)
                return;
        }
        idx2 = 2 * idx1;
        for (int k = 0; k < l1; k++) {
            idx1 = k * ido;
            int oidx1 = out_off + idx2 + ido;
            int iidx1 = in_off + ido - 1 + idx1;

            out[oidx1] = -in[iidx1 + idx0];
            out[oidx1 - 1] = in[iidx1];
        }
    }

    /*-------------------------------------------------
       radb2: Real FFT's backward processing of factor 2
      -------------------------------------------------*/
    void radb2(final int ido, final int l1, final float in[], final int in_off, final float out[], final int out_off, final int offset) {
        int i, ic;
        float t1i, t1r, w1r, w1i;
        int iw1 = offset;

        int idx0 = l1 * ido;
        for (int k = 0; k < l1; k++) {
            int idx1 = k * ido;
            int idx2 = 2 * idx1;
            int idx3 = idx2 + ido;
            int oidx1 = out_off + idx1;
            int iidx1 = in_off + idx2;
            int iidx2 = in_off + ido - 1 + idx3;
            float i1r = in[iidx1];
            float i2r = in[iidx2];
            out[oidx1] = i1r + i2r;
            out[oidx1 + idx0] = i1r - i2r;
        }
        if (ido < 2)
            return;
        if (ido != 2) {
            for (int k = 0; k < l1; ++k) {
                int idx1 = k * ido;
                int idx2 = 2 * idx1;
                int idx3 = idx2 + ido;
                int idx4 = idx1 + idx0;
                for (i = 2; i < ido; i += 2) {
                    ic = ido - i;
                    int idx5 = i - 1 + iw1;
                    int idx6 = out_off + i;
                    int idx7 = in_off + i;
                    int idx8 = in_off + ic;
                    w1r = wtable_r[idx5 - 1];
                    w1i = wtable_r[idx5];
                    int iidx1 = idx7 + idx2;
                    int iidx2 = idx8 + idx3;
                    int oidx1 = idx6 + idx1;
                    int oidx2 = idx6 + idx4;
                    t1r = in[iidx1 - 1] - in[iidx2 - 1];
                    t1i = in[iidx1] + in[iidx2];
                    float i1i = in[iidx1];
                    float i1r = in[iidx1 - 1];
                    float i2i = in[iidx2];
                    float i2r = in[iidx2 - 1];

                    out[oidx1 - 1] = i1r + i2r;
                    out[oidx1] = i1i - i2i;
                    out[oidx2 - 1] = w1r * t1r - w1i * t1i;
                    out[oidx2] = w1r * t1i + w1i * t1r;
                }
            }
            if (ido % 2 == 1)
                return;
        }
        for (int k = 0; k < l1; k++) {
            int idx1 = k * ido;
            int idx2 = 2 * idx1;
            int oidx1 = out_off + ido - 1 + idx1;
            int iidx1 = in_off + idx2 + ido;
            out[oidx1] = 2 * in[iidx1 - 1];
            out[oidx1 + idx0] = -2 * in[iidx1];
        }
    }

    /*-------------------------------------------------
       radf3: Real FFT's forward processing of factor 3 
      -------------------------------------------------*/
    void radf3(final int ido, final int l1, final float in[], final int in_off, final float out[], final int out_off, final int offset) {
        final float taur = -0.5f;
        final float taui = 0.866025403784438707610604524234076962f;
        int i, ic;
        float ci2, di2, di3, cr2, dr2, dr3, ti2, ti3, tr2, tr3, w1r, w2r, w1i, w2i;
        int iw1, iw2;
        iw1 = offset;
        iw2 = iw1 + ido;

        int idx0 = l1 * ido;
        for (int k = 0; k < l1; k++) {
            int idx1 = k * ido;
            int idx3 = 2 * idx0;
            int idx4 = (3 * k + 1) * ido;
            int iidx1 = in_off + idx1;
            int iidx2 = iidx1 + idx0;
            int iidx3 = iidx1 + idx3;
            float i1r = in[iidx1];
            float i2r = in[iidx2];
            float i3r = in[iidx3];
            cr2 = i2r + i3r;
            out[out_off + 3 * idx1] = i1r + cr2;
            out[out_off + idx4 + ido] = taui * (i3r - i2r);
            out[out_off + ido - 1 + idx4] = i1r + taur * cr2;
        }
        if (ido == 1)
            return;
        for (int k = 0; k < l1; k++) {
            int idx3 = k * ido;
            int idx4 = 3 * idx3;
            int idx5 = idx3 + idx0;
            int idx6 = idx5 + idx0;
            int idx7 = idx4 + ido;
            int idx8 = idx7 + ido;
            for (i = 2; i < ido; i += 2) {
                ic = ido - i;
                int widx1 = i - 1 + iw1;
                int widx2 = i - 1 + iw2;

                w1r = wtable_r[widx1 - 1];
                w1i = wtable_r[widx1];
                w2r = wtable_r[widx2 - 1];
                w2i = wtable_r[widx2];

                int idx9 = in_off + i;
                int idx10 = out_off + i;
                int idx11 = out_off + ic;
                int iidx1 = idx9 + idx3;
                int iidx2 = idx9 + idx5;
                int iidx3 = idx9 + idx6;

                float i1i = in[iidx1 - 1];
                float i1r = in[iidx1];
                float i2i = in[iidx2 - 1];
                float i2r = in[iidx2];
                float i3i = in[iidx3 - 1];
                float i3r = in[iidx3];

                dr2 = w1r * i2i + w1i * i2r;
                di2 = w1r * i2r - w1i * i2i;
                dr3 = w2r * i3i + w2i * i3r;
                di3 = w2r * i3r - w2i * i3i;
                cr2 = dr2 + dr3;
                ci2 = di2 + di3;
                tr2 = i1i + taur * cr2;
                ti2 = i1r + taur * ci2;
                tr3 = taui * (di2 - di3);
                ti3 = taui * (dr3 - dr2);

                int oidx1 = idx10 + idx4;
                int oidx2 = idx11 + idx7;
                int oidx3 = idx10 + idx8;

                out[oidx1 - 1] = i1i + cr2;
                out[oidx1] = i1r + ci2;
                out[oidx2 - 1] = tr2 - tr3;
                out[oidx2] = ti3 - ti2;
                out[oidx3 - 1] = tr2 + tr3;
                out[oidx3] = ti2 + ti3;
            }
        }
    }

    /*-------------------------------------------------
       radb3: Real FFT's backward processing of factor 3
      -------------------------------------------------*/
    void radb3(final int ido, final int l1, final float in[], final int in_off, final float out[], final int out_off, final int offset) {
        final float taur = -0.5f;
        final float taui = 0.866025403784438707610604524234076962f;
        int i, ic;
        float ci2, ci3, di2, di3, cr2, cr3, dr2, dr3, ti2, tr2, w1r, w2r, w1i, w2i;
        int iw1, iw2;
        iw1 = offset;
        iw2 = iw1 + ido;

        for (int k = 0; k < l1; k++) {
            int idx1 = k * ido;
            int iidx1 = in_off + 3 * idx1;
            int iidx2 = iidx1 + 2 * ido;
            float i1i = in[iidx1];

            tr2 = 2 * in[iidx2 - 1];
            cr2 = i1i + taur * tr2;
            ci3 = 2 * taui * in[iidx2];

            out[out_off + idx1] = i1i + tr2;
            out[out_off + (k + l1) * ido] = cr2 - ci3;
            out[out_off + (k + 2 * l1) * ido] = cr2 + ci3;
        }
        if (ido == 1)
            return;
        int idx0 = l1 * ido;
        for (int k = 0; k < l1; k++) {
            int idx1 = k * ido;
            int idx2 = 3 * idx1;
            int idx3 = idx2 + ido;
            int idx4 = idx3 + ido;
            int idx5 = idx1 + idx0;
            int idx6 = idx5 + idx0;
            for (i = 2; i < ido; i += 2) {
                ic = ido - i;
                int idx7 = in_off + i;
                int idx8 = in_off + ic;
                int idx9 = out_off + i;
                int iidx1 = idx7 + idx2;
                int iidx2 = idx7 + idx4;
                int iidx3 = idx8 + idx3;

                float i1i = in[iidx1 - 1];
                float i1r = in[iidx1];
                float i2i = in[iidx2 - 1];
                float i2r = in[iidx2];
                float i3i = in[iidx3 - 1];
                float i3r = in[iidx3];

                tr2 = i2i + i3i;
                cr2 = i1i + taur * tr2;
                ti2 = i2r - i3r;
                ci2 = i1r + taur * ti2;
                cr3 = taui * (i2i - i3i);
                ci3 = taui * (i2r + i3r);
                dr2 = cr2 - ci3;
                dr3 = cr2 + ci3;
                di2 = ci2 + cr3;
                di3 = ci2 - cr3;

                int widx1 = i - 1 + iw1;
                int widx2 = i - 1 + iw2;

                w1r = wtable_r[widx1 - 1];
                w1i = wtable_r[widx1];
                w2r = wtable_r[widx2 - 1];
                w2i = wtable_r[widx2];

                int oidx1 = idx9 + idx1;
                int oidx2 = idx9 + idx5;
                int oidx3 = idx9 + idx6;

                out[oidx1 - 1] = i1i + tr2;
                out[oidx1] = i1r + ti2;
                out[oidx2 - 1] = w1r * dr2 - w1i * di2;
                out[oidx2] = w1r * di2 + w1i * dr2;
                out[oidx3 - 1] = w2r * dr3 - w2i * di3;
                out[oidx3] = w2r * di3 + w2i * dr3;
            }
        }
    }

    /*-------------------------------------------------
       radf4: Real FFT's forward processing of factor 4
      -------------------------------------------------*/
    void radf4(final int ido, final int l1, final float in[], final int in_off, final float out[], final int out_off, final int offset) {
        final float hsqt2 = 0.707106781186547572737310929369414225f;
        int i, ic;
        float ci2, ci3, ci4, cr2, cr3, cr4, ti1, ti2, ti3, ti4, tr1, tr2, tr3, tr4, w1r, w1i, w2r, w2i, w3r, w3i;
        int iw1, iw2, iw3;
        iw1 = offset;
        iw2 = offset + ido;
        iw3 = iw2 + ido;
        int idx0 = l1 * ido;
        for (int k = 0; k < l1; k++) {
            int idx1 = k * ido;
            int idx2 = 4 * idx1;
            int idx3 = idx1 + idx0;
            int idx4 = idx3 + idx0;
            int idx5 = idx4 + idx0;
            int idx6 = idx2 + ido;
            float i1r = in[in_off + idx1];
            float i2r = in[in_off + idx3];
            float i3r = in[in_off + idx4];
            float i4r = in[in_off + idx5];

            tr1 = i2r + i4r;
            tr2 = i1r + i3r;

            int oidx1 = out_off + idx2;
            int oidx2 = out_off + idx6 + ido;

            out[oidx1] = tr1 + tr2;
            out[oidx2 - 1 + ido + ido] = tr2 - tr1;
            out[oidx2 - 1] = i1r - i3r;
            out[oidx2] = i4r - i2r;
        }
        if (ido < 2)
            return;
        if (ido != 2) {
            for (int k = 0; k < l1; k++) {
                int idx1 = k * ido;
                int idx2 = idx1 + idx0;
                int idx3 = idx2 + idx0;
                int idx4 = idx3 + idx0;
                int idx5 = 4 * idx1;
                int idx6 = idx5 + ido;
                int idx7 = idx6 + ido;
                int idx8 = idx7 + ido;
                for (i = 2; i < ido; i += 2) {
                    ic = ido - i;
                    int widx1 = i - 1 + iw1;
                    int widx2 = i - 1 + iw2;
                    int widx3 = i - 1 + iw3;
                    w1r = wtable_r[widx1 - 1];
                    w1i = wtable_r[widx1];
                    w2r = wtable_r[widx2 - 1];
                    w2i = wtable_r[widx2];
                    w3r = wtable_r[widx3 - 1];
                    w3i = wtable_r[widx3];

                    int idx9 = in_off + i;
                    int idx10 = out_off + i;
                    int idx11 = out_off + ic;
                    int iidx1 = idx9 + idx1;
                    int iidx2 = idx9 + idx2;
                    int iidx3 = idx9 + idx3;
                    int iidx4 = idx9 + idx4;

                    float i1i = in[iidx1 - 1];
                    float i1r = in[iidx1];
                    float i2i = in[iidx2 - 1];
                    float i2r = in[iidx2];
                    float i3i = in[iidx3 - 1];
                    float i3r = in[iidx3];
                    float i4i = in[iidx4 - 1];
                    float i4r = in[iidx4];

                    cr2 = w1r * i2i + w1i * i2r;
                    ci2 = w1r * i2r - w1i * i2i;
                    cr3 = w2r * i3i + w2i * i3r;
                    ci3 = w2r * i3r - w2i * i3i;
                    cr4 = w3r * i4i + w3i * i4r;
                    ci4 = w3r * i4r - w3i * i4i;
                    tr1 = cr2 + cr4;
                    tr4 = cr4 - cr2;
                    ti1 = ci2 + ci4;
                    ti4 = ci2 - ci4;
                    ti2 = i1r + ci3;
                    ti3 = i1r - ci3;
                    tr2 = i1i + cr3;
                    tr3 = i1i - cr3;

                    int oidx1 = idx10 + idx5;
                    int oidx2 = idx11 + idx6;
                    int oidx3 = idx10 + idx7;
                    int oidx4 = idx11 + idx8;

                    out[oidx1 - 1] = tr1 + tr2;
                    out[oidx4 - 1] = tr2 - tr1;
                    out[oidx1] = ti1 + ti2;
                    out[oidx4] = ti1 - ti2;
                    out[oidx3 - 1] = ti4 + tr3;
                    out[oidx2 - 1] = tr3 - ti4;
                    out[oidx3] = tr4 + ti3;
                    out[oidx2] = tr4 - ti3;
                }
            }
            if (ido % 2 == 1)
                return;
        }
        for (int k = 0; k < l1; k++) {
            int idx1 = k * ido;
            int idx2 = 4 * idx1;
            int idx3 = idx1 + idx0;
            int idx4 = idx3 + idx0;
            int idx5 = idx4 + idx0;
            int idx6 = idx2 + ido;
            int idx7 = idx6 + ido;
            int idx8 = idx7 + ido;
            int idx9 = in_off + ido;
            int idx10 = out_off + ido;

            float i1i = in[idx9 - 1 + idx1];
            float i2i = in[idx9 - 1 + idx3];
            float i3i = in[idx9 - 1 + idx4];
            float i4i = in[idx9 - 1 + idx5];

            ti1 = -hsqt2 * (i2i + i4i);
            tr1 = hsqt2 * (i2i - i4i);

            out[idx10 - 1 + idx2] = tr1 + i1i;
            out[idx10 - 1 + idx7] = i1i - tr1;
            out[out_off + idx6] = ti1 - i3i;
            out[out_off + idx8] = ti1 + i3i;
        }
    }

    /*-------------------------------------------------
       radb4: Real FFT's backward processing of factor 4
      -------------------------------------------------*/
    void radb4(final int ido, final int l1, final float in[], final int in_off, final float out[], final int out_off, final int offset) {
        final float sqrt2 = 1.41421356237309514547462185873882845f;
        int i, ic;
        float ci2, ci3, ci4, cr2, cr3, cr4;
        float ti1, ti2, ti3, ti4, tr1, tr2, tr3, tr4, w1r, w1i, w2r, w2i, w3r, w3i;
        int iw1, iw2, iw3;
        iw1 = offset;
        iw2 = iw1 + ido;
        iw3 = iw2 + ido;

        int idx0 = l1 * ido;
        for (int k = 0; k < l1; k++) {
            int idx1 = k * ido;
            int idx2 = 4 * idx1;
            int idx3 = idx1 + idx0;
            int idx4 = idx3 + idx0;
            int idx5 = idx4 + idx0;
            int idx6 = idx2 + ido;
            int idx7 = idx6 + ido;
            int idx8 = idx7 + ido;

            float i1r = in[in_off + idx2];
            float i2r = in[in_off + idx7];
            float i3r = in[in_off + ido - 1 + idx8];
            float i4r = in[in_off + ido - 1 + idx6];

            tr1 = i1r - i3r;
            tr2 = i1r + i3r;
            tr3 = i4r + i4r;
            tr4 = i2r + i2r;

            out[out_off + idx1] = tr2 + tr3;
            out[out_off + idx3] = tr1 - tr4;
            out[out_off + idx4] = tr2 - tr3;
            out[out_off + idx5] = tr1 + tr4;
        }
        if (ido < 2)
            return;
        if (ido != 2) {
            for (int k = 0; k < l1; ++k) {
                int idx1 = k * ido;
                int idx2 = idx1 + idx0;
                int idx3 = idx2 + idx0;
                int idx4 = idx3 + idx0;
                int idx5 = 4 * idx1;
                int idx6 = idx5 + ido;
                int idx7 = idx6 + ido;
                int idx8 = idx7 + ido;
                for (i = 2; i < ido; i += 2) {
                    ic = ido - i;
                    int widx1 = i - 1 + iw1;
                    int widx2 = i - 1 + iw2;
                    int widx3 = i - 1 + iw3;
                    w1r = wtable_r[widx1 - 1];
                    w1i = wtable_r[widx1];
                    w2r = wtable_r[widx2 - 1];
                    w2i = wtable_r[widx2];
                    w3r = wtable_r[widx3 - 1];
                    w3i = wtable_r[widx3];

                    int idx12 = in_off + i;
                    int idx13 = in_off + ic;
                    int idx14 = out_off + i;

                    int iidx1 = idx12 + idx5;
                    int iidx2 = idx13 + idx6;
                    int iidx3 = idx12 + idx7;
                    int iidx4 = idx13 + idx8;

                    float i1i = in[iidx1 - 1];
                    float i1r = in[iidx1];
                    float i2i = in[iidx2 - 1];
                    float i2r = in[iidx2];
                    float i3i = in[iidx3 - 1];
                    float i3r = in[iidx3];
                    float i4i = in[iidx4 - 1];
                    float i4r = in[iidx4];

                    ti1 = i1r + i4r;
                    ti2 = i1r - i4r;
                    ti3 = i3r - i2r;
                    tr4 = i3r + i2r;
                    tr1 = i1i - i4i;
                    tr2 = i1i + i4i;
                    ti4 = i3i - i2i;
                    tr3 = i3i + i2i;
                    cr3 = tr2 - tr3;
                    ci3 = ti2 - ti3;
                    cr2 = tr1 - tr4;
                    cr4 = tr1 + tr4;
                    ci2 = ti1 + ti4;
                    ci4 = ti1 - ti4;

                    int oidx1 = idx14 + idx1;
                    int oidx2 = idx14 + idx2;
                    int oidx3 = idx14 + idx3;
                    int oidx4 = idx14 + idx4;

                    out[oidx1 - 1] = tr2 + tr3;
                    out[oidx1] = ti2 + ti3;
                    out[oidx2 - 1] = w1r * cr2 - w1i * ci2;
                    out[oidx2] = w1r * ci2 + w1i * cr2;
                    out[oidx3 - 1] = w2r * cr3 - w2i * ci3;
                    out[oidx3] = w2r * ci3 + w2i * cr3;
                    out[oidx4 - 1] = w3r * cr4 - w3i * ci4;
                    out[oidx4] = w3r * ci4 + w3i * cr4;
                }
            }
            if (ido % 2 == 1)
                return;
        }
        for (int k = 0; k < l1; k++) {
            int idx1 = k * ido;
            int idx2 = 4 * idx1;
            int idx3 = idx1 + idx0;
            int idx4 = idx3 + idx0;
            int idx5 = idx4 + idx0;
            int idx6 = idx2 + ido;
            int idx7 = idx6 + ido;
            int idx8 = idx7 + ido;
            int idx9 = in_off + ido;
            int idx10 = out_off + ido;

            float i1r = in[idx9 - 1 + idx2];
            float i2r = in[idx9 - 1 + idx7];
            float i3r = in[in_off + idx6];
            float i4r = in[in_off + idx8];

            ti1 = i3r + i4r;
            ti2 = i4r - i3r;
            tr1 = i1r - i2r;
            tr2 = i1r + i2r;

            out[idx10 - 1 + idx1] = tr2 + tr2;
            out[idx10 - 1 + idx3] = sqrt2 * (tr1 - ti1);
            out[idx10 - 1 + idx4] = ti2 + ti2;
            out[idx10 - 1 + idx5] = -sqrt2 * (tr1 + ti1);
        }
    }

    /*-------------------------------------------------
       radf5: Real FFT's forward processing of factor 5
      -------------------------------------------------*/
    void radf5(final int ido, final int l1, final float in[], final int in_off, final float out[], final int out_off, final int offset) {
        final float tr11 = 0.309016994374947451262869435595348477f;
        final float ti11 = 0.951056516295153531181938433292089030f;
        final float tr12 = -0.809016994374947340240566973079694435f;
        final float ti12 = 0.587785252292473248125759255344746634f;
        int i, ic;
        float ci2, di2, ci4, ci5, di3, di4, di5, ci3, cr2, cr3, dr2, dr3, dr4, dr5, cr5, cr4, ti2, ti3, ti5, ti4, tr2, tr3, tr4, tr5, w1r, w1i, w2r, w2i, w3r, w3i, w4r, w4i;
        int iw1, iw2, iw3, iw4;
        iw1 = offset;
        iw2 = iw1 + ido;
        iw3 = iw2 + ido;
        iw4 = iw3 + ido;

        int idx0 = l1 * ido;
        for (int k = 0; k < l1; k++) {
            int idx1 = k * ido;
            int idx2 = 5 * idx1;
            int idx3 = idx2 + ido;
            int idx4 = idx3 + ido;
            int idx5 = idx4 + ido;
            int idx6 = idx5 + ido;
            int idx7 = idx1 + idx0;
            int idx8 = idx7 + idx0;
            int idx9 = idx8 + idx0;
            int idx10 = idx9 + idx0;
            int idx11 = out_off + ido - 1;

            float i1r = in[in_off + idx1];
            float i2r = in[in_off + idx7];
            float i3r = in[in_off + idx8];
            float i4r = in[in_off + idx9];
            float i5r = in[in_off + idx10];

            cr2 = i5r + i2r;
            ci5 = i5r - i2r;
            cr3 = i4r + i3r;
            ci4 = i4r - i3r;

            out[out_off + idx2] = i1r + cr2 + cr3;
            out[idx11 + idx3] = i1r + tr11 * cr2 + tr12 * cr3;
            out[out_off + idx4] = ti11 * ci5 + ti12 * ci4;
            out[idx11 + idx5] = i1r + tr12 * cr2 + tr11 * cr3;
            out[out_off + idx6] = ti12 * ci5 - ti11 * ci4;
        }
        if (ido == 1)
            return;
        for (int k = 0; k < l1; ++k) {
            int idx1 = k * ido;
            int idx2 = 5 * idx1;
            int idx3 = idx2 + ido;
            int idx4 = idx3 + ido;
            int idx5 = idx4 + ido;
            int idx6 = idx5 + ido;
            int idx7 = idx1 + idx0;
            int idx8 = idx7 + idx0;
            int idx9 = idx8 + idx0;
            int idx10 = idx9 + idx0;
            for (i = 2; i < ido; i += 2) {
                int widx1 = i - 1 + iw1;
                int widx2 = i - 1 + iw2;
                int widx3 = i - 1 + iw3;
                int widx4 = i - 1 + iw4;
                w1r = wtable_r[widx1 - 1];
                w1i = wtable_r[widx1];
                w2r = wtable_r[widx2 - 1];
                w2i = wtable_r[widx2];
                w3r = wtable_r[widx3 - 1];
                w3i = wtable_r[widx3];
                w4r = wtable_r[widx4 - 1];
                w4i = wtable_r[widx4];

                ic = ido - i;
                int idx15 = in_off + i;
                int idx16 = out_off + i;
                int idx17 = out_off + ic;

                int iidx1 = idx15 + idx1;
                int iidx2 = idx15 + idx7;
                int iidx3 = idx15 + idx8;
                int iidx4 = idx15 + idx9;
                int iidx5 = idx15 + idx10;

                float i1i = in[iidx1 - 1];
                float i1r = in[iidx1];
                float i2i = in[iidx2 - 1];
                float i2r = in[iidx2];
                float i3i = in[iidx3 - 1];
                float i3r = in[iidx3];
                float i4i = in[iidx4 - 1];
                float i4r = in[iidx4];
                float i5i = in[iidx5 - 1];
                float i5r = in[iidx5];

                dr2 = w1r * i2i + w1i * i2r;
                di2 = w1r * i2r - w1i * i2i;
                dr3 = w2r * i3i + w2i * i3r;
                di3 = w2r * i3r - w2i * i3i;
                dr4 = w3r * i4i + w3i * i4r;
                di4 = w3r * i4r - w3i * i4i;
                dr5 = w4r * i5i + w4i * i5r;
                di5 = w4r * i5r - w4i * i5i;

                cr2 = dr2 + dr5;
                ci5 = dr5 - dr2;
                cr5 = di2 - di5;
                ci2 = di2 + di5;
                cr3 = dr3 + dr4;
                ci4 = dr4 - dr3;
                cr4 = di3 - di4;
                ci3 = di3 + di4;

                tr2 = i1i + tr11 * cr2 + tr12 * cr3;
                ti2 = i1r + tr11 * ci2 + tr12 * ci3;
                tr3 = i1i + tr12 * cr2 + tr11 * cr3;
                ti3 = i1r + tr12 * ci2 + tr11 * ci3;
                tr5 = ti11 * cr5 + ti12 * cr4;
                ti5 = ti11 * ci5 + ti12 * ci4;
                tr4 = ti12 * cr5 - ti11 * cr4;
                ti4 = ti12 * ci5 - ti11 * ci4;

                int oidx1 = idx16 + idx2;
                int oidx2 = idx17 + idx3;
                int oidx3 = idx16 + idx4;
                int oidx4 = idx17 + idx5;
                int oidx5 = idx16 + idx6;

                out[oidx1 - 1] = i1i + cr2 + cr3;
                out[oidx1] = i1r + ci2 + ci3;
                out[oidx3 - 1] = tr2 + tr5;
                out[oidx2 - 1] = tr2 - tr5;
                out[oidx3] = ti2 + ti5;
                out[oidx2] = ti5 - ti2;
                out[oidx5 - 1] = tr3 + tr4;
                out[oidx4 - 1] = tr3 - tr4;
                out[oidx5] = ti3 + ti4;
                out[oidx4] = ti4 - ti3;
            }
        }
    }

    /*-------------------------------------------------
       radb5: Real FFT's backward processing of factor 5
      -------------------------------------------------*/
    void radb5(final int ido, final int l1, final float in[], final int in_off, final float out[], final int out_off, final int offset) {
        final float tr11 = 0.309016994374947451262869435595348477f;
        final float ti11 = 0.951056516295153531181938433292089030f;
        final float tr12 = -0.809016994374947340240566973079694435f;
        final float ti12 = 0.587785252292473248125759255344746634f;
        int i, ic;
        float ci2, ci3, ci4, ci5, di3, di4, di5, di2, cr2, cr3, cr5, cr4, ti2, ti3, ti4, ti5, dr3, dr4, dr5, dr2, tr2, tr3, tr4, tr5, w1r, w1i, w2r, w2i, w3r, w3i, w4r, w4i;
        int iw1, iw2, iw3, iw4;
        iw1 = offset;
        iw2 = iw1 + ido;
        iw3 = iw2 + ido;
        iw4 = iw3 + ido;

        int idx0 = l1 * ido;
        for (int k = 0; k < l1; k++) {
            int idx1 = k * ido;
            int idx2 = 5 * idx1;
            int idx3 = idx2 + ido;
            int idx4 = idx3 + ido;
            int idx5 = idx4 + ido;
            int idx6 = idx5 + ido;
            int idx7 = idx1 + idx0;
            int idx8 = idx7 + idx0;
            int idx9 = idx8 + idx0;
            int idx10 = idx9 + idx0;
            int idx11 = in_off + ido - 1;

            float i1r = in[in_off + idx2];

            ti5 = 2 * in[in_off + idx4];
            ti4 = 2 * in[in_off + idx6];
            tr2 = 2 * in[idx11 + idx3];
            tr3 = 2 * in[idx11 + idx5];
            cr2 = i1r + tr11 * tr2 + tr12 * tr3;
            cr3 = i1r + tr12 * tr2 + tr11 * tr3;
            ci5 = ti11 * ti5 + ti12 * ti4;
            ci4 = ti12 * ti5 - ti11 * ti4;

            out[out_off + idx1] = i1r + tr2 + tr3;
            out[out_off + idx7] = cr2 - ci5;
            out[out_off + idx8] = cr3 - ci4;
            out[out_off + idx9] = cr3 + ci4;
            out[out_off + idx10] = cr2 + ci5;
        }
        if (ido == 1)
            return;
        for (int k = 0; k < l1; ++k) {
            int idx1 = k * ido;
            int idx2 = 5 * idx1;
            int idx3 = idx2 + ido;
            int idx4 = idx3 + ido;
            int idx5 = idx4 + ido;
            int idx6 = idx5 + ido;
            int idx7 = idx1 + idx0;
            int idx8 = idx7 + idx0;
            int idx9 = idx8 + idx0;
            int idx10 = idx9 + idx0;
            for (i = 2; i < ido; i += 2) {
                ic = ido - i;
                int widx1 = i - 1 + iw1;
                int widx2 = i - 1 + iw2;
                int widx3 = i - 1 + iw3;
                int widx4 = i - 1 + iw4;
                w1r = wtable_r[widx1 - 1];
                w1i = wtable_r[widx1];
                w2r = wtable_r[widx2 - 1];
                w2i = wtable_r[widx2];
                w3r = wtable_r[widx3 - 1];
                w3i = wtable_r[widx3];
                w4r = wtable_r[widx4 - 1];
                w4i = wtable_r[widx4];

                int idx15 = in_off + i;
                int idx16 = in_off + ic;
                int idx17 = out_off + i;

                int iidx1 = idx15 + idx2;
                int iidx2 = idx16 + idx3;
                int iidx3 = idx15 + idx4;
                int iidx4 = idx16 + idx5;
                int iidx5 = idx15 + idx6;

                float i1i = in[iidx1 - 1];
                float i1r = in[iidx1];
                float i2i = in[iidx2 - 1];
                float i2r = in[iidx2];
                float i3i = in[iidx3 - 1];
                float i3r = in[iidx3];
                float i4i = in[iidx4 - 1];
                float i4r = in[iidx4];
                float i5i = in[iidx5 - 1];
                float i5r = in[iidx5];

                ti5 = i3r + i2r;
                ti2 = i3r - i2r;
                ti4 = i5r + i4r;
                ti3 = i5r - i4r;
                tr5 = i3i - i2i;
                tr2 = i3i + i2i;
                tr4 = i5i - i4i;
                tr3 = i5i + i4i;

                cr2 = i1i + tr11 * tr2 + tr12 * tr3;
                ci2 = i1r + tr11 * ti2 + tr12 * ti3;
                cr3 = i1i + tr12 * tr2 + tr11 * tr3;
                ci3 = i1r + tr12 * ti2 + tr11 * ti3;
                cr5 = ti11 * tr5 + ti12 * tr4;
                ci5 = ti11 * ti5 + ti12 * ti4;
                cr4 = ti12 * tr5 - ti11 * tr4;
                ci4 = ti12 * ti5 - ti11 * ti4;
                dr3 = cr3 - ci4;
                dr4 = cr3 + ci4;
                di3 = ci3 + cr4;
                di4 = ci3 - cr4;
                dr5 = cr2 + ci5;
                dr2 = cr2 - ci5;
                di5 = ci2 - cr5;
                di2 = ci2 + cr5;

                int oidx1 = idx17 + idx1;
                int oidx2 = idx17 + idx7;
                int oidx3 = idx17 + idx8;
                int oidx4 = idx17 + idx9;
                int oidx5 = idx17 + idx10;

                out[oidx1 - 1] = i1i + tr2 + tr3;
                out[oidx1] = i1r + ti2 + ti3;
                out[oidx2 - 1] = w1r * dr2 - w1i * di2;
                out[oidx2] = w1r * di2 + w1i * dr2;
                out[oidx3 - 1] = w2r * dr3 - w2i * di3;
                out[oidx3] = w2r * di3 + w2i * dr3;
                out[oidx4 - 1] = w3r * dr4 - w3i * di4;
                out[oidx4] = w3r * di4 + w3i * dr4;
                out[oidx5 - 1] = w4r * dr5 - w4i * di5;
                out[oidx5] = w4r * di5 + w4i * dr5;
            }
        }
    }

    /*---------------------------------------------------------
       radfg: Real FFT's forward processing of general factor
      --------------------------------------------------------*/
    void radfg(final int ido, final int ip, final int l1, final int idl1, final float in[], final int in_off, final float out[], final int out_off, final int offset) {
        int idij, ipph, j2, ic, jc, lc, is, nbd;
        float dc2, ai1, ai2, ar1, ar2, ds2, dcp, arg, dsp, ar1h, ar2h, w1r, w1i;
        int iw1 = offset;

        arg = TWO_PI / (float) ip;
        dcp = (float)Math.cos(arg);
        dsp = (float)Math.sin(arg);
        ipph = (ip + 1) / 2;
        nbd = (ido - 1) / 2;
        if (ido != 1) {
            for (int ik = 0; ik < idl1; ik++)
                out[out_off + ik] = in[in_off + ik];
            for (int j = 1; j < ip; j++) {
                int idx1 = j * l1 * ido;
                for (int k = 0; k < l1; k++) {
                    int idx2 = k * ido + idx1;
                    out[out_off + idx2] = in[in_off + idx2];
                }
            }
            if (nbd <= l1) {
                is = -ido;
                for (int j = 1; j < ip; j++) {
                    is += ido;
                    idij = is - 1;
                    int idx1 = j * l1 * ido;
                    for (int i = 2; i < ido; i += 2) {
                        idij += 2;
                        int idx2 = idij + iw1;
                        int idx4 = in_off + i;
                        int idx5 = out_off + i;
                        w1r = wtable_r[idx2 - 1];
                        w1i = wtable_r[idx2];
                        for (int k = 0; k < l1; k++) {
                            int idx3 = k * ido + idx1;
                            int oidx1 = idx5 + idx3;
                            int iidx1 = idx4 + idx3;
                            float i1i = in[iidx1 - 1];
                            float i1r = in[iidx1];

                            out[oidx1 - 1] = w1r * i1i + w1i * i1r;
                            out[oidx1] = w1r * i1r - w1i * i1i;
                        }
                    }
                }
            } else {
                is = -ido;
                for (int j = 1; j < ip; j++) {
                    is += ido;
                    int idx1 = j * l1 * ido;
                    for (int k = 0; k < l1; k++) {
                        idij = is - 1;
                        int idx3 = k * ido + idx1;
                        for (int i = 2; i < ido; i += 2) {
                            idij += 2;
                            int idx2 = idij + iw1;
                            w1r = wtable_r[idx2 - 1];
                            w1i = wtable_r[idx2];
                            int oidx1 = out_off + i + idx3;
                            int iidx1 = in_off + i + idx3;
                            float i1i = in[iidx1 - 1];
                            float i1r = in[iidx1];

                            out[oidx1 - 1] = w1r * i1i + w1i * i1r;
                            out[oidx1] = w1r * i1r - w1i * i1i;
                        }
                    }
                }
            }
            if (nbd >= l1) {
                for (int j = 1; j < ipph; j++) {
                    jc = ip - j;
                    int idx1 = j * l1 * ido;
                    int idx2 = jc * l1 * ido;
                    for (int k = 0; k < l1; k++) {
                        int idx3 = k * ido + idx1;
                        int idx4 = k * ido + idx2;
                        for (int i = 2; i < ido; i += 2) {
                            int idx5 = in_off + i;
                            int idx6 = out_off + i;
                            int iidx1 = idx5 + idx3;
                            int iidx2 = idx5 + idx4;
                            int oidx1 = idx6 + idx3;
                            int oidx2 = idx6 + idx4;
                            float o1i = out[oidx1 - 1];
                            float o1r = out[oidx1];
                            float o2i = out[oidx2 - 1];
                            float o2r = out[oidx2];

                            in[iidx1 - 1] = o1i + o2i;
                            in[iidx1] = o1r + o2r;

                            in[iidx2 - 1] = o1r - o2r;
                            in[iidx2] = o2i - o1i;
                        }
                    }
                }
            } else {
                for (int j = 1; j < ipph; j++) {
                    jc = ip - j;
                    int idx1 = j * l1 * ido;
                    int idx2 = jc * l1 * ido;
                    for (int i = 2; i < ido; i += 2) {
                        int idx5 = in_off + i;
                        int idx6 = out_off + i;
                        for (int k = 0; k < l1; k++) {
                            int idx3 = k * ido + idx1;
                            int idx4 = k * ido + idx2;
                            int iidx1 = idx5 + idx3;
                            int iidx2 = idx5 + idx4;
                            int oidx1 = idx6 + idx3;
                            int oidx2 = idx6 + idx4;
                            float o1i = out[oidx1 - 1];
                            float o1r = out[oidx1];
                            float o2i = out[oidx2 - 1];
                            float o2r = out[oidx2];

                            in[iidx1 - 1] = o1i + o2i;
                            in[iidx1] = o1r + o2r;
                            in[iidx2 - 1] = o1r - o2r;
                            in[iidx2] = o2i - o1i;
                        }
                    }
                }
            }
        } else {
            System.arraycopy(out, out_off, in, in_off, idl1);
        }
        for (int j = 1; j < ipph; j++) {
            jc = ip - j;
            int idx1 = j * l1 * ido;
            int idx2 = jc * l1 * ido;
            for (int k = 0; k < l1; k++) {
                int idx3 = k * ido + idx1;
                int idx4 = k * ido + idx2;
                int oidx1 = out_off + idx3;
                int oidx2 = out_off + idx4;
                float o1r = out[oidx1];
                float o2r = out[oidx2];

                in[in_off + idx3] = o1r + o2r;
                in[in_off + idx4] = o2r - o1r;
            }
        }

        ar1 = 1;
        ai1 = 0;
        int idx0 = (ip - 1) * idl1;
        for (int l = 1; l < ipph; l++) {
            lc = ip - l;
            ar1h = dcp * ar1 - dsp * ai1;
            ai1 = dcp * ai1 + dsp * ar1;
            ar1 = ar1h;
            int idx1 = l * idl1;
            int idx2 = lc * idl1;
            for (int ik = 0; ik < idl1; ik++) {
                int idx3 = out_off + ik;
                int idx4 = in_off + ik;
                out[idx3 + idx1] = in[idx4] + ar1 * in[idx4 + idl1];
                out[idx3 + idx2] = ai1 * in[idx4 + idx0];
            }
            dc2 = ar1;
            ds2 = ai1;
            ar2 = ar1;
            ai2 = ai1;
            for (int j = 2; j < ipph; j++) {
                jc = ip - j;
                ar2h = dc2 * ar2 - ds2 * ai2;
                ai2 = dc2 * ai2 + ds2 * ar2;
                ar2 = ar2h;
                int idx3 = j * idl1;
                int idx4 = jc * idl1;
                for (int ik = 0; ik < idl1; ik++) {
                    int idx5 = out_off + ik;
                    int idx6 = in_off + ik;
                    out[idx5 + idx1] += ar2 * in[idx6 + idx3];
                    out[idx5 + idx2] += ai2 * in[idx6 + idx4];
                }
            }
        }
        for (int j = 1; j < ipph; j++) {
            int idx1 = j * idl1;
            for (int ik = 0; ik < idl1; ik++) {
                out[out_off + ik] += in[in_off + ik + idx1];
            }
        }

        if (ido >= l1) {
            for (int k = 0; k < l1; k++) {
                int idx1 = k * ido;
                int idx2 = idx1 * ip;
                for (int i = 0; i < ido; i++) {
                    in[in_off + i + idx2] = out[out_off + i + idx1];
                }
            }
        } else {
            for (int i = 0; i < ido; i++) {
                for (int k = 0; k < l1; k++) {
                    int idx1 = k * ido;
                    in[in_off + i + idx1 * ip] = out[out_off + i + idx1];
                }
            }
        }
        int idx01 = ip * ido;
        for (int j = 1; j < ipph; j++) {
            jc = ip - j;
            j2 = 2 * j;
            int idx1 = j * l1 * ido;
            int idx2 = jc * l1 * ido;
            int idx3 = j2 * ido;
            for (int k = 0; k < l1; k++) {
                int idx4 = k * ido;
                int idx5 = idx4 + idx1;
                int idx6 = idx4 + idx2;
                int idx7 = k * idx01;
                in[in_off + ido - 1 + idx3 - ido + idx7] = out[out_off + idx5];
                in[in_off + idx3 + idx7] = out[out_off + idx6];
            }
        }
        if (ido == 1)
            return;
        if (nbd >= l1) {
            for (int j = 1; j < ipph; j++) {
                jc = ip - j;
                j2 = 2 * j;
                int idx1 = j * l1 * ido;
                int idx2 = jc * l1 * ido;
                int idx3 = j2 * ido;
                for (int k = 0; k < l1; k++) {
                    int idx4 = k * idx01;
                    int idx5 = k * ido;
                    for (int i = 2; i < ido; i += 2) {
                        ic = ido - i;
                        int idx6 = in_off + i;
                        int idx7 = in_off + ic;
                        int idx8 = out_off + i;
                        int iidx1 = idx6 + idx3 + idx4;
                        int iidx2 = idx7 + idx3 - ido + idx4;
                        int oidx1 = idx8 + idx5 + idx1;
                        int oidx2 = idx8 + idx5 + idx2;
                        float o1i = out[oidx1 - 1];
                        float o1r = out[oidx1];
                        float o2i = out[oidx2 - 1];
                        float o2r = out[oidx2];

                        in[iidx1 - 1] = o1i + o2i;
                        in[iidx2 - 1] = o1i - o2i;
                        in[iidx1] = o1r + o2r;
                        in[iidx2] = o2r - o1r;
                    }
                }
            }
        } else {
            for (int j = 1; j < ipph; j++) {
                jc = ip - j;
                j2 = 2 * j;
                int idx1 = j * l1 * ido;
                int idx2 = jc * l1 * ido;
                int idx3 = j2 * ido;
                for (int i = 2; i < ido; i += 2) {
                    ic = ido - i;
                    int idx6 = in_off + i;
                    int idx7 = in_off + ic;
                    int idx8 = out_off + i;
                    for (int k = 0; k < l1; k++) {
                        int idx4 = k * idx01;
                        int idx5 = k * ido;
                        int iidx1 = idx6 + idx3 + idx4;
                        int iidx2 = idx7 + idx3 - ido + idx4;
                        int oidx1 = idx8 + idx5 + idx1;
                        int oidx2 = idx8 + idx5 + idx2;
                        float o1i = out[oidx1 - 1];
                        float o1r = out[oidx1];
                        float o2i = out[oidx2 - 1];
                        float o2r = out[oidx2];

                        in[iidx1 - 1] = o1i + o2i;
                        in[iidx2 - 1] = o1i - o2i;
                        in[iidx1] = o1r + o2r;
                        in[iidx2] = o2r - o1r;
                    }
                }
            }
        }
    }

    /*---------------------------------------------------------
       radbg: Real FFT's backward processing of general factor
      --------------------------------------------------------*/
    void radbg(final int ido, final int ip, final int l1, final int idl1, final float in[], final int in_off, final float out[], final int out_off, final int offset) {
        int idij, ipph, j2, ic, jc, lc, is;
        float dc2, ai1, ai2, ar1, ar2, ds2, w1r, w1i;
        int nbd;
        float dcp, arg, dsp, ar1h, ar2h;
        int iw1 = offset;

        arg = TWO_PI / (float) ip;
        dcp = (float)Math.cos(arg);
        dsp = (float)Math.sin(arg);
        nbd = (ido - 1) / 2;
        ipph = (ip + 1) / 2;
        int idx0 = ip * ido;
        if (ido >= l1) {
            for (int k = 0; k < l1; k++) {
                int idx1 = k * ido;
                int idx2 = k * idx0;
                for (int i = 0; i < ido; i++) {
                    out[out_off + i + idx1] = in[in_off + i + idx2];
                }
            }
        } else {
            for (int i = 0; i < ido; i++) {
                int idx1 = out_off + i;
                int idx2 = in_off + i;
                for (int k = 0; k < l1; k++) {
                    out[idx1 + k * ido] = in[idx2 + k * idx0];
                }
            }
        }
        int iidx0 = in_off + ido - 1;
        for (int j = 1; j < ipph; j++) {
            jc = ip - j;
            j2 = 2 * j;
            int idx1 = j * l1 * ido;
            int idx2 = jc * l1 * ido;
            int idx3 = j2 * ido;
            for (int k = 0; k < l1; k++) {
                int idx4 = k * ido;
                int idx5 = idx4 * ip;
                int iidx1 = iidx0 + idx3 + idx5 - ido;
                int iidx2 = in_off + idx3 + idx5;
                float i1r = in[iidx1];
                float i2r = in[iidx2];

                out[out_off + idx4 + idx1] = i1r + i1r;
                out[out_off + idx4 + idx2] = i2r + i2r;
            }
        }

        if (ido != 1) {
            if (nbd >= l1) {
                for (int j = 1; j < ipph; j++) {
                    jc = ip - j;
                    int idx1 = j * l1 * ido;
                    int idx2 = jc * l1 * ido;
                    int idx3 = 2 * j * ido;
                    for (int k = 0; k < l1; k++) {
                        int idx4 = k * ido + idx1;
                        int idx5 = k * ido + idx2;
                        int idx6 = k * ip * ido + idx3;
                        for (int i = 2; i < ido; i += 2) {
                            ic = ido - i;
                            int idx7 = out_off + i;
                            int idx8 = in_off + ic;
                            int idx9 = in_off + i;
                            int oidx1 = idx7 + idx4;
                            int oidx2 = idx7 + idx5;
                            int iidx1 = idx9 + idx6;
                            int iidx2 = idx8 + idx6 - ido;
                            float a1i = in[iidx1 - 1];
                            float a1r = in[iidx1];
                            float a2i = in[iidx2 - 1];
                            float a2r = in[iidx2];

                            out[oidx1 - 1] = a1i + a2i;
                            out[oidx2 - 1] = a1i - a2i;
                            out[oidx1] = a1r - a2r;
                            out[oidx2] = a1r + a2r;
                        }
                    }
                }
            } else {
                for (int j = 1; j < ipph; j++) {
                    jc = ip - j;
                    int idx1 = j * l1 * ido;
                    int idx2 = jc * l1 * ido;
                    int idx3 = 2 * j * ido;
                    for (int i = 2; i < ido; i += 2) {
                        ic = ido - i;
                        int idx7 = out_off + i;
                        int idx8 = in_off + ic;
                        int idx9 = in_off + i;
                        for (int k = 0; k < l1; k++) {
                            int idx4 = k * ido + idx1;
                            int idx5 = k * ido + idx2;
                            int idx6 = k * ip * ido + idx3;
                            int oidx1 = idx7 + idx4;
                            int oidx2 = idx7 + idx5;
                            int iidx1 = idx9 + idx6;
                            int iidx2 = idx8 + idx6 - ido;
                            float a1i = in[iidx1 - 1];
                            float a1r = in[iidx1];
                            float a2i = in[iidx2 - 1];
                            float a2r = in[iidx2];

                            out[oidx1 - 1] = a1i + a2i;
                            out[oidx2 - 1] = a1i - a2i;
                            out[oidx1] = a1r - a2r;
                            out[oidx2] = a1r + a2r;
                        }
                    }
                }
            }
        }

        ar1 = 1;
        ai1 = 0;
        int idx01 = (ip - 1) * idl1;
        for (int l = 1; l < ipph; l++) {
            lc = ip - l;
            ar1h = dcp * ar1 - dsp * ai1;
            ai1 = dcp * ai1 + dsp * ar1;
            ar1 = ar1h;
            int idx1 = l * idl1;
            int idx2 = lc * idl1;
            for (int ik = 0; ik < idl1; ik++) {
                int idx3 = in_off + ik;
                int idx4 = out_off + ik;
                in[idx3 + idx1] = out[idx4] + ar1 * out[idx4 + idl1];
                in[idx3 + idx2] = ai1 * out[idx4 + idx01];
            }
            dc2 = ar1;
            ds2 = ai1;
            ar2 = ar1;
            ai2 = ai1;
            for (int j = 2; j < ipph; j++) {
                jc = ip - j;
                ar2h = dc2 * ar2 - ds2 * ai2;
                ai2 = dc2 * ai2 + ds2 * ar2;
                ar2 = ar2h;
                int idx5 = j * idl1;
                int idx6 = jc * idl1;
                for (int ik = 0; ik < idl1; ik++) {
                    int idx7 = in_off + ik;
                    int idx8 = out_off + ik;
                    in[idx7 + idx1] += ar2 * out[idx8 + idx5];
                    in[idx7 + idx2] += ai2 * out[idx8 + idx6];
                }
            }
        }
        for (int j = 1; j < ipph; j++) {
            int idx1 = j * idl1;
            for (int ik = 0; ik < idl1; ik++) {
                int idx2 = out_off + ik;
                out[idx2] += out[idx2 + idx1];
            }
        }
        for (int j = 1; j < ipph; j++) {
            jc = ip - j;
            int idx1 = j * l1 * ido;
            int idx2 = jc * l1 * ido;
            for (int k = 0; k < l1; k++) {
                int idx3 = k * ido;
                int oidx1 = out_off + idx3;
                int iidx1 = in_off + idx3 + idx1;
                int iidx2 = in_off + idx3 + idx2;
                float i1r = in[iidx1];
                float i2r = in[iidx2];

                out[oidx1 + idx1] = i1r - i2r;
                out[oidx1 + idx2] = i1r + i2r;
            }
        }

        if (ido == 1)
            return;
        if (nbd >= l1) {
            for (int j = 1; j < ipph; j++) {
                jc = ip - j;
                int idx1 = j * l1 * ido;
                int idx2 = jc * l1 * ido;
                for (int k = 0; k < l1; k++) {
                    int idx3 = k * ido;
                    for (int i = 2; i < ido; i += 2) {
                        int idx4 = out_off + i;
                        int idx5 = in_off + i;
                        int oidx1 = idx4 + idx3 + idx1;
                        int oidx2 = idx4 + idx3 + idx2;
                        int iidx1 = idx5 + idx3 + idx1;
                        int iidx2 = idx5 + idx3 + idx2;
                        float i1i = in[iidx1 - 1];
                        float i1r = in[iidx1];
                        float i2i = in[iidx2 - 1];
                        float i2r = in[iidx2];

                        out[oidx1 - 1] = i1i - i2r;
                        out[oidx2 - 1] = i1i + i2r;
                        out[oidx1] = i1r + i2i;
                        out[oidx2] = i1r - i2i;
                    }
                }
            }
        } else {
            for (int j = 1; j < ipph; j++) {
                jc = ip - j;
                int idx1 = j * l1 * ido;
                int idx2 = jc * l1 * ido;
                for (int i = 2; i < ido; i += 2) {
                    int idx4 = out_off + i;
                    int idx5 = in_off + i;
                    for (int k = 0; k < l1; k++) {
                        int idx3 = k * ido;
                        int oidx1 = idx4 + idx3 + idx1;
                        int oidx2 = idx4 + idx3 + idx2;
                        int iidx1 = idx5 + idx3 + idx1;
                        int iidx2 = idx5 + idx3 + idx2;
                        float i1i = in[iidx1 - 1];
                        float i1r = in[iidx1];
                        float i2i = in[iidx2 - 1];
                        float i2r = in[iidx2];

                        out[oidx1 - 1] = i1i - i2r;
                        out[oidx2 - 1] = i1i + i2r;
                        out[oidx1] = i1r + i2i;
                        out[oidx2] = i1r - i2i;
                    }
                }
            }
        }
        System.arraycopy(out, out_off, in, in_off, idl1);
        for (int j = 1; j < ip; j++) {
            int idx1 = j * l1 * ido;
            for (int k = 0; k < l1; k++) {
                int idx2 = k * ido + idx1;
                in[in_off + idx2] = out[out_off + idx2];
            }
        }
        if (nbd <= l1) {
            is = -ido;
            for (int j = 1; j < ip; j++) {
                is += ido;
                idij = is - 1;
                int idx1 = j * l1 * ido;
                for (int i = 2; i < ido; i += 2) {
                    idij += 2;
                    int idx2 = idij + iw1;
                    w1r = wtable_r[idx2 - 1];
                    w1i = wtable_r[idx2];
                    int idx4 = in_off + i;
                    int idx5 = out_off + i;
                    for (int k = 0; k < l1; k++) {
                        int idx3 = k * ido + idx1;
                        int iidx1 = idx4 + idx3;
                        int oidx1 = idx5 + idx3;
                        float o1i = out[oidx1 - 1];
                        float o1r = out[oidx1];

                        in[iidx1 - 1] = w1r * o1i - w1i * o1r;
                        in[iidx1] = w1r * o1r + w1i * o1i;
                    }
                }
            }
        } else {
            is = -ido;
            for (int j = 1; j < ip; j++) {
                is += ido;
                int idx1 = j * l1 * ido;
                for (int k = 0; k < l1; k++) {
                    idij = is - 1;
                    int idx3 = k * ido + idx1;
                    for (int i = 2; i < ido; i += 2) {
                        idij += 2;
                        int idx2 = idij + iw1;
                        w1r = wtable_r[idx2 - 1];
                        w1i = wtable_r[idx2];
                        int idx4 = in_off + i;
                        int idx5 = out_off + i;
                        int iidx1 = idx4 + idx3;
                        int oidx1 = idx5 + idx3;
                        float o1i = out[oidx1 - 1];
                        float o1r = out[oidx1];

                        in[iidx1 - 1] = w1r * o1i - w1i * o1r;
                        in[iidx1] = w1r * o1r + w1i * o1i;

                    }
                }
            }
        }
    }

    /*---------------------------------------------------------
       cfftf1: further processing of Complex forward FFT
      --------------------------------------------------------*/
    void cfftf(float a[], int offa, int isign) {
        int idot;
        int l1, l2;
        int na, nf, ip, iw, ido, idl1;
        int[] nac = new int[1];
        final int twon = 2 * n;

        int iw1, iw2;
        float[] ch = new float[twon];

        iw1 = twon;
        iw2 = 4 * n;
        nac[0] = 0;
        nf = (int) wtable[1 + iw2];
        na = 0;
        l1 = 1;
        iw = iw1;
        for (int k1 = 2; k1 <= nf + 1; k1++) {
            ip = (int) wtable[k1 + iw2];
            l2 = ip * l1;
            ido = n / l2;
            idot = ido + ido;
            idl1 = idot * l1;
            switch (ip) {
            case 4:
                if (na == 0) {
                    passf4(idot, l1, a, offa, ch, 0, iw, isign);
                } else {
                    passf4(idot, l1, ch, 0, a, offa, iw, isign);
                }
                na = 1 - na;
                break;
            case 2:
                if (na == 0) {
                    passf2(idot, l1, a, offa, ch, 0, iw, isign);
                } else {
                    passf2(idot, l1, ch, 0, a, offa, iw, isign);
                }
                na = 1 - na;
                break;
            case 3:
                if (na == 0) {
                    passf3(idot, l1, a, offa, ch, 0, iw, isign);
                } else {
                    passf3(idot, l1, ch, 0, a, offa, iw, isign);
                }
                na = 1 - na;
                break;
            case 5:
                if (na == 0) {
                    passf5(idot, l1, a, offa, ch, 0, iw, isign);
                } else {
                    passf5(idot, l1, ch, 0, a, offa, iw, isign);
                }
                na = 1 - na;
                break;
            default:
                if (na == 0) {
                    passfg(nac, idot, ip, l1, idl1, a, offa, ch, 0, iw, isign);
                } else {
                    passfg(nac, idot, ip, l1, idl1, ch, 0, a, offa, iw, isign);
                }
                if (nac[0] != 0)
                    na = 1 - na;
                break;
            }
            l1 = l2;
            iw += (ip - 1) * idot;
        }
        if (na == 0)
            return;
        System.arraycopy(ch, 0, a, offa, twon);

    }

    /*----------------------------------------------------------------------
       passf2: Complex FFT's forward/backward processing of factor 2;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/

    void passf2(final int ido, final int l1, final float in[], final int in_off, final float out[], final int out_off, final int offset, final int isign) {
        float t1i, t1r;
        int iw1;
        iw1 = offset;
        int idx = ido * l1;
        if (ido <= 2) {
            for (int k = 0; k < l1; k++) {
                int idx0 = k * ido;
                int iidx1 = in_off + 2 * idx0;
                int iidx2 = iidx1 + ido;
                float a1r = in[iidx1];
                float a1i = in[iidx1 + 1];
                float a2r = in[iidx2];
                float a2i = in[iidx2 + 1];

                int oidx1 = out_off + idx0;
                int oidx2 = oidx1 + idx;
                out[oidx1] = a1r + a2r;
                out[oidx1 + 1] = a1i + a2i;
                out[oidx2] = a1r - a2r;
                out[oidx2 + 1] = a1i - a2i;
            }
        } else {
            for (int k = 0; k < l1; k++) {
                for (int i = 0; i < ido - 1; i += 2) {
                    int idx0 = k * ido;
                    int iidx1 = in_off + i + 2 * idx0;
                    int iidx2 = iidx1 + ido;
                    float i1r = in[iidx1];
                    float i1i = in[iidx1 + 1];
                    float i2r = in[iidx2];
                    float i2i = in[iidx2 + 1];

                    int widx1 = i + iw1;
                    float w1r = wtable[widx1];
                    float w1i = isign * wtable[widx1 + 1];

                    t1r = i1r - i2r;
                    t1i = i1i - i2i;

                    int oidx1 = out_off + i + idx0;
                    int oidx2 = oidx1 + idx;
                    out[oidx1] = i1r + i2r;
                    out[oidx1 + 1] = i1i + i2i;
                    out[oidx2] = w1r * t1r - w1i * t1i;
                    out[oidx2 + 1] = w1r * t1i + w1i * t1r;
                }
            }
        }
    }

    /*----------------------------------------------------------------------
       passf3: Complex FFT's forward/backward processing of factor 3;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/
    void passf3(final int ido, final int l1, final float in[], final int in_off, final float out[], final int out_off, final int offset, final int isign) {
        final float taur = -0.5f;
        final float taui = 0.866025403784438707610604524234076962f;
        float ci2, ci3, di2, di3, cr2, cr3, dr2, dr3, ti2, tr2;
        int iw1, iw2;

        iw1 = offset;
        iw2 = iw1 + ido;

        final int idxt = l1 * ido;

        if (ido == 2) {
            for (int k = 1; k <= l1; k++) {
                int iidx1 = in_off + (3 * k - 2) * ido;
                int iidx2 = iidx1 + ido;
                int iidx3 = iidx1 - ido;
                float i1r = in[iidx1];
                float i1i = in[iidx1 + 1];
                float i2r = in[iidx2];
                float i2i = in[iidx2 + 1];
                float i3r = in[iidx3];
                float i3i = in[iidx3 + 1];

                tr2 = i1r + i2r;
                cr2 = i3r + taur * tr2;
                ti2 = i1i + i2i;
                ci2 = i3i + taur * ti2;
                cr3 = isign * taui * (i1r - i2r);
                ci3 = isign * taui * (i1i - i2i);

                int oidx1 = out_off + (k - 1) * ido;
                int oidx2 = oidx1 + idxt;
                int oidx3 = oidx2 + idxt;
                out[oidx1] = in[iidx3] + tr2;
                out[oidx1 + 1] = i3i + ti2;
                out[oidx2] = cr2 - ci3;
                out[oidx2 + 1] = ci2 + cr3;
                out[oidx3] = cr2 + ci3;
                out[oidx3 + 1] = ci2 - cr3;
            }
        } else {
            for (int k = 1; k <= l1; k++) {
                int idx1 = in_off + (3 * k - 2) * ido;
                int idx2 = out_off + (k - 1) * ido;
                for (int i = 0; i < ido - 1; i += 2) {
                    int iidx1 = i + idx1;
                    int iidx2 = iidx1 + ido;
                    int iidx3 = iidx1 - ido;
                    float a1r = in[iidx1];
                    float a1i = in[iidx1 + 1];
                    float a2r = in[iidx2];
                    float a2i = in[iidx2 + 1];
                    float a3r = in[iidx3];
                    float a3i = in[iidx3 + 1];

                    tr2 = a1r + a2r;
                    cr2 = a3r + taur * tr2;
                    ti2 = a1i + a2i;
                    ci2 = a3i + taur * ti2;
                    cr3 = isign * taui * (a1r - a2r);
                    ci3 = isign * taui * (a1i - a2i);
                    dr2 = cr2 - ci3;
                    dr3 = cr2 + ci3;
                    di2 = ci2 + cr3;
                    di3 = ci2 - cr3;

                    int widx1 = i + iw1;
                    int widx2 = i + iw2;
                    float w1r = wtable[widx1];
                    float w1i = isign * wtable[widx1 + 1];
                    float w2r = wtable[widx2];
                    float w2i = isign * wtable[widx2 + 1];

                    int oidx1 = i + idx2;
                    int oidx2 = oidx1 + idxt;
                    int oidx3 = oidx2 + idxt;
                    out[oidx1] = a3r + tr2;
                    out[oidx1 + 1] = a3i + ti2;
                    out[oidx2] = w1r * dr2 - w1i * di2;
                    out[oidx2 + 1] = w1r * di2 + w1i * dr2;
                    out[oidx3] = w2r * dr3 - w2i * di3;
                    out[oidx3 + 1] = w2r * di3 + w2i * dr3;
                }
            }
        }
    }

    /*----------------------------------------------------------------------
       passf4: Complex FFT's forward/backward processing of factor 4;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/
    void passf4(final int ido, final int l1, final float in[], final int in_off, final float out[], final int out_off, final int offset, final int isign) {
        float ci2, ci3, ci4, cr2, cr3, cr4, ti1, ti2, ti3, ti4, tr1, tr2, tr3, tr4;
        int iw1, iw2, iw3;
        iw1 = offset;
        iw2 = iw1 + ido;
        iw3 = iw2 + ido;

        int idx0 = l1 * ido;
        if (ido == 2) {
            for (int k = 0; k < l1; k++) {
                int idxt1 = k * ido;
                int iidx1 = in_off + 4 * idxt1 + 1;
                int iidx2 = iidx1 + ido;
                int iidx3 = iidx2 + ido;
                int iidx4 = iidx3 + ido;

                float i1i = in[iidx1 - 1];
                float i1r = in[iidx1];
                float i2i = in[iidx2 - 1];
                float i2r = in[iidx2];
                float i3i = in[iidx3 - 1];
                float i3r = in[iidx3];
                float i4i = in[iidx4 - 1];
                float i4r = in[iidx4];

                ti1 = i1r - i3r;
                ti2 = i1r + i3r;
                tr4 = i4r - i2r;
                ti3 = i2r + i4r;
                tr1 = i1i - i3i;
                tr2 = i1i + i3i;
                ti4 = i2i - i4i;
                tr3 = i2i + i4i;

                int oidx1 = out_off + idxt1;
                int oidx2 = oidx1 + idx0;
                int oidx3 = oidx2 + idx0;
                int oidx4 = oidx3 + idx0;
                out[oidx1] = tr2 + tr3;
                out[oidx1 + 1] = ti2 + ti3;
                out[oidx2] = tr1 + isign * tr4;
                out[oidx2 + 1] = ti1 + isign * ti4;
                out[oidx3] = tr2 - tr3;
                out[oidx3 + 1] = ti2 - ti3;
                out[oidx4] = tr1 - isign * tr4;
                out[oidx4 + 1] = ti1 - isign * ti4;
            }
        } else {
            for (int k = 0; k < l1; k++) {
                int idx1 = k * ido;
                int idx2 = in_off + 1 + 4 * idx1;
                for (int i = 0; i < ido - 1; i += 2) {
                    int iidx1 = i + idx2;
                    int iidx2 = iidx1 + ido;
                    int iidx3 = iidx2 + ido;
                    int iidx4 = iidx3 + ido;
                    float i1i = in[iidx1 - 1];
                    float i1r = in[iidx1];
                    float i2i = in[iidx2 - 1];
                    float i2r = in[iidx2];
                    float i3i = in[iidx3 - 1];
                    float i3r = in[iidx3];
                    float i4i = in[iidx4 - 1];
                    float i4r = in[iidx4];

                    ti1 = i1r - i3r;
                    ti2 = i1r + i3r;
                    ti3 = i2r + i4r;
                    tr4 = i4r - i2r;
                    tr1 = i1i - i3i;
                    tr2 = i1i + i3i;
                    ti4 = i2i - i4i;
                    tr3 = i2i + i4i;
                    cr3 = tr2 - tr3;
                    ci3 = ti2 - ti3;
                    cr2 = tr1 + isign * tr4;
                    cr4 = tr1 - isign * tr4;
                    ci2 = ti1 + isign * ti4;
                    ci4 = ti1 - isign * ti4;

                    int widx1 = i + iw1;
                    int widx2 = i + iw2;
                    int widx3 = i + iw3;
                    float w1r = wtable[widx1];
                    float w1i = isign * wtable[widx1 + 1];
                    float w2r = wtable[widx2];
                    float w2i = isign * wtable[widx2 + 1];
                    float w3r = wtable[widx3];
                    float w3i = isign * wtable[widx3 + 1];

                    int oidx1 = out_off + i + idx1;
                    int oidx2 = oidx1 + idx0;
                    int oidx3 = oidx2 + idx0;
                    int oidx4 = oidx3 + idx0;
                    out[oidx1] = tr2 + tr3;
                    out[oidx1 + 1] = ti2 + ti3;
                    out[oidx2] = w1r * cr2 - w1i * ci2;
                    out[oidx2 + 1] = w1r * ci2 + w1i * cr2;
                    out[oidx3] = w2r * cr3 - w2i * ci3;
                    out[oidx3 + 1] = w2r * ci3 + w2i * cr3;
                    out[oidx4] = w3r * cr4 - w3i * ci4;
                    out[oidx4 + 1] = w3r * ci4 + w3i * cr4;
                }
            }
        }
    }

    /*----------------------------------------------------------------------
       passf5: Complex FFT's forward/backward processing of factor 5;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/
    void passf5(final int ido, final int l1, final float in[], final int in_off, final float out[], final int out_off, final int offset, final int isign)
    /* isign==-1 for forward transform and+1 for backward transform */
    {
        final float tr11 = 0.309016994374947451262869435595348477f;
        final float ti11 = 0.951056516295153531181938433292089030f;
        final float tr12 = -0.809016994374947340240566973079694435f;
        final float ti12 = 0.587785252292473248125759255344746634f;
        float ci2, ci3, ci4, ci5, di3, di4, di5, di2, cr2, cr3, cr5, cr4, ti2, ti3, ti4, ti5, dr3, dr4, dr5, dr2, tr2, tr3, tr4, tr5;
        int iw1, iw2, iw3, iw4;

        iw1 = offset;
        iw2 = iw1 + ido;
        iw3 = iw2 + ido;
        iw4 = iw3 + ido;

        int idx0 = l1 * ido;

        if (ido == 2) {
            for (int k = 1; k <= l1; ++k) {
                int iidx1 = in_off + (5 * k - 4) * ido + 1;
                int iidx2 = iidx1 + ido;
                int iidx3 = iidx1 - ido;
                int iidx4 = iidx2 + ido;
                int iidx5 = iidx4 + ido;

                float i1i = in[iidx1 - 1];
                float i1r = in[iidx1];
                float i2i = in[iidx2 - 1];
                float i2r = in[iidx2];
                float i3i = in[iidx3 - 1];
                float i3r = in[iidx3];
                float i4i = in[iidx4 - 1];
                float i4r = in[iidx4];
                float i5i = in[iidx5 - 1];
                float i5r = in[iidx5];

                ti5 = i1r - i5r;
                ti2 = i1r + i5r;
                ti4 = i2r - i4r;
                ti3 = i2r + i4r;
                tr5 = i1i - i5i;
                tr2 = i1i + i5i;
                tr4 = i2i - i4i;
                tr3 = i2i + i4i;
                cr2 = i3i + tr11 * tr2 + tr12 * tr3;
                ci2 = i3r + tr11 * ti2 + tr12 * ti3;
                cr3 = i3i + tr12 * tr2 + tr11 * tr3;
                ci3 = i3r + tr12 * ti2 + tr11 * ti3;
                cr5 = isign * (ti11 * tr5 + ti12 * tr4);
                ci5 = isign * (ti11 * ti5 + ti12 * ti4);
                cr4 = isign * (ti12 * tr5 - ti11 * tr4);
                ci4 = isign * (ti12 * ti5 - ti11 * ti4);

                int oidx1 = out_off + (k - 1) * ido;
                int oidx2 = oidx1 + idx0;
                int oidx3 = oidx2 + idx0;
                int oidx4 = oidx3 + idx0;
                int oidx5 = oidx4 + idx0;
                out[oidx1] = i3i + tr2 + tr3;
                out[oidx1 + 1] = i3r + ti2 + ti3;
                out[oidx2] = cr2 - ci5;
                out[oidx2 + 1] = ci2 + cr5;
                out[oidx3] = cr3 - ci4;
                out[oidx3 + 1] = ci3 + cr4;
                out[oidx4] = cr3 + ci4;
                out[oidx4 + 1] = ci3 - cr4;
                out[oidx5] = cr2 + ci5;
                out[oidx5 + 1] = ci2 - cr5;
            }
        } else {
            for (int k = 1; k <= l1; k++) {
                int idx1 = in_off + 1 + (k * 5 - 4) * ido;
                int idx2 = out_off + (k - 1) * ido;
                for (int i = 0; i < ido - 1; i += 2) {
                    int iidx1 = i + idx1;
                    int iidx2 = iidx1 + ido;
                    int iidx3 = iidx1 - ido;
                    int iidx4 = iidx2 + ido;
                    int iidx5 = iidx4 + ido;
                    float i1i = in[iidx1 - 1];
                    float i1r = in[iidx1];
                    float i2i = in[iidx2 - 1];
                    float i2r = in[iidx2];
                    float i3i = in[iidx3 - 1];
                    float i3r = in[iidx3];
                    float i4i = in[iidx4 - 1];
                    float i4r = in[iidx4];
                    float i5i = in[iidx5 - 1];
                    float i5r = in[iidx5];

                    ti5 = i1r - i5r;
                    ti2 = i1r + i5r;
                    ti4 = i2r - i4r;
                    ti3 = i2r + i4r;
                    tr5 = i1i - i5i;
                    tr2 = i1i + i5i;
                    tr4 = i2i - i4i;
                    tr3 = i2i + i4i;
                    cr2 = i3i + tr11 * tr2 + tr12 * tr3;
                    ci2 = i3r + tr11 * ti2 + tr12 * ti3;
                    cr3 = i3i + tr12 * tr2 + tr11 * tr3;
                    ci3 = i3r + tr12 * ti2 + tr11 * ti3;
                    cr5 = isign * (ti11 * tr5 + ti12 * tr4);
                    ci5 = isign * (ti11 * ti5 + ti12 * ti4);
                    cr4 = isign * (ti12 * tr5 - ti11 * tr4);
                    ci4 = isign * (ti12 * ti5 - ti11 * ti4);
                    dr3 = cr3 - ci4;
                    dr4 = cr3 + ci4;
                    di3 = ci3 + cr4;
                    di4 = ci3 - cr4;
                    dr5 = cr2 + ci5;
                    dr2 = cr2 - ci5;
                    di5 = ci2 - cr5;
                    di2 = ci2 + cr5;

                    int widx1 = i + iw1;
                    int widx2 = i + iw2;
                    int widx3 = i + iw3;
                    int widx4 = i + iw4;
                    float w1r = wtable[widx1];
                    float w1i = isign * wtable[widx1 + 1];
                    float w2r = wtable[widx2];
                    float w2i = isign * wtable[widx2 + 1];
                    float w3r = wtable[widx3];
                    float w3i = isign * wtable[widx3 + 1];
                    float w4r = wtable[widx4];
                    float w4i = isign * wtable[widx4 + 1];

                    int oidx1 = i + idx2;
                    int oidx2 = oidx1 + idx0;
                    int oidx3 = oidx2 + idx0;
                    int oidx4 = oidx3 + idx0;
                    int oidx5 = oidx4 + idx0;
                    out[oidx1] = i3i + tr2 + tr3;
                    out[oidx1 + 1] = i3r + ti2 + ti3;
                    out[oidx2] = w1r * dr2 - w1i * di2;
                    out[oidx2 + 1] = w1r * di2 + w1i * dr2;
                    out[oidx3] = w2r * dr3 - w2i * di3;
                    out[oidx3 + 1] = w2r * di3 + w2i * dr3;
                    out[oidx4] = w3r * dr4 - w3i * di4;
                    out[oidx4 + 1] = w3r * di4 + w3i * dr4;
                    out[oidx5] = w4r * dr5 - w4i * di5;
                    out[oidx5 + 1] = w4r * di5 + w4i * dr5;
                }
            }
        }
    }

    /*----------------------------------------------------------------------
       passfg: Complex FFT's forward/backward processing of general factor;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/
    void passfg(final int nac[], final int ido, final int ip, final int l1, final int idl1, final float in[], final int in_off, final float out[], final int out_off, final int offset, final int isign) {
        int idij, idlj, idot, ipph, l, jc, lc, idj, idl, inc, idp;
        float w1r, w1i, w2i, w2r;
        int iw1;

        iw1 = offset;
        idot = ido / 2;
        ipph = (ip + 1) / 2;
        idp = ip * ido;
        if (ido >= l1) {
            for (int j = 1; j < ipph; j++) {
                jc = ip - j;
                int idx1 = j * ido;
                int idx2 = jc * ido;
                for (int k = 0; k < l1; k++) {
                    int idx3 = k * ido;
                    int idx4 = idx3 + idx1 * l1;
                    int idx5 = idx3 + idx2 * l1;
                    int idx6 = idx3 * ip;
                    for (int i = 0; i < ido; i++) {
                        int oidx1 = out_off + i;
                        float i1r = in[in_off + i + idx1 + idx6];
                        float i2r = in[in_off + i + idx2 + idx6];
                        out[oidx1 + idx4] = i1r + i2r;
                        out[oidx1 + idx5] = i1r - i2r;
                    }
                }
            }
            for (int k = 0; k < l1; k++) {
                int idxt1 = k * ido;
                int idxt2 = idxt1 * ip;
                for (int i = 0; i < ido; i++) {
                    out[out_off + i + idxt1] = in[in_off + i + idxt2];
                }
            }
        } else {
            for (int j = 1; j < ipph; j++) {
                jc = ip - j;
                int idxt1 = j * l1 * ido;
                int idxt2 = jc * l1 * ido;
                int idxt3 = j * ido;
                int idxt4 = jc * ido;
                for (int i = 0; i < ido; i++) {
                    for (int k = 0; k < l1; k++) {
                        int idx1 = k * ido;
                        int idx2 = idx1 * ip;
                        int idx3 = out_off + i;
                        int idx4 = in_off + i;
                        float i1r = in[idx4 + idxt3 + idx2];
                        float i2r = in[idx4 + idxt4 + idx2];
                        out[idx3 + idx1 + idxt1] = i1r + i2r;
                        out[idx3 + idx1 + idxt2] = i1r - i2r;
                    }
                }
            }
            for (int i = 0; i < ido; i++) {
                for (int k = 0; k < l1; k++) {
                    int idx1 = k * ido;
                    out[out_off + i + idx1] = in[in_off + i + idx1 * ip];
                }
            }
        }

        idl = 2 - ido;
        inc = 0;
        int idxt0 = (ip - 1) * idl1;
        for (l = 1; l < ipph; l++) {
            lc = ip - l;
            idl += ido;
            int idxt1 = l * idl1;
            int idxt2 = lc * idl1;
            int idxt3 = idl + iw1;
            w1r = wtable[idxt3 - 2];
            w1i = isign * wtable[idxt3 - 1];
            for (int ik = 0; ik < idl1; ik++) {
                int idx1 = in_off + ik;
                int idx2 = out_off + ik;
                in[idx1 + idxt1] = out[idx2] + w1r * out[idx2 + idl1];
                in[idx1 + idxt2] = w1i * out[idx2 + idxt0];
            }
            idlj = idl;
            inc += ido;
            for (int j = 2; j < ipph; j++) {
                jc = ip - j;
                idlj += inc;
                if (idlj > idp)
                    idlj -= idp;
                int idxt4 = idlj + iw1;
                w2r = wtable[idxt4 - 2];
                w2i = isign * wtable[idxt4 - 1];
                int idxt5 = j * idl1;
                int idxt6 = jc * idl1;
                for (int ik = 0; ik < idl1; ik++) {
                    int idx1 = in_off + ik;
                    int idx2 = out_off + ik;
                    in[idx1 + idxt1] += w2r * out[idx2 + idxt5];
                    in[idx1 + idxt2] += w2i * out[idx2 + idxt6];
                }
            }
        }
        for (int j = 1; j < ipph; j++) {
            int idxt1 = j * idl1;
            for (int ik = 0; ik < idl1; ik++) {
                int idx1 = out_off + ik;
                out[idx1] += out[idx1 + idxt1];
            }
        }
        for (int j = 1; j < ipph; j++) {
            jc = ip - j;
            int idx1 = j * idl1;
            int idx2 = jc * idl1;
            for (int ik = 1; ik < idl1; ik += 2) {
                int idx3 = out_off + ik;
                int idx4 = in_off + ik;
                int iidx1 = idx4 + idx1;
                int iidx2 = idx4 + idx2;
                float i1i = in[iidx1 - 1];
                float i1r = in[iidx1];
                float i2i = in[iidx2 - 1];
                float i2r = in[iidx2];

                int oidx1 = idx3 + idx1;
                int oidx2 = idx3 + idx2;
                out[oidx1 - 1] = i1i - i2r;
                out[oidx2 - 1] = i1i + i2r;
                out[oidx1] = i1r + i2i;
                out[oidx2] = i1r - i2i;
            }
        }
        nac[0] = 1;
        if (ido == 2)
            return;
        nac[0] = 0;
        System.arraycopy(out, out_off, in, in_off, idl1);
        int idx0 = l1 * ido;
        for (int j = 1; j < ip; j++) {
            int idx1 = j * idx0;
            for (int k = 0; k < l1; k++) {
                int idx2 = k * ido;
                int oidx1 = out_off + idx2 + idx1;
                int iidx1 = in_off + idx2 + idx1;
                in[iidx1] = out[oidx1];
                in[iidx1 + 1] = out[oidx1 + 1];
            }
        }
        if (idot <= l1) {
            idij = 0;
            for (int j = 1; j < ip; j++) {
                idij += 2;
                int idx1 = j * l1 * ido;
                for (int i = 3; i < ido; i += 2) {
                    idij += 2;
                    int idx2 = idij + iw1 - 1;
                    w1r = wtable[idx2 - 1];
                    w1i = isign * wtable[idx2];
                    int idx3 = in_off + i;
                    int idx4 = out_off + i;
                    for (int k = 0; k < l1; k++) {
                        int idx5 = k * ido + idx1;
                        int iidx1 = idx3 + idx5;
                        int oidx1 = idx4 + idx5;
                        float o1i = out[oidx1 - 1];
                        float o1r = out[oidx1];
                        in[iidx1 - 1] = w1r * o1i - w1i * o1r;
                        in[iidx1] = w1r * o1r + w1i * o1i;
                    }
                }
            }
        } else {
            idj = 2 - ido;
            for (int j = 1; j < ip; j++) {
                idj += ido;
                int idx1 = j * l1 * ido;
                for (int k = 0; k < l1; k++) {
                    idij = idj;
                    int idx3 = k * ido + idx1;
                    for (int i = 3; i < ido; i += 2) {
                        idij += 2;
                        int idx2 = idij - 1 + iw1;
                        w1r = wtable[idx2 - 1];
                        w1i = isign * wtable[idx2];
                        int iidx1 = in_off + i + idx3;
                        int oidx1 = out_off + i + idx3;
                        float o1i = out[oidx1 - 1];
                        float o1r = out[oidx1];
                        in[iidx1 - 1] = w1r * o1i - w1i * o1r;
                        in[iidx1] = w1r * o1r + w1i * o1i;
                    }
                }
            }
        }
    }

    private void cftfsub(int n, float[] a, int offa, int[] ip, int nw, float[] w) {
        if (n > 8) {
            if (n > 32) {
                cftf1st(n, a, offa, w, nw - (n >> 2));
                if ((ConcurrencyUtils.getNumberOfThreads() > 1) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
                    cftrec4_th(n, a, offa, nw, w);
                } else if (n > 512) {
                    cftrec4(n, a, offa, nw, w);
                } else if (n > 128) {
                    cftleaf(n, 1, a, offa, nw, w);
                } else {
                    cftfx41(n, a, offa, nw, w);
                }
                bitrv2(n, ip, a, offa);
            } else if (n == 32) {
                cftf161(a, offa, w, nw - 8);
                bitrv216(a, offa);
            } else {
                cftf081(a, offa, w, 0);
                bitrv208(a, offa);
            }
        } else if (n == 8) {
            cftf040(a, offa);
        } else if (n == 4) {
            cftxb020(a, offa);
        }
    }

    private void cftbsub(int n, float[] a, int offa, int[] ip, int nw, float[] w) {
        if (n > 8) {
            if (n > 32) {
                cftb1st(n, a, offa, w, nw - (n >> 2));
                if ((ConcurrencyUtils.getNumberOfThreads() > 1) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
                    cftrec4_th(n, a, offa, nw, w);
                } else if (n > 512) {
                    cftrec4(n, a, offa, nw, w);
                } else if (n > 128) {
                    cftleaf(n, 1, a, offa, nw, w);
                } else {
                    cftfx41(n, a, offa, nw, w);
                }
                bitrv2conj(n, ip, a, offa);
            } else if (n == 32) {
                cftf161(a, offa, w, nw - 8);
                bitrv216neg(a, offa);
            } else {
                cftf081(a, offa, w, 0);
                bitrv208neg(a, offa);
            }
        } else if (n == 8) {
            cftb040(a, offa);
        } else if (n == 4) {
            cftxb020(a, offa);
        }
    }

    private void bitrv2(int n, int[] ip, float[] a, int offa) {
        int j1, k1, l, m, nh, nm;
        float xr, xi, yr, yi;
        int idx0, idx1, idx2;

        m = 1;
        for (l = n >> 2; l > 8; l >>= 2) {
            m <<= 1;
        }
        nh = n >> 1;
        nm = 4 * m;
        if (l == 8) {
            for (int k = 0; k < m; k++) {
                idx0 = 4 * k;
                for (int j = 0; j < k; j++) {
                    j1 = 4 * j + 2 * ip[m + k];
                    k1 = idx0 + 2 * ip[m + j];
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nh;
                    k1 += 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += 2;
                    k1 += nh;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nh;
                    k1 -= 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                }
                k1 = idx0 + 2 * ip[m + k];
                j1 = k1 + 2;
                k1 += nh;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 += nm;
                k1 += 2 * nm;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 += nm;
                k1 -= nm;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 -= 2;
                k1 -= nh;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 += nh + 2;
                k1 += nh + 2;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 -= nh - nm;
                k1 += 2 * nm - 2;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
            }
        } else {
            for (int k = 0; k < m; k++) {
                idx0 = 4 * k;
                for (int j = 0; j < k; j++) {
                    j1 = 4 * j + ip[m + k];
                    k1 = idx0 + ip[m + j];
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nh;
                    k1 += 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += 2;
                    k1 += nh;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nh;
                    k1 -= 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                }
                k1 = idx0 + ip[m + k];
                j1 = k1 + 2;
                k1 += nh;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 += nm;
                k1 += nm;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
            }
        }
    }

    private void bitrv2conj(int n, int[] ip, float[] a, int offa) {
        int j1, k1, l, m, nh, nm;
        float xr, xi, yr, yi;
        int idx0, idx1, idx2;

        m = 1;
        for (l = n >> 2; l > 8; l >>= 2) {
            m <<= 1;
        }
        nh = n >> 1;
        nm = 4 * m;
        if (l == 8) {
            for (int k = 0; k < m; k++) {
                idx0 = 4 * k;
                for (int j = 0; j < k; j++) {
                    j1 = 4 * j + 2 * ip[m + k];
                    k1 = idx0 + 2 * ip[m + j];
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nh;
                    k1 += 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += 2;
                    k1 += nh;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nh;
                    k1 -= 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                }
                k1 = idx0 + 2 * ip[m + k];
                j1 = k1 + 2;
                k1 += nh;
                idx1 = offa + j1;
                idx2 = offa + k1;
                a[idx1 - 1] = -a[idx1 - 1];
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                a[idx2 + 3] = -a[idx2 + 3];
                j1 += nm;
                k1 += 2 * nm;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 += nm;
                k1 -= nm;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 -= 2;
                k1 -= nh;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 += nh + 2;
                k1 += nh + 2;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 -= nh - nm;
                k1 += 2 * nm - 2;
                idx1 = offa + j1;
                idx2 = offa + k1;
                a[idx1 - 1] = -a[idx1 - 1];
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                a[idx2 + 3] = -a[idx2 + 3];
            }
        } else {
            for (int k = 0; k < m; k++) {
                idx0 = 4 * k;
                for (int j = 0; j < k; j++) {
                    j1 = 4 * j + ip[m + k];
                    k1 = idx0 + ip[m + j];
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nh;
                    k1 += 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += 2;
                    k1 += nh;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nh;
                    k1 -= 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                }
                k1 = idx0 + ip[m + k];
                j1 = k1 + 2;
                k1 += nh;
                idx1 = offa + j1;
                idx2 = offa + k1;
                a[idx1 - 1] = -a[idx1 - 1];
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                a[idx2 + 3] = -a[idx2 + 3];
                j1 += nm;
                k1 += nm;
                idx1 = offa + j1;
                idx2 = offa + k1;
                a[idx1 - 1] = -a[idx1 - 1];
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                a[idx2 + 3] = -a[idx2 + 3];
            }
        }
    }

    private void bitrv216(float[] a, int offa) {
        float x1r, x1i, x2r, x2i, x3r, x3i, x4r, x4i, x5r, x5i, x7r, x7i, x8r, x8i, x10r, x10i, x11r, x11i, x12r, x12i, x13r, x13i, x14r, x14i;

        x1r = a[offa + 2];
        x1i = a[offa + 3];
        x2r = a[offa + 4];
        x2i = a[offa + 5];
        x3r = a[offa + 6];
        x3i = a[offa + 7];
        x4r = a[offa + 8];
        x4i = a[offa + 9];
        x5r = a[offa + 10];
        x5i = a[offa + 11];
        x7r = a[offa + 14];
        x7i = a[offa + 15];
        x8r = a[offa + 16];
        x8i = a[offa + 17];
        x10r = a[offa + 20];
        x10i = a[offa + 21];
        x11r = a[offa + 22];
        x11i = a[offa + 23];
        x12r = a[offa + 24];
        x12i = a[offa + 25];
        x13r = a[offa + 26];
        x13i = a[offa + 27];
        x14r = a[offa + 28];
        x14i = a[offa + 29];
        a[offa + 2] = x8r;
        a[offa + 3] = x8i;
        a[offa + 4] = x4r;
        a[offa + 5] = x4i;
        a[offa + 6] = x12r;
        a[offa + 7] = x12i;
        a[offa + 8] = x2r;
        a[offa + 9] = x2i;
        a[offa + 10] = x10r;
        a[offa + 11] = x10i;
        a[offa + 14] = x14r;
        a[offa + 15] = x14i;
        a[offa + 16] = x1r;
        a[offa + 17] = x1i;
        a[offa + 20] = x5r;
        a[offa + 21] = x5i;
        a[offa + 22] = x13r;
        a[offa + 23] = x13i;
        a[offa + 24] = x3r;
        a[offa + 25] = x3i;
        a[offa + 26] = x11r;
        a[offa + 27] = x11i;
        a[offa + 28] = x7r;
        a[offa + 29] = x7i;
    }

    private void bitrv216neg(float[] a, int offa) {
        float x1r, x1i, x2r, x2i, x3r, x3i, x4r, x4i, x5r, x5i, x6r, x6i, x7r, x7i, x8r, x8i, x9r, x9i, x10r, x10i, x11r, x11i, x12r, x12i, x13r, x13i, x14r, x14i, x15r, x15i;

        x1r = a[offa + 2];
        x1i = a[offa + 3];
        x2r = a[offa + 4];
        x2i = a[offa + 5];
        x3r = a[offa + 6];
        x3i = a[offa + 7];
        x4r = a[offa + 8];
        x4i = a[offa + 9];
        x5r = a[offa + 10];
        x5i = a[offa + 11];
        x6r = a[offa + 12];
        x6i = a[offa + 13];
        x7r = a[offa + 14];
        x7i = a[offa + 15];
        x8r = a[offa + 16];
        x8i = a[offa + 17];
        x9r = a[offa + 18];
        x9i = a[offa + 19];
        x10r = a[offa + 20];
        x10i = a[offa + 21];
        x11r = a[offa + 22];
        x11i = a[offa + 23];
        x12r = a[offa + 24];
        x12i = a[offa + 25];
        x13r = a[offa + 26];
        x13i = a[offa + 27];
        x14r = a[offa + 28];
        x14i = a[offa + 29];
        x15r = a[offa + 30];
        x15i = a[offa + 31];
        a[offa + 2] = x15r;
        a[offa + 3] = x15i;
        a[offa + 4] = x7r;
        a[offa + 5] = x7i;
        a[offa + 6] = x11r;
        a[offa + 7] = x11i;
        a[offa + 8] = x3r;
        a[offa + 9] = x3i;
        a[offa + 10] = x13r;
        a[offa + 11] = x13i;
        a[offa + 12] = x5r;
        a[offa + 13] = x5i;
        a[offa + 14] = x9r;
        a[offa + 15] = x9i;
        a[offa + 16] = x1r;
        a[offa + 17] = x1i;
        a[offa + 18] = x14r;
        a[offa + 19] = x14i;
        a[offa + 20] = x6r;
        a[offa + 21] = x6i;
        a[offa + 22] = x10r;
        a[offa + 23] = x10i;
        a[offa + 24] = x2r;
        a[offa + 25] = x2i;
        a[offa + 26] = x12r;
        a[offa + 27] = x12i;
        a[offa + 28] = x4r;
        a[offa + 29] = x4i;
        a[offa + 30] = x8r;
        a[offa + 31] = x8i;
    }

    private void bitrv208(float[] a, int offa) {
        float x1r, x1i, x3r, x3i, x4r, x4i, x6r, x6i;

        x1r = a[offa + 2];
        x1i = a[offa + 3];
        x3r = a[offa + 6];
        x3i = a[offa + 7];
        x4r = a[offa + 8];
        x4i = a[offa + 9];
        x6r = a[offa + 12];
        x6i = a[offa + 13];
        a[offa + 2] = x4r;
        a[offa + 3] = x4i;
        a[offa + 6] = x6r;
        a[offa + 7] = x6i;
        a[offa + 8] = x1r;
        a[offa + 9] = x1i;
        a[offa + 12] = x3r;
        a[offa + 13] = x3i;
    }

    private void bitrv208neg(float[] a, int offa) {
        float x1r, x1i, x2r, x2i, x3r, x3i, x4r, x4i, x5r, x5i, x6r, x6i, x7r, x7i;

        x1r = a[offa + 2];
        x1i = a[offa + 3];
        x2r = a[offa + 4];
        x2i = a[offa + 5];
        x3r = a[offa + 6];
        x3i = a[offa + 7];
        x4r = a[offa + 8];
        x4i = a[offa + 9];
        x5r = a[offa + 10];
        x5i = a[offa + 11];
        x6r = a[offa + 12];
        x6i = a[offa + 13];
        x7r = a[offa + 14];
        x7i = a[offa + 15];
        a[offa + 2] = x7r;
        a[offa + 3] = x7i;
        a[offa + 4] = x3r;
        a[offa + 5] = x3i;
        a[offa + 6] = x5r;
        a[offa + 7] = x5i;
        a[offa + 8] = x1r;
        a[offa + 9] = x1i;
        a[offa + 10] = x6r;
        a[offa + 11] = x6i;
        a[offa + 12] = x2r;
        a[offa + 13] = x2i;
        a[offa + 14] = x4r;
        a[offa + 15] = x4i;
    }

    private void cftf1st(int n, float[] a, int offa, float[] w, int startw) {
        int j0, j1, j2, j3, k, m, mh;
        float wn4r, csc1, csc3, wk1r, wk1i, wk3r, wk3i, wd1r, wd1i, wd3r, wd3i;
        float x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i, y0r, y0i, y1r, y1i, y2r, y2i, y3r, y3i;
        int idx0, idx1, idx2, idx3, idx4, idx5;
        mh = n >> 3;
        m = 2 * mh;
        j1 = m;
        j2 = j1 + m;
        j3 = j2 + m;
        idx1 = offa + j1;
        idx2 = offa + j2;
        idx3 = offa + j3;
        x0r = a[offa] + a[idx2];
        x0i = a[offa + 1] + a[idx2 + 1];
        x1r = a[offa] - a[idx2];
        x1i = a[offa + 1] - a[idx2 + 1];
        x2r = a[idx1] + a[idx3];
        x2i = a[idx1 + 1] + a[idx3 + 1];
        x3r = a[idx1] - a[idx3];
        x3i = a[idx1 + 1] - a[idx3 + 1];
        a[offa] = x0r + x2r;
        a[offa + 1] = x0i + x2i;
        a[idx1] = x0r - x2r;
        a[idx1 + 1] = x0i - x2i;
        a[idx2] = x1r - x3i;
        a[idx2 + 1] = x1i + x3r;
        a[idx3] = x1r + x3i;
        a[idx3 + 1] = x1i - x3r;
        wn4r = w[startw + 1];
        csc1 = w[startw + 2];
        csc3 = w[startw + 3];
        wd1r = 1;
        wd1i = 0;
        wd3r = 1;
        wd3i = 0;
        k = 0;
        for (int j = 2; j < mh - 2; j += 4) {
            k += 4;
            idx4 = startw + k;
            wk1r = csc1 * (wd1r + w[idx4]);
            wk1i = csc1 * (wd1i + w[idx4 + 1]);
            wk3r = csc3 * (wd3r + w[idx4 + 2]);
            wk3i = csc3 * (wd3i + w[idx4 + 3]);
            wd1r = w[idx4];
            wd1i = w[idx4 + 1];
            wd3r = w[idx4 + 2];
            wd3i = w[idx4 + 3];
            j1 = j + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            idx5 = offa + j;
            x0r = a[idx5] + a[idx2];
            x0i = a[idx5 + 1] + a[idx2 + 1];
            x1r = a[idx5] - a[idx2];
            x1i = a[idx5 + 1] - a[idx2 + 1];
            y0r = a[idx5 + 2] + a[idx2 + 2];
            y0i = a[idx5 + 3] + a[idx2 + 3];
            y1r = a[idx5 + 2] - a[idx2 + 2];
            y1i = a[idx5 + 3] - a[idx2 + 3];
            x2r = a[idx1] + a[idx3];
            x2i = a[idx1 + 1] + a[idx3 + 1];
            x3r = a[idx1] - a[idx3];
            x3i = a[idx1 + 1] - a[idx3 + 1];
            y2r = a[idx1 + 2] + a[idx3 + 2];
            y2i = a[idx1 + 3] + a[idx3 + 3];
            y3r = a[idx1 + 2] - a[idx3 + 2];
            y3i = a[idx1 + 3] - a[idx3 + 3];
            a[idx5] = x0r + x2r;
            a[idx5 + 1] = x0i + x2i;
            a[idx5 + 2] = y0r + y2r;
            a[idx5 + 3] = y0i + y2i;
            a[idx1] = x0r - x2r;
            a[idx1 + 1] = x0i - x2i;
            a[idx1 + 2] = y0r - y2r;
            a[idx1 + 3] = y0i - y2i;
            x0r = x1r - x3i;
            x0i = x1i + x3r;
            a[idx2] = wk1r * x0r - wk1i * x0i;
            a[idx2 + 1] = wk1r * x0i + wk1i * x0r;
            x0r = y1r - y3i;
            x0i = y1i + y3r;
            a[idx2 + 2] = wd1r * x0r - wd1i * x0i;
            a[idx2 + 3] = wd1r * x0i + wd1i * x0r;
            x0r = x1r + x3i;
            x0i = x1i - x3r;
            a[idx3] = wk3r * x0r + wk3i * x0i;
            a[idx3 + 1] = wk3r * x0i - wk3i * x0r;
            x0r = y1r + y3i;
            x0i = y1i - y3r;
            a[idx3 + 2] = wd3r * x0r + wd3i * x0i;
            a[idx3 + 3] = wd3r * x0i - wd3i * x0r;
            j0 = m - j;
            j1 = j0 + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx0 = offa + j0;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            x0r = a[idx0] + a[idx2];
            x0i = a[idx0 + 1] + a[idx2 + 1];
            x1r = a[idx0] - a[idx2];
            x1i = a[idx0 + 1] - a[idx2 + 1];
            y0r = a[idx0 - 2] + a[idx2 - 2];
            y0i = a[idx0 - 1] + a[idx2 - 1];
            y1r = a[idx0 - 2] - a[idx2 - 2];
            y1i = a[idx0 - 1] - a[idx2 - 1];
            x2r = a[idx1] + a[idx3];
            x2i = a[idx1 + 1] + a[idx3 + 1];
            x3r = a[idx1] - a[idx3];
            x3i = a[idx1 + 1] - a[idx3 + 1];
            y2r = a[idx1 - 2] + a[idx3 - 2];
            y2i = a[idx1 - 1] + a[idx3 - 1];
            y3r = a[idx1 - 2] - a[idx3 - 2];
            y3i = a[idx1 - 1] - a[idx3 - 1];
            a[idx0] = x0r + x2r;
            a[idx0 + 1] = x0i + x2i;
            a[idx0 - 2] = y0r + y2r;
            a[idx0 - 1] = y0i + y2i;
            a[idx1] = x0r - x2r;
            a[idx1 + 1] = x0i - x2i;
            a[idx1 - 2] = y0r - y2r;
            a[idx1 - 1] = y0i - y2i;
            x0r = x1r - x3i;
            x0i = x1i + x3r;
            a[idx2] = wk1i * x0r - wk1r * x0i;
            a[idx2 + 1] = wk1i * x0i + wk1r * x0r;
            x0r = y1r - y3i;
            x0i = y1i + y3r;
            a[idx2 - 2] = wd1i * x0r - wd1r * x0i;
            a[idx2 - 1] = wd1i * x0i + wd1r * x0r;
            x0r = x1r + x3i;
            x0i = x1i - x3r;
            a[idx3] = wk3i * x0r + wk3r * x0i;
            a[idx3 + 1] = wk3i * x0i - wk3r * x0r;
            x0r = y1r + y3i;
            x0i = y1i - y3r;
            a[offa + j3 - 2] = wd3i * x0r + wd3r * x0i;
            a[offa + j3 - 1] = wd3i * x0i - wd3r * x0r;
        }
        wk1r = csc1 * (wd1r + wn4r);
        wk1i = csc1 * (wd1i + wn4r);
        wk3r = csc3 * (wd3r - wn4r);
        wk3i = csc3 * (wd3i - wn4r);
        j0 = mh;
        j1 = j0 + m;
        j2 = j1 + m;
        j3 = j2 + m;
        idx0 = offa + j0;
        idx1 = offa + j1;
        idx2 = offa + j2;
        idx3 = offa + j3;
        x0r = a[idx0 - 2] + a[idx2 - 2];
        x0i = a[idx0 - 1] + a[idx2 - 1];
        x1r = a[idx0 - 2] - a[idx2 - 2];
        x1i = a[idx0 - 1] - a[idx2 - 1];
        x2r = a[idx1 - 2] + a[idx3 - 2];
        x2i = a[idx1 - 1] + a[idx3 - 1];
        x3r = a[idx1 - 2] - a[idx3 - 2];
        x3i = a[idx1 - 1] - a[idx3 - 1];
        a[idx0 - 2] = x0r + x2r;
        a[idx0 - 1] = x0i + x2i;
        a[idx1 - 2] = x0r - x2r;
        a[idx1 - 1] = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        a[idx2 - 2] = wk1r * x0r - wk1i * x0i;
        a[idx2 - 1] = wk1r * x0i + wk1i * x0r;
        x0r = x1r + x3i;
        x0i = x1i - x3r;
        a[idx3 - 2] = wk3r * x0r + wk3i * x0i;
        a[idx3 - 1] = wk3r * x0i - wk3i * x0r;
        x0r = a[idx0] + a[idx2];
        x0i = a[idx0 + 1] + a[idx2 + 1];
        x1r = a[idx0] - a[idx2];
        x1i = a[idx0 + 1] - a[idx2 + 1];
        x2r = a[idx1] + a[idx3];
        x2i = a[idx1 + 1] + a[idx3 + 1];
        x3r = a[idx1] - a[idx3];
        x3i = a[idx1 + 1] - a[idx3 + 1];
        a[idx0] = x0r + x2r;
        a[idx0 + 1] = x0i + x2i;
        a[idx1] = x0r - x2r;
        a[idx1 + 1] = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        a[idx2] = wn4r * (x0r - x0i);
        a[idx2 + 1] = wn4r * (x0i + x0r);
        x0r = x1r + x3i;
        x0i = x1i - x3r;
        a[idx3] = -wn4r * (x0r + x0i);
        a[idx3 + 1] = -wn4r * (x0i - x0r);
        x0r = a[idx0 + 2] + a[idx2 + 2];
        x0i = a[idx0 + 3] + a[idx2 + 3];
        x1r = a[idx0 + 2] - a[idx2 + 2];
        x1i = a[idx0 + 3] - a[idx2 + 3];
        x2r = a[idx1 + 2] + a[idx3 + 2];
        x2i = a[idx1 + 3] + a[idx3 + 3];
        x3r = a[idx1 + 2] - a[idx3 + 2];
        x3i = a[idx1 + 3] - a[idx3 + 3];
        a[idx0 + 2] = x0r + x2r;
        a[idx0 + 3] = x0i + x2i;
        a[idx1 + 2] = x0r - x2r;
        a[idx1 + 3] = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        a[idx2 + 2] = wk1i * x0r - wk1r * x0i;
        a[idx2 + 3] = wk1i * x0i + wk1r * x0r;
        x0r = x1r + x3i;
        x0i = x1i - x3r;
        a[idx3 + 2] = wk3i * x0r + wk3r * x0i;
        a[idx3 + 3] = wk3i * x0i - wk3r * x0r;
    }

    private void cftb1st(int n, float[] a, int offa, float[] w, int startw) {
        int j0, j1, j2, j3, k, m, mh;
        float wn4r, csc1, csc3, wk1r, wk1i, wk3r, wk3i, wd1r, wd1i, wd3r, wd3i;
        float x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i, y0r, y0i, y1r, y1i, y2r, y2i, y3r, y3i;
        int idx0, idx1, idx2, idx3, idx4, idx5;
        mh = n >> 3;
        m = 2 * mh;
        j1 = m;
        j2 = j1 + m;
        j3 = j2 + m;
        idx1 = offa + j1;
        idx2 = offa + j2;
        idx3 = offa + j3;

        x0r = a[offa] + a[idx2];
        x0i = -a[offa + 1] - a[idx2 + 1];
        x1r = a[offa] - a[idx2];
        x1i = -a[offa + 1] + a[idx2 + 1];
        x2r = a[idx1] + a[idx3];
        x2i = a[idx1 + 1] + a[idx3 + 1];
        x3r = a[idx1] - a[idx3];
        x3i = a[idx1 + 1] - a[idx3 + 1];
        a[offa] = x0r + x2r;
        a[offa + 1] = x0i - x2i;
        a[idx1] = x0r - x2r;
        a[idx1 + 1] = x0i + x2i;
        a[idx2] = x1r + x3i;
        a[idx2 + 1] = x1i + x3r;
        a[idx3] = x1r - x3i;
        a[idx3 + 1] = x1i - x3r;
        wn4r = w[startw + 1];
        csc1 = w[startw + 2];
        csc3 = w[startw + 3];
        wd1r = 1;
        wd1i = 0;
        wd3r = 1;
        wd3i = 0;
        k = 0;
        for (int j = 2; j < mh - 2; j += 4) {
            k += 4;
            idx4 = startw + k;
            wk1r = csc1 * (wd1r + w[idx4]);
            wk1i = csc1 * (wd1i + w[idx4 + 1]);
            wk3r = csc3 * (wd3r + w[idx4 + 2]);
            wk3i = csc3 * (wd3i + w[idx4 + 3]);
            wd1r = w[idx4];
            wd1i = w[idx4 + 1];
            wd3r = w[idx4 + 2];
            wd3i = w[idx4 + 3];
            j1 = j + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            idx5 = offa + j;
            x0r = a[idx5] + a[idx2];
            x0i = -a[idx5 + 1] - a[idx2 + 1];
            x1r = a[idx5] - a[offa + j2];
            x1i = -a[idx5 + 1] + a[idx2 + 1];
            y0r = a[idx5 + 2] + a[idx2 + 2];
            y0i = -a[idx5 + 3] - a[idx2 + 3];
            y1r = a[idx5 + 2] - a[idx2 + 2];
            y1i = -a[idx5 + 3] + a[idx2 + 3];
            x2r = a[idx1] + a[idx3];
            x2i = a[idx1 + 1] + a[idx3 + 1];
            x3r = a[idx1] - a[idx3];
            x3i = a[idx1 + 1] - a[idx3 + 1];
            y2r = a[idx1 + 2] + a[idx3 + 2];
            y2i = a[idx1 + 3] + a[idx3 + 3];
            y3r = a[idx1 + 2] - a[idx3 + 2];
            y3i = a[idx1 + 3] - a[idx3 + 3];
            a[idx5] = x0r + x2r;
            a[idx5 + 1] = x0i - x2i;
            a[idx5 + 2] = y0r + y2r;
            a[idx5 + 3] = y0i - y2i;
            a[idx1] = x0r - x2r;
            a[idx1 + 1] = x0i + x2i;
            a[idx1 + 2] = y0r - y2r;
            a[idx1 + 3] = y0i + y2i;
            x0r = x1r + x3i;
            x0i = x1i + x3r;
            a[idx2] = wk1r * x0r - wk1i * x0i;
            a[idx2 + 1] = wk1r * x0i + wk1i * x0r;
            x0r = y1r + y3i;
            x0i = y1i + y3r;
            a[idx2 + 2] = wd1r * x0r - wd1i * x0i;
            a[idx2 + 3] = wd1r * x0i + wd1i * x0r;
            x0r = x1r - x3i;
            x0i = x1i - x3r;
            a[idx3] = wk3r * x0r + wk3i * x0i;
            a[idx3 + 1] = wk3r * x0i - wk3i * x0r;
            x0r = y1r - y3i;
            x0i = y1i - y3r;
            a[idx3 + 2] = wd3r * x0r + wd3i * x0i;
            a[idx3 + 3] = wd3r * x0i - wd3i * x0r;
            j0 = m - j;
            j1 = j0 + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx0 = offa + j0;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            x0r = a[idx0] + a[idx2];
            x0i = -a[idx0 + 1] - a[idx2 + 1];
            x1r = a[idx0] - a[idx2];
            x1i = -a[idx0 + 1] + a[idx2 + 1];
            y0r = a[idx0 - 2] + a[idx2 - 2];
            y0i = -a[idx0 - 1] - a[idx2 - 1];
            y1r = a[idx0 - 2] - a[idx2 - 2];
            y1i = -a[idx0 - 1] + a[idx2 - 1];
            x2r = a[idx1] + a[idx3];
            x2i = a[idx1 + 1] + a[idx3 + 1];
            x3r = a[idx1] - a[idx3];
            x3i = a[idx1 + 1] - a[idx3 + 1];
            y2r = a[idx1 - 2] + a[idx3 - 2];
            y2i = a[idx1 - 1] + a[idx3 - 1];
            y3r = a[idx1 - 2] - a[idx3 - 2];
            y3i = a[idx1 - 1] - a[idx3 - 1];
            a[idx0] = x0r + x2r;
            a[idx0 + 1] = x0i - x2i;
            a[idx0 - 2] = y0r + y2r;
            a[idx0 - 1] = y0i - y2i;
            a[idx1] = x0r - x2r;
            a[idx1 + 1] = x0i + x2i;
            a[idx1 - 2] = y0r - y2r;
            a[idx1 - 1] = y0i + y2i;
            x0r = x1r + x3i;
            x0i = x1i + x3r;
            a[idx2] = wk1i * x0r - wk1r * x0i;
            a[idx2 + 1] = wk1i * x0i + wk1r * x0r;
            x0r = y1r + y3i;
            x0i = y1i + y3r;
            a[idx2 - 2] = wd1i * x0r - wd1r * x0i;
            a[idx2 - 1] = wd1i * x0i + wd1r * x0r;
            x0r = x1r - x3i;
            x0i = x1i - x3r;
            a[idx3] = wk3i * x0r + wk3r * x0i;
            a[idx3 + 1] = wk3i * x0i - wk3r * x0r;
            x0r = y1r - y3i;
            x0i = y1i - y3r;
            a[idx3 - 2] = wd3i * x0r + wd3r * x0i;
            a[idx3 - 1] = wd3i * x0i - wd3r * x0r;
        }
        wk1r = csc1 * (wd1r + wn4r);
        wk1i = csc1 * (wd1i + wn4r);
        wk3r = csc3 * (wd3r - wn4r);
        wk3i = csc3 * (wd3i - wn4r);
        j0 = mh;
        j1 = j0 + m;
        j2 = j1 + m;
        j3 = j2 + m;
        idx0 = offa + j0;
        idx1 = offa + j1;
        idx2 = offa + j2;
        idx3 = offa + j3;
        x0r = a[idx0 - 2] + a[idx2 - 2];
        x0i = -a[idx0 - 1] - a[idx2 - 1];
        x1r = a[idx0 - 2] - a[idx2 - 2];
        x1i = -a[idx0 - 1] + a[idx2 - 1];
        x2r = a[idx1 - 2] + a[idx3 - 2];
        x2i = a[idx1 - 1] + a[idx3 - 1];
        x3r = a[idx1 - 2] - a[idx3 - 2];
        x3i = a[idx1 - 1] - a[idx3 - 1];
        a[idx0 - 2] = x0r + x2r;
        a[idx0 - 1] = x0i - x2i;
        a[idx1 - 2] = x0r - x2r;
        a[idx1 - 1] = x0i + x2i;
        x0r = x1r + x3i;
        x0i = x1i + x3r;
        a[idx2 - 2] = wk1r * x0r - wk1i * x0i;
        a[idx2 - 1] = wk1r * x0i + wk1i * x0r;
        x0r = x1r - x3i;
        x0i = x1i - x3r;
        a[idx3 - 2] = wk3r * x0r + wk3i * x0i;
        a[idx3 - 1] = wk3r * x0i - wk3i * x0r;
        x0r = a[idx0] + a[idx2];
        x0i = -a[idx0 + 1] - a[idx2 + 1];
        x1r = a[idx0] - a[idx2];
        x1i = -a[idx0 + 1] + a[idx2 + 1];
        x2r = a[idx1] + a[idx3];
        x2i = a[idx1 + 1] + a[idx3 + 1];
        x3r = a[idx1] - a[idx3];
        x3i = a[idx1 + 1] - a[idx3 + 1];
        a[idx0] = x0r + x2r;
        a[idx0 + 1] = x0i - x2i;
        a[idx1] = x0r - x2r;
        a[idx1 + 1] = x0i + x2i;
        x0r = x1r + x3i;
        x0i = x1i + x3r;
        a[idx2] = wn4r * (x0r - x0i);
        a[idx2 + 1] = wn4r * (x0i + x0r);
        x0r = x1r - x3i;
        x0i = x1i - x3r;
        a[idx3] = -wn4r * (x0r + x0i);
        a[idx3 + 1] = -wn4r * (x0i - x0r);
        x0r = a[idx0 + 2] + a[idx2 + 2];
        x0i = -a[idx0 + 3] - a[idx2 + 3];
        x1r = a[idx0 + 2] - a[idx2 + 2];
        x1i = -a[idx0 + 3] + a[idx2 + 3];
        x2r = a[idx1 + 2] + a[idx3 + 2];
        x2i = a[idx1 + 3] + a[idx3 + 3];
        x3r = a[idx1 + 2] - a[idx3 + 2];
        x3i = a[idx1 + 3] - a[idx3 + 3];
        a[idx0 + 2] = x0r + x2r;
        a[idx0 + 3] = x0i - x2i;
        a[idx1 + 2] = x0r - x2r;
        a[idx1 + 3] = x0i + x2i;
        x0r = x1r + x3i;
        x0i = x1i + x3r;
        a[idx2 + 2] = wk1i * x0r - wk1r * x0i;
        a[idx2 + 3] = wk1i * x0i + wk1r * x0r;
        x0r = x1r - x3i;
        x0i = x1i - x3r;
        a[idx3 + 2] = wk3i * x0r + wk3r * x0i;
        a[idx3 + 3] = wk3i * x0i - wk3r * x0r;
    }

    private void cftrec4_th(final int n, final float[] a, final int offa, final int nw, final float[] w) {
        int i;
        int idiv4, m, nthreads;
        int idx = 0;
        nthreads = 2;
        idiv4 = 0;
        m = n >> 1;
        if (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads()) {
            nthreads = 4;
            idiv4 = 1;
            m >>= 1;
        }
        Future<?>[] futures = new Future[nthreads];
        final int mf = m;
        for (i = 0; i < nthreads; i++) {
            final int firstIdx = offa + i * m;
            if (i != idiv4) {
                futures[idx++] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        int isplt, j, k, m;
                        int idx1 = firstIdx + mf;
                        m = n;
                        while (m > 512) {
                            m >>= 2;
                            cftmdl1(m, a, idx1 - m, w, nw - (m >> 1));
                        }
                        cftleaf(m, 1, a, idx1 - m, nw, w);
                        k = 0;
                        int idx2 = firstIdx - m;
                        for (j = mf - m; j > 0; j -= m) {
                            k++;
                            isplt = cfttree(m, j, k, a, firstIdx, nw, w);
                            cftleaf(m, isplt, a, idx2 + j, nw, w);
                        }
                    }
                });
            } else {
                futures[idx++] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        int isplt, j, k, m;
                        int idx1 = firstIdx + mf;
                        k = 1;
                        m = n;
                        while (m > 512) {
                            m >>= 2;
                            k <<= 2;
                            cftmdl2(m, a, idx1 - m, w, nw - m);
                        }
                        cftleaf(m, 0, a, idx1 - m, nw, w);
                        k >>= 1;
                        int idx2 = firstIdx - m;
                        for (j = mf - m; j > 0; j -= m) {
                            k++;
                            isplt = cfttree(m, j, k, a, firstIdx, nw, w);
                            cftleaf(m, isplt, a, idx2 + j, nw, w);
                        }
                    }
                });
            }
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void cftrec4(int n, float[] a, int offa, int nw, float[] w) {
        int isplt, j, k, m;

        m = n;
        int idx1 = offa + n;
        while (m > 512) {
            m >>= 2;
            cftmdl1(m, a, idx1 - m, w, nw - (m >> 1));
        }
        cftleaf(m, 1, a, idx1 - m, nw, w);
        k = 0;
        int idx2 = offa - m;
        for (j = n - m; j > 0; j -= m) {
            k++;
            isplt = cfttree(m, j, k, a, offa, nw, w);
            cftleaf(m, isplt, a, idx2 + j, nw, w);
        }
    }

    private int cfttree(int n, int j, int k, float[] a, int offa, int nw, float[] w) {
        int i, isplt, m;
        int idx1 = offa - n;
        if ((k & 3) != 0) {
            isplt = k & 1;
            if (isplt != 0) {
                cftmdl1(n, a, idx1 + j, w, nw - (n >> 1));
            } else {
                cftmdl2(n, a, idx1 + j, w, nw - n);
            }
        } else {
            m = n;
            for (i = k; (i & 3) == 0; i >>= 2) {
                m <<= 2;
            }
            isplt = i & 1;
            int idx2 = offa + j;
            if (isplt != 0) {
                while (m > 128) {
                    cftmdl1(m, a, idx2 - m, w, nw - (m >> 1));
                    m >>= 2;
                }
            } else {
                while (m > 128) {
                    cftmdl2(m, a, idx2 - m, w, nw - m);
                    m >>= 2;
                }
            }
        }
        return isplt;
    }

    private void cftleaf(int n, int isplt, float[] a, int offa, int nw, float[] w) {
        if (n == 512) {
            cftmdl1(128, a, offa, w, nw - 64);
            cftf161(a, offa, w, nw - 8);
            cftf162(a, offa + 32, w, nw - 32);
            cftf161(a, offa + 64, w, nw - 8);
            cftf161(a, offa + 96, w, nw - 8);
            cftmdl2(128, a, offa + 128, w, nw - 128);
            cftf161(a, offa + 128, w, nw - 8);
            cftf162(a, offa + 160, w, nw - 32);
            cftf161(a, offa + 192, w, nw - 8);
            cftf162(a, offa + 224, w, nw - 32);
            cftmdl1(128, a, offa + 256, w, nw - 64);
            cftf161(a, offa + 256, w, nw - 8);
            cftf162(a, offa + 288, w, nw - 32);
            cftf161(a, offa + 320, w, nw - 8);
            cftf161(a, offa + 352, w, nw - 8);
            if (isplt != 0) {
                cftmdl1(128, a, offa + 384, w, nw - 64);
                cftf161(a, offa + 480, w, nw - 8);
            } else {
                cftmdl2(128, a, offa + 384, w, nw - 128);
                cftf162(a, offa + 480, w, nw - 32);
            }
            cftf161(a, offa + 384, w, nw - 8);
            cftf162(a, offa + 416, w, nw - 32);
            cftf161(a, offa + 448, w, nw - 8);
        } else {
            cftmdl1(64, a, offa, w, nw - 32);
            cftf081(a, offa, w, nw - 8);
            cftf082(a, offa + 16, w, nw - 8);
            cftf081(a, offa + 32, w, nw - 8);
            cftf081(a, offa + 48, w, nw - 8);
            cftmdl2(64, a, offa + 64, w, nw - 64);
            cftf081(a, offa + 64, w, nw - 8);
            cftf082(a, offa + 80, w, nw - 8);
            cftf081(a, offa + 96, w, nw - 8);
            cftf082(a, offa + 112, w, nw - 8);
            cftmdl1(64, a, offa + 128, w, nw - 32);
            cftf081(a, offa + 128, w, nw - 8);
            cftf082(a, offa + 144, w, nw - 8);
            cftf081(a, offa + 160, w, nw - 8);
            cftf081(a, offa + 176, w, nw - 8);
            if (isplt != 0) {
                cftmdl1(64, a, offa + 192, w, nw - 32);
                cftf081(a, offa + 240, w, nw - 8);
            } else {
                cftmdl2(64, a, offa + 192, w, nw - 64);
                cftf082(a, offa + 240, w, nw - 8);
            }
            cftf081(a, offa + 192, w, nw - 8);
            cftf082(a, offa + 208, w, nw - 8);
            cftf081(a, offa + 224, w, nw - 8);
        }
    }

    private void cftmdl1(int n, float[] a, int offa, float[] w, int startw) {
        int j0, j1, j2, j3, k, m, mh;
        float wn4r, wk1r, wk1i, wk3r, wk3i;
        float x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;
        int idx0, idx1, idx2, idx3, idx4, idx5;

        mh = n >> 3;
        m = 2 * mh;
        j1 = m;
        j2 = j1 + m;
        j3 = j2 + m;
        idx1 = offa + j1;
        idx2 = offa + j2;
        idx3 = offa + j3;
        x0r = a[offa] + a[idx2];
        x0i = a[offa + 1] + a[idx2 + 1];
        x1r = a[offa] - a[idx2];
        x1i = a[offa + 1] - a[idx2 + 1];
        x2r = a[idx1] + a[idx3];
        x2i = a[idx1 + 1] + a[idx3 + 1];
        x3r = a[idx1] - a[idx3];
        x3i = a[idx1 + 1] - a[idx3 + 1];
        a[offa] = x0r + x2r;
        a[offa + 1] = x0i + x2i;
        a[idx1] = x0r - x2r;
        a[idx1 + 1] = x0i - x2i;
        a[idx2] = x1r - x3i;
        a[idx2 + 1] = x1i + x3r;
        a[idx3] = x1r + x3i;
        a[idx3 + 1] = x1i - x3r;
        wn4r = w[startw + 1];
        k = 0;
        for (int j = 2; j < mh; j += 2) {
            k += 4;
            idx4 = startw + k;
            wk1r = w[idx4];
            wk1i = w[idx4 + 1];
            wk3r = w[idx4 + 2];
            wk3i = w[idx4 + 3];
            j1 = j + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            idx5 = offa + j;
            x0r = a[idx5] + a[idx2];
            x0i = a[idx5 + 1] + a[idx2 + 1];
            x1r = a[idx5] - a[idx2];
            x1i = a[idx5 + 1] - a[idx2 + 1];
            x2r = a[idx1] + a[idx3];
            x2i = a[idx1 + 1] + a[idx3 + 1];
            x3r = a[idx1] - a[idx3];
            x3i = a[idx1 + 1] - a[idx3 + 1];
            a[idx5] = x0r + x2r;
            a[idx5 + 1] = x0i + x2i;
            a[idx1] = x0r - x2r;
            a[idx1 + 1] = x0i - x2i;
            x0r = x1r - x3i;
            x0i = x1i + x3r;
            a[idx2] = wk1r * x0r - wk1i * x0i;
            a[idx2 + 1] = wk1r * x0i + wk1i * x0r;
            x0r = x1r + x3i;
            x0i = x1i - x3r;
            a[idx3] = wk3r * x0r + wk3i * x0i;
            a[idx3 + 1] = wk3r * x0i - wk3i * x0r;
            j0 = m - j;
            j1 = j0 + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx0 = offa + j0;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            x0r = a[idx0] + a[idx2];
            x0i = a[idx0 + 1] + a[idx2 + 1];
            x1r = a[idx0] - a[idx2];
            x1i = a[idx0 + 1] - a[idx2 + 1];
            x2r = a[idx1] + a[idx3];
            x2i = a[idx1 + 1] + a[idx3 + 1];
            x3r = a[idx1] - a[idx3];
            x3i = a[idx1 + 1] - a[idx3 + 1];
            a[idx0] = x0r + x2r;
            a[idx0 + 1] = x0i + x2i;
            a[idx1] = x0r - x2r;
            a[idx1 + 1] = x0i - x2i;
            x0r = x1r - x3i;
            x0i = x1i + x3r;
            a[idx2] = wk1i * x0r - wk1r * x0i;
            a[idx2 + 1] = wk1i * x0i + wk1r * x0r;
            x0r = x1r + x3i;
            x0i = x1i - x3r;
            a[idx3] = wk3i * x0r + wk3r * x0i;
            a[idx3 + 1] = wk3i * x0i - wk3r * x0r;
        }
        j0 = mh;
        j1 = j0 + m;
        j2 = j1 + m;
        j3 = j2 + m;
        idx0 = offa + j0;
        idx1 = offa + j1;
        idx2 = offa + j2;
        idx3 = offa + j3;
        x0r = a[idx0] + a[idx2];
        x0i = a[idx0 + 1] + a[idx2 + 1];
        x1r = a[idx0] - a[idx2];
        x1i = a[idx0 + 1] - a[idx2 + 1];
        x2r = a[idx1] + a[idx3];
        x2i = a[idx1 + 1] + a[idx3 + 1];
        x3r = a[idx1] - a[idx3];
        x3i = a[idx1 + 1] - a[idx3 + 1];
        a[idx0] = x0r + x2r;
        a[idx0 + 1] = x0i + x2i;
        a[idx1] = x0r - x2r;
        a[idx1 + 1] = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        a[idx2] = wn4r * (x0r - x0i);
        a[idx2 + 1] = wn4r * (x0i + x0r);
        x0r = x1r + x3i;
        x0i = x1i - x3r;
        a[idx3] = -wn4r * (x0r + x0i);
        a[idx3 + 1] = -wn4r * (x0i - x0r);
    }

    private void cftmdl2(int n, float[] a, int offa, float[] w, int startw) {
        int j0, j1, j2, j3, k, kr, m, mh;
        float wn4r, wk1r, wk1i, wk3r, wk3i, wd1r, wd1i, wd3r, wd3i;
        float x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i, y0r, y0i, y2r, y2i;
        int idx0, idx1, idx2, idx3, idx4, idx5, idx6;

        mh = n >> 3;
        m = 2 * mh;
        wn4r = w[startw + 1];
        j1 = m;
        j2 = j1 + m;
        j3 = j2 + m;
        idx1 = offa + j1;
        idx2 = offa + j2;
        idx3 = offa + j3;
        x0r = a[offa] - a[idx2 + 1];
        x0i = a[offa + 1] + a[idx2];
        x1r = a[offa] + a[idx2 + 1];
        x1i = a[offa + 1] - a[idx2];
        x2r = a[idx1] - a[idx3 + 1];
        x2i = a[idx1 + 1] + a[idx3];
        x3r = a[idx1] + a[idx3 + 1];
        x3i = a[idx1 + 1] - a[idx3];
        y0r = wn4r * (x2r - x2i);
        y0i = wn4r * (x2i + x2r);
        a[offa] = x0r + y0r;
        a[offa + 1] = x0i + y0i;
        a[idx1] = x0r - y0r;
        a[idx1 + 1] = x0i - y0i;
        y0r = wn4r * (x3r - x3i);
        y0i = wn4r * (x3i + x3r);
        a[idx2] = x1r - y0i;
        a[idx2 + 1] = x1i + y0r;
        a[idx3] = x1r + y0i;
        a[idx3 + 1] = x1i - y0r;
        k = 0;
        kr = 2 * m;
        for (int j = 2; j < mh; j += 2) {
            k += 4;
            idx4 = startw + k;
            wk1r = w[idx4];
            wk1i = w[idx4 + 1];
            wk3r = w[idx4 + 2];
            wk3i = w[idx4 + 3];
            kr -= 4;
            idx5 = startw + kr;
            wd1i = w[idx5];
            wd1r = w[idx5 + 1];
            wd3i = w[idx5 + 2];
            wd3r = w[idx5 + 3];
            j1 = j + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            idx6 = offa + j;
            x0r = a[idx6] - a[idx2 + 1];
            x0i = a[idx6 + 1] + a[idx2];
            x1r = a[idx6] + a[idx2 + 1];
            x1i = a[idx6 + 1] - a[idx2];
            x2r = a[idx1] - a[idx3 + 1];
            x2i = a[idx1 + 1] + a[idx3];
            x3r = a[idx1] + a[idx3 + 1];
            x3i = a[idx1 + 1] - a[idx3];
            y0r = wk1r * x0r - wk1i * x0i;
            y0i = wk1r * x0i + wk1i * x0r;
            y2r = wd1r * x2r - wd1i * x2i;
            y2i = wd1r * x2i + wd1i * x2r;
            a[idx6] = y0r + y2r;
            a[idx6 + 1] = y0i + y2i;
            a[idx1] = y0r - y2r;
            a[idx1 + 1] = y0i - y2i;
            y0r = wk3r * x1r + wk3i * x1i;
            y0i = wk3r * x1i - wk3i * x1r;
            y2r = wd3r * x3r + wd3i * x3i;
            y2i = wd3r * x3i - wd3i * x3r;
            a[idx2] = y0r + y2r;
            a[idx2 + 1] = y0i + y2i;
            a[idx3] = y0r - y2r;
            a[idx3 + 1] = y0i - y2i;
            j0 = m - j;
            j1 = j0 + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx0 = offa + j0;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            x0r = a[idx0] - a[idx2 + 1];
            x0i = a[idx0 + 1] + a[idx2];
            x1r = a[idx0] + a[idx2 + 1];
            x1i = a[idx0 + 1] - a[idx2];
            x2r = a[idx1] - a[idx3 + 1];
            x2i = a[idx1 + 1] + a[idx3];
            x3r = a[idx1] + a[idx3 + 1];
            x3i = a[idx1 + 1] - a[idx3];
            y0r = wd1i * x0r - wd1r * x0i;
            y0i = wd1i * x0i + wd1r * x0r;
            y2r = wk1i * x2r - wk1r * x2i;
            y2i = wk1i * x2i + wk1r * x2r;
            a[idx0] = y0r + y2r;
            a[idx0 + 1] = y0i + y2i;
            a[idx1] = y0r - y2r;
            a[idx1 + 1] = y0i - y2i;
            y0r = wd3i * x1r + wd3r * x1i;
            y0i = wd3i * x1i - wd3r * x1r;
            y2r = wk3i * x3r + wk3r * x3i;
            y2i = wk3i * x3i - wk3r * x3r;
            a[idx2] = y0r + y2r;
            a[idx2 + 1] = y0i + y2i;
            a[idx3] = y0r - y2r;
            a[idx3 + 1] = y0i - y2i;
        }
        wk1r = w[startw + m];
        wk1i = w[startw + m + 1];
        j0 = mh;
        j1 = j0 + m;
        j2 = j1 + m;
        j3 = j2 + m;
        idx0 = offa + j0;
        idx1 = offa + j1;
        idx2 = offa + j2;
        idx3 = offa + j3;
        x0r = a[idx0] - a[idx2 + 1];
        x0i = a[idx0 + 1] + a[idx2];
        x1r = a[idx0] + a[idx2 + 1];
        x1i = a[idx0 + 1] - a[idx2];
        x2r = a[idx1] - a[idx3 + 1];
        x2i = a[idx1 + 1] + a[idx3];
        x3r = a[idx1] + a[idx3 + 1];
        x3i = a[idx1 + 1] - a[idx3];
        y0r = wk1r * x0r - wk1i * x0i;
        y0i = wk1r * x0i + wk1i * x0r;
        y2r = wk1i * x2r - wk1r * x2i;
        y2i = wk1i * x2i + wk1r * x2r;
        a[idx0] = y0r + y2r;
        a[idx0 + 1] = y0i + y2i;
        a[idx1] = y0r - y2r;
        a[idx1 + 1] = y0i - y2i;
        y0r = wk1i * x1r - wk1r * x1i;
        y0i = wk1i * x1i + wk1r * x1r;
        y2r = wk1r * x3r - wk1i * x3i;
        y2i = wk1r * x3i + wk1i * x3r;
        a[idx2] = y0r - y2r;
        a[idx2 + 1] = y0i - y2i;
        a[idx3] = y0r + y2r;
        a[idx3 + 1] = y0i + y2i;
    }

    private void cftfx41(int n, float[] a, int offa, int nw, float[] w) {
        if (n == 128) {
            cftf161(a, offa, w, nw - 8);
            cftf162(a, offa + 32, w, nw - 32);
            cftf161(a, offa + 64, w, nw - 8);
            cftf161(a, offa + 96, w, nw - 8);
        } else {
            cftf081(a, offa, w, nw - 8);
            cftf082(a, offa + 16, w, nw - 8);
            cftf081(a, offa + 32, w, nw - 8);
            cftf081(a, offa + 48, w, nw - 8);
        }
    }

    private void cftf161(float[] a, int offa, float[] w, int startw) {
        float wn4r, wk1r, wk1i, x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i, y0r, y0i, y1r, y1i, y2r, y2i, y3r, y3i, y4r, y4i, y5r, y5i, y6r, y6i, y7r, y7i, y8r, y8i, y9r, y9i, y10r, y10i, y11r, y11i, y12r, y12i, y13r, y13i, y14r, y14i, y15r, y15i;

        wn4r = w[startw + 1];
        wk1r = w[startw + 2];
        wk1i = w[startw + 3];

        x0r = a[offa] + a[offa + 16];
        x0i = a[offa + 1] + a[offa + 17];
        x1r = a[offa] - a[offa + 16];
        x1i = a[offa + 1] - a[offa + 17];
        x2r = a[offa + 8] + a[offa + 24];
        x2i = a[offa + 9] + a[offa + 25];
        x3r = a[offa + 8] - a[offa + 24];
        x3i = a[offa + 9] - a[offa + 25];
        y0r = x0r + x2r;
        y0i = x0i + x2i;
        y4r = x0r - x2r;
        y4i = x0i - x2i;
        y8r = x1r - x3i;
        y8i = x1i + x3r;
        y12r = x1r + x3i;
        y12i = x1i - x3r;
        x0r = a[offa + 2] + a[offa + 18];
        x0i = a[offa + 3] + a[offa + 19];
        x1r = a[offa + 2] - a[offa + 18];
        x1i = a[offa + 3] - a[offa + 19];
        x2r = a[offa + 10] + a[offa + 26];
        x2i = a[offa + 11] + a[offa + 27];
        x3r = a[offa + 10] - a[offa + 26];
        x3i = a[offa + 11] - a[offa + 27];
        y1r = x0r + x2r;
        y1i = x0i + x2i;
        y5r = x0r - x2r;
        y5i = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        y9r = wk1r * x0r - wk1i * x0i;
        y9i = wk1r * x0i + wk1i * x0r;
        x0r = x1r + x3i;
        x0i = x1i - x3r;
        y13r = wk1i * x0r - wk1r * x0i;
        y13i = wk1i * x0i + wk1r * x0r;
        x0r = a[offa + 4] + a[offa + 20];
        x0i = a[offa + 5] + a[offa + 21];
        x1r = a[offa + 4] - a[offa + 20];
        x1i = a[offa + 5] - a[offa + 21];
        x2r = a[offa + 12] + a[offa + 28];
        x2i = a[offa + 13] + a[offa + 29];
        x3r = a[offa + 12] - a[offa + 28];
        x3i = a[offa + 13] - a[offa + 29];
        y2r = x0r + x2r;
        y2i = x0i + x2i;
        y6r = x0r - x2r;
        y6i = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        y10r = wn4r * (x0r - x0i);
        y10i = wn4r * (x0i + x0r);
        x0r = x1r + x3i;
        x0i = x1i - x3r;
        y14r = wn4r * (x0r + x0i);
        y14i = wn4r * (x0i - x0r);
        x0r = a[offa + 6] + a[offa + 22];
        x0i = a[offa + 7] + a[offa + 23];
        x1r = a[offa + 6] - a[offa + 22];
        x1i = a[offa + 7] - a[offa + 23];
        x2r = a[offa + 14] + a[offa + 30];
        x2i = a[offa + 15] + a[offa + 31];
        x3r = a[offa + 14] - a[offa + 30];
        x3i = a[offa + 15] - a[offa + 31];
        y3r = x0r + x2r;
        y3i = x0i + x2i;
        y7r = x0r - x2r;
        y7i = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        y11r = wk1i * x0r - wk1r * x0i;
        y11i = wk1i * x0i + wk1r * x0r;
        x0r = x1r + x3i;
        x0i = x1i - x3r;
        y15r = wk1r * x0r - wk1i * x0i;
        y15i = wk1r * x0i + wk1i * x0r;
        x0r = y12r - y14r;
        x0i = y12i - y14i;
        x1r = y12r + y14r;
        x1i = y12i + y14i;
        x2r = y13r - y15r;
        x2i = y13i - y15i;
        x3r = y13r + y15r;
        x3i = y13i + y15i;
        a[offa + 24] = x0r + x2r;
        a[offa + 25] = x0i + x2i;
        a[offa + 26] = x0r - x2r;
        a[offa + 27] = x0i - x2i;
        a[offa + 28] = x1r - x3i;
        a[offa + 29] = x1i + x3r;
        a[offa + 30] = x1r + x3i;
        a[offa + 31] = x1i - x3r;
        x0r = y8r + y10r;
        x0i = y8i + y10i;
        x1r = y8r - y10r;
        x1i = y8i - y10i;
        x2r = y9r + y11r;
        x2i = y9i + y11i;
        x3r = y9r - y11r;
        x3i = y9i - y11i;
        a[offa + 16] = x0r + x2r;
        a[offa + 17] = x0i + x2i;
        a[offa + 18] = x0r - x2r;
        a[offa + 19] = x0i - x2i;
        a[offa + 20] = x1r - x3i;
        a[offa + 21] = x1i + x3r;
        a[offa + 22] = x1r + x3i;
        a[offa + 23] = x1i - x3r;
        x0r = y5r - y7i;
        x0i = y5i + y7r;
        x2r = wn4r * (x0r - x0i);
        x2i = wn4r * (x0i + x0r);
        x0r = y5r + y7i;
        x0i = y5i - y7r;
        x3r = wn4r * (x0r - x0i);
        x3i = wn4r * (x0i + x0r);
        x0r = y4r - y6i;
        x0i = y4i + y6r;
        x1r = y4r + y6i;
        x1i = y4i - y6r;
        a[offa + 8] = x0r + x2r;
        a[offa + 9] = x0i + x2i;
        a[offa + 10] = x0r - x2r;
        a[offa + 11] = x0i - x2i;
        a[offa + 12] = x1r - x3i;
        a[offa + 13] = x1i + x3r;
        a[offa + 14] = x1r + x3i;
        a[offa + 15] = x1i - x3r;
        x0r = y0r + y2r;
        x0i = y0i + y2i;
        x1r = y0r - y2r;
        x1i = y0i - y2i;
        x2r = y1r + y3r;
        x2i = y1i + y3i;
        x3r = y1r - y3r;
        x3i = y1i - y3i;
        a[offa] = x0r + x2r;
        a[offa + 1] = x0i + x2i;
        a[offa + 2] = x0r - x2r;
        a[offa + 3] = x0i - x2i;
        a[offa + 4] = x1r - x3i;
        a[offa + 5] = x1i + x3r;
        a[offa + 6] = x1r + x3i;
        a[offa + 7] = x1i - x3r;
    }

    private void cftf162(float[] a, int offa, float[] w, int startw) {
        float wn4r, wk1r, wk1i, wk2r, wk2i, wk3r, wk3i, x0r, x0i, x1r, x1i, x2r, x2i, y0r, y0i, y1r, y1i, y2r, y2i, y3r, y3i, y4r, y4i, y5r, y5i, y6r, y6i, y7r, y7i, y8r, y8i, y9r, y9i, y10r, y10i, y11r, y11i, y12r, y12i, y13r, y13i, y14r, y14i, y15r, y15i;

        wn4r = w[startw + 1];
        wk1r = w[startw + 4];
        wk1i = w[startw + 5];
        wk3r = w[startw + 6];
        wk3i = -w[startw + 7];
        wk2r = w[startw + 8];
        wk2i = w[startw + 9];
        x1r = a[offa] - a[offa + 17];
        x1i = a[offa + 1] + a[offa + 16];
        x0r = a[offa + 8] - a[offa + 25];
        x0i = a[offa + 9] + a[offa + 24];
        x2r = wn4r * (x0r - x0i);
        x2i = wn4r * (x0i + x0r);
        y0r = x1r + x2r;
        y0i = x1i + x2i;
        y4r = x1r - x2r;
        y4i = x1i - x2i;
        x1r = a[offa] + a[offa + 17];
        x1i = a[offa + 1] - a[offa + 16];
        x0r = a[offa + 8] + a[offa + 25];
        x0i = a[offa + 9] - a[offa + 24];
        x2r = wn4r * (x0r - x0i);
        x2i = wn4r * (x0i + x0r);
        y8r = x1r - x2i;
        y8i = x1i + x2r;
        y12r = x1r + x2i;
        y12i = x1i - x2r;
        x0r = a[offa + 2] - a[offa + 19];
        x0i = a[offa + 3] + a[offa + 18];
        x1r = wk1r * x0r - wk1i * x0i;
        x1i = wk1r * x0i + wk1i * x0r;
        x0r = a[offa + 10] - a[offa + 27];
        x0i = a[offa + 11] + a[offa + 26];
        x2r = wk3i * x0r - wk3r * x0i;
        x2i = wk3i * x0i + wk3r * x0r;
        y1r = x1r + x2r;
        y1i = x1i + x2i;
        y5r = x1r - x2r;
        y5i = x1i - x2i;
        x0r = a[offa + 2] + a[offa + 19];
        x0i = a[offa + 3] - a[offa + 18];
        x1r = wk3r * x0r - wk3i * x0i;
        x1i = wk3r * x0i + wk3i * x0r;
        x0r = a[offa + 10] + a[offa + 27];
        x0i = a[offa + 11] - a[offa + 26];
        x2r = wk1r * x0r + wk1i * x0i;
        x2i = wk1r * x0i - wk1i * x0r;
        y9r = x1r - x2r;
        y9i = x1i - x2i;
        y13r = x1r + x2r;
        y13i = x1i + x2i;
        x0r = a[offa + 4] - a[offa + 21];
        x0i = a[offa + 5] + a[offa + 20];
        x1r = wk2r * x0r - wk2i * x0i;
        x1i = wk2r * x0i + wk2i * x0r;
        x0r = a[offa + 12] - a[offa + 29];
        x0i = a[offa + 13] + a[offa + 28];
        x2r = wk2i * x0r - wk2r * x0i;
        x2i = wk2i * x0i + wk2r * x0r;
        y2r = x1r + x2r;
        y2i = x1i + x2i;
        y6r = x1r - x2r;
        y6i = x1i - x2i;
        x0r = a[offa + 4] + a[offa + 21];
        x0i = a[offa + 5] - a[offa + 20];
        x1r = wk2i * x0r - wk2r * x0i;
        x1i = wk2i * x0i + wk2r * x0r;
        x0r = a[offa + 12] + a[offa + 29];
        x0i = a[offa + 13] - a[offa + 28];
        x2r = wk2r * x0r - wk2i * x0i;
        x2i = wk2r * x0i + wk2i * x0r;
        y10r = x1r - x2r;
        y10i = x1i - x2i;
        y14r = x1r + x2r;
        y14i = x1i + x2i;
        x0r = a[offa + 6] - a[offa + 23];
        x0i = a[offa + 7] + a[offa + 22];
        x1r = wk3r * x0r - wk3i * x0i;
        x1i = wk3r * x0i + wk3i * x0r;
        x0r = a[offa + 14] - a[offa + 31];
        x0i = a[offa + 15] + a[offa + 30];
        x2r = wk1i * x0r - wk1r * x0i;
        x2i = wk1i * x0i + wk1r * x0r;
        y3r = x1r + x2r;
        y3i = x1i + x2i;
        y7r = x1r - x2r;
        y7i = x1i - x2i;
        x0r = a[offa + 6] + a[offa + 23];
        x0i = a[offa + 7] - a[offa + 22];
        x1r = wk1i * x0r + wk1r * x0i;
        x1i = wk1i * x0i - wk1r * x0r;
        x0r = a[offa + 14] + a[offa + 31];
        x0i = a[offa + 15] - a[offa + 30];
        x2r = wk3i * x0r - wk3r * x0i;
        x2i = wk3i * x0i + wk3r * x0r;
        y11r = x1r + x2r;
        y11i = x1i + x2i;
        y15r = x1r - x2r;
        y15i = x1i - x2i;
        x1r = y0r + y2r;
        x1i = y0i + y2i;
        x2r = y1r + y3r;
        x2i = y1i + y3i;
        a[offa] = x1r + x2r;
        a[offa + 1] = x1i + x2i;
        a[offa + 2] = x1r - x2r;
        a[offa + 3] = x1i - x2i;
        x1r = y0r - y2r;
        x1i = y0i - y2i;
        x2r = y1r - y3r;
        x2i = y1i - y3i;
        a[offa + 4] = x1r - x2i;
        a[offa + 5] = x1i + x2r;
        a[offa + 6] = x1r + x2i;
        a[offa + 7] = x1i - x2r;
        x1r = y4r - y6i;
        x1i = y4i + y6r;
        x0r = y5r - y7i;
        x0i = y5i + y7r;
        x2r = wn4r * (x0r - x0i);
        x2i = wn4r * (x0i + x0r);
        a[offa + 8] = x1r + x2r;
        a[offa + 9] = x1i + x2i;
        a[offa + 10] = x1r - x2r;
        a[offa + 11] = x1i - x2i;
        x1r = y4r + y6i;
        x1i = y4i - y6r;
        x0r = y5r + y7i;
        x0i = y5i - y7r;
        x2r = wn4r * (x0r - x0i);
        x2i = wn4r * (x0i + x0r);
        a[offa + 12] = x1r - x2i;
        a[offa + 13] = x1i + x2r;
        a[offa + 14] = x1r + x2i;
        a[offa + 15] = x1i - x2r;
        x1r = y8r + y10r;
        x1i = y8i + y10i;
        x2r = y9r - y11r;
        x2i = y9i - y11i;
        a[offa + 16] = x1r + x2r;
        a[offa + 17] = x1i + x2i;
        a[offa + 18] = x1r - x2r;
        a[offa + 19] = x1i - x2i;
        x1r = y8r - y10r;
        x1i = y8i - y10i;
        x2r = y9r + y11r;
        x2i = y9i + y11i;
        a[offa + 20] = x1r - x2i;
        a[offa + 21] = x1i + x2r;
        a[offa + 22] = x1r + x2i;
        a[offa + 23] = x1i - x2r;
        x1r = y12r - y14i;
        x1i = y12i + y14r;
        x0r = y13r + y15i;
        x0i = y13i - y15r;
        x2r = wn4r * (x0r - x0i);
        x2i = wn4r * (x0i + x0r);
        a[offa + 24] = x1r + x2r;
        a[offa + 25] = x1i + x2i;
        a[offa + 26] = x1r - x2r;
        a[offa + 27] = x1i - x2i;
        x1r = y12r + y14i;
        x1i = y12i - y14r;
        x0r = y13r - y15i;
        x0i = y13i + y15r;
        x2r = wn4r * (x0r - x0i);
        x2i = wn4r * (x0i + x0r);
        a[offa + 28] = x1r - x2i;
        a[offa + 29] = x1i + x2r;
        a[offa + 30] = x1r + x2i;
        a[offa + 31] = x1i - x2r;
    }

    private void cftf081(float[] a, int offa, float[] w, int startw) {
        float wn4r, x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i, y0r, y0i, y1r, y1i, y2r, y2i, y3r, y3i, y4r, y4i, y5r, y5i, y6r, y6i, y7r, y7i;

        wn4r = w[startw + 1];
        x0r = a[offa] + a[offa + 8];
        x0i = a[offa + 1] + a[offa + 9];
        x1r = a[offa] - a[offa + 8];
        x1i = a[offa + 1] - a[offa + 9];
        x2r = a[offa + 4] + a[offa + 12];
        x2i = a[offa + 5] + a[offa + 13];
        x3r = a[offa + 4] - a[offa + 12];
        x3i = a[offa + 5] - a[offa + 13];
        y0r = x0r + x2r;
        y0i = x0i + x2i;
        y2r = x0r - x2r;
        y2i = x0i - x2i;
        y1r = x1r - x3i;
        y1i = x1i + x3r;
        y3r = x1r + x3i;
        y3i = x1i - x3r;
        x0r = a[offa + 2] + a[offa + 10];
        x0i = a[offa + 3] + a[offa + 11];
        x1r = a[offa + 2] - a[offa + 10];
        x1i = a[offa + 3] - a[offa + 11];
        x2r = a[offa + 6] + a[offa + 14];
        x2i = a[offa + 7] + a[offa + 15];
        x3r = a[offa + 6] - a[offa + 14];
        x3i = a[offa + 7] - a[offa + 15];
        y4r = x0r + x2r;
        y4i = x0i + x2i;
        y6r = x0r - x2r;
        y6i = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        x2r = x1r + x3i;
        x2i = x1i - x3r;
        y5r = wn4r * (x0r - x0i);
        y5i = wn4r * (x0r + x0i);
        y7r = wn4r * (x2r - x2i);
        y7i = wn4r * (x2r + x2i);
        a[offa + 8] = y1r + y5r;
        a[offa + 9] = y1i + y5i;
        a[offa + 10] = y1r - y5r;
        a[offa + 11] = y1i - y5i;
        a[offa + 12] = y3r - y7i;
        a[offa + 13] = y3i + y7r;
        a[offa + 14] = y3r + y7i;
        a[offa + 15] = y3i - y7r;
        a[offa] = y0r + y4r;
        a[offa + 1] = y0i + y4i;
        a[offa + 2] = y0r - y4r;
        a[offa + 3] = y0i - y4i;
        a[offa + 4] = y2r - y6i;
        a[offa + 5] = y2i + y6r;
        a[offa + 6] = y2r + y6i;
        a[offa + 7] = y2i - y6r;
    }

    private void cftf082(float[] a, int offa, float[] w, int startw) {
        float wn4r, wk1r, wk1i, x0r, x0i, x1r, x1i, y0r, y0i, y1r, y1i, y2r, y2i, y3r, y3i, y4r, y4i, y5r, y5i, y6r, y6i, y7r, y7i;

        wn4r = w[startw + 1];
        wk1r = w[startw + 2];
        wk1i = w[startw + 3];
        y0r = a[offa] - a[offa + 9];
        y0i = a[offa + 1] + a[offa + 8];
        y1r = a[offa] + a[offa + 9];
        y1i = a[offa + 1] - a[offa + 8];
        x0r = a[offa + 4] - a[offa + 13];
        x0i = a[offa + 5] + a[offa + 12];
        y2r = wn4r * (x0r - x0i);
        y2i = wn4r * (x0i + x0r);
        x0r = a[offa + 4] + a[offa + 13];
        x0i = a[offa + 5] - a[offa + 12];
        y3r = wn4r * (x0r - x0i);
        y3i = wn4r * (x0i + x0r);
        x0r = a[offa + 2] - a[offa + 11];
        x0i = a[offa + 3] + a[offa + 10];
        y4r = wk1r * x0r - wk1i * x0i;
        y4i = wk1r * x0i + wk1i * x0r;
        x0r = a[offa + 2] + a[offa + 11];
        x0i = a[offa + 3] - a[offa + 10];
        y5r = wk1i * x0r - wk1r * x0i;
        y5i = wk1i * x0i + wk1r * x0r;
        x0r = a[offa + 6] - a[offa + 15];
        x0i = a[offa + 7] + a[offa + 14];
        y6r = wk1i * x0r - wk1r * x0i;
        y6i = wk1i * x0i + wk1r * x0r;
        x0r = a[offa + 6] + a[offa + 15];
        x0i = a[offa + 7] - a[offa + 14];
        y7r = wk1r * x0r - wk1i * x0i;
        y7i = wk1r * x0i + wk1i * x0r;
        x0r = y0r + y2r;
        x0i = y0i + y2i;
        x1r = y4r + y6r;
        x1i = y4i + y6i;
        a[offa] = x0r + x1r;
        a[offa + 1] = x0i + x1i;
        a[offa + 2] = x0r - x1r;
        a[offa + 3] = x0i - x1i;
        x0r = y0r - y2r;
        x0i = y0i - y2i;
        x1r = y4r - y6r;
        x1i = y4i - y6i;
        a[offa + 4] = x0r - x1i;
        a[offa + 5] = x0i + x1r;
        a[offa + 6] = x0r + x1i;
        a[offa + 7] = x0i - x1r;
        x0r = y1r - y3i;
        x0i = y1i + y3r;
        x1r = y5r - y7r;
        x1i = y5i - y7i;
        a[offa + 8] = x0r + x1r;
        a[offa + 9] = x0i + x1i;
        a[offa + 10] = x0r - x1r;
        a[offa + 11] = x0i - x1i;
        x0r = y1r + y3i;
        x0i = y1i - y3r;
        x1r = y5r + y7r;
        x1i = y5i + y7i;
        a[offa + 12] = x0r - x1i;
        a[offa + 13] = x0i + x1r;
        a[offa + 14] = x0r + x1i;
        a[offa + 15] = x0i - x1r;
    }

    private void cftf040(float[] a, int offa) {
        float x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;

        x0r = a[offa] + a[offa + 4];
        x0i = a[offa + 1] + a[offa + 5];
        x1r = a[offa] - a[offa + 4];
        x1i = a[offa + 1] - a[offa + 5];
        x2r = a[offa + 2] + a[offa + 6];
        x2i = a[offa + 3] + a[offa + 7];
        x3r = a[offa + 2] - a[offa + 6];
        x3i = a[offa + 3] - a[offa + 7];
        a[offa] = x0r + x2r;
        a[offa + 1] = x0i + x2i;
        a[offa + 2] = x1r - x3i;
        a[offa + 3] = x1i + x3r;
        a[offa + 4] = x0r - x2r;
        a[offa + 5] = x0i - x2i;
        a[offa + 6] = x1r + x3i;
        a[offa + 7] = x1i - x3r;
    }

    private void cftb040(float[] a, int offa) {
        float x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;

        x0r = a[offa] + a[offa + 4];
        x0i = a[offa + 1] + a[offa + 5];
        x1r = a[offa] - a[offa + 4];
        x1i = a[offa + 1] - a[offa + 5];
        x2r = a[offa + 2] + a[offa + 6];
        x2i = a[offa + 3] + a[offa + 7];
        x3r = a[offa + 2] - a[offa + 6];
        x3i = a[offa + 3] - a[offa + 7];
        a[offa] = x0r + x2r;
        a[offa + 1] = x0i + x2i;
        a[offa + 2] = x1r + x3i;
        a[offa + 3] = x1i - x3r;
        a[offa + 4] = x0r - x2r;
        a[offa + 5] = x0i - x2i;
        a[offa + 6] = x1r - x3i;
        a[offa + 7] = x1i + x3r;
    }

    private void cftx020(float[] a, int offa) {
        float x0r, x0i;
        x0r = a[offa] - a[offa + 2];
        x0i = -a[offa + 1] + a[offa + 3];
        a[offa] += a[offa + 2];
        a[offa + 1] += a[offa + 3];
        a[offa + 2] = x0r;
        a[offa + 3] = x0i;
    }

    private void cftxb020(float[] a, int offa) {
        float x0r, x0i;

        x0r = a[offa] - a[offa + 2];
        x0i = a[offa + 1] - a[offa + 3];
        a[offa] += a[offa + 2];
        a[offa + 1] += a[offa + 3];
        a[offa + 2] = x0r;
        a[offa + 3] = x0i;
    }

    private void cftxc020(float[] a, int offa) {
        float x0r, x0i;
        x0r = a[offa] - a[offa + 2];
        x0i = a[offa + 1] + a[offa + 3];
        a[offa] += a[offa + 2];
        a[offa + 1] -= a[offa + 3];
        a[offa + 2] = x0r;
        a[offa + 3] = x0i;
    }

    private void rftfsub(int n, float[] a, int offa, int nc, float[] c, int startc) {
        int k, kk, ks, m;
        float wkr, wki, xr, xi, yr, yi;
        int idx1, idx2;

        m = n >> 1;
        ks = 2 * nc / m;
        kk = 0;
        for (int j = 2; j < m; j += 2) {
            k = n - j;
            kk += ks;
            wkr = (float)(0.5 - c[startc + nc - kk]);
            wki = c[startc + kk];
            idx1 = offa + j;
            idx2 = offa + k;
            xr = a[idx1] - a[idx2];
            xi = a[idx1 + 1] + a[idx2 + 1];
            yr = wkr * xr - wki * xi;
            yi = wkr * xi + wki * xr;
            a[idx1] -= yr;
            a[idx1 + 1] = yi - a[idx1 + 1];
            a[idx2] += yr;
            a[idx2 + 1] = yi - a[idx2 + 1];
        }
        a[offa + m + 1] = -a[offa + m + 1];
    }

    private void rftbsub(int n, float[] a, int offa, int nc, float[] c, int startc) {
        int k, kk, ks, m;
        float wkr, wki, xr, xi, yr, yi;
        int idx1, idx2;

        m = n >> 1;
        ks = 2 * nc / m;
        kk = 0;
        for (int j = 2; j < m; j += 2) {
            k = n - j;
            kk += ks;
            wkr = (float)(0.5 - c[startc + nc - kk]);
            wki = c[startc + kk];
            idx1 = offa + j;
            idx2 = offa + k;
            xr = a[idx1] - a[idx2];
            xi = a[idx1 + 1] + a[idx2 + 1];
            yr = wkr * xr - wki * xi;
            yi = wkr * xi + wki * xr;
            a[idx1] -= yr;
            a[idx1 + 1] -= yi;
            a[idx2] += yr;
            a[idx2 + 1] -= yi;
        }
    }

    private void scale(final float m, final float[] a, int offa, boolean complex) {
        final float norm = (float)(1.0 / m);
        int n2;
        if (complex) {
            n2 = 2 * n;
        } else {
            n2 = n;
        }
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && (n2 >= ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
            final int k = n2 / nthreads;
            Future<?>[] futures = new Future[nthreads];
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = offa + i * k;
                final int lastIdx = (i == (nthreads - 1)) ? offa + n2 : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int i = firstIdx; i < lastIdx; i++) {
                            a[i] *= norm;
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {
            for (int i = offa; i < offa + n2; i++) {
                a[i] *= norm;
            }

        }
    }
}
