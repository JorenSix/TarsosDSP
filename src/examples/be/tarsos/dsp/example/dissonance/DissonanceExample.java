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

package be.tarsos.dsp.example.dissonance;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

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

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SpectralPeakProcessor;
import be.tarsos.dsp.SpectralPeakProcessor.SpectralPeak;
import be.tarsos.dsp.example.dissonance.KernelDensityEstimate.GaussianKernel;
import be.tarsos.dsp.example.dissonance.KernelDensityEstimate.Kernel;
import be.tarsos.dsp.example.spectrum.SpectralInfo;
import be.tarsos.dsp.io.PipeDecoder;
import be.tarsos.dsp.io.PipedAudioStream;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.AxisUnit;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.ViewPort;
import be.tarsos.dsp.ui.ViewPort.ViewPortChangedListener;
import be.tarsos.dsp.ui.layers.AmplitudeAxisLayer;
import be.tarsos.dsp.ui.layers.BackgroundLayer;
import be.tarsos.dsp.ui.layers.DragMouseListenerLayer;
import be.tarsos.dsp.ui.layers.HorizontalFrequencyAxisLayer;
import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.SelectionLayer;
import be.tarsos.dsp.ui.layers.SpectrumLayer;
import be.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;
import be.tarsos.dsp.ui.layers.pch.ScaleLayer;
import be.tarsos.dsp.util.PitchConverter;

public class DissonanceExample extends JFrame {
	
	private SpectrumLayer spectrumLayer;
	private SpectrumLayer noiseFloorLayer;
	private LinkedPanel spectrumPanel;
	private LinkedPanel sensoryDissonancePanel;
	private JTextArea textArea;
	private JSlider frameSlider;
	private JSlider noiseFloorMedianLengthSlider;
	
	private AudioDispatcher dispatcher;
	private AudioDispatcher player;
	
	private int sampleRate;
	private int fftsize;
	private int stepsize;//50% overlap
	private int noiseFloorMedianFilterLenth;//35
	private float noiseFloorFactor;
	private String fileName;
	private int numberOfSpectralPeaks;
	private int currentFrame;

	private final Integer[] fftSizes = {256,512,1024,2048,4096,8192,16384,32768,65536,131072};
	private final Integer[] inputSampleRate = {22050,44100,192000};
	
	//current frequencies and amplitudes of peak list, for sensory dissonance curve
	private final List<Double> frequencies;
	private final List<Double> amplitudes;
	
	
	private final List<SpectralInfo> spectalInfo;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5600205438242149179L;
	
	public DissonanceExample(String startDir){		
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Spectral Peaks");
		
		spectalInfo = new ArrayList<SpectralInfo>();
		
		JPanel subPanel = new JPanel();
		subPanel.add(createButtonPanel(startDir));
		
		frequencies = new ArrayList<Double>();
		amplitudes = new ArrayList<Double>();
		
		JPanel otherSubPanel = new JPanel(new GridLayout(2,1));
		otherSubPanel.add(createSpectrumPanel());
		otherSubPanel.add(createSensoryDisonancePanel());
		this.add(otherSubPanel,BorderLayout.CENTER);
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
				int returnVal = fileChooser.showOpenDialog(DissonanceExample.this);
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
				System.out.println("Step size Changed to " + value + ", overlap is " + (fftsize - stepsize));
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
		
		JSlider noiseFloorSlider = new JSlider(100, 250);
		final JLabel noiseFloorFactorLabel = new JLabel("Noise floor factor    :");
		noiseFloorSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				double actualValue = newValue/100.0;
				noiseFloorFactorLabel.setText(String.format("Noise floor factor (%.2f):", actualValue));
				System.out.println("New noise floor factor: " + actualValue);
				noiseFloorFactor = (float) actualValue;
				repaintSpectralInfo();
				
			}
		});
		noiseFloorSlider.setValue(150);
		buttonPanel.add(noiseFloorFactorLabel);
		buttonPanel.add(noiseFloorSlider);
		
		final JLabel noiseFloorMedianLengthLabel = new JLabel("Noise floor median filter length ("+ noiseFloorMedianFilterLenth +"):");
		noiseFloorMedianLengthSlider = new JSlider(3,fftsize/2);
		noiseFloorMedianLengthSlider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				int newValue = ((JSlider) e.getSource()).getValue();	
				noiseFloorMedianLengthLabel.setText("Noise floor median filter length (" + newValue + "):");
				noiseFloorMedianFilterLenth = newValue;
				repaintSpectralInfo();
			}
		});
		buttonPanel.add(noiseFloorMedianLengthLabel);
		buttonPanel.add(noiseFloorMedianLengthSlider);
		noiseFloorMedianLengthSlider.setValue(noiseFloorMedianFilterLenth);
		
		
		JSlider numberOfPeaksSlider = new JSlider(1, 20);
		final JLabel numberOfPeaksLabel = new JLabel("Number of peaks  :");
		numberOfPeaksSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();			
				numberOfPeaksLabel.setText("Number of peaks (" + newValue + "):");
				System.out.println("New amount of peaks: " + newValue);
				numberOfSpectralPeaks = newValue;
				repaintSpectralInfo();
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
				currentFrame = newValue;
				repaintSpectralInfo();
			}
		});
		buttonPanel.add(frameLabel);
		buttonPanel.add(frameSlider);
		
		
		textArea = new JTextArea(10,20);
		buttonPanel.add(new JLabel("Peaks:"));
		motherPanel.add(buttonPanel, BorderLayout.NORTH);
		motherPanel.add(textArea, BorderLayout.CENTER);
		
		
		return motherPanel;
	}
	
	
	private void repaintSpectralInfo() {
		if(currentFrame < spectalInfo.size()){
			SpectralInfo info = spectalInfo.get(currentFrame);
			
			spectrumLayer.clearPeaks();
			spectrumLayer.setSpectrum(info.getMagnitudes());
			noiseFloorLayer.setSpectrum(info.getNoiseFloor(noiseFloorMedianFilterLenth,noiseFloorFactor));
			List<SpectralPeak> peaks = info.getPeakList(noiseFloorMedianFilterLenth, noiseFloorFactor, numberOfSpectralPeaks,50);
			
			StringBuilder sb = new StringBuilder("Frequency(Hz);Step(cents);Magnitude;Ratio\n");
			frequencies.clear();
			amplitudes.clear();
			for(SpectralPeak peak : peaks){
				String message = String.format("%.2f;%.2f;%.2f;%.2f\n", peak.getFrequencyInHertz(),peak.getRelativeFrequencyInCents(),peak.getMagnitude(),peak.getFrequencyInHertz()/peak.getRefFrequencyInHertz());
				sb.append(message);
				//float peakFrequencyInCents =(float) PitchConverter.hertzToAbsoluteCent(peak.getFrequencyInHertz());
				spectrumLayer.setPeak(peak.getBin());
				frequencies.add((double) peak.getFrequencyInHertz());
				amplitudes.add((double) peak.getMagnitude());
			}
			textArea.setText(sb.toString());
			
			DissonanceExample.this.spectrumPanel.repaint();
			DissonanceExample.this.sensoryDissonancePanel.repaint();
		}			
	}
	
	private JPanel createSpectrumPanel(){
		CoordinateSystem cs =  new CoordinateSystem(AxisUnit.FREQUENCY, AxisUnit.AMPLITUDE, -1000, 10, false);
		cs.setMax(Axis.X, 4800);
		cs.setMax(Axis.X, 13200);
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
					sensoryDissonancePanel.repaint();
					painting = false;
				}
			}
		});
		return spectrumPanel;
	}
	
	private JPanel createSensoryDisonancePanel(){
		CoordinateSystem cs =  new CoordinateSystem(AxisUnit.FREQUENCY, AxisUnit.AMPLITUDE, 0, 1100, false);
		cs.setMin(Axis.X, 0);
		cs.setMax(Axis.X, 1800);
		final ScaleLayer valleyLayer = new ScaleLayer(cs, false);
	
		Layer sensoryDissonanceLayer = new Layer() {
			SensoryDissonanceCurve sdc = new SensoryDissonanceCurve();
			@Override
			public String getName() {
				return "Sensory dissonance layer";
			}
			
			@Override
			public void draw(Graphics2D graphics) {
				if(!frequencies.isEmpty()){
					List<SensoryDissonanceResult> results = sdc.calculate(frequencies, amplitudes);
					int prevFreqInCents = 0;
					int prevMagnitude = 0;
					double maxDissonance = 0;
					for(SensoryDissonanceResult result : results){
						maxDissonance = Math.max(result.dissonanceValue, maxDissonance);
					}
					graphics.setColor(Color.RED);
					for(SensoryDissonanceResult result : results){
						int currentFreqInCents = Math.round((float)result.getdifferenceInCents());
						int currentMagnitude = Math.round((float) (result.dissonanceValue / maxDissonance * 1000));
						graphics.drawLine(prevFreqInCents, prevMagnitude,currentFreqInCents,currentMagnitude );
						prevFreqInCents = currentFreqInCents;
						prevMagnitude = currentMagnitude;
						
					}
					List<SensoryDissonanceResult> valleys = sdc.valleys(results);
					double[] newScale = new double[valleys.size()];
					for(int i = 0;i<valleys.size();i++){
						newScale[i]  = valleys.get(i).getdifferenceInCents();
					}
					valleyLayer.setScale(newScale);
				}
			}
		};
		
		
		sensoryDissonancePanel = new LinkedPanel(cs);
		sensoryDissonancePanel.addLayer(new BackgroundLayer(cs));
		sensoryDissonancePanel.addLayer(sensoryDissonanceLayer);
		sensoryDissonancePanel.addLayer(valleyLayer);
		sensoryDissonancePanel.addLayer(new ScaleLayer(cs, true));
		
		//sensoryDissonancePanel.addLayer(new ZoomMouseListenerLayer());
		//sensoryDissonancePanel.addLayer(new DragMouseListenerLayer(cs));
		
		sensoryDissonancePanel.addLayer(new AmplitudeAxisLayer(cs));
		sensoryDissonancePanel.addLayer(new SelectionLayer(cs));
		sensoryDissonancePanel.addLayer(new HorizontalFrequencyAxisLayer(cs));
		
		sensoryDissonancePanel.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			boolean painting = false;
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				if(!painting){
					painting = true;
					sensoryDissonancePanel.repaint();
					spectrumPanel.repaint();
					painting = false;
				}
			}
		});
		
		return sensoryDissonancePanel;
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
		PipedAudioStream f = new PipedAudioStream(fileName);
		spectalInfo.clear();
		TarsosDSPAudioInputStream stream = f.getMonoStream(sampleRate,0);
		int overlap = fftsize - stepsize;
		if(overlap < 1){
			overlap = 128;
		}
		
		spectrumLayer.setSampleRate(sampleRate);
		spectrumLayer.setFFTSize(fftsize);
		noiseFloorLayer.setSampleRate(sampleRate);
		noiseFloorLayer.setFFTSize(fftsize);

		
		final SpectralPeakProcessor spectralPeakFollower = new SpectralPeakProcessor(fftsize, overlap, sampleRate);
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
				
				//write spectral info to file
				 try {
					writeSpectralInfoToFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				spectalInfo.add(new SpectralInfo(spectralPeakFollower.getMagnitudes(),spectralPeakFollower.getFrequencyEstimates()));
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
		
		TarsosDSPAudioInputStream audioPlayStream = f.getMonoStream(sampleRate,0);
		player = new AudioDispatcher(audioPlayStream, 2048, 0);
		player.addAudioProcessor(new AudioPlayer(JVMAudioInputStream.toAudioFormat(PipeDecoder.getTargetAudioFormat(sampleRate))));
		
		new Thread(player).start();
		new Thread(dispatcher).start();
		
		
	}
	
	private void writeSpectralInfoToFile() throws IOException{
		StringBuilder sb = new StringBuilder();
		
		File file = new File("spectrum.txt");
		 
		// if file doesnt exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}

		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		
		Kernel kernel = new GaussianKernel(80);
		
		KernelDensityEstimate kde = new KernelDensityEstimate(kernel,14400);
		
		HashMap<Integer, Integer> peakCounter = new HashMap<Integer, Integer>();
		HashMap<Integer, List<Integer>> peakFrames = new HashMap<Integer, List<Integer>>();
		
		for(int i = 0 ; i < spectalInfo.size() ;i++){
			
			SpectralInfo spi = spectalInfo.get(i);
			//float[] mags = spi.getMagnitudes().clone();
			//float[] floor = spi.getNoiseFloor(80,1.0f);
			List<SpectralPeak> peaks = spi.getPeakList(80, 1.06f, 20,50);
			
			double maxPeak = 0;
			double pitchInCents=0;
			for(SpectralPeak peak:peaks){
				if(peak.getMagnitude() > maxPeak){
					 pitchInCents = PitchConverter.hertzToAbsoluteCent(peak.getFrequencyInHertz());
					 maxPeak = peak.getMagnitude();
					 int key = (int) Math.round(pitchInCents/50.0f);
					 if(!peakCounter.containsKey(key)){
						 peakCounter.put(key, 0);
						 peakFrames.put(key, new ArrayList<Integer>());
					 }
					 peakCounter.put(key, peakCounter.get(key) + 1);
					 peakFrames.get(key).add(i);
				}
			}
		}
		
		int maxValue = 0; 
		int maxKey = 0;
		for(Entry<Integer,Integer> entry : peakCounter.entrySet()){
			System.out.println(entry.getKey() * 50 + ";" + entry.getValue());
			if(entry.getValue() > maxValue){
				maxValue = entry.getValue();
				maxKey = entry.getKey();
			}
		}
		List<Integer> framesToIterate = peakFrames.get(maxKey); 
		
		for(int i : framesToIterate){
			SpectralInfo spi = spectalInfo.get(i);
			float[] mags = spi.getMagnitudes().clone();
			float[] floor = spi.getNoiseFloor(80,1.0f);
			
			List<SpectralPeak> peaks = spi.getPeakList(80, 1.06f, 20,50);
			for(SpectralPeak peak:peaks){
				double pitchInCents = PitchConverter.hertzToAbsoluteCent(peak.getFrequencyInHertz());
				for(int k = 0 ; k < Math.max(0,mags[peak.getBin()]-floor[peak.getBin()])*10; k++){
					kde.add(pitchInCents);
				}
			}
			
			sb.append("frame_");
			sb.append(i);
			sb.append(" ");
			for(int j = 0;j<mags.length ; j++){
				sb.append(Math.max(0,mags[j]-floor[j]));
				sb.append(" ");
			}
			sb.append("\n");
			
			if(sb.length() > 100000){
				String content = sb.toString();
				bw.write(content);
				sb = new StringBuilder();
			}
		}
		
		String content = sb.toString();
		bw.write(content);
		bw.close();
		
		
		file = new File("estimate.txt");
		 
		// if file doesnt exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}

		fw = new FileWriter(file.getAbsoluteFile());
		bw = new BufferedWriter(fw);
		
		double[] estimate = kde.getEstimate();
		sb = new StringBuilder();
		sb.append("\n");
		for(int i = 6000 ; i < 12000 ; i++){
			sb.append(i);
			sb.append(" ");
			sb.append(estimate[i]);
			sb.append("\n");
		}
		content = sb.toString();
		bw.write(content);
		bw.close();		
	}
	
	

	public static void main(String[] args) throws InvocationTargetException, InterruptedException, UnsupportedAudioFileException, LineUnavailableException, IOException{	
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				DissonanceExample frame = new DissonanceExample("/home/joren/Dropbox/UGent/LaTeX/Articles/2014.Sethares-Theory/etc/octave/flute-test/");
				frame.pack();
				frame.setSize(450,650);
				frame.setVisible(true);
			}
		});
	}

}
