package be.tarsos.dsp.io.patcher;

import java.lang.reflect.Method;

import com.cycling74.max.*;
import com.cycling74.msp.*;

public class panner extends MSPObject {
    float left = 1, right = 1;
    
    public panner() {
        declareInlets( new int[] { SIGNAL, DataTypes.ANYTHING } );
        declareOutlets( new int[] { SIGNAL, SIGNAL } );
    }
    
    /**
     * From 0..127
     */
    public void inlet(float val) {
        if ( val > 64 ) {
            right = 1;
            left = ((127-val) / 64);
        } else {
            left = 1;
            right = val / 64;
        }
    }
    
    public Method dsp(MSPSignal[] ins, MSPSignal[] outs) {
        return getPerformMethod("perform");
    }

    public void perform(MSPSignal[] ins, MSPSignal[] outs) {
        for (int i=0;i<ins[0].n;i++) {
            outs[0].vec[i] = ins[0].vec[i] * left;
            outs[1].vec[i] = ins[0].vec[i] * right;
        }
    }
}
