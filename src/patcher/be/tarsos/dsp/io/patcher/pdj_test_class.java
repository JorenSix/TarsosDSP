package be.tarsos.dsp.io.patcher;

import com.cycling74.max.*;
import com.cycling74.msp.MSPBuffer;

public class pdj_test_class extends MaxObject implements Executable {
	MaxClock clock;
	float patate;

	public pdj_test_class(float f) {
		clock = new MaxClock(this);

		declareAttribute("patate");
		declareIO(2,2);
		
		post("received " + f + " in constructor");
	}
	
	public pdj_test_class() {
		clock = new MaxClock(this);

		declareAttribute("patate");
		declareIO(2,2);
	}
	
	public pdj_test_class(Atom args[]) {
		declareAttribute("patate");
		declareIO(2,2);
		
		for (int i=0;i<args.length;i++) {
			post("arg[" + i +"]:" + args[i].toString());
		}
		clock = new MaxClock(this);
	}

	public void bang() {
		Atom[] atom = new Atom[1];

		atom[0] = Atom.newAtom(10);
		MaxSystem.sendMessageToBoundObject("allo", "float", atom);
		outlet(0, 20);
		outlet(0, new float[] { 0.5f, 0.1f, 1, 200 });
		clock.delay(600);

	}
	
	public void grostas() {
	    Atom[] tosend = new Atom[2048];
	    
	    for(int i=0;i<tosend.length;i++) {
	        tosend[i] = Atom.newAtom(i);
	    }
	    outlet(0, tosend);
	}

	void testle() {
		post("array size: " + MSPBuffer.getSize("array_tester"));
		float f[] = MSPBuffer.peek("array_tester");

		for(int i=0;i<f.length;i++) {
			f[i] = patate;
		}
		MSPBuffer.poke("array_tester", f);

		MSPBuffer.poke("array_tester", 1, 11, 1f);
		MSPBuffer.poke("array_tester", 1, 2, new float[] { 0.2f, -0.2f} );
		f = MSPBuffer.peek("array_tester", 1, 2, 2);
		post("array_tester[2:3]=" + f[0] + "," + f[1]);
		MSPBuffer.poke("array_tester", 1, 9, -1f);
		post("array_tester[9]=" + MSPBuffer.peek("array_tester", 1, 9));
		post("path of this patch:" + MaxSystem.locateFile("pdj-test.pd"));
		post("path of this patch(from patcher):" + getParentPatcher().getPath());
	}

	protected void inlet(float f) {
		post("le float " + f + "inlet " + getInlet());
		
		Atom args[] = new Atom[2];
		args[0] = Atom.newAtom(1);
		
		outlet(0, args);
	}

	void wer(Atom[] atom) {
		post("atom len "+ atom.length);
		post("calle " + atom[0].getString());
		post("calle 2 " + atom[1].toString());
	}

	void sizeArray(Atom[] atom) {
		MSPBuffer.setSize("array_tester", 0, atom[0].toInt());
	}

	protected void loadbang() {
		post("hey!!!! this is loadbang");
	}

	public void execute() {
		System.out.println("allo");
	}
	
	public void callstring(String call) {
		post("callstring value:" + call);
	}
	
	public void callstring(String call, float f) {
		post("callstring value:" + call + " with float " + f) ;		
	}
	
	public void anything(String sym, Atom args[]) {
		if ( sym.equals("test_anything") ) {
			super.anything(sym, args);
			return;
		}
		post("anything is called with: " +sym + " args.length: " + (args == null ? -1 : args.length));
	}
}
