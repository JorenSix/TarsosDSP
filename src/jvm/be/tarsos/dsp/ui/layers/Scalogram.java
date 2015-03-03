package be.tarsos.dsp.ui.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.layers.TooltipLayer.TooltipTextGenerator;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.wavelet.HaarWaveletTransform;
import be.tarsos.dsp.wavelet.lift.Daubechies4Wavelet;

public class Scalogram implements Layer, Runnable, TooltipTextGenerator {
	
	private final String audioFile;
	private TreeMap<Double,ScalogramFrame> features;
	private final CoordinateSystem cs;  
	public Scalogram(CoordinateSystem cs, String audioFile){
		this.audioFile = audioFile;
		this.cs = cs;
		features = null;
		new Thread(this,"Extract Scalogram").start();
	}	

	@Override
	public void draw(Graphics2D graphics) {
		if(features==null){
		return ;
		}
		Map<Double, ScalogramFrame> spectralInfoSubMap = features.subMap(cs.getMin(Axis.X) / 1000.0, cs.getMax(Axis.X) / 1000.0);
		for (Map.Entry<Double, ScalogramFrame> frameEntry : spectralInfoSubMap.entrySet()) {
				double timeStart = frameEntry.getKey();// in seconds
				ScalogramFrame frame = frameEntry.getValue();// in cents
				
				
				for (int level = 0; level < frame.dataPerScale.length; level++) {
					for(int block = 0; block < frame.dataPerScale[level].length ; block++){
						Color color = Color.black;
						float centsStartingPoint = frame.startFrequencyPerLevel[level];
						float centsHeight = frame.stopFrequencyPerLevel[level] - centsStartingPoint;
						// only draw the visible frequency range
						if (centsStartingPoint + centsHeight >= cs.getMin(Axis.Y) && centsStartingPoint <= cs.getMax(Axis.Y)) {
							float factor = Math.abs(frame.dataPerScale[level][block] / frame.currentMax);
							
							double startTimeBlock = timeStart + (block+1) * frame.durationsOfBlockPerLevel[level];
							double timeDuration = frame.durationsOfBlockPerLevel[level];
							
							int greyValue = (int) ( factor* 0.99 * 255);
							greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(startTimeBlock * 1000),
									Math.round(centsStartingPoint),
									(int) Math.round(timeDuration * 1000),
									(int) Math.ceil(centsHeight));
						}
					}
				}
		}
				
	}

	@Override
	public String getName() {
		return "Scalogram";
	}



	@Override
	public void run() {
		AudioDispatcher adp = AudioDispatcherFactory.fromPipe(audioFile, 44100, 131072, 0);
		adp.addAudioProcessor(new AudioProcessor() {
			
			Daubechies4Wavelet wt = new Daubechies4Wavelet();
			TreeMap<Double,ScalogramFrame> calculatigFeatures = new TreeMap<Double, Scalogram.ScalogramFrame>();
			ScalogramFrame prevFrame;
			@Override
			public boolean process(AudioEvent audioEvent) {
			float[] audioBuffer = audioEvent.getFloatBuffer().clone();
				wt.forwardTrans(audioBuffer);
				float currentMax = 0;
				if(prevFrame != null){
					currentMax = prevFrame.currentMax * 0.99f;
				}
				ScalogramFrame currentFrame = new ScalogramFrame(audioBuffer,currentMax);
				calculatigFeatures.put(audioEvent.getTimeStamp(),currentFrame);
				prevFrame = currentFrame;
				return true;
			}
			
			@Override
			public void processingFinished() {
				features = calculatigFeatures;
			}
		});
		adp.run();
	}
	
	private static class ScalogramFrame{
		float[][] dataPerScale;
		float[] durationsOfBlockPerLevel;
		float[] startFrequencyPerLevel;//cents
		float[] stopFrequencyPerLevel;//cents
		
		float currentMax;
		
		public ScalogramFrame(float[] transformedData, float currentMax){
			this.currentMax = currentMax;
			int levels = HaarWaveletTransform.log2(transformedData.length);
			dataPerScale = new float[levels][];
			durationsOfBlockPerLevel = new float[levels];
			startFrequencyPerLevel = new float[levels];
			stopFrequencyPerLevel = new float[levels];
			for(int i = 0 ; i <levels; i++){
				int samples = HaarWaveletTransform.pow2(i);
				dataPerScale[i] = new float[samples];
				durationsOfBlockPerLevel[i] = (131072/(float) samples)/44100.0f;
				stopFrequencyPerLevel[i] = (float)PitchConverter.hertzToAbsoluteCent(44100/(float)HaarWaveletTransform.pow2(levels-i));
				if(i>0){
					startFrequencyPerLevel[i] = stopFrequencyPerLevel[i-1];
				}
				mra(transformedData,i,dataPerScale);
			}
			
		}
		
		private void mra(float[] transformedData,int level,float[][] dataPerScale){
			int startIndex = (int) (transformedData.length/HaarWaveletTransform.pow2(dataPerScale.length - level));
			int stopIndex = (int) (transformedData.length/HaarWaveletTransform.pow2(dataPerScale.length - level-1));
		
			int j = 0;
			for(int i = startIndex ; i < stopIndex ;i++){
				dataPerScale[level][j] = -1 * transformedData[i];
				j++;
			}
			normalize(dataPerScale[level]);
		}
		
		private void normalize(float[] data){
			for(int i = 0 ; i < data.length ;i++){
				currentMax = Math.max(Math.abs(data[i]),currentMax);
			}
			for(int i = 0 ; i < data.length ;i++){
				//data[i]=data[i]/maxValue;
			}
		}
		
	}

	@Override
	public String generateTooltip(CoordinateSystem cs, Point2D point) {
		return "Scale info";
	}

}
