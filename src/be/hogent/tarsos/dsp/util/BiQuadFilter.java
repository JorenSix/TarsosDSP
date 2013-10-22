package be.hogent.tarsos.dsp.util;

/**
 * Implements a <a
 * href="http://en.wikipedia.org/wiki/Digital_biquad_filter">BiQuad filter</a>,
 * which can be used for e.g. low pass filtering.
 * 
 * The implementation is a translation of biquad.c from Aubio, Copyright (C)
 * 2003-2009 Paul Brossier <piem@aubio.org>
 * 
 * @author Joren Six
 * @auhror Paul Brossiers
 */
public class BiQuadFilter {

	private double i1;
	private double i2;
	private double o1;
	private double o2;
	private double a2;
	private double a3;
	private double b1;
	private double b2;
	private double b3;

	public BiQuadFilter(double b1, double b2, double b3, double a2, double a3) {
		this.a2 = a2;
		this.a3 = a3;
		this.b1 = b1;
		this.b2 = b2;
		this.b3 = b3;
		this.i1 = 0.;
		this.i2 = 0.;
		this.o1 = 0.;
		this.o2 = 0.;		
	}
	
	public void doFiltering(float[] in, float[] tmp){
		  double mir;
		  /* mirroring */
		  mir = 2*in[0];
		  i1 = mir - in[2];
		  i2 = mir - in[1];
		  /* apply filtering */
		  doBiQuad(in);
		  /* invert  */
		  for (int j = 0; j < in.length; j++){
		    tmp[in.length-j-1] = in[j];
		  }
		  /* mirror again */
		  mir = 2*tmp[0];
		  i1 = mir - tmp[2];
		  i2 = mir - tmp[1];
		  /* apply filtering */
		  doBiQuad(tmp);
		  /* invert back */
		  for (int j = 0; j < in.length; j++){
		    in[j] = tmp[in.length-j-1];
		  }
	}

	private void doBiQuad(float[] in) {
		for (int j = 0; j < in.length; j++) {
			double i0 = in[j];
			double o0 = b1 * i0 + b2 * i1 + b3 * i2 - a2 * o1 - a3 * o2;
			in[j] = (float) o0;
			i2 = i1;
			i1 = i0;
			o2 = o1;
			o1 = o0;
		}
	}
}
