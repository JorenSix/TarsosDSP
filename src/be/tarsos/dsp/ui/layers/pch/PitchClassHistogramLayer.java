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
