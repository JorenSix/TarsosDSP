/*
*
*  TarsosDSP is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*
*/

package be.hogent.tarsos.dsp;


public class EnvelopeFollower implements AudioProcessor {

	float gainAttack ;
	float gainRelease;
	float envelopeOut = 0.0f;
	
	public EnvelopeFollower(float sampleRate){
		float attackTime = 0.0001f;//in seconds
		float releaseTime = 0.0002f;//in seconds
		gainAttack = (float) Math.exp(-1.0/(sampleRate*attackTime));
		gainRelease = (float) Math.exp(-1.0/(sampleRate*releaseTime));
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] buffer = audioEvent.getFloatBuffer();
		for(int i = 0 ; i < buffer.length ; i++){
			float envelopeIn = Math.abs(buffer[i]);
			if(envelopeOut < envelopeIn){
				envelopeOut = envelopeIn + gainAttack * (envelopeOut - envelopeIn);
			} else {
				envelopeOut = envelopeIn + gainRelease * (envelopeOut - envelopeIn);
			}
			buffer[i] = envelopeOut;
		}
		return true;
	}

	@Override
	public void processingFinished() {
		
	}
}
