package be.tarsos.dsp.io.android;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

/**
 * Plays audio from an {@link be.tarsos.dsp.AudioDispatcher} or {@link be.tarsos.dsp.AudioGenerator}
 * on an Android {@link AudioTrack}. This class only supports mono or stereo, 16 bit PCM.
 *
 * @author Alex Mikhalev
 * @see AudioTrack
 */
public class AndroidAudioPlayer implements AudioProcessor {
    /**
     * The default stream type to use.
     */
    public static final int DEFAULT_STREAM_TYPE = AudioManager.STREAM_MUSIC;
    private static final String TAG = "AndroidAudioPlayer";
    private final int sampleRate;
    private final int streamType;
    private final int channelConfig;
    private final int encoding;
    private final int bufferSize;
    private final TarsosDSPAudioFloatConverter converter;
    private AudioTrack audioTrack;

    /**
     * Constructs a new AndroidAudioPlayer from an audio format, default buffer size and stream type.
     *
     * @param audioFormat The audio format of the stream that this AndroidAudioPlayer will process.
     *                    This can only be 1 or 2 channels, PCM 16 bit.
     * @param bufferSize  The requested buffer size of the AndroidAudioPlayer. If set to 0 a default
     *                    from the AudioTrack will be chosen.
     * @param streamType  The type of audio stream that the internal AudioTrack should use. For
     *                    example, {@link AudioManager#STREAM_MUSIC}.
     * @throws IllegalArgumentException if audioFormat is not valid
     * @see AudioTrack
     */
    public AndroidAudioPlayer(TarsosDSPAudioFormat audioFormat, int bufferSize, int streamType) {
        sampleRate = (int) audioFormat.getSampleRate();
        this.streamType = streamType;
        if (audioFormat.getChannels() == 1) {
            channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        } else if (audioFormat.getChannels() == 2) {
            channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        } else {
            throw new IllegalArgumentException("Unsupported channel count: " + audioFormat.getChannels());
        }
        if (audioFormat.getEncoding() == TarsosDSPAudioFormat.Encoding.PCM_SIGNED
                || audioFormat.getEncoding() == TarsosDSPAudioFormat.Encoding.PCM_UNSIGNED) {
            if (audioFormat.getSampleSizeInBits() == 16) {
                encoding = AudioFormat.ENCODING_PCM_16BIT;
            } else {
                throw new IllegalArgumentException("Unsupported sample size: " + audioFormat.getSampleSizeInBits());
            }
        } else {
            throw new IllegalArgumentException("Unsupported encoding: " + audioFormat.getEncoding());
        }
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding);
        this.bufferSize = Math.max(bufferSize, minBufferSize);
        this.converter = TarsosDSPAudioFloatConverter.getConverter(audioFormat);
    }

    /**
     * Constructs a new AndroidAudioPlayer from an audio format.
     *
     * @param audioFormat The audio format that this AndroidAudioPlayer will process.
     * @see AndroidAudioPlayer#AndroidAudioPlayer(TarsosDSPAudioFormat, int, int)
     */
    public AndroidAudioPlayer(TarsosDSPAudioFormat audioFormat) {
        this(audioFormat, 0, DEFAULT_STREAM_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean process(AudioEvent audioEvent) {
        if (audioTrack == null) {
            audioTrack = new AudioTrack(streamType, sampleRate, channelConfig, encoding, bufferSize,
                    AudioTrack.MODE_STREAM);
            audioTrack.play();
        }
        int offset = audioEvent.getOverlap();
        int length = audioEvent.getBufferSize() - offset;
        byte[] buffer = new byte[length * 2];
        converter.toByteArray(audioEvent.getFloatBuffer(), offset, length, buffer);
        int ret = audioTrack.write(buffer, 0, buffer.length);
        if (ret < 0) {
            Log.e(TAG, "AudioTrack.write returned error code " + ret);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processingFinished() {
        if (audioTrack != null) {
            audioTrack.flush();
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }
}