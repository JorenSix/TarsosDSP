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


package be.tarsos.dsp.example;

import java.awt.BorderLayout;
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
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;


public class OnsetDetector extends JFrame implements OnsetHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3501426880288136245L;

	private final JTextArea textArea;
	ArrayList<Clip> clipList;
	int counter;
	double threshold = 0.4;

	public OnsetDetector() {
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Percussion Detector");
			
		JPanel inputPanel = new InputPanel();
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
		
			
		JSlider thresholdSlider = initialzeThresholdSlider();		
		JPanel params = new JPanel(new GridLayout(0,1));
		params.setBorder(new TitledBorder("2. Set the algorithm parameters"));
		
		JLabel label = new JLabel("Threshold");
		label.setToolTipText("Peak picking threshold.");
		params.add(label);
		params.add(thresholdSlider);
		
		JPanel paramsAndInputPanel = new JPanel(new GridLayout(1,0));
		paramsAndInputPanel.add(inputPanel);
		paramsAndInputPanel.add(params);
		
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		
		this.add(paramsAndInputPanel,BorderLayout.NORTH);
		this.add(new JScrollPane(textArea),BorderLayout.CENTER);
		
		clipList = new ArrayList<Clip>();
	
		addClips();
	}
	
	private JSlider initialzeThresholdSlider() {
		JSlider thresholdSlider = new JSlider(0,100);
		thresholdSlider.setValue((int)threshold);
		thresholdSlider.setPaintLabels(true);
		thresholdSlider.setPaintTicks(true);
		thresholdSlider.setMajorTickSpacing(20);
		thresholdSlider.setMinorTickSpacing(10);
		thresholdSlider.setValue(25);
		thresholdSlider.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
			    if (!source.getValueIsAdjusting()) {
			       threshold = source.getValue()/100.0;
			       if(onsetDetector!=null){
			    	   onsetDetector.setThreshold(threshold);
			       }
			    }
			}
		});
		return thresholdSlider;
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

	ComplexOnsetDetector onsetDetector;
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
		
		textArea.append("Started listening with " + Shared.toLocalString(mixer.getMixerInfo().getName()) + "\n\tparams: " + threshold + "dB\n");

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
		JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
		// create a new dispatcher
		dispatcher = new AudioDispatcher(audioStream, bufferSize,
				overlap);

		onsetDetector = new ComplexOnsetDetector(bufferSize, threshold,0.07,-60);
		onsetDetector.setHandler(this);
		// add a processor, handle percussion event.
		dispatcher.addAudioProcessor(onsetDetector);

		// run the dispatcher (on a new thread).
		new Thread(dispatcher,"Audio dispatching").start();
	}

	public static void main(String... strings) throws InterruptedException,
			InvocationTargetException {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					//ignore failure to set default look en feel;
				}
				JFrame frame = new OnsetDetector();
				frame.pack();
				frame.setSize(640, 480);
				frame.setVisible(true);
			}
		});
	}

	@Override
	public void handleOnset(double time, double salience) {
		textArea.append("Percussion at:" + time + "\n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
		Clip clip = clipList.get(counter % clipList.size());
		clip.setFramePosition(0);
		clip.start();
		counter++;
		if(counter == clipList.size()){
			Collections.shuffle(clipList);
		}
	}
}
