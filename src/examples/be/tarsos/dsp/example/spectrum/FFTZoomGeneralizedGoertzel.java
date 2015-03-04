package be.tarsos.dsp.example.spectrum;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.example.constantq.Player;
import be.tarsos.dsp.example.constantq.Player.PlayerState;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.AxisUnit;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.ViewPort;
import be.tarsos.dsp.ui.ViewPort.ViewPortChangedListener;
import be.tarsos.dsp.ui.layers.AmplitudeAxisLayer;
import be.tarsos.dsp.ui.layers.BackgroundLayer;
import be.tarsos.dsp.ui.layers.DragMouseListenerLayer;
import be.tarsos.dsp.ui.layers.LegendLayer;
import be.tarsos.dsp.ui.layers.MouseCursorLayer;
import be.tarsos.dsp.ui.layers.PitchContourLayer;
import be.tarsos.dsp.ui.layers.Scalogram;
import be.tarsos.dsp.ui.layers.SelectionLayer;
import be.tarsos.dsp.ui.layers.TimeAxisLayer;
import be.tarsos.dsp.ui.layers.TooltipLayer;
import be.tarsos.dsp.ui.layers.VerticalFrequencyAxisLayer;
import be.tarsos.dsp.ui.layers.WaveFormLayer;
import be.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;

public class FFTZoomGeneralizedGoertzel extends JPanel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5689356875546643126L;
	
	JLabel progressLabel;
	JLabel totalLabel;
	
	//position value in the slider
	private int newPositionValue;
	
	JSlider positionSlider;
	
	final Player player;
	
	private LinkedPanel waveForm;
	private LinkedPanel timeFrequencyPane;
	private CoordinateSystem waveFormCS;
	private CoordinateSystem timeFrequencyPaneCS;
	

	final AudioProcessor processor = new AudioProcessor() {
		
		@Override
		public boolean process(AudioEvent audioEvent) {
			double timeStamp =  audioEvent.getTimeStamp();
			if(!positionSlider.getValueIsAdjusting()){
				newPositionValue = (int) (audioEvent.getProgress() * 1000);
				positionSlider.setValue(newPositionValue);
				setProgressLabelText(timeStamp,player.getDurationInSeconds());
			}
			return true;
		}
		
		@Override
		public void processingFinished() {
			
		}
	};

	
	public FFTZoomGeneralizedGoertzel(){
		this.setLayout(new BorderLayout());
		
	
		player = new Player(processor,1024,0);
		
		JPanel subPanel = new JPanel(new GridLayout(0,1));
		subPanel.add(createButtonPanel());
		subPanel.add(createProgressPanel());
		subPanel.add(createGainPanel());
		
		JComponent featurePanel =  createFeaturePanel();
		
		JComponent splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, subPanel,featurePanel);
		
		
		player.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				if(arg0.getPropertyName()=="state"){
					PlayerState newState = (PlayerState) arg0.getNewValue();
					reactToPlayerState(newState);
				}
			}
		});
		reactToPlayerState(player.getState());
		
		this.add(splitPane);
	}
	
	private void reactToPlayerState(PlayerState newState){
		positionSlider.setEnabled(newState != PlayerState.NO_FILE_LOADED);
		if(newState == PlayerState.STOPPED || newState == PlayerState.FILE_LOADED){
			newPositionValue = 0;
			positionSlider.setValue(0);
			setProgressLabelText(0, player.getDurationInSeconds());
		}
		if(newState == PlayerState.FILE_LOADED){
	
			
			waveForm.removeLayers();
			waveForm.addLayer(new BackgroundLayer(waveFormCS));
			waveForm.addLayer(new AmplitudeAxisLayer(waveFormCS));
			waveForm.addLayer(new TimeAxisLayer(waveFormCS));
			waveForm.addLayer(new WaveFormLayer(waveFormCS, player.getLoadedFile()));
			waveForm.addLayer(new ZoomMouseListenerLayer());
			waveForm.addLayer(new DragMouseListenerLayer(waveFormCS));
			
			
			final MouseCursorLayer waveFormCursor = new MouseCursorLayer(waveFormCS);
			waveForm.addLayer(waveFormCursor);
			
			LegendLayer legend = new LegendLayer(waveFormCS,50);
			waveForm.addLayer(legend);
			legend.addEntry("Wave",Color.BLACK);
			
			timeFrequencyPane.removeLayers();
			timeFrequencyPane.addLayer(new BackgroundLayer(timeFrequencyPaneCS));
		//	timeFrequencyPane.addLayer(new GeneralizedGoertzelLayer(timeFrequencyPaneCS,player.getLoadedFile(),20));
			
			Scalogram fftLayer = new Scalogram(timeFrequencyPaneCS,player.getLoadedFile().getAbsolutePath());
			timeFrequencyPane.addLayer(fftLayer);
			MouseCursorLayer cl = new MouseCursorLayer(timeFrequencyPaneCS);
			timeFrequencyPane.addLayer(cl);
			
			
			timeFrequencyPane.addLayer(new PitchContourLayer(timeFrequencyPaneCS,player.getLoadedFile(),Color.red,2048,0));
			timeFrequencyPane.addLayer(new VerticalFrequencyAxisLayer(timeFrequencyPaneCS));
			timeFrequencyPane.addLayer(new ZoomMouseListenerLayer());
			timeFrequencyPane.addLayer(new DragMouseListenerLayer(timeFrequencyPaneCS));
			timeFrequencyPane.addLayer(new SelectionLayer(timeFrequencyPaneCS));
			timeFrequencyPane.addLayer(new TimeAxisLayer(timeFrequencyPaneCS));
			timeFrequencyPane.addLayer(new TooltipLayer(timeFrequencyPaneCS,fftLayer));
			
			legend = new LegendLayer(timeFrequencyPaneCS,110);
			timeFrequencyPane.addLayer(legend);
			legend.addEntry("ConstantQ",Color.BLACK);
			legend.addEntry("Pitch estimations",Color.RED);
			
			cl.addPropertyChangeListener(new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					if(evt.getPropertyName()=="cursor"){
						Point newPoint = (Point) evt.getNewValue();
						waveFormCursor.setPoint(newPoint);
					}
					
				}
			});
		}
	}
	
	private CoordinateSystem getCoordinateSystem(AxisUnit yUnits) {
		float minValue = -1000;
		float maxValue = 1000;
		if(yUnits == AxisUnit.FREQUENCY){
			minValue = 200;
			maxValue = 8000;
		}
		return new CoordinateSystem(yUnits, minValue, maxValue);
	}
	
	
	private JComponent createFeaturePanel() {
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		
		waveFormCS = getCoordinateSystem(AxisUnit.AMPLITUDE);
		waveForm = new LinkedPanel(waveFormCS);
		waveForm.addLayer(new BackgroundLayer(waveFormCS));
		waveForm.addLayer(new AmplitudeAxisLayer(waveFormCS));
		waveForm.addLayer(new TimeAxisLayer(waveFormCS));
		waveForm.addLayer(new ZoomMouseListenerLayer());
		waveForm.addLayer(new DragMouseListenerLayer(waveFormCS));
		LegendLayer legend = new LegendLayer(waveFormCS,50);
		waveForm.addLayer(legend);
		legend.addEntry("Wave",Color.BLACK);
		
		splitPane.add(waveForm, JSplitPane.TOP);
		
		timeFrequencyPaneCS = getCoordinateSystem(AxisUnit.FREQUENCY);
		timeFrequencyPane = new LinkedPanel(timeFrequencyPaneCS);
		timeFrequencyPane.addLayer(new BackgroundLayer(timeFrequencyPaneCS));
		timeFrequencyPane.addLayer(new VerticalFrequencyAxisLayer(timeFrequencyPaneCS));
		timeFrequencyPane.addLayer(new TimeAxisLayer(timeFrequencyPaneCS));
		timeFrequencyPane.addLayer(new ZoomMouseListenerLayer());
		timeFrequencyPane.addLayer(new DragMouseListenerLayer(timeFrequencyPaneCS));
		timeFrequencyPane.addLayer(new SelectionLayer(timeFrequencyPaneCS));
		
		legend = new LegendLayer(timeFrequencyPaneCS,110);
		timeFrequencyPane.addLayer(legend);
		
		legend.addEntry("Spectrogram",Color.BLACK);
		legend.addEntry("Pitch estimations",Color.RED);
		
		splitPane.add(timeFrequencyPane, JSplitPane.BOTTOM);
		splitPane.setDividerLocation(150);
		
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				waveForm.repaint();
				timeFrequencyPane.repaint();
			}
		};
		waveForm.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				timeFrequencyPaneCS.setMin(Axis.X,waveFormCS.getMin(Axis.X));
				timeFrequencyPaneCS.setMax(Axis.X,waveFormCS.getMax(Axis.X));
			}
		});
		
		
		
		waveForm.getViewPort().addViewPortChangedListener(listener);
		
		timeFrequencyPane.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				waveFormCS.setMin(Axis.X,timeFrequencyPaneCS.getMin(Axis.X));
				waveFormCS.setMax(Axis.X,timeFrequencyPaneCS.getMax(Axis.X));
			}
		});
		timeFrequencyPane.getViewPort().addViewPortChangedListener(listener);
		return splitPane;
	}

	private JComponent createProgressPanel(){
	    positionSlider = new JSlider(0,1000);
		positionSlider.setValue(0);
		positionSlider.setPaintLabels(false);
		positionSlider.setPaintTicks(false);
		positionSlider.setEnabled(false);
		positionSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (newPositionValue != positionSlider.getValue()) {
					double promille = positionSlider.getValue() / 1000.0;
					double currentPosition = player.getDurationInSeconds() * promille;
					if (positionSlider.getValueIsAdjusting()) {
						setProgressLabelText(currentPosition, player.getDurationInSeconds());
					} else {
						double secondsToSkip = currentPosition;
						PlayerState currentState = player.getState();
						
						player.pauze(secondsToSkip);
						if(currentState == PlayerState.PLAYING){
							player.play();							
						} 
					}
				}
			}
		});
		
		progressLabel = new JLabel();
		totalLabel = new JLabel();
		setProgressLabelText(0, 0);
		
		JPanel subPanel = new JPanel(new BorderLayout());
		subPanel.add(progressLabel,BorderLayout.WEST);
		subPanel.add(positionSlider,BorderLayout.CENTER);
		subPanel.add(totalLabel,BorderLayout.EAST);
		
		JPanel panel = new JPanel(new BorderLayout());
		JLabel label = new JLabel("Progress (in %°)");
		label.setToolTipText("Progress in promille.");
		panel.add(label,BorderLayout.NORTH);
		panel.add(subPanel,BorderLayout.CENTER);
		panel.setBorder(new TitledBorder("Progress control"));

		return panel;
	}
	
	private void setProgressLabelText(double current, double max){
		progressLabel.setText(formattedToString(current));
		totalLabel.setText(formattedToString(max));
	}
	
	public String formattedToString(double seconds) {
		int minutes = (int) (seconds / 60);
		int completeSeconds = (int) seconds - (minutes * 60);
		int hundred =  (int) ((seconds - (int) seconds) * 100);
		return String.format(Locale.US, "%02d:%02d:%02d", minutes , completeSeconds, hundred);
	}
	
	private JComponent createButtonPanel(){
		JPanel fileChooserPanel = new JPanel(new GridLayout(1,0));
		fileChooserPanel.setBorder(new TitledBorder("Actions"));
		
		final JFileChooser fileChooser = new JFileChooser();
		
		final JButton chooseFileButton = new JButton("Open...");
		chooseFileButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(FFTZoomGeneralizedGoertzel.this);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fileChooser.getSelectedFile();	                
	                PlayerState currentState = player.getState();
	                player.load(file);
	                if(currentState == PlayerState.NO_FILE_LOADED || currentState == PlayerState.PLAYING){
	                	player.play();
	                }
	            } else {
	                //canceled
	            }
			}			
		});
		fileChooserPanel.add(chooseFileButton);
		
		final JButton stopButton = new JButton("Stop");
		stopButton.setEnabled(false);
		fileChooserPanel.add(stopButton);
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				player.stop();
			}
		});
		
		
		final JButton playButton = new JButton("Play");
		playButton.setEnabled(false);
		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				player.play();
			}
		});
		fileChooserPanel.add(playButton);		
		
		final JButton pauzeButton = new JButton("Pauze");
		pauzeButton.setEnabled(false);
		pauzeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				player.pauze();
			}
		});
		fileChooserPanel.add(pauzeButton);
		
		player.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if(evt.getPropertyName()=="state"){
					PlayerState newState = (PlayerState) evt.getNewValue();
					playButton.setEnabled(newState != PlayerState.PLAYING && newState != PlayerState.NO_FILE_LOADED);
					pauzeButton.setEnabled(newState == PlayerState.PLAYING && newState != PlayerState.NO_FILE_LOADED );
					stopButton.setEnabled((newState == PlayerState.PLAYING || newState == PlayerState.PAUZED) && newState != PlayerState.NO_FILE_LOADED);
				}
			}
		});
		
		return fileChooserPanel;
	}
	
	
	private JComponent createGainPanel(){
		JSlider gainSlider;
		gainSlider = new JSlider(0,200);
		gainSlider.setValue(100);
		gainSlider.setPaintLabels(true);
		gainSlider.setPaintTicks(true);
		final JLabel label = new JLabel("Gain: 100%");
		gainSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				JSlider gainSlider = ((JSlider)arg0.getSource());
				double gainValue = gainSlider.getValue() / 100.0;
				label.setText(String.format("Gain: %3d", gainSlider.getValue())+"%");
				player.setGain(gainValue);
			}
		});
		
		JPanel gainPanel = new JPanel(new BorderLayout());
		label.setToolTipText("Volume in % (100 is no change).");
		gainPanel.add(label,BorderLayout.NORTH);
		gainPanel.add(gainSlider,BorderLayout.CENTER);
		gainPanel.setBorder(new TitledBorder("Volume control"));
		return gainPanel;
	}
	
	public static void main(String... args) throws InterruptedException, InvocationTargetException {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new JFrame();
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setTitle("FFT Zoom with Generalized Goertzel");
				frame.add(new FFTZoomGeneralizedGoertzel());
				frame.pack();
				frame.setSize(450,650);
				frame.setVisible(true);
			}
		});
	}
}