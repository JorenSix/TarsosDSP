package be.tarsos.dsp.writer;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

/**
 * This class writes the ongoing sound to an output specified by the programmer
 *
 */
public class WriterProcessor implements AudioProcessor {
    RandomAccessFile output;
    TarsosDSPAudioFormat audioFormat;
    private int audioLen=0;
    private  static final int HEADER_LENGTH=44;//byte

    /**
     *
     * @param audioFormat which this processor is attached to
     * @param output randomaccessfile of the output file
     */
    public WriterProcessor(TarsosDSPAudioFormat audioFormat,RandomAccessFile output){
        this.output=output;
        this.audioFormat=audioFormat;
        try {
            output.write(new byte[HEADER_LENGTH]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public boolean process(AudioEvent audioEvent) {
        try {
            audioLen+=audioEvent.getByteBuffer().length;
            //write audio to the output
            output.write(audioEvent.getByteBuffer());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void processingFinished() {
        //write header and data to the result output
        WaveHeader waveHeader=new WaveHeader(WaveHeader.FORMAT_PCM,
                (short)audioFormat.getChannels(),
                (int)audioFormat.getSampleRate(),(short)16,audioLen);//16 is for pcm, Read WaveHeader class for more details
        ByteArrayOutputStream header=new ByteArrayOutputStream();
        try {
            waveHeader.write(header);
            output.seek(0);
            output.write(header.toByteArray());
            output.close();
        }catch (IOException e){
            e.printStackTrace();
        }

    }
}
