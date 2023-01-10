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

/*  BeatRoot: An interactive beat tracking system
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
	http://www.gnu.org/licenses/gpl.txt or write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package be.tarsos.dsp.beatroot;

import java.util.ListIterator;


/** Class for maintaining the set of all Agents involved in beat tracking a piece of music.
 *  Implements a simple linked list terminated by an AgentList with a null Agent (ag).
 */
public class AgentList {

	/** Flag for choice between sum and average beat salience values for Agent scores.
	 *  The use of summed saliences favours faster tempi or lower metrical levels. */
	public static boolean useAverageSalience = false;
	
	/** Flag for printing debugging output. */
	public static boolean debug = false;
	
	/** For the purpose of removing duplicate agents, the default JND of IBI */
	public static final double DEFAULT_BI = 0.02;
	
	/** For the purpose of removing duplicate agents, the default JND of phase */
	public static final double DEFAULT_BT = 0.04;

	/** A beat tracking Agent */
	public Agent ag;
	
	/** The remainder of the linked list */
	public AgentList next;
	
	/** The length of the list (number of beat tracking Agents) */
	public static int count = 0;
	
	/** For the purpose of removing duplicate agents, the JND of IBI.
	 *  Not changed in the current version. */
	public static double thresholdBI = DEFAULT_BI;

	/** For the purpose of removing duplicate agents, the JND of phase.
	 *  Not changed in the current version. */
	public static double thresholdBT = DEFAULT_BT;

	/** Default constructor */
	public AgentList() {
		this(null, null);
	}
	
	/** Constructor for an AgentList: the Agent a is prepended to the list al.
	 *  @param a The Agent at the head of the list
	 *  @param al The tail of the list
	 */
	public AgentList(Agent a, AgentList al) {
		ag = a;
		next = al;
		if (next == null) {
			if (ag != null)
				next = new AgentList();		// insert null-terminator if it was forgotten
			else {
				count = 0;
				thresholdBI = DEFAULT_BI;
				thresholdBT = DEFAULT_BT;
			}
		}
	} // constructor

	/** Deep print of AgentList for debugging */
	public void print() {
		System.out.println("agentList.print: (size=" + count + ")");
		for (AgentList ptr = this; ptr.ag != null; ptr = ptr.next)
			ptr.ag.print(2);
		System.out.println("End of agentList.print()");
	} // print()

	/**
	 *  Inserts newAgent into the list in ascending order of beatInterval
	 * @param a 	The new agent to add
	 */
	public void add(Agent a) {
		add(a, true);
	} // add()/1

	/** Appends newAgent to list (sort==false), or inserts newAgent into the list
	 *  in ascending order of beatInterval
	 *  @param newAgent The agent to be added to the list
	 *  @param sort Flag indicating whether the list is sorted or not
	 */
	public void add(Agent newAgent, boolean sort){
		if (newAgent == null)
			return;
		AgentList ptr;
		count++;
		for (ptr = this; ptr.ag != null; ptr = ptr.next)
			if (sort && (newAgent.beatInterval <= ptr.ag.beatInterval)) {
				ptr.next = new AgentList(ptr.ag, ptr.next);
				ptr.ag = newAgent;
				return;
			}
		ptr.next = new AgentList();
		ptr.ag = newAgent;
	} // add()/2

	/** Sorts the AgentList by increasing beatInterval, using a bubble sort
	 *  since it is assumed that the list is almost sorted. */
	public void sort() {
		boolean sorted = false;
		while (!sorted) {
			sorted = true;
			for (AgentList ptr = this; ptr.ag != null; ptr = ptr.next) {
				if ((ptr.next.ag != null) &&
						(ptr.ag.beatInterval > ptr.next.ag.beatInterval)) {
					Agent temp = ptr.ag;
					ptr.ag = ptr.next.ag;
					ptr.next.ag = temp;
					sorted = false;
				}
			} // for
		} // while
	} // sort()

	/** Removes the current item from the list.
	 *  The current item does not need to be the head of the whole list.
	 *  @param ptr Points to the Agent which is removed from the list
	 */
	public void remove(AgentList ptr) {
		count--;
		ptr.ag = ptr.next.ag;	// null-terminated list always has next
		ptr.next = ptr.next.next;
	} // remove()

	/** Removes Agents from the list which are duplicates of other Agents.
	 *  A duplicate is defined by the tempo and phase thresholds
	 *  thresholdBI and thresholdBT respectively.
	 */
	protected void removeDuplicates() {
		sort();
		for (AgentList ptr = this; ptr.ag != null; ptr = ptr.next) {
			if (ptr.ag.phaseScore < 0.0)		// already flagged for deletion
				continue;
			for (AgentList ptr2 = ptr.next; ptr2.ag != null; ptr2 = ptr2.next) {
				if (ptr2.ag.beatInterval - ptr.ag.beatInterval > thresholdBI)
					break;
				if (Math.abs(ptr.ag.beatTime - ptr2.ag.beatTime) > thresholdBT)
					continue;
				if (ptr.ag.phaseScore < ptr2.ag.phaseScore) {
					ptr.ag.phaseScore = -1.0;	// flag for deletion
					if (ptr2.ag.topScoreTime < ptr.ag.topScoreTime)
						ptr2.ag.topScoreTime = ptr.ag.topScoreTime;
					break;
				} else {
					ptr2.ag.phaseScore = -1.0;	// flag for deletion
					if (ptr.ag.topScoreTime < ptr2.ag.topScoreTime)
						ptr.ag.topScoreTime = ptr2.ag.topScoreTime;
				}
			}
		}
		for (AgentList ptr = this; ptr.ag != null; ) {
			if (ptr.ag.phaseScore < 0.0) {
				remove(ptr);
			} else
				ptr = ptr.next;
		}
	} // removeDuplicates()

	/** Perform beat tracking on a list of events (onsets).
	 *  @param el The list of onsets (or events or peaks) to beat track
	 */
	public void beatTrack(EventList el) {
		beatTrack(el, -1.0);
	} // beatTrack()/1
	
	/** Perform beat tracking on a list of events (onsets).
	 *  @param el The list of onsets (or events or peaks) to beat track.
	 *  @param stop Do not find beats after <code>stop</code> seconds.
	 */
	public void beatTrack(EventList el, double stop) {
		ListIterator<Event> ptr = el.listIterator();
		boolean phaseGiven = (ag != null) &&
							 (ag.beatTime >= 0); // if given for one, assume given for others
		while (ptr.hasNext()) {
			Event ev = ptr.next();
			if ((stop > 0) && (ev.keyDown > stop))
				break;
			boolean created = phaseGiven;
			double prevBeatInterval = -1.0;
			for (AgentList ap = this; ap.ag != null; ap = ap.next) {
				Agent currentAgent = ap.ag;
				if (currentAgent.beatInterval != prevBeatInterval) {
					if ((prevBeatInterval>=0) && !created && (ev.keyDown<5.0)) {
						// Create new agent with different phase
						Agent newAgent = new Agent(prevBeatInterval);
						newAgent.considerAsBeat(ev, this);
						add(newAgent);
					}
					prevBeatInterval = currentAgent.beatInterval;
					created = phaseGiven;
				}
				if (currentAgent.considerAsBeat(ev, this))
					created = true;
				if (currentAgent != ap.ag)	// new one been inserted, skip it
					ap = ap.next;
			} // loop for each agent
			removeDuplicates();
		} // loop for each event
	} // beatTrack()

	/** Finds the Agent with the highest score in the list.
	 *  @return The Agent with the highest score
	 */
	public Agent bestAgent() {
		double best = -1.0;
		Agent bestAg = null;
		for (AgentList ap = this; ap.ag != null; ap = ap.next) {
			double startTime = ap.ag.events.l.getFirst().keyDown;
			double conf = (ap.ag.phaseScore + ap.ag.tempoScore) /
					(useAverageSalience? (double)ap.ag.beatCount: 1.0);
			if (conf > best) {
				bestAg = ap.ag;
				best = conf;
			}
			if (debug) {
				ap.ag.print(0);
				System.out.printf(" +%5.3f    Av-salience = %3.1f\n",
									startTime, conf);
			}
		}
		if (debug) {
			if (bestAg != null) {
				System.out.print("Best ");
				bestAg.print(0);
				System.out.printf("    Av-salience = %5.1f\n", best);
				// bestAg.events.print();
			} else
				System.out.println("No surviving agent - beat tracking failed");
		}
		return bestAg;
	} // bestAgent()

} // class AgentList
