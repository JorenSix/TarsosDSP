package be.hogent.tarsos.dsp.example.catify;

class MidiNoteInfo implements Comparable<MidiNoteInfo>{
	private final int start;
	private final int midiNote;
	private int velocity;
	private int duration;
	
	public MidiNoteInfo(int start,int midiNote,int velocity){
		this.start=start;
		this.midiNote=midiNote;
		this.velocity=velocity;
	}

	public int getMidiNote() {
		return midiNote;
	}
	
	public void setStop(int stop){
		duration = stop-start;
	}
	
	public String toString(){
		return String.format("start:%dms note:%d velocity:%d duration:%dms",start,midiNote,velocity,duration);
	}
	
	public String shortName(){
		return String.format("%d_%d_%d_%d",start,midiNote,velocity,duration);
	}

	@Override
	public int compareTo(MidiNoteInfo o) {
		return Integer.valueOf(start).compareTo(Integer.valueOf(o.start));
	}

	public double getDuration() {
		return duration/1000.0;
	}

	public double getStart() {
		return start/1000.0;
	}

	public int getVelocity() {
		return velocity;
	}
	public void setVelocity(int newVelocity){
		velocity=newVelocity;
	}
}