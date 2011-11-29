package be.hogent.tarsos.dsp.example;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.PercussionOnsetDetector;
import be.hogent.tarsos.dsp.PercussionOnsetDetector.PercussionHandler;

public class PercussionDetectorInterface extends JFrame implements PercussionHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3501426880288136245L;

	private final JTextArea textArea;
	ArrayList<Clip> clipList;
	int counter;
	double sensitivity;
	double threshold;

	public PercussionDetectorInterface() {
		this.setLayout(new GridLayout(0, 1));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Percussion Detector");
		
		this.sensitivity = PercussionOnsetDetector.DEFAULT_SENSITIVITY;
		this.threshold = PercussionOnsetDetector.DEFAULT_THRESHOLD;
		
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
		
		
		JSlider sensitivitySlider = initializeSensitivitySlider();		
		JSlider thresholdSlider = initialzeThresholdSlider();		
		JPanel params = new JPanel(new GridLayout(0,1));
		params.setBorder(new TitledBorder("Set the algorithm parameters"));
		
		JLabel label = new JLabel("Threshold");
		label.setToolTipText("Energy rise within a frequency bin necessary to count toward broadband total (dB).");
		params.add(label);
		params.add(thresholdSlider);
		label = new JLabel("Sensitivity");
		label.setToolTipText("Sensitivity of peak detector applied to broadband detection function (%).");
		params.add(label);
		params.add(sensitivitySlider);
		add(params);
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		add(new JScrollPane(textArea));
		
		clipList = new ArrayList<Clip>();
	
		addClips();
	}
	
	private JSlider initialzeThresholdSlider() {
		JSlider thresholdSlider = new JSlider(0,20);
		thresholdSlider.setValue((int)threshold);
		thresholdSlider.setPaintLabels(true);
		thresholdSlider.setPaintTicks(true);
		thresholdSlider.setMajorTickSpacing(5);
		thresholdSlider.setMinorTickSpacing(1);
		thresholdSlider.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
			    if (!source.getValueIsAdjusting()) {
			        threshold = source.getValue();
			        try {
						setNewMixer(currentMixer);
					} catch (LineUnavailableException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (UnsupportedAudioFileException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			    }
			}
		});
		return thresholdSlider;
	}

	private JSlider initializeSensitivitySlider(){
		JSlider sensitivitySlider = new JSlider(0,100);
		sensitivitySlider.setValue((int)sensitivity);
		sensitivitySlider.setPaintLabels(true);
		sensitivitySlider.setPaintTicks(true);
		sensitivitySlider.setMajorTickSpacing(20);
		sensitivitySlider.setMinorTickSpacing(10);
		sensitivitySlider.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
			    if (!source.getValueIsAdjusting()) {
			        sensitivity = source.getValue();
			        try {
						setNewMixer(currentMixer);
					} catch (LineUnavailableException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (UnsupportedAudioFileException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			    }
			}
		});
		return sensitivitySlider;
	}
	
	private void addClips(){
		String[] clips = {"50863__chipfork__Echopop.wav","50771__Digital_System__BOOM_reverb.wav","30667__HardPCM__HardBassDrum001.wav","30669__HardPCM__HardKick002.wav","33637__HerbertBoland__CinematicBoomNorm.wav"};
		for(String clipName : clips){
		
			try {
				final InputStream inputStream = this.getClass().getResourceAsStream("resources/" + clipName);
				AudioInputStream sound;
				sound = AudioSystem.getAudioInputStream(inputStream);
				  // load the sound into memory (a Clip)
			    DataLine.Info info = new DataLine.Info(Clip.class, sound.getFormat());
			    Clip clip = (Clip) AudioSystem.getLine(info);
			    clip.open(sound);
			    clipList.add(clip);
			} catch (UnsupportedAudioFileException e) {
				System.out.println(clipName);
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	

		}
	}

	AudioDispatcher dispatcher;
	Mixer currentMixer;
	private void setNewMixer(Mixer mixer) throws LineUnavailableException,
			UnsupportedAudioFileException {
		
		if(dispatcher!= null){
			dispatcher.stop();
		}
		currentMixer = mixer;
		
		float sampleRate = 44100;
		int bufferSize = 512;
		int overlap = 0;
		
		textArea.append("Started listening with " + mixer.getMixerInfo().getName() + "\n\tparams: " + sensitivity + "%, " + threshold + "dB\n");

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
		dispatcher.addAudioProcessor(new PercussionOnsetDetector(sampleRate,
				bufferSize, overlap, this,sensitivity,threshold));

		// run the dispatcher (on a new thread).
		new Thread(dispatcher,"Audio dispatching").start();
	}

	public void handlePercussion(double timestamp) {
		textArea.append("Percussion at:" + timestamp + "\n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
		Clip clip = clipList.get(counter % clipList.size());
		clip.setFramePosition(0);
		clip.start();
		counter++;
		if(counter == clipList.size()){
			Collections.shuffle(clipList);
		}
	}

	public static void main(String... strings) throws InterruptedException,
			InvocationTargetException {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new PercussionDetectorInterface();
				frame.pack();
				frame.setVisible(true);
			}
		});
	}
}
