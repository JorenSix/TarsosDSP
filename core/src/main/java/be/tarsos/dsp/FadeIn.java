package be.tarsos.dsp;

public class FadeIn implements AudioProcessor {

	private double duration;
	private double firstTime=-1;
	private double time;
	private GainProcessor gp=new GainProcessor(0.1);
	private boolean fadingIn=true;

	/**
	 * A new fade in processor
	 * @param d duration of the fade  in seconds
	 */
	public FadeIn(double d) //
	{
		this.duration=d;
	}

	/**
	 * 	Stop fade in processing immediately
 	 */
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
