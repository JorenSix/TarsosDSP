package be.tarsos.dsp;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.GainProcessor;

public class FadeIn implements AudioProcessor
{
	// VARIABLES
	
	private double duration;
	private double firstTime=-1;
	private double time;
	private GainProcessor gp=new GainProcessor(0.1);
	private boolean fadingIn=true;
	
	// METHODS
	
	// Constructor
	public FadeIn(double d) // d=duration of the fade in in seconds
	{
		this.duration=d;
	}
	
	// Stop fade in processing immediately
	public void stopFadeIn()
	{
		this.fadingIn=false;
	}

	@Override
	public boolean process(AudioEvent audioEvent)
	{
		// Don't do anything after the end of the Fade In
		if(fadingIn)
		{
			if(firstTime==-1)
				firstTime=audioEvent.getTimeStamp();
			
			
			// Increase the gain according to time since the beginning of the Fade In
			time=audioEvent.getTimeStamp()-firstTime;
			gp.setGain(time/duration);
			gp.process(audioEvent);
			if(time > duration){
				fadingIn = false;
			}
		}
		return true;
	}
	
	@Override
	public void processingFinished()
	{
		gp.processingFinished();
	}
}
