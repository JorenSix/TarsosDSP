package be.tarsos.dsp.example.cli.feature_extractor;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

class SoundPressureLevelExtractor implements FeatureExtractorApp {

    @Override
    public String name() {
        return "sound_pressure_level";
    }

    @Override
    public String description() {
        return "\tCalculates a sound pressure level in dB for each\n\tblock of 2048 samples.The output gives you a timestamp and a value in dBSPL.\n\tSeparated by a semicolon.\n\n\t\n\nWith input.wav\ta readable audio file.";
    }

    @Override
    public String synopsis() {
        return "input.wav";
    }

    @Override
    public boolean run(String... args) throws UnsupportedAudioFileException, IOException {
        if (args.length != 2) {
            return false;
        }

        String inputFile = args[1];
        File audioFile = new File(inputFile);
        int size = 2048;
        int overlap = 0;
        final SilenceDetector silenceDetecor = new SilenceDetector();
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(audioFile.getAbsolutePath(), 16000, size, overlap);
        dispatcher.addAudioProcessor(silenceDetecor);
        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public void processingFinished() {
            }

            @Override
            public boolean process(AudioEvent audioEvent) {
                System.out.println(audioEvent.getTimeStamp() + "," + silenceDetecor.currentSPL());
                return true;
            }
        });
        dispatcher.run();
        return true;
    }
}
