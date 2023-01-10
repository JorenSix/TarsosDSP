package be.tarsos.dsp.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Logger;

/**
 * Downloads a static ffmpeg binary for several platforms:
 * Windows x64 and x32
 * Max OS X x64
 * Linux x32 and x64
 * The code tries to determine the correct platform and downloads it to 
 * the temporary directory <code>System.getProperty("java.io.tmpdir")</code>.
 * 
 * After downloading it makes the binary executable.
 * The location of the downloaded binary is returned by <code>ffmpegBinary();</code>
 * 
 * @author Joren Six
 */
public class FFMPEGDownloader {
	
	private static String url = "https://0110.be/releases/TarsosDSP/TarsosDSP-static-ffmpeg/";
	
	private final String ffmpegBinary;
	
	private final static Logger LOG = Logger.getLogger(FFMPEGDownloader.class.getName());

	/**
	 * A new FFMPEGDownloader
	 */
	public FFMPEGDownloader(){
		String filename = operatingSystemName() + "_" + processorArchitecture() + "_ffmpeg" + suffix();
		url = url + filename;
	
		String tempDirectory = System.getProperty("java.io.tmpdir");
		String saveTo = new File(tempDirectory,filename).getAbsolutePath();
		
		if(new File(saveTo).exists() && new File(saveTo).length() > 1000){
			LOG.info("Found an already download ffmpeg static binary: " + saveTo);
			ffmpegBinary = saveTo;
		}else{
			LOG.info("Started downloading an ffmpeg static binary from  " + url + " to " + saveTo );
			downloadExecutable(saveTo);
			
			if(new File(saveTo).exists() && new File(saveTo).length() > 1000){
				LOG.info("Downloaded an ffmpeg static binary. Stored at: " + saveTo);
				//make it executable
				new File(saveTo).setExecutable(true);
				ffmpegBinary = saveTo;
			}else{
				//Unable to download or unknown architecture
				LOG.warning("Unable to find or download an ffmpeg static binary.  " + filename);
				ffmpegBinary = null;
			}
		}	
	}

	/**
	 * The path of the downloaded ffmpeg binary or null
	 * @return The path of the downloaded ffmpeg binary or null
	 */
	public String ffmpegBinary(){
		if(ffmpegBinary!=null){
			return ffmpegBinary.replace(suffix(), "");
		}
		return null;
	} 
	
	private void downloadExecutable(String saveTo){
		try{
			URL website = new URL(url);
			ReadableByteChannel rbc = Channels.newChannel(website.openStream());
			FileOutputStream fos = new FileOutputStream(saveTo);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
		}catch(MalformedURLException e){
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	
	private String operatingSystemName(){
		String name;
		String operatingSystem = System.getProperty("os.name").toLowerCase();
		if(operatingSystem.indexOf("indows") > 0 ){
			name = "windows";
		}else if(operatingSystem.indexOf("nux") >= 0){
			name="linux";
		}else if(operatingSystem.indexOf("mac") >= 0){
			name="mac_os_x";
		}else{
			name = null;
		}
		return name;
	}
	
	private String processorArchitecture(){
		boolean is64bit = false;
		if (System.getProperty("os.name").contains("Windows")) {
		    is64bit = (System.getenv("ProgramFiles(x86)") != null);
		} else {
		    is64bit = (System.getProperty("os.arch").indexOf("64") != -1);
		}
		if(is64bit){
			return "64_bits";
		}else{
			return "32_bits";
		}
	}
	
	private String suffix(){
		String suffix = "";
		if (System.getProperty("os.name").contains("Windows")) {
		    suffix = ".exe";
		}
		return suffix;
	}
}
