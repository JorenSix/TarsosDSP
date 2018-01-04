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


// dan.14jr@gmail.com


package be.tarsos.dsp.pitch;


import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;
import be.tarsos.dsp.util.fft.WindowFunction;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Implements a pitch tracker by simply locating the most
 * salient frequency component in a signal.
 *
 * @author Joren Six
 */


public class FFTPitch implements PitchDetector {

    private class HarmonicAnalyzer {
        private float mFrequency1;
        private float mFrequency2;
        private float mFrequency3;

        private HarmonicAnalyzer() {
            mFrequency1 = -1;
            mFrequency2 = -1;
            mFrequency3 = -1;
        }

        private void setFrequency1(float freq) {
            mFrequency1 = freq;
        }

        private void setFrequency2(float freq) {
            mFrequency2 = freq;
        }

        private void setFrequency3(float freq) {
            mFrequency3 = freq;
        }

        private float getFrequency1() {
            return mFrequency1;
        }

        private float getFrequency2() {
            return mFrequency2;
        }

        private float getFrequency3() {
            return mFrequency3;
        }
    }


    // Some constants for convenience
    public static final int DEFAULT_BUFFER_SIZE = 2048;
    public static final int DEFAULT_FFT_SIZE = 2048;

    private static final float DEFAULT_POWER_THRESHOLD = 0.50f;
    private static final float PEAK_LEVEL = 1.5f;

    private static final float PI = 3.14159265358979311599796346854418516f;

    private static final float TWO_PI = 6.28318530717958623199592693708837032f;


    private final PitchDetectionResult result;

    /**
     * The audio sample rate. Most audio has a sample rate of 44.1kHz.
     */
    private final float sampleRate;
    private final int sampleSize;

    /**
     * Holds the FFT data, twice the length of the audio buffer.
     */
    private final float[] audioBufferFFT;

    private final FFT fft;


    /**
     * Hold Raw data and results from processing
     */
    private final float[] audioPower;
    private final float[] audioPhase;
    private final float[] audioFrequency;
    private final float[] audioRaw;
    private final HarmonicAnalyzer harmResult;

    private final float[] audioLastPhase;

    // audio buffer iterator
    private int gRover;
    private int rawIndex;
    private final int stepSize;
    private final int latency;
    private final int m_osamp = 4;

    private float mFrequency1;
    private float mFrequency2;
    private float mFrequency3;
    private final float m_calibration = 1.0f;

    private final int fftSize;
    private final float expct, freqPerBin;


    public FFTPitch(int audioSampleRate, int bufferSize) {

        fftSize = bufferSize;
        this.sampleRate = audioSampleRate;
        this.sampleSize = bufferSize;
        audioPower = new float[fftSize / 2];
        audioPhase = new float[fftSize / 2];
        audioFrequency = new float[fftSize / 2];
        audioLastPhase = new float[fftSize / 2 + 1];
        audioRaw = new float[fftSize * 2];
        harmResult = new HarmonicAnalyzer();
        final WindowFunction window;

        //Initializations for FFT
        window = new HammingWindow();
        audioBufferFFT = new float[fftSize * 2];
        fft = new FFT(fftSize, window);
        result = new PitchDetectionResult();

        rawIndex = 0;

        stepSize = fftSize / m_osamp;
        latency = fftSize - stepSize;
        gRover = latency;
        expct = 2.f * PI * (float) stepSize / (float) fftSize;
        freqPerBin = sampleRate / (float) fftSize;

    }

    @Override
    public PitchDetectionResult getPitch(float[] audioBuffer) {

        result.setPitch(-1.0f);
        for (int i = 0; i < sampleSize; i++) {

            /* As long as we have not yet collected enough data just read in */
            audioRaw[gRover] = audioBuffer[i];
            gRover++;

            /* now we have enough data for processing */
            if (gRover >= fftSize) {
                gRover = latency;

                    /* ***************** ANALYSIS ******************* */
                    /* fill fft buffer and do transform */
                for (rawIndex = 0; rawIndex < fftSize; rawIndex++) {
                    audioBufferFFT[rawIndex] = audioRaw[rawIndex];
                }
                fft.powerPhaseFFT(audioBufferFFT, audioPower, audioPhase);


                    /* this is the analysis step */
                for (int k = 0; k < fftSize / 2; k++) {
                    float tmp;
                    int qpd;

                            /* compute phase difference */
                    tmp = audioPhase[k] - audioLastPhase[k];
                    audioLastPhase[k] = audioPhase[k];

                            /* subtract expected phase difference */
                    tmp -= (double) k * expct;

                            /* map delta phase into +/- Pi interval */
                    qpd = (int) (tmp / PI);
                    if (qpd >= 0) qpd += qpd & 1;
                    else qpd -= qpd & 1;
                    tmp -= PI * (float) qpd;

                            /* get deviation from bin frequency from the +/- Pi interval */
                    tmp = m_osamp * tmp / (TWO_PI);

                            /* compute the k-th partials' true frequency */
                    tmp = (float) k * freqPerBin + tmp * freqPerBin;

                    audioFrequency[k] = tmp;

                }


                // Now do the harmonic analysis on the frequency domain
//                harmonicAnalysis(audioFrequency, audioPower, harmResult);
                harmonicAnalysis2(audioFrequency, audioPower, harmResult);

                mFrequency1 = harmResult.getFrequency1();
                mFrequency2 = harmResult.getFrequency2();
                mFrequency3 = harmResult.getFrequency3();


                // if our calibration is off, fix frequencies here
                if (m_calibration != 1.0) {
                    mFrequency1 *= m_calibration;
                    mFrequency2 *= m_calibration;
                    mFrequency3 *= m_calibration;
                }

                    /* shift data in FIFO buffer*/
                for (int k = 0; k < latency; k++) audioRaw[k] = audioRaw[k + stepSize];

            }
        }


        result.setPitch(mFrequency1);
        result.setProbability(mFrequency2);
        return result;
    }

//Perform some harmonic analysis to determine fundamental and the related harmonic frequencies.
//The fundamental may not be the strongest peak, but the strongest peak will be a harmonic,
//or an integer multiple of the fundamental. Knowing this we can examine the data looking for
//related peaks and determine the true fundamental frequency in the signal.

    private void harmonicAnalysis2(float[] freq, float[] power, HarmonicAnalyzer result) {

        int i, j, diff, baseIndex=0;
        float maxAmp = 0;
        int maxIndex = 0;
        int size = power.length;
        int peak1=0, peak2=0, peak3=0;
        float noiseFloor;

        result.setFrequency1(-1);
        result.setFrequency2(-1);
        result.setFrequency3(-1);


        noiseFloor = average(power);
        // find the highest peak
        peak1 = _calc_peak_in_range(2,size-2, power, noiseFloor);
        if(peak1 < 2) return;

        // We are going to assume that peak1 is either f0 or f1
        // Let's see if there is a smaller peak closer to the beginning
        peak2 = _calc_peak_in_range(2, peak1-1, power, power[peak1]/3);


        if (peak2 > 2) // we found a good peak
            baseIndex = min(peak2, peak1-peak2);
        else
            baseIndex = peak1;

        // Now let's see if there is another strong peak above peak1
        peak3 = _calc_first_peak_in_range(peak1+1, min(peak1+baseIndex+1,size-2), power, power[peak1]/3);

        if (peak3 > peak1) // we found a good peak
        {
            baseIndex = min(baseIndex, peak3 - peak1);
            // Since the buckets don't always line up right, make sure we find the real peak,
            // not it's next-door neighbor
            baseIndex = _calc_first_peak_in_range(baseIndex,baseIndex+1,power);
        }

        // We've got something; use it.
        result.setFrequency1(freq[baseIndex]);

        // find frequencies for harmonics above the base
        if(baseIndex > 0 && baseIndex < size/2)
            i = _calc_peak_in_range((baseIndex*2)-2,(baseIndex<<1)+2,power);
        else
            i = 0;

        if (i != 0) {
            result.setFrequency2(freq[i]);
        } else {
            result.setFrequency2(result.getFrequency1()*2);
        }

        if(baseIndex > 0 && baseIndex < size/3)
            i = _calc_peak_in_range((baseIndex*3)-2,(baseIndex<<1)+2,power);
        else
            i = 0;

        if (i != 0) {
            result.setFrequency3(freq[i]);
        } else {
            result.setFrequency3(result.getFrequency1()*3);
        }
    }



    private void harmonicAnalysis(float[] freq, float[] power, HarmonicAnalyzer result) {

        int i, j, diff, baseIndex;
        float maxAmp = 0;
        int maxIndex = 0;
        int size = power.length;

        result.setFrequency1(-1);
        result.setFrequency2(-1);
        result.setFrequency3(-1);

        // Find the largest peak
        maxIndex = _calc_peak_in_range(2, size - 2, power);
        maxAmp = power[maxIndex];
        if (maxIndex < 2) return;


        // find the neighboring peaks large enough to be of interest
        // local maximum > max * threshold

        // Find the next peak greater than max
        j = min(maxIndex*2+1, size -2);
        for(i=maxIndex+1;i<j;i++) {
            if(_calc_is_peak(i,power))
                if (power[i] *DEFAULT_POWER_THRESHOLD > maxAmp)
                    break;
        }

        if(i >= j)
            diff = maxIndex;  // If we didn't find one, use maxIndex
        else
            diff = i - maxIndex;

        // Find the next peak less than max
        j = max(maxIndex/2-1, 2);
        for(i=maxIndex-1;i>=j;i--) {
            if(_calc_is_peak(i,power))
                if (power[i] * DEFAULT_POWER_THRESHOLD > maxAmp) // at least 1/5 of max amp?
                    break;
        }

        if(i < j)
            j = maxIndex;  // If we didn't find one in expected range, use maxIndex
        else
            j = maxIndex - i;


        // Take the smallest of j and diff
        diff = min(diff,j);






/*
        // Find peak in lower range
        if (maxIndex > 2)
            i = _calc_peak_in_range(1, maxIndex - 1, power);
        else
            i = maxIndex;

        // Find peak in upper range
        if (maxIndex < size/2)
            j = _calc_peak_in_range(maxIndex + 2, size - 2, power);
        else
            j = maxIndex;

        if (j!=0 && j != maxIndex)
            diff = min(maxIndex, j - maxIndex);
        else
            diff = maxIndex;

        if (i!=0 && i != maxIndex)
            i = min(maxIndex, maxIndex = i);
        else
            i = maxIndex;



        // Take the smallest of i and diff
        diff = min(diff,i);

*/

        if (diff < 2) return;
        baseIndex = _calc_peak_in_range(diff-1,diff+1,power);

        result.setFrequency1(freq[baseIndex]);


        // find frequencies for harmonics above the base
        if(baseIndex > 0 && baseIndex < size/2)
            i = _calc_peak_in_range((baseIndex*2)-2,(baseIndex<<1)+2,power);
        else
            i = 0;

        if (i != 0) {
            result.setFrequency2(freq[i]);
        } else {
            result.setFrequency2(result.getFrequency1()*2);
        }

        if(baseIndex > 0 && baseIndex < size/3)
            i = _calc_peak_in_range((baseIndex*3)-2,(baseIndex<<1)+2,power);
        else
            i = 0;

        if (i != 0) {
            result.setFrequency3(freq[i]);
        } else {
            result.setFrequency3(result.getFrequency1()*3);
        }

    }

    private boolean
    _calc_is_peak(int i, float[] values) {
        return (values[i] > values[i - 1]
            && values[i] > values[i + 1]);
    }

    private boolean
    _calc_is_peak(int i, float[] values, float minPower) {
        return (values[i] > values[i - 1]
                 && values[i] > values[i + 1]
                 && values[i] >= minPower);
    }

    private int
    _calc_peak_in_range(int a, int b, float[] values) {
        int i, maxi = 0;
        double max = 0;
        for (i = a; i <= b; i++)
            if (max < values[i] && _calc_is_peak(i,values)) {
                max = values[i];
                maxi = i;
            }
        return maxi;
    }

    private int
    _calc_peak_in_range(int a, int b, float[] values, float minPower) {
        int i, maxi = 0;
        double max = 0;
        for (i = a; i <= b; i++)
            if (max < values[i] && _calc_is_peak(i,values, minPower)) {
                max = values[i];
                maxi = i;
            }
        return maxi;
    }

    private int
    _calc_first_peak_in_range(int a, int b, float[] values) {

        for (int i = a; i <= b; i++)
            if (_calc_is_peak(i,values))
                return i;
        return 0;
    }

    private int
    _calc_first_peak_in_range(int a, int b, float[] values, float minPower) {

        for (int i = a; i <= b; i++)
            if (_calc_is_peak(i,values, minPower))
                return i;
        return 0;
    }

    private float average(float[] values)
    {
        int size = values.length;
        float total=0f;

        for(int i=0; i <size; i++)
            total += values[i];
        return total/(float)size;
    }

}
