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

/*
	Copyright (C) 2001, 2006 by Simon Dixon

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License along
	with this program (the file gpl.txt); if not, download it from
	http://www.gnu.org/licenses/gpl.txt or write to the
	Free Software Foundation, Inc.,
	51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package be.tarsos.dsp.beatroot;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;



// Adapted from eventList::readMatchFile in beatroot/src/eventMidi.cpp

// Reads in a Prolog score+performance (.match) file; returns it as an eventList
// Lines in the match file can be of the form:
//		hammer_bounce-PlayedNote.
//		info(Attribute, Value).
//		insertion-PlayedNote.
//		ornament(Anchor)-PlayedNote.
//		ScoreNote-deletion.
//		ScoreNote-PlayedNote.
//		ScoreNote-trailing_score_note.
//		trailing_played_note-PlayedNote.
//		trill(Anchor)-PlayedNote.
// where ScoreNote is of the form
//		snote(Anchor,[NoteName,Modifier],Octave,Bar:Beat,Offset,Duration,
//				BeatNumber,DurationInBeats,ScoreAttributesList)
//		e.g. snote(n1,[b,b],5,1:1,0,3/16,0,0.75,[s])
// and PlayedNote is of the form
//		note(Number,[NoteName,Modifier],Octave,Onset,Offset,AdjOffset,Velocity)
//		e.g. note(1,[a,#],5,5054,6362,6768,53)

class WormFileParseException extends RuntimeException {

	static final long serialVersionUID = 0;
	public WormFileParseException(String s) {
		super(s);
	} // constructor

} // class WormFileParseException

class MatchFileParseException extends RuntimeException {

	static final long serialVersionUID = 0;
	public MatchFileParseException(String s) {
		super(s);
	} // constructor

} // class MatchFileParseException

class BTFileParseException extends RuntimeException {

	static final long serialVersionUID = 0;
	public BTFileParseException(String s) {
		super(s);
	} // constructor

} // class BTFileParseException


// Process the strings which label extra features of notes in match files.
// We assume no more than 32 distinct labels in a file.
class Flags {

	String[] labels = new String[32];
	int size = 0;
	
	int getFlag(String s) {
		if ((s == null) || s.equals(""))
			return 0;
		//int val = 1;
		for (int i = 0; i < size; i++)
			if (s.equals(labels[i]))
				return 1 << i;
		if (size == 32)	{
			System.err.println("Overflow: Too many flags: " + s);
			size--;
		}
		labels[size] = s;
		return 1 << size++;
	} // getFlag()

	String getLabel(int i) {
		if (i >= size)
			return "ERROR: Unknown flag";
		return labels[i];
	} // getLabel()

} // class Flags

/**
 *  A score/match/midi file is represented as an EventList object,
 *  which contains pointers to the head and tail links, and some
 *   class-wide parameters. Parameters are class-wide, as it is
 *   assumed that the Worm has only one input file at a time.
 */
public class EventList {

	public LinkedList<Event> l;

	private static boolean timingCorrection = false;
	private static double timingDisplacement = 0;
	private static int clockUnits = 480;
	private static int clockRate = 500000;
	private static double metricalLevel = 0;
	private static final double UNKNOWN = Double.NaN;
	private static boolean noMelody = false;
	private static boolean onlyMelody = false;
	private static Flags flags = new Flags();

	public EventList() {
		l = new LinkedList<Event>();
	} // constructor

	public EventList(EventList e) {
		this();
		ListIterator<Event> it = e.listIterator();
		while (it.hasNext())
			add(it.next());
	} // constructor

	public EventList(Event[] e) {
		this();
		for (int i=0; i < e.length; i++)
			add(e[i]);
	} // constructor

	public void add(Event e) {
		l.add(e);
	} // add()

	public void add(EventList ev) {
		l.addAll(ev.l);
	} // add()

	public void insert(Event newEvent, boolean uniqueTimes) {
		ListIterator<Event> li = l.listIterator();
		while (li.hasNext()) {
			int sgn = newEvent.compareTo(li.next());
			if (sgn < 0) {
				li.previous();
				break;
			} else if (uniqueTimes && (sgn == 0)) {
				li.remove();
				break;
			}
		}
		li.add(newEvent);
	} // insert()

	public ListIterator<Event> listIterator() {
		return l.listIterator();
	} // listIterator()

	public Iterator<Event> iterator() {
		return l.iterator();
	} // iterator()

	public int size() {
		return l.size();
	} // size()

	public Event[] toArray() {
		return toArray(0);
	} // toArray()

	public double[] toOnsetArray() {
		double[] d = new double[l.size()];
		int i = 0;
		for (Iterator<Event> it = l.iterator(); it.hasNext(); i++)
			d[i] = it.next().keyDown;
		return d;
	} // toOnsetArray()

	public Event[] toArray(int match) {
		int count = 0;
		for (Event e : l)
			if ((match == 0) || (e.midiCommand == match))
				count++;
		Event[] a = new Event[count];
		int i = 0;
		for (Event e : l)
			if ((match == 0) || (e.midiCommand == match))
				a[i++] = e;
		return a;
	} // toArray()

	public void writeBinary(String fileName) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(
										new FileOutputStream(fileName));
			oos.writeObject(this);
			oos.close();
		} catch (IOException e) {
			System.err.println(e);
		}
	} // writeBinary()

	public static EventList readBinary(String fileName) {
		try {
			ObjectInputStream ois = new ObjectInputStream(
										new FileInputStream(fileName));
			EventList e = (EventList) ois.readObject();
			ois.close();
			return e;
		} catch (IOException e) {
			System.err.println(e);
			return null;
		} catch (ClassNotFoundException e) {
			System.err.println(e);
			return null;
		}
	} // readBinary()

	/*
	public void writeMIDI(String fileName) {
		writeMIDI(fileName, null);
	} // writeMIDI()

	public void writeMIDI(String fileName, EventList pedal) {
		try {
			MidiSystem.write(toMIDI(pedal), 1, new File(fileName));
		} catch (Exception e) {
			System.err.println("Error: Unable to write MIDI file " + fileName);
			e.printStackTrace();
		}
	} // writeMIDI()

	public Sequence toMIDI(EventList pedal) throws InvalidMidiDataException {
		final int midiTempo = 1000000;
		Sequence s = new Sequence(Sequence.PPQ, 1000);
		Track[] tr = new Track[16];
		tr[0] = s.createTrack();
		MetaMessage mm = new MetaMessage();
		byte[] b = new byte[3];
		b[0] = (byte)((midiTempo >> 16) & 0xFF);
		b[1] = (byte)((midiTempo >> 8) & 0xFF);
		b[2] = (byte)(midiTempo & 0xFF);
		mm.setMessage(0x51, b, 3);
		tr[0].add(new MidiEvent(mm, 0L));
		for (Event e : l) {		// from match or beatTrack file
			if (e.midiCommand == 0)	// skip beatTrack file
				break;
			if (tr[e.midiTrack] == null)
				tr[e.midiTrack] = s.createTrack();
			//switch (e.midiCommand) 
			//case ShortMessage.NOTE_ON:
			//case ShortMessage.POLY_PRESSURE:
			//case ShortMessage.CONTROL_CHANGE:
			//case ShortMessage.PROGRAM_CHANGE:
			//case ShortMessage.CHANNEL_PRESSURE:
			//case ShortMessage.PITCH_BEND:
			ShortMessage sm = new ShortMessage();
			sm.setMessage(e.midiCommand, e.midiChannel,
							e.midiPitch, e.midiVelocity);
			tr[e.midiTrack].add(new MidiEvent(sm,
						(long)Math.round(1000 * e.keyDown)));
			if (e.midiCommand == ShortMessage.NOTE_ON) {
				sm = new ShortMessage();
				sm.setMessage(ShortMessage.NOTE_OFF, e.midiChannel, e.midiPitch, 0);
				tr[e.midiTrack].add(new MidiEvent(sm, (long)Math.round(1000 * e.keyUp)));
			}
		}
		if (pedal != null) {	// from MIDI file
	//		if (t.size() > 0)	// otherwise beatTrack files leave an empty trk
	//			t = s.createTrack();
			for (Event e : pedal.l) {
				if (tr[e.midiTrack] == null)
					tr[e.midiTrack] = s.createTrack();
				ShortMessage sm = new ShortMessage();
				sm.setMessage(e.midiCommand, e.midiChannel, 
								e.midiPitch, e.midiVelocity);
				tr[e.midiTrack].add(new MidiEvent(sm,
						(long)Math.round(1000 * e.keyDown)));
				if (e.midiCommand == ShortMessage.NOTE_ON) {
					sm = new ShortMessage();
					sm.setMessage(ShortMessage.NOTE_OFF, e.midiChannel,
									e.midiPitch,e.midiVelocity);
					tr[e.midiTrack].add(new MidiEvent(sm,
							(long)Math.round(1000 * e.keyUp)));
				}
				//catch (InvalidMidiDataException exception) {}
			}
		}
		return s;
	} // toMIDI()

	public static EventList readMidiFile(String fileName) {
		return readMidiFile(fileName, 0);
	} // readMidiFile()

	public static EventList readMidiFile(String fileName, int skipTrackFlag) {
		EventList list = new EventList();
		Sequence s;
		try {
			s = MidiSystem.getSequence(new File(fileName));
		} catch (Exception e) {
			e.printStackTrace();
			return list;
		}
		double midiTempo = 500000;
		double tempoFactor = midiTempo / s.getResolution() / 1000000.0;
		// System.err.println(tempoFactor);
		Event[][] noteOns = new Event[128][16];
		Track[] tracks = s.getTracks();
		for (int t = 0; t < tracks.length; t++, skipTrackFlag >>= 1) {
			if ((skipTrackFlag & 1) == 1)
				continue;
			for (int e = 0; e < tracks[t].size(); e++) {
				MidiEvent me = tracks[t].get(e);
				MidiMessage mm = me.getMessage();
				double time = me.getTick() * tempoFactor;
				byte[] mesg = mm.getMessage();
				int channel = mesg[0] & 0x0F;
				int command = mesg[0] & 0xF0;
				if (command == ShortMessage.NOTE_ON) {
					int pitch = mesg[1] & 0x7F;
					int velocity = mesg[2] & 0x7F;
					if (noteOns[pitch][channel] != null) {
						if (velocity == 0) {	// NOTE_OFF in disguise :(
							noteOns[pitch][channel].keyUp = time;
							noteOns[pitch][channel].pedalUp = time;
							noteOns[pitch][channel] = null;
						} else
 							System.err.println("Double note on: n=" + pitch +
									" c=" + channel +
									" t1=" + noteOns[pitch][channel] +
									" t2=" + time);
					} else {
						Event n = new Event(time, 0, 0, pitch, velocity, -1, -1,
										0, ShortMessage.NOTE_ON, channel, t);
						noteOns[pitch][channel] = n;
						list.add(n);
					}
				} else if (command == ShortMessage.NOTE_OFF) {
					int pitch = mesg[1] & 0x7F;
					noteOns[pitch][channel].keyUp = time;
					noteOns[pitch][channel].pedalUp = time;
					noteOns[pitch][channel] = null;
				} else if (command == 0xF0) {
					if ((channel == 0x0F) && (mesg[1] == 0x51)) {
						midiTempo = (mesg[5] & 0xFF) |
									((mesg[4] & 0xFF) << 8) |
									((mesg[3] & 0xFF) << 16);
						tempoFactor = midiTempo / s.getResolution() / 1000000.0;
					//	System.err.println("Info: Tempo change: " + midiTempo +
					//						"  tf=" + tempoFactor);
					}
				} else if (mesg.length > 3) {
					System.err.println("midi message too long: " + mesg.length);
					System.err.println("\tFirst byte: " + mesg[0]);
				} else {
					int b0 = mesg[0] & 0xFF;
					int b1 = -1;
					int b2 = -1;
					if (mesg.length > 1)
						b1 = mesg[1] & 0xFF;
					if (mesg.length > 2)
						b2 = mesg[2] & 0xFF;
					list.add(new Event(time, time, -1, b1, b2, -1, -1, 0,
										b0 & 0xF0, b0 & 0x0F, t));
				}
			}
		}
		for (int pitch = 0; pitch < 128; pitch++)
			for (int channel = 0; channel < 16; channel++)
				if (noteOns[pitch][channel] != null)
					System.err.println("Missing note off: n=" + 
							noteOns[pitch][channel].midiPitch + " t=" +
							noteOns[pitch][channel].keyDown);
		return list;
	} // readMidiFile()
*/
	public void print() {
		for (Iterator<Event> i = l.iterator(); i.hasNext(); )
			i.next().print(flags);
	} // print()

	public static void setTimingCorrection(double corr) {
		timingCorrection = corr >= 0;
		timingDisplacement = corr;
	} // setTimingCorrection()

	
	
	
	
} // class EventList
