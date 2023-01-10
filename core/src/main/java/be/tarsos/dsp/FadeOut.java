package be.tarsos.dsp;

public class FadeOut implements AudioProcessor {

	private double duration;
	private double firstTime=-1;
	private double time;
	private boolean isFadeOut=false;
	private GainProcessor gp=new GainProcessor(0.9);

	/**
	 * A new fade out processor
	 * @param d duration of the fade out in seconds
	 */
	public FadeOut(double d) // d=
	{
		this.duration=d;
	}

	/**
	 * Start fade out processing now
 	 */
	public void startFadeOut()
	{
		this.isFadeOut=true;
	}
	
	@Override
	public boolean process(AudioEvent audioEvent)
	{
		// Don't do anything before the beginning of Fade Out
		if(isFadeOut==true)
		{
			if(firstTime==-1)
				firstTime=audioEvent.getTimeStamp();

			// Decrease the gain according to time since the beginning of the Fade Out
			time=audioEvent.getTimeStamp()-firstTime;
			gp.setGain(1-time/duration);
			gp.process(audioEvent);
		}
		return true;
	}
	
	@Override
	public void processingFinished()
	{
		gp.processingFinished();
	}
}