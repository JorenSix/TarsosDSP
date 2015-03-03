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

package be.tarsos.dsp.ui.layers;

import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import be.tarsos.dsp.ui.LinkedPanel;



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
