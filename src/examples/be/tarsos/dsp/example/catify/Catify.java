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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
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

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters;
import be.tarsos.dsp.example.PitchShiftingExample;
import be.tarsos.dsp.example.SharedCommandLineUtilities;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.resample.RateTransposer;



public class Catify {
	
	public static void main(String[] args) throws InvalidMidiDataException, IOException, UnsupportedAudioFileException, LineUnavailableException {
		try {
			if (args.length == 0) {
				final String tempDir = System.getProperty("java.io.tmpdir");
				String path = new File(tempDir, "jingle_bells.mid").getAbsolutePath();
				String resource = "/be/tarsos/dsp/example/catify/resources/jingle_bells.mid";
				copyFileFromJar(resource, path);
				Catify c = new Catify(new File(path), new File("out.wav"),null);
				c.catify();
			} else if (args.length == 1) {
				Catify c = new Catify(new File(args[0]), new File("out.wav"),null);
				c.catify();
			} else if (args.length == 2) {
				Catify c = new Catify(new File(args[0]), new File(args[1]),null);
				c.catify();
			}else if (args.length == 3) {
				File dir = new File(args[2]);
				if(dir.isDirectory()){
					Catify c = new Catify(new File(args[0]), new File(args[1]),dir);
					c.catify();
				}else{
					System.err.println("Third argument should be a directory containing wav files.");
					new IllegalArgumentException("Third argument should be a directory containing wav files.");
				}
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
		System.err.println("\tjava -jar catify-latest.jar input.mid output.wav [dir]");
		System.err.println("\t\tinput.mid\tA midi file to render with the audio samples.");
		System.err.println("\t\toutput.wav\tA name of a wav file to render the midi to.");
		System.err.println("\t\tdir\tAn optional directory with audio samples used to render the midi. By default a cat sample is used");
		SharedCommandLineUtilities.printLine();
		System.err.println("Description:");
		System.err.println("\tCatifys the midi file defined in input.mid. It renders the midi with audio samples in either a directory or using a cat sample.");
	}
	
	List<MidiNoteInfo> processedSamples;
	File sampleDirectory;
	private ArrayList<CatSample> catSamples;
	
	public Catify(File midiFile,File outputFile,File sampleDirectory) throws InvalidMidiDataException, IOException{
		MidiParser p = new MidiParser(midiFile);
		processedSamples = p.generateNoteInfo();
		this.sampleDirectory=sampleDirectory;
	}
	
	public void catify() throws UnsupportedAudioFileException, IOException, LineUnavailableException{
		Collections.sort(processedSamples);
		int maxVelocity=0;
		for(MidiNoteInfo s: processedSamples){
			maxVelocity = Math.max(maxVelocity, s.getVelocity());
		}
		for(MidiNoteInfo s: processedSamples){
			s.setVelocity((int) (s.getVelocity()/(float) maxVelocity*128));
		}
		buildSamples();
		generateSound();
	}
	
	

	
	
	public void buildSamples(){
		catSamples = new ArrayList<CatSample>();
		//default cat sample
		if(sampleDirectory==null){
			final String tempDir = System.getProperty("java.io.tmpdir");
			String path = new File(tempDir,"4915__noisecollector__cat3_mod.wav").getAbsolutePath();
			String resource = "/be/tarsos/dsp/example/catify/resources/4915__noisecollector__cat3_mod.wav";
			copyFileFromJar(resource,path);
			catSamples.add(new CatSample(new File(path)));
		} else {
			File[] samples = sampleDirectory.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File arg0, String arg1) {
					return arg1.toLowerCase().endsWith(".wav");
				}
			});
			if(samples.length==0){
				System.err.println("No audio samples found!!!!\n\n");
			}
			for(File sample:samples){
				catSamples.add(new CatSample(sample));	
			}
		}
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
			System.err.println("File not foud: " +  e.getMessage());
		} catch (final IOException e) {
			System.err.println("IO error: " + e.getMessage());
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
			final AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(cs.getFile(),wsola.getInputBufferSize(), wsola.getOverlap());
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
