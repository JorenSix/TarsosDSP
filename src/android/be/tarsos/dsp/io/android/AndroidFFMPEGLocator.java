package be.tarsos.dsp.io.android;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * <p>
 * The Android FFMPEG locator determines the current CPU architecture of the
 * running Android device and extracts a statically compiled <a href="http://ffmpeg.org">ffmpeg</a>
 * binary from the assets folder to the temporary directory of the currently running Android application.
 * For this to work the assets folder should contain these binaries:
 * </p>
 * 
 * <li>
 * <ul><code>assets/x86_ffmpeg</code> for x86</ul>
 * <ul><code>assets/armeabi-v7a_ffmpeg</code> for armeabi-v7a</ul>
 * <ul><code>assets/armeabi-v7a-neon_ffmpeg</code> for armeabi-v7a-neon</ul>
 * </li>
 * 
 * 
 * <p>
 * You can download these binaries 
 * <a href="https://github.com/hiteshsondhi88/ffmpeg-android/releases/download/v0.3.3/prebuilt-binaries.zip">here</a> 
 * and on the <a href="http://0110.be/releases/TarsosDSP/TarsosDSP-static-ffmpeg/Android/">TarsosDSP ffmpeg repository</a>.
 * Other architectures are currently not supported but could be included in later releases.
 * </p>
 *   
 * <p>
 * If you are a masochist and want to compile ffmpeg for Android yourself you can get your fix <a href="https://github.com/hiteshsondhi88/ffmpeg-android">here</a>
 * </p> 
 * @author Joren Six
 */
public class AndroidFFMPEGLocator {

    private static final String TAG = "AndroidFFMPEGLocator";

    public AndroidFFMPEGLocator(Context context){
        CPUArchitecture architecture = getCPUArchitecture();

        Log.i(TAG,"Detected Native CPU Architecture: " + architecture.name());

        if(!ffmpegIsCorrectlyInstalled()){
            String ffmpegFileName = getFFMPEGFileName(architecture);
            AssetManager assetManager = context.getAssets();
            unpackFFmpeg(assetManager,ffmpegFileName);
        }
        File ffmpegTargetLocation = AndroidFFMPEGLocator.ffmpegTargetLocation();
        Log.i(TAG, "Ffmpeg binary location: " + ffmpegTargetLocation.getAbsolutePath() + " is executable? " + ffmpegTargetLocation.canExecute() + " size: " + ffmpegTargetLocation.length() + " bytes");
    }

    private String getFFMPEGFileName(CPUArchitecture architecture){
        final String ffmpegFileName;
        switch (architecture){
            case X86:
                ffmpegFileName = "x86_ffmpeg";
                break;
            case ARMEABI_V7A:
                ffmpegFileName = "armeabi-v7a_ffmpeg";
                break;
            case ARMEABI_V7A_NEON:
                ffmpegFileName = "armeabi-v7a-neon_ffmpeg";
                break;
            default:
                ffmpegFileName = null;
                String message= "Could not determine your processor architecture correctly, no ffmpeg binary available.";
                Log.e(TAG,message);
                throw new Error(message);
        }
        return ffmpegFileName;
    }

    private boolean ffmpegIsCorrectlyInstalled(){
        File ffmpegTargetLocation = AndroidFFMPEGLocator.ffmpegTargetLocation();
        //assumed to be correct if existing and executable and larger than 1MB:
        return ffmpegTargetLocation.exists() && ffmpegTargetLocation.canExecute() && ffmpegTargetLocation.length() > 1000000;
    }

    private void unpackFFmpeg(AssetManager assetManager,String ffmpegAssetFileName) {
        InputStream inputStream=null;
        OutputStream outputStream=null;
        try{
            File ffmpegTargetLocation = AndroidFFMPEGLocator.ffmpegTargetLocation();
            inputStream = assetManager.open(ffmpegAssetFileName);
            outputStream = new FileOutputStream(ffmpegTargetLocation);
            byte buffer[] = new byte[1024];
            int length = 0;
            while((length=inputStream.read(buffer)) > 0) {
                outputStream.write(buffer,0,length);
            }
            //makes ffmpeg executable
            ffmpegTargetLocation.setExecutable(true);
            Log.i(TAG,"Unpacked ffmpeg binary " + ffmpegAssetFileName + " , extracted  " + ffmpegTargetLocation.length() + " bytes. Extracted to: " + ffmpegTargetLocation.getAbsolutePath());
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            //cleanup
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final File ffmpegTargetLocation(){
        String tempDirectory = System.getProperty("java.io.tmpdir");
        File ffmpegTargetLocation = new File(tempDirectory,"ffmpeg");
        return ffmpegTargetLocation;
    }

    private enum CPUArchitecture{
        X86,ARMEABI_V7A,ARMEABI_V7A_NEON;
    }

    private boolean isCPUArchitectureSupported(String alias) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (String supportedAlias : Build.SUPPORTED_ABIS) {
                if (supportedAlias.equals(alias))
                    return true;
            }

            return false;
        } else {
            return Build.CPU_ABI.equals(alias);
        }
    }

    private CPUArchitecture getCPUArchitecture() {
        // check if device is x86
        if (isCPUArchitectureSupported("x86")) {
            return CPUArchitecture.X86;
        } else if (isCPUArchitectureSupported("armeabi-v7a")) {
            // check if NEON is supported:
            if (isNeonSupported()) {
                return CPUArchitecture.ARMEABI_V7A_NEON;
            } else {
                return CPUArchitecture.ARMEABI_V7A;
            }
        }
        return null;
    }

    private boolean isNeonSupported() {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(new File("/proc/cpuinfo"))));
            String line = null;
            while ((line = input.readLine()) != null) {
                Log.d(TAG, "CPUINFO line: " + line);
                if(line.toLowerCase().contains("neon")) {
                    return true;
                }
            }
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
