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
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.PitchShifter;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.resample.Resampler;

public class TimeStrechingBasedOnPitchShifting extends JFrame implements TarsosDSPDemo {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7188163235158960778L;
	private final JFileChooser fileChooser;
	private final JSlider factorSlider;
	private final JLabel factorLabel;
	
	private double currentFactor = 1.2;// pitch shift factor
	private AudioDispatcher dispatcher;
	private PitchShifter pitchShifter;
	
	private float[] buffer;
	
	private ChangeListener parameterSettingChangedListener = new ChangeListener(){
@Override
		public void stateChanged(ChangeEvent arg0) {
			currentFactor = factorSlider.getValue() / 100.0;
			factorLabel.setText("Factor " + Math.round(currentFactor * 100) + "%");
			if (TimeStrechingBasedOnPitchShifting.this.dispatcher != null) {				 
				pitchShifter.setPitchShiftFactor((float) currentFactor);
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
		factorSlider.setValue((int) (currentFactor*100));
		factorSlider.setPaintLabels(true);
		factorSlider.addChangeListener(parameterSettingChangedListener);
		
		JLabel label = new JLabel("Factor 100%");
		label.setText("Factor " + Math.round(currentFactor * 100) + "%");
		label.setToolTipText("The tempo factor in % (100 is no change, 50 is double tempo, 200 half).");
		factorLabel = label;
		params.add(label,BorderLayout.NORTH);
		params.add(factorSlider,BorderLayout.CENTER);
		this.add(params,BorderLayout.CENTER);
	}
	
	private void startFile(File file) {
		final int size = 2048;
		final int overlap = 2048 - 128;
		int samplerate = 44100;
		final AudioDispatcher d = AudioDispatcherFactory.fromPipe(file.getAbsolutePath(), samplerate, size, overlap);
		pitchShifter = new PitchShifter(1.0/currentFactor, samplerate, size, overlap);
		
		d.addAudioProcessor(new AudioProcessor() {
			@Override
			public void processingFinished() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				buffer = audioEvent.getFloatBuffer();
				return true;
			}
		});
		
		d.addAudioProcessor(pitchShifter);
		
		d.addAudioProcessor(new AudioProcessor() {
			Resampler r= new Resampler(false,0.1,4.0);
			@Override
			public void processingFinished() {
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				
				float factor = (float) (currentFactor);
				float[] src = audioEvent.getFloatBuffer();
				float[] out = new float[(int) ((size-overlap) * factor)];
				r.process(factor, src, overlap,size-overlap, false, out, 0, out.length);
				//The size of the output buffer changes (according to factor).
				d.setStepSizeAndOverlap(out.length, 0);
				
				audioEvent.setFloatBuffer(out);
				audioEvent.setOverlap(0);
		
				return true;
			}
		});
		//d.addAudioProcessor(rateTransposer);
		try {
			d.addAudioProcessor(new AudioPlayer(d.getFormat()));
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		d.addAudioProcessor(new AudioProcessor() {
			
			@Override
			public void processingFinished() {
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				d.setStepSizeAndOverlap(size, overlap);
				d.setAudioFloatBuffer(buffer);
				audioEvent.setFloatBuffer(buffer);
				audioEvent.setOverlap(overlap);
				return true;
			}
		});
		dispatcher = d ;
		new Thread(d).start();
		
	}		
	
	public static void main(String... args){
		new TimeStrechingBasedOnPitchShifting().start();
	}

	@Override
	public String name() {
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
