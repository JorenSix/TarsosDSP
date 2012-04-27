package be.hogent.tarsos.dsp;

/**
 * Simply stops the audio processing 
 * pipeline if the stop time is reached.
 * @author Joren Six
 *
 */
public class StopAudioProcessor implements AudioProcessor {

	private final double stopTime;
	public StopAudioProcessor(double stopTime){
		this.stopTime = stopTime;
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		return audioEvent.getTimeStamp() <= stopTime;
	}

	@Override
	public void processingFinished() {
		
	}
}
