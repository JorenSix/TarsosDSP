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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.MultichannelToMono;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.jvm.WaveformWriter;
import be.tarsos.dsp.resample.RateTransposer;

public class PitchShiftingExample extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3830419374132803358L;

	private JFileChooser fileChooser;

	private AudioDispatcher dispatcher;
	private WaveformSimilarityBasedOverlapAdd wsola;
	private GainProcessor gain;
	private AudioPlayer audioPlayer;
	private RateTransposer rateTransposer;
	private double currentFactor;// pitch shift factor
	private double sampleRate;
	private boolean loop;
	
	private final JSlider factorSlider;
	private final JLabel factorLabel;
	private final JSlider gainSlider;
	private final JCheckBox originalTempoCheckBox;
	private final JSpinner centsSpinner;
	
	
	
	private ChangeListener parameterSettingChangedListener = new ChangeListener(){
@Override
		public void stateChanged(ChangeEvent arg0) {
			if (arg0.getSource() instanceof JSpinner) {
				int centValue = Integer.valueOf(((JSpinner) arg0.getSource())
						.getValue().toString());
				currentFactor = centToFactor(centValue);
				factorSlider.removeChangeListener(this);
				factorSlider.setValue((int) Math.round(currentFactor * 100));
				factorSlider.addChangeListener(this);
			} else if (arg0.getSource() instanceof JSlider) {
				currentFactor = factorSlider.getValue() / 100.0;
				centsSpinner.removeChangeListener(this);
				centsSpinner.setValue(factorToCents(currentFactor));
				centsSpinner.addChangeListener(this);
			}
			factorLabel.setText("Factor " + Math.round(currentFactor * 100) + "%");
			if (PitchShiftingExample.this.dispatcher != null) {				 
				 if(originalTempoCheckBox.getModel().isSelected()){
					 wsola.setParameters(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(currentFactor, sampleRate));
				 } else {
					 wsola.setParameters(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(1, sampleRate));
				 }
				 rateTransposer.setFactor(currentFactor);
			 }
		}}; 
		
	public PitchShiftingExample(){
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Pitch shifting: change the pitch of your audio.");
		
		originalTempoCheckBox = new JCheckBox("Keep original tempo?", true);
		originalTempoCheckBox.addChangeListener(parameterSettingChangedListener);
		
		this.loop = false;
		
		currentFactor = 1;
		
		factorSlider = new JSlider(20, 250);
		factorSlider.setValue(100);
		factorSlider.setPaintLabels(true);
		factorSlider.addChangeListener(parameterSettingChangedListener);
		
		JPanel fileChooserPanel = new JPanel(new BorderLayout());
		fileChooserPanel.setBorder(new TitledBorder("1... Or choose your audio (wav mono)"));
		
		fileChooser = new JFileChooser();
		
		JButton chooseFileButton = new JButton("Choose a file...");
		chooseFileButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(PitchShiftingExample.this);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fileChooser.getSelectedFile();
	                startFile(file,null);
	            } else {
	                //canceled
	            }
			}			
		});
		fileChooserPanel.add(chooseFileButton);
		fileChooser.setLayout(new BoxLayout(fileChooser, BoxLayout.PAGE_AXIS));
		

		JPanel inputSubPanel = new JPanel(new BorderLayout());
		JPanel inputPanel = new InputPanel();
		inputPanel.addPropertyChangeListener("mixer",
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						startFile(null,(Mixer) arg0.getNewValue());
					}
				});
		inputSubPanel.add(inputPanel,BorderLayout.NORTH);
		inputSubPanel.add(fileChooserPanel,BorderLayout.SOUTH);
		
		gainSlider = new JSlider(0,200);
		gainSlider.setValue(100);
		gainSlider.setPaintLabels(true);
		gainSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (PitchShiftingExample.this.dispatcher != null) {
					double gainValue = gainSlider.getValue() / 100.0;
					gain.setGain(gainValue);
				}
			}
		});
		
		
		JPanel params = new JPanel(new BorderLayout());
		params.setBorder(new TitledBorder("2. Set the algorithm parameters"));
		
		JCheckBox loopCheckbox = new JCheckBox(("Loop sample?"));
		loopCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loop = ((JCheckBox)e.getSource()).isSelected();
			}
		});
		
		JLabel label = new JLabel("Factor 100%");
		label.setToolTipText("The pitch shift factor in % (100 is no change, 50 is double pitch, 200 half).");
		factorLabel = label;
		params.add(label,BorderLayout.NORTH);
		params.add(factorSlider,BorderLayout.CENTER);
		
		
		JPanel subPanel = new JPanel(new GridLayout(0, 2));
			
		centsSpinner = new JSpinner();
		centsSpinner.addChangeListener(parameterSettingChangedListener);
		label = new JLabel("Pitch shift in cents");
		label.setToolTipText("Pitch shift in cents.");
		subPanel.add(label);
		subPanel.add(centsSpinner);

		label = new JLabel("Keep original tempo");
		label.setToolTipText("Pitch shift in cents.");
		subPanel.add(label);
		subPanel.add(originalTempoCheckBox);
		
		label = new JLabel("Loop sample?");
		subPanel.add(label);
		subPanel.add(loopCheckbox);
		
		params.add(subPanel,BorderLayout.SOUTH);
		
		JPanel gainPanel = new JPanel(new BorderLayout());
		label = new JLabel("Gain (in %)");
		label.setToolTipText("Volume in % (100 is no change).");
		gainPanel.add(label,BorderLayout.NORTH);
		gainPanel.add(gainSlider,BorderLayout.CENTER);
		gainPanel.setBorder(new TitledBorder("3. Optionally change the volume"));
		
		this.add(inputSubPanel,BorderLayout.NORTH);
		this.add(params,BorderLayout.CENTER);
		this.add(gainPanel,BorderLayout.SOUTH);
		
	}
	
	public static double centToFactor(double cents){
		return 1 / Math.pow(Math.E,cents*Math.log(2)/1200/Math.log(Math.E)); 
	}
	private static double factorToCents(double factor){
		return 1200 * Math.log(1/factor) / Math.log(2); 
	}
	
	private void startFile(final File inputFile,Mixer mixer){
		if(dispatcher != null){
			dispatcher.stop();
		}
		AudioFormat format;
		try {
			if(inputFile != null){
				format = AudioSystem.getAudioFileFormat(inputFile).getFormat();
			}else{
				format = new AudioFormat(44100, 16, 1, true,true);
			}
			rateTransposer = new RateTransposer(currentFactor);
			gain = new GainProcessor(1.0);
			audioPlayer = new AudioPlayer(format);
			sampleRate = format.getSampleRate();
			
			//can not time travel, unfortunately. It would be nice to go back and kill Hitler or something...
			 if(originalTempoCheckBox.getModel().isSelected() && inputFile != null){
				 wsola = new WaveformSimilarityBasedOverlapAdd(Parameters.musicDefaults(currentFactor, sampleRate));
			 } else {
				 wsola = new WaveformSimilarityBasedOverlapAdd(Parameters.musicDefaults(1, sampleRate));
			 }
			 if(inputFile == null){
				 DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
					TargetDataLine line;
					line = (TargetDataLine) mixer.getLine(dataLineInfo);
					line.open(format, wsola.getInputBufferSize());
					line.start();
					final AudioInputStream stream = new AudioInputStream(line);
					JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
					// create a new dispatcher
					dispatcher = new AudioDispatcher(audioStream, wsola.getInputBufferSize(),wsola.getOverlap()); 
			 }else{
					if(format.getChannels() != 1){
						dispatcher = AudioDispatcherFactory.fromFile(inputFile,wsola.getInputBufferSize() * format.getChannels(),wsola.getOverlap() * format.getChannels());
						dispatcher.addAudioProcessor(new MultichannelToMono(format.getChannels(),true));
					}else{
						dispatcher = AudioDispatcherFactory.fromFile(inputFile,wsola.getInputBufferSize(),wsola.getOverlap());
					}
				 //dispatcher = AudioDispatcher.fromFile(inputFile,wsola.getInputBufferSize(),wsola.getOverlap());
			 }
			wsola.setDispatcher(dispatcher);
			dispatcher.addAudioProcessor(wsola);
			dispatcher.addAudioProcessor(rateTransposer);
			dispatcher.addAudioProcessor(gain);
			dispatcher.addAudioProcessor(audioPlayer);
			dispatcher.addAudioProcessor(new AudioProcessor() {
				
				@Override
				public void processingFinished() {
					if(loop){
					dispatcher =null;
					startFile(inputFile,null);
					}
					
				}
				
				@Override
				public boolean process(AudioEvent audioEvent) {
					return true;
				}
			});

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
				printHelp("Unsupported audio file, please check if the input is 16bit 44.1kHz MONO PCM wav.");
			} catch (IOException e) {
				printHelp("IO error, could not read from, or write to the audio file, does it exist?");
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
	
	private static void startGui() throws InterruptedException, InvocationTargetException{
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					//ignore failure to set default look en feel;
				}
				JFrame frame = new PitchShiftingExample();
				frame.pack();
				frame.setSize(400,450);
				frame.setVisible(true);
			}
		});
	}
	
	private static void startCli(String source,String target,double cents) throws UnsupportedAudioFileException, IOException{
		File inputFile = new File(source);
		AudioFormat format = AudioSystem.getAudioFileFormat(inputFile).getFormat();	
		double sampleRate = format.getSampleRate();
		double factor = PitchShiftingExample.centToFactor(cents);
		RateTransposer rateTransposer = new RateTransposer(factor);
		WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(Parameters.musicDefaults(factor, sampleRate));
		WaveformWriter writer = new WaveformWriter(format,target);
		AudioDispatcher dispatcher;
		if(format.getChannels() != 1){
			dispatcher = AudioDispatcherFactory.fromFile(inputFile,wsola.getInputBufferSize() * format.getChannels(),wsola.getOverlap() * format.getChannels());
			dispatcher.addAudioProcessor(new MultichannelToMono(format.getChannels(),true));
		}else{
			dispatcher = AudioDispatcherFactory.fromFile(inputFile,wsola.getInputBufferSize(),wsola.getOverlap());
		}
		wsola.setDispatcher(dispatcher);
		dispatcher.addAudioProcessor(wsola);
		dispatcher.addAudioProcessor(rateTransposer);
		dispatcher.addAudioProcessor(writer);
		dispatcher.run();
	}

	
	private static final void printHelp(String error){
		SharedCommandLineUtilities.printPrefix();
		System.err.println("Name:");
		System.err.println("\tTarsosDSP Pitch shifting utility.");
		SharedCommandLineUtilities.printLine();
		System.err.println("Synopsis:");
		System.err.println("\tjava -jar PitchShift.jar source.wav target.wav cents");
		SharedCommandLineUtilities.printLine();
		System.err.println("Description:");
		System.err.println("\tChange the play back speed of audio without changing the pitch.\n");
		System.err.println("\t\tsource.wav\tA readable, mono wav file.");
		System.err.println("\t\ttarget.wav\tTarget location for the pitch shifted file.");
		System.err.println("\t\tcents\t\tPitch shifting in cents: 100 means one semitone up, -100 one down, 0 is no change. 1200 is one octave up.");
		if(!error.isEmpty()){
			SharedCommandLineUtilities.printLine();
			System.err.println("Error:");
			System.err.println("\t" + error);
		}
    }
}
