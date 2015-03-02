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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import be.tarsos.dsp.ui.layers.Layer;


public class LinkedPanel extends JPanel {

	private static final long serialVersionUID = -5055686566048886896L;
	
	private List<Layer> layers;
	private final ViewPort viewPort;
	private CoordinateSystem cs;


	public LinkedPanel(CoordinateSystem coordinateSystem) {
		super();
		//makes sure key events are registered
		this.setFocusable(true);
		layers = new ArrayList<Layer>();
		this.cs = coordinateSystem;
		viewPort = new ViewPort(this.cs);
		this.setVisible(true);
		
		//regain focus on mouse enter to get key presses
		this.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {}
			
			@Override
			public void mousePressed(MouseEvent e) {}
			
			@Override
			public void mouseExited(MouseEvent e) {
				LinkedPanel.this.transferFocusBackward();
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				LinkedPanel.this.requestFocus();
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {}
		});
	}
	
	public CoordinateSystem getCoordinateSystem() {
		return cs;
	}

	public ViewPort getViewPort() {
		return viewPort;
	}

	public void addLayer(Layer l) {
		this.layers.add(l);		
		if(l instanceof MouseMotionListener){
			this.addMouseMotionListener((MouseMotionListener) l);
		}
		if(l instanceof MouseListener){
			this.addMouseListener((MouseListener) l);
		}
		if(l instanceof MouseWheelListener){
			this.addMouseWheelListener((MouseWheelListener) l);
		}
		if(l instanceof KeyListener){
			this.addKeyListener((KeyListener)l);
		}
	}
	
	public AffineTransform getTransform() {
		double xDelta = cs.getDelta(Axis.X);
		double yDelta = cs.getDelta(Axis.Y);
		AffineTransform transform = new AffineTransform();
		transform.translate(0, getHeight());
		transform.scale(getWidth() / xDelta, - getHeight() / yDelta);
		transform.translate(-cs.getMin(Axis.X),-cs.getMin(Axis.Y));
		return transform;
	}


	public AffineTransform updateTransform(AffineTransform transform) {
		double xDelta = cs.getDelta(Axis.X);
		double yDelta = cs.getDelta(Axis.Y);
		transform.translate(0, getHeight());
		transform.scale(getWidth() / xDelta, -getHeight() / yDelta);
		transform.translate(-cs.getMin(Axis.X),-cs.getMin(Axis.Y));
		return transform;
	}
	
	@Override
	public void paintComponent(final Graphics g) {
		super.paintComponent(g);
		final Graphics2D graphics = (Graphics2D) g.create();
		graphics.setTransform(this.updateTransform(graphics.getTransform()));
		if (!layers.isEmpty()) {
			for (Layer layer : layers) {
				layer.draw(graphics);
			}
		}
	}
	
	public void removeLayer(Layer layer) {
		layers.remove(layer);
		if(layer instanceof MouseMotionListener){
			this.removeMouseMotionListener((MouseMotionListener) layer);
		}
		if(layer instanceof MouseListener){
			this.removeMouseListener((MouseListener) layer);
		}
		if(layer instanceof MouseWheelListener){
			this.removeMouseWheelListener((MouseWheelListener) layer);
		}
		if(layer instanceof KeyListener){
			this.removeKeyListener((KeyListener)layer);
		}
	}
	
	public void removeLayers(){
		while(layers.size()> 0){
			removeLayer(layers.get(0));
		}
	}	
}
