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

package be.tarsos.dsp.ui;

public class CoordinateSystem  {
	
	private final AxisUnit xAxisUnits;
	private final AxisUnit yAxisUnits;
	
	private float xMin = 0;
	private float xMax = 10000;
	
	private float yMin;
	private float yMax;
	
	private final boolean wraps;
	private float wrappingOrigin;
	
	public CoordinateSystem(AxisUnit yAxisUnits, float yMin, float yMax){
		this(AxisUnit.TIME, yAxisUnits,yMin,yMax,false);
	}
	
	public CoordinateSystem(AxisUnit yAxisUnits, float yMin, float yMax,boolean wraps){
		this(AxisUnit.TIME,yAxisUnits,yMin,yMax,wraps);
	}
	
	public CoordinateSystem(AxisUnit xAxisUnits, AxisUnit yAxisUnits, float yMin, float yMax,boolean wraps){
		this.yAxisUnits = yAxisUnits;
		this.xAxisUnits = xAxisUnits;
		this.yMin = yMin;
		this.yMax = yMax;
		this.wraps = wraps;
		wrappingOrigin = 0.0f;
	}
	
	public float getWrappingOrigin(){
		return this.wrappingOrigin;
	}
	
	public void setWrappingOrigin(float wrappingOrigin){
		this.wrappingOrigin = wrappingOrigin;
	}
	
	public float getRealXValue(float value){
		final float realValue;
		if(isWrapping()){
			realValue = (1000 * getDelta(Axis.X) + value + getWrappingOrigin()) % getDelta(Axis.X);
		}else{
			realValue = value;
		}
		return realValue;
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
	
	//For selection Layer
	private double startX =  Double.MAX_VALUE;
	private double startY =  Double.MAX_VALUE;
	private double endX =  Double.MAX_VALUE;
	private double endY =  Double.MAX_VALUE;;
	public void setStartPoint(double x,double y){
		startX = x;
		startY = y;
	}
	
	public boolean hasStartPoint(){return startX != Double.MAX_VALUE;}
	
	public void setEndPoint(double x,double y){
		endX=x;
		endY=y;		
	}
	
	public void clearPoints(){
		startX = Double.MAX_VALUE;
		startY = Double.MAX_VALUE;
		endX = Double.MAX_VALUE;
		endY = Double.MAX_VALUE;
	}
	
	public double getStartX(){return startX;}
	public double getStartY(){return startY;}
	public double getEndY(){return endY;}
	public double getEndX(){return endX;}


	public boolean isWrapping() {
		return wraps;
	}
}
