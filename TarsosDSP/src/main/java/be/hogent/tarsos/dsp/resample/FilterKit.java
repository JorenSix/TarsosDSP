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
/******************************************************************************
 *
 * libresample4j
 * Copyright (c) 2009 Laszlo Systems, Inc. All Rights Reserved.
 *
 * libresample4j is a Java port of Dominic Mazzoni's libresample 0.1.3,
 * which is in turn based on Julius Smith's Resample 1.7 library.
 *      http://www-ccrma.stanford.edu/~jos/resample/
 *
 * License: LGPL -- see the file LICENSE.txt for more information
 *
 *****************************************************************************/
package be.hogent.tarsos.dsp.resample;

/**
 * This file provides Kaiser-windowed low-pass filter support,
 * including a function to create the filter coefficients, and
 * two functions to apply the filter at a particular point.
 * 
 * <pre>
 * reference: "Digital Filters, 2nd edition"
 *            R.W. Hamming, pp. 178-179
 *
 * Izero() computes the 0th order modified bessel function of the first kind.
 *    (Needed to compute Kaiser window).
 *
 * LpFilter() computes the coeffs of a Kaiser-windowed low pass filter with
 *    the following characteristics:
 *
 *       c[]  = array in which to store computed coeffs
 *       frq  = roll-off frequency of filter
 *       N    = Half the window length in number of coeffs
 *       Beta = parameter of Kaiser window
 *       Num  = number of coeffs before 1/frq
 *
 * Beta trades the rejection of the lowpass filter against the transition
 *    width from passband to stopband.  Larger Beta means a slower
 *    transition and greater stopband rejection.  See Rabiner and Gold
 *    (Theory and Application of DSP) under Kaiser windows for more about
 *    Beta.  The following table from Rabiner and Gold gives some feel
 *    for the effect of Beta:
 *
 * All ripples in dB, width of transition band = D*N where N = window length
 *
 *               BETA    D       PB RIP   SB RIP
 *               2.120   1.50  +-0.27      -30
 *               3.384   2.23    0.0864    -40
 *               4.538   2.93    0.0274    -50
 *               5.658   3.62    0.00868   -60
 *               6.764   4.32    0.00275   -70
 *               7.865   5.0     0.000868  -80
 *               8.960   5.7     0.000275  -90
 *               10.056  6.4     0.000087  -100
 * </pre>
 */
class FilterKit {

    // Max error acceptable in Izero
    private static final double IzeroEPSILON = 1E-21;

    private static double Izero(double x) {
        double sum, u, halfx, temp;
        int n;

        sum = u = n = 1;
        halfx = x / 2.0;
        do {
            temp = halfx / (double) n;
            n += 1;
            temp *= temp;
            u *= temp;
            sum += u;
        } while (u >= IzeroEPSILON * sum);
        return (sum);
    }

    public static void lrsLpFilter(double c[], int N, double frq, double Beta, int Num) {
        double IBeta, temp, temp1, inm1;
        int i;

        // Calculate ideal lowpass filter impulse response coefficients:
        c[0] = 2.0 * frq;
        for (i = 1; i < N; i++) {
            temp = Math.PI * (double) i / (double) Num;
            c[i] = Math.sin(2.0 * temp * frq) / temp; // Analog sinc function,
            // cutoff = frq
        }

        /*
         * Calculate and Apply Kaiser window to ideal lowpass filter. Note: last
         * window value is IBeta which is NOT zero. You're supposed to really
         * truncate the window here, not ramp it to zero. This helps reduce the
         * first sidelobe.
         */
        IBeta = 1.0 / Izero(Beta);
        inm1 = 1.0 / ((double) (N - 1));
        for (i = 1; i < N; i++) {
            temp = (double) i * inm1;
            temp1 = 1.0 - temp * temp;
            temp1 = (temp1 < 0 ? 0 : temp1); /*
                                              * make sure it's not negative
                                              * since we're taking the square
                                              * root - this happens on Pentium
                                              * 4's due to tiny roundoff errors
                                              */
            c[i] *= Izero(Beta * Math.sqrt(temp1)) * IBeta;
        }
    }

    /**
     * 
     * @param Imp impulse response
     * @param ImpD impulse response deltas
     * @param Nwing length of one wing of filter
     * @param Interp Interpolate coefs using deltas?
     * @param Xp_array Current sample array
     * @param Xp_index Current sample index
     * @param Ph Phase
     * @param Inc increment (1 for right wing or -1 for left)
     * @return v.
     */
    public static float lrsFilterUp(float Imp[], float ImpD[], int Nwing, boolean Interp, float[] Xp_array, int Xp_index, double Ph,
            int Inc) {
        double a = 0;
        float v, t;

        Ph *= Resampler.Npc; // Npc is number of values per 1/delta in impulse
        // response

        v = 0.0f; // The output value

        float[] Hp_array = Imp;
        int Hp_index = (int) Ph;

        int End_index = Nwing;

        float[] Hdp_array = ImpD;
        int Hdp_index = (int) Ph;

        if (Interp) {
            // Hdp = &ImpD[(int)Ph];
            a = Ph - Math.floor(Ph); /* fractional part of Phase */
        }

        if (Inc == 1) // If doing right wing...
        { // ...drop extra coeff, so when Ph is
            End_index--; // 0.5, we don't do too many mult's
            if (Ph == 0) // If the phase is zero...
            { // ...then we've already skipped the
                Hp_index += Resampler.Npc; // first sample, so we must also
                Hdp_index += Resampler.Npc; // skip ahead in Imp[] and ImpD[]
            }
        }

        if (Interp)
            while (Hp_index < End_index) {
                t = Hp_array[Hp_index]; /* Get filter coeff */
                t += Hdp_array[Hdp_index] * a; /* t is now interp'd filter coeff */
                Hdp_index += Resampler.Npc; /* Filter coeff differences step */
                t *= Xp_array[Xp_index]; /* Mult coeff by input sample */
                v += t; /* The filter output */
                Hp_index += Resampler.Npc; /* Filter coeff step */
                Xp_index += Inc; /* Input signal step. NO CHECK ON BOUNDS */
            }
        else
            while (Hp_index < End_index) {
                t = Hp_array[Hp_index]; /* Get filter coeff */
                t *= Xp_array[Xp_index]; /* Mult coeff by input sample */
                v += t; /* The filter output */
                Hp_index += Resampler.Npc; /* Filter coeff step */
                Xp_index += Inc; /* Input signal step. NO CHECK ON BOUNDS */
            }

        return v;
    }

    /**
     * 
     * @param Imp impulse response
     * @param ImpD impulse response deltas
     * @param Nwing length of one wing of filter
     * @param Interp Interpolate coefs using deltas?
     * @param Xp_array Current sample array
     * @param Xp_index Current sample index
     * @param Ph Phase
     * @param Inc increment (1 for right wing or -1 for left)
     * @param dhb filter sampling period
     * @return v.
     */
    public static float lrsFilterUD(float Imp[], float ImpD[], int Nwing, boolean Interp, float[] Xp_array, int Xp_index, double Ph,
            int Inc, double dhb) {
        float a;
        float v, t;
        double Ho;

        v = 0.0f; // The output value
        Ho = Ph * dhb;

        int End_index = Nwing;

        if (Inc == 1) // If doing right wing...
        { // ...drop extra coeff, so when Ph is
            End_index--; // 0.5, we don't do too many mult's
            if (Ph == 0) // If the phase is zero...
                Ho += dhb; // ...then we've already skipped the
        } // first sample, so we must also
        // skip ahead in Imp[] and ImpD[]

        float[] Hp_array = Imp;
        int Hp_index;

        if (Interp) {
            float[] Hdp_array = ImpD;
            int Hdp_index;

            while ((Hp_index = (int) Ho) < End_index) {
                t = Hp_array[Hp_index]; // Get IR sample
                Hdp_index = (int) Ho; // get interp bits from diff table
                a = (float) (Ho - Math.floor(Ho)); // a is logically between 0
                                                   // and 1
                t += Hdp_array[Hdp_index] * a; // t is now interp'd filter coeff
                t *= Xp_array[Xp_index]; // Mult coeff by input sample
                v += t; // The filter output
                Ho += dhb; // IR step
                Xp_index += Inc; // Input signal step. NO CHECK ON BOUNDS
            }
        } else {
            while ((Hp_index = (int) Ho) < End_index) {
                t = Hp_array[Hp_index]; // Get IR sample
                t *= Xp_array[Xp_index]; // Mult coeff by input sample
                v += t; // The filter output
                Ho += dhb; // IR step
                Xp_index += Inc; // Input signal step. NO CHECK ON BOUNDS
            }
        }

        return v;
    }

}
