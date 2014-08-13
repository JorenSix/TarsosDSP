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

package be.tarsos.dsp.example.catify;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;

public class MidiParser {
	private Sequence seq;
	private int currentTrack;
	private ArrayList<Integer> nextMessageOf;
	private List<MidiNoteInfo> temporaryMidiNotes;
	private List<MidiNoteInfo> midiNotes;
	
	public MidiParser(File midiFile) throws InvalidMidiDataException, IOException{
		seq = MidiSystem.getSequence(midiFile);		
	}
	
	public List<MidiNoteInfo> generateNoteInfo(){
		temporaryMidiNotes = new ArrayList<MidiNoteInfo>();
		midiNotes = new ArrayList<MidiNoteInfo>();
		nextMessageOf = new ArrayList<Integer>();
		currentTrack = 0;
		convertMidi2RealTime(seq);
		return midiNotes;
	}
	
	private  void convertMidi2RealTime(Sequence seq) {
		double currentTempo = 500000;
		int tickOfTempoChange = 0;
		double msb4 = 0;

		for (int track = 0; track < seq.getTracks().length; track++){
			nextMessageOf.add(0);
		}


		MidiEvent nextEvent;
		while ((nextEvent = getNextEvent()) != null) {
			int tick = (int) nextEvent.getTick();
			if (noteIsOff(nextEvent)) {
				double time = (msb4 + (((currentTempo / seq.getResolution()) / 1000) * (tick - tickOfTempoChange)));
				int stop = (int) (time + 0.5);
				int midiNote = ((int) nextEvent.getMessage().getMessage()[1] & 0xFF);
					
				MidiNoteInfo midiNoteInfo = null;
				for(MidiNoteInfo s : temporaryMidiNotes){
					if(s.getMidiNote()==midiNote){
						midiNoteInfo = s;
					}
				}
				if(midiNoteInfo==null){
					System.err.println("Note off without note on...");
				}else{
					temporaryMidiNotes.remove(midiNoteInfo);
					midiNoteInfo.setStop(stop);
					midiNotes.add(midiNoteInfo);					
				}
			} else if (noteIsOn(nextEvent)) {
				double time = (msb4 + (((currentTempo / seq.getResolution()) / 1000) * (tick - tickOfTempoChange)));
				int start = (int) (time + 0.5);
				int midiNote = ((int) nextEvent.getMessage().getMessage()[1] & 0xFF);
				int velocity = ((int) nextEvent.getMessage().getMessage()[2] & 0xFF);
				
				//System.out.println("track=" + currentTrack + " tick=" + tick + " time=" + start  + "ms " + " note " + midiNote + " on" + " velocity " + velocity);
				temporaryMidiNotes.add(new MidiNoteInfo(start,midiNote, velocity));
			} else if (changeTemp(nextEvent)) {
				String a = (Integer.toHexString((int) nextEvent.getMessage()
						.getMessage()[3] & 0xFF));
				String b = (Integer.toHexString((int) nextEvent.getMessage()
						.getMessage()[4] & 0xFF));
				String c = (Integer.toHexString((int) nextEvent.getMessage()
						.getMessage()[5] & 0xFF));
				if (a.length() == 1)
					a = ("0" + a);
				if (b.length() == 1)
					b = ("0" + b);
				if (c.length() == 1)
					c = ("0" + c);
				String whole = a + b + c;
				int newTempo = Integer.parseInt(whole, 16);
				double newTime = (currentTempo / seq.getResolution())
						* (tick - tickOfTempoChange);
				msb4 += (newTime / 1000);
				tickOfTempoChange = tick;
				currentTempo = newTempo;
			}
		}
	}

	private MidiEvent getNextEvent() {
		ArrayList<MidiEvent> nextEvent = new ArrayList<MidiEvent>();
		ArrayList<Integer> trackOfNextEvent = new ArrayList<Integer>();
		for (int track = 0; track < seq.getTracks().length; track++) {
			if (seq.getTracks()[track].size() - 1 > (nextMessageOf.get(track))) {
				nextEvent.add(seq.getTracks()[track].get(nextMessageOf
						.get(track)));
				trackOfNextEvent.add(track);
			}
		}
		if (nextEvent.size() == 0)
			return null;
		int closestMessage = 0;
		int smallestTick = (int) nextEvent.get(0).getTick();
		for (int trialMessage = 1; trialMessage < nextEvent.size(); trialMessage++) {
			if ((int) nextEvent.get(trialMessage).getTick() < smallestTick) {
				smallestTick = (int) nextEvent.get(trialMessage).getTick();
				closestMessage = trialMessage;
			}
		}
		currentTrack = trackOfNextEvent.get(closestMessage);
		nextMessageOf.set(currentTrack, (nextMessageOf.get(currentTrack) + 1));
		return nextEvent.get(closestMessage);
	}

	private static boolean noteIsOff(MidiEvent event) {
		if (Integer.toString((int) event.getMessage().getStatus(), 16)
				.toUpperCase().charAt(0) == '8'
				|| (noteIsOn(event) && event.getMessage().getLength() >= 3 && ((int) event
						.getMessage().getMessage()[2] & 0xFF) == 0))
			return true;
		return false;
	}

	private static boolean noteIsOn(MidiEvent event) {
		if (Integer.toString(event.getMessage().getStatus(), 16).toUpperCase()
				.charAt(0) == '9')
			return true;
		return false;
	}

	private static boolean changeTemp(MidiEvent event) {
		if ((int) Integer.valueOf(
				("" + Integer
						.toString((int) event.getMessage().getStatus(), 16)
						.toUpperCase().charAt(0)), 16) == 15
				&& (int) Integer.valueOf(
						("" + ((String) (Integer.toString((int) event
								.getMessage().getStatus(), 16).toUpperCase()))
								.charAt(1)), 16) == 15
				&& Integer
						.toString((int) event.getMessage().getMessage()[1], 16)
						.toUpperCase().length() == 2
				&& Integer
						.toString((int) event.getMessage().getMessage()[1], 16)
						.toUpperCase().equals("51")
				&& Integer
						.toString((int) event.getMessage().getMessage()[2], 16)
						.toUpperCase().equals("3"))
			return true;
		return false;
	}
}
