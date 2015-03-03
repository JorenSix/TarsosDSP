package be.tarsos.dsp.io.patcher;

import java.lang.reflect.Method;

import be.tarsos.dsp.pitch.FastYin;
import be.tarsos.dsp.pitch.PitchDetectionResult;

import com.cycling74.max.DataTypes;
import com.cycling74.msp.MSPObject;
import com.cycling74.msp.MSPSignal;

public class tarsosdsppitch  extends MSPObject {
	
	private FastYin yin;
	private float[] audioBuffer;
	private int waterMark;
	
	public tarsosdsppitch(){
		//The signal
		 declareInlets( new int[] { SIGNAL } );
		 // A pitch in Hertz
	     declareOutlets( new int[] { DataTypes.FLOAT} );
	     yin = new FastYin(44100, 1024);
	     audioBuffer = new float[1024];
	}

	public Method dsp(MSPSignal[] ins, MSPSignal[] outs) {
        return getPerformMethod("doit");
    }
    
    public void doit(MSPSignal[] ins, MSPSignal[] outs) {
        
       float[] mspAudioBuffer = ins[0].vec;
       
       System.out.println(mspAudioBuffer.length);
       // Shift the array so that there is room for new samples at the beginning
       System.arraycopy(audioBuffer, 0, audioBuffer, mspAudioBuffer.length, audioBuffer.length-mspAudioBuffer.length);
      // Copy the new samples to the beginning
       System.arraycopy(mspAudioBuffer, 0, audioBuffer, 0, mspAudioBuffer.length);
       waterMark += mspAudioBuffer.length;
       //if there is at least one complete buffer, estimate pitch
       float pitch = 0;
       if(waterMark>= audioBuffer.length){
    	   PitchDetectionResult result = yin.getPitch(audioBuffer);
    	   pitch = result.getPitch();
       }
       outs[0].vec[0] = pitch;
    }

}
