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


package be.tarsos.dsp.example;

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

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.WaveformWriter;

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
			dispatcher = AudioDispatcherFactory.fromFile(inputFile,wsola.getInputBufferSize(),wsola.getOverlap());
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
	

	public static void main(String[] argv) {
		if (argv.length == 3) {
			try {
				startCli(argv[0],argv[1],Double.parseDouble(argv[2]));
			} catch (NumberFormatException e) {
				printHelp("Please provide a well formatted number for the time stretching factor. See Synopsis.");
			} catch (UnsupportedAudioFileException e) {
				printHelp("Unsupported audio file, please check if the input is 16bit 44.1kHz mono PCM wav.");
			} catch (IOException e) {
				printHelp("IO error, could not read from or write to the audio file, does it exist?");
			}
		} else if(argv.length!=0){
			printHelp("Please provide exactly 3 arguments, see Synopsis.");
		}else{
			try {
				startGui();
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new Error(e);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				throw new Error(e);
			}
		}
	}
	
	private static final void printHelp(String error){
		SharedCommandLineUtilities.printPrefix();
		System.err.println("Name:");
		System.err.println("\tTarsosDSP Time stretch utility.");
		SharedCommandLineUtilities.printLine();
		System.err.println("Synopsis:");
		System.err.println("\tjava -jar TimeStretch.jar source.wav target.wav factor");
		SharedCommandLineUtilities.printLine();
		System.err.println("Description:");
		System.err.println("\tChange the play back speed of audio without changing the pitch.\n");
		System.err.println("\t\tsource.wav\tA readable, mono wav file.");
		System.err.println("\t\ttarget.wav\tTarget location for the time stretched file.");
		System.err.println("\t\tfactor\t\tTime stretching factor: 2.0 means double the length, 0.5 half. 1.0 is no change.");
		if(!error.isEmpty()){
			SharedCommandLineUtilities.printLine();
			System.err.println("Error:");
			System.err.println("\t" + error);
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
		File inputFile = new File(source);
		AudioFormat format = AudioSystem.getAudioFileFormat(inputFile).getFormat();	
		WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(Parameters.slowdownDefaults(tempo,format.getSampleRate()));
		WaveformWriter writer = new WaveformWriter(format,target);
		AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(inputFile,wsola.getInputBufferSize(),wsola.getOverlap());
		wsola.setDispatcher(dispatcher);
		dispatcher.addAudioProcessor(wsola);
		dispatcher.addAudioProcessor(writer);
		dispatcher.run();
	}

}
