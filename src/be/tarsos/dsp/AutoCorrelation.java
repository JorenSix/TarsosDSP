package be.hogent.tarsos.dsp;

public class AutoCorrelation implements AudioProcessor {

    private float result;
    
    public AutoCorrelation(){
    }
    
    @Override
    public boolean process(AudioEvent audioEvent) {
        float[] audioFloatbuffer = audioEvent.getFloatBuffer();
        
        result = 0;

        for (int i=0; i<audioFloatbuffer.length; i++){
        	result += ((float)Math.abs(audioFloatbuffer[i]))/(float)audioFloatbuffer.length;
        }
        return true;
    }
    
    @Override
    public void processingFinished() {
    }

    public float[] getValues() {
    	float[] returnValue = new float[1];
    	returnValue[0] = result;
        return returnValue;
    }
}
