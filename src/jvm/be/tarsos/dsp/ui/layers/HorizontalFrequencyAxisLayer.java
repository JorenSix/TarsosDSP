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
import be.tarsos.dsp.ui.CoordinateSystem;

public class HorizontalFrequencyAxisLayer implements Layer{
	CoordinateSystem cs;
	
	public HorizontalFrequencyAxisLayer(CoordinateSystem cs) {
		this.cs = cs;
	}

	public void draw(Graphics2D graphics){
		
		//draw legend
		graphics.setColor(Color.black);
		int minY = Math.round(cs.getMin(Axis.Y)+1);
		int minX = Math.round(cs.getMin(Axis.X));
		int maxX = Math.round(cs.getMax(Axis.X));
		
		int wideMarkHeight = Math.round(LayerUtilities.pixelsToUnits(graphics,8, false));
		int smallMarkHeight = Math.round(LayerUtilities.pixelsToUnits(graphics,6, false));
		int verySmallMarkHeight = Math.round(LayerUtilities.pixelsToUnits(graphics,2, false));
		int textOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,12, false));	
		int textLabelOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,120, true));
		
		float widthOf100CentsInPixels = LayerUtilities.unitsToPixels(graphics, 100, true);
		
		//Every 100 and 1200 cents
		for(int i = (int) cs.getMin(Axis.X) ; i < cs.getMax(Axis.X) ; i++){
			if(i%1200 == 0){
				graphics.drawLine(i,minY,i,minY+wideMarkHeight);
				String text = String.valueOf(i);			
				LayerUtilities.drawString(graphics,text,i,minY+textOffset,true,false,null);
			} else if(i%600 == 0){			
				graphics.drawLine(i, minY, i,minY+smallMarkHeight);
			} else if(widthOf100CentsInPixels > 10 && i%100 == 0){
				graphics.drawLine(i, minY, i,minY+verySmallMarkHeight);
			}
		}
		
		graphics.drawLine(minX, minY, maxX, minY);
		
		LayerUtilities.drawString(graphics,"Frequency (cents)",maxX-textLabelOffset,minY+(4*wideMarkHeight),false,true,Color.white);
		
		//LayerUtilities.drawString(graphics,"Frequency (cents)",minX+textOffset,maxY-textLabelOffset,false,true,Color.white);
	}

	@Override
	public String getName() {
		return "Frequency Axis";
	}
}
