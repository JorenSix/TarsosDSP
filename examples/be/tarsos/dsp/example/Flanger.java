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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
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
import be.tarsos.dsp.MultichannelToMono;
import be.tarsos.dsp.effects.FlangerEffect;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.jvm.WaveformWriter;

public class Flanger extends JFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5020248948695915733L;
	private AudioDispatcher dispatcher;
	private FlangerEffect flangerEffect;
	private GainProcessor inputGain;
	
	private int defaultInputGain = 100;//%
	private int defaultLength = 20;//ms
	private int defaultImpact = 50;//%
	private int defaultFrequency = 3;//Hz
	
	
	public Flanger() {
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener( new WindowAdapter() {
              @Override
              public void windowClosing(WindowEvent we) {
                  if(dispatcher != null){
                	  dispatcher.stop();
                  }
                  System.exit(0);
              }
          } );
		this.setTitle("Flanger Effect Example");
		
		JPanel inputPanel = buildInputPanel();		
		
		final JSlider flangerLengthSlider = new JSlider(1, 100);
		flangerLengthSlider.setValue(defaultLength);
		flangerLengthSlider.setPaintLabels(true);
		flangerLengthSlider.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (flangerEffect != null) {
					double flangerLength = flangerLengthSlider.getValue() / 1000.0;
					flangerEffect.setFlangerLength(flangerLength);
				}
				defaultLength = flangerLengthSlider.getValue();
			}
		});
		
		final JSlider impact = new JSlider(0,100);
		impact.setValue(defaultImpact);
		impact.setPaintLabels(true);
		impact.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (flangerEffect != null) {
					double depth = impact.getValue() / 100.0;
					flangerEffect.setWet(depth);
				}
				defaultImpact = impact.getValue();
			}
		});
		
		final JSlider lfoFrequency = new JSlider(0,100);
		lfoFrequency.setValue(defaultFrequency);
		lfoFrequency.setPaintLabels(true);
		lfoFrequency.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (flangerEffect != null) {
					double frequency = lfoFrequency.getValue() / 10.0;
					flangerEffect.setLFOFrequency(frequency);		
				}
				defaultFrequency = lfoFrequency.getValue();		
			}
		});
		
		
		
		JPanel params = new JPanel(new GridLayout(0,1));
		params.setBorder(new TitledBorder("2. Set the algorithm parameters"));
		
		JLabel label = new JLabel("Impact (%)");
		label.setToolTipText("The impact factor in % (100 is no change).");
		params.add(label);
		params.add(impact);
		
		label = new JLabel("Flanger length (in ms)");
		label.setToolTipText("The flanger buffer lengt in ms.");
		params.add(label);
		params.add(flangerLengthSlider);	
		
		label = new JLabel("Flanger LFO Frequency (in Hz x 10)");
		label.setToolTipText("The flanger LFO Frequency lengt in dHz.");
		params.add(label);
		params.add(lfoFrequency);
		
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
				defaultInputGain = gainSlider.getValue();
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
	


	private JPanel buildInputPanel() {
		JPanel fileChooserPanel = new JPanel(new BorderLayout());
		fileChooserPanel.setBorder(new TitledBorder("1... Or choose your audio (wav mono)"));
		
		final JFileChooser fileChooser = new JFileChooser();
		
		JButton chooseFileButton = new JButton("Choose a file...");
		chooseFileButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(Flanger.this);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fileChooser.getSelectedFile();
	                startFile(file,null);
	            } else {
	                //canceled
	            }
			}			
		});
		fileChooserPanel.add(chooseFileButton);
		fileChooser.setLayout(new BoxLayout(fileChooser, BoxLayout.PAGE_AXIS));
		

		JPanel inputSubPanel = new JPanel(new BorderLayout());
		JPanel inputPanel = new InputPanel();
		inputPanel.addPropertyChangeListener("mixer",
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						startFile(null,(Mixer) arg0.getNewValue());
					}
				});
		inputSubPanel.add(inputPanel,BorderLayout.NORTH);
		inputSubPanel.add(fileChooserPanel,BorderLayout.SOUTH);
		return inputSubPanel;
	}
	
	private void startFile(File inputFile, Mixer mixer) {
		if (dispatcher != null) {
			dispatcher.stop();
		}
		AudioFormat format;
		int bufferSize = 1024;
		int overlap = 0;
		double sampleRate = 44100;
		try {
			if(inputFile != null){
				format = AudioSystem.getAudioFileFormat(inputFile).getFormat();
				sampleRate = format.getSampleRate();
			}else{
				format = new AudioFormat((float) sampleRate, 16, 1, true,true);
			}
			
			inputGain = new GainProcessor(defaultInputGain/100.0);
			AudioPlayer audioPlayer = new AudioPlayer(format);	
			
			 if(inputFile == null){
				 DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
				TargetDataLine line;
				line = (TargetDataLine) mixer.getLine(dataLineInfo);
				line.open(format, bufferSize);
				line.start();
				final AudioInputStream stream = new AudioInputStream(line);
				final TarsosDSPAudioInputStream audioStream = new JVMAudioInputStream(stream);
				dispatcher = new AudioDispatcher(audioStream, bufferSize,overlap); 
			 }else{
					if(format.getChannels() != 1){
						dispatcher = AudioDispatcherFactory.fromFile(inputFile,bufferSize * format.getChannels(),overlap * format.getChannels());
						dispatcher.addAudioProcessor(new MultichannelToMono(format.getChannels(),true));
					}else{
						dispatcher = AudioDispatcherFactory.fromFile(inputFile,bufferSize,overlap);
					}
			 }
			 
			flangerEffect = new FlangerEffect(defaultLength/1000.0,defaultImpact/100.0,sampleRate,defaultFrequency/10.0);
				
			dispatcher.addAudioProcessor(flangerEffect);
			dispatcher.addAudioProcessor(inputGain);
			dispatcher.addAudioProcessor(new WaveformWriter(format, "flanger.wav"));
			dispatcher.addAudioProcessor(audioPlayer);

			Thread t = new Thread(dispatcher);
			t.start();
		} catch (UnsupportedAudioFileException e) {
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
				JFrame frame = new Flanger();
				frame.pack();
				frame.setVisible(true);
			}
		});
	}
}
