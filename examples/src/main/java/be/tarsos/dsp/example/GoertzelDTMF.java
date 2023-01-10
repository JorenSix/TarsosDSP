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
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.DTMF;
import be.tarsos.dsp.pitch.Goertzel;
import be.tarsos.dsp.pitch.Goertzel.FrequenciesDetectedHandler;

/**
 * An example of DTMF ( Dual-tone multi-frequency signaling ) decoding with the Goertzel algorithm.
 * @author Joren Six
 */
public class GoertzelDTMF extends JFrame implements ActionListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1143769091770146361L;
	
	private KeyAdapter keyAdapter = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent event) {
			if(DTMF.isDTMFCharacter(event.getKeyChar())){
				try {
					process(event.getKeyChar());
				} catch (UnsupportedAudioFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (LineUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	};
	
	private final int stepSize = 256;
	
	private final AudioProcessor goertzelAudioProcessor = new Goertzel(44100, stepSize,DTMF.DTMF_FREQUENCIES, new FrequenciesDetectedHandler() {
		@Override
		public void handleDetectedFrequencies(double time,final double[] frequencies, final double[] powers, final double[] allFrequencies, final double allPowers[]) {
			if (frequencies.length == 2) {
				int rowIndex = -1;
				int colIndex = -1;
				for (int i = 0; i < 4; i++) {
					if (frequencies[0] == DTMF.DTMF_FREQUENCIES[i] || frequencies[1] == DTMF.DTMF_FREQUENCIES[i])
						rowIndex = i;
				}
				for (int i = 4; i < DTMF.DTMF_FREQUENCIES.length; i++) {
					if (frequencies[0] == DTMF.DTMF_FREQUENCIES[i] || frequencies[1] == DTMF.DTMF_FREQUENCIES[i])
						colIndex = i-4;
				}
				if(rowIndex>=0 && colIndex>=0){
					detectedChar.setText(""+DTMF.DTMF_CHARACTERS[rowIndex][colIndex]);
					for (int i = 0; i < allPowers.length; i++) {
						powerBars[i].setValue((int) allPowers[i]);
					}
				}
			}
		}
	});
	
	private final JProgressBar[] powerBars;
	private final JLabel detectedChar = new JLabel(" ");
	
	public GoertzelDTMF(){
		this.getContentPane().setLayout(new BorderLayout(5,3));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Goertzel");
		
		JPanel detectionPanel = new JPanel(new GridLayout(DTMF.DTMF_FREQUENCIES.length,2,5,3));
		powerBars = new JProgressBar[DTMF.DTMF_FREQUENCIES.length];
		for(int i= 0 ; i < DTMF.DTMF_FREQUENCIES.length ; i++){
			detectionPanel.add(new JLabel(DTMF.DTMF_FREQUENCIES[i] + "Hz"));
			powerBars[i] = new JProgressBar(-30,50);
			detectionPanel.add(powerBars[i]);
			powerBars[i].setValue(-30);
		}
		detectionPanel.setBorder(new TitledBorder("Detected Powers"));
		
		JPanel labelPanel = new JPanel(new BorderLayout());
		labelPanel.add(detectionPanel,BorderLayout.NORTH);
		
		detectedChar.setBorder(new TitledBorder("Detected character"));
		detectedChar.setHorizontalAlignment(JLabel.CENTER);
		
		Font f = new Font("Police", Font.PLAIN, 20);
		detectedChar.setFont(f);
		
		labelPanel.add(detectedChar,BorderLayout.CENTER);
		
		JPanel dailPad = new JPanel(new GridLayout(4,4));
		dailPad.setBorder(new TitledBorder("DailPad"));
		for(int row = 0 ; row < DTMF.DTMF_CHARACTERS.length ; row++){
			for(int col = 0 ; col < DTMF.DTMF_CHARACTERS[row].length ; col++){
				JButton numberButton = new JButton(DTMF.DTMF_CHARACTERS[row][col]+"");
				numberButton.addActionListener(this);
				numberButton.addKeyListener(keyAdapter);
				dailPad.add(numberButton);
			}
		}
		this.addKeyListener(keyAdapter);
		dailPad.addKeyListener(keyAdapter);
		
		this.add(dailPad,BorderLayout.CENTER);
		this.add(labelPanel,BorderLayout.SOUTH);
	}

	public static void main(String...strings){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					//ignore failure to set default look & feel;
				}
				JFrame frame = new GoertzelDTMF();
				frame.pack();
				frame.setSize(200,420);
				frame.setVisible(true);
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		JButton button = ((JButton) event.getSource());
		//System.out.println(button.getText().charAt(0));
		try {
			process(button.getText().charAt(0));
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Process a DTMF character: generate sound and decode the sound.
	 * @param character The character.
	 * @throws UnsupportedAudioFileException
	 * @throws LineUnavailableException
	 */
	public void process(char character) throws UnsupportedAudioFileException, LineUnavailableException{
		final float[] floatBuffer = DTMF.generateDTMFTone(character);		
		final AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
		JVMAudioInputStream.toTarsosDSPFormat(format);
		final TarsosDSPAudioFloatConverter converter = TarsosDSPAudioFloatConverter.getConverter(JVMAudioInputStream.toTarsosDSPFormat(format));
		final byte[] byteBuffer = new byte[floatBuffer.length * format.getFrameSize()];
		converter.toByteArray(floatBuffer, byteBuffer);
		final ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);		
		final AudioInputStream inputStream = new AudioInputStream(bais, format,floatBuffer.length);
		final TarsosDSPAudioInputStream stream = new JVMAudioInputStream(inputStream);
		final AudioDispatcher dispatcher = new AudioDispatcher(stream, stepSize, 0);		
		dispatcher.addAudioProcessor(goertzelAudioProcessor);
		dispatcher.addAudioProcessor(new AudioPlayer(format));
		new Thread(dispatcher).start();
		
	}
}
