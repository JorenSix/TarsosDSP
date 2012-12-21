package be.hogent.tarsos.dsp.example.catify;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.GainProcessor;
import be.hogent.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.hogent.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters;
import be.hogent.tarsos.dsp.example.PitchShiftingExample;
import be.hogent.tarsos.dsp.example.SharedCommandLineUtilities;
import be.hogent.tarsos.dsp.resample.RateTransposer;



public class Catify {
	
	public static void main(String[] args) throws InvalidMidiDataException, IOException, UnsupportedAudioFileException, LineUnavailableException {
		try {
			if (args.length == 0) {
				final String tempDir = System.getProperty("java.io.tmpdir");
				String path = new File(tempDir, "jingle_bells.mid")
						.getAbsolutePath();
				String resource = "/be/hogent/tarsos/dsp/example/catify/resources/jingle_bells.mid";
				copyFileFromJar(resource, path);
				Catify c = new Catify(new File(path), new File("out.wav"));
				c.catify();
			} else if (args.length == 1) {
				Catify c = new Catify(new File(args[0]), new File("out.wav"));
				c.catify();
			} else if (args.length == 2) {
				Catify c = new Catify(new File(args[0]), new File(args[1]));
				c.catify();
			}
		} catch (Exception e) {
			printDescription();
		}
	}
	
	private static void printDescription(){
		SharedCommandLineUtilities.printPrefix();
		System.err.println("Name:");
		System.err.println("\tTarsosDSP catify");
		SharedCommandLineUtilities.printLine();
		System.err.println("Synopsis:");
		System.err.println("\tjava -jar catify-latest.jar input.mid output.wav");
		SharedCommandLineUtilities.printLine();
		System.err.println("Description:");
		System.err.println("\tCatifys the midi file defined in input.mid.");
	}
	
	List<MidiNoteInfo> processedSamples;
	public Catify(File midiFile,File outputFile) throws InvalidMidiDataException, IOException{
		MidiParser p = new MidiParser(midiFile);
		processedSamples = p.generateNoteInfo();
	}
	
	public void catify() throws UnsupportedAudioFileException, IOException, LineUnavailableException{
		Collections.sort(processedSamples);
		int maxVelocity=0;
		for(MidiNoteInfo s: processedSamples){
			System.out.println(s.toString());
			maxVelocity = Math.max(maxVelocity, s.getVelocity());
		}
		for(MidiNoteInfo s: processedSamples){
			s.setVelocity((int) (s.getVelocity()/(float) maxVelocity*128));
		}
		buildSamples();
		generateSound();
		//ffmpegify();
	}
	
	
	
	
	public void ffmpegify(){
		for(int i = 0 ; i < processedSamples.size() -1 ; i++){
			MidiNoteInfo current = processedSamples.get(i);
			MidiNoteInfo next = processedSamples.get(i+1);
			if(current.getStart()==next.getStart()){
				if(next.getDuration() > current.getDuration()){
					processedSamples.remove(next);
				}else{
					processedSamples.remove(current);
				}
			}
		}
		System.out.println("*********");
		for(int i = 0 ; i < processedSamples.size() -1 ; i++){
			MidiNoteInfo current = processedSamples.get(i);
			MidiNoteInfo next = processedSamples.get(i+1);
			double duration = next.getStart() - current.getStart();
			if(duration != 0){
				System.out.println(String.format("[ -f vout_%.4f.mpg ] || ffmpeg -sameq  -t %.7f -i Miaauw.mpg -vcodec copy -an -y vout_%.4f.mpg", duration, duration,duration));
				System.out.println(String.format("cat vout_%.4f.mpg >> out.mpg ", duration));
			}
		}
		int i = processedSamples.size() -1;
		MidiNoteInfo last = processedSamples.get(i);
		System.out.println(String.format("[ -f vout_%.4f.mpg ] ||  ffmpeg -sameq  -t %.7f -i Miaauw.mpg -vcodec copy -an -y vout_%.4f.mpg", last.getDuration(), last.getDuration(),last.getDuration()));
		System.out.println(String.format("cat vout_%.4f.mpg >> out.mpg ", last.getDuration()));
	}
	
	private ArrayList<CatSample> catSamples;
	public void buildSamples(){
		catSamples = new ArrayList<CatSample>();
		final String tempDir = System.getProperty("java.io.tmpdir");
		String path = new File(tempDir,"4915__noisecollector__cat3_mod.wav").getAbsolutePath();
		String resource = "/be/hogent/tarsos/dsp/example/catify/resources/4915__noisecollector__cat3_mod.wav";
		copyFileFromJar(resource,path);
		catSamples.add(new CatSample(new File(path)));
	}
	
	/**
	 * Copy a file from a jar.
	 * 
	 * @param source
	 *            The path to read e.g. /package/name/here/help.html
	 * @param target
	 *            The target to save the file to.
	 */
	public static void copyFileFromJar(final String source, final String target) {
		try {
			final InputStream inputStream = new MidiNoteInfo(0,0,0).getClass().getResourceAsStream(source);
			OutputStream out;
			out = new FileOutputStream(target);
			final byte[] buffer = new byte[4096];
			int len = inputStream.read(buffer);
			while (len != -1) {
				out.write(buffer, 0, len);
				len = inputStream.read(buffer);
			}
			out.close();
			inputStream.close();
		} catch (final FileNotFoundException e) {
			
		} catch (final IOException e) {
			
		}
	}
	
	private static double hertzToMidiNote(double hertz){
		return 69 + 12 * Math.log(hertz/440)/Math.log(2);
	}
	
	private void generateSound() throws UnsupportedAudioFileException, IOException, LineUnavailableException{
		
		double duration = 0;
		for(MidiNoteInfo s: processedSamples){
			duration = Math.max(s.getStart()+s.getDuration(),duration);
		}
		final float sampleRate = 44100;
		final float[] buffer = new float[(int) (duration * sampleRate)];
		
		for(final MidiNoteInfo s: processedSamples){
	
			Collections.shuffle(catSamples);
			CatSample cs = catSamples.get(0);
			double originalDuration =cs.getDuration();
			int cents = (int) (s.getMidiNote() * 100 - hertzToMidiNote(cs.getAvgPitch()) * 100) ;//shift in cents
			double newDuration = s.getDuration();
			double pitchFactor = PitchShiftingExample.centToFactor(cents);
			double durationFactor = originalDuration/newDuration * pitchFactor;
			double gain = s.getVelocity()/128.0;

			WaveformSimilarityBasedOverlapAdd wsola;
			RateTransposer rateTransposer;
			rateTransposer = new RateTransposer(pitchFactor);
			wsola = new WaveformSimilarityBasedOverlapAdd(Parameters.musicDefaults(durationFactor,sampleRate));
			final AudioDispatcher dispatcher = AudioDispatcher.fromFile(cs.getFile(),wsola.getInputBufferSize(), wsola.getOverlap());
			wsola.setDispatcher(dispatcher);
			dispatcher.addAudioProcessor(new GainProcessor(gain));
			dispatcher.addAudioProcessor(wsola);
			dispatcher.addAudioProcessor(rateTransposer);			
			dispatcher.addAudioProcessor(new AudioProcessor() {
				
				@Override
				public void processingFinished() {
				}
				
				int dispatcherIndex = 0;
				@Override
				public boolean process(AudioEvent audioEvent) {
					int startIndex = (int) (s.getStart()*sampleRate) + dispatcherIndex ;
					
					float[] sampleBuffer = audioEvent.getFloatBuffer();
					for(int i = startIndex ; i < startIndex + sampleBuffer.length ; i++){
						buffer[i]+=sampleBuffer[i-startIndex];
					}
					dispatcherIndex+=sampleBuffer.length;
					return true;
				}
			});
			try{
			dispatcher.run();
			} catch (IllegalArgumentException e){
				
			}
		}
		
		float maxValue = 0;
		for(int i = 0 ; i < buffer.length ; i++){
			maxValue=Math.max(Math.abs(buffer[i]), maxValue);
		}
		
		float factor = 0.95f / maxValue;
		for(int i = 0 ; i < buffer.length ; i++){
			buffer[i]= factor * buffer[i];
		}
		
		final byte[] byteBuffer = new byte[buffer.length * 2];
		int bufferIndex = 0;
		for (int i = 0; i < byteBuffer.length; i++) {
			final int x = (int) (buffer[bufferIndex++] * 32767.0);
			byteBuffer[i] = (byte) x;
			i++;
			byteBuffer[i] = (byte) (x >>> 8);
		}		
		File out = new File("out.wav");
		boolean bigEndian = false;
		boolean signed = true;
		int bits = 16;
		int channels = 1;
		AudioFormat format;
		format = new AudioFormat(sampleRate, bits, channels, signed, bigEndian);
		ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
		AudioInputStream audioInputStream;
		audioInputStream = new AudioInputStream(bais, format,buffer.length);
		AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
		audioInputStream.close();		
	}	
}
