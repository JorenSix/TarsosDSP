package be.hogent.tarsos.dsp.ui.layers;

import java.awt.Graphics2D;

public interface Layer {
	
	public void draw(Graphics2D graphics);
	
	public String getName();
}
