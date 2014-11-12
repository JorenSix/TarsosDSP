package be.tarsos.dsp.util.fft;

/**
 * @author joren
 * See https://mgasior.web.cern.ch/mgasior/pap/FFT_resol_note.pdf
 */
public class BlackmanHarrisNuttall extends WindowFunction {
	float c0 = 0.355768f;
	float c1 = 0.487396f;
	float c2 = 0.144232f; 
	float c3 = 0.012604f;
	
	@Override
	protected float value(int length, int index) {
	      
		float sum = 0;
		
		sum += c0 * Math.cos((TWO_PI * 0 * index ) / (float) (length)) ;
		sum += c1 * Math.cos((TWO_PI * 1 * index ) / (float) (length));
		sum += c2 * Math.cos((TWO_PI * 2 * index ) / (float) (length));
		sum += c3 * Math.cos((TWO_PI * 3 * index ) / (float) (length));
		
		return sum;
	}

}
