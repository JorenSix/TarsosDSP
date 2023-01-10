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
	
	boolean active;
	
	/**
	 * Sets the given Grain to start immediately.
	 * 
	 * @param g
	 *            the g
	 * @param time
	 *            the time
	 */
	void reset(double grainSize,double randomness,double position,double timeStretchFactor,double pitchShiftFactor){
		double randomTimeDiff = (Math.random() > 0.5 ? +1 : -1) * grainSize * randomness;
		double actualGrainSize = (grainSize + randomTimeDiff) * 1.0/timeStretchFactor + 1;
		this.position = position - actualGrainSize;
		this.age = 0f;
		this.grainSize = actualGrainSize;
		this.active =true;
	}		
}