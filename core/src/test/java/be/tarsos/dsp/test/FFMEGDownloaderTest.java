package be.tarsos.dsp.test;

import be.tarsos.dsp.util.FFMPEGDownloader;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FFMEGDownloaderTest {

    @Test
    public void testFFMPEGDownload() {
        FFMPEGDownloader d = new FFMPEGDownloader();
        assertTrue(new File(d.ffmpegBinary()).exists());
        assertTrue(new File(d.ffmpegBinary()).length() > 1000);
    }
}
