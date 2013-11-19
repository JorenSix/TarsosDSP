package be.hogent.tarsos.dsp.example.visualisation.layers;

import java.awt.Color;
import java.awt.Graphics2D;

import be.hogent.tarsos.dsp.example.visualisation.CoordinateSystem;

/**
 * Draws the current selection.
 */
public class SelectionLayer implements Layer{
	
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
}
