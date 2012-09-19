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
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*  https://github.com/JorenSix/TarsosDSP
*  http://tarsos.0110.be/releases/TarsosDSP/
* 
*/

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
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
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

public class TimeStretch extends JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6072837777935275806L;
	
	private JFileChooser fileChooser;
	
	private AudioDispatcher dispatcher;
	private WaveformSimilarityBasedOverlapAdd wsola; 
	private GainProcessor gain;
	private AudioPlayer audioPlayer;
	
	private final JSlider tempoSlider;
	private final JSlider gainSlider;
	SpinnerModel overlapModel;
	SpinnerModel windowModel;
	SpinnerModel sequenceModel;
	
	
	public TimeStretch(){
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Time Stretching: WSOLA TIME-SCALE MODIFICATION OF SOUND");
		tempoSlider = new JSlider(20, 250);
		tempoSlider.setValue(100);
		tempoSlider.setPaintLabels(true);
		tempoSlider.addChangeListener(parameterSettingChangedListener);
		
		gainSlider = new JSlider(0,200);
		gainSlider.setValue(100);
		gainSlider.setPaintLabels(true);
		gainSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (TimeStretch.this.dispatcher != null) {
					double gainValue = gainSlider.getValue() / 100.0;
					gain.setGain(gainValue);
				}
			}
		});
		
	
		
		JPanel fileChooserPanel = new JPanel(new BorderLayout());
		fileChooserPanel.setBorder(new TitledBorder("1. Choose your audio (wav mono)"));
		
		fileChooser = new JFileChooser();
		
		JButton chooseFileButton = new JButton("Choose a file...");
		chooseFileButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(TimeStretch.this);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fileChooser.getSelectedFile();
	                startFile(file);
	            } else {
	                //canceled
	            }
			}			
		});
		fileChooserPanel.add(chooseFileButton,BorderLayout.CENTER);
		
		
		JPanel params = new JPanel(new BorderLayout());
		params.setBorder(new TitledBorder("2. Set the algorithm parameters"));
		
		JLabel label = new JLabel("Tempo");
		label.setToolTipText("The time stretching factor in % (100 is no change).");
		params.add(label,BorderLayout.NORTH);
		params.add(tempoSlider,BorderLayout.CENTER);
		
		JPanel subPanel = new JPanel(new GridLayout(3, 2));
		overlapModel =  new SpinnerNumberModel(12,0,5000,1);
		JSpinner overlapSpinnner = new JSpinner(overlapModel);
		overlapSpinnner.addChangeListener(parameterSettingChangedListener);
		windowModel =  new SpinnerNumberModel(28,0,5000,1);
		JSpinner windowSpinnner = new JSpinner(windowModel);
		windowSpinnner.addChangeListener(parameterSettingChangedListener);
		sequenceModel =   new SpinnerNumberModel(82,0,5000,1);
		JSpinner sequenceSpinnner = new JSpinner(sequenceModel);
		sequenceSpinnner.addChangeListener(parameterSettingChangedListener);
		

		label = new JLabel("Sequence length");
		label.setToolTipText("Sequence length in ms.");
		subPanel.add(label);
		subPanel.add(sequenceSpinnner);
		
		label = new JLabel("Seek window length");
		label.setToolTipText("Seek window length in ms.");
		subPanel.add(label);
		subPanel.add(windowSpinnner);
		
		label = new JLabel("Overlap length");
		label.setToolTipText("Overlap length in ms.");
		subPanel.add(label);
		subPanel.add(overlapSpinnner);
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
	
	private ChangeListener parameterSettingChangedListener = new ChangeListener(){

		@Override
		public void stateChanged(ChangeEvent arg0) {
			 if (TimeStretch.this.dispatcher != null) {
				 wsola.setParameters(new Parameters(tempoSlider.getValue()/100.0,44100,(Integer) sequenceModel.getValue(),(Integer)windowModel.getValue(),(Integer)overlapModel.getValue()));
			 }
		}}; 
	

	private void startFile(File inputFile){
		if(dispatcher != null){
			dispatcher.stop();
		}
		AudioFormat format;
		try {
			format = AudioSystem.getAudioFileFormat(inputFile).getFormat();
			
			gain = new GainProcessor(1.0);
			audioPlayer = new AudioPlayer(format);
			
			wsola = new WaveformSimilarityBasedOverlapAdd(Parameters.slowdownDefaults(tempoSlider.getValue()/100.0,format.getSampleRate()));
			dispatcher = AudioDispatcher.fromFile(inputFile,wsola.getInputBufferSize(),wsola.getOverlap());
			wsola.setDispatcher(dispatcher);
			dispatcher.addAudioProcessor(wsola);
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
	

	public static void main(String[] argv)
			throws UnsupportedAudioFileException, IOException,
			LineUnavailableException, InterruptedException, InvocationTargetException {
		if (argv.length == 3) {
			startCli(argv[1],argv[2],Double.parseDouble(argv[0]));
		} else {
			startGui();
		}
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
				JFrame frame = new TimeStretch();
				frame.pack();
				frame.setSize(400,350);
				frame.setVisible(true);
			}
		});
	}
	
	private static void startCli(String source,String target,double tempo) throws UnsupportedAudioFileException, IOException{
		/*
		File inputFile = new File(source);
		AudioFormat format = AudioSystem.getAudioFileFormat(inputFile).getFormat();	
		WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(format,Parameters.slowdownDefaults(tempo,format.getSampleRate()));
		wsola.setWaveFormWriter(new WaveformWriter(format, wsola.getOutputBufferSize(), 0, target));
		AudioDispatcher dispatcher = AudioDispatcher.fromFile(inputFile,wsola.getInputBufferSize(),wsola.getOverlap());
		wsola.setDispatcher(dispatcher);
		dispatcher.addAudioProcessor(wsola);
		dispatcher.run();
		*/
	}

}
