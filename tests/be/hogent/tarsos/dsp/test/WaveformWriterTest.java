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

import java.io.FileNotFoundException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Ignore;
import org.junit.Test;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.SilenceDetector;
import be.hogent.tarsos.dsp.WaveformWriter;
import be.hogent.tarsos.dsp.util.Shared;

public class WaveformWriterTest {

	@Ignore
	@Test
	public void testSilenceWriter() throws UnsupportedAudioFileException,
			InterruptedException, LineUnavailableException,
			FileNotFoundException {
		float sampleRate = 44100;
		int bufferSize = 1024;
		int overlap = 0;

		// available mixers
		int index = 0;
		int selectedMixerIndex = 4;
		for (Mixer.Info mixer : AudioSystem.getMixerInfo()) {
			System.out.println(index + ": " + Shared.toLocalString(mixer));
			index++;
		}
		Mixer.Info selectedMixer = AudioSystem.getMixerInfo()[selectedMixerIndex];
		System.out.println("Selected mixer: " + Shared.toLocalString(selectedMixer));

		// open a line
		final Mixer mixer = AudioSystem.getMixer(selectedMixer);
		final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true,
				true);
		final DataLine.Info dataLineInfo = new DataLine.Info(
				TargetDataLine.class, format);
		TargetDataLine line;
		line = (TargetDataLine) mixer.getLine(dataLineInfo);
		final int numberOfSamples = bufferSize;
		line.open(format, numberOfSamples);
		line.start();
		final AudioInputStream stream = new AudioInputStream(line);

		// create a new dispatcher
		AudioDispatcher dispatcher = new AudioDispatcher(stream, bufferSize,
				overlap);

		WaveformWriter writer = new WaveformWriter(format,"01.file.wav");
		// add a processor, handle percussion event.
		dispatcher.addAudioProcessor(new SilenceDetector());
		dispatcher.addAudioProcessor(writer);

		// run the dispatcher (on the same thread, use start() to run it on
		// another thread).
		new Thread(dispatcher).start();

		Thread.sleep(3000);
		
		dispatcher.removeAudioProcessor(writer);
		writer = new WaveformWriter(format,"02.file.wav");
		dispatcher.addAudioProcessor(writer);
		
		Thread.sleep(3000);
		
		dispatcher.stop();
	}
}
