package be.hogent.tarsos.dsp;

public class DetermineDurationProcessor implements AudioProcessor {
	
	long durationInSamples;
	float sampleRate;
	AudioEvent lastEvent;

	@Override
	public boolean process(AudioEvent audioEvent) {
		lastEvent = audioEvent;
		return true;
	}
	
	public double getDurationInSeconds(){
		return durationInSamples/sampleRate;
	}
	
	public double getDurationInSamples(){
		return durationInSamples;
	}

	@Override
	public void processingFinished() {
		sampleRate = lastEvent.getSampleRate();
		durationInSamples = lastEvent.getSamplesProcessed() + lastEvent.getFloatBuffer().length;
	}
}
