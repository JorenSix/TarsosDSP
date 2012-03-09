package be.hogent.tarsos.dsp.pitch;

import be.hogent.tarsos.dsp.test.TestUtilities;

/**
 * I should implement swipe... it is a bit hard to implement.
 * @author Joren Six
 *
 */
public class Swipe implements PitchDetector {
	
	private static final double DLOG2P  = 1.0/96.0;
	private static final double LOG2 = Math.log(2);
	private static final double DERBS = 0.1; 
	private static final double POLYV = 1 / 12 / 64;
	

	final private double pitchLimitMin;//Hz plim

	final private double pitchLimitMax;//Hz  plim

	final private double dt; //seconds


	final private double threshold; //sTHR

	final private double sampleRate;
	

	final private double strengthThreshold; // st [0,1]
	
	public Swipe(){
		pitchLimitMin = 30;//hz
		pitchLimitMax = 5000;//hz
		dt = 0.01;
		threshold = Double.NEGATIVE_INFINITY;
		sampleRate = 44100;//Hz
		strengthThreshold = 0.3;
	}
	
	private double lg(double value){
		return Math.log(value)/LOG2;
	}
	
	public double[] swipe(){
	    double nyquist = sampleRate / 2.;
	    double nyquist2 = nyquist * 2;
	    double nyquist16 = nyquist * 16;
	    
	    long wsSize = Math.round(lg(nyquist16/pitchLimitMin) - lg(nyquist16/pitchLimitMax)) + 1;
	    int[] ws = new int[(int) wsSize];
	    
	    for(int i = 0 ; i < ws.length ; i++){
	    	ws[i] = (int) (Math.pow(2, Math.round(lg(nyquist16 / pitchLimitMin))) / Math.pow(2, i));
	    }
	    
	    int pcSize = (int) Math.ceil((lg(pitchLimitMax)-lg(pitchLimitMin))/DLOG2P);
	    double[] pc = new double[pcSize];
	    double[] d = new double[pcSize];
	    
	    double td;
	    for(int i = 0 ; i <  pc.length ; i++){
	    	td = lg(pitchLimitMin) + (i*DLOG2P);
	    	pc[i] = Math.pow(2, td);
	    	d[i] = 1 + td - lg(nyquist16 / ws[0]);
	    }
	    td = lg(pitchLimitMin);
	    
	    float[] x = TestUtilities.audioBufferSine();
	    
	    int fERBsSize = (int) Math.ceil((HertzToERB(nyquist) - HertzToERB(Math.pow(2, td) / 4)) / DERBS);
	    double [] fERBs =  new double[fERBsSize];
	    
	    td = HertzToERB(pitchLimitMin/4.0);
	    
	    for (int i = 0; i < fERBs.length; i++){
	    	fERBs[i] = erbToHertz(td + (i * DERBS));
	    }
	    
	    int[] ps = new int[(int) Math.floor(fERBs[fERBs.length - 1 ]/ pc[0] - 0.75)];
	    sieve(ps);
	    ps[0] = 1; // Hack to make 1 "act" prime...don't ask
	    
	    double[][] S = new double[pc.length][(int) (Math.ceil(x.length/nyquist2) / dt)];
	    SFrist(S, x, pc, fERBs, d, ws,ps,0);
	    for(int i = 1 ; i < ws.length - 1 ; i ++){
	    	Snth(S, x, pc, fERBs,i);
	    }
	    Slast(S,x,pc,fERBs,ws.length - 1);
	    
	    double[] p = pitch(S,pc);
	    
	    return p;
	    
	}
	
	private double[] pitch(double[][] s, double[] pc) {

	    
	    double[] p = new double[s[0].length];
	    return p;
		
	}
	
	private double[][] loudness(float[] x , double[] fERBs, double nyquist, int w, int w2){
		return new double[0][0];
	}

	// Helper function for populating the strength matrix on left boundary
	private void SFrist(double[][] s, float[] x, double[] pc, double[] fERBs, double[] d , int[] ws, int[] ps, int n){
	    int i; 
	    int w2 = ws[n] / 2;
	    double[][] L = loudness(x, fERBs, sampleRate/2.0, ws[n], w2);
	    int lo = 0; // The start of Sfirst-specific code
	    int hi = bisectv(d, 2.);
	    int psz = hi - lo;
	    double[] mu = new double[psz];
	    double[] pci = new double[psz];
	    for (i = 0; i < hi; i++) {
	        pci[i] = pc[i];
	        mu[i] = 1. - Math.abs(d[i] - 1.);
	    } // End of Sfirst-specific code
	    //Sadd(s, L, fERBs, pci, mu, ps, dt, sampleRate/2.0, lo, hi, psz, w2);
	}
	
	private void Snth(double[][] s, float[] x, double[] pc, double[] fERBs, int n){
		
	}
	
	private void Slast(double[][] s, float[] x, double[] pc, double[] fERBs, int n){
		
	}
	
	
	// Converts from hertz to ERBs
	double HertzToERB(double hz) { 
	    return(21.4 * Math.log10(1. + hz / 229.));
	}

	// Converts from ERBs to hertz 
	double erbToHertz(double erb) { 
	    return((Math.pow(10, erb / 21.4) - 1.) * 229.);
	}
	
	// a naive Sieve of Erasthones for prime numbers
	private static int sieve(int[] ones) {
	    int k = 0;
	    int sp = (int) Math.floor(Math.sqrt(ones.length));
	    ones[0] = 0; // Because 1 is not prime (though sometimes we wish it was)
	    for (int i = 1; i < sp; i++) { 
	        if (1 == ones[i]) {
	            for (int j = i + i + 1; j < ones.length; j += i + 1) 
	                ones[j] = 0;
	            k++;
	        }
	    }
	    for (int i = sp; i < ones.length; i++) { // Now we're only counting
	        if (1 == ones[i]) 
	            k++;
	    }
	    return(k); 
	}
	
	// find the bisection index of the vector for key
	private static int bisectv(double[] yr_vector, double key) { 
	    int md;                                
	    int lo = 1;                           
	    int hi = yr_vector.length;                   
	    while (hi - lo > 1) {
	        md = (hi + lo) >> 1;
	        if (yr_vector[md] > key) hi = md;
	        else lo = md;
	    }
	    return(hi);
	}
	
	@Override
	public float getPitch(float[] audioBuffer) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getProbability() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private static int[] primes(int n) {
		int j = 0;
	    int[] myOnes = new int[n];
	    for(int i = 0 ; i < myOnes.length ; i++){
	    	myOnes[i] = 1;
		}
	    int[] myPrimes = new int[sieve(myOnes)]; // size of the # of primes
	    for (int i = 0; i < myOnes.length; i++) { // could start at 1, unless we're hacking
	        if (1 == myOnes[i]) 
	            myPrimes[j++] = i + 1;
	    }
	    return myPrimes;
	}
	

	public static void main(String...strings){
		int [] primes = primes(100);
		for(int i = 0 ; i < primes.length ; i++){
			System.out.print(primes[i]  + " ");	
		}
		System.out.println();
		
	}
}
