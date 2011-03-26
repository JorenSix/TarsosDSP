package be.hogent.tarsos.dsp.example;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;

import javax.swing.JPanel;

public class UtterAsteriksPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5330666476785715988L;
	private double patternLength;//in seconds
	private double currentMarker;
	private long lastReset;
	private int score;
	
	double[] pattern={200,400,600,200};
	// 0.5 1 1.5 2 2.5 
	
	ArrayList<Double> startTimeStamps;
	ArrayList<Double> pitches;
	
	public UtterAsteriksPanel(){
		patternLength = 5;
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
		
		
		graphics.setBackground(Color.GRAY);
		for(int i = 0 ; i < pattern.length ; i++){
			int patternWidth = (int) (0.5 / (double) patternLength * getWidth());//0.5 seconds
			int patternHeight = (int) (30.0 / 1200.0 * getHeight());
			int patternX = (int) ( (0.5 + 0.5 * i) / (double) patternLength * getWidth());
			int patternY = getHeight() - (int) (pattern[i] / 1200.0 * getHeight()) - patternHeight/2 ;
			graphics.drawRect(patternX, patternY, patternWidth, patternHeight);
		}
		
		graphics.setBackground(Color.RED);
		for(int i = 0 ; i < pitches.size() ; i++){
			double pitchInCents = pitches.get(i);
			double startTimeStamp = startTimeStamps.get(i) % patternLength;
			int patternX = (int) ( startTimeStamp / (double) patternLength * getWidth());
			int patternY = getHeight() - (int) (pitchInCents / 1200.0 * getHeight());
			graphics.drawRect(patternX, patternY, 3, 3);
		}
	}
	
	private void score(){
		score = 0;
		for(int i = 0 ; i < pitches.size() ; i++){
			double pitchInCents = pitches.get(i);
			double startTimeStamp = startTimeStamps.get(i) % patternLength;
			if(startTimeStamp > 0.5 && startTimeStamp <= 0.5 + 0.5 * pattern.length){
				for(int j = 0 ; j < pattern.length ; j++){
					if(startTimeStamp > 0.5 + j * 0.5 && startTimeStamp <= 0.5 + 0.5 * (j + 1) && Math.abs(pitchInCents-pattern[j]) < 30){
						score++;
					}
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
