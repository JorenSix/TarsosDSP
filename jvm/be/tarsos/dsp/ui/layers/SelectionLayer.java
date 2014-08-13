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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;


/**
 * Draws the current selection.
 */
public class SelectionLayer extends MouseAdapter implements Layer{
	
	private final CoordinateSystem cs;
	private final Color color;

	public SelectionLayer(final CoordinateSystem cs){
		this(cs,Color.ORANGE);
	}
	
	public SelectionLayer(final CoordinateSystem cs, Color color){
		this.cs=cs;
		this.color = color;
	}		
	

	@Override
	public void draw(Graphics2D graphics) {
		double startX,startY,endX,endY;
		startX = cs.getStartX();
		startY = cs.getStartY();
		endX = cs.getEndX();
		endY = cs.getEndY();
		
		if(startX != Double.MAX_VALUE){
			if(startX>endX){
				double temp = startX;
				startX = endX;
				endX = temp;
			}
			if(startY>endY){
				double temp = startY;
				startY = endY;
				endY = temp;
			}
			int x = (int) Math.round(startX);
			int y = (int) Math.round(startY);
			int width = ((int) Math.round(endX)) - x;
			int height = ((int) Math.round(endY)) - y;
			
			graphics.setColor(color);
			graphics.drawRect(x, y, width, height);
		}
	}

	@Override
	public String getName() {
		return "Selection Layer";
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if(SwingUtilities.isLeftMouseButton(e)){
			LinkedPanel panel = (LinkedPanel) e.getComponent();
			Graphics2D graphics = (Graphics2D) panel.getGraphics();
			graphics.setTransform(panel.getTransform());
			Point2D units = LayerUtilities.pixelsToUnits(graphics,e.getX(), (int) e.getY());
			if(!panel.getCoordinateSystem().hasStartPoint()){
				panel.getCoordinateSystem().setStartPoint(units.getX(), units.getY());
			} else {
				panel.getCoordinateSystem().setEndPoint(units.getX(), units.getY());
			}
			panel.repaint();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(SwingUtilities.isLeftMouseButton(e)){
			LinkedPanel panel = (LinkedPanel) e.getComponent();
			panel.getViewPort().zoomToSelection();
			panel.invalidate();
		}
		
	}
}
