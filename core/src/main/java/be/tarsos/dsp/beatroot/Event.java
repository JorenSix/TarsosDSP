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

/**
 * A beatroot event
 */
public class Event implements Comparable<Event>, Cloneable {

	public double keyDown, keyUp, pedalUp, scoreBeat, scoreDuration, salience;
	public int midiPitch, midiVelocity, flags, midiCommand, midiChannel,
				midiTrack;
	//public String label;

	public Event(double onset, double offset, double eOffset, int pitch,
				 int velocity, double beat, double duration, int eventFlags,
				 int command, int channel, int track) {
		this(onset, offset, eOffset, pitch, velocity, beat,duration,eventFlags);
		midiCommand = command;
		midiChannel = channel;
		midiTrack = track;
	} // constructor

	public Event(double onset, double offset, double eOffset, int pitch,
				 int velocity, double beat, double duration, int eventFlags) {
		keyDown = onset;
		keyUp = offset;
		pedalUp = eOffset;
		midiPitch = pitch;
		midiVelocity = velocity;
		scoreBeat = beat;
		scoreDuration = duration;
		flags = eventFlags;
		midiCommand = 144;//javax.sound.midi.ShortMessage.NOTE_ON;
		midiChannel = 1;
		midiTrack = 0;
		salience = 0;
	} // constructor

	public Event clone() {
		return new Event(keyDown, keyUp, pedalUp, midiPitch, midiVelocity,
					scoreBeat, scoreDuration, flags, midiCommand, midiChannel,
					midiTrack);
	} // clone()

	// Interface Comparable
	public int compareTo(Event e) {
		return (int)Math.signum(keyDown - e.keyDown);
	} // compareTo()

	public String toString() {
		return "n=" + midiPitch + " v=" + midiVelocity + " t=" + keyDown +
				" to " + keyUp + " (" + pedalUp + ")";
	} // toString()

	public void print(Flags f) {
		System.out.printf("Event:\n");
		System.out.printf("\tkeyDown / Up / pedalUp: %5.3f / %5.3f /  %5.3f\n",
			keyDown, keyUp, pedalUp);
		//System.out.printf("\tkeyUp: %5.3f\n", keyUp);
		//System.out.printf("\tpedalUp: %5.3f\n", pedalUp);
		System.out.printf("\tmidiPitch: %d\n", midiPitch);
		System.out.printf("\tmidiVelocity: %d\n", midiVelocity);
		System.out.printf("\tmidiCommand: %02x\t", midiCommand | midiChannel);
		//System.out.printf("\tmidiChannel: %d\n", midiChannel);
		System.out.printf("\tmidiTrack: %d\n", midiTrack);
		System.out.printf("\tsalience: %5.3f\t", salience);
		System.out.printf("\tscoreBeat: %5.3f\t", scoreBeat);
		System.out.printf("\tscoreDuration: %5.3f\n", scoreDuration);
		System.out.printf("\tflags: %X", flags);
		if (f != null) {
			int ff = flags;
			for (int i=0; ff != 0; i++) {
				if (ff % 2 == 1)
					System.out.print(" " + f.getLabel(i));
				ff >>>= 1;
			}
		}
		System.out.print("\n\n");
	} // print()

} // class Event
