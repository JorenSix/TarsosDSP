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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
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

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.Oscilloscope;
import be.hogent.tarsos.dsp.Oscilloscope.OscilloscopeEventHandler;

public class OscilloscopeExample extends JFrame implements OscilloscopeEventHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3501426880288136245L;
	int counter;
	double threshold;
	AudioDispatcher dispatcher;
	Mixer currentMixer;
	private final GaphPanel panel;
	
	public OscilloscopeExample() {
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Osciloscope Example");
		
		JPanel inputPanel = new InputPanel();
		//add(inputPanel);
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
		
				
		panel = new GaphPanel();
		this.add(inputPanel,BorderLayout.NORTH);
		this.add(panel,BorderLayout.CENTER);
	}
	
	private static class GaphPanel extends JPanel{

		/**
		 * 
		 */
		private static final long serialVersionUID = 4969781241442094359L;
		
		float data[];
		
		public GaphPanel(){
			setMinimumSize(new Dimension(80,60)); 
		}
		
		public void paintComponent(Graphics g) {
	        super.paintComponent(g); //paint background
	        g.setColor(Color.BLACK);
			g.fillRect(0, 0,getWidth(), getHeight());
			g.setColor(Color.WHITE);
			if(data != null){
				float width = getWidth();
				float height = getHeight();
				float halfHeight = height / 2;
				for(int i=0; i < data.length ; i+=4){
					 g.drawLine((int)(data[i]* width),(int)( halfHeight - data[i+1]* height),(int)( data[i+2]*width),(int)( halfHeight - data[i+3]*height));
				}
			}
	    }
		
		public void paint(float[] data, AudioEvent event){
			this.data = data;
		}
	}
	
	

	private void setNewMixer(Mixer mixer) throws LineUnavailableException,
			UnsupportedAudioFileException {
		
		if(dispatcher!= null){
			dispatcher.stop();
		}
		currentMixer = mixer;
		
		float sampleRate = 44100;
		int bufferSize = 2048;
		int overlap = 0;
		
		final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true,
				true);
		final DataLine.Info dataLineInfo = new DataLine.Info(
				TargetDataLine.class, format);
		TargetDataLine line;
		line = (TargetDataLine) mixer.getLine(dataLineInfo);
		final int numberOfSamples = bufferSize;
		line.open(format, numberOfSamples);
		line.start();
		final AudioInputStream stream = new AudioInputStream(line);

		// create a new dispatcher
		dispatcher = new AudioDispatcher(stream, bufferSize,overlap);

		// add a processor, handle percussion event.
		//dispatcher.addAudioProcessor(new DelayEffect(400,0.3,sampleRate));
		dispatcher.addAudioProcessor(new Oscilloscope(this));
		//dispatcher.addAudioProcessor(new AudioPlayer(format));
		
		// run the dispatcher (on a new thread).
		new Thread(dispatcher,"Audio dispatching").start();
	}

	public static void main(String... strings) throws InterruptedException,
			InvocationTargetException {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new OscilloscopeExample();
				frame.pack();
				frame.setSize(640,480);
				frame.setVisible(true);
			}
		});
	}

	@Override
	public void handleEvent(float[] data, AudioEvent event) {
		panel.paint(data,event);
		panel.repaint();
	}
}
