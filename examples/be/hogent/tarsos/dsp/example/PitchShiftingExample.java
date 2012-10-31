package be.hogent.tarsos.dsp.example;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioPlayer;
import be.hogent.tarsos.dsp.GainProcessor;
import be.hogent.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.hogent.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters;
import be.hogent.tarsos.dsp.resample.RateTransposer;

public class PitchShiftingExample extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3830419374132803358L;

	private JFileChooser fileChooser;

	private AudioDispatcher dispatcher;
	private WaveformSimilarityBasedOverlapAdd wsola;
	private GainProcessor gain;
	private AudioPlayer audioPlayer;
	private RateTransposer rateTransposer;
	private double currentFactor;// pitch shift factor
	private double sampleRate;
	
	private final JSlider factorSlider;
	private final JLabel factorLabel;
	private final JSlider gainSlider;
	private final JCheckBox originalTempoCheckBox;
	private final JSpinner centsSpinner;
	
	
	
	private ChangeListener parameterSettingChangedListener = new ChangeListener(){
@Override
		public void stateChanged(ChangeEvent arg0) {
			if (arg0.getSource() instanceof JSpinner) {
				int centValue = Integer.valueOf(((JSpinner) arg0.getSource())
						.getValue().toString());
				currentFactor = centToFactor(centValue);
				factorSlider.removeChangeListener(this);
				factorSlider.setValue((int) Math.round(currentFactor * 100));
				factorSlider.addChangeListener(this);
			} else if (arg0.getSource() instanceof JSlider) {
				currentFactor = factorSlider.getValue() / 100.0;
				centsSpinner.removeChangeListener(this);
				centsSpinner.setValue(factorToCents(currentFactor));
				centsSpinner.addChangeListener(this);
			}
			factorLabel.setText("Factor " + Math.round(currentFactor * 100) + "%");
			if (PitchShiftingExample.this.dispatcher != null) {				 
				 if(originalTempoCheckBox.getModel().isSelected()){
					 wsola.setParameters(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(currentFactor, sampleRate));
				 } else {
					 wsola.setParameters(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(1, sampleRate));
				 }
				 rateTransposer.setFactor(currentFactor);
			 }
		}}; 
		
	public PitchShiftingExample(){
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Pitch shifting: change the pitch of your audio.");
		
		originalTempoCheckBox = new JCheckBox("Keep original tempo?", true);
		originalTempoCheckBox.addChangeListener(parameterSettingChangedListener);
		
		currentFactor = 1;
		
		factorSlider = new JSlider(20, 250);
		factorSlider.setValue(100);
		factorSlider.setPaintLabels(true);
		factorSlider.addChangeListener(parameterSettingChangedListener);
		
		JPanel fileChooserPanel = new JPanel(new BorderLayout());
		fileChooserPanel.setBorder(new TitledBorder("1. Choose your audio (wav mono)"));
		
		fileChooser = new JFileChooser();
		
		JButton chooseFileButton = new JButton("Choose a file...");
		chooseFileButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(PitchShiftingExample.this);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fileChooser.getSelectedFile();
	                startFile(file);
	            } else {
	                //canceled
	            }
			}			
		});
		fileChooserPanel.add(chooseFileButton,BorderLayout.CENTER);
		
		
		gainSlider = new JSlider(0,200);
		gainSlider.setValue(100);
		gainSlider.setPaintLabels(true);
		gainSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (PitchShiftingExample.this.dispatcher != null) {
					double gainValue = gainSlider.getValue() / 100.0;
					gain.setGain(gainValue);
				}
			}
		});
		
		
		JPanel params = new JPanel(new BorderLayout());
		params.setBorder(new TitledBorder("2. Set the algorithm parameters"));
		
		JLabel label = new JLabel("Factor 100%");
		label.setToolTipText("The pitch shift factor in % (100 is no change, 50 is double pitch, 200 half).");
		factorLabel = label;
		params.add(label,BorderLayout.NORTH);
		params.add(factorSlider,BorderLayout.CENTER);
		
		JPanel subPanel = new JPanel(new GridLayout(2, 2));
			
		centsSpinner = new JSpinner();
		centsSpinner.addChangeListener(parameterSettingChangedListener);
		label = new JLabel("Pitch shift in cents");
		label.setToolTipText("Pitch shift in cents.");
		subPanel.add(label);
		subPanel.add(centsSpinner);

		label = new JLabel("Keep original tempo");
		label.setToolTipText("Pitch shift in cents.");
		subPanel.add(label);
		subPanel.add(originalTempoCheckBox);
		
		params.add(subPanel,BorderLayout.SOUTH);
		
		JPanel gainPanel = new JPanel(new BorderLayout());
		label = new JLabel("Gain (in %)");
		label.setToolTipText("Volume in % (100 is no change).");
		gainPanel.add(label,BorderLayout.NORTH);
		gainPanel.add(gainSlider,BorderLayout.CENTER);
		gainPanel.setBorder(new TitledBorder("3. Optionally change the volume"));
		
		this.add(fileChooserPanel,BorderLayout.NORTH);
		this.add(params,BorderLayout.CENTER);
		this.add(gainPanel,BorderLayout.SOUTH);
		
	}
	
	private void startFile(File inputFile){
		if(dispatcher != null){
			dispatcher.stop();
		}
		AudioFormat format;
		try {
			format = AudioSystem.getAudioFileFormat(inputFile).getFormat();
		
			rateTransposer = new RateTransposer(currentFactor);
			gain = new GainProcessor(1.0);
			audioPlayer = new AudioPlayer(format);
			sampleRate = format.getSampleRate();
			
			 if(originalTempoCheckBox.getModel().isSelected()){
				 wsola = new WaveformSimilarityBasedOverlapAdd(Parameters.musicDefaults(currentFactor, sampleRate));
			 } else {
				 wsola = new WaveformSimilarityBasedOverlapAdd(Parameters.musicDefaults(1, sampleRate));
			 }
			dispatcher = AudioDispatcher.fromFile(inputFile,wsola.getInputBufferSize(),wsola.getOverlap());
			wsola.setDispatcher(dispatcher);
			dispatcher.addAudioProcessor(wsola);
			dispatcher.addAudioProcessor(rateTransposer);
			dispatcher.addAudioProcessor(gain);
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
	
	
	public static void main(String[] argv) {
		try {
			startGui();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private double centToFactor(double cents){
		return 1 / Math.pow(Math.E,cents*Math.log(2)/1200/Math.log(Math.E)); 
	}
	private double factorToCents(double factor){
		return 1200 * Math.log(1/factor) / Math.log(2); 
	}
	
	private static void startGui() throws InterruptedException, InvocationTargetException{
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					//ignore failure to set default look en feel;
				}
				JFrame frame = new PitchShiftingExample();
				frame.pack();
				frame.setSize(400,350);
				frame.setVisible(true);
			}
		});
	}
}
