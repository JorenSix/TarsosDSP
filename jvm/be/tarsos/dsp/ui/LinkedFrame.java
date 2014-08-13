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

package be.tarsos.dsp.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JSplitPane;

import be.tarsos.dsp.ui.ViewPort.ViewPortChangedListener;
import be.tarsos.dsp.ui.layers.AmplitudeAxisLayer;
import be.tarsos.dsp.ui.layers.BackgroundLayer;
import be.tarsos.dsp.ui.layers.BeatLayer;
import be.tarsos.dsp.ui.layers.ConstantQLayer;
import be.tarsos.dsp.ui.layers.DragMouseListenerLayer;
import be.tarsos.dsp.ui.layers.LegendLayer;
import be.tarsos.dsp.ui.layers.PitchContourLayer;
import be.tarsos.dsp.ui.layers.SelectionLayer;
import be.tarsos.dsp.ui.layers.TimeAxisLayer;
import be.tarsos.dsp.ui.layers.VerticalFrequencyAxisLayer;
import be.tarsos.dsp.ui.layers.WaveFormLayer;
import be.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;
import be.tarsos.dsp.ui.layers.pch.PitchClassHistogramLayer;
import be.tarsos.dsp.ui.layers.pch.ScaleLayer;

public class LinkedFrame extends JFrame implements ViewPortChangedListener {
	private JSplitPane lastSplitPane;
	private static final long serialVersionUID = 7301610309790983406L;

	private static LinkedFrame instance;

	private static HashMap<String, LinkedPanel> panels;

	private boolean drawing = false;

	public static void main(String... strings) {
		LinkedFrame.getInstance();
	}
	
	protected JSplitPane getLastSplitPane(){
		return this.lastSplitPane;
	}

	private LinkedFrame() {
		super();
		panels = new HashMap<String, LinkedPanel>();
	}

	public static LinkedFrame getInstance() {
		if (instance == null) {
			instance = new LinkedFrame();
			instance.initialise();
		}
		return instance;
	}

	public void initialise() {
		this.setMinimumSize(new Dimension(800, 400));
		JSplitPane contentPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.lastSplitPane = contentPane;
		this.setContentPane(contentPane);

		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		contentPane.setDividerLocation(0);
		buildStdSetUp();
		setVisible(true);
	}

	public void createNewSplitPane() {
		lastSplitPane.setDividerSize(2);
		JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		sp.setDividerSize(0);
		lastSplitPane.add(sp, JSplitPane.BOTTOM);
		lastSplitPane = sp;
	}

	public void viewPortChanged(ViewPort newViewPort) {
		if (!drawing) {
			drawing = true;
			for (LinkedPanel panel : panels.values()) {
				panel.repaint();
			}
			drawing = false;
		}
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

	private void buildStdSetUp() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.AMPLITUDE);
		
		File audioFile = new File("/home/joren/Desktop/08._Ladrang_Kandamanyura_10s-20s.wav");
		
		LinkedPanel panel = new LinkedPanel(cs);
		panel.addLayer(new ZoomMouseListenerLayer());
		panel.addLayer(new DragMouseListenerLayer(cs));
		panel.addLayer(new BackgroundLayer(cs));
		panel.addLayer(new AmplitudeAxisLayer(cs));
		panel.addLayer(new TimeAxisLayer(cs));
		panel.addLayer(new WaveFormLayer(cs, audioFile));
		panel.addLayer(new BeatLayer(cs,audioFile,true,true));
		panel.addLayer(new SelectionLayer(cs));

		LegendLayer legend = new LegendLayer(cs,50);
		panel.addLayer(legend);
		legend.addEntry("Onsets",Color.BLUE);
		legend.addEntry("Beats", Color.RED);
		
		panel.getViewPort().addViewPortChangedListener(this);
		
		LinkedFrame.panels.put("Waveform", panel);
		
		this.lastSplitPane.add(panel, JSplitPane.TOP);
		
		cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		panel = new LinkedPanel(cs);
		panel.addLayer(new ZoomMouseListenerLayer());
		panel.addLayer(new DragMouseListenerLayer(cs));
		panel.addLayer(new BackgroundLayer(cs));
		panel.addLayer(new ConstantQLayer(cs,audioFile,2048,3600,10800,12));
	//	panel.addLayer(new FFTLayer(cs,audioFile,2048,512));
		panel.addLayer(new PitchContourLayer(cs,audioFile,Color.red,2048,1024));
		panel.addLayer(new SelectionLayer(cs));
		panel.addLayer(new VerticalFrequencyAxisLayer(cs));
		panel.addLayer(new TimeAxisLayer(cs));
		panel.getViewPort().addViewPortChangedListener(this);
		
		
		CoordinateSystem pchCS = new CoordinateSystem(AxisUnit.OCCURENCES, 0, 1000,true);
		pchCS.setMin(Axis.X, 0);
		pchCS.setMax(Axis.X, 1200);
		final LinkedPanel pchPanel = new LinkedPanel(pchCS);
		
		pchPanel.addLayer(new BackgroundLayer(pchCS));
		pchPanel.addLayer(new DragMouseListenerLayer(pchCS));
		pchPanel.addLayer(new PitchClassHistogramLayer());
		pchPanel.addLayer(new ScaleLayer(pchCS,true));
		pchPanel.addLayer(new ScaleLayer(pchCS,false));
		
		
		pchPanel.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			boolean painting = false;
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				if(!painting){
					painting = true;
					pchPanel.repaint();
					painting = false;
				}
					
				
			}
		});
		
		this.lastSplitPane.add(pchPanel, JSplitPane.BOTTOM);
		
		
		LinkedFrame.panels.put("Spectral info", panel);
		
		lastSplitPane.setDividerLocation(0.7);
	}
}
