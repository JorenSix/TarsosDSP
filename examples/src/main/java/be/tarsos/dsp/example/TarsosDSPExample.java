package be.tarsos.dsp.example;

public abstract class TarsosDSPExample {
	
	public abstract String name();

	public String description(){
		return name();
	}

	public abstract void start();
}
