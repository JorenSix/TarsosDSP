/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -------------------------------------------------------------
*
* TarsosDSP is developed by Joren Six at IPEM, University Ghent
*  
* -------------------------------------------------------------
*
*  Info: http://0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://0110.be/releases/TarsosDSP/
*  
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
* 
*/


package be.tarsos.dsp.test;

import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.Yin;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FFTTest {

	@Test
	public void testFFTWindow() {
		final int FFT_SIZE = 4096;
		final FFT noWindow = new FFT(FFT_SIZE);
		final FFT withWindow = new FFT(FFT_SIZE, new HammingWindow());

		final float[] test1 = new float[FFT_SIZE * 2];
		final float[] test2 = new float[FFT_SIZE * 2];
		float[] magnitudes;
		float max;

		// generate a 440 Hz tone at amplitude 0.6
		final double SAMPLE_RATE = 48000.0;
		final double HZ = 440.0;
		final double AMPLITUDE = 0.6;
		final float[] sinWave = TestUtilities.audioBufferSine(SAMPLE_RATE, HZ, FFT_SIZE, AMPLITUDE);

		// copy audio into first half of test arrays (zeros remain in last half)
		System.arraycopy(sinWave, 0, test1, 0, FFT_SIZE);
		System.arraycopy(sinWave, 0, test2, 0, FFT_SIZE);

		// no window
		max = 0;
		noWindow.forwardTransform(test1);
		magnitudes = new float[test1.length / 2];
		noWindow.modulus(test1, magnitudes);
		for (int i = 0; i < magnitudes.length; i++) {
			float m = magnitudes[i];
			assertTrue(Float.isFinite(m) && m >= 0f, i + ": " + m);
			if (m > max)
				max = m;
		}
		assertTrue(max > 0);

		// with window
		max = 0;
		withWindow.forwardTransform(test2);
		magnitudes = new float[test2.length / 2];
		withWindow.modulus(test2, magnitudes);
		for (int i = 0; i < magnitudes.length; i++) {
			float m = magnitudes[i];
			assertTrue(Float.isFinite(m) && m >= 0f, i + ": " + m);
			if (m > max)
				max = m;
		}
		assertTrue(max > 0);
	}

	@Test
	public void testComplexFFTWindow() {
		final int FFT_SIZE = 1024;
		final FFT noWindow = new FFT(FFT_SIZE);
		final FFT withWindow = new FFT(FFT_SIZE, new HammingWindow());

		// interleaved complex arrays: re0, im0, re1, im1, ...
		final float[] bufNoWindow = new float[FFT_SIZE * 2];
		final float[] bufWithWindow = new float[FFT_SIZE * 2];

		// generate a short sine as real parts (imag parts remain zero)
		final double SAMPLE_RATE = 48000.0;
		final double HZ = 440.0;
		final double AMPLITUDE = 0.6;
		final float[] sinWave = TestUtilities.audioBufferSine(SAMPLE_RATE, HZ, FFT_SIZE, AMPLITUDE);

		for (int i = 0; i < FFT_SIZE; i++) {
			bufNoWindow[2 * i] = sinWave[i];
			bufNoWindow[2 * i + 1] = 0f;
			bufWithWindow[2 * i] = sinWave[i];
			bufWithWindow[2 * i + 1] = 0f;
		}

		// run complex forward transforms
		noWindow.complexForwardTransform(bufNoWindow);
		withWindow.complexForwardTransform(bufWithWindow);

		// verify magnitudes are finite and non-negative and that a peak exists
		float maxNo = 0f;
		float maxWith = 0f;
		for (int i = 0; i < FFT_SIZE; i++) {
			int reIdx = 2 * i;
			int imIdx = reIdx + 1;
			float reNo = bufNoWindow[reIdx];
			float imNo = bufNoWindow[imIdx];
			float magNo = (float) Math.hypot(reNo, imNo);
			assertTrue(Float.isFinite(magNo) && magNo >= 0f, "no-window mag invalid at bin " + i);
			if (magNo > maxNo) maxNo = magNo;

			float reW = bufWithWindow[reIdx];
			float imW = bufWithWindow[imIdx];
			float magW = (float) Math.hypot(reW, imW);
			assertTrue(Float.isFinite(magW) && magW >= 0f, "windowed mag invalid at bin " + i);
			if (magW > maxWith) maxWith = magW;
		}

		assertTrue(maxNo > 0f, "expected non-zero magnitude without window");
		assertTrue(maxWith > 0f, "expected non-zero magnitude with window");

		// optional sanity: ensure transforms produced different results (window alters spectrum)
		// This is not strictly required but useful as a lightweight check.
		boolean different = false;
		for (int i = 0; i < bufNoWindow.length; i++)
			if (bufNoWindow[i] != bufWithWindow[i]) { different = true; break; }
		assertTrue(different, "windowed and non-windowed outputs should differ");
	}

	@Test
	public void testForwardAndBackwardsFFT() {
		final double sampleRate = 44100.0;
		final double f0 = 440.0;
		final double amplitudeF0 = 0.5;
		final int  audioBufferSize = 1024;
		final int numberOfAudioSamples = 2 * audioBufferSize * 44; // about two seconds
		final float[]  floatBuffer = TestUtilities.audioBufferSine(sampleRate,f0,numberOfAudioSamples, amplitudeF0);

	      final TarsosDSPAudioFormat format = new TarsosDSPAudioFormat((float) sampleRate, 16, 1, true, false);
	      final TarsosDSPAudioFloatConverter converter =  TarsosDSPAudioFloatConverter.getConverter(format);
	      final byte[] byteBuffer = new byte[floatBuffer.length * format.getFrameSize()];

	      converter.toByteArray(floatBuffer, byteBuffer);
	      final ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
          final AudioInputStream inputStream = new AudioInputStream(bais, JVMAudioInputStream.toAudioFormat(format),floatBuffer.length);
          JVMAudioInputStream stream = new JVMAudioInputStream(inputStream);
		  final Yin y = new Yin((float) sampleRate,audioBufferSize);

          final AudioDispatcher dispatcher = new AudioDispatcher(stream, audioBufferSize, 0);
          dispatcher.addAudioProcessor(new AudioProcessor() {
			private FFT  fft = new FFT(512);
			@Override
			public void processingFinished() {
			}
			
	
			@Override
			public boolean process(AudioEvent audioEvent) {
				float[] audioFloatBuffer = audioEvent.getFloatBuffer();
				fft.forwardTransform(audioFloatBuffer);
				fft.backwardsTransform(audioFloatBuffer);
				PitchDetectionResult r = y.getPitch(audioFloatBuffer);
				assertTrue(r.isPitched());
				assertEquals(f0,(double) r.getPitch(), 0.04, "Expected around 440Hz");
				return true;
			}
		});
        dispatcher.run();
	}
}
