package be.hogent.tarsos.dsp.example;

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
