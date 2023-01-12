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

package be.tarsos.dsp.example.gui;

import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;

import javax.sound.sampled.AudioFormat;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.BitDepthProcessor;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.example.TarsosDSPExampleStarter;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.wavelet.HaarWaveletCoder;
import be.tarsos.dsp.wavelet.HaarWaveletDecoder;

public class HaarWaveletAudioCompression extends JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7385116325411493047L;

	private HaarWaveletCoder coder;
	private GainProcessor gain;
	private BitDepthProcessor bithDeptProcessor;

	private AudioDispatcher adp;

	
	public HaarWaveletAudioCompression(final String source){
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setLocationRelativeTo(null);
		this.setTitle("HaarWavelet Wavelet Audio Compression Example");

		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosed(java.awt.event.WindowEvent windowEvent) {
				if(adp!=null) adp.stop();
			}
		});

		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					adp = AudioDispatcherFactory.fromPipe(source, 44100, 32,0);
					AudioFormat format = JVMAudioInputStream.toAudioFormat(adp.getFormat());
					coder = new HaarWaveletCoder();
					HaarWaveletDecoder decoder = new HaarWaveletDecoder();
					gain = new GainProcessor(1.0);
					bithDeptProcessor = new BitDepthProcessor();
					bithDeptProcessor.setBitDepth(adp.getFormat().getSampleSizeInBits());

					adp.addAudioProcessor(coder);
					adp.addAudioProcessor(decoder);
					adp.addAudioProcessor(gain);
					adp.addAudioProcessor(bithDeptProcessor);
					adp.addAudioProcessor(new AudioPlayer(format));
					// start on a new thread
					new Thread(adp, "Audio processor").start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		new Thread(r, "Start processor").start();

		this.add(this.createGainPanel(),BorderLayout.NORTH);
		this.add(this.createCompressionPanel(),BorderLayout.CENTER);
		this.add(this.createBitDepthCompressionPanel(16),BorderLayout.SOUTH);
		
	}
	
	private JComponent createGainPanel(){
		final JSlider gainSlider;
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
				if(gain  !=null)
				gain.setGain(gainValue);
			}
		});
		
		JPanel gainPanel = new JPanel(new BorderLayout());
		label.setToolTipText("Volume in % (100 is no change).");
		gainPanel.add(label,BorderLayout.NORTH);
		gainPanel.add(gainSlider,BorderLayout.CENTER);
		gainPanel.setBorder(new TitledBorder("Volume control"));
		return gainPanel;
	}
	
	private JComponent createCompressionPanel(){
		
		final JSlider compressionSlider = new JSlider(0,31);
		compressionSlider.setValue(10);
		compressionSlider.setPaintLabels(true);
		compressionSlider.setPaintTicks(true);
		final JLabel label = new JLabel("Compression: 10");
		compressionSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				int compressionValue = compressionSlider.getValue();
				label.setText(String.format("Compression: %3d", compressionValue));
				if(coder  !=null)
				coder.setCompression(compressionValue);
			}
		});
		
		JPanel compressionPanel = new JPanel(new BorderLayout());
		label.setToolTipText("Compression in steps (0 is no compression, 32 is no signal).");
		compressionPanel.add(label,BorderLayout.NORTH);
		compressionPanel.add(compressionSlider,BorderLayout.CENTER);
		compressionPanel.setBorder(new TitledBorder("Compression control"));
		return compressionPanel;
	}
	
	private JComponent createBitDepthCompressionPanel(int maxValue){
		
		final JSlider bitDepthcompressionSlider = new JSlider(0,maxValue);
		bitDepthcompressionSlider.setValue(maxValue);
		bitDepthcompressionSlider.setPaintLabels(true);
		bitDepthcompressionSlider.setPaintTicks(true);
		final JLabel label = new JLabel("Bit depth (bits): "+maxValue);
		bitDepthcompressionSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				int bitDepth = bitDepthcompressionSlider.getValue();
				label.setText(String.format("Bit depth (bits): %3d", bitDepth));
				if(bithDeptProcessor  !=null)
				bithDeptProcessor.setBitDepth(bitDepth);
			}
		});
		JPanel compressionPanel = new JPanel(new BorderLayout());
		label.setToolTipText("Bit depth in bits.");
		compressionPanel.add(label,BorderLayout.NORTH);
		compressionPanel.add(bitDepthcompressionSlider,BorderLayout.CENTER);
		compressionPanel.setBorder(new TitledBorder("Bith depth control"));
		return compressionPanel;
	}
	

	public static void main (String[] args) throws InvocationTargetException, InterruptedException{
		final String source;
		if(args.length == 1){
			source = args[0];
		}else{
			source =  "https://22653.live.streamtheworld.com/RADIO1_128.mp3";
		}

		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					//ignore failure to set default look & feel;
				}
				JFrame frame = new HaarWaveletAudioCompression(source);
				frame.pack();
				frame.setSize(450,250);
				frame.setVisible(true);
				frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
			}
		};
		new Thread(r).start();
	}


	public static class HaarWaveletAudioCompressionStarter extends TarsosDSPExampleStarter {

		@Override
		public String name() {
			return "Audio Compression";
		}

		@Override
		public String description(){
			return "Use a Haar wavelet or bit depth audio compressor on audio streamed over http";
		}

		@Override
		public void start(String... args) {
			try {
				HaarWaveletAudioCompression.main(args);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
