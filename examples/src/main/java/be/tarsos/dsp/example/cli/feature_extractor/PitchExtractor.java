package be.tarsos.dsp.example.cli.feature_extractor;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

class PitchExtractor implements FeatureExtractorApp, PitchDetectionHandler {

    @Override
    public String name() {
        return "pitch";
    }

    @Override
    public String description() {
        String descr = "\tCalculates pitch in Hz for each block of 2048 samples. \n\tThe output is a semicolon separated list of a timestamp, frequency in hertz and \n\ta probability which describes how pitched the sound is at the given time. ";
        descr += "\n\n\tinput.wav\t\ta readable wav file.";
        descr += "\n\t--detector DETECTOR\tdefaults to FFT_YIN or one of these:\n\t\t\t\t";
        for (PitchProcessor.PitchEstimationAlgorithm algo : PitchProcessor.PitchEstimationAlgorithm.values()) {
            descr += algo.name() + "\n\t\t\t\t";
        }
        return descr;
    }

    @Override
    public String synopsis() {
        String helpString = "[--detector DETECTOR] input.wav";
        return helpString;
    }

    @Override
    public boolean run(String... args) throws UnsupportedAudioFileException, IOException {
        PitchProcessor.PitchEstimationAlgorithm algo = PitchProcessor.PitchEstimationAlgorithm.FFT_YIN;
        String inputFile = args[1];

        if (args.length == 1 || args.length == 3) {
            return false;
        } else if (args.length == 4 && !args[1].equalsIgnoreCase("--detector")) {
            return false;
        } else if (args.length == 4 && args[1].equalsIgnoreCase("--detector")) {
            try {
                algo = PitchProcessor.PitchEstimationAlgorithm.valueOf(args[2].toUpperCase());
                inputFile = args[3];
            } catch (IllegalArgumentException e) {
                //if enum value string is not recognized
                return false;
            }
        }
        File audioFile = new File(inputFile);
        int size = 1024;
        int overlap = 0;
        int samplerate = 16000;
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(audioFile.getAbsolutePath(), 16000, size, overlap);
        dispatcher.addAudioProcessor(new PitchProcessor(algo, samplerate, size, this));
        dispatcher.run();
        return true;
    }

    @Override
    public void handlePitch(PitchDetectionResult pitchDetectionResult,
                            AudioEvent audioEvent) {
        double timeStamp = audioEvent.getTimeStamp();
        float pitch = pitchDetectionResult.getPitch();
        float probability = pitchDetectionResult.getProbability();
        System.out.println(timeStamp + "," + pitch + "," + probability);
    }
}
