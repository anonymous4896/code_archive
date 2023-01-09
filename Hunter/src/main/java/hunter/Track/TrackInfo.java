package hunter.Track;

import hunter.Preprocess.FeatureBuilder;
import hunter.Utils.TypeHelper;
import hunter.Utils.UnitHelper;
import soot.ArrayType;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;

import java.util.*;

public class TrackInfo {
    //set with the configuration file.
    private String apkName;
    private String modelName;
    private String framework;
    private String inputType; //image, audio, ...

    private ArrayList<Layer> inputLayers;
    private ArrayList<Layer> outputLayers;

    //set through analyzing the apk.
    private String confusedClazzName;
    private HashMap<String, ArrayList<Unit>> sinkUnits; //method name -> unit list (determined during tracking).

    public class Layer {
        public String layerName;
        public String dataType;
        public ArrayList<Value> nameValues;
        public ArrayList<Value> dataValues;
        public ArrayList<Value> shapeValues;
        public ArrayList<Value> indexValues;
        public ArrayList<Value> assignValues;
        public ArrayList<Value> dataOps;

        public Layer() {
            this.layerName = "";
            this.dataType = "";
            this.nameValues = new ArrayList<>();
            this.dataValues = new ArrayList<>();
            this.shapeValues = new ArrayList<>();
            this.dataOps = new ArrayList<>();
            this.assignValues = new ArrayList<>();
            this.indexValues = new ArrayList<>();
        }

        public Layer(String name) {
            this();
            this.layerName = name;
        }

        private String list2String(ArrayList<Value> list) {
            String res = "";
            res += String.format("[");
            for (Value v : list) {
                res += v.toString();
                res += " ";
            }
            res += String.format("]");
            return res;
        }

        @Override
        public String toString() {
            String res = "";
            res += String.format("Layer: %s(%s)\n", layerName, dataType);
            res += String.format("  name values: %s\n", list2String(nameValues));
            res += String.format("  data values: %s\n", list2String(dataValues));
            res += String.format("  data ops: %s\n", list2String(dataOps));
            res += String.format("  shape values: %s\n", list2String(shapeValues));
            res += String.format("  assign values: %s\n", list2String(assignValues));
            res += String.format("  index values: %s\n", list2String(indexValues));
            return res;
        }
    }

    public TrackInfo(String apkName) {
        this.apkName = apkName;
        this.inputLayers = new ArrayList<>();
        this.outputLayers = new ArrayList<>();
        this.sinkUnits = new HashMap<>();
    }

    public void addInputLayer(String name) {
        inputLayers.add(new Layer(name));
    }

    public void addInputLayer(Layer l) {
        inputLayers.add(l);
    }

    public ArrayList<Layer> getInputLayers() {
        return inputLayers;
    }

    public void addOutputLayer(String name) {
        outputLayers.add(new Layer(name));
    }

    public void addOutputLayer(Layer l) {
        outputLayers.add(l);
    }

    public ArrayList<Layer> getOutputLayers() {
        return outputLayers;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getFramework() {
        return framework;
    }

    public void setApkName(String apkName) {
        this.apkName = apkName;
    }

    public String getApkName() {
        return apkName;
    }

    public void setConfusedClazzName(String confusedClazzName) {
        this.confusedClazzName = confusedClazzName;
    }

    public String getConfusedClazzName() {
        return confusedClazzName;
    }

    public void addSink(String label, Unit sink) {
        if (sinkUnits.containsKey(label)) {
            sinkUnits.get(label).add(sink);
        } else {
            sinkUnits.put(label, new ArrayList<>(Collections.singletonList(sink)));
        }
    }

    public void initFromSinks(HashMap<SootMethod, ArrayList<Unit>> sinks) {
        for (SootMethod sootMethod : sinks.keySet()) {
            if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody()) {
                continue;
            }

            for (Unit unit : sinks.get(sootMethod)) {
                InvokeExpr ie = UnitHelper.GetInvokeExpr(unit);
                if (ie != null) {
                    SootMethod sm = ie.getMethod();
                    setFramework(FeatureBuilder.getFramework(FeatureBuilder.getSinkIndex(sm)));
                    addSink(FeatureBuilder.getSinkMethodName(sm), unit);
                }
            }
        }

        if (framework.equals("tf")) {
            for (Unit u : sinkUnits.get("feed")) {
                InvokeExpr ie = UnitHelper.GetInvokeExpr(u);
                if (ie != null) {
                    Value name = ie.getArg(0);
                    Value data = ie.getArg(1);
                    Value shape = ie.getArg(2);
                    Layer l = new Layer();
                    l.nameValues.add(name);
                    l.dataValues.add(data);
                    l.shapeValues.add(shape);

                    if (data.getType() instanceof ArrayType) {
                        l.dataType = TypeHelper.getBaseType((ArrayType) data.getType()).toString();
                    } else {
                        l.dataType = data.getType().toString();
                    }

                    addInputLayer(l);
                }
            }
            for (Unit u : sinkUnits.get("fetch")) {
                InvokeExpr ie = UnitHelper.GetInvokeExpr(u);
                if (ie != null) {
                    Value v = ie.getArg(0);
                    Layer l = new Layer();
                    l.nameValues.add(v);
                    addOutputLayer(l);
                }
            }
        } else if (framework.equals("tflite")) {
            for (Unit u : sinkUnits.get("run")){
                InvokeExpr ie = UnitHelper.GetInvokeExpr(u);
                if (ie != null) {
                    Value data = ie.getArg(0);
                    Layer l = new Layer();
                    l.dataValues.add(data);

                    if (data.getType() instanceof ArrayType) {
                        l.dataType = TypeHelper.getBaseType((ArrayType) data.getType()).toString();
                    } else {
                        l.dataType = data.getType().toString();
                    }

                    addInputLayer(l);
                }
            }
        }
    }

    public void parseOp(ArrayList<Value> ops, ArrayList<Value> write) {
        for (Layer l : inputLayers) {
            if (containAny(l.dataValues, write)) {
                for (Value v : ops) {
                    if (!l.dataOps.contains(v)) {
                        l.dataOps.add(v);
                    }
                }
            }
        }
    }

    /**
     * search if list contains any value from toSearch
     *
     * @param list
     * @param toSearch
     * @return
     */
    private boolean containAny(ArrayList<Value> list, ArrayList<Value> toSearch) {
        for (Value v : toSearch) {
            if (list.contains(v)) {
                return true;
            }
        }
        return false;
    }

    public void updateValues(ArrayList<Value> assigns, ArrayList<Value> indexs, ArrayList<Value> write){
        for (Layer l : inputLayers) {
            if (containAny(l.dataValues, write)) {
                for (Value v : assigns) {
                    if (!l.assignValues.contains(v)) {
                        l.assignValues.add(v);
                    }
                }
                for (Value v : indexs) {
                    if (!l.indexValues.contains(v)) {
                        l.indexValues.add(v);
                    }
                }
            }
        }
    }

    public void parseValuesForInput(ArrayList<Value> read, ArrayList<Value> constants, ArrayList<Value> write) {
        for (Layer l : inputLayers) {
            if (containAny(l.nameValues, write)) {
                for (Value v : read) {
                    if (!l.nameValues.contains(v)) {
                        l.nameValues.add(v);
                    }
                }
                for (Value v : constants) {
                    if (!l.nameValues.contains(v)) {
                        l.nameValues.add(v);
                    }
                }
            }
            if (containAny(l.dataValues, write)) {
                for (Value v : read) {
                    if (!l.dataValues.contains(v)) {
                        l.dataValues.add(v);
                    }
                }
                for (Value v : constants) {
                    if (!l.dataValues.contains(v)) {
                        l.dataValues.add(v);
                    }
                }
            }
            if (containAny(l.shapeValues, write)) {
                for (Value v : read) {
                    if (!l.shapeValues.contains(v)) {
                        l.shapeValues.add(v);
                    }
                }
                for (Value v : constants) {
                    if (!l.shapeValues.contains(v)) {
                        l.shapeValues.add(v);
                    }
                }
            }
        }
    }

    public void print() {
        System.out.println("====== Track Info ======");
        System.out.println(String.format("APK name: %s", apkName));
        System.out.println(String.format("Model name: %s", modelName));
        System.out.println(String.format("Framework: %s", framework));
        System.out.println(String.format("Input Type: %s", inputType));
        for (String s : sinkUnits.keySet()) {
            System.out.println(String.format("Sink: %s", s));
            for (Unit u : sinkUnits.get(s)) {
                System.out.println(String.format("  %s", u));
            }
        }
        for (Layer l : inputLayers) {
            System.out.println(String.format("Input %s", l.toString()));
        }
        for (Layer l : outputLayers) {
            System.out.println(String.format("Output %s", l.toString()));
        }
    }
}