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


public class TimeAxisLayer implements Layer {

	private int[] intervals = { 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000};
	private int intervalIndex;
	CoordinateSystem cs;
	public TimeAxisLayer(CoordinateSystem cs) {
		this.cs = cs;
	}

	public void draw(Graphics2D graphics) {
		if(cs.getUnitsForAxis(Axis.X) == AxisUnit.TIME){
			// draw legend
			graphics.setColor(Color.black);
			// every second
			int minY = Math.round(cs.getMin(Axis.Y));
			int maxX = Math.round(cs.getMax(Axis.X));
			
			//float deltaX = cs.getDelta(Axis.X); //Breedte in milisec.
			int beginDrawInterval = 1000;
			intervalIndex = 0;
			int smallDrawInterval = beginDrawInterval*intervals[intervalIndex];
		
			int markerHeight = Math.round(LayerUtilities.pixelsToUnits(graphics, 9, false));
			int textOffset = Math.round(LayerUtilities.pixelsToUnits(graphics, 12, false));
			
			int smallMarkerheight = Math.round(LayerUtilities.pixelsToUnits(graphics, 4, false));
			int smallTextOffset = Math.round(LayerUtilities.pixelsToUnits(graphics, 9, false));
			
			int smallestMarkerheight = Math.round(LayerUtilities.pixelsToUnits(graphics, 2, false));
			
			int minValue = (int) cs.getMin(Axis.X);
			int maxValue = (int) cs.getMax(Axis.X);
			int differenceInMs = maxValue - minValue;
		
		if(differenceInMs >= 240000){
			//only draw seconds
			for (int i = minValue; i < maxValue; i++) {
				if (i % (smallDrawInterval*60) == 0) {
					graphics.drawLine(i, minY, i, minY + markerHeight);
					String text = String.valueOf(i / 1000);
					LayerUtilities.drawString(graphics, text, i, minY + textOffset, true, false,null);
				}
			}
				
		} else if(differenceInMs >= 120000 && differenceInMs < 240000 ){
				//only draw seconds
				for (int i = minValue; i < maxValue; i++) {
					if (i % (smallDrawInterval*10) == 0) {
						graphics.drawLine(i, minY, i, minY + markerHeight);
						String text = String.valueOf(i / 1000);
						LayerUtilities.drawString(graphics, text, i, minY + textOffset, true, false,null);
					}
				}
			}else if(differenceInMs >= 30000 && differenceInMs < 120000 ){
				//only draw seconds
				for (int i = minValue; i < maxValue; i++) {
					if (i % (smallDrawInterval*5) == 0) {
						graphics.drawLine(i, minY, i, minY + markerHeight);
						String text = String.valueOf(i / 1000);
						LayerUtilities.drawString(graphics, text, i, minY + textOffset, true, false,null);
					} else if (i % smallDrawInterval == 0) {
						graphics.drawLine(i, minY, i, minY + smallMarkerheight);
						
					}
				}
			}else if(differenceInMs > 10000 && differenceInMs < 30000){
				//only draw seconds
				for (int i = minValue; i < maxValue; i++) {
					if (i % (smallDrawInterval*5) == 0) {
						graphics.drawLine(i, minY, i, minY + markerHeight);
						String text = String.valueOf(i / 1000);
						LayerUtilities.drawString(graphics, text, i, minY + textOffset, true, false,null);
					} else if (i % smallDrawInterval == 0) {
						graphics.drawLine(i, minY, i, minY + smallMarkerheight);
						String text = String.valueOf(i / 1000);
						LayerUtilities.drawString(graphics, text, i, minY + smallTextOffset, true, false,null);
					}
				}
			}else{
				//also draw 0.1s
				for (int i = minValue; i < maxValue; i++) {
					if (i % (smallDrawInterval*5) == 0) {
						graphics.drawLine(i, minY, i, minY + markerHeight);
						String text = String.valueOf(i / 1000);
						LayerUtilities.drawString(graphics, text, i, minY + textOffset, true, false,null);
					} else if (i % smallDrawInterval == 0) {
						graphics.drawLine(i, minY, i, minY + smallMarkerheight);
						String text = String.valueOf(i / 1000);
						LayerUtilities.drawString(graphics, text, i, minY + smallTextOffset, true, false,null);
					} else if (i % 100 == 0) {
						graphics.drawLine(i, minY, i, minY + smallestMarkerheight);
					}
				}
			}
			
			int axisLabelOffset = Math.round(LayerUtilities.pixelsToUnits(graphics, 26, true));
			textOffset = Math.round(LayerUtilities.pixelsToUnits(graphics, 14, false));
			LayerUtilities.drawString(graphics,"Time (s)",maxX-axisLabelOffset,minY + textOffset,true,true,Color.white);	
		}
			
	}

	@Override
	public String getName() {
		return "Time axis";
	}
}
