package hunter.Preprocess.Adapters;

import hunter.Constant;
import hunter.Utils.ListOperator;
import hunter.Utils.MethodHelper;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.HashSet;
import java.util.Iterator;

public class DeadInitAdapter extends Adapter {
    private boolean verbose = false;

    public DeadInitAdapter(boolean verbose) {
        super(verbose);
        this.adapterName = "DeadInitAdapter";
    }

    public void go() {
        PrintAdapterLog("Start");

        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (ListOperator._ListFuzzyContains(sootClass.getName(), Constant.specialMethods2Skip)) {
                continue;
            }

            HashSet<SootMethod> deadInits = new HashSet<>();

            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || MethodHelper.isSkippedMethod(sootMethod)) {
                    continue;
                }

                if (sootMethod.isConstructor()) {
                    Iterator<Edge> it = Scene.v().getCallGraph().edgesInto(sootMethod);
                    HashSet<SootMethod> callerExceptSysMethod = new HashSet<>();
                    while (it.hasNext()) {
                        SootMethod caller = it.next().src();
                        if (caller.isConcrete() && caller.hasActiveBody() && !MethodHelper.isSkippedMethod(caller)) {
                            callerExceptSysMethod.add(caller);
                        }
                    }
                    if (callerExceptSysMethod.isEmpty()) {
                        deadInits.add(sootMethod);
                    }
                }
            }

            if (verbose) {
                PrintAdapterLog("old class start");
                PrintAdapterLog("classname: "+sootClass.getName());
                for (SootMethod sootMethod : sootClass.getMethods()) {
                    PrintAdapterLog("  " + sootMethod.getSignature());
                }
                PrintAdapterLog("old class end");
            }

            for (SootMethod sm : deadInits) {
                sootClass.removeMethod(sm);
            }

            if (verbose) {
                PrintAdapterLog("new class start");
                PrintAdapterLog("classname: "+sootClass.getName());
                for (SootMethod sootMethod : sootClass.getMethods()) {
                    PrintAdapterLog("  " + sootMethod.getSignature());
                }
                PrintAdapterLog("new class end");
            }
        }
        PrintAdapterLog("Finish");
    }
}