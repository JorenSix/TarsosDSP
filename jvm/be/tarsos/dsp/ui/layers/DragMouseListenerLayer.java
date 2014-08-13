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
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;



public class DragMouseListenerLayer extends MouseAdapter implements Layer {
	
	private final boolean onlyHorizontal;
	private Point previousPoint;
	private final CoordinateSystem cs;
	
	public DragMouseListenerLayer(CoordinateSystem cs){
		onlyHorizontal =  (cs.isWrapping());
		previousPoint = null;
		this.cs = cs;
	}

	@Override
	public void draw(Graphics2D graphics) {
		//do nothing, only capture mouse events
	}

	@Override
	public String getName() {
		return "Listen to drag events.";
	}
	
	
	@Override
	public void mousePressed(MouseEvent e) {
		previousPoint = e.getPoint();
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(!SwingUtilities.isLeftMouseButton(e)){
			previousPoint = null;
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if(! SwingUtilities.isLeftMouseButton(e)  && previousPoint != null) {
			if(onlyHorizontal){
				dragHorizontally(e);
			}else{
				dragBoth(e);
			}
		}
	}
	
	private void dragBoth(MouseEvent e){
		LinkedPanel panel = (LinkedPanel) e.getComponent();
		Graphics2D graphics = (Graphics2D) panel.getGraphics();
		graphics.setTransform(panel.getTransform());
		Point2D unitsCurrent = LayerUtilities.pixelsToUnits(graphics,
				e.getX(), e.getY());
		Point2D unitsPrevious = LayerUtilities.pixelsToUnits(graphics,(int) previousPoint.getX(), (int) previousPoint.getY());
		float millisecondAmount = (float) (unitsPrevious.getX() - unitsCurrent.getX());
		float centAmount = (float) (unitsPrevious.getY() - unitsCurrent.getY());
		previousPoint = e.getPoint();
		panel.getViewPort().drag(millisecondAmount, centAmount);
		graphics.dispose();
	}
	
	private void dragHorizontally(MouseEvent e){
		LinkedPanel panel = (LinkedPanel) e.getComponent();
		Graphics2D graphics = (Graphics2D) panel.getGraphics();
		graphics.setTransform(panel.getTransform());
		Point2D unitsCurrent = LayerUtilities.pixelsToUnits(graphics,e.getX(), (int) previousPoint.getY());
		Point2D unitsPrevious = LayerUtilities.pixelsToUnits(graphics,(int) previousPoint.getX(), (int) previousPoint.getY());
		float millisecondAmount = (float) (unitsPrevious.getX() - unitsCurrent.getX());
		previousPoint = e.getPoint();
		if(cs.isWrapping()){
			cs.setWrappingOrigin(cs.getWrappingOrigin() + millisecondAmount);
			panel.getViewPort().drag(0, 0);
		}else{
			panel.getViewPort().drag(millisecondAmount, 0);
		}
		
		graphics.dispose();
	}
}
