package be.tarsos.dsp.example;

import be.tarsos.dsp.example.unverified.Spectrogram;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TarsosDSPExampleRunner {


    public static void main(String... args){

        final List<TarsosDSPExample> allExamples = new ArrayList<TarsosDSPExample>();

        Spectrogram.SpectrogramStarter s = new Spectrogram.SpectrogramStarter();

        //find all example starters, instantiate and add to list
        Reflections reflections = new Reflections("be.tarsos.dsp");
        Set<Class<? extends TarsosDSPExample>> modules =  reflections.getSubTypesOf(be.tarsos.dsp.example.TarsosDSPExample.class);
        for(Class<? extends TarsosDSPExample> module : modules) {
            try {
                allExamples.add((TarsosDSPExample) module.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                //should not happen, instantiation should not be a problem
                e.printStackTrace();
            }
        }

        System.out.println("test " + allExamples.size());

        for(TarsosDSPExample example : allExamples){
            System.out.println(example.name());
        }
    }
}
