package be.hogent.tarsos.dsp.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.beatroot.Agent;
import be.hogent.tarsos.dsp.beatroot.AgentList;
import be.hogent.tarsos.dsp.beatroot.Event;
import be.hogent.tarsos.dsp.beatroot.EventList;
import be.hogent.tarsos.dsp.beatroot.Induction;
import be.hogent.tarsos.dsp.onsets.BeatRootSpectralFluxOnsetDetection;
import be.hogent.tarsos.dsp.onsets.OnsetHandler;

public class BeatRootTest {
	@Test
	public void testExpectedOnsets() throws UnsupportedAudioFileException, IOException{
		File audioFile = TestUtilities.onsetsAudioFile();
		String contents = TestUtilities.readFileFromJar("/be/hogent/tarsos/dsp/test/resources/NR45_expected_onsets.txt");
		String[] onsetStrings = contents.split("\n");
		final double[] expectedOnsets = new double[onsetStrings.length];
		int i = 0;
		for(String onset : onsetStrings){
			expectedOnsets[i] = Double.parseDouble(onset);
			i++;
		}
		
		AudioDispatcher d = AudioDispatcher.fromFile(audioFile, 2048, 2048-441);
		d.setZeroPad(true);
		BeatRootSpectralFluxOnsetDetection b = new BeatRootSpectralFluxOnsetDetection(d, 2048,441);
		b.setHandler(new OnsetHandler(){
			int i = 0;
			@Override
			public void handleOnset(double actualTime, double salience) {
				double expectedTime = expectedOnsets[i];
				assertEquals("Onset time should be the expected value!",expectedTime,actualTime,0.0001);
				i++;
			}
		});		
		d.addAudioProcessor(b);
		d.run();
	}
	
	@Test
	public void testExpectedBeats() throws UnsupportedAudioFileException, IOException{
		File audioFile = TestUtilities.onsetsAudioFile();
		String contents = TestUtilities.readFileFromJar("/be/hogent/tarsos/dsp/test/resources/NR45_expected_beats.txt");
		String[] beatsStrings = contents.split("\n");
		final double[] expectedBeats = new double[beatsStrings.length];
		int i = 0;
		for(String beat : beatsStrings){
			expectedBeats[i] = Double.parseDouble(beat);
			i++;
		}
		i = 0;
		
		AudioDispatcher d = AudioDispatcher.fromFile(audioFile, 2048, 2048-441);
		d.setZeroPad(true);
		BeatRootSpectralFluxOnsetDetection b = new BeatRootSpectralFluxOnsetDetection(d, 2048,441);
		b.setHandler(new OnsetHandler(){
			@Override
			public void handleOnset(double time, double salience) {
				onsetList.add(newEvent(time, 0));
			}});
		d.addAudioProcessor(b);
		d.run();
		
		AgentList agents = null;
		double beatTime = -1.0;
		int count = 0;
		
		// tempo not given; use tempo induction
		agents = Induction.beatInduction(onsetList);
		for (AgentList ptr = agents; ptr.ag != null; ptr = ptr.next) {
			ptr.ag.beatTime = beatTime;
			ptr.ag.beatCount = count;
			ptr.ag.events = new EventList();
		}
		agents.beatTrack(onsetList, -1);
		Agent best = agents.bestAgent();
		if (best != null) {
			best.fillBeats(0);
			EventList beats = best.events;
			Iterator<Event> eventIterator = beats.iterator();
			while(eventIterator.hasNext()){
				Event beat = eventIterator.next();
				double expectedTime = expectedBeats[i];
				double actualTime = beat.keyDown;
				//assertEquals("Beat time should be the expected value!",expectedTime,actualTime,0.00001);
				i++;
			}
			
		} else {
			System.err.println("No best agent");
		}
		
	}
	
	/** location of selected beat (previous()) in the EventList <code>beats</code> */
	protected ListIterator<Event> beatPtr;
	/** beat data encoded as a list of Events */
	private EventList onsetList = new EventList();
	


	/** Creates a new Event object representing an onset or beat.
	 *  @param time The time of the beat in seconds
	 *  @param beatNum The index of the beat or onset.
	 *  @return The Event object representing the beat or onset.
	 */
	public static Event newEvent(double time, int beatNum) {
		return new Event(time,time, time, 56, 64, beatNum, 0, 1);
	} // newBeat()
}
