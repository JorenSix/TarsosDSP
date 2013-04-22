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
package be.hogent.tarsos.dsp.mfcc;

import java.io.*;
import java.util.StringTokenizer;

public class DCT {

	int f[][];
	int g[][];
	int inv[][];

	static public void main(String args[]) {

		int fm[][] = new int[8][8];

		if ( args.length != 1 ) {
			System.out.println("usage: java DCT <matrix-filename>");
			return;
		}

		File f = new File(args[0]);
		if ( !f.canRead() ) {
			System.out.println("Error! can't open "+args[0]+" for reading");
			return;
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			for ( int i = 0; i < 8; i++ ) {
				String line = br.readLine();
				StringTokenizer tok = new StringTokenizer(line,", ");
				if ( tok.countTokens() != 8 ) {
					System.out.println("Error! File format error: 8 tokens required!");
					throw new IOException("Error");
				}
				for ( int j = 0; j < 8; j++ ) {
					String numstr = tok.nextToken();
					int num = Integer.parseInt(numstr);
					fm[i][j] = num;
				}
			}
		}
		catch ( FileNotFoundException e ) {
			System.out.println("Error! can't create FileReader for "+args[0]);
			return;
		}
		catch ( IOException e ) {
			System.out.println("Error! during read of "+args[0]);
			return;
		}
		catch ( NumberFormatException e ) {
			System.out.println("Error! NumberFormatExecption");
			return;
		}

		DCT dct = new DCT(fm);
		dct.transform();
		dct.printout();
		dct.inverse();
		dct.printoutinv();
	}

	public DCT(int f[][]) {
		this.f = f;
	}

	public void transform() {
		g = new int[8][8];

		for ( int i = 0; i < 8; i++ ) {
			for ( int j = 0; j < 8; j++ ) {
				double ge = 0.0;
				for ( int x = 0; x < 8; x++ ) {
					for ( int y = 0; y < 8; y++ ) {
						double cg1 = (2.0*(double)x+1.0)*(double)i*Math.PI/16.0;
						double cg2 = (2.0*(double)y+1.0)*(double)j*Math.PI/16.0;

						ge += ((double)f[x][y]) * Math.cos(cg1) * Math.cos(cg2);

					}
				}						
				double ci = ((i==0)?1.0/Math.sqrt(2.0):1.0);
				double cj = ((j==0)?1.0/Math.sqrt(2.0):1.0);
				ge *= ci * cj * 0.25;
				g[i][j] = (int)Math.round(ge);
			}
		}
	}


	public void inverse() {
		inv = new int[8][8];

		for ( int x = 0; x < 8; x++ ) {
			for ( int y = 0; y < 8; y++ ) {
				double ge = 0.0;
				for ( int i = 0; i < 8; i++ ) {
					double cg1 = (2.0*(double)x + 1.0)*(double)i*Math.PI/16.0;
					double ci = ((i==0)?1.0/Math.sqrt(2.0):1.0);
					for ( int j = 0; j < 8; j++ ) {
						double cg2 = (2.0*(double)y + 1.0)*(double)j*Math.PI/16.0;
						double cj = ((j==0)?1.0/Math.sqrt(2.0):1.0);
						double cij4 = ci*cj*0.25;
						ge += cij4 * Math.cos(cg1) * Math.cos(cg2) * (double)g[i][j];
					}
				}
				inv[x][y] = (int)Math.round(ge);
			}
		}
	}	
					
	public void printout() {
		for ( int i = 0; i < 8; i++ ) {
			System.out.print("\n");
			for ( int k = 0; k < 8; k++ ) {
				System.out.print(g[i][k]+" ");
			}
		}
	}

	public void printoutinv() {
		for ( int i = 0; i < 8; i++ ) {
			System.out.print("\n");
			for ( int k = 0; k < 8; k++ ) {
				System.out.print(inv[i][k]+" ");
			}
		}
	}	
}

				
