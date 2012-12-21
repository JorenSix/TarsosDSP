package be.hogent.tarsos.dsp.example.catify;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.pitch.PitchDetectionHandler;
import be.hogent.tarsos.dsp.pitch.PitchDetectionResult;
import be.hogent.tarsos.dsp.pitch.PitchProcessor;
import be.hogent.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

class CatSample{
	double duration;
	ArrayList<Float> pitches;
	float avgPitch = 0;
	AudioFormat format ;
	File file;
	public CatSample(File file){
		this.file=file;
		pitches = new ArrayList<Float>();
		try {
			format = AudioSystem.getAudioFileFormat(file).getFormat();
			duration = AudioSystem.getAudioFileFormat(file).getFrameLength()/format.getFrameRate();
			AudioDispatcher dispatcher = AudioDispatcher.fromFile(file,1024, 0);
			dispatcher.addAudioProcessor(new PitchProcessor(PitchEstimationAlgorithm.FFT_YIN, format.getSampleRate(), 1024, new PitchDetectionHandler() {
				@Override
				public void handlePitch(PitchDetectionResult pitchDetectionResult,AudioEvent audioEvent) {
					if(pitchDetectionResult.isPitched())
					pitches.add(pitchDetectionResult.getPitch());
				}
			}));
			dispatcher.run();
			for(Float pitch:pitches){
				avgPitch+=pitch;
			}
			avgPitch = avgPitch/pitches.size();
			
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public double getDuration(){
		return duration;
	}
	
	public float getAvgPitch(){
		return avgPitch;
	}
	
	public String toString(){
		return ""+avgPitch;
	}

	public double getSampleRate() {
		return format.getSampleRate();
	}

	public File getFile() {
		return file;
	}

	public AudioFormat getFormat() {
		return format;
	}
}