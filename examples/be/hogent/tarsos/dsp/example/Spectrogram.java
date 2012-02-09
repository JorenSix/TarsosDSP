package be.hogent.tarsos.dsp.example;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.PitchProcessor;
import be.hogent.tarsos.dsp.PitchProcessor.DetectedPitchHandler;
import be.hogent.tarsos.dsp.PitchProcessor.PitchEstimationAlgorithm;
import be.hogent.tarsos.dsp.util.FFT;

public class Spectrogram extends JFrame implements DetectedPitchHandler {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1383896180290138076L;
	private final SpectrogramPanel panel;
	private AudioDispatcher dispatcher;
	private Mixer currentMixer;	
	private PitchEstimationAlgorithm algo;
	double pitch ; 
	
	private final float sampleRate = 44100;
	private final int bufferSize = 8192;
	private final int overlap = 7168;
	
	private ActionListener algoChangeListener = new ActionListener(){
		@Override
		public void actionPerformed(final ActionEvent e) {
			String name = e.getActionCommand();
			PitchEstimationAlgorithm newAlgo = PitchEstimationAlgorithm.valueOf(name);
			algo = newAlgo;
			try {
				setNewMixer(currentMixer);
			} catch (LineUnavailableException e1) {
				e1.printStackTrace();
			} catch (UnsupportedAudioFileException e1) {
				e1.printStackTrace();
			}
	}};
		
	public Spectrogram(){
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Spectrogram");
		panel = new SpectrogramPanel();
		algo = PitchEstimationAlgorithm.YIN;
		
		JPanel pitchDetectionPanel = new PitchDetectionPanel(algoChangeListener);
		
		JPanel inputPanel = new InputPanel();
	
		inputPanel.addPropertyChangeListener("mixer",
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						try {
							setNewMixer((Mixer) arg0.getNewValue());
						} catch (LineUnavailableException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (UnsupportedAudioFileException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
		
		JPanel containerPanel = new JPanel(new GridLayout(1,0));
		containerPanel.add(inputPanel);
		containerPanel.add(pitchDetectionPanel);
		this.add(containerPanel,BorderLayout.NORTH);
		
		JPanel otherContainer = new JPanel(new BorderLayout());
		otherContainer.add(panel,BorderLayout.CENTER);
		otherContainer.setBorder(new TitledBorder("3. Utter a sound (whistling works best)"));
			
		this.add(otherContainer,BorderLayout.CENTER);
	}
	
	
	
	private void setNewMixer(Mixer mixer) throws LineUnavailableException, UnsupportedAudioFileException {

		if(dispatcher!= null){
			dispatcher.stop();
		}
		currentMixer = mixer;
		

		
		//textArea.append("Started listening with " + mixer.getMixerInfo().getName() + "\n\tparams: " + threshold + "dB\n");

		final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true,
				false);
		final DataLine.Info dataLineInfo = new DataLine.Info(
				TargetDataLine.class, format);
		TargetDataLine line;
		line = (TargetDataLine) mixer.getLine(dataLineInfo);
		final int numberOfSamples = bufferSize;
		line.open(format, numberOfSamples);
		line.start();
		final AudioInputStream stream = new AudioInputStream(line);

		// create a new dispatcher
		dispatcher = new AudioDispatcher(stream, bufferSize,
				overlap);

		// add a processor, handle pitch event.
		dispatcher.addAudioProcessor(new PitchProcessor(algo, sampleRate, bufferSize, overlap, 0, this));
		
		dispatcher.addAudioProcessor(fftProcessor);

		// run the dispatcher (on a new thread).
		new Thread(dispatcher,"Audio dispatching").start();
	}
	
	AudioProcessor fftProcessor = new AudioProcessor(){
		
		FFT fft = new FFT(bufferSize);
		float[] amplitudes = new float[bufferSize/2];
	

		@Override
		public boolean processFull(float[] audioFloatBuffer,
				byte[] audioByteBuffer) {
			return true;
		}

		@Override
		public boolean processOverlapping(float[] audioFloatBuffer,
				byte[] audioByteBuffer) {
			float[] transformbuffer = new float[bufferSize*2];
			
			System.arraycopy(audioFloatBuffer, 0, transformbuffer, 0, audioFloatBuffer.length); 
			fft.forwardTransform(transformbuffer);
			fft.modulus(transformbuffer, amplitudes);
			panel.drawFFT(pitch, amplitudes,fft);
			return true;
		}

		@Override
		public void processingFinished() {
			// TODO Auto-generated method stub
		}
		
	};
	
	@Override
	public void handlePitch(float pitch, float probability, float timeStamp,
			float progress) {
		this.pitch = pitch;
	}
	
	public static void main(String... strings) throws InterruptedException,
			InvocationTargetException {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager
							.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					// ignore failure to set default look en feel;
				}
				JFrame frame = new Spectrogram();
				frame.pack();
				frame.setSize(640, 480);
				frame.setVisible(true);
			}
		});
}
	

}
