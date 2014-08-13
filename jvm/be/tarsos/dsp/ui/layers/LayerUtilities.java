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
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class LayerUtilities {
	
	private LayerUtilities(){
		
	}
	
	/**
	 * Transforms pixels to time and frequency.
	 * @param g The current graphics, with a meaningful transform applied to it.
	 * @param x The x coordinate, in pixels.
	 * @param y The y coordinate, in pixels.
	 * @return A point with time (in milliseconds) as x coordinate, and frequency (in cents) as y coordinate.
	 */
	public static Point2D pixelsToUnits(Graphics2D g,int x,int y){
		Point2D units = null;
		try {
			units = g.getTransform().inverseTransform(new Point2D.Double(x,y), null);
		} catch (NoninvertibleTransformException e1) {
			e1.printStackTrace();
		}
		return units;
	}
	
	/**
	 * Transforms a number of pixels into a corresponding time or frequency span. E.g. 10 horizontal
	 * pixels could translate to 320 milliseconds. 10 vertical pixels could translate to 32cents.
	 * @param g The current graphics, with a meaningful transform applied to it.
	 * @param pixels The number of pixels
	 * @param horizontal Is it the horizontal or vertical axis?
	 * @return A number of cents or milliseconds.
	 */
	public static float pixelsToUnits(Graphics2D g,int pixels,boolean horizontal){
		float numberOfUnits=0;
		try {
			Point2D originSrc = new Point2D.Double(0,0);
			Point2D originDest;
			originDest = g.getTransform().inverseTransform(originSrc, null);
			Point2D destSrc =  new Point2D.Double(pixels,pixels);
			Point2D destDest;
			destDest = g.getTransform().inverseTransform(destSrc, null);
			if(horizontal){		
				numberOfUnits = (float) (destDest.getX() - originDest.getX());
			}else{
				numberOfUnits = (float) (- destDest.getY() + originDest.getY());
			}
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		return numberOfUnits;
	}
	
	public static float unitsToPixels(Graphics2D g, float units, boolean horizontal){
		Point2D firstSource = new Point2D.Float(units,units);
		Point2D firstDest = new Point2D.Float(0, 0);
		
		Point2D secondSource = new Point2D.Float(0,0);
		Point2D secondDest = new Point2D.Float(0, 0);
		
		g.getTransform().transform(firstSource, firstDest);
		g.getTransform().transform(secondSource, secondDest);
		
		if(horizontal)
			return (float) (firstDest.getX()-secondDest.getX());
		else
			return (float) (firstDest.getY()-secondDest.getY());	
	}
	
	
	public static Rectangle2D drawString(Graphics2D graphics, String text, double x, double y,boolean centerHorizontal,boolean centerVertical,Color backgroundColor){
		return drawString(graphics, text, x, y,centerHorizontal, centerVertical, backgroundColor,Color.BLACK);
	}
	
	public static Rectangle2D drawString(Graphics2D graphics, String text, double x, double y,boolean centerHorizontal,boolean centerVertical,Color backgroundColor,Color textColor){
		AffineTransform transform = graphics.getTransform();
		Point2D source = new Point2D.Double(x,y);
		Point2D destination = new Point2D.Double();
		transform.transform(source, destination);
		try {
			transform.invert();
		} catch (NoninvertibleTransformException e1) {
			e1.printStackTrace();
		}
		graphics.transform(transform);
		Rectangle2D r = graphics.getFontMetrics().getStringBounds(text, graphics);
		int xPosition = Math.round((float) (destination.getX() - (centerHorizontal ? r.getWidth()/2.0f - 1 : 0) ));
		int yPosition = Math.round((float) (destination.getY() + (centerVertical ? r.getHeight() /2.0f - 1.5 : 0) ));
		
		if(backgroundColor != null){
			graphics.setColor(backgroundColor);
			int width = (int) (r.getMaxY()-r.getMinY());
			int height = (int) (r.getMaxX()-r.getMinX());
			graphics.fillRect(xPosition,yPosition-width,height,width);
		}
		
		
		Rectangle2D boundingRectangle = new Rectangle2D.Double(xPosition,yPosition - r.getHeight(),r.getWidth(),r.getHeight());
		transform.createTransformedShape(boundingRectangle);
		
		graphics.setColor(textColor);		
		graphics.drawString(text,xPosition,yPosition);
		
		try {
			transform.invert();
		} catch (NoninvertibleTransformException e1) {
			e1.printStackTrace();
		}
		graphics.transform(transform);
		return boundingRectangle;
	}
}
