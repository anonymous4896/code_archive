package hunter;

import hunter.Postprocess.CodeGenerate.Optimizer;
import hunter.Postprocess.CodeGenerate.Translator;
import hunter.Postprocess.ControlGenerate.Generator;
import hunter.Preprocess.Adapters.*;
import hunter.Preprocess.FeatureBuilder;
import hunter.Preprocess.InitSinkTask;
import hunter.Utils.GraphHelper;
import hunter.Utils.MethodHelper;
import hunter.Track.Tracker;
import soot.*;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Hunter {
    private boolean verbose;
    private String outputPath;
    private String workMode;
    private String androidPlatformPath;
    private String apkPath;
    private String apkName;
    private HashSet<String> modelNames;
    private ArrayList<String> inputs;
    private ArrayList<String> outputs;
    private ConcurrentHashMap<SootMethod, ArrayList<Unit>> sinks;

    /**
     * @param modelNames: String -> find the sink point to use model.
     * @param inputs:     ArrayList<String> -> the model's inputs
     * @param outputs:    ArrayList<String> -> the model's outputs
     * @description:
     */
    public Hunter(String apkPath, HashSet<String> modelNames, ArrayList<String> inputs, ArrayList<String> outputs, String outputPath, String androidPlatformPath, String workMode, boolean verbose) {
        this.apkPath = apkPath;
        this.modelNames = modelNames;
        this.inputs = inputs;
        this.outputs = outputs;
        this.outputPath = outputPath;
        this.androidPlatformPath = androidPlatformPath;
        this.workMode = workMode;
        this.verbose = verbose;

        String[] apkPaths = apkPath.split(Constant.SPLITTER);
        this.apkName = apkPaths[apkPaths.length - 1];
    }

    public void go() {
        System.out.println("into hunter'go");

        // load sink features
        FeatureBuilder.loadFeatures();
        FeatureBuilder.printFeatures();

        // prepare soot
        _prepareSootEnv(apkPath);

        // execute adapters and rebuild callgraph
        try {
            Adapter[] adapters = new Adapter[]{
                    new ReflectAdapter(false),
                    new AsyncTaskAdapter(false),
                    new KotlinIteratorAdapter(false),
                    new DeadInitAdapter(false),
                    new FieldAdapter(false)
            };
            for (Adapter a : adapters) {
                a.go();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        _buildCallGraph();

        // search for sinks
        _initSinkPoints(modelNames, inputs, outputs);
        HashMap<SootMethod, ArrayList<Unit>> sinksNormal = new HashMap<>();
        sinksNormal.putAll(sinks);

        Tracker tracker = new Tracker(sinksNormal, Scene.v().getCallGraph(), verbose, apkName);
        tracker.go();
        tracker.info.print();
//        GraphHelper.printGraphDot(tracker.info.getApkName(), outputPath, tracker.resultJimpleGraph4Visual);
//        GraphHelper.printGraphPNG(tracker.info.getApkName(), outputPath, tracker.resultJimpleGraph4Visual);

        if(workMode == Constant.WORK_MODE_CODE){
            Optimizer codeOptimizer = new Optimizer(tracker, sinksNormal);
            codeOptimizer.go();

            Translator translator = new Translator(tracker, codeOptimizer, apkName, outputPath);
            translator.go();
            translator.print();
        }else if(workMode.equals(Constant.WORK_MODE_CONTROL)){
            Generator generator = new Generator(tracker, verbose);
            generator.go();
            generator.print();
            generator.outputJson(outputPath);
        }else{
            System.out.println("error: unknown work mode: " + workMode);
        }
    }

    private void _prepareSootEnv(String apkPath) {
        try {
            G.reset();

            Scene.v().releaseCallGraph();
            Scene.v().releasePointsToAnalysis();
            Scene.v().releaseReachableMethods();
            G.v().resetSpark();

            Options.v().set_android_jars(androidPlatformPath);
            Options.v().set_allow_phantom_refs(true);
            Options.v().set_whole_program(true);
            Options.v().set_no_bodies_for_excluded(true);
            Options.v().set_output_format(Options.output_format_n);
            Options.v().set_src_prec(Options.src_prec_apk);
            Options.v().set_process_multiple_dex(true);
            Options.v().ignore_resolution_errors();

            List<String> processDir = new ArrayList<>();
            processDir.add(apkPath);
            Options.v().set_process_dir(processDir);

            Scene.v().loadNecessaryClasses();

            // Call-graph options
//            Options.v().setPhaseOption("cg", "safe-newinstance:true");

            // Enable SPARK call-graph construction
            Options.v().setPhaseOption("cg.spark", "enabled:true");
            Options.v().setPhaseOption("cg.spark", "on");
//            Options.v().setPhaseOption("cg.cha", "enabled:true");
//            Options.v().setPhaseOption("cg.cha", "on");
//            Options.v().setPhaseOption("cg.spark", "simulate-natives:true");

            PackManager.v().getPack("cg").apply();
            PackManager.v().runPacks();

            _buildCallGraph();
        } catch (Exception e) {
            System.err.println(String.format("%s encounters error %s.", apkPath, e.getMessage()));
        }
    }

    private void _buildCallGraph() {
        CallGraphBuilder cgb = new CallGraphBuilder();
        cgb.build();
        System.out.println("CallGraph size: " + cgb.getCallGraph().size());
        System.out.println();
    }

    private void _initSinkPoints(HashSet<String> modelName, ArrayList<String> inputs, ArrayList<String> outputs) {
        ExecutorService executor = Executors.newFixedThreadPool(Constant.DEFAULT_THREAD_NUM);

        sinks = new ConcurrentHashMap<>();
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || MethodHelper.isSkippedMethod(sootMethod)) {
                    continue;
                }

                executor.execute(new InitSinkTask(sootMethod, sinks, modelName, inputs, outputs));
            }
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
        }
    }
}
