package be.tarsos.dsp.example.cli.feature_extractor;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

class OnsetExtractor implements FeatureExtractorApp, OnsetHandler {

    @Override
    public String name() {
        return "onset";
    }

    @Override
    public String description() {
        String descr = "\tCalculates onsets using a complex domain onset detector. " +
                "\n\tThe output is a semicolon separated list of a timestamp, and a salliance. ";
        descr += "\n\n\tinput.wav\t\ta readable wav file.";
        descr += "";
        return descr;
    }

    @Override
    public String synopsis() {
        String helpString = "input.wav";
        return helpString;
    }

    @Override
    public boolean run(String... args) throws UnsupportedAudioFileException, IOException {
        String inputFile = args[1];
        File audioFile = new File(inputFile);
        int size = 512;
        int overlap = 256;
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(audioFile.getAbsolutePath(), 44100, size, overlap);
        ComplexOnsetDetector detector = new ComplexOnsetDetector(size, 0.7, 0.1);
        detector.setHandler(this);
        dispatcher.addAudioProcessor(detector);

        dispatcher.run();
        return true;
    }

    @Override
    public void handleOnset(double time, double salience) {
        System.out.println(time + "," + salience);
    }
}
