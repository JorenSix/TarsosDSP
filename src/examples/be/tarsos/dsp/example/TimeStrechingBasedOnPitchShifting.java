package be.tarsos.dsp.example;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.sound.sampled.LineUnavailableException;
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
import be.tarsos.dsp.PitchShifter;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.resample.RateTransposer;

public class TimeStrechingBasedOnPitchShifting extends JFrame implements TarsosDSPDemo {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7188163235158960778L;
	private final JFileChooser fileChooser;
	private final JSlider factorSlider;
	private final JLabel factorLabel;
	
	private double currentFactor;// pitch shift factor
	private AudioDispatcher dispatcher;
	private PitchShifter pitchShifter;
	private RateTransposer rateTransposer;
	
	private ChangeListener parameterSettingChangedListener = new ChangeListener(){
@Override
		public void stateChanged(ChangeEvent arg0) {
			currentFactor = factorSlider.getValue() / 100.0;
			factorLabel.setText("Factor " + Math.round(currentFactor * 100) + "%");
			if (TimeStrechingBasedOnPitchShifting.this.dispatcher != null) {				 
				pitchShifter.setPitchShiftFactor((float) currentFactor);
				//rateTransposer.setFactor(currentFactor);
			}
		}}; 
	
	public TimeStrechingBasedOnPitchShifting(){
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Pitch shifting: change the tempo of your audio.");
		currentFactor = 1.;
		JPanel fileChooserPanel = new JPanel(new BorderLayout());
		fileChooserPanel.setBorder(new TitledBorder("1... Or choose your audio (wav mono)"));
		
		fileChooser = new JFileChooser();
		
		JButton chooseFileButton = new JButton("Choose a file...");
		chooseFileButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(TimeStrechingBasedOnPitchShifting.this);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fileChooser.getSelectedFile();
	                startFile(file);
	            } else {
	                //canceled
	            }
			}

		
		});
		fileChooserPanel.add(chooseFileButton);
		fileChooser.setLayout(new BoxLayout(fileChooser, BoxLayout.PAGE_AXIS));
		this.add(fileChooserPanel,BorderLayout.NORTH);
		
		JPanel params = new JPanel(new BorderLayout());
		params.setBorder(new TitledBorder("2. Set the algorithm parameters"));
		
		factorSlider = new JSlider(20, 250);
		factorSlider.setValue(120);
		factorSlider.setPaintLabels(true);
		factorSlider.addChangeListener(parameterSettingChangedListener);
		
		JLabel label = new JLabel("Factor 100%");
		label.setToolTipText("The pitch shift factor in % (100 is no change, 50 is double pitch, 200 half).");
		factorLabel = label;
		params.add(label,BorderLayout.NORTH);
		params.add(factorSlider,BorderLayout.CENTER);
		this.add(params,BorderLayout.CENTER);
		
	}
	
	private void startFile(File file) {
		int size = 4096;
		int overlap = 4096 - 128;
		int samplerate = 44100;
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(file.getAbsolutePath(), samplerate, size, overlap);
		pitchShifter = new PitchShifter(currentFactor, samplerate, size, overlap);
		
		//rateTransposer = new RateTransposer(currentFactor);
		
		d.addAudioProcessor(pitchShifter);
		//d.addAudioProcessor(rateTransposer);
		try {
			d.addAudioProcessor(new AudioPlayer(d.getFormat()));
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		dispatcher = d ;
		new Thread(d).start();
		
	}		
	
	public static void main(String... args){
		new TimeStrechingBasedOnPitchShifting().start();
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return "TimeStrechingBasedOnPitchShifting";
	}

	@Override
	public String description() {
		return "Shows how to do time stretching by pitch shifting and resampling";
	}

	@Override
	public void start() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					try {
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					} catch (Exception e) {
						//ignore failure to set default look en feel;
					}
					JFrame frame = new TimeStrechingBasedOnPitchShifting();
					frame.pack();
					frame.setSize(400,450);
					frame.setVisible(true);
				}
			});
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
