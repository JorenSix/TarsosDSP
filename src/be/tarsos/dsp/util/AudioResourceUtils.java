package be.hogent.tarsos.dsp.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Some utility functions to handle audio resources.
 * 
 * @author Joren Six
 */
public class AudioResourceUtils {

	private AudioResourceUtils() {
	}

	/**
	 * Returns a more practical audio resource name. E.g. if
	 * http://stream.com/stream.pls is given, the PLS-file is parsed and the
	 * first audio file is returned. It supports PLS, M3U, AXS and XSPF"
	 * 
	 * @param inputResource
	 *            The input resource, a file, URL, PLS-file or M3U-file.
	 * 
	 * @return A more practical audio resource name.
	 */
	public static String sanitizeResource(String inputResource) {
		if (inputResource.toLowerCase().endsWith("pls")) {
			inputResource = parsePLS(inputResource);
		} else if (inputResource.toLowerCase().endsWith("m3u")) {
			inputResource = parseM3U(inputResource);
		} else if (inputResource.toLowerCase().endsWith("asx")){
			inputResource = parseASX(inputResource);
		}  else if (inputResource.toLowerCase().endsWith("xspf")){
			inputResource = parseXSPF(inputResource);
		}
		return inputResource;
	}
	
	private static String parseXSPF(String inputResource){
		String inputFile = "";
		try {
			String contents = readTextFromUrl(new URL(inputResource));
			for (String line : contents.split("\n")) {
				if (line.toLowerCase().contains("href")) {
					String pattern = "(?i)<location>(.*)</location>.*";
					inputFile = line.replaceAll(pattern, "$1");
					break;
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return inputFile;
	}

	private static String parseASX(String inputResource) {
		String inputFile = "";
		try {
			String contents = readTextFromUrl(new URL(inputResource));
			for (String line : contents.split("\n")) {
				if (line.toLowerCase().contains("href")) {
					String pattern = "(?i).*href=\"(.*)\".*";
					inputFile = line.replaceAll(pattern, "$1");
					break;
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return inputFile;
	}

	/**
	 * Parses the PLS file and returns the first file name.
	 * 
	 * @param inputUrl
	 *            The input PLS file.
	 * @return The first file name in the PLS playlist.
	 */
	public static String parsePLS(String inputUrl) {
		String inputFile = "";
		try {
			String plsContents = readTextFromUrl(new URL(inputUrl));
			for (String line : plsContents.split("\n")) {
				if (line.startsWith("File1=")) {
					inputFile = line.replace("File1=", "").trim();
					break;
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return inputFile;
	}

	/**
	 * Parses the M3U file and returns the first file name.
	 * 
	 * @param inputUrl
	 *            The input M3U file.
	 * @return The first file name in the M3U play list.
	 */
	public static String parseM3U(String inputUrl) {
		String inputFile = "";
		try {
			String plsContents = readTextFromUrl(new URL(inputUrl));
			for (String line : plsContents.split("\n")) {
				if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
					inputFile = line.trim();
					break;
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return inputFile;
	}

	/**
	 * Return the text of the file with the given URL. E.g. if
	 * http://test.be/text.txt is given the contents of text.txt is returned.
	 * 
	 * @param url
	 *            The URL.
	 * @return The contents of the file.
	 */
	public static String readTextFromUrl(URL url) {
		StringBuffer fubber = new StringBuffer();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					url.openStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				fubber.append(inputLine).append("\n");
			}
			in.close();
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		return fubber.toString();
	}

}
