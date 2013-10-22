package be.hogent.tarsos.dsp.example.visualisation;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;


public class ViewPort {
	
	private final List<ViewPortChangedListener> listeners;

	private final CoordinateSystem cs;

	
	public ViewPort(CoordinateSystem cs){
		listeners = new ArrayList<ViewPortChangedListener>();
		this.cs = cs;
	}
	
	public void addViewPortChangedListener(ViewPortChangedListener listener){
		listeners.add(listener);
	}
	
	public static interface ViewPortChangedListener{
		void viewPortChanged(ViewPort newViewPort);
	}
	
	private void viewPortChanged(){
		for(ViewPortChangedListener listener : listeners){
			listener.viewPortChanged(this);
		}
	}
	
	public void zoom(int amount, Point zoomPoint){
		float xDelta = cs.getDelta(Axis.X);
		float newXDelta = xDelta + amount * 1000;
		if(newXDelta > 20 && newXDelta < 600000) {
			cs.setMax(Axis.X, cs.getMin(Axis.X) + newXDelta);
		}
		viewPortChanged();
	}
	
	public void drag(float xAmount, float yAmount){
		cs.setMin(Axis.X, cs.getMin(Axis.X) + xAmount);
		cs.setMax(Axis.X, cs.getMax(Axis.X) + xAmount);
		
		cs.setMin(Axis.Y, cs.getMin(Axis.Y) + yAmount);
		cs.setMax(Axis.Y, cs.getMax(Axis.Y) + yAmount);
		
		viewPortChanged();
	}	
}
