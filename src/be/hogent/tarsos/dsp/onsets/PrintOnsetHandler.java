package be.hogent.tarsos.dsp.onsets;

public class PrintOnsetHandler implements OnsetHandler{
	@Override
	public void handleOnset(double time, double salience) {
		System.out.println(String.format("%.4f;%.4f", time,salience));	
	}		
}