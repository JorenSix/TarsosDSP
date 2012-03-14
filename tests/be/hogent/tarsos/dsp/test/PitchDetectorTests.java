package be.hogent.tarsos.dsp.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import be.hogent.tarsos.dsp.pitch.PitchDetector;
import be.hogent.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

public class PitchDetectorTests {
	
	@Test
	public void testSine(){
		float[] audioBuffer = TestUtilities.audioBufferSine();
		for(PitchEstimationAlgorithm algorithm : PitchEstimationAlgorithm.values()){
			PitchDetector detector = algorithm.getDetector(44100, 1024);
			float pitch = 0;
			float[] shortAudioBuffer = new float[1024];
			
			System.arraycopy(audioBuffer, 0, shortAudioBuffer, 0, shortAudioBuffer.length);
			pitch = detector.getPitch(shortAudioBuffer);
			//System.out.println(String.format("Pitch: %8.3f Hz", pitch));
			assertEquals("Expected about 440Hz for " + algorithm,440,pitch,0.03);
			
			System.arraycopy(audioBuffer, 1024, shortAudioBuffer, 0, shortAudioBuffer.length);
			pitch = detector.getPitch(shortAudioBuffer);
			//System.out.println(String.format("Pitch: %8.3f Hz", pitch));
			assertEquals("Expected about 440Hz for " + algorithm,440,pitch,0.03);
			
			System.arraycopy(audioBuffer, 2048, shortAudioBuffer, 0, shortAudioBuffer.length);
			pitch = detector.getPitch(shortAudioBuffer);
			//System.out.println(String.format("Pitch: %8.3f Hz", pitch));
			assertEquals("Expected about 440Hz for " + algorithm,440,pitch,0.03);	
		}
	}
	
	@Test
	public void testFlute(){
		float[] audioBuffer = TestUtilities.audioBufferFlute();
		for(PitchEstimationAlgorithm algorithm : PitchEstimationAlgorithm.values()){
			PitchDetector detector = algorithm.getDetector(44100, 1024);
			float pitch = 0;
			float[] shortAudioBuffer = new float[1024];
			
			System.arraycopy(audioBuffer, 2048, shortAudioBuffer, 0, shortAudioBuffer.length);
			pitch = detector.getPitch(shortAudioBuffer);
			//System.out.println(String.format("Pitch: %8.3f Hz", pitch));
			assertEquals("Expected about 440Hz for " + algorithm,442,pitch,2);
		}
	}
	
	@Test
	public void testPiano(){
		float[] audioBuffer = TestUtilities.audioBufferPiano();
		for(PitchEstimationAlgorithm algorithm : PitchEstimationAlgorithm.values()){
			PitchDetector detector = algorithm.getDetector(44100, 1024);
			float pitch = 0;
			float[] shortAudioBuffer = new float[1024];
			
			System.arraycopy(audioBuffer, 0, shortAudioBuffer, 0, shortAudioBuffer.length);
			pitch = detector.getPitch(shortAudioBuffer);
			//System.out.println(String.format("Pitch: %8.3f Hz", pitch));
			assertEquals("Expected about 440Hz for " + algorithm,443,pitch,3);
		}
	}
}
