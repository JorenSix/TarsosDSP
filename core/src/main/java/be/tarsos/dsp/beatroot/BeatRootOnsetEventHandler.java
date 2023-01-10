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

package be.tarsos.dsp.beatroot;

import java.util.Iterator;

import be.tarsos.dsp.onsets.OnsetHandler;

/**
 * Forms a bridge between the BeatRoot beat tracking system and an
 * interchangeable onset detector. The beat tracker does not work in real-time.
 * First all onsets need to be detected. In a post-processing step a beat
 * estimation is done using reocurring inter onset intervals (IOI's). To return
 * the time of the beats an OnsetHandler is abused.
 * 
 * @author Joren Six
 */
public class BeatRootOnsetEventHandler implements OnsetHandler {

	private final EventList onsetList = new EventList();
	
	@Override
	public void handleOnset(double time, double salience) {
		double roundedTime = Math.round(time *100 )/100.0;
		Event e = newEvent(roundedTime,0);
		e.salience = salience;
		onsetList.add(e);		
	}
	
	
	/**
	 * Creates a new Event object representing an onset or beat.
	 * 
	 * @param time
	 *            The time of the beat in seconds
	 * @param beatNum
	 *            The index of the beat or onset.
	 * @return The Event object representing the beat or onset.
	 */
	private Event newEvent(double time, int beatNum) {
		return new Event(time,time, time, 56, 64, beatNum, 0, 1);
	}
	
	/**
	 * Guess the beats using the populated list of onsets.
	 * 
	 * @param beatHandler
	 *            Use this handler to get the time of the beats. The salience of
	 *            the beat is not calculated: -1 is returned.
	 */
	public void trackBeats(OnsetHandler beatHandler){
		AgentList agents = null;		
		// tempo not given; use tempo induction
		agents = Induction.beatInduction(onsetList);
		agents.beatTrack(onsetList, -1);
		Agent best = agents.bestAgent();
		if (best != null) {
			best.fillBeats(-1.0);
			EventList beats = best.events;
			Iterator<Event> eventIterator = beats.iterator();
			while(eventIterator.hasNext()){
				Event beat = eventIterator.next();
				double time = beat.keyDown;
				beatHandler.handleOnset(time, -1);
			}
		} else {
			System.err.println("No best agent");
		}
	}

}
