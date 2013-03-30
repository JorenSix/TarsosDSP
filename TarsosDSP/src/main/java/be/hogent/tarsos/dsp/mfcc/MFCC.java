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
package be.hogent.tarsos.dsp.mfcc;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;

public class MFCC implements AudioProcessor {
	
    private final int AMOUNT_OF_CEPSTRUM_COEF = 14; //Number of MFCCs per frame
    //private final static int FRAMELENGTH = 512;
    //private final static int FRAMELENGTH = 2048; //Number of samples per frame
    //private final int FFTLENGHT = FRAMELENGTH; //Must be power of 2; in jAudio = Number of samples per frame
    protected final static int MELFILTERS = 23; //Number of mel filters (SPHINX-III uses 40)
    protected final static float lowerFilterFreq = (float) 133.3334; //lower limit of filter (or 64 Hz?)
    //protected final static float upperFilterFreq = (float) 6855.4976; //upper limit of filter (or half of sampling freq.?)
    
    
    float[] audioFloatBuffer;
    //Er zijn evenveel mfccs als er frames zijn!?
    //Per frame zijn er dan CEPSTRA coëficienten
    private float[] mfcc;

    
    private int samplesPerFrame; 
    private int sampleRate;
    //private int overlap;
    
    float w[]; 
    //private FFT fourier;
    
    /**
     * Pre-Emphasis Alpha (Set to 0 if no pre-emphasis should be performed)
     */
    private final static float PREEMPHASISALPHA = (float) 0.95;
    
	
    //public MFCC(AudioFile audioFile) {
    public MFCC(int samplesPerFrame,int sampleRate,int overlap) {
        this.samplesPerFrame = samplesPerFrame; 
        this.sampleRate = sampleRate;
        //this.overlap = overlap;
        
       
        w = new float[samplesPerFrame];
        for (int n = 0; n < samplesPerFrame; n++) {
            w[n] = (float) (0.54 - 0.46 * Math.cos((2 * Math.PI * n) / (samplesPerFrame - 1)));
        }
    }

	@Override
	public boolean process(AudioEvent audioEvent) {
		audioFloatBuffer = audioEvent.getFloatBuffer();
		
		hammingWindow(audioFloatBuffer);
		
        // Magnitude Spectrum
        float bin[] = magnitudeSpectrum(audioFloatBuffer);
        // Mel Filtering
        int cbin[] = fftBinIndices();
        // get Mel Filterbank
        float fbank[] = melFilter(bin, cbin);
        // Non-linear transformation
        float f[] = nonLinearTransformation(fbank);
        // Cepstral coefficients
        mfcc = cepCoefficients(f);
        
		return true;
	}

	@Override
	public void processingFinished() {

	}
	
    float[] preEmphase(float[] in){
        float[] EmphasedSamples = new float[in.length];
        for (int i = 1; i < in.length; i++){
            EmphasedSamples[i] = (float) in[i] - PREEMPHASISALPHA * in[i - 1];
        }
        return EmphasedSamples;
    }
    
	
    void framing(float[] in){
        float temp = (float) in.length / samplesPerFrame;
        int numFrames = (int) temp;
        
        // unconditionally round up
        if ((temp / numFrames) != 1) {
            numFrames = numFrames + 1;
        }
        
        mfcc = new float[AMOUNT_OF_CEPSTRUM_COEF];
    }
    
	
    void hammingWindow(float[] in){
        for (int n = 0; n < samplesPerFrame; n++) {
            in[n] *= w[n];
        }        
    }
    
	
    /**
     * computes the magnitude spectrum of the input frame<br>
     * calls: none<br>
     * called by: featureExtraction
     * @param frame Input frame signal
     * @return Magnitude Spectrum array
     */
    public float[] magnitudeSpectrum(float frame[]){
        float magSpectrum[] = new float[frame.length];
        
        // calculate FFT for current frame
        FFT.computeFFT(frame);
        
        // calculate magnitude spectrum
        for (int k = 0; k < frame.length; k++){
            //stelling van pitagoras
            magSpectrum[k] = (float) Math.pow(FFT.real[k] * FFT.real[k] + FFT.imag[k] * FFT.imag[k], 0.5);
        }

        return magSpectrum;
    }
	
    /**
     * calculates the FFT bin indices<br>
     * calls: none<br>
     * called by: featureExtraction
     * 
     * 5-3-05 Daniel MCEnnis paramaterize sampling rate and frameSize
     * 
     * @return array of FFT bin indices
     */
    public int[] fftBinIndices(){
        int cbin[] = new int[MELFILTERS + 2];
        
        cbin[0] = (int)Math.round(lowerFilterFreq / sampleRate * samplesPerFrame);
        cbin[cbin.length - 1] = (int)(samplesPerFrame / 2);
        
        for (int i = 1; i <= MELFILTERS; i++){
            float fc = centerFreq(i,sampleRate);

            cbin[i] = (int)Math.round(fc / sampleRate * samplesPerFrame);
        }
        
        return cbin;
    }
    
	
    /**
     * the output of mel filtering is subjected to a logarithm function (natural logarithm)<br>
     * calls: none<br>
     * called by: featureExtraction
     * @param fbank Output of mel filtering
     * @return Natural log of the output of mel filtering
     */
    public float[] nonLinearTransformation(float fbank[]){
        float f[] = new float[fbank.length];
        final float FLOOR = -50;
        
        for (int i = 0; i < fbank.length; i++){
            f[i] = (float) Math.log(fbank[i]);
            
            // check if ln() returns a value less than the floor
            if (f[i] < FLOOR) f[i] = FLOOR;
        }
        
        return f;
    }
    
    /**
     * Calculate the output of the mel filter<br>
     * calls: none
     * called by: featureExtraction
     * @param bin 
     * @param cbin 
     * @return output of the mel filter
     */
    public float[] melFilter(float bin[], int cbin[]){
        float temp[] = new float[MELFILTERS + 2];

        for (int k = 1; k <= MELFILTERS; k++){
            float num1 = 0, num2 = 0;

            for (int i = cbin[k - 1]; i <= cbin[k]; i++){
                num1 += ((i - cbin[k - 1] + 1) / (cbin[k] - cbin[k-1] + 1)) * bin[i];
            }

            for (int i = cbin[k] + 1; i <= cbin[k + 1]; i++){
                num2 += (1 - ((i - cbin[k]) / (cbin[k + 1] - cbin[k] + 1))) * bin[i];
            }

            temp[k] = num1 + num2;
        }

        float fbank[] = new float[MELFILTERS];
        for (int i = 0; i < MELFILTERS; i++){
            fbank[i] = temp[i + 1];
        }

        return fbank;
    }
    
    
    /**
     * Cepstral coefficients are calculated from the output of the Non-linear Transformation method<br>
     * calls: none<br>
     * called by: featureExtraction
     * @param f Output of the Non-linear Transformation method
     * @return Cepstral Coefficients
     */
    public float[] cepCoefficients(float f[]){
        float cepc[] = new float[AMOUNT_OF_CEPSTRUM_COEF];
        
        for (int i = 0; i < cepc.length; i++){
            for (int j = 1; j <= MELFILTERS; j++){
                cepc[i] += f[j - 1] * Math.cos(Math.PI * i / MELFILTERS * (j - 0.5));
            }
        }
        
        return cepc;
    }
    
    /**
     * calculates center frequency<br>
     * calls: none<br>
     * called by: featureExtraction
     * @param i Index of mel filters
     * @return Center Frequency
     */
    private static float centerFreq(int i,float samplingRate){
        double mel[] = new double[2];
        mel[0] = freqToMel(lowerFilterFreq);
        mel[1] = freqToMel(samplingRate / 2);
        
        // take inverse mel of:
        double temp = mel[0] + ((mel[1] - mel[0]) / (MELFILTERS + 1)) * i;
        return inverseMel(temp);
    }
    
    /**
     * convert frequency to mel-frequency<br>
     * calls: none<br>
     * called by: featureExtraction
     * @param freq Frequency
     * @return Mel-Frequency
     */
    protected static float freqToMel(float freq){
        return (float) (2595 * log10(1 + freq / 700));
    }
    
    /**
     * calculates the inverse of Mel Frequency<br>
     * calls: none<br>
     * called by: featureExtraction
     */
    private static float inverseMel(double x){
        float temp = (float) Math.pow(10, x / 2595) - 1;
        return 700 * (temp);
    }
    
    /**
     * calculates logarithm with base 10<br>
     * calls: none<br>
     * called by: featureExtraction
     * @param value Number to take the log of
     * @return base 10 logarithm of the input values
     */
    protected static float log10(float value){
        return (float) (Math.log(value) / Math.log(10));
    }

	public float[] getMFCC() {
		return mfcc;
	}

}
