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

package be.tarsos.dsp.example.spectrum;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SpectralPeakProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;


/**
 * Currently unfinished example!
 * @author Joren Six
 *
 */
public class SpectralParabolicInterpolationExample {
	

	
	public static void main(String[] args) throws UnsupportedAudioFileException, IOException{		
		String fileName ="/home/joren/Desktop/desktop/440Hz-44.1kHz.wav";
		fileName = "/home/joren/Desktop/desktop/452Hz-44.1kHz.wav";
		fileName = "/home/joren/Desktop/desktop/430Hz-473Hz-44.1kHz.wav";
		fileName = "/home/joren/Desktop/desktop/440Hz-550Hz-44.1kHz.wav";
		fileName = "/home/joren/Desktop/desktop/440Hz-550Hz+5percent-44.1kHz.wav";
		AudioDispatcher d = AudioDispatcherFactory.fromFile(new File(fileName), 1024, 512);
		final SpectralPeakProcessor spp = new SpectralPeakProcessor(1024, 512, 44100);
		d.addAudioProcessor(spp);
		d.addAudioProcessor(new AudioProcessor() {
			FFT fft = new FFT(1024, new HammingWindow());
			float[] amplitudes = new float[1024/2];
			@Override
			public void processingFinished() {
								
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				float[] buffer = audioEvent.getFloatBuffer().clone();
				fft.forwardTransform(buffer);
				fft.modulus(buffer, amplitudes);
				int maxIndex = -10;
				float maxValue = -20000;
				
				for(int i = 0; i < amplitudes.length; i++){
					 amplitudes[i] = (float) (20 * Math.log10(amplitudes[i]));
				}
				
				for(int i = 0; i < amplitudes.length; i++){
					if(amplitudes[i]>maxValue){
						maxIndex = i;
						maxValue = amplitudes[i];
					}
				}
				
				//float offset = (amplitudes[maxIndex+1] - amplitudes[maxIndex-1])/( 2*(2 * amplitudes[maxIndex] - amplitudes[maxIndex+1] - amplitudes[maxIndex-1] ));
				//offset/=2.0f;
				//excpected offset = + 0.2167
				//float adjustedBin = maxIndex  - offset;
				//System.out.println(adjustedBin * 44100 / 1024.0f);
				
				float alpha,beta,gamma;
				alpha = amplitudes[maxIndex-1];
				beta = amplitudes[maxIndex];
				gamma = amplitudes[maxIndex+1];
				
				float adjustedBinIndex= maxIndex - 1/2.0f * (alpha-gamma)/(2 * alpha - 2*beta + gamma);
				
				System.out.println(adjustedBinIndex * 44100 / 1024.0f + " Hz in stead of " + maxIndex * 44100 / 1024.0f + " Hz" + " phase: " + spp.getFrequencyEstimates()[maxIndex]);
				
				
				
				//System.out.println(maxIndex * 44100 / 1024.0f);
				
				
				
				return true;
			}
		});
		d.run();
		
		
		
	}

}
