package be.hogent.tarsos.dsp.speechmusicdiscriminator;

import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioFile;
import be.hogent.tarsos.dsp.AudioPlayer;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.ZeroCrossingRateProcessor;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.features.EnergyBelowThreshold;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.features.EnergyStandardDeviation;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.features.SpeechMusicDiscriminationFeature;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.features.ZCRAboveThreshold;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.features.ZCRDiffAboveAndBelowMean;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.features.ZCRFirstOrderDiffStdDev;

/**
 * 
 * An implementation of a music / speech discriminator mainly based on
 * two papers:  
 * <ul>
 *  <li>
 *  <a href="http://www.ee.columbia.edu/~dpwe/papers/Saun96-spmus.pdf">Real-Time discrimination of Broadcast Speech/Music</a>
 *  - by John Saunders (1996).
 * </li>
 * <li>
 * 	<a href="http://www.ee.columbia.edu/~dpwe/papers/ScheiS97-mussp.pdf">
 * 	Construction and evaluation of a robust multifeature speech/music discriminator
 * 	</a>
 *  - by Schreier and Slaney (1997)
 * </li>
 * </ul>
 * 
 * The general method proposed in the papers is followed: features based on zero crossing rate and energy
 * are computed over a window of 2.5 seconds of unlabeled audio. Statistics from these features are 
 * computed and subsequently fed into a classifier. The classifier responds with either the label "music" or
 * "speech". 
 * 
 *  
 * @author Joren Six
 *
 */
public class SpeechMusicDiscriminator implements AudioProcessor {
	
	private final int resolution = 215; // every 2.5 seconds
	private final ZeroCrossingRateProcessor zeroCrossingRateProcessor = new ZeroCrossingRateProcessor();
	
	private final Deque<Double> zeroCrossingRatesDeque;
	private final Deque<Double> energyDeque;
	
	private final boolean isTraining;
	private static TrainingDataSet trainingSet;
	
	private final List<SpeechMusicDiscriminationFeature> features;
	
	private final ClassificationResultHandler handler;
	
	private final NaiveBayesClassifier classifier;
	private final TrainedDataSet trainedSpeechDataSet;
	private final TrainedDataSet trainedMusicDataSet;
	
	interface ClassificationResultHandler{
		void handleClassification(double timeStamp,ClassificationLabel label);
	}
	
	public SpeechMusicDiscriminator(ClassificationResultHandler handler){
		this(handler,false,null);
	}
	
	public SpeechMusicDiscriminator(ClassificationResultHandler handler,boolean isTraining,ClassificationLabel label){
		this.handler = handler;
		this.isTraining = isTraining;
		
		
		zeroCrossingRatesDeque = new LinkedBlockingDeque<Double>(resolution);
		energyDeque = new LinkedBlockingDeque<Double>(resolution);
		
		features = new ArrayList<SpeechMusicDiscriminationFeature>();
		features.add(new ZCRAboveThreshold());
		features.add(new ZCRDiffAboveAndBelowMean());
		features.add(new ZCRFirstOrderDiffStdDev());
		//features.add(new ZCRSkewness());
		features.add(new EnergyBelowThreshold());
		features.add(new EnergyStandardDeviation());
		
		
		if(isTraining && trainingSet == null){
			trainingSet = new TrainingDataSet(label.name(), features.size());
		}
		
		trainedSpeechDataSet = new TrainedDataSet(ClassificationLabel.SPEECH.name(), features.size());
		trainedMusicDataSet = new TrainedDataSet(ClassificationLabel.MUSIC.name(), features.size());	
		for(int i = 0 ; i < features.size() ; i++){
			SpeechMusicDiscriminationFeature feature = features.get(i);
			trainedSpeechDataSet.setFeature(i, feature.name(), feature.trainedMean(ClassificationLabel.SPEECH), feature.trainedVariance(ClassificationLabel.SPEECH));
			trainedMusicDataSet.setFeature(i, feature.name(), feature.trainedMean(ClassificationLabel.MUSIC), feature.trainedVariance(ClassificationLabel.MUSIC));
		}
			
		List<DataSet> datasets = new ArrayList<DataSet>();
		datasets.add(trainedSpeechDataSet);
		datasets.add(trainedMusicDataSet);
		classifier = new NaiveBayesClassifier(datasets);
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		zeroCrossingRateProcessor.process(audioEvent);
			
		double zeroCrossingRate = zeroCrossingRateProcessor.getZeroCrossingRate();
		double energy = audioEvent.getRMS();
		
		if(zeroCrossingRate > 0.005){
			energyDeque.add(energy);
			//store the current zero crossing rate
			zeroCrossingRatesDeque.add(zeroCrossingRate);
		}
		
		if(zeroCrossingRatesDeque.size() == resolution){
			double[] values = new double[features.size()];
			List<Double> zeroCrossingRates = new ArrayList<Double>(zeroCrossingRatesDeque);
			List<Double> energies = new ArrayList<Double>(energyDeque);
			for(int i = 0 ;i<features.size();i++){
				values[i] = features.get(i).calculateFeature(audioEvent, zeroCrossingRates, energies);
			}
			if(isTraining){
				for(int i = 0 ;i<features.size();i++){
					trainingSet.setDescription(i, features.get(i).name());
					trainingSet.addDataPoint(i, values[i]);
				}
			}
			ClassificationLabel classification = ClassificationLabel.valueOf(classifier.classify(values));
			handler.handleClassification(audioEvent.getTimeStamp(), classification);
			
			for(int i = 0 ; i < 20; i++){
				zeroCrossingRatesDeque.removeFirst();
				energyDeque.removeFirst();
			}
		}
		return true;
	}
	

	@Override
	public void processingFinished() {
	
	}


	
	public static void main(String...args) throws UnsupportedAudioFileException, LineUnavailableException{
		trackStream();
		String music = "/media/data/datasets/Sounds/Music-Speech/music/";
		String speech = "/media/data/datasets/Sounds/Music-Speech/more-speech/";
		trainDataSet(ClassificationLabel.MUSIC,music);
		trainDataSet(ClassificationLabel.SPEECH,speech);
	}
	
	public static void trackStream() throws UnsupportedAudioFileException, LineUnavailableException{
		int sampleRate = 22050;
		
		SpeechMusicDiscriminator rtsmd = new SpeechMusicDiscriminator(new ClassificationResultHandler() {
			@Override
			public void handleClassification(double timeStamp, ClassificationLabel label) {
				String msg = String.format("%.2fs: %s", timeStamp,label.toString().toLowerCase());
				System.out.println(msg);
			}
		});
		AudioFile audioFile = new AudioFile("http://mp3.streampower.be/stubru-high.mp3");
		AudioInputStream stream = audioFile.getMonoStream(sampleRate);
		AudioDispatcher adp = new AudioDispatcher(stream,256,0);
		adp.addAudioProcessor(rtsmd);
		adp.addAudioProcessor(new AudioPlayer(audioFile.getTargetFormat(sampleRate)));
		adp.run();
	}
	
	private static void trainDataSet(final ClassificationLabel currentLabel,String dir) throws UnsupportedAudioFileException{
		File folder = new File(dir);
		File[] listOfFiles = folder.listFiles();
		
		ClassificationResultHandler handler = new ClassificationResultHandler() {
			private int total = 0;
			private int correct = 0;
			@Override
			public void handleClassification(double timeStamp, ClassificationLabel label) {
				total++;
				if(label == currentLabel){
					correct++;
				}
			}
			
			public String toString(){
				return String.format("%.2f%% correct for %s",correct/(double) total,currentLabel);
			}
		};
		for(File file : listOfFiles){
			if(file.getAbsolutePath().endsWith(".wav")){
				SpeechMusicDiscriminator rtsmd = new SpeechMusicDiscriminator(handler,true,currentLabel);
				AudioFile audioFile = new AudioFile(file.getAbsolutePath());
				AudioInputStream stream = audioFile.getMonoStream(22050);
				AudioDispatcher adp = new AudioDispatcher(stream,256,0);
				adp.addAudioProcessor(rtsmd);
				adp.run();
			}
		}
			
		System.out.println(handler.toString());
		System.out.println(trainingSet.toString());
		trainingSet = null;
	}
}
