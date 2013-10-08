package be.hogent.tarsos.dsp.example.visualisation;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import be.hogent.tarsos.dsp.example.visualisation.layers.Layer;
import be.hogent.tarsos.dsp.example.visualisation.layers.LayerUtilities;


public class LinkedPanel extends JPanel {

	private static final long serialVersionUID = -5055686566048886896L;

	private List<Layer> layers;

	private final ViewPort viewPort;
	private CoordinateSystem cs;

	public CoordinateSystem getCoordinateSystem() {
		return cs;
	}

	public void setCoordinateSystem(CoordinateSystem cs) {
		this.cs = cs;
	}

	public ViewPort getViewPort() {
		return viewPort;
	}

	public LinkedPanel(CoordinateSystem coordinateSystem) {
		super();
		layers = new ArrayList<Layer>();
		setCoordinateSystem(coordinateSystem);
		viewPort = new ViewPort(this.cs);
		DragListener dragListener;
		if (cs.getUnitsForAxis(Axis.Y) == AxisUnit.AMPLITUDE || cs.getUnitsForAxis(Axis.Y) == AxisUnit.NONE){
			dragListener = new HorizontalDragListener(this);
		} else {
			dragListener = new DragListener(this);
		}
		ZoomListener zoomListener = new ZoomListener();
		addMouseWheelListener(zoomListener);
		addMouseListener(dragListener);
		addMouseMotionListener(dragListener);
		this.setVisible(true);
	}

	public void addLayer(Layer l) {
		this.layers.add(l);		
	}
	
	public void removeLayers(){
		this.layers.clear();
	}

	private class ZoomListener implements MouseWheelListener {

		public void mouseWheelMoved(MouseWheelEvent arg0) {
			int amount = arg0.getWheelRotation() * arg0.getScrollAmount();
			viewPort.zoom(amount, arg0.getPoint());
		}
	}

	private class HorizontalDragListener extends DragListener {
		private HorizontalDragListener(LinkedPanel p){
			super(p);
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			if (previousPoint != null) {
				Graphics2D graphics = (Graphics2D) panel.getGraphics();
				graphics.setTransform(panel.getTransform());
				Point2D unitsCurrent = LayerUtilities.pixelsToUnits(graphics,
						e.getX(), (int) previousPoint.getY());
				Point2D unitsPrevious = LayerUtilities.pixelsToUnits(graphics,
						(int) previousPoint.getX(), (int) previousPoint.getY());
				float millisecondAmount = (float) (unitsPrevious.getX() - unitsCurrent
						.getX());
				previousPoint = e.getPoint();
				viewPort.drag(millisecondAmount, 0);
			}
		}
	}
	
	private class DragListener extends MouseAdapter {

		LinkedPanel panel;
		Point previousPoint;

		private DragListener(LinkedPanel p) {
			panel = p;
			previousPoint = null;
		}
		
		@Override 
		public void mouseClicked(MouseEvent e){
	
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			previousPoint = e.getPoint();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			previousPoint = null;
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (previousPoint != null) {
				Graphics2D graphics = (Graphics2D) panel.getGraphics();
				graphics.setTransform(panel.getTransform());
				Point2D unitsCurrent = LayerUtilities.pixelsToUnits(graphics,
						e.getX(), e.getY());
				Point2D unitsPrevious = LayerUtilities.pixelsToUnits(graphics,
						(int) previousPoint.getX(), (int) previousPoint.getY());
				float millisecondAmount = (float) (unitsPrevious.getX() - unitsCurrent
						.getX());
				float centAmount = (float) (unitsPrevious.getY() - unitsCurrent
						.getY());
				previousPoint = e.getPoint();
				viewPort.drag(millisecondAmount, centAmount);
				graphics.dispose();
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
		
		}
	}

	private AffineTransform getTransform() {
		double xDelta = cs.getDelta(Axis.X);
		double yDelta = cs.getDelta(Axis.Y);
		AffineTransform transform = new AffineTransform();
		transform.translate(0, getHeight());
		transform.scale(getWidth() / xDelta, -getHeight() / yDelta);
		transform.translate(-cs.getMin(Axis.X),-cs.getMin(Axis.Y));
		return transform;
	}


	@Override
	public void paintComponent(final Graphics g) {
		super.paintComponent(g);
		final Graphics2D graphics = (Graphics2D) g.create();
		graphics.setTransform(this.updateTransform(graphics.getTransform()));
		if (!layers.isEmpty()) {
			for (Layer layer : layers) {
				layer.draw(graphics);
			}
		}
	}

	public AffineTransform updateTransform(AffineTransform transform) {
		double xDelta = cs.getDelta(Axis.X);
		double yDelta = cs.getDelta(Axis.Y);
		transform.translate(0, getHeight());
		transform.scale(getWidth() / xDelta, -getHeight() / yDelta);
		transform.translate(-cs.getMin(Axis.X),-cs.getMin(Axis.Y));
		return transform;
	}

	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub
	}
}
