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
