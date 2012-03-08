package be.hogent.tarsos.dsp;

public class Oscilloscope implements AudioProcessor {
	public static interface OscilloscopeEventHandler{
		void handleEvent(float[] data, AudioEvent event);
	}
	float[] dataBuffer;
	private final OscilloscopeEventHandler handler;
	public Oscilloscope(OscilloscopeEventHandler handler){
		this.handler = handler;
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audioBuffer = audioEvent.getFloatBuffer();
		int offset = 0;
		float maxdx = 0;
		for (int i = 0; i < audioBuffer.length / 4; ++i) {
			float dx = audioBuffer[i + 1] - audioBuffer[i];
			if (dx > maxdx) {
				offset = i;
				maxdx = dx;
			}
		}
		
		float tbase = audioBuffer.length / 2;
		

		int length = Math.min((int) tbase, audioBuffer.length-offset);
		if(dataBuffer == null || dataBuffer.length != length * 4){
			dataBuffer = new float[length * 4];
		}
		
		int j = 0;
		for(int i = 0; i < length - 1; i++){
		    float x1 = i / tbase;
		    float x2 = i / tbase;
		    dataBuffer[j] = x1;
		    dataBuffer[j+1] = audioBuffer[i+offset];
		    dataBuffer[j+2] = x2;
		    dataBuffer[j+3] = audioBuffer[i+1+offset];
		    j = j + 4;
		}
		handler.handleEvent(dataBuffer, audioEvent);
		return true;
	}

	@Override
	public void processingFinished() {
	}

}
