package be.hogent.tarsos.dsp.example.visualisation.layers;

import java.awt.Color;
import java.awt.Graphics2D;

import be.hogent.tarsos.dsp.example.visualisation.Axis;
import be.hogent.tarsos.dsp.example.visualisation.CoordinateSystem;

public class BackgroundLayer implements Layer{

	private final CoordinateSystem cs;
	private final Color color;

	public BackgroundLayer(final CoordinateSystem cs){
		this(cs,Color.WHITE);
	}
	
	public BackgroundLayer(final CoordinateSystem cs, Color color){
		this.cs=cs;
		this.color = color;
	}
	
	public void draw(final Graphics2D graphics) {
		graphics.setColor(color);		
		graphics.fillRect(
				Math.round(cs.getMin(Axis.X)), 
				Math.round(cs.getMin(Axis.Y)), 
				Math.round(cs.getDelta(Axis.X)), 
				Math.round(cs.getDelta(Axis.Y)));
	}

	public String getName() {
		return "Background layer";
	}
}
