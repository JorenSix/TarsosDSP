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


package be.tarsos.dsp.example;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;

import javax.swing.JPanel;

import be.tarsos.dsp.util.PitchConverter;

public class UtterAsteriskPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5330666476785715988L;
	private double patternLength;//in seconds
	private double currentMarker;
	private long lastReset;
	private int score;
	private double patternLengthInQuarterNotes;
	
	private static final double CENTS_DEVIATION = 30.0;
	
	double[] pattern={400,400,600,400,900,800,400,400,600,400,1100,900}; // in cents
	double[] timing ={3  ,1  ,4  ,4  ,4  ,6  ,3  ,1  ,4  ,4  ,4  ,6   }; //in eight notes
	
	ArrayList<Double> startTimeStamps;
	ArrayList<Double> pitches;
	
	public UtterAsteriskPanel(){
		for(double timeInQuarterNotes : timing){
			patternLengthInQuarterNotes+=timeInQuarterNotes;
		}
		patternLength = 12;
		currentMarker = 0;
		startTimeStamps = new ArrayList<Double>();
		pitches = new ArrayList<Double>();
	}
	
	@Override
	public void paint(final Graphics g) {
		final Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		graphics.setBackground(Color.WHITE);
		graphics.clearRect(0, 0, getWidth(), getHeight());
		int x = (int) (currentMarker / (float) patternLength * getWidth());
	
		if(x < 3 && System.currentTimeMillis() - lastReset > 1000){
			lastReset = System.currentTimeMillis();
			score();
			pitches.clear();
			startTimeStamps.clear();
		}
		graphics.drawLine(x, 0, x, getHeight());
		
		if(lastReset != 0){
			graphics.drawString("Score: " + String.valueOf(score), getWidth()/2, 20);
		}
		
		
		graphics.setColor(Color.GRAY);
		double lengthPerQuarterNote = patternLength/patternLengthInQuarterNotes; // in seconds per quarter note
		double currentXPosition = 0.5; // seconds of pause before start
		for(int i = 0 ; i < pattern.length ; i++){
			double lengthInSeconds = timing[i] * lengthPerQuarterNote;//seconds
			int patternWidth = (int) ( lengthInSeconds / (double) patternLength * getWidth());//pixels
			int patternHeight = (int) (CENTS_DEVIATION / 1200.0 * getHeight());
			int patternX = (int) ( (currentXPosition) / (double) patternLength * getWidth());
			int patternY = getHeight() - (int) (pattern[i] / 1200.0 * getHeight()) - patternHeight/2 ;
			graphics.drawRect(patternX, patternY, patternWidth, patternHeight);
			currentXPosition += lengthInSeconds; //in seconds
		}
		
		graphics.setColor(Color.RED);
		for(int i = 0 ; i < pitches.size() ; i++){
			double pitchInCents = pitches.get(i);
			double startTimeStamp = startTimeStamps.get(i) % patternLength;
			int patternX = (int) ( startTimeStamp / (double) patternLength * getWidth());
			int patternY = getHeight() - (int) (pitchInCents / 1200.0 * getHeight());
			graphics.drawRect(patternX, patternY, 2,2);
		}
	}
	
	private void score(){
		score = 0;
		for(int i = 0 ; i < pitches.size() ; i++){
			double pitchInCents = pitches.get(i);
			double startTimeStamp = startTimeStamps.get(i) % patternLength;
			if(startTimeStamp > 0.5 && startTimeStamp <= 0.5 + 0.5 * pattern.length){
				double lengthPerQuarterNote = patternLength/patternLengthInQuarterNotes; // in seconds per quarter note
				double currentXPosition = 0.5; // seconds of pause before start
				for(int j = 0 ; j < pattern.length ; j++){
					double lengthInSeconds = timing[j] * lengthPerQuarterNote;//seconds
					if(startTimeStamp > currentXPosition && startTimeStamp <= currentXPosition + lengthInSeconds && Math.abs(pitchInCents-pattern[j]) < CENTS_DEVIATION){
						score++;
					}
					currentXPosition += lengthInSeconds; //in seconds
				}
			}
		}
	}
	
	public void setMarker(double timeStamp,double frequency){
		currentMarker = timeStamp % patternLength;
		//ignore everything outside 80-2000Hz
		if(frequency > 80 && frequency < 2000){
			double pitchInCents = PitchConverter.hertzToRelativeCent(frequency);
			pitches.add(pitchInCents);
			startTimeStamps.add(timeStamp);
		}
		this.repaint();
	}
}
