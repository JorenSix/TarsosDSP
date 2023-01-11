package be.tarsos.dsp.example;

import be.tarsos.dsp.example.unverified.Spectrogram;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class TarsosDSPExampleRunner {


    public static void main(String... args){

        boolean startGUI = args.length == 0;


        final List<TarsosDSPExampleStarter> allExamples = new ArrayList<TarsosDSPExampleStarter>();

        Spectrogram.SpectrogramStarter s = new Spectrogram.SpectrogramStarter();

        //find all example starters, instantiate and add to list
        Reflections reflections = new Reflections("be.tarsos.dsp");
        Set<Class<? extends TarsosDSPExampleStarter>> modules =  reflections.getSubTypesOf(TarsosDSPExampleStarter.class);
        for(Class<? extends TarsosDSPExampleStarter> module : modules) {
            try {
                TarsosDSPExampleStarter starter = module.getDeclaredConstructor().newInstance();
                allExamples.add(starter);
            } catch (Exception e) {
                //should not happen, instantiation should not be a problem
                e.printStackTrace();
            }
        }

        for(TarsosDSPExampleStarter example : allExamples){
            System.out.println(example.name());
        }
    }
}
