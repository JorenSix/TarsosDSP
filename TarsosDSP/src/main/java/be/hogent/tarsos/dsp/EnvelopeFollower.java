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

package be.hogent.tarsos.dsp;

/**
 * An envelope follower follows the envelope of a signal. Sometimes the name
 * envelope detector is used. From wikipedia:
 *  <blockquote> An envelope detector
 * is an electronic circuit that takes a high-frequency signal as input and
 * provides an output which is the envelope of the original signal. The
 * capacitor in the circuit stores up charge on the rising edge, and releases it
 * slowly through the resistor when the signal falls. The diode in series
 * rectifies the incoming signal, allowing current flow only when the positive
 * input terminal is at a higher potential than the negative input terminal.
 * </blockquote>
 * 
 * The resulting envelope is stored in the buffer in the processed AudioEvent. The class can be used thusly:
 * 
 * <pre>
 * EnvelopeFollower follower = new EnvelopeFollower(44100);
 * 		
 * AudioDispatcher dispatcher = AudioDispatcher.fromFloatArray(sine, 44100, 1024, 0);
 * 	
 * 	
 * 	dispatcher.addAudioProcessor(follower);
 * 	dispatcher.addAudioProcessor(new AudioProcessor() {
 * 	
 * 		public boolean process(AudioEvent audioEvent) {
 * 			//envelope
 * 			float buffer[] = audioEvent.getFloatBuffer();
 * 			for(int i = 0 ; i < buffer.length ; i++){
 * 				System.out.println(buffer[i]);
 * 			}
 * 			return true;
 * 		}
 * 			
 * 		public void processingFinished() {
 *  	}
 * 	});
 * 	dispatcher.run();
 *  </pre>
 *  
 * 
 * @author Joren Six
 * 
 */
public class EnvelopeFollower implements AudioProcessor {
	
	/**
	 * Defines how fast the envelope raises, defined in seconds.
	 */
	private static final double DEFAULT_ATTACK_TIME =  0.0002;//in seconds
	/**
	 * Defines how fast the envelope goes down, defined in seconds.
	 */
	private static final double DEFAULT_RELEASE_TIME =  0.0004;//in seconds
	
	float gainAttack ;
	float gainRelease;
	float envelopeOut = 0.0f;
	
	/**
	 * Create a new envelope follower, with a certain sample rate.
	 * @param sampleRate The sample rate of the audio signal.
	 */
	public EnvelopeFollower(double sampleRate){
		this(sampleRate,DEFAULT_ATTACK_TIME,DEFAULT_RELEASE_TIME);
	}
	
	/**
	 * Create a new envelope follower, with a certain sample rate.
	 * @param sampleRate The sample rate of the audio signal.
	 * @param attackTime Defines how fast the envelope raises, defined in seconds.
	 * @param releaseTime Defines how fast the envelope goes down, defined in seconds.
	 */
	public EnvelopeFollower(double sampleRate, double attackTime,double releaseTime){
		gainAttack = (float) Math.exp(-1.0/(sampleRate*attackTime));
		gainRelease = (float) Math.exp(-1.0/(sampleRate*releaseTime));
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] buffer = audioEvent.getFloatBuffer();
		calculateEnvelope(buffer);
		return true;
	}
	
	public void calculateEnvelope(float[] buffer){
		for(int i = 0 ; i < buffer.length ; i++){
			float envelopeIn = Math.abs(buffer[i]);
			if(envelopeOut < envelopeIn){
				envelopeOut = envelopeIn + gainAttack * (envelopeOut - envelopeIn);
			} else {
				envelopeOut = envelopeIn + gainRelease * (envelopeOut - envelopeIn);
			}
			buffer[i] = envelopeOut;
		}
	}

	@Override
	public void processingFinished() {
		
	}
}
