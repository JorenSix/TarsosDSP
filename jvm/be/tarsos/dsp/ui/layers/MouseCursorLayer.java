package be.tarsos.dsp.ui.layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;

public class MouseCursorLayer implements Layer, MouseMotionListener, MouseListener {
	
	private boolean drawCursor = false;
	private Point lastPoint = null;
	private Component component = null;
	
	boolean onlyDrawVertical = false;
	
	CoordinateSystem cs;
	public MouseCursorLayer(CoordinateSystem cs){
		this.cs=cs;
	}
	
	@Override
	public void draw(Graphics2D graphics) {
		if(drawCursor){
			Point2D unitPoint = LayerUtilities.pixelsToUnits(graphics, (int) lastPoint.getX(), (int)lastPoint.getY() );
			graphics.setColor(Color.blue);
			if(!onlyDrawVertical){
				graphics.drawLine(Math.round(cs.getMax(Axis.X)),Math.round((float) unitPoint.getY()), Math.round(cs.getMin(Axis.X)), Math.round((float) unitPoint.getY()));
				//notify listeners of change
				this.pcs.firePropertyChange("cursor", null, lastPoint);
			}
			graphics.drawLine(Math.round((float) unitPoint.getX()),(int)Math.floor(cs.getMin(Axis.Y)),Math.round((float) unitPoint.getX()),(int) Math.ceil(cs.getMax(Axis.Y)));
		}
	}
	

	@Override
	public String getName() {
		return "Cursor Layer";
	}

	@Override
	public void mouseDragged(MouseEvent e) {

	}

	@Override
	public void mouseMoved(MouseEvent e) {
		lastPoint = e.getPoint();
		component = e.getComponent();
		component.repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		drawCursor = false;
		component = e.getComponent();
		component.repaint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		lastPoint = e.getPoint();
		drawCursor = true;
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		lastPoint = e.getPoint();
		drawCursor = true;
		onlyDrawVertical = false;
	}

	@Override
	public void mouseExited(MouseEvent e) {
		drawCursor = false;
		component = e.getComponent();
		component.repaint();
	}
	
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

	public void setPoint(Point newPosition) {
		drawCursor = true;
		onlyDrawVertical = true;
		lastPoint = newPosition;
		if(component!=null)
			component.repaint();
	}    

}
