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
