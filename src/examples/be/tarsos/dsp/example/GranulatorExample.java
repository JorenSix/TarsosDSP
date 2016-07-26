package be.tarsos.dsp.example;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.granulator.Granulator;
import be.tarsos.dsp.granulator.OptimizedGranulator;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;

public class GranulatorExample extends JFrame {

	/**
		 * 
		 */
	private static final long serialVersionUID = 8730005768957982611L;

	private OptimizedGranulator granulator;
	private float[] orig;
	int sampleRate = 48000;
	int bufferSize = 256;
	

	final JSlider timeStretchSlider = new JSlider(-3000, 3000);
	final JSlider pitchShiftSlider = new JSlider(-3000, 3000);
	final JSlider grainSizeSlider = new JSlider(1, 300);
	final JSlider grainIntervallSlider = new JSlider(1, 300);
	final JSlider grainRandomnesslSlider = new JSlider(0, 1000);
	final JSlider positionSlider = new JSlider(0, 20000);

	public GranulatorExample() {
		setLayout(new GridLayout(0, 2));
		final JLabel openFileLabel = new JLabel("Open file:");
		final JLabel timeStretchLabel = new JLabel("Time stretch factor (%): 100%");
		final JLabel pitchShiftLabel = new JLabel("Pitch shift factor (%): 100%");
		final JLabel grainSizeLabel = new JLabel("Grain size (ms): 100");
		final JLabel grainIntervalLabel = new JLabel("Grain interval (ms): 40");
		final JLabel grainRandomnessLabel = new JLabel("Grain randomness (%): 10");
		final JLabel positionLabel = new JLabel("Position (s): 0");
		
		
		final JFileChooser fileChooser = new JFileChooser();
		final JButton openFileButton  = new JButton("Open file...");
		openFileButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(GranulatorExample.this);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fileChooser.getSelectedFile();
	                openFile(file.getAbsolutePath());
	            } else {
	                //canceled
	            }
			}
		});
		

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		timeStretchSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				float currentFactor = (float) timeStretchSlider.getValue() / 1000.0f;
				timeStretchLabel.setText(String.format(
						"Time stretch factor: %.1f", currentFactor * 100));
				if(granulator!=null)
				granulator.setTimestretchFactor(currentFactor);
			}
		});

		pitchShiftSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				float currentFactor = (float) pitchShiftSlider.getValue() / 1000.0f;
				pitchShiftLabel.setText(String.format(
						"Pitch shift factor: %.1f", currentFactor * 100));
				if(granulator!=null)
				granulator.setPitchShiftFactor(currentFactor);
			}
		});
		
		grainIntervallSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				int grainInterval = grainIntervallSlider.getValue();
				grainIntervalLabel.setText(String.format(
						"Grain interval (ms): %d", grainInterval));
				if(granulator!=null)
				granulator.setGrainInterval(grainInterval);
			}
		});
		
		grainSizeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				int grainSize = grainSizeSlider.getValue();
				grainSizeLabel.setText(String.format(
						"Grain size (ms): %d", grainSize));
				if(granulator!=null)
				granulator.setGrainSize(grainSize);
			}
		});
		
		grainRandomnesslSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				float grainRandomness = (float) grainRandomnesslSlider.getValue() / 1000.0f;
				grainRandomnessLabel.setText(String.format(
						"Grain randomness (%%): %.1f", grainRandomness * 100));
				if(granulator!=null)
				granulator.setGrainRandomness(grainRandomness);
			}
		});
		
		positionSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				float position = (float) positionSlider.getValue() / 1000.0f;
				positionLabel.setText(String.format("Position (s): %.3f", position));
				if(granulator!=null)
				granulator.setPosition(position);
			}
		});

		timeStretchSlider.setValue(1000);
		pitchShiftSlider.setValue(950);
		positionSlider.setValue(0);
		grainRandomnesslSlider.setValue(0);
		grainSizeSlider.setValue(100);
		grainIntervallSlider.setValue(70);
		this.add(openFileLabel);
		this.add(openFileButton);
		this.add(timeStretchLabel);
		this.add(timeStretchSlider);
		this.add(pitchShiftLabel);
		this.add(pitchShiftSlider);
		this.add(grainIntervalLabel);
		this.add(grainIntervallSlider);
		this.add(grainSizeLabel);
		this.add(grainSizeSlider);
		this.add(grainRandomnessLabel);
		this.add(grainRandomnesslSlider);
		this.add(positionLabel);
		this.add(positionSlider);
		
		openFile("/home/joren/Desktop/sort/christina_40s-80s.wav");
	}
	
	private void openFile(String audioFile){
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(audioFile, sampleRate,bufferSize,0);
		granulator = new OptimizedGranulator(sampleRate, bufferSize);
		d.addAudioProcessor(new AudioProcessor() {

			@Override
			public void processingFinished() {
			}

			@Override
			public boolean process(AudioEvent audioEvent) {
				orig = audioEvent.getFloatBuffer();
				return true;
			}
		});
		d.addAudioProcessor(granulator);
		try {
			d.addAudioProcessor(new AudioPlayer(d.getFormat()));
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		d.addAudioProcessor(new AudioProcessor() {

			@Override
			public void processingFinished() {
				// TODO Auto-generated method stub
			}

			@Override
			public boolean process(AudioEvent audioEvent) {
				audioEvent.setFloatBuffer(orig);
				return true;
			}
		});

		granulator.setGrainInterval(grainIntervallSlider.getValue());
		granulator.setTimestretchFactor(timeStretchSlider.getValue()/1000.0f);
		granulator.setPitchShiftFactor(pitchShiftSlider.getValue()/1000.0f);
		granulator.setPosition(positionSlider.getValue()/1000f);
		granulator.setGrainRandomness(grainRandomnesslSlider.getValue()/100.0f);
		
		new Thread(d).start();

	}

	public static void main(String[] args) {
		GranulatorExample g = new GranulatorExample();
		g.pack();
		g.setVisible(true);
	}

}
