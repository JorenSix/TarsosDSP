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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
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
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.WaveformWriter;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.dsp.synthesis.PitchResyntheziser;

/**
 * The resynthesizer example shows how to use the PitchResnthesizer class. It is
 * an application that extracts pitch from an audio file and resynthesizes it
 * using the envelope of the original signal and the pitch information. It can
 * be used to check pitch detection results.
 * 
 * @author Joren Six
 * 
 */

public class Resynthesizer{

	/**
	 * @param arguments
	 * @throws InterruptedException 
	 * @throws InvocationTargetException 
	 */
	public static void main(String... arguments) throws InterruptedException, InvocationTargetException {
		if(arguments.length==0){
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					try {
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					} catch (Exception e) {
						//ignore failure to set default look en feel;
					}
					JFrame frame = new GraphicalResynthesizer();
					frame.pack();
					frame.setSize(400,350);
					frame.setVisible(true);
				}
			});
		}else{
			new CommandLineResynthesizer(arguments);
		}
	}
	
	private static class GraphicalResynthesizer extends JFrame{

		/**
		 * 
		 */
		private static final long serialVersionUID = 401554060116566946L;
		private final JSlider estimationGainSlider;
		private final JSlider sourceGainSlider;
		private AudioDispatcher estimationDispatcher;
		private AudioDispatcher sourceDispatcher;
		private GainProcessor estimationGain;
		private GainProcessor sourceGain;
		private final JFileChooser fileChooser;
		PitchEstimationAlgorithm algo;
		File currentFile;
		
		private ActionListener algoChangeListener = new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				String name = e.getActionCommand();
				PitchEstimationAlgorithm newAlgo = PitchEstimationAlgorithm.valueOf(name);
				algo = newAlgo;
				if(currentFile!=null ){
					estimationDispatcher.stop();
					sourceDispatcher.stop();
					startFile(currentFile);
				}
				
		}};
		
		public GraphicalResynthesizer(){
			this.setLayout(new BorderLayout());
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.setTitle("Pitch Estimation Synthesizer");
			
			estimationGainSlider = new JSlider(0,200);
			estimationGainSlider.setValue(100);
			estimationGainSlider.setPaintLabels(true);
			estimationGainSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent arg0) {
					if (GraphicalResynthesizer.this.estimationDispatcher != null) {
						double gainValue = estimationGainSlider.getValue() / 100.0;
						estimationGain.setGain(gainValue);
					}
				}
			});
			
			sourceGainSlider = new JSlider(0,200);
			sourceGainSlider.setValue(100);
			sourceGainSlider.setPaintLabels(true);
			sourceGainSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent arg0) {
					if (GraphicalResynthesizer.this.sourceDispatcher != null) {
						double gainValue = sourceGainSlider.getValue() / 100.0;
						sourceGain.setGain(gainValue);
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
					int returnVal = fileChooser.showOpenDialog(GraphicalResynthesizer.this);
		            if (returnVal == JFileChooser.APPROVE_OPTION) {
		                File file = fileChooser.getSelectedFile();
		                startFile(file);
		            } else {
		                //canceled
		            }
				}			
			});
			fileChooserPanel.add(chooseFileButton,BorderLayout.CENTER);
			
			JPanel gainPanel = new JPanel(new GridLayout(2,2));
			JLabel label = new JLabel("Gain source (in %)");
			label.setToolTipText("Volume in % (100 is no change).");
			gainPanel.add(label);
			gainPanel.add(sourceGainSlider);
			label = new JLabel("Gain estimations (in %)");
			label.setToolTipText("Volume in % (100 is no change).");
			gainPanel.add(label);
			gainPanel.add(estimationGainSlider);
			gainPanel.setBorder(new TitledBorder("3. Change the estimation / source"));		
			this.add(fileChooserPanel,BorderLayout.NORTH);
			this.add(gainPanel,BorderLayout.SOUTH);
			JPanel pitchDetectionPanel = new PitchDetectionPanel(algoChangeListener);
			algo=PitchEstimationAlgorithm.YIN;
			this.add(pitchDetectionPanel,BorderLayout.CENTER);
		}



		protected void startFile(File file) {
			currentFile = file;
			AudioFormat format;
			try {
				format = AudioSystem.getAudioFileFormat(file).getFormat();
				float samplerate = format.getSampleRate();
				int size = 1024;
				int overlap = 0;
				
				PitchResyntheziser prs = new PitchResyntheziser(samplerate);
				estimationGain = new GainProcessor(estimationGainSlider.getValue()/100.0);
				estimationDispatcher = AudioDispatcherFactory.fromFile(file, size, overlap);
				estimationDispatcher.addAudioProcessor(new PitchProcessor(algo, samplerate, size, prs));
				estimationDispatcher.addAudioProcessor(estimationGain);
				estimationDispatcher.addAudioProcessor(new AudioPlayer(format));
				
				sourceGain = new GainProcessor(sourceGainSlider.getValue()/100.0);
				sourceDispatcher = AudioDispatcherFactory.fromFile(file, size, overlap);
				sourceDispatcher.addAudioProcessor(sourceGain);
				sourceDispatcher.addAudioProcessor(new AudioPlayer(format));
				
				new Thread(estimationDispatcher).start();
				new Thread(sourceDispatcher).start();
				
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
		
	}
	
	private static class CommandLineResynthesizer {
		public CommandLineResynthesizer(String[] arguments) {
			checkArgumentsAndRun(arguments);
		}
		
		private void checkArgumentsAndRun(String... arguments){
			if(arguments.length == 0){
				printError();
			} else {
				try {
					run(arguments);
				} catch (UnsupportedAudioFileException e) {
					printError();
					SharedCommandLineUtilities.printLine();
					System.err.println("Error:");
					System.err.println("\tThe audio file is not supported!");
				} catch (IOException e) {
					printError();
					SharedCommandLineUtilities.printLine();
					System.err.println("Current error:");
					System.err.println("\tIO error, maybe the audio file is not found or not supported!");
				}
				catch (IllegalArgumentException e) {
						printError();
						SharedCommandLineUtilities.printLine();
						System.err.println("Current error:");
						System.err.println("\tThe algorithm provided is unknown!");
				} catch (LineUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
		}
		
		private void combineTwoMonoAudioFilesInTwoChannels(String first,String second,String outputFile) throws IOException, UnsupportedAudioFileException, LineUnavailableException{
			AudioInputStream stream = AudioSystem.getAudioInputStream(new File(first));
			final float sampleRate = (int) stream.getFormat().getSampleRate();
			final int numberOfSamples = (int) stream.getFrameLength();
			//2 bytes per sample, stereo (2 channels)
			final byte[] byteBuffer = new byte[numberOfSamples * 2 * 2]; 
			/*
			 * Read the source file data in the left channel
			 */
			stream = AudioSystem.getAudioInputStream(new File(first));
			byte[] sampleAsByteArray = new byte[2]; 
			for (int sample = 0; sample < numberOfSamples; sample++) {
				stream.read(sampleAsByteArray);
				byteBuffer[sample * 4 + 0] = sampleAsByteArray[0];
				byteBuffer[sample * 4 + 1] = sampleAsByteArray[1];
			}
			
			/*
			 * Read the source file data in the right channel
			 */
			stream = AudioSystem.getAudioInputStream(new File(second));
			sampleAsByteArray = new byte[2]; 
			for (int sample = 0; sample < numberOfSamples; sample++) {
				stream.read(sampleAsByteArray);
				byteBuffer[sample * 4 + 2] = sampleAsByteArray[0];
				byteBuffer[sample * 4 + 3] = sampleAsByteArray[1];
			}
			
			/*
			 * Write the data to a file.
			 */
			final AudioFormat audioFormat = new AudioFormat(sampleRate, 16, 2, true, false);
			final ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
			final AudioInputStream audioInputStream = new AudioInputStream(bais, audioFormat, numberOfSamples);
			final File out = new File(outputFile);
			AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
			audioInputStream.close();
		}

		private void run(String[] args) throws UnsupportedAudioFileException, IOException, IllegalArgumentException, LineUnavailableException {
			PitchEstimationAlgorithm algo = PitchEstimationAlgorithm.FFT_YIN;
			String inputFile = args[0];
			String outputFile = null;
			String combinedFile = null;
			if(args.length == 3 && args[0].equalsIgnoreCase("--detector")){
				algo = PitchEstimationAlgorithm.valueOf(args[1].toUpperCase());
				inputFile = args[2];
			}else if(args.length == 3 && args[0].equalsIgnoreCase("--output")){
				outputFile = args[1];
				inputFile = args[2];
			}else if(args.length == 5 && args[0].equalsIgnoreCase("--detector") && args[2].equalsIgnoreCase("--output")){
				algo = PitchEstimationAlgorithm.valueOf(args[1].toUpperCase());
				outputFile = args[3];
				inputFile = args[4];
			}else if(args.length == 7 && args[0].equalsIgnoreCase("--detector") && args[2].equalsIgnoreCase("--output") && args[4].equalsIgnoreCase("--combined")){
				algo = PitchEstimationAlgorithm.valueOf(args[1].toUpperCase());
				outputFile = args[3];
				combinedFile = args[5];
				inputFile = args[6];
			} else if(args.length !=1){
				printError();
				SharedCommandLineUtilities.printLine();
				System.err.println("Current error:");
				System.err.println("\tThe command expects the options in the specified order, the current command is not parsed correctly!");
				return;
			}
			File audioFile = new File(inputFile);
			AudioFormat format = AudioSystem.getAudioFileFormat(audioFile).getFormat();
			float samplerate = format.getSampleRate();
			int size = 1024;
			int overlap = 0;
			PitchResyntheziser prs = new PitchResyntheziser(samplerate);
			AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, size, overlap);
			dispatcher.addAudioProcessor(new PitchProcessor(algo, samplerate, size, prs));
			if(outputFile!=null){
				dispatcher.addAudioProcessor(new WaveformWriter(format, outputFile));
			}else{
				dispatcher.addAudioProcessor(new AudioPlayer(format));
			}
			dispatcher.addAudioProcessor(new AudioProcessor() {
				@Override
				public void processingFinished() {}
				
				@Override
				public boolean process(AudioEvent audioEvent) {
					System.err.print(String.format("%3.0f %%",audioEvent.getProgress() * 100));
					System.err.print(String.format("\b\b\b\b\b",audioEvent.getProgress()));
					return true;
				}
			});
			dispatcher.run();
			
			if(combinedFile!=null){
				combineTwoMonoAudioFilesInTwoChannels(inputFile, outputFile, combinedFile);
			}
		}

		private final void printError(){
			SharedCommandLineUtilities.printPrefix();
			System.err.println("Name:");
			System.err.println("\tTarsosDSP resynthesizer");
			SharedCommandLineUtilities.printLine();
			System.err.println("Synopsis:");
			System.err.println("\tjava -jar CommandLineResynthesizer.jar [--detector DETECTOR] [--output out.wav] [--combined combined.wav] input.wav");
			SharedCommandLineUtilities.printLine();
			System.err.println("Description:");
			System.err.println("\tExtracts pitch and loudnes from audio and resynthesises the audio with that information.\n\t" +
					"The result is either played back our written in an output file. \n\t" +
					"There is als an option to combine source and synthezized material\n\t" +
					"in the left and right channels of a stereo audio file.");
			String descr="";
			descr += "\n\n\tinput.wav\t\ta readable wav file.";
			descr += "\n\n\t--output out.wav\t\ta writable file.";
			descr += "\n\n\t--combined combined.wav\t\ta writable output file. One channel original, other synthesized.";
			descr += "\n\t--detector DETECTOR\tdefaults to FFT_YIN or one of these:\n\t\t\t\t";
			for(PitchEstimationAlgorithm algo : PitchEstimationAlgorithm.values()){
				descr += algo.name() + "\n\t\t\t\t";
			}
			System.err.println(descr);
	    }		
	}

}
