/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -------------------------------------------------------------
*
* TarsosDSP is developed by Joren Six at IPEM, University Ghent
*  
* -------------------------------------------------------------
*
*  Info: http://0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://0110.be/releases/TarsosDSP/
*  
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.dsp.example;

public  class SharedCommandLineUtilities {
	private SharedCommandLineUtilities(){
	}

	public static final void printPrefix(){
		 System.err.println(" _______                       _____   _____ _____  ");
		 System.err.println("|__   __|                     |  __ \\ / ____|  __ \\ ");
		 System.err.println("   | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |");
		 System.err.println("   | |/ _` | '__/ __|/ _ \\/ __| |  | |\\___ \\|  ___/ ");
		 System.err.println("   | | (_| | |  \\__ \\ (_) \\__ \\ |__| |____) | |     ");
		 System.err.println("   |_|\\__,_|_|  |___/\\___/|___/_____/|_____/|_|     ");
		 System.err.println("                                                    ");
		 SharedCommandLineUtilities.printLine();
	}

	public static void printLine(){
		System.err.println("----------------------------------------------------");
	}
}
