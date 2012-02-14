/**
*
*  TarsosDSP is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*
**/
package be.hogent.tarsos.dsp.example;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.PitchProcessor;
import be.hogent.tarsos.dsp.PitchProcessor.DetectedPitchHandler;
import be.hogent.tarsos.dsp.PitchProcessor.PitchEstimationAlgorithm;

public class PitchDetector extends JFrame implements DetectedPitchHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3501426880288136245L;

	private final JTextArea textArea;
	ArrayList<Clip> clipList;
	int counter;
	AudioDispatcher dispatcher;
	Mixer currentMixer;

	public PitchDetector() {
		this.setLayout(new GridLayout(0, 1));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Pitch Detector");
		
		JPanel inputPanel = new InputPanel();
		add(inputPanel);
		inputPanel.addPropertyChangeListener("mixer",
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						try {
							setNewMixer((Mixer) arg0.getNewValue());
						} catch (LineUnavailableException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (UnsupportedAudioFileException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
	
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		add(new JScrollPane(textArea));
	}


	
	private void setNewMixer(Mixer mixer) throws LineUnavailableException,
			UnsupportedAudioFileException {
		
		if(dispatcher!= null){
			dispatcher.stop();
		}
		currentMixer = mixer;
		
		float sampleRate = 44100;
		int bufferSize = 512;
		int overlap = 0;
		
		textArea.append("Started listening with " + mixer.getMixerInfo().getName() + "\n");

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
		dispatcher = new AudioDispatcher(stream, bufferSize,
				overlap);

		// add a processor, handle percussion event.
		dispatcher.addAudioProcessor(new PitchProcessor(PitchEstimationAlgorithm.YIN, sampleRate, bufferSize, overlap, 0, this));
		

		// run the dispatcher (on a new thread).
		new Thread(dispatcher,"Audio dispatching").start();
	}

	public static void main(String... strings) throws InterruptedException,
			InvocationTargetException {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new PitchDetector();
				frame.pack();
				frame.setVisible(true);
			}
		});
	}


	@Override
	public void handlePitch(float pitch, float probability, float timeStamp,
			float progress) {
		if(pitch != -1){
			String message = String.format("Pitch detected at %.2fs: %.2fHz ( %.2f probability )\n", timeStamp,pitch,probability);
			textArea.append(message);
			textArea.setCaretPosition(textArea.getDocument().getLength());
		}
	}
}
