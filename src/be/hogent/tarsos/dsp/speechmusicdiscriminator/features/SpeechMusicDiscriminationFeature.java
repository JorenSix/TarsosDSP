package be.hogent.tarsos.dsp.speechmusicdiscriminator.features;

import java.util.List;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.speechmusicdiscriminator.ClassificationLabel;

/**
 * Defines the interface for a Music/Speech discrimination feature.
 * @author Joren Six
 *
 */
public interface SpeechMusicDiscriminationFeature{
	public double calculateFeature(AudioEvent e,List<Double> zeroCrossingRates,List<Double> energies);
	public String name();
	public double trainedMean(ClassificationLabel label);
	public double trainedVariance(ClassificationLabel label);
}