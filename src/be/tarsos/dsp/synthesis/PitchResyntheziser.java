package be.hogent.tarsos.dsp.synthesis;

import java.util.Arrays;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.EnvelopeFollower;
import be.hogent.tarsos.dsp.pitch.PitchDetectionHandler;
import be.hogent.tarsos.dsp.pitch.PitchDetectionResult;

/**
 * This pitch detection handler replaces the audio buffer in the pipeline with a
 * synthesized wave. It either follows the envelope of the original signal or
 * not. Use it wisely. The following demonstrates how it can be used.
 * 
 * <pre>
 * <code>
 * PitchEstimationAlgorithm algo = PitchEstimationAlgorithm.FFT_YIN;
 * PitchResyntheziser prs = new PitchResyntheziser(samplerate);
 * AudioDispatcher dispatcher = AudioDispatcher.fromFile(new File("in.wav"),1024, 0);
 * //Handle pitch detection
 * dispatcher.addAudioProcessor(new PitchProcessor(algo, samplerate, size, prs));
 * //Write the synthesized pitch to an output file.
 * dispatcher.addAudioProcessor(new WaveformWriter(format, "out.wav"));//
 * dispatcher.run();
 * </code>
 * </pre>
 * 
 * @author Joren Six
 */
public class PitchResyntheziser implements PitchDetectionHandler {

	private double phase = 0;
	private double phaseFirst = 0;
	private double phaseSecond = 0;
	private double prevFrequency = 0;
	private float samplerate;
	private final EnvelopeFollower envelopeFollower;
	private boolean usePureSine;
	private boolean followEnvelope;
	private final double[] previousFrequencies;
	private int previousFrequencyIndex;
	
	public PitchResyntheziser(float samplerate){
		this(samplerate,true,false);
	}
	
	public PitchResyntheziser(float samplerate,boolean followEnvelope,boolean pureSine){
		this(samplerate,followEnvelope,pureSine,5);
	}
	
	public PitchResyntheziser(float samplerate,boolean followEnvelope,boolean pureSine,int filterSize){
		envelopeFollower = new EnvelopeFollower(samplerate,0.005,0.01);
		this.followEnvelope=followEnvelope;
		this.usePureSine = pureSine;
		this.samplerate = samplerate;
		previousFrequencies = new double[filterSize];
		previousFrequencyIndex = 0;
	}
	
	@Override
	public void handlePitch(PitchDetectionResult pitchDetectionResult,
			AudioEvent audioEvent) {
		double frequency = pitchDetectionResult.getPitch();
		
		if(frequency==-1){
			frequency=prevFrequency;
		}else{
			if(previousFrequencies.length!=0){
				//median filter
				//store and adjust pointer
				previousFrequencies[previousFrequencyIndex] = frequency;
				previousFrequencyIndex++;
				previousFrequencyIndex %= previousFrequencies.length;
				//sort to get median frequency
				double[] frequenciesCopy = previousFrequencies.clone();
				Arrays.sort(frequenciesCopy);
				//use the median as frequency
				frequency = frequenciesCopy[frequenciesCopy.length/2];
			}
			
			prevFrequency = frequency;
		}
		
		
	
		final double twoPiF = 2 * Math.PI * frequency;
		float[] audioBuffer = audioEvent.getFloatBuffer();
		float[] envelope = null;
		if(followEnvelope){
			envelope = audioBuffer.clone();
			envelopeFollower.calculateEnvelope(envelope);
		}
		
		for (int sample = 0; sample < audioBuffer.length; sample++) {
			double time =   sample / samplerate;
			double wave =  Math.sin(twoPiF * time + phase);
			if(!usePureSine){
				wave += 0.05 * Math.sin(twoPiF * 4 * time + phaseFirst);
				wave += 0.01 * Math.sin(twoPiF * 8 * time + phaseSecond);
			}			
			audioBuffer[sample] = (float) wave;
			if(followEnvelope){
				audioBuffer[sample] = audioBuffer[sample] * envelope[sample];
			}
		}
		
		double timefactor = twoPiF * audioBuffer.length / samplerate; 
		phase =  timefactor + phase;
		if(!usePureSine){
			phaseFirst = 4 * timefactor + phaseFirst;
			phaseSecond = 8 * timefactor + phaseSecond;
		}
	}
}
