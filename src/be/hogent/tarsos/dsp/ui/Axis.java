package be.hogent.tarsos.dsp.ui;

public enum Axis {
	X,Y;
	
	boolean isHorizontal(){
		if(this == X){
			return true;
		}else{
			return false;
		}
	}
}
