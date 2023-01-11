package be.tarsos.dsp.example.cli.feature_extractor;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

public interface FeatureExtractorApp {
    String name();

    String description();

    String synopsis();

    boolean run(String... args) throws UnsupportedAudioFileException, IOException;

}
