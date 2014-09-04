package be.tarsos.dsp.ui.layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;

import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;

public class TooltipLayer implements Layer, MouseMotionListener,MouseListener{

	private boolean enableTooltip = false;
	private int millisecondsBeforAppearance = 1000;
	private long mouseStoppedAtMilliseconds = System.currentTimeMillis();
	private Point lastPoint = null;
	private Point lastDrawnPoint = null;
	private Component component = null;
	private final CoordinateSystem cs;
	
	private final TooltipTextGenerator tooltipTextGenerator;
	
	public interface TooltipTextGenerator {
		public String generateTooltip(CoordinateSystem cs,Point2D point);
	}
	
	private final static TooltipTextGenerator defaultTooltipGenerator = new TooltipTextGenerator() {
		@Override
		public String generateTooltip(CoordinateSystem cs, Point2D point) {
			return String.format("[%.03f%s , %.02f%s]", point.getX()/1000.0,cs.getUnitsForAxis(Axis.X).getUnit(),point.getY(),cs.getUnitsForAxis(Axis.Y).getUnit());
		}
	};
	
	public TooltipLayer(CoordinateSystem cs){
		this(cs,defaultTooltipGenerator);
	}
	
	public TooltipLayer(CoordinateSystem cs,TooltipTextGenerator tooltipTextGenerator){
		this.cs = cs;
		this.tooltipTextGenerator = tooltipTextGenerator;
		
		Thread checkToolTipNeededThread = new Thread(new Runnable(){
			private void sleep(){
				try {
					Thread.sleep(30);
				} catch (InterruptedException e) {
					//Ignore
				}
			}
			@Override
			public void run() {
				while(true){
					sleep();
					long diff = System.currentTimeMillis() - mouseStoppedAtMilliseconds;
					if(component!=null && diff > millisecondsBeforAppearance && lastDrawnPoint != lastPoint){
						component.repaint();
					}
				}
			}},"Tooltip Repaint Check");
		checkToolTipNeededThread.start();
	}
	
	
	@Override
	public void draw(Graphics2D graphics) {
		long diff = System.currentTimeMillis() - mouseStoppedAtMilliseconds;
		if(enableTooltip &&  diff > millisecondsBeforAppearance){
			Point2D unitPoint = LayerUtilities.pixelsToUnits(graphics, (int) lastPoint.getX(), (int) lastPoint.getY());
			
			int textYOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,10, false));
			int textXOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,10, true));
			
			String text = tooltipTextGenerator.generateTooltip(cs, unitPoint);
			
			LayerUtilities.drawString(graphics,text,unitPoint.getX() + textXOffset,unitPoint.getY()+textYOffset,false,true,Color.white,Color.black);
			lastDrawnPoint = lastPoint;
		}		
	}

	@Override
	public String getName() {
		return "Tooltip Layer";
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		enableTooltip = false;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		lastPoint = e.getPoint();
		component = e.getComponent();
		mouseStoppedAtMilliseconds = System.currentTimeMillis();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		enableTooltip = false;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		enableTooltip = true;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		lastPoint = e.getPoint();
		component = e.getComponent();
		enableTooltip = true;
	}

	@Override
	public void mouseExited(MouseEvent e) {
		enableTooltip = false;
	}
}
