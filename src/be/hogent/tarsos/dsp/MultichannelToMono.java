package be.hogent.tarsos.dsp;

public class MultichannelToMono implements AudioProcessor{
	
	private int channels;
	private boolean mean;
	
	public MultichannelToMono(int numberOfChannels,boolean meanOfchannels){
		channels = numberOfChannels;
		mean = meanOfchannels;
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] buffer = audioEvent.getFloatBuffer();
		float[] newBuffer = new float[buffer.length/channels];
		
		if(mean){
			if(channels==2){
				for(int i = 0 ; i < buffer.length ; i = i + channels){
					newBuffer[i/channels]=(buffer[i]+buffer[i+1])/2;
				}	
			}else{
				for(int i = 0 ; i < buffer.length ; i = i + channels){
					double sum = 0;
					for(int j = 0; j < channels;j++){
						sum = sum + buffer[i+j];
					}
					newBuffer[i/channels]=(float) (sum/channels);
				}	
			}
		}else{
			for(int i = 0 ; i < buffer.length ; i = i + channels){
				newBuffer[i/channels]=buffer[i];
			}
		}
		
		audioEvent.setFloatBuffer(newBuffer);
		return true;
	}

	@Override
	public void processingFinished() {
	}
}
