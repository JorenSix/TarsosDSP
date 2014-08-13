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

import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.AxisUnit;
import be.tarsos.dsp.ui.CoordinateSystem;

public class AmplitudeAxisLayer implements Layer {
	
	CoordinateSystem cs;
	
	public AmplitudeAxisLayer(CoordinateSystem cs) {
		this.cs = cs;
	}
	
	public void draw(Graphics2D graphics) {
		if(cs.getUnitsForAxis(Axis.Y) == AxisUnit.AMPLITUDE){
			drawAmplitudeXAxis(graphics);
		}
	}
	
	public void drawAmplitudeXAxis(Graphics2D graphics){
		graphics.setColor(Color.black);
		
		int minX = Math.round(cs.getMin(Axis.X));
		int maxY = Math.round(cs.getMax(Axis.Y));
		
		int lineWidthFourPixels = Math.round(LayerUtilities.pixelsToUnits(graphics, 4, true));
		int textOffset = Math.round(LayerUtilities.pixelsToUnits(graphics, 14, true));
		int textLabelOffset = Math.round(LayerUtilities.pixelsToUnits(graphics, 20, false));
			int lineWidthTwoPixels = Math.round(LayerUtilities.pixelsToUnits(graphics, 2, true));
	
		for (int i = (int) cs.getMin(Axis.Y); i < cs.getMax(Axis.Y); i+=1) {
			if (i % 100 == 0) {
				graphics.drawLine(minX, i, minX + lineWidthFourPixels, i);
				String text = String.format("%.0f",i/10.0);
				LayerUtilities.drawString(graphics, text, minX +  textOffset, i, true, true,null);
			} else if (i%10 == 0) {
				graphics.drawLine(minX, i, minX + lineWidthTwoPixels, i);				
			}
		}
		graphics.drawLine(minX, 0, minX, maxY);
		LayerUtilities.drawString(graphics,"Amplitude (%)",minX+textOffset,maxY-textLabelOffset,false,true,Color.white);
	}

	public String getName() {
		return "Amplitude Axis";
	}
}
