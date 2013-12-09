package be.hogent.tarsos.dsp.ui.layers;

import java.awt.Color;
import java.awt.Graphics2D;

import be.hogent.tarsos.dsp.ui.Axis;
import be.hogent.tarsos.dsp.ui.CoordinateSystem;


public class AmplitudeAxisLayer implements Layer {
	
	CoordinateSystem cs;
	
	public AmplitudeAxisLayer(CoordinateSystem cs) {
		this.cs = cs;
	}
	
	public void draw(Graphics2D graphics) {
		// draw legend
		graphics.setColor(Color.black);
		
		int minX = Math.round(cs.getMin(Axis.X));
		
		int lineWidthFourPixels = Math.round(LayerUtilities.pixelsToUnits(graphics, 4, true));
		int textOffset = Math.round(LayerUtilities.pixelsToUnits(graphics, 12, true));
		int lineWidthTwoPixels = Math.round(LayerUtilities.pixelsToUnits(graphics, 2, true));
	
		for (int i = (int) cs.getMin(Axis.Y); i < cs.getMax(Axis.Y); i+=1) {
			if (i % 1000 == 0) {
				graphics.drawLine(minX, i, minX + lineWidthFourPixels, i);
				String text = String.valueOf(i/1000);
				LayerUtilities.drawString(graphics, text, minX + textOffset, i, true, true,null);
			} else if (i%100 == 0) {
				graphics.drawLine(minX, i, minX + lineWidthTwoPixels, i);				
			}
		}
	}


	public String getName() {
		return "Amplitude Axis";
	}
}
