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
			pitch = detector.getPitch(shortAudioBuffer).getPitch();
			System.out.println(String.format("%15s %8.3f Hz", algorithm, pitch));
			assertEquals("Expected about 440Hz for " + algorithm,440,pitch,1.5);
			
			System.arraycopy(audioBuffer, 1024, shortAudioBuffer, 0, shortAudioBuffer.length);
			pitch = detector.getPitch(shortAudioBuffer).getPitch();
			System.out.println(String.format("%15s %8.3f Hz", algorithm, pitch));
			assertEquals("Expected about 440Hz for " + algorithm,440,pitch,1.5);
			
			System.arraycopy(audioBuffer, 2048, shortAudioBuffer, 0, shortAudioBuffer.length);
			pitch = detector.getPitch(shortAudioBuffer).getPitch();
			System.out.println(String.format("%15s %8.3f Hz", algorithm, pitch));
			assertEquals("Expected about 440Hz for " + algorithm,440,pitch,1.5);	
		}
		System.out.println();
	}
	
	@Test
	public void testFlute(){
		float[] audioBuffer = TestUtilities.audioBufferFlute();
		for(PitchEstimationAlgorithm algorithm : PitchEstimationAlgorithm.values()){
			PitchDetector detector = algorithm.getDetector(44100, 1024);
			float pitch = 0;
			float[] shortAudioBuffer = new float[1024];
			
			System.arraycopy(audioBuffer, 2048, shortAudioBuffer, 0, shortAudioBuffer.length);
			pitch = detector.getPitch(shortAudioBuffer).getPitch();
			System.out.println(String.format("%15s %8.3f Hz", algorithm, pitch));
			assertEquals("Expected about 440Hz for " + algorithm,442,pitch,2);
		}
		System.out.println();
	}
	
	@Test
	public void testPiano(){
		float[] audioBuffer = TestUtilities.audioBufferPiano();
		for(PitchEstimationAlgorithm algorithm : PitchEstimationAlgorithm.values()){
			PitchDetector detector = algorithm.getDetector(44100, 1024);
			float pitch = 0;
			float[] shortAudioBuffer = new float[1024];
			
			System.arraycopy(audioBuffer, 0, shortAudioBuffer, 0, shortAudioBuffer.length);
			pitch = detector.getPitch(shortAudioBuffer).getPitch();
			System.out.println(String.format("%15s %8.3f Hz", algorithm, pitch));
			assertEquals("Expected about 440Hz for " + algorithm,443,pitch,3);
		}
		System.out.println();
	}
	
	@Test
	public void testLowPiano(){
		float[] audioBuffer = TestUtilities.audioBufferLowPiano();
		for(PitchEstimationAlgorithm algorithm : PitchEstimationAlgorithm.values()){
			PitchDetector detector = algorithm.getDetector(44100, 1024);
			float pitch = 0;
			float[] shortAudioBuffer = new float[1024];
			
			System.arraycopy(audioBuffer, 0, shortAudioBuffer, 0, shortAudioBuffer.length);
			pitch = detector.getPitch(shortAudioBuffer).getPitch();
			System.out.println(String.format("%15s %8.3f Hz", algorithm, pitch));
			assertEquals("Expected about 130.81Hz for " + algorithm,130.81,pitch,2);
		}
		System.out.println();
	}
	
	@Test
	public void testHighFlute(){
		float[] audioBuffer = TestUtilities.audioBufferHighFlute();
		for(PitchEstimationAlgorithm algorithm : PitchEstimationAlgorithm.values()){
			PitchDetector detector = algorithm.getDetector(44100, 1024);
			float pitch = 0;
			float[] shortAudioBuffer = new float[1024];
			
			System.arraycopy(audioBuffer, 3000, shortAudioBuffer, 0, shortAudioBuffer.length);
			pitch = detector.getPitch(shortAudioBuffer).getPitch();
			System.out.println(String.format("%15s %8.3f Hz", algorithm, pitch));
			//this fails with dynamic wavelet and amdf
			//assertEquals("Expected about 1975.53Hz for " + algorithm,1975.53,pitch,30);
		}
		System.out.println();
	}	
}
