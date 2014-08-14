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
import java.lang.reflect.InvocationTargetException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.effects.DelayEffect;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;

public class Delay extends JFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5020248948695915733L;
	private AudioDispatcher dispatcher;
	private DelayEffect delayEffect;
	private GainProcessor inputGain;
	
	private int defaultInputGain = 100;//%
	private int defaultDelay = 200;//ms
	private int defaultDecay = 50;//%
	
	PropertyChangeListener mixerChangedListener = new PropertyChangeListener() {
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
	};
	
	
	public Delay() {
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Delay Effect Example");
		JPanel inputPanel = new InputPanel();
		inputPanel.addPropertyChangeListener("mixer",mixerChangedListener);
		
		
		final JSlider echoLengthSlider = new JSlider(1, 4000);
		echoLengthSlider.setValue(defaultDelay);
		echoLengthSlider.setPaintLabels(true);
		echoLengthSlider.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (delayEffect != null) {
					double echoLength = echoLengthSlider.getValue() / 1000.0;
					delayEffect.setEchoLength(echoLength);
				}
				
			}
			
		});
		
		final JSlider decaySlider = new JSlider(0,100);
		decaySlider.setValue(defaultDecay);
		decaySlider.setPaintLabels(true);
		decaySlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (delayEffect != null) {
					double decay = decaySlider.getValue() / 100.0;
					delayEffect.setDecay(decay);
				}
			}
		});
		
		
		
		JPanel params = new JPanel(new GridLayout(0,1));
		params.setBorder(new TitledBorder("2. Set the algorithm parameters"));
		
		JLabel label = new JLabel("Decay");
		label.setToolTipText("The decay factor in % (100 is no change).");
		params.add(label);
		params.add(decaySlider);
		
		label = new JLabel("Echo length (in ms)");
		label.setToolTipText("The echo lengt in ms.");
		params.add(label);
		params.add(echoLengthSlider);	
		
		final JSlider gainSlider = new JSlider(0,200);
		gainSlider.setValue(defaultInputGain);
		gainSlider.setPaintLabels(true);
		gainSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (inputGain != null) {
					double gainValue = gainSlider.getValue() / 100.0;
					inputGain.setGain(gainValue);
				}
			}
		});
		
		JPanel gainPanel = new JPanel(new BorderLayout());
		label = new JLabel("Gain (in %)");
		label.setToolTipText("Volume in % (100 is no change).");
		gainPanel.add(label,BorderLayout.NORTH);
		gainPanel.add(gainSlider,BorderLayout.CENTER);
		gainPanel.setBorder(new TitledBorder("3. Optionally change the input volume"));
		
		add(inputPanel,BorderLayout.NORTH);
		add(params,BorderLayout.CENTER);
		add(gainPanel,BorderLayout.SOUTH);
	}
	


	private void setNewMixer(Mixer mixer) throws LineUnavailableException,
			UnsupportedAudioFileException {

		if (dispatcher != null) {
			dispatcher.stop();
		}

		float sampleRate = 44100;
		int bufferSize = 1024;
		int overlap = 0;

		final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true,
				false);
		final DataLine.Info dataLineInfo = new DataLine.Info(
				TargetDataLine.class, format);
		TargetDataLine line;
		line = (TargetDataLine) mixer.getLine(dataLineInfo);
		final int numberOfSamples = bufferSize;
		line.open(format, numberOfSamples);
		line.start();
		final AudioInputStream stream = new AudioInputStream(line);
		final TarsosDSPAudioInputStream audioStream = new JVMAudioInputStream(stream);

		// create a new dispatcher
		dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);
		
		delayEffect = new DelayEffect(defaultDelay/1000.0,defaultDecay/100.0,sampleRate);
		inputGain = new GainProcessor(defaultInputGain/100.0);
		
		//add  processors
		dispatcher.addAudioProcessor(inputGain);
		dispatcher.addAudioProcessor(delayEffect);
		dispatcher.addAudioProcessor(new AudioPlayer(format));

		// run the dispatcher (on a new thread).
		new Thread(dispatcher, "Audio dispatching").start();
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
				JFrame frame = new Delay();
				frame.pack();
				frame.setVisible(true);
			}
		});
	}
}
