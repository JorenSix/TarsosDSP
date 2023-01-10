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


package be.tarsos.dsp.example.constantq;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
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
import be.tarsos.dsp.example.constantq.Player.PlayerState;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.AxisUnit;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.ViewPort;
import be.tarsos.dsp.ui.ViewPort.ViewPortChangedListener;
import be.tarsos.dsp.ui.layers.AmplitudeAxisLayer;
import be.tarsos.dsp.ui.layers.BackgroundLayer;
import be.tarsos.dsp.ui.layers.ConstantQLayer;
import be.tarsos.dsp.ui.layers.DragMouseListenerLayer;
import be.tarsos.dsp.ui.layers.LegendLayer;
import be.tarsos.dsp.ui.layers.PitchContourLayer;
import be.tarsos.dsp.ui.layers.SelectionLayer;
import be.tarsos.dsp.ui.layers.TimeAxisLayer;
import be.tarsos.dsp.ui.layers.VerticalFrequencyAxisLayer;
import be.tarsos.dsp.ui.layers.WaveFormLayer;
import be.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;

public class ConstantQAudioPlayer extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4000269621209901229L;
	
	private JSlider gainSlider;
	private JSlider positionSlider;
	
	private JButton playButton;
	private JButton stopButton;
	private JButton pauzeButton;
	private JLabel progressLabel;
	private JLabel totalLabel;
	
	private LinkedPanel waveForm;
	private LinkedPanel constantQ;
	private CoordinateSystem waveFormCS;
	private CoordinateSystem constantQCS;
	
	private JFileChooser fileChooser;
	
	//position value in the slider
	private int newPositionValue;
	
	final Player player;
	
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
	

	
	public ConstantQAudioPlayer(){
		this.setLayout(new BorderLayout());
		
		JPanel subPanel = new JPanel(new GridLayout(0,1));
		
		subPanel.add(createGainPanel());
		subPanel.add(createProgressPanel());
		subPanel.add(createButtonPanel());
		
		this.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, subPanel, createConstantQ()));
		
		player = new Player(processor,1024,0);
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
	
	private Component createConstantQ() {
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
		
		constantQCS = getCoordinateSystem(AxisUnit.FREQUENCY);
		constantQ = new LinkedPanel(constantQCS);
		constantQ.addLayer(new BackgroundLayer(constantQCS));
		constantQ.addLayer(new VerticalFrequencyAxisLayer(constantQCS));
		constantQ.addLayer(new TimeAxisLayer(constantQCS));
		constantQ.addLayer(new ZoomMouseListenerLayer());
		constantQ.addLayer(new DragMouseListenerLayer(constantQCS));
		constantQ.addLayer(new SelectionLayer(constantQCS));
		
		legend = new LegendLayer(constantQCS,110);
		constantQ.addLayer(legend);
		
		legend.addEntry("ConstantQ",Color.BLACK);
		legend.addEntry("Pitch estimations",Color.RED);
		
		splitPane.add(constantQ, JSplitPane.BOTTOM);
		splitPane.setDividerLocation(150);
		
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				waveForm.repaint();
				constantQ.repaint();
			}
		};
		waveForm.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				constantQCS.setMin(Axis.X,waveFormCS.getMin(Axis.X));
				constantQCS.setMax(Axis.X,waveFormCS.getMax(Axis.X));
			}
		});		
		waveForm.getViewPort().addViewPortChangedListener(listener);
		
		constantQ.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				waveFormCS.setMin(Axis.X,constantQCS.getMin(Axis.X));
				waveFormCS.setMax(Axis.X,constantQCS.getMax(Axis.X));
			}
		});
		constantQ.getViewPort().addViewPortChangedListener(listener);
		
		return splitPane;
	}

	private void reactToPlayerState(PlayerState newState){
		positionSlider.setEnabled(newState != PlayerState.NO_FILE_LOADED);
		playButton.setEnabled(newState != PlayerState.PLAYING && newState != PlayerState.NO_FILE_LOADED);
		pauzeButton.setEnabled(newState == PlayerState.PLAYING && newState != PlayerState.NO_FILE_LOADED );
		stopButton.setEnabled((newState == PlayerState.PLAYING || newState == PlayerState.PAUZED) && newState != PlayerState.NO_FILE_LOADED);
		
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
			
			
			LegendLayer legend = new LegendLayer(waveFormCS,50);
			waveForm.addLayer(legend);
			legend.addEntry("Wave",Color.BLACK);
			
			constantQ.removeLayers();
			constantQ.addLayer(new BackgroundLayer(constantQCS));
			constantQ.addLayer(new ConstantQLayer(constantQCS,player.getLoadedFile(),2048,3600,10800,12));
			constantQ.addLayer(new PitchContourLayer(constantQCS,player.getLoadedFile(),Color.red,2048,0));
			constantQ.addLayer(new VerticalFrequencyAxisLayer(constantQCS));
			constantQ.addLayer(new ZoomMouseListenerLayer());
			constantQ.addLayer(new DragMouseListenerLayer(constantQCS));
			constantQ.addLayer(new SelectionLayer(constantQCS));
			constantQ.addLayer(new TimeAxisLayer(constantQCS));
			
			legend = new LegendLayer(constantQCS,110);
			constantQ.addLayer(legend);
			legend.addEntry("ConstantQ",Color.BLACK);
			legend.addEntry("Pitch estimations",Color.RED);
		}
	}
	
	public String formattedToString(double seconds) {
		int minutes = (int) (seconds / 60);
		int completeSeconds = (int) seconds - (minutes * 60);
		int hundred =  (int) ((seconds - (int) seconds) * 100);
		return String.format(Locale.US, "%02d:%02d:%02d", minutes , completeSeconds, hundred);
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
	
	
	private JComponent createButtonPanel(){
		JPanel fileChooserPanel = new JPanel(new GridLayout(1,0));
		fileChooserPanel.setBorder(new TitledBorder("Actions"));
		
		fileChooser = new JFileChooser();
		
		final JButton chooseFileButton = new JButton("Open...");
		chooseFileButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(ConstantQAudioPlayer.this);
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
		
		stopButton = new JButton("Stop");
		fileChooserPanel.add(stopButton);
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				player.stop();
			}
		});
		
		playButton = new JButton("Play");
		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				player.play();
			}
		});
		fileChooserPanel.add(playButton);		
		
		pauzeButton = new JButton("Pauze");
		pauzeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				player.pauze();
			}
		});
		fileChooserPanel.add(pauzeButton);
		
		return fileChooserPanel;
	}
	
	private void setProgressLabelText(double current, double max){
		progressLabel.setText(formattedToString(current));
		totalLabel.setText(formattedToString(max));
	}
	
	private JComponent createGainPanel(){
		gainSlider = new JSlider(0,200);
		gainSlider.setValue(100);
		gainSlider.setPaintLabels(true);
		gainSlider.setPaintTicks(true);
		final JLabel label = new JLabel("Gain: 100%");
		gainSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
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
				frame.setTitle("Constant Q Audio Player");
				frame.add(new ConstantQAudioPlayer());
				frame.pack();
				frame.setSize(450,650);
				frame.setVisible(true);
			}
		});
	}
}
