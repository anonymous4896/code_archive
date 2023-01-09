package hunter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import hunter.Preprocess.FeatureBuilder;
import org.apache.commons.cli.*;

public class Main {
    static Options options = new Options();
    static boolean verbose = false;
    static String outputPath;

    public static void main(String[] args) {
        options.addRequiredOption("i", "input", true, "APK file input.");
        options.addOption("v", "verbose", false, "Turn on debug log.");
        options.addRequiredOption("o", "output", true, "Output directory.");
        options.addRequiredOption("m", "mode", true, "Work mode: generate Java code(-m code) or Control file(-m control).");
        options.addRequiredOption("p", "android-platform", true, "Path of android platform.");

        CommandLine cli = null;
        CommandLineParser cliParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();
        try {
            cli = cliParser.parse(options, args);
        } catch (ParseException e) {
            helpFormatter.printHelp("APK file, output path, work mode, and android platform are must.", options);
            e.printStackTrace();
        }

        if (cli.hasOption("v")){
            verbose = true;
        }
        String apkPath = cli.getOptionValue("i");
        String mode = cli.getOptionValue("m");
        String androidPlatformPath = cli.getOptionValue("p");
        outputPath = cli.getOptionValue("o");

        try {
            System.out.println("Hunter Start...");
            System.out.println(String.format("APK file: %s", apkPath));
            System.out.println(String.format("Verbose mode: %b", verbose));
            System.out.println(String.format("Work mode: %s", mode));
            System.out.println(String.format("Output path: %s", outputPath));
            System.out.println(String.format("Android Platform path: %s", androidPlatformPath));

            long startTime = System.currentTimeMillis();
            Hunter hunter = new Hunter(apkPath, Constant.models, Constant.inputs, Constant.outputs, outputPath, androidPlatformPath, mode, verbose);
            hunter.go();

            long endTime = System.currentTimeMillis();
            System.out.println("Success!");
            System.out.println(String.format("APK file: %s", apkPath));
            System.out.println(String.format("Elapsed: %d s", (endTime - startTime) / 1000));
        } catch (Exception e) {
            System.err.println("Failed");
            System.out.println(String.format("APK file: %s", apkPath));
            e.printStackTrace();
        }
    }
}