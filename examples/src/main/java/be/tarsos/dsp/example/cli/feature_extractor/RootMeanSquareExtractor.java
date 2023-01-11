package be.tarsos.dsp.example.cli.feature_extractor;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

class RootMeanSquareExtractor implements FeatureExtractorApp {

    @Override
    public String name() {
        return "rms";
    }

    @Override
    public String description() {
        return "\tCalculates the root mean square of an audio signal for each \n\tblock of 2048 samples. The output gives you a timestamp and the RMS value,\n\tSeparated by a semicolon.\n\n\t\n\ninput.wav: a\treadable audio file.";
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
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(audioFile.getAbsolutePath(), 16000, size, overlap);

        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public void processingFinished() {
            }

            @Override
            public boolean process(AudioEvent audioEvent) {
                System.out.println(audioEvent.getTimeStamp() + "," + audioEvent.getRMS());
                return true;
            }
        });
        dispatcher.run();
        return true;
    }
}
