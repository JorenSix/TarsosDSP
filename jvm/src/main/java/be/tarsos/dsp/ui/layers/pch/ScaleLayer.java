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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.Arrays;

import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.LayerUtilities;


public class ScaleLayer extends MouseAdapter implements Layer, MouseMotionListener, KeyListener {

	private double movingElement = -1.0;
	private double[] scale;
	private final CoordinateSystem cs;
	private final boolean enableEditor; 
	
	public ScaleLayer(CoordinateSystem cs,boolean enableEditor) {
		this.cs = cs;
		double[] scale = {0,100,200,400,1000,1100};
		this.scale = scale;
		this.enableEditor = enableEditor;
	}
	
	@Override
	public String getName() {
		return "Scale Editor Layer";
	}
	
	public void setScale(double[] newScale){
		scale = newScale;
	}

	@Override
	public void draw(Graphics2D graphics) {
		//draw legend
		graphics.setColor(Color.black);
		
		int minY = Math.round(cs.getMin(Axis.Y));
		int maxY = Math.round(cs.getMax(Axis.Y));
		int maxX = Math.round(cs.getMax(Axis.X));
	
		//int markerheightOffset = Math.round(LayerUtilities.pixelsToUnits(graphics, 15, false));
		int textOffset = Math.round(LayerUtilities.pixelsToUnits(graphics, 20, false));
		
		
		for (int i = (int) cs.getMin(Axis.X); i < cs.getMax(Axis.X); i++) {
			int realValue = (int) cs.getRealXValue(i);
			for(double scaleEntry : scale){
				if(realValue == (int) scaleEntry){
					if(scaleEntry == movingElement){
						graphics.setColor(Color.RED);
					}else{
						if(enableEditor){
							graphics.setColor(Color.GRAY);
						}else{
							graphics.setColor(Color.LIGHT_GRAY);
						}
					}
					graphics.drawLine(i, minY + (int) (1.5 * textOffset) , i, maxY - (int) (1.5 * textOffset) );
					String text = String.valueOf(realValue);
					if(enableEditor){
						LayerUtilities.drawString(graphics, text, i, minY + textOffset, true, false,null);
					}else{
						LayerUtilities.drawString(graphics, text, i, maxY - textOffset, true, false,null);
					}
					
					
				}
			}
		}
		
		int axisLabelOffset = Math.round(LayerUtilities.pixelsToUnits(graphics, 60, true));
		textOffset = Math.round(LayerUtilities.pixelsToUnits(graphics, 10, false));
		LayerUtilities.drawString(graphics,"Frequency (cents)",maxX-axisLabelOffset,maxY - textOffset,true,true,Color.white);

	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		if(!enableEditor){
			return;
		}
		
		if (movingElement != -1.0) {
			Arrays.sort(scale);
		}
		movingElement = -1.0;
		e.getComponent().repaint();
	}
	
	public void mouseMoved(MouseEvent e) {
		if(!enableEditor){
			return;
		}
		e.getComponent().requestFocus();
		if (e.isAltDown() || e.isAltGraphDown()) {
			//request focus for the key listener to work...
			e.getComponent().requestFocus();
			// add new element
			if (movingElement != -1.0) {
				int index = -1;
				for (int i = 0; i < scale.length; i++) {
					if (scale[i] == movingElement) {
						index = i;
					}
				}
				if (index == -1) {
					movingElement = -1.0;
				} else {
					scale[index] = getCents(e);
					movingElement = scale[index];
				}
				e.getComponent().repaint();
			} else {
				double[] newScale = new double[scale.length + 1];
				for (int i = 0; i < scale.length; i++) {
					newScale[i] = scale[i];
				}
				
				newScale[newScale.length - 1] = getCents(e);
				movingElement = newScale[newScale.length - 1];
				Arrays.sort(newScale);
				scale = newScale;
				e.getComponent().repaint();
			}
		} else if (e.isControlDown() && scale.length > 0) {
			//request focus for the key listener to work...
			
			// move the closest element
			if (movingElement == -1.0) {
				int index = closestIndex(getCents(e));
				movingElement = scale[index];
			}
			for (int i = 0; i < scale.length; i++) {
				if (scale[i] == movingElement) {
					scale[i] = getCents(e);
					movingElement = scale[i];
				}
			}
			e.getComponent().repaint();
		}
	}
	
	private double getCents(MouseEvent e){
		LinkedPanel panel = (LinkedPanel) e.getComponent();
		Graphics2D graphics = (Graphics2D) panel.getGraphics();
		graphics.setTransform(panel.getTransform());
		Point2D unitsCurrent = LayerUtilities.pixelsToUnits(graphics,e.getX(),e.getY());
		
		return cs.getRealXValue((float) unitsCurrent.getX());
	}

	private int closestIndex(double key) {
		double distance = Double.MAX_VALUE;
		int index = -1;
		for (int i = 0; i < scale.length; i++) {
			double currentDistance = Math.abs(key - scale[i]);
			double wrappedDistance = Math.abs(key - (scale[i] + 1200));
			if (Math.min(currentDistance, wrappedDistance) < distance) {
				distance = Math.min(currentDistance, wrappedDistance);
				index = i;
			}
		}
		return index;
	}

	

	@Override
	public void keyTyped(KeyEvent e) {
		if(!enableEditor){
			return;
		}
		boolean elementSelected = movingElement != -1.0;
		boolean deleteKeyPressed = (e.getKeyChar() == 'd' || e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyChar() == 127 );
		if( elementSelected && deleteKeyPressed){
			double[] newScale = new double[scale.length-1];
			int j = 0;
			for (int i = 0; i < scale.length;i++) {
				if (scale[i] != movingElement) {
					newScale[j] = scale[i];  
					j++;
				}
			}
			Arrays.sort(newScale);
			scale = newScale;
			movingElement = -1.0;
			e.getComponent().repaint();
		}
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}
}
