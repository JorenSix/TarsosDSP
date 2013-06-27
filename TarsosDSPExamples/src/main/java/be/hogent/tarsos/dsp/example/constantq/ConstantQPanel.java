/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -----------------------------------------------------------
*
*  TarsosDSP is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*  https://github.com/JorenSix/TarsosDSP
*  http://tarsos.0110.be/releases/TarsosDSP/
* 
*/
package be.hogent.tarsos.dsp.example.constantq;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.JPanel;

import be.hogent.tarsos.dsp.ConstantQ;
import be.hogent.tarsos.dsp.example.PitchConverter;

public class ConstantQPanel extends JPanel implements ComponentListener {
	
	private BufferedImage bufferedImage;
	private Graphics2D bufferedGraphics;
	
	private int position;
	private float[] maxMagnitude;
	private int maxPosition;
	    
	public ConstantQPanel(){
		bufferedImage = new BufferedImage(640*4,480*4, BufferedImage.TYPE_INT_RGB);
		bufferedGraphics = bufferedImage.createGraphics();
		this.addComponentListener(this);
		maxMagnitude = new float[5];
		maxPosition = 0;
	}
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -7838113856967821918L;

	public void drawMagnitudes(ConstantQ constantQ) {
		float currentMax = 0;
		for(int i = 0 ; i < constantQ.getMagnitudes().length ; i++){
			currentMax = Math.max(constantQ.getMagnitudes()[i], currentMax);
		}
		maxMagnitude[maxPosition] = currentMax;
		maxPosition++;
		if(maxPosition == maxMagnitude.length){
			maxPosition=0;
		}
		float[] scratch = maxMagnitude.clone();
		Arrays.sort(scratch);
		currentMax = scratch[scratch.length - 1];
		
		int height = getHeight();
		// draw the pixels
		for (int i = 0; i < height; i++) {
			Color color = Color.black;
			int index = positionToFrequency(constantQ, i);
			if (currentMax != 0) {
				final int greyValue = (int) (Math.log1p(constantQ.getMagnitudes()[index]/ currentMax)/ Math.log1p(1.0000001) * 255);
				color = new Color(greyValue, greyValue, greyValue);
			}
			bufferedGraphics.setColor(color);
			bufferedGraphics.fillRect(position, height - i, 3, 1);
		}
		
	   
		bufferedGraphics.clearRect(0,0, 190,30);
        bufferedGraphics.setColor(Color.WHITE);
        for(int i = 100 ; i < 500; i += 100){
        	int bin = frequencyToPixel(constantQ,i);
			bufferedGraphics.drawLine(0, getHeight() -bin, 5, getHeight() -bin);
		}
		
        for(int i = 500 ; i <= 20000; i += 500){
			int bin = frequencyToPixel(constantQ,i);
			bufferedGraphics.drawLine(0, getHeight() - bin, 5, getHeight() -bin);
		}
        
        for(int i = 100 ; i <= 20000; i*=10){
			int bin = frequencyToPixel(constantQ,i);
			bufferedGraphics.drawString(String.valueOf(i), 10, getHeight() -bin);
		}
        
		repaint();
		position+=3;
		position = position % getWidth();
				
	}
	
	public void drawPitch(ConstantQ constantQ, double pitch) {
		if(pitch < 0){
			return;
		}
		double minPitch = PitchConverter.hertzToAbsoluteCent(constantQ.getFreqencies()[0]);
		double maxPitch = PitchConverter.hertzToAbsoluteCent(constantQ.getFreqencies()[constantQ.getFreqencies().length-1]);
		double pitchInCents = PitchConverter.hertzToAbsoluteCent(pitch);
		if (pitchInCents > minPitch && pitchInCents < maxPitch){
			Color color = new Color(200, 0, 0);
			bufferedGraphics.setColor(color);
			int i = frequencyToPixel(constantQ,pitch);
			bufferedGraphics.fillRect(position-3, getHeight() - i, 3, 1);
		}
	}
	
	private int frequencyToPixel(ConstantQ constantQ,double pitch) {
		double minPitch = PitchConverter.hertzToAbsoluteCent(constantQ.getFreqencies()[0]);
		double maxPitch = PitchConverter.hertzToAbsoluteCent(constantQ.getFreqencies()[constantQ.getFreqencies().length-1]);
		double pitchInCents = PitchConverter.hertzToAbsoluteCent(pitch);
		return (int) ((pitchInCents-minPitch)/(maxPitch - minPitch) * getHeight());
	}

	private int positionToFrequency(ConstantQ constantQ,int position){
		int index = 0;
		double percentage = position / (double) getHeight();
		double minPitch = PitchConverter.hertzToAbsoluteCent(constantQ.getFreqencies()[0]);
		double maxPitch = PitchConverter.hertzToAbsoluteCent(constantQ.getFreqencies()[constantQ.getFreqencies().length-1]);
		double pitchInCents = minPitch + (percentage * (maxPitch - minPitch) );
		for(int i = 0 ; i < constantQ.getFreqencies().length -1; i++){
			if(	pitchInCents >= PitchConverter.hertzToAbsoluteCent(constantQ.getFreqencies()[i])
				&&
				pitchInCents <= PitchConverter.hertzToAbsoluteCent(constantQ.getFreqencies()[i+1])	
			){
				index = i;
				System.out.println(i);
				break;
			}
		}
		return index;
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {

	}
	
	public void paintComponent(final Graphics g) {
        g.drawImage(bufferedImage, 0, 0, null);
    }
	

	@Override
	public void componentResized(ComponentEvent arg0) {
		bufferedImage = new BufferedImage(getWidth()*2,getHeight()*2, BufferedImage.TYPE_INT_RGB);
		bufferedGraphics = bufferedImage.createGraphics();
		position = 0;
	}

	@Override
	public void componentShown(ComponentEvent arg0) {

	}

	
	
}
