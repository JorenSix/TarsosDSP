package be.tarsos.dsp.test;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.PipedAudioStream;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AudioDispatcherTest {

	
	public TarsosDSPAudioInputStream  getAudioInputStream()  {
		File audioFile = TestUtilities.sineOf4000Samples();
		AudioInputStream stream = null;
		try {
			stream = AudioSystem.getAudioInputStream(audioFile);
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		TarsosDSPAudioInputStream audioStream = new JVMAudioInputStream(stream);
		return audioStream;
	}
	
	public TarsosDSPAudioInputStream getAudioInputStreamPiped(){
		File audioFile = TestUtilities.sineOf4000Samples();
		return new PipedAudioStream(audioFile.getAbsolutePath()).getMonoStream(44100,0);
	}
	
	@Test
	public void testZeroPaddingFirstBuffer(){
		testZeroPaddingFirstBufferForStream(getAudioInputStream());
		testZeroPaddingFirstBufferForStream(getAudioInputStreamPiped());
	}


	public void testZeroPaddingFirstBufferForStream(TarsosDSPAudioInputStream audioStream) {
		final int bufferSize = 4096;
		final int stepSize = 2048;
		final int totalSamples = 4000;
		AudioDispatcher adp =  new AudioDispatcher(audioStream, bufferSize, stepSize);
		adp.setZeroPadFirstBuffer(true);
		adp.setZeroPadLastBuffer(true);
		adp.addAudioProcessor(new AudioProcessor() {
			int bufferCounter = 0;
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				//Check if the first samples are zero
				if(audioEvent.getSamplesProcessed()==0){
					for(int i = 0 ; i < (bufferSize - stepSize); i++){
						assertEquals( 0 , audioEvent.getFloatBuffer()[i],0.00000001,"First buffer should be zero padded");
					}
					assertEquals(bufferSize,audioEvent.getBufferSize(),"Buffer size should always equal 4096");
				}
				//Check if the last samples are zero
				//first buffer contains [0-2048] second buffer[2048-4000]
				if(audioEvent.getSamplesProcessed()==stepSize){
					for(int i = totalSamples; i < bufferSize; i++){
						assertEquals(0 , audioEvent.getFloatBuffer()[i],0.00000001,"Last buffer should be zero padded");
					}
					assertEquals(bufferSize,audioEvent.getBufferSize(),"Buffer size should always equal 4096");
				}
				bufferCounter++;
				return true;
			}
			
			@Override
			public void processingFinished() {
				assertEquals(2,bufferCounter,"Should have processed 2 buffers.");
			}
			
		});
		adp.run();
	}
	
	/**
	 * Tests the case when the first buffer is immediately the last.
	 */
	@Test
	public void testFirstAndLastBuffer(){

		testFirstAndLastBufferForStream(getAudioInputStream());
		testFirstAndLastBufferForStream(getAudioInputStreamPiped());
	}
	
	public void testFirstAndLastBufferForStream(TarsosDSPAudioInputStream audioStream) {
		final int bufferSize = 4096;
		final int stepSize = 0;
		final int totalSamples = 4000;
		AudioDispatcher adp =  new AudioDispatcher(getAudioInputStream(), bufferSize, stepSize);
		adp.setZeroPadFirstBuffer(false);
		adp.setZeroPadLastBuffer(false);
		adp.addAudioProcessor(new AudioProcessor() {
			int bufferCounter = 0;
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				//Check if the first samples are zero
				if(audioEvent.getSamplesProcessed()==0){
					assertEquals(totalSamples,audioEvent.getBufferSize(),"Buffer size should always equal 4000");
				}
				
				bufferCounter++;
				return true;
			}
			
			@Override
			public void processingFinished() {
				assertEquals(1,bufferCounter,"Should have processed 1 buffer.");
			}
			
		});
		adp.run();
	}
}
