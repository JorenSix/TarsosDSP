package be.tarsos.dsp.granulator;

/**
 * The nested class Grain. Stores information about the start time, current position, age, and grain size of the grain.
 */
class Grain{

	/** The position in millseconds. */
	double position;

	/** The age of the grain in milliseconds. */
	double age;

	/** The grain size of the grain. Fixed at instantiation. */
	double grainSize;		
	
	/**
	 * Sets the given Grain to start immediately.
	 * 
	 * @param g
	 *            the g
	 * @param time
	 *            the time
	 */
	void reset(double grainSize,double randomness,double position){
		this.position = position + (grainSize * randomness * (Math.random() * 2.0 - 1.0));
		this.age = 0f;
		this.grainSize = grainSize;
	}		
}