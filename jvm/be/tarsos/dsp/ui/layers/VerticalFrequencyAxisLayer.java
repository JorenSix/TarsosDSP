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

public class VerticalFrequencyAxisLayer implements Layer{
	CoordinateSystem cs;
	
	public VerticalFrequencyAxisLayer(CoordinateSystem cs) {
		this.cs = cs;
	}

	public void draw(Graphics2D graphics){
		
		//draw legend
		graphics.setColor(Color.black);
		int minX = Math.round(cs.getMin(Axis.X));
		int maxY = Math.round(cs.getMax(Axis.Y));
		
		int wideMarkWidth = Math.round(LayerUtilities.pixelsToUnits(graphics,8, true));
		int smallMarkWidth = Math.round(LayerUtilities.pixelsToUnits(graphics,4, true));
		int textOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,12, true));	
		int textLabelOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,12, false));
		
		//Every 100 and 1200 cents
		for(int i = (int) cs.getMin(Axis.Y) ; i < cs.getMax(Axis.Y) ; i++){
			if(i%1200 == 0){
				graphics.drawLine(minX, i, minX+wideMarkWidth,i);
				String text = String.valueOf(i);				
				LayerUtilities.drawString(graphics,text,minX+textOffset,i,false,true,null);
			} else if(i%100 == 0){			
				graphics.drawLine(minX, i, minX+smallMarkWidth,i);
			}
		}
		
		LayerUtilities.drawString(graphics,"Frequency (cents)",minX+textOffset,maxY-textLabelOffset,false,true,Color.white);
	}

	@Override
	public String getName() {
		return "Frequency Axis";
	}
}
