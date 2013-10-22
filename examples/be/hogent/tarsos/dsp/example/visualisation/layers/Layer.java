package be.hogent.tarsos.dsp.example.visualisation.layers;

import java.awt.Graphics2D;

public interface Layer {
	
	public void draw(Graphics2D graphics);
	
	public String getName();
}
