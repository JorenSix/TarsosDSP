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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.hogent.tarsos.dsp.util.AudioFloatConverter;



public class TestUtilities {

	/**
	 * Constructs and returns a buffer of a two seconds long pure sine of 440Hz
	 * sampled at 44.1kHz.
	 * 
	 * @return A buffer of a two seconds long pure sine (440Hz) sampled at
	 *         44.1kHz.
	 */
	public static float[] audioBufferSine() {
		final double sampleRate = 44100.0;
		final double f0 = 440.0;
		final double amplitudeF0 = 0.5;
		final double seconds = 4.0;
		final float[] buffer = new float[(int) (seconds * sampleRate)];
		for (int sample = 0; sample < buffer.length; sample++) {
			final double time = sample / sampleRate;
			buffer[sample] = (float) (amplitudeF0 * Math.sin(2 * Math.PI * f0 * time));
		}
		return buffer;
	}
	
	/**
	 * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
	 *         of a flute played double forte at A6 (theoretically 440Hz) without vibrato
	 */
	public static float[] audioBufferFlute() {
		int lengthInSamples = 4096;
		String file = "/be/hogent/tarsos/dsp/test/resources/flute.novib.ff.A4.wav";
		return audioBufferFile(file,lengthInSamples);
	}
	
	public static File fluteFile(){
		String file = "/be/hogent/tarsos/dsp/test/resources/flute.novib.ff.A4.wav";
		final URL url = TestUtilities.class.getResource(file);
		try {
			return new File(new URI(url.toString()));
		} catch (URISyntaxException e) {
			return null;
		}
	}
	
	public static File onsetsAudioFile(){
		String file = "/be/hogent/tarsos/dsp/test/resources/NR45.wav";
		final URL url = TestUtilities.class.getResource(file);
		try {
			return new File(new URI(url.toString()));
		} catch (URISyntaxException e) {
			return null;
		}
	}
	
	/**
	 * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
	 *         of a flute played double forte at B6 (theoretically 1975.53Hz) without vibrato
	 */
	public static float[] audioBufferHighFlute() {
		int lengthInSamples = 4096;
		String file = "/be/hogent/tarsos/dsp/test/resources/flute.novib.ff.B6.wav";
		return audioBufferFile(file,lengthInSamples);
	}
	
	/**
	 * Reads the contents of a file.
	 * 
	 * @param name
	 *            the name of the file to read
	 * @return the contents of the file if successful, an empty string
	 *         otherwise.
	 */
	public static String readFile(final String name) {
		FileReader fileReader = null;
		final StringBuilder contents = new StringBuilder();
		try {
			final File file = new File(name);
			if (!file.exists()) {
				throw new IllegalArgumentException("File " + name + " does not exist");
			}
			fileReader = new FileReader(file);
			final BufferedReader reader = new BufferedReader(fileReader);
			String inputLine = reader.readLine();
			while (inputLine != null) {
				contents.append(inputLine).append("\n");
				inputLine = reader.readLine();
			}
			reader.close();
		} catch (final IOException i1) {
			throw new RuntimeException(i1);
		}
		return contents.toString();
	}
	
	/**
	 * Reads the contents of a file in a jar.
	 * 
	 * @param path
	 *            the path to read e.g. /package/name/here/help.html
	 * @return the contents of the file when successful, an empty string
	 *         otherwise.
	 */
	public static String readFileFromJar(final String path) {
		final StringBuilder contents = new StringBuilder();
		final URL url = TestUtilities.class.getResource(path);
		URLConnection connection;
		try {
			connection = url.openConnection();
			final InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
			final BufferedReader reader = new BufferedReader(inputStreamReader);
			String inputLine;
			inputLine = reader.readLine();
			while (inputLine != null) {
				contents.append(new String(inputLine.getBytes(), "UTF-8")).append("\n");
				inputLine = reader.readLine();
			}
			reader.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} catch (final NullPointerException e) {
			throw new RuntimeException(e);
		}
		return contents.toString();
	}
	
	/**
	 * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
	 *         of a piano played double forte at A4 (theoretically 440Hz)
	 */
	public static float[] audioBufferPiano() {
		int lengthInSamples = 4096;
		String file = "/be/hogent/tarsos/dsp/test/resources/piano.ff.A4.wav";
		return audioBufferFile(file,lengthInSamples);
	}
	
	/**
	 	 * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
	 *         of a piano played double forte at C3 (theoretically 130.81Hz)
	 */
	public static float[] audioBufferLowPiano() {
		int lengthInSamples = 4096;
		String file = "/be/hogent/tarsos/dsp/test/resources/piano.ff.C3.wav";
		return audioBufferFile(file,lengthInSamples);
	}
	
	private static float[] audioBufferFile(String file,int lengthInSamples){
		float[] buffer = new float[lengthInSamples];
		try {
			final URL url = TestUtilities.class.getResource(file);
			AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
			AudioFormat format = audioStream.getFormat();
			AudioFloatConverter converter = AudioFloatConverter.getConverter(format);
			byte[] bytes = new byte[lengthInSamples * format.getSampleSizeInBits()];
			audioStream.read(bytes);		
			converter.toFloatArray(bytes, buffer);
		} catch (IOException e) {
			throw new Error("Test audio file should be present.");
		} catch (UnsupportedAudioFileException e) {
			throw new Error("Test audio file format should be supported.");
		}		
		return buffer;
	}

	/**
	 * 
	 * @return a half a second long silent buffer (all zeros), at 44.1kHz.
	 */
	public static float[] audioBufferSilence() {
		final double sampleRate = 44100.0;
		final double seconds = 0.5;
		final float[] buffer = new float[(int) (seconds * sampleRate)];
		return buffer;
	}

	@Test
	public void testNothing(){
		//a test to please maven
		assertEquals(true,true);
	}
}
