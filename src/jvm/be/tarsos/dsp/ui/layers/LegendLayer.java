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
import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;

/**
 * Adds a legend to the upper right corner of the map.
 * @author Joren Six
 */
public class LegendLayer  implements Layer {
	List<Color> colors;
	List<String> texts;
	CoordinateSystem cs;
	int pixelsFromRight;
	
	public LegendLayer(CoordinateSystem cs,int pixelsFromRight){
		this.cs = cs;
		colors = new ArrayList<Color>();
		texts = new ArrayList<String>();
		this.pixelsFromRight = pixelsFromRight;
	}

	@Override
	public void draw(Graphics2D graphics) {
		int maxX = Math.round(cs.getMax(Axis.X));
		int maxY = Math.round(cs.getMax(Axis.Y));
		
		for(int i = 0;i < colors.size(); i++){
			String text = texts.get(i);
			Color color = colors.get(i);
			int textYOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,14, false));
			int textXOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,pixelsFromRight, true));
			LayerUtilities.drawString(graphics,text,maxX-textXOffset,maxY-textYOffset*(i+1),false,true,Color.white,color);	
		}
	}

	@Override
	public String getName() {
		return "Legend";
	}

	public void addEntry(String string, Color blue) {
		colors.add(blue);
		texts.add(string);		
	}
	
	public void removeAllEntries(){
		colors.clear();
		texts.clear();
	}

}
