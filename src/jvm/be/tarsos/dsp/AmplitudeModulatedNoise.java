package be.tarsos.dsp;

import java.util.Random;

import javax.sound.sampled.LineUnavailableException;

import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;

public class AmplitudeModulatedNoise implements AudioProcessor {
	
	Random rnd = new Random();
	float dry = 0.7f;
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audioBuffer = audioEvent.getFloatBuffer();
		
		float max = 0;
		for(int i = 0 ; i < audioBuffer.length ; i++){
			max = Math.max(audioBuffer[i],max);
		}
		
		float[] noiseBuffer = new float[audioBuffer.length];
		for(int i = 0 ; i < audioBuffer.length ; i++){
			if(rnd.nextBoolean())
				noiseBuffer[i] = (float) (rnd.nextGaussian() * max);
			else
				noiseBuffer[i] = (float) (rnd.nextGaussian() * max * -1);
		}
		
		
		float stdDevNoise = standardDeviation(noiseBuffer);
		float stdDevAudio = standardDeviation(audioBuffer);
		for(int i = 0 ; i < audioBuffer.length ; i++){
			audioBuffer[i] = audioBuffer[i] / stdDevNoise  * stdDevAudio;
		}
	
		for(int i = 0 ; i < audioBuffer.length ; i++){
			audioBuffer[i] = audioBuffer[i] * dry + noiseBuffer[i] * (1.0f - dry);
		}
		
		return true;
	}
	
	private float standardDeviation(float[] data){
		float sum = 0;
		for(int i = 0 ; i < data.length ; i++){
			sum += data[i];
		}
		
		float mean = sum/(float) data.length;
		
		sum = 0;
		for(int i = 0 ; i < data.length ; i++){
			sum += (data[i] - mean) * (data[i] - mean);
		}
		float variance = sum / (float) (data.length - 1);
		
		return (float) Math.sqrt(variance);
	}

	@Override
	public void processingFinished() {
	}
	
	
	public static void main(String... args) throws LineUnavailableException{
		String file = "/home/joren/Desktop/parklife/Alles_Is_Op.wav";
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(file, 44100, 1024, 0);
		final AmplitudeModulatedNoise noise = new AmplitudeModulatedNoise();
		d.addAudioProcessor(noise);
		d.addAudioProcessor(new AudioPlayer(d.getFormat()));
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				for(float i = 0 ; i < 1.10 ; i+= 0.1  ){
					noise.dry = i;
					System.out.println(i);
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}}).start();
		
		d.run();
		
		
		
	}


}
