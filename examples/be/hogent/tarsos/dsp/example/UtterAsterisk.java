package be.hogent.tarsos.dsp.example;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.PitchProcessor;
import be.hogent.tarsos.dsp.PitchProcessor.DetectedPitchHandler;
import be.hogent.tarsos.dsp.PitchProcessor.PitchEstimationAlgorithm;

public class UtterAsterisk extends JFrame implements DetectedPitchHandler {
	
	AudioDispatcher dispatcher;
	Mixer currentMixer;
	UtterAsteriksPanel panel;
	
	public UtterAsterisk(){
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("UtterAsteriks");
		
		JPanel inputPanel = new InputPanel();
		add(inputPanel,BorderLayout.NORTH);
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
		panel = new UtterAsteriksPanel();
		
		this.add(panel,BorderLayout.CENTER);
	}
	
	private void setNewMixer(Mixer mixer) throws LineUnavailableException, UnsupportedAudioFileException {

		if(dispatcher!= null){
			dispatcher.stop();
		}
		currentMixer = mixer;
		
		float sampleRate = 44100;
		int bufferSize = 256;
		int overlap = 0;
		
		//textArea.append("Started listening with " + mixer.getMixerInfo().getName() + "\n\tparams: " + threshold + "dB\n");

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

	/**
	 * 
	 */
	private static final long serialVersionUID = 4787721035066991486L;

	public static void main(String... strings) throws InterruptedException,
			InvocationTargetException {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new UtterAsterisk();
				frame.pack();
				frame.setSize(640,480);
				frame.setVisible(true);
			}
		});
	}

	@Override
	public void handlePitch(float pitch, float probability, float timeStamp,
			float progress) {
		panel.setMarker(timeStamp,pitch);
	}

}
