package be.hogent.tarsos.dsp.example;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioFile;
import be.hogent.tarsos.dsp.AudioPlayer;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.PipeDecoder;
import be.hogent.tarsos.dsp.SpectralPeakFollower;
import be.hogent.tarsos.dsp.SpectralPeakFollower.SpectralPeak;
import be.hogent.tarsos.dsp.ui.AxisUnit;
import be.hogent.tarsos.dsp.ui.CoordinateSystem;
import be.hogent.tarsos.dsp.ui.LinkedPanel;
import be.hogent.tarsos.dsp.ui.ViewPort;
import be.hogent.tarsos.dsp.ui.ViewPort.ViewPortChangedListener;
import be.hogent.tarsos.dsp.ui.layers.AmplitudeAxisLayer;
import be.hogent.tarsos.dsp.ui.layers.BackgroundLayer;
import be.hogent.tarsos.dsp.ui.layers.DragMouseListenerLayer;
import be.hogent.tarsos.dsp.ui.layers.HorizontalFrequencyAxisLayer;
import be.hogent.tarsos.dsp.ui.layers.SelectionLayer;
import be.hogent.tarsos.dsp.ui.layers.SpectrumLayer;
import be.hogent.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;

public class SpectralPeaksExample extends JFrame {
	
	private SpectrumLayer spectrumLayer;
	private SpectrumLayer noiseFloorLayer;
	private LinkedPanel spectrumPanel;
	private JTextArea textArea;
	private JSlider frameSlider;
	
	private AudioDispatcher dispatcher;
	private AudioDispatcher player;
	
	private int sampleRate;
	private int fftsize;
	private int stepsize;//50% overlap
	private int noiseFloorMedianFilterLenth;//35
	private float noiseFloorFactor;
	private String fileName;
	private int numberOfSpectralPeaks;

	private final Integer[] fftSizes = {256,512,1024,2048,4096,8192,16384,32768,65536,131072};
	private final Integer[] inputSampleRate = {22050,44100,192000};
	
	private final List<SpectralInfo> spectalInfo;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5600205438242149179L;
	
	public SpectralPeaksExample(String startDir){		
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Spectral Peaks");
		
		spectalInfo = new ArrayList<SpectralInfo>();
		
		JPanel subPanel = new JPanel();
		subPanel.add(createButtonPanel(startDir));
		this.add(createSpectrumPanel(),BorderLayout.CENTER);
		this.add(subPanel,BorderLayout.EAST);
	}
	
	private Component createButtonPanel(String startDir) {
		JPanel motherPanel = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel(new GridLayout(0,1));
		
		final JFileChooser fileChooser = new JFileChooser(new File(startDir));
		final JButton chooseFileButton = new JButton("Open...");
		chooseFileButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(SpectralPeaksExample.this);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fileChooser.getSelectedFile();
	                System.out.println(file.toString());
	                fileName = file.getAbsolutePath();
	                startProcessing();
	            }
			}	
		});
		buttonPanel.add(new JLabel("Choose a file:"));
		buttonPanel.add(chooseFileButton);
		
		JComboBox<Integer> fftSizeComboBox = new JComboBox<Integer>(fftSizes);
		fftSizeComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				Integer value = (Integer) ((JComboBox<Integer>) e.getSource()).getSelectedItem();
				fftsize = value;
				noiseFloorMedianFilterLenth = fftsize/117;
				System.out.println("FFT Changed to " + value + " median filter length to " + noiseFloorMedianFilterLenth);
				startProcessing();
			}
		});
		fftSizeComboBox.setSelectedIndex(3);
		buttonPanel.add(new JLabel("FFT-size:"));
		buttonPanel.add(fftSizeComboBox);
		
		Integer value = new Integer(50);
		Integer min = new Integer(32);
		Integer max = new Integer(131072);
		Integer step = new Integer(32);
		SpinnerNumberModel model = new SpinnerNumberModel(value, min, max, step);
		 
		JSpinner stepSizeSpinner = new JSpinner(model);
		stepSizeSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				Integer value = (Integer) ((JSpinner) e.getSource()).getValue();
				stepsize = value;
				System.out.println("step size Changed to " + value + ", overlap is " + (fftsize - stepsize));
				startProcessing();
			}
		});
		stepSizeSpinner.setValue(512);
		buttonPanel.add(new JLabel("Step size:"));
		buttonPanel.add(stepSizeSpinner);
		
		
		JComboBox<Integer> inputSampleRateCombobox = new JComboBox<Integer>(inputSampleRate);
		inputSampleRateCombobox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				Integer value = (Integer) ((JComboBox<Integer>) e.getSource()).getSelectedItem();
				sampleRate = value;
				System.out.println("Sample rate Changed to " + value);
				startProcessing();
			}
		});
		inputSampleRateCombobox.setSelectedIndex(1);
		buttonPanel.add(new JLabel("Input sample rate"));
		buttonPanel.add(inputSampleRateCombobox);
		
		JSlider noiseFloorSlider = new JSlider(1, 2000);
		final JLabel noiseFloorFactorLabel = new JLabel("Noise floor factor    :");
		noiseFloorSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				double actualValue = newValue/100.0;
				noiseFloorFactorLabel.setText(String.format("Noise floor factor (%.2f):", actualValue));
				if(!source.getValueIsAdjusting()){
					System.out.println("New noise floor factor: " + actualValue);
					noiseFloorFactor = (float) actualValue;
					startProcessing();
				}
			}
		});
		noiseFloorSlider.setValue(150);
		buttonPanel.add(noiseFloorFactorLabel);
		buttonPanel.add(noiseFloorSlider);
		
		
		JSlider numberOfPeaksSlider = new JSlider(1, 20);
		final JLabel numberOfPeaksLabel = new JLabel("Number of peaks  :");
		numberOfPeaksSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();			
				
				numberOfPeaksLabel.setText("Number of peaks (" + newValue + "):");
				if(!source.getValueIsAdjusting()){
					System.out.println("New amount of peaks: " + newValue);
					numberOfSpectralPeaks = newValue;
					startProcessing();
				}
			}
		});
		numberOfPeaksSlider.setValue(7);
		buttonPanel.add(numberOfPeaksLabel);
		buttonPanel.add(numberOfPeaksSlider);
		
		
		
		
		final JLabel frameLabel = new JLabel("Analysis frame (0):");
		frameSlider = new JSlider(0,0);
		frameSlider.setEnabled(false);
		frameSlider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				int newValue = ((JSlider) e.getSource()).getValue();	
				frameLabel.setText("Analysis frame (" + newValue + "):");
				
				if(newValue < spectalInfo.size()){
					SpectralInfo info = spectalInfo.get(newValue);
					
					spectrumLayer.clearPeaks();
					spectrumLayer.setSpectrum(info.getSpectrum());
					noiseFloorLayer.setSpectrum(info.getNoiseFloor());
					
					List<SpectralPeak> peaks = info.getPeakList();
					
					StringBuilder sb = new StringBuilder("Frequency(Hz);Step(cents);Magnitude\n");
					for(SpectralPeak peak : peaks){
						String message = String.format("%.2f;%.2f;%.2f\n", peak.getFrequencyInHertz(),peak.getRelativeFrequencyInCents(),peak.getMagnitude());
						sb.append(message);
						//float peakFrequencyInCents =(float) PitchConverter.hertzToAbsoluteCent(peak.getFrequencyInHertz());
						spectrumLayer.setPeak(peak.getBin());
					}
					textArea.setText(sb.toString());
					
					SpectralPeaksExample.this.spectrumPanel.repaint();
				}
				
			}
		});
		buttonPanel.add(frameLabel);
		buttonPanel.add(frameSlider);
		
		final JButton saveDataButton = new JButton("Save");
		saveDataButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(frameSlider.isEnabled() && fileName != null){
					SpectralInfo info = spectalInfo.get(frameSlider.getValue());
					info.Store(new File(fileName).getName());
				}
			}	
		});
		buttonPanel.add(new JLabel("Save current spectum data:"));
		buttonPanel.add(saveDataButton);
		
		textArea = new JTextArea(10,20);
		buttonPanel.add(new JLabel("Peaks:"));
		motherPanel.add(buttonPanel, BorderLayout.NORTH);
		motherPanel.add(textArea, BorderLayout.CENTER);
		
		
		return motherPanel;
	}

	private JPanel createSpectrumPanel(){
		CoordinateSystem cs =  new CoordinateSystem(AxisUnit.FREQUENCY, AxisUnit.AMPLITUDE, 0, 1000, false);
		
		spectrumLayer = new SpectrumLayer(cs,fftsize,sampleRate,Color.red);
		noiseFloorLayer = new SpectrumLayer(cs,fftsize,sampleRate,Color.gray);
		
		spectrumPanel = new LinkedPanel(cs);
		spectrumPanel.addLayer(new ZoomMouseListenerLayer());
		spectrumPanel.addLayer(new DragMouseListenerLayer(cs));
		spectrumPanel.addLayer(new BackgroundLayer(cs));
		spectrumPanel.addLayer(new AmplitudeAxisLayer(cs));
		
		spectrumPanel.addLayer(new SelectionLayer(cs));
		spectrumPanel.addLayer(new HorizontalFrequencyAxisLayer(cs));
		spectrumPanel.addLayer(spectrumLayer);
		spectrumPanel.addLayer(noiseFloorLayer);
		
		spectrumPanel.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			boolean painting = false;
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				if(!painting){
					painting = true;
					spectrumPanel.repaint();
					painting = false;
				}
			}
		});
		return spectrumPanel;
	}
	
	private void startProcessing(){
		if(fileName !=null){
			try {
				extractPeakListList();
			} catch (UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void extractPeakListList() throws UnsupportedAudioFileException, LineUnavailableException{
		if(dispatcher != null){
			dispatcher.stop();
			dispatcher = null;
			
		}
		if(player != null){
			player.stop();
			player = null;
		}
		this.setTitle("Spectral Peaks - " + new File(fileName).getName());
		frameSlider.setEnabled(false);
		frameSlider.setMaximum(0);
		AudioFile f = new AudioFile(fileName);
		spectalInfo.clear();
		AudioInputStream stream = f.getMonoStream(sampleRate);
		int overlap = fftsize - stepsize;
		if(overlap < 1){
			overlap = 128;
		}
		
		spectrumLayer.setSampleRate(sampleRate);
		spectrumLayer.setFFTSize(fftsize);
		noiseFloorLayer.setSampleRate(sampleRate);
		noiseFloorLayer.setFFTSize(fftsize);

		
		final SpectralPeakFollower spectralPeakFollower = new SpectralPeakFollower(fftsize, overlap, sampleRate,noiseFloorMedianFilterLenth,numberOfSpectralPeaks,noiseFloorFactor);
		dispatcher = new AudioDispatcher(stream, fftsize, overlap);
		dispatcher.addAudioProcessor(spectralPeakFollower);
		
		dispatcher.addAudioProcessor(new AudioProcessor() {
			int frameCounter=0;
			@Override
			public void processingFinished() {
				if(frameCounter > frameSlider.getMaximum()){
					frameSlider.setMaximum(frameCounter);
				}
				frameSlider.setValue(frameCounter);
				frameSlider.setEnabled(true);
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				spectalInfo.add(new SpectralInfo(audioEvent.getTimeStamp(),spectralPeakFollower.getSpectrum(), spectralPeakFollower.getNoiseFloor(),spectralPeakFollower.getPeakList()));
				if(frameCounter % 1000 == 0){
					if(frameCounter > frameSlider.getMaximum()){
						frameSlider.setMaximum(frameCounter);
					}
					frameSlider.setValue(frameCounter);
				}
				frameCounter++;
				
				return true;
			}
		});
		
		AudioInputStream audioPlayStream = f.getMonoStream(sampleRate);
		player = new AudioDispatcher(audioPlayStream, 2048, 0);
		player.addAudioProcessor(new AudioPlayer(PipeDecoder.getTargetAudioFormat(sampleRate)));
		
		new Thread(player).start();
		new Thread(dispatcher).start();
	}
	
	private   static class SpectralInfo{
		private float[] spectrum;
		private float[] noiseFloor;
		private List<SpectralPeak> peaks;
		private double timeStamp;
		
		public SpectralInfo(double timeStamp,float[] spectrum, float[] noisefloor, List<SpectralPeak> list){
			this.spectrum = spectrum.clone();
			this.noiseFloor = noisefloor.clone();
			this.peaks = new ArrayList<SpectralPeak>(list);
			this.timeStamp = timeStamp;
		}
		
		public void Store(String fileName) {
			System.out.println("Store " + fileName);
			int ms = (int) Math.round(timeStamp * 1000);
			StringBuilder sb = new StringBuilder();
			for(int i = 0 ; i < spectrum.length ; i++){
				sb.append(String.format("%d;%.2f",i,spectrum[i]));
			}
			String contents = sb.toString();
			
		}

		public List<SpectralPeak> getPeakList() {
			return peaks;
		}

		public float[] getSpectrum(){
			return spectrum;
		}
		
		public float[] getNoiseFloor(){
			return noiseFloor;
		}
	}
	

	public static void main(String[] args) throws InvocationTargetException, InterruptedException, UnsupportedAudioFileException, LineUnavailableException, IOException{
		for(int i = 1 ; i < 100 ; i++){
			System.out.println(String.format("%d %.3f", i, Math.log1p(i/100.0)));
		}
		
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				SpectralPeaksExample frame = new SpectralPeaksExample("/home/joren/Dropbox/UGent/LaTeX/Articles/2014.Sethares-Theory/etc/octave/flute-test/");
				frame.pack();
				frame.setSize(450,650);
				frame.setVisible(true);
			}
		});
	}

}
