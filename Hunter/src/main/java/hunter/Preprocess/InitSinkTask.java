package hunter.Preprocess;

import hunter.Utils.UnitHelper;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class InitSinkTask implements Runnable {
    SootMethod sootMethod;
    ConcurrentHashMap<SootMethod, ArrayList<Unit>> sinks;
    String framework;
    HashSet<String> modelName;
    ArrayList<String> inputs;
    ArrayList<String> outputs;

    public InitSinkTask(SootMethod sootMethod,
                        ConcurrentHashMap<SootMethod, ArrayList<Unit>> sinks,
                        HashSet<String> modelName,
                        ArrayList<String> inputs, ArrayList<String> outputs) {
        this.sootMethod = sootMethod;
        this.sinks = sinks;
        this.modelName = modelName;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    @Override
    public void run() {
        ArrayList<Unit> methodUnits = new ArrayList<>();

        for (Unit unit : sootMethod.retrieveActiveBody().getUnits()) {
            if (_isStdSink(sootMethod, unit)) {
                methodUnits.add(unit);
                System.out.println(String.format("- Find sink by feature: %s", unit));
            } else if (_containsModel(modelName, unit)) {
                methodUnits.add(unit);
                System.out.println(String.format("- Find sink by model name: %s", unit));
            } else if (_containsInputsOrOutputs(inputs, unit)) {
                methodUnits.add(unit);
                System.out.println(String.format("- Find sink by input name: %s", unit));
            } else if (_containsInputsOrOutputs(outputs, unit)) {
                methodUnits.add(unit);
                System.out.println(String.format("- Find sink by output name: %s", unit));
            }
        }

        if (!methodUnits.isEmpty()) {
            sinks.put(sootMethod, methodUnits);
        }
    }


    private boolean _containsInputsOrOutputs(ArrayList<String> inputOrOutputs, Unit unit) {
        boolean contains = false;
        for (String inputOrOutput : inputOrOutputs) {
            if (unit.toString().contains(inputOrOutput)) {
                contains = true;
                break;
            }
        }
        return contains;
    }

    private boolean _containsModel(HashSet<String> modelNames, Unit unit) {
        for (String m : modelNames) {
            if (unit.toString().contains(m))
                return true;
        }
        return false;
    }


    private boolean _isStdSink(SootMethod sm, Unit unit) {
        if (!unit.toString().contains("<org.tensorflow.lite.Interpreter: void runForMultipleInputsOutputs(java.lang.Object[],java.util.Map)>")) {//todo why runForMultipleInputsOutputs
            for (String s : hunter.Constant.methodBlockList) {
                // filter opencv method
                if (sm.getSignature().equals(s)) {
                    return false;
                }
            }
        }


        if (UnitHelper.GetInvokeExpr(unit) != null) {
            InvokeExpr ie = UnitHelper.GetInvokeExpr(unit);
            SootMethod ieMethod = ie.getMethod();

            if (FeatureBuilder.isSinkPoint(ieMethod)) {
                return true;
            }
        }


//            for (String stdFeature : Constant.sinks4Values.keySet()) {
//                boolean isStdSink = true;
//                String[] tfs = stdFeature.split("#");
//                for (String tf : tfs) {
//                    if (!unit.toString().contains(tf))
//                        isStdSink = false;
//                }
//                if (isStdSink && !unit.toString().contains("Variable"))
//                    return true;
//            }


        return false;
    }

}
