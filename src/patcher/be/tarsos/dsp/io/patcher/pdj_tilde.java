package be.tarsos.dsp.io.patcher;

import java.lang.reflect.Method;

import com.cycling74.max.*;
import com.cycling74.msp.*;


public class pdj_tilde extends MSPObject {
    float f = 1;
    boolean debug = false;
    
    public pdj_tilde() {
        int[] inlets = new int[] { MSPObject.SIGNAL, MSPObject.SIGNAL, MSPObject.SIGNAL, DataTypes.FLOAT };
        int[] outlets = new int[] { MSPObject.SIGNAL , MSPObject.SIGNAL, MSPObject.SIGNAL };  
        
        declareInlets(inlets);
        declareOutlets(outlets);
    }

    public void inlet(float f) {

        this.f = f;
        System.out.println(""+f);
    }
    
    public void bang() {
        if ( debug == false )
            debug = true;
        else
            debug = false;
    }
    
    public Method dsp(MSPSignal[] ins, MSPSignal[] outs) {
        return getPerformMethod("doit");
    }
    
    public void doit(MSPSignal[] ins, MSPSignal[] outs) {
        int i;
        if ( debug ) 
            System.out.println("a:" + ins[0].vec[0] + " b:" + ins[1].vec[0] + " c:" + ins[2].vec[0]);
        for(i=0; i<ins[0].n;i++) {
            outs[0].vec[i] = ins[0].vec[i] * f;
            outs[1].vec[i] = ins[1].vec[i] * f;
            outs[2].vec[i] = ins[2].vec[i] * f;
        }
    }
}
