package be.hogent.tarsos.dsp.example.visualisation.layers;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import be.hogent.tarsos.dsp.example.visualisation.Axis;
import be.hogent.tarsos.dsp.example.visualisation.AxisUnit;
import be.hogent.tarsos.dsp.example.visualisation.CoordinateSystem;
import be.hogent.tarsos.dsp.example.visualisation.LinkedPanel;

public class DragMouseListenerLayer extends MouseAdapter implements Layer {
	
	private final boolean onlyHorizontal;
	private Point previousPoint;
	
	public DragMouseListenerLayer(CoordinateSystem cs){
		onlyHorizontal =  (cs.getUnitsForAxis(Axis.Y) == AxisUnit.AMPLITUDE || cs.getUnitsForAxis(Axis.Y) == AxisUnit.NONE);
		previousPoint = null;
	}

	@Override
	public void draw(Graphics2D graphics) {
		//do nothing, only capture mouse events
	}

	@Override
	public String getName() {
		return "Listen to drag events.";
	}
	
	
	@Override
	public void mousePressed(MouseEvent e) {
		previousPoint = e.getPoint();
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(!SwingUtilities.isLeftMouseButton(e)){
			previousPoint = null;
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if(! SwingUtilities.isLeftMouseButton(e)  && previousPoint != null) {
			if(onlyHorizontal){
				dragHorizontally(e);
			}else{
				dragBoth(e);
			}
		}
	}
	
	private void dragBoth(MouseEvent e){
		LinkedPanel panel = (LinkedPanel) e.getComponent();
		Graphics2D graphics = (Graphics2D) panel.getGraphics();
		graphics.setTransform(panel.getTransform());
		Point2D unitsCurrent = LayerUtilities.pixelsToUnits(graphics,
				e.getX(), e.getY());
		Point2D unitsPrevious = LayerUtilities.pixelsToUnits(graphics,(int) previousPoint.getX(), (int) previousPoint.getY());
		float millisecondAmount = (float) (unitsPrevious.getX() - unitsCurrent.getX());
		float centAmount = (float) (unitsPrevious.getY() - unitsCurrent.getY());
		previousPoint = e.getPoint();
		panel.getViewPort().drag(millisecondAmount, centAmount);
		graphics.dispose();
	}
	
	private void dragHorizontally(MouseEvent e){
		LinkedPanel panel = (LinkedPanel) e.getComponent();
		Graphics2D graphics = (Graphics2D) panel.getGraphics();
		graphics.setTransform(panel.getTransform());
		Point2D unitsCurrent = LayerUtilities.pixelsToUnits(graphics,e.getX(), (int) previousPoint.getY());
		Point2D unitsPrevious = LayerUtilities.pixelsToUnits(graphics,(int) previousPoint.getX(), (int) previousPoint.getY());
		float millisecondAmount = (float) (unitsPrevious.getX() - unitsCurrent.getX());
		previousPoint = e.getPoint();
		panel.getViewPort().drag(millisecondAmount, 0);
		graphics.dispose();
	}
}
