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

package be.tarsos.dsp.ui.layers.pch;

import java.awt.Graphics2D;
import java.util.Random;

import be.tarsos.dsp.ui.layers.Layer;

public class PitchClassHistogramLayer implements Layer{

	int[] pch;
	
	public PitchClassHistogramLayer(){
		pch = new int[1200];
		Random r = new Random();
		for(int i = 0 ; i <pch.length ;i++){
			pch[i] = r.nextInt(1000);
		}
	}
			
	@Override
	public void draw(Graphics2D graphics) {
		
	}
	
	

	@Override
	public String getName() {
		return "Pitch Class Histogram Layer";
	}

}
