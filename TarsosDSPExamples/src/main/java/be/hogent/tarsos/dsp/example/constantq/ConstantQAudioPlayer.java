/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -----------------------------------------------------------
*
*  TarsosDSP is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://tarsos.0110.be/releases/TarsosDSP/
*  
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
* 
*/

package be.hogent.tarsos.dsp.example.constantq;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
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

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.ConstantQ;
import be.hogent.tarsos.dsp.example.constantq.Player.PlayerState;
import be.hogent.tarsos.dsp.pitch.FastYin;

public class ConstantQAudioPlayer extends JFrame {

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
	
	private JFileChooser fileChooser;
	
	private final ConstantQ constantQ;
	
	//position value in the slider
	private int newPositionValue;
	
	final Player player;
	
	final ConstantQPanel panel = new ConstantQPanel();
	
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
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Constant Q Audio Player");
		
		JPanel subPanel = new JPanel(new GridLayout(0,1));
		
		subPanel.add(createGainPanel());
		subPanel.add(createProgressPanel());
		subPanel.add(createButtonPanel());
		
		this.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, subPanel, createConstantQ()));
		constantQ = new ConstantQ(44100, 50, 3200, 60);
		player = new Player(cteQProcessor,constantQ.getFFTlength(),constantQ.getFFTlength()-4096);
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
	
	private Component createConstantQ() {
		return panel;
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
	
	AudioProcessor cteQProcessor = new AudioProcessor(){

		FastYin yin;
		@Override
		public boolean process(AudioEvent audioEvent) {
			if(yin == null){
				
				yin = new FastYin(audioEvent.getSampleRate(), 4096);
			}
			float[] pitchBuffer = Arrays.copyOfRange(audioEvent.getFloatBuffer(), 4096*3 , 4096*4);
			double pitch = yin.getPitch(pitchBuffer).getPitch();
			constantQ.process(audioEvent);
			
			panel.drawMagnitudes(constantQ);
			panel.drawPitch(constantQ,pitch);
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
			// TODO Auto-generated method stub
		}
		
	};
	
	
	public static void main(String... args) throws InterruptedException, InvocationTargetException {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new ConstantQAudioPlayer();
				frame.pack();
				frame.setSize(450,650);
				frame.setVisible(true);
			}
		});
	}
}
