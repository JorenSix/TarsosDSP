package be.tarsos.dsp.example;

import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import be.tarsos.dsp.example.constantq.ConstantQAudioPlayer;

public class HelloWorld extends JApplet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 2124867206882266077L;

	//Called when this applet is loaded into the browser.
    public void init() {
        //Execute a job on the event-dispatching thread; creating this applet's GUI.
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    
                    add(createGui());
                }

			
            });
        } catch (Exception e) {
            System.err.println("createGUI didn't complete successfully");
        }
    }
    
	private JPanel createGui() {
		
		return new ConstantQAudioPlayer();
	}
    
    
}