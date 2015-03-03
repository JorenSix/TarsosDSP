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


package be.tarsos.dsp.example.constantq;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;

public class Player implements AudioProcessor {
	

	private PropertyChangeSupport support = new PropertyChangeSupport(this);
	
	private PlayerState state;
	private File loadedFile;
	private GainProcessor gainProcessor;
	private AudioPlayer audioPlayer;
	private AudioDispatcher dispatcher;
	
	private double durationInSeconds;
	private double currentTime;
	private double pauzedAt;
	
	private final AudioProcessor processor;
	private final int stepSize;
	private final int overlap;
	
	
	private double gain;
	
	public Player(AudioProcessor processor,int stepSize,int overlap){
		state = PlayerState.NO_FILE_LOADED;
		gain = 1.0;
		this.processor = processor;
		this.stepSize =stepSize;
		this.overlap = overlap;
	}
	
	public void setStepSizeAndOverlap(int audioBufferSize,int bufferOverlap){
		dispatcher.setStepSizeAndOverlap(audioBufferSize, bufferOverlap);
	}	

	
	public void load(File file) {
		if(state != PlayerState.NO_FILE_LOADED){
			eject();
		}
		loadedFile = file;
		AudioFileFormat fileFormat;
		try {
			fileFormat = AudioSystem.getAudioFileFormat(loadedFile);
		} catch (UnsupportedAudioFileException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		}
		AudioFormat format = fileFormat.getFormat();
		durationInSeconds = fileFormat.getFrameLength() / format.getFrameRate();
		pauzedAt = 0;
		currentTime = 0;
		setState(PlayerState.FILE_LOADED);
	}
	
	public void eject(){
		loadedFile = null;
		stop();
		setState(PlayerState.NO_FILE_LOADED);
	}
	
	public void play(){
		if(state == PlayerState.NO_FILE_LOADED){
			throw new IllegalStateException("Can not play when no file is loaded");
		} else if(state == PlayerState.PAUZED) {
			play(pauzedAt);
		} else {
			play(0);
		}
	}
	
	public void play(double startTime) {
		if(state == PlayerState.NO_FILE_LOADED){
			throw new IllegalStateException("Can not play when no file is loaded");
		} else {
			try {
				AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(loadedFile);
				AudioFormat format = fileFormat.getFormat();
				
				gainProcessor = new GainProcessor(gain);
				audioPlayer = new AudioPlayer(format);		
				
				dispatcher = AudioDispatcherFactory.fromFile(loadedFile,stepSize,overlap);
				
				dispatcher.skip(startTime);
				dispatcher.addAudioProcessor(this);
				dispatcher.addAudioProcessor(processor);
				dispatcher.addAudioProcessor(gainProcessor);
				dispatcher.addAudioProcessor(audioPlayer);
				
				Thread t = new Thread(dispatcher,"Audio Player Thread");
				t.start();
				setState(PlayerState.PLAYING);
			} catch (UnsupportedAudioFileException e) {
				throw new Error(e);
			} catch (IOException e) {
				throw new Error(e);
			} catch (LineUnavailableException e) {
				throw new Error(e);
			}
		}
	}
	
	public void pauze(){
		pauze(currentTime);
	}
	
	public void pauze(double pauzeAt) {
		if(state == PlayerState.PLAYING || state == PlayerState.PAUZED){
			setState(PlayerState.PAUZED);
			dispatcher.stop();
			pauzedAt = pauzeAt;
		} else {
			throw new IllegalStateException("Can not pauze when nothing is playing");
		}
	}
	
	public void stop(){
		if(state == PlayerState.PLAYING || state == PlayerState.PAUZED){
			setState(PlayerState.STOPPED);
			dispatcher.stop();
		} else if(state != PlayerState.STOPPED){
			throw new IllegalStateException("Can not stop when nothing is playing");
		}
		
	}
	
	public void setGain(double newGain){
		gain = newGain;
		if(state == PlayerState.PLAYING ){
			gainProcessor.setGain(gain);
		}
	}
	
	public double getDurationInSeconds() {
		if(state == PlayerState.NO_FILE_LOADED){
			throw new IllegalStateException("No file loaded, unable to determine the duration in seconds");
		}
		return durationInSeconds;
	}

	private void setState(PlayerState newState){
		PlayerState oldState = state;
		state = newState;
		support.firePropertyChange("state", oldState, newState);
	}
	
	public PlayerState getState() {
		return state;
	}

	public void addPropertyChangeListener(PropertyChangeListener l) {
		support.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		support.removePropertyChangeListener(l);
	}
	
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		currentTime = audioEvent.getTimeStamp();
		return true;
	}

	@Override
	public void processingFinished() {
		if(state==PlayerState.PLAYING){
			setState(PlayerState.STOPPED);
		}
	}
	
	public File getLoadedFile(){
		return loadedFile;
	}

	
	/**
	 * Defines the state of the audio player.
	 * @author Joren Six
	 */
	public static enum PlayerState{
		/**
		 * No file is loaded.
		 */
		NO_FILE_LOADED,
		/**
		 * A file is loaded and ready to be played.
		 */
		FILE_LOADED,
		/**
		 * The file is playing
		 */
		PLAYING,
		/**
		 * Audio play back is paused.
		 */
		PAUZED,		
		/**
		 * Audio play back is stopped. 
		 */
		STOPPED
	}
}
