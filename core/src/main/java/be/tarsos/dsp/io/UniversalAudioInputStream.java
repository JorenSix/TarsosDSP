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

package be.tarsos.dsp.io;

import java.io.IOException;
import java.io.InputStream;

public class UniversalAudioInputStream implements TarsosDSPAudioInputStream {
	
	private final InputStream underlyingStream;
	private final TarsosDSPAudioFormat format;
	
	public UniversalAudioInputStream(InputStream underlyingInputStream, TarsosDSPAudioFormat format){
		this.underlyingStream = underlyingInputStream;
		this.format = format;
	}

	@Override
	public long skip(long bytesToSkip) throws IOException {
		//the skip probably
		int bytesSkipped = 0;
		for(int i = 0 ; i < bytesToSkip ; i++){
			int theByte = underlyingStream.read();
			if(theByte!=-1){
				bytesSkipped++;
			}
		}
		return bytesSkipped;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return underlyingStream.read(b, off, len);
	}

	@Override
	public void close() throws IOException {
		underlyingStream.close();
	}

	@Override
	public TarsosDSPAudioFormat getFormat() {
		return format;
	}

	@Override
	public long getFrameLength() {
		return -1;
	}

}
