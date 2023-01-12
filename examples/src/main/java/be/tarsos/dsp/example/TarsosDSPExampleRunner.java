package be.tarsos.dsp.example;

import be.tarsos.dsp.example.cli.SharedCommandLineUtilities;
import be.tarsos.dsp.example.unverified.OnsetDetector;
import be.tarsos.dsp.example.unverified.Spectrogram;
import be.tarsos.dsp.example.util.Trie;
import org.reflections.Reflections;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;


/**
 * Runs either GUI or CLI example applications.
 */
public class TarsosDSPExampleRunner {

    public static class TarsosDSPExampleChooser extends JFrame {

        public TarsosDSPExampleChooser(List<TarsosDSPExampleStarter> guiStarters){
            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.setLocationRelativeTo(null);
            this.setTitle("TarsosDSP GUI examples");

            JPanel p = new JPanel();
            p.setBorder(new EmptyBorder(10, 10, 10, 10));

            p.setLayout(new GridLayout(0, 3,10,10));
            p.add(new JLabel("Name"));
            p.add(new JLabel("Description"));
            p.add(new JLabel("Start"));

            for(final TarsosDSPExampleStarter starter : guiStarters){
                p.add(new JLabel( starter.name()));
                JTextArea description = new JTextArea( starter.description());
                description.setLineWrap(true);
                description.setWrapStyleWord(true);
                p.add(description);
                JButton button = new JButton( starter.name());
                button.addActionListener(e -> starter.start());
                p.add(button);
            }

            this.add( new JScrollPane(p));
        }
    }

    private static void startChooserGUI(List<TarsosDSPExampleStarter> guiStarters){
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    //ignore failure to set default look en feel;
                }
                JFrame frame = new TarsosDSPExampleChooser(guiStarters);
                frame.pack();
                frame.setSize(640, 480);
                frame.setVisible(true);
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static void startCLIExample(List<TarsosDSPExampleStarter> cliExamples,String... args){
        final Trie applicationTrie = new Trie();
        final Map<String, TarsosDSPExampleStarter> applications = new HashMap<>();

        for(TarsosDSPExampleStarter cliExample : cliExamples){
            applicationTrie.insert(cliExample.name());
            applications.put(cliExample.name(), cliExample);
        }
        String applicationName = args[0];

        if(applicationName.equalsIgnoreCase("list")){
            SharedCommandLineUtilities.printPrefix();
            System.out.println("Use one TarsosDSP command line examples:");
            SharedCommandLineUtilities.printLine();
            for(TarsosDSPExampleStarter starter : cliExamples){
                System.out.println("\t" + starter.name());
                System.out.println("\t\t" + starter.description());
            }
            SharedCommandLineUtilities.printLine();
            System.out.println();
            System.out.println("For example start an example with:");
            System.out.println("java -jar tarsosdsp-examples.jar " + cliExamples.get(0).name());
            System.out.println();
            return;
        }

        //autocomplete application name
        if (!applications.containsKey(applicationName)) {
            Collection<String> completions = applicationTrie.autoComplete(applicationName);
            //found one match, it is the application to start
            if(completions.size()==1){
                applicationName = completions.iterator().next();
            }
        }

        //remove the first application name argument from the arguments
        String[] applicationArguments = new String[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            applicationArguments[i - 1] = args[i];
        }



        //start the CLI application
        if (applications.containsKey(applicationName)) {
            applications.get(applicationName).start(applicationArguments);
        } else {
            System.err.println("Did not find application " + applicationName);
            System.err.print("\tArguments: ");
            for (int i = 0; i < args.length; i++) {
                System.err.print(args[i]);
                System.err.print(" ");
            }
            System.err.println("");
            System.err.println("Known applications:");
            for(TarsosDSPExampleStarter starter : cliExamples){
                System.err.println("\t" + starter.name());
                System.err.println("\t\t" + starter.description());
            }
        }
    }


    public static void main(String... args){


        boolean startGUI = args.length == 0;

        final List<TarsosDSPExampleStarter> guiExamples = new ArrayList<>();
        final List<TarsosDSPExampleStarter> cliExamples = new ArrayList<>();

        //find all example starters, instantiate and add to list
        Reflections reflections = new Reflections("be.tarsos.dsp");
        Set<Class<? extends TarsosDSPExampleStarter>> modules =  reflections.getSubTypesOf(TarsosDSPExampleStarter.class);
        for(Class<? extends TarsosDSPExampleStarter> module : modules) {
            TarsosDSPExampleStarter starter = null;
            try {

                starter = module.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                //should not happen, instantiation should not be a problem
                e.printStackTrace();
            }
            if(starter != null && starter.hasGUI()){
                guiExamples.add(starter);
            }else {
                cliExamples.add(starter);
            }
        }

        if(startGUI){
            startChooserGUI(guiExamples);
        }else{
            startCLIExample(cliExamples,args);
        }
    }
}
