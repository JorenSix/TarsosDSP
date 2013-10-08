package be.hogent.tarsos.dsp.example.visualisation;

public class CoordinateSystem  {
	
	private AxisUnit xAxisUnits = AxisUnit.TIME;
	private AxisUnit yAxisUnits;
	
	private static float xMin = 0;
	private static float xMax = 10000;
	
	private float yMin;
	private float yMax;
	
	public CoordinateSystem(AxisUnit yAxisUnits, float yMin, float yMax){
		this.yAxisUnits = yAxisUnits;
		this.yMin = yMin;
		this.yMax = yMax;
	}
	
	public float getDelta(Axis axis){
		final float delta;
		if (axis == Axis.X){
			delta = xMax-xMin;
		} else {
			delta = yMax-yMin;
		}
		return delta;
	}
	
	public AxisUnit getUnitsForAxis(Axis axis){
		final AxisUnit unit;
		if (axis == Axis.X){
			unit =  xAxisUnits;
		} else {
			unit =  yAxisUnits;
		}
		return unit;
	}
	
	public float getMin(Axis axis){
		final float min;
		if (axis == Axis.X){
			min = xMin;
		} else {
			min = yMin;
		}
		return min;
	}
	
	public float getMax(Axis axis){
		final float max;
		if (axis == Axis.X){
			max = xMax;
		} else {
			max = yMax;
		}
		return max;
	}
	

	public void setMax(Axis axis, float value){
		if (axis == Axis.X){
			xMax = value;
		} else {
			yMax = value;
		}
	}
	
	public void setMin(Axis axis, float value){
		if (axis == Axis.X){
			xMin = value;
		} else {
			yMin = value;
		}
	}
}
