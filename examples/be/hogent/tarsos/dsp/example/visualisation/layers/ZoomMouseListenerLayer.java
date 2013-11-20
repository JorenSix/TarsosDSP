package be.hogent.tarsos.dsp.example.visualisation.layers;

import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import be.hogent.tarsos.dsp.example.visualisation.LinkedPanel;

public class ZoomMouseListenerLayer extends MouseAdapter implements Layer {

	@Override
	public void draw(Graphics2D graphics) {
		//draw nothing, react to mouse events
	}

	@Override
	public String getName() {
		return "Zoom mouse listener";
	}
	
	
	@Override 
	public void mouseWheelMoved(MouseWheelEvent e) {
		LinkedPanel panel = (LinkedPanel) e.getComponent();
		int amount = e.getWheelRotation() * e.getScrollAmount();
		panel.getViewPort().zoom(amount, e.getPoint());
	}
	
	@Override 
	public void mouseClicked(MouseEvent e){
		if(e.getClickCount()==2){
			LinkedPanel panel = (LinkedPanel) e.getComponent();
			panel.getViewPort().resetZoom();
		}
	}

}
