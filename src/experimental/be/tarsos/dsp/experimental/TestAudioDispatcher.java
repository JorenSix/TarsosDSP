package be.tarsos.dsp.experimental;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;

public class TestAudioDispatcher {

	
	public TarsosDSPAudioInputStream  getAudioInputStream()  {
		File audioFile = new File("/home/joren/Desktop/4000_samples_of_440Hz_at_44.1kHz.wav");
		AudioInputStream stream = null;
		try {
			stream = AudioSystem.getAudioInputStream(audioFile);
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TarsosDSPAudioInputStream audioStream = new JVMAudioInputStream(stream);
		return audioStream;
	}

	@Test
	public void testZeroPadding() {
		final int bufferSize = 4096;
		final int stepSize = 2048;
		final int totalSamples = 4000;
		AudioDispatcher adp =  new AudioDispatcher(getAudioInputStream(), bufferSize, stepSize);
		adp.setZeroPadFirstBuffer(true);
		adp.setZeroPadLastBuffer(true);
		adp.addAudioProcessor(new AudioProcessor() {
			int bufferCounter = 0;
			
			@Override
			public AudioEvent process(AudioEvent audioEvent) {
				//Check if the first samples are zero
				if(audioEvent.getSamplesProcessed()==0){
					for(int i = 0 ; i < (bufferSize - stepSize); i++){
						assertEquals("First buffer should be zero padded", 0 , audioEvent.getFloatBuffer()[i],0.00000001);
					}
					assertEquals("Buffer size should always equal 4096",bufferSize,audioEvent.getBufferSize());
				}
				//Check if the last samples are zero
				//first buffer contains [0-2048] second buffer[2048-4000]
				if(audioEvent.getSamplesProcessed()==stepSize){
					for(int i = totalSamples; i < bufferSize; i++){
						assertEquals("Last buffer should be zero padded", 0 , audioEvent.getFloatBuffer()[i],0.00000001);
					}
					assertEquals("Buffer size should always equal 4096",bufferSize,audioEvent.getBufferSize());
				}
				bufferCounter++;
				return audioEvent;
			}
			
			@Override
			public void processingFinished() {
				assertEquals("Should have processed 2 buffers.",2,bufferCounter);
			}
			
		});
		adp.run();
	}
	
	@Test
	public void testFirstAndLastBuffer() {
		final int bufferSize = 4096;
		final int stepSize = 0;
		final int totalSamples = 4000;
		AudioDispatcher adp =  new AudioDispatcher(getAudioInputStream(), bufferSize, stepSize);
		adp.setZeroPadFirstBuffer(false);
		adp.setZeroPadLastBuffer(false);
		adp.addAudioProcessor(new AudioProcessor() {
			int bufferCounter = 0;
			
			@Override
			public AudioEvent process(AudioEvent audioEvent) {
				//Check if the first samples are zero
				if(audioEvent.getSamplesProcessed()==0){
					assertEquals("Buffer size should always equal 4000",totalSamples,audioEvent.getBufferSize());
				}
				
				bufferCounter++;
				return audioEvent;
			}
			
			@Override
			public void processingFinished() {
				assertEquals("Should have processed 2 buffers.",1,bufferCounter);
			}
			
		});
		adp.run();
	}
}
