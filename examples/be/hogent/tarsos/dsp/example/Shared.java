/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -----------------------------------------------------------
*
*  TarsosDSP is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://tarsos.0110.be/releases/TarsosDSP/
*  
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
* 
*/

package be.hogent.tarsos.dsp.example;

import java.util.Vector;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Shared {
	
	public static Vector<Mixer.Info> getMixerInfo(
			final boolean supportsPlayback, final boolean supportsRecording) {
		final Vector<Mixer.Info> infos = new Vector<Mixer.Info>();
		final Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		for (final Info mixerinfo : mixers) {
			if (supportsRecording
					&& AudioSystem.getMixer(mixerinfo).getTargetLineInfo().length != 0) {
				// Mixer capable of recording audio if target line length != 0
				infos.add(mixerinfo);
			} else if (supportsPlayback
					&& AudioSystem.getMixer(mixerinfo).getSourceLineInfo().length != 0) {
				// Mixer capable of audio play back if source line length != 0
				infos.add(mixerinfo);
			}
		}
		return infos;
	}
	public static String toLocalString(Object info)
	{
		if(!isWindows())
			return info.toString();
		String defaultEncoding = Charset.defaultCharset().toString();
		try
		{
			return new String(info.toString().getBytes("windows-1252"), defaultEncoding);
		}
		catch(UnsupportedEncodingException ex)
		{
			return info.toString();
		}
	}
	private static String OS = null;
	public static String getOsName()
	{
		if(OS == null)
			OS = System.getProperty("os.name");
	    return OS;
	}
	public static boolean isWindows()
	{
	   return getOsName().startsWith("Windows");
	}
}
