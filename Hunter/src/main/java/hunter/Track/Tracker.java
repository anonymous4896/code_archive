package hunter.Track;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.util.mxCellRenderer;
import heros.solver.Pair;
import hunter.Preprocess.FeatureBuilder;
import hunter.Utils.*;
import hunter.Constant;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocalBox;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.HashMutableEdgeLabelledDirectedGraph;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Tracker {
    // inputs
    private HashMap<SootMethod, ArrayList<Unit>> sinks;
    private CallGraph callGraph;
    private boolean verbose;

    // outputs
    public HashMap<Unit, SootMethod> allUnits2Methods;
    public HashMutableEdgeLabelledDirectedGraph<Unit, String> resultJimpleGraph;
    public Graph<Unit, GraphEdge> resultJimpleGraph4Visual;
    public HashMap<Unit, ArrayList<Unit>> newObjects2Alias; // A = new x.x.x B = A A = A.<init>(param)
    public HashMap<Unit, HashSet<Integer>> methodValueSwitchIndex; // data structure 3
    public HashMap<SootMethod, ArrayList<Pair<Pair<Unit, Integer>, Pair<Unit, Integer>>>> loopRecord;
    public HashMap<Unit, SootMethod> caller2Callee; // data structure 1
    public HashMap<Type, Unit> length2ArrayPair;
    public TrackInfo info;

    // variables
    private HashMap<Integer, HashSet<SootField>> visitedFields;
    private HashMap<Pair<Unit, BriefUnitGraph>, ArrayList<Value>> methodValueContext; // data structure 2
    private HashMap<_Path, Set<Value>> visitedPathMap;
    private HashMap<SootClass, HashMap<SootClass, HashSet<SootMethod>>> superClass2SubClass;
    private HashMap<SootMethod, BriefUnitGraph> method2Graph;
    private ConcurrentHashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> unitOrder;
    private HashMap<String, Value> localAlias;// "method signature + value name" -> local
    private int buildPathDepth;
    private int maxDepth;
    private boolean isAnalyzingPreprocess;
    private HashMap<SootMethod, Integer> methodAccessRecords;

    public Tracker(HashMap<SootMethod, ArrayList<Unit>> sinks, CallGraph callGraph, boolean verbose, String apkName) {
        // inputs
        this.sinks = sinks;
        this.callGraph = callGraph;
        this.verbose = verbose;

        // outputs
        this.allUnits2Methods = new HashMap<>();
        this.resultJimpleGraph = new HashMutableEdgeLabelledDirectedGraph();
        this.resultJimpleGraph4Visual = new DirectedPseudograph<>(GraphEdge.class);
        this.newObjects2Alias = new HashMap<>();
        this.methodValueSwitchIndex = new HashMap<>();
        this.loopRecord = new HashMap<>();
        this.caller2Callee = new HashMap<>();
        this.length2ArrayPair = new HashMap<>();
        this.info = new TrackInfo(apkName);

        // variables
        this.method2Graph = new HashMap<>();
        this.localAlias = new HashMap<>();
        this.methodAccessRecords = new HashMap();
        this.isAnalyzingPreprocess = false;
        this.buildPathDepth = 0;
        this.maxDepth = 0;
        this.visitedFields = new HashMap<>() {
            {
                put(Constant.DIRECTION_FORWARD, new HashSet<>());
                put(Constant.DIRECTION_BACKWARD, new HashSet<>());
            }
        };
        this.methodValueContext = new HashMap<>();
        this.visitedPathMap = new HashMap<>();
        this.superClass2SubClass = new HashMap<>();
    }

    private static boolean _isStdSrc(Unit unit) {
        return false;
    }


    private void prepare() {
        superClass2SubClass = InfoCollector.maintainAbstractReference();
        unitOrder = UnitOrder.maintain();

        LoopHelper loopHelper = new LoopHelper(unitOrder, verbose);
        loopHelper.typeBAdapter();
        loopHelper.pigtailLoopAdapter();

        unitOrder = UnitOrder.updatePartly(unitOrder, loopHelper.methodHasTypeB);

        loopHelper.maintain();
        if (verbose) {
            loopHelper.print();
        }

        loopRecord = loopHelper.loopRecord;
        allUnits2Methods = InfoCollector.maintainUnits2Method();
        method2Graph = InfoCollector.maintainMethod2Graph();
    }

    public HashMutableEdgeLabelledDirectedGraph go() {
        System.out.println("into tracker's go");
        prepare();
        info.initFromSinks(sinks);

        for (SootMethod sootMethod : sinks.keySet()) {
            if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody()) {
                continue;
            }

            BriefUnitGraph method_cfg = method2Graph.get(sootMethod);
            // BriefUnitGraph(sootMethod.retrieveActiveBody());

            for (Unit unit : sinks.get(sootMethod)) {
                HashMap<String, ArrayList<Value>> value2TrackBackward = _extractValues(unit, Constant.EDGE_TYPE_BACKWARD); // extract read and write values.
                HashMap<String, ArrayList<Value>> value2TrackForward = _extractValues(unit, Constant.EDGE_TYPE_FORWARD); // extract read and write values.
                for (Unit nextUnit : method_cfg.getPredsOf(unit)) {
                    // System.out.println("Sink READ -> " + value2Track.get(Constant.READ).toString());
                    // current method cfg,
                    ArrayList<Value> c_values = new ArrayList<>();
                    c_values.addAll(value2TrackBackward.get(Constant.READ));
                    // InvokeExpr invokeExpr = null;
                    // if (unit instanceof AssignStmt && ((AssignStmt)unit).getRightOp() instanceof InvokeExpr) {
                    // invokeExpr = ((AssignStmt) unit).getInvokeExpr();
                    // } else if (unit instanceof InvokeStmt) {
                    // invokeExpr = ((InvokeStmt) unit).getInvokeExpr();
                    // }
                    // if (invokeExpr instanceof InstanceInvokeExpr) {
                    // c_values.add(((InstanceInvokeExpr)invokeExpr).getBase());
                    // }
                    isAnalyzingPreprocess = true;
                    _buildPath(method_cfg, unit, nextUnit, Constant.EDGE_TYPE_BACKWARD, c_values);
                }

                for (Unit nextUnit : method_cfg.getSuccsOf(unit)) {
                    // System.out.println("Sink WRITE -> " + value2Track.get(Constant.WRITE).toString());
                    isAnalyzingPreprocess = false;
                    _buildPath(method_cfg, unit, nextUnit, Constant.EDGE_TYPE_FORWARD, value2TrackForward.get(Constant.WRITE));
                }
            }
        }

        return resultJimpleGraph;
    }


    private HashMap<String, ArrayList<Value>> _extractValues(Unit unit, int edgeType) {
        boolean isInvoke = false;

        HashMap<String, ArrayList<Value>> values = new HashMap<>();

        values.put(Constant.READ, new ArrayList<>());
        values.put(Constant.WRITE, new ArrayList<>());
        values.put(Constant.CONSTANT, new ArrayList<>());
        values.put(Constant.OP, new ArrayList<>());
        values.put(Constant.ASSIGN, new ArrayList<>());
        values.put(Constant.ARRAY_INDEX, new ArrayList<>());

        // assign_stmt = variable "=" rvalue;
        // if A = B, where B is any expr. A is marked as write, and B is marked as read.
        if (unit instanceof AssignStmt) {
            Value rop = ((AssignStmt) unit).getRightOp();
            Value lop = ((AssignStmt) unit).getLeftOp();

            // lvalue = array_ref | instance_field_ref | static_field_ref | local;
            if (lop instanceof ArrayRef) {
                values.get(Constant.WRITE).add(((ArrayRef) lop).getBase());
                values.get(Constant.READ).add(((ArrayRef) lop).getBase());
                if (((ArrayRef) lop).getIndex() instanceof Local) {
                    values.get(Constant.READ).add(((ArrayRef) lop).getIndex());
                }
                values.get(Constant.ARRAY_INDEX).add(((ArrayRef) lop).getIndex());
            } else if (lop instanceof InstanceFieldRef) {
                values.get(Constant.WRITE).add(((InstanceFieldRef) lop).getBase());
                values.get(Constant.READ).add(((InstanceFieldRef) lop).getBase());
                values.get(Constant.WRITE).add(lop);
            } else {
                values.get(Constant.WRITE).add(lop);
            }
            // rvalue = array_ref | constant | expr | instance_field_ref | local |
            // static_field_ref
            if (rop instanceof Expr) {
                // expr = binop_expr | cast_expr | instance_of_expr | invoke_expr |
                // new_array_expr | new_expr |
                // new_multi_array_expr | unop_expr;
                // $i0 = $i3 * $i2
                // $r13 = newarray (int)[$i0]
                if (rop instanceof InvokeExpr) {
                    isInvoke = true;
                    // if the right is sink point, the left value should be removed from the list.
                    if (FeatureBuilder.isSinkPoint(((InvokeExpr) rop).getMethod())) {
                        for (ValueBox vBox : unit.getUseBoxes()) {
                            if (vBox instanceof JimpleLocalBox) {
                                values.get(Constant.WRITE).remove(vBox.getValue());
                            }
                        }
                    }
//                    for (Value v : ((InvokeExpr) rop).getArgs()) {
//                        values.get(Constant.READ).add(v);
//                    }
//                    if(rop instanceof InstanceInvokeExpr){
//                        values.get(Constant.READ).add(((InstanceInvokeExpr) rop).getBase());
//                    }
                    for (ValueBox vBox : rop.getUseBoxes()) {
                        Value v = vBox.getValue();
                        if (v instanceof Local) {
                            values.get(Constant.READ).add(v);
                        }
                    }
                } else if (rop instanceof CastExpr) {
                    if (ListOperator._ListFuzzy2Maintain(((CastExpr) rop).getCastType().toString(), Constant.specialTypeNotFilter,
                            Constant.specialType2Filter) || ((CastExpr) rop).getCastType() instanceof PrimType
                            || TypeHelper.isArrayType(((CastExpr) rop).getCastType())) {
                        values.get(Constant.READ).add(((CastExpr) rop).getOp());
                    }
                } else {
                    for (ValueBox vBox : rop.getUseBoxes()) {
                        Value v = vBox.getValue();
                        if (v instanceof Local) {
                            values.get(Constant.READ).add(v);
                        } else if (v instanceof soot.jimple.Constant) {
                            values.get(Constant.CONSTANT).add(v);
                        }
                    }
                }

                if(rop instanceof BinopExpr){
                    values.get(Constant.OP).add(rop);
                }
            } else if (rop instanceof ArrayRef) {
                // $r6[2]
                // $r6[$i1]
                Value base = ((ArrayRef) rop).getBase();
                Value index = ((ArrayRef) rop).getIndex();
                values.get(Constant.READ).add(base);

                if (index instanceof Local) {
                    values.get(Constant.READ).add(index);
                }

                if (lop instanceof Local && lop.getType() instanceof ArrayType) {
//                    localAlias.put(lop, base);
                    String key = allUnits2Methods.get(unit).getSignature() + "@" + base.toString();
                    localAlias.put(key, lop);
                }
            } else if (rop instanceof InstanceFieldRef) {
                values.get((Constant.READ)).add(((InstanceFieldRef) rop).getBase());
                values.get(Constant.READ).add(rop);
            } else {
                // Local, Constant, StaticFieldRef
                if (rop instanceof soot.jimple.Constant) {
                    values.get(Constant.CONSTANT).add(rop);
                } else {
                    values.get(Constant.READ).add(rop);
                }
                values.get(Constant.ASSIGN).add(rop);
            }
        } else if (unit instanceof InvokeStmt) {
            // virtualinvoke $r11.<android.graphics.Bitmap: void getPixels(int[],int,int,int,int,int,int)>($r13, 0, $i3, 0, 0, $i3, $i2);
            // specialinvoke $r1.<org.tensorflow.contrib.android.a: void <init>(android.content.res.AssetManager,java.lang.String)>($r3, $r2)
            // staticinvoke <kotlin.collections.ArraysKt: java.lang.Iterable withIndex(int[])>($r1)
            // if any usebox in dependencies, all useboxes get in
            isInvoke = true;
            InvokeExpr invokeExpr = ((InvokeStmt) unit).getInvokeExpr();
            if (invokeExpr.getMethod().isConstructor()) {
                for (ValueBox vBox : unit.getUseBoxes()) {
                    if (vBox instanceof JimpleLocalBox) {
                        if (ListOperator._ListFuzzy2Maintain(vBox.getValue().getType().toString(), Constant.specialTypeNotFilter,
                                Constant.specialType2Filter)) {
                            values.get(Constant.WRITE).add(vBox.getValue());
                        }
                    }
                }
                for (Value v : ((InvokeStmt) unit).getInvokeExpr().getArgs()) {
                    values.get(Constant.READ).add(v);
                }
            } else if (Constant.specialMethods4Values.containsKey(((InvokeStmt) unit).getInvokeExpr().getMethod().getSignature())) {
                SootMethod method = ((InvokeStmt) unit).getInvokeExpr().getMethod();

                for (int index : Constant.specialMethods4Values.get(method.getSignature())) {
                    if (index != -1) {
                        values.get(Constant.WRITE).add(((InvokeStmt) unit).getInvokeExpr().getArg(index));
                    } else {
                        for (ValueBox vBox : unit.getUseBoxes()) {
                            if (vBox instanceof JimpleLocalBox) {
                                values.get(Constant.WRITE).add(vBox.getValue());
                            }
                        }
                    }
                }

                // // filter the bitmap field.
                // if (!((InvokeStmt) unit).getInvokeExpr().getMethod().getSignature()
                // .equals("<android.graphics.Bitmap: void
                // getPixels(int[],int,int,int,int,int,int)>")) {
                // for (ValueBox vBox : unit.getUseBoxes()) {
                // if (vBox instanceof JimpleLocalBox) {
                // values.get(Constant.READ).add(vBox.getValue());
                // }
                // }
                // }

                for (Value value : ((InvokeStmt) unit).getInvokeExpr().getArgs()) {
                    values.get(Constant.READ).add(value);
                }
                if (((InvokeStmt) unit).getInvokeExpr() instanceof InstanceInvokeExpr) {
                    values.get(Constant.READ).add(((InstanceInvokeExpr) ((InvokeStmt) unit).getInvokeExpr()).getBase());
                }

            } else if (FeatureBuilder.isSinkPoint(((InvokeStmt) unit).getInvokeExpr().getMethod())) {
                SootMethod method = ((InvokeStmt) unit).getInvokeExpr().getMethod();

                for (int index : FeatureBuilder.getSinkWriteValues(method)) {
                    values.get(Constant.WRITE).add(((InvokeStmt) unit).getInvokeExpr().getArg(index));
                }
                // System.out.println("sink write" + values.get(Constant.WRITE).toString());

                for (Value value : ((InvokeStmt) unit).getInvokeExpr().getArgs()) {
                    values.get(Constant.READ).add(value);
                }
                // System.out.println("sink read" + values.get(Constant.READ).toString());
                // System.out.println();
            } else {
                for (Value v : ((InvokeStmt) unit).getInvokeExpr().getArgs()) {
                    if (v instanceof soot.jimple.Constant) {
                        values.get(Constant.CONSTANT).add(v);
                    } else {
                        values.get(Constant.READ).add(v);
                    }
                }
                for (ValueBox vBox : unit.getUseBoxes()) {
                    if (vBox instanceof JimpleLocalBox) {
                        values.get(Constant.READ).add(vBox.getValue());
                    }
                }

                if (!MethodHelper.isSkippedMethod(invokeExpr.getMethod())) {
                    InvokeExpr ie = ((InvokeStmt) unit).getInvokeExpr();
                    if (ie.getArgCount() == 2) {
                        if (ie.getArg(0).getType().equals(RefType.v("android.graphics.Bitmap")) && ie.getArg(1).getType() instanceof ArrayType || ie.getArg(1).getType().equals(RefType.v("android.graphics.Bitmap")) && ie.getArg(0).getType() instanceof ArrayType) {
                            values.get(Constant.WRITE).add(ie.getArg(0));
                            values.get(Constant.WRITE).add(ie.getArg(1));
                        }
                    }
//                    for (Value v : ((InvokeStmt) unit).getInvokeExpr().getArgs()) {
//                        if (v instanceof Local && v.getType() instanceof ArrayType) {
//                            values.get(Constant.WRITE).add(v);
//                        }
//                    }
                }
            }

            if(invokeExpr.getMethod().getName().startsWith("put")){
                values.get(Constant.OP).add(invokeExpr);
            }
        } else if (unit instanceof IdentityStmt) {
            // $r0 := @this: uk.tensorzoom.browser.c;
            // $r1 := @parameter0: android.graphics.Rect;

            Value lop = ((IdentityStmt) unit).getLeftOp();
            Value rop = ((IdentityStmt) unit).getRightOp();
            if (rop instanceof ParameterRef || rop instanceof ThisRef) {
                values.get(Constant.READ).add(rop);
                values.get(Constant.WRITE).add(lop);
            }
        } else if (unit instanceof IfStmt || unit instanceof SwitchStmt) {
            for (ValueBox vBox : unit.getUseBoxes()) {
                if (vBox.getValue() instanceof Local) {
                    values.get(Constant.READ).add(vBox.getValue());
                } else if (vBox.getValue() instanceof soot.jimple.Constant) {
                    values.get(Constant.CONSTANT).add(vBox.getValue());
                }
            }
        } else if (unit instanceof ReturnStmt) {
            Value value = ((ReturnStmt) unit).getOp();
            if (value instanceof Local) {
                values.get(Constant.READ).add(value);
            } else if (value instanceof soot.jimple.Constant) {
                values.get(Constant.CONSTANT).add(value);
            }
        } else if (unit instanceof GotoStmt) {
            values = _extractValues(((GotoStmt) unit).getTarget(), edgeType);
        } else if (unit instanceof ThrowStmt || unit instanceof ReturnVoidStmt || unit instanceof MonitorStmt
                || unit instanceof NopStmt || unit instanceof BreakpointStmt) {
            // SKIP
        } else {
            System.err.println(String.format("Unknown stmt %s.", unit.toString()));
        }


        // add local alias
        ArrayList<Value> aliasRead = new ArrayList<>();
        for (Value v : values.get(Constant.READ)) {
            String key = allUnits2Methods.get(unit).getSignature() + "@" + v.toString();
            if (localAlias.containsKey(key) && !values.get(Constant.READ).contains(localAlias.get(key))) {
                aliasRead.add(localAlias.get(key));
            }
        }
        if (!aliasRead.isEmpty()) {
            values.get(Constant.READ).addAll(aliasRead);
            System.out.println("log alias read " + aliasRead);
            System.out.println(localAlias);
            System.out.println();
        }
//        ArrayList<Value> aliasWrite = new ArrayList<>();
//        for(Value v : values.get(Constant.WRITE)){
//            String key = allUnits2Methods.get(unit).getSignature() + "@" + v.toString();
//            if(localAlias.containsKey(key) && !values.get(Constant.WRITE).contains(localAlias.get(key))){
//                aliasWrite.add(localAlias.get(key));
//            }
//        }
//        if(!aliasWrite.isEmpty()){
//            values.get(Constant.WRITE).addAll(aliasWrite);
//        }
//
//        if(!aliasWrite.isEmpty() || !aliasRead.isEmpty()){
//            System.out.println("log alias write " + aliasWrite);
//            System.out.println("log alias read " + aliasRead);
//            System.out.println(localAlias);
//            System.out.println();
//        }

        ArrayList<Value> toRemoveRead = new ArrayList<>();
        for (Value v : values.get(Constant.READ)) {
            if ((v instanceof soot.jimple.Constant && !isInvoke)
                    || !(v.getType() instanceof PrimType || v.getType() instanceof ArrayType || ListOperator._ListFuzzy2Maintain(
                    v.getType().toString(), Constant.specialTypeNotFilter, Constant.specialType2Filter))) {
                toRemoveRead.add(v);
            } else if (v.getType() instanceof ArrayType
                    && !ListOperator._ListFuzzy2Maintain(TypeHelper.getBaseType((ArrayType) v.getType()).toString(),
                    Constant.specialTypeNotFilter, Constant.specialType2Filter)
                    && !(TypeHelper.getBaseType((ArrayType) v.getType()) instanceof PrimType)) {

                toRemoveRead.add(v);
            }

        }

        if (isAnalyzingPreprocess) {
            if (unit instanceof InvokeStmt || (unit instanceof AssignStmt && ((AssignStmt) unit).getRightOp() instanceof InvokeExpr)) {
                InvokeExpr invokeExpr = ((Stmt) unit).getInvokeExpr();
                for (String srcStmt : Constant.backwardEndPointValues2Remove.keySet()) {
                    if (unit.toString().contains(srcStmt)) {
                        for (int idx : Constant.backwardEndPointValues2Remove.get(srcStmt)) {
                            if (idx == -1 && invokeExpr instanceof InstanceInvokeExpr) {
                                toRemoveRead.add(((InstanceInvokeExpr) invokeExpr).getBase());
                            } else if (idx >= 0) {
                                toRemoveRead.add(((InvokeExpr) invokeExpr).getArg(idx));
                            }
                        }
                    }
                }
            }
        }
        values.get(Constant.READ).removeAll(toRemoveRead);


        ArrayList<Value> toRemoveWrite = new ArrayList<>();

        if (isAnalyzingPreprocess) {
            if (unit instanceof AssignStmt && ((AssignStmt) unit).getRightOp() instanceof InvokeExpr) {
                for (String srcStmt : Constant.backwardEndPointValues2Remove.keySet()) {
                    if (unit.toString().contains(srcStmt)) {
                        for (int idx : Constant.backwardEndPointValues2Remove.get(srcStmt)) {
                            if (idx == -2) {
                                toRemoveWrite.add(((AssignStmt) unit).getLeftOp());
                            }
                        }
                    }
                }
            }
        }


        for (Value v : values.get(Constant.WRITE)) {
            if (v instanceof soot.jimple.Constant
                    || !(v.getType() instanceof PrimType || v.getType() instanceof ArrayType || ListOperator._ListFuzzy2Maintain(
                    v.getType().toString(), Constant.specialTypeNotFilter, Constant.specialType2Filter))) {
                toRemoveWrite.add(v);
            } else if (v.getType() instanceof ArrayType
                    && !ListOperator._ListFuzzy2Maintain(TypeHelper.getBaseType((ArrayType) v.getType()).toString(),
                    Constant.specialTypeNotFilter, Constant.specialType2Filter)
                    && !(TypeHelper.getBaseType((ArrayType) v.getType()) instanceof PrimType)) {
                toRemoveWrite.add(v);
            }
        }
        values.get(Constant.WRITE).removeAll(toRemoveWrite);

        if (!isAnalyzingPreprocess) {
            if (unit instanceof InvokeStmt || (unit instanceof AssignStmt && ((AssignStmt) unit).getRightOp() instanceof InvokeExpr)) {
                for (String endStmt : Constant.forwardEndPointValues2Remove.keySet()) {
                    if (unit.toString().contains(endStmt)) {
                        values.get(Constant.WRITE).clear();
                    }
                }
            }
        }

        return values;
    }

    /**
     * @param c_unitGraph: the unitGraph of current unit
     * @param c_direction: current search direction
     * @return HashMap<Pair < Unit, BriefUnitGraph>, Integer>: a pair of next unit its
     * unitGraph; the integer is the edge type between current unit and next
     * unit; Edge type: Constant.Constant.EDGE_TYPE_FORWARD,
     * Constant.EDGE_TYPE_BACKWARD, Constant.EDGE_TYPE_CALL_FORWARD,
     * Constant.EDGE_TYPE_CALL_BACKWARD, Constant.EDGE_TYPE_RETURN_FORWARD,
     * Constant.EDGE_TYPE_RETURN_BACKWARD, Constant.EDGE_TYPE_GLOBAL
     */
    private HashMap<Pair<Unit, BriefUnitGraph>, Integer> _findNextNextUnit(BriefUnitGraph c_unitGraph, Unit c_currentUnit, int c_direction) {
        HashMap<Pair<Unit, BriefUnitGraph>, Integer> n_unitGraphType = new HashMap<>();

        if ((c_currentUnit instanceof InvokeStmt || (c_currentUnit instanceof AssignStmt
                && ((AssignStmt) c_currentUnit).getRightOp() instanceof InvokeExpr))
                && !FeatureBuilder.isSinkPoint(((Stmt) c_currentUnit).getInvokeExpr().getMethod())) {

            SootMethod tgtMethod = ((Stmt) c_currentUnit).getInvokeExpr().getMethod();

            // we must view the non-analyzed function as a normal path .
            boolean isSkip = false;
            boolean isAbstract = false;
            SootMethod nextMethod = null;
            if (MethodHelper.isSkippedMethod(tgtMethod) || c_unitGraph.getBody().getMethod().equals(tgtMethod)) { // Change 1
                isSkip = true;
            } else if (!tgtMethod.isConcrete() || !tgtMethod.hasActiveBody()) {
                for (SootClass superClass : superClass2SubClass.keySet()) {
                    for (SootClass subClass : superClass2SubClass.get(superClass).keySet()) {
                        for (SootMethod abstractMethod : superClass2SubClass.get(superClass).get(subClass)) {
                            if (c_currentUnit.toString().contains("<" + superClass.getName())
                                    && c_currentUnit.toString().contains(abstractMethod.getName() + "(")) {

                                String possibleMethodSig = tgtMethod.getSignature().replaceFirst(
                                        java.util.regex.Matcher.quoteReplacement(superClass.getName()),
                                        java.util.regex.Matcher.quoteReplacement(subClass.getName()));
                                for (SootMethod sootMethod : subClass.getMethods()) {
                                    if (sootMethod.getSignature().equals(possibleMethodSig) && sootMethod.isConcrete()
                                            && sootMethod.hasActiveBody()) {
                                        nextMethod = Scene.v().getMethod(possibleMethodSig);
                                        isAbstract = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if (!isAbstract)
                    isSkip = true;
            }

            if (isSkip) {
                if (c_direction == Constant.DIRECTION_BACKWARD) { // unit in unitGraph
                    for (Unit unit : c_unitGraph.getPredsOf(c_currentUnit)) {
                        Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(unit, c_unitGraph);
                        n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_BACKWARD);
                    }
                }
                if (c_direction == Constant.DIRECTION_FORWARD) { // unit in unitGraph
                    for (Unit unit : c_unitGraph.getSuccsOf(c_currentUnit)) {
                        Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(unit, c_unitGraph);
                        n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_FORWARD);
                    }
                }
                processContextSwitch(c_currentUnit, n_unitGraphType);
                return n_unitGraphType;
            }

            BriefUnitGraph n_tgtUnitGraph;
            if (!isAbstract) {
                n_tgtUnitGraph = method2Graph.get(tgtMethod);// new BriefUnitGraph(tgtMethod.retrieveActiveBody());
            } else {
                n_tgtUnitGraph = method2Graph.get(nextMethod);// new BriefUnitGraph(nextMethod.retrieveActiveBody());
            }

            // record the caller point
            caller2Callee.put(c_currentUnit, n_tgtUnitGraph.getBody().getMethod());

            if (c_direction == Constant.DIRECTION_FORWARD) {
                for (Unit n_head : n_tgtUnitGraph.getHeads()) {
                    if (n_head instanceof IdentityStmt && ((IdentityStmt) n_head).getRightOp() instanceof CaughtExceptionRef) {
                        continue;
                    }
                    Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(n_head, n_tgtUnitGraph);
                    n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_CALL_FORWARD);

                }
            } else if (c_direction == Constant.DIRECTION_BACKWARD) {
                Unit retUnit = MethodHelper.chooseReturn(n_tgtUnitGraph.getBody().getMethod());

                if (retUnit != null) {
                    Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(retUnit, n_tgtUnitGraph);
                    n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_CALL_BACKWARD);
                }
                //
                // for (Unit tail : n_tgtUnitGraph.getTails()) {
                // if (tail instanceof JThrowStmt || tail instanceof ReturnVoidStmt
                // || (tail instanceof ReturnStmt && tail.toString().contains("null"))) {
                // continue;
                // }
                // Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(tail, n_tgtUnitGraph);
                // n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_CALL_BACKWARD);
                // }
            }
            processContextSwitch(c_currentUnit, n_unitGraphType);
            return n_unitGraphType;
        }

        if (c_currentUnit instanceof ReturnStmt || c_currentUnit instanceof ReturnVoidStmt) {
            if (c_direction == Constant.DIRECTION_FORWARD) {
                if (caller2Callee.containsValue(c_unitGraph.getBody().getMethod())) {
                    for (Unit key : caller2Callee.keySet()) {
                        if (caller2Callee.get(key).equals(c_unitGraph.getBody().getMethod())) {
                            Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(key, method2Graph.get(allUnits2Methods.get(key)));
                            n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_RETURN_FORWARD); // normal forward
                        }
                    }
                } else {
                    ArrayList<Edge> edges = new ArrayList<>();

                    for (Iterator<Edge> it = callGraph.edgesInto(c_unitGraph.getBody().getMethod()); it.hasNext(); ) {
                        Edge edge = it.next();
                        SootMethod srcMethod = edge.getSrc().method();

                        if (!srcMethod.isConcrete() || !srcMethod.hasActiveBody() || MethodHelper.isSkippedMethod(srcMethod)
                                || !_srcContainsUnit(srcMethod, c_unitGraph.getBody().getMethod()))
                            continue; // This is no use forever,,

                        if (!edge.srcUnit().toString().contains(c_unitGraph.getBody().getMethod().getSubSignature())) {
                            continue;
                        }

                        edges.add(edge);
                    }
                    if (!edges.isEmpty()) {
                        int index = 0;
                        Edge tgtEdge = edges.get(index);

                        caller2Callee.put(tgtEdge.srcUnit(), tgtEdge.tgt());

                        BriefUnitGraph srcGraph = method2Graph.get(tgtEdge.getSrc().method());// new
                        // BriefUnitGraph(srcMethod.retrieveActiveBody());
                        Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(tgtEdge.srcUnit(), srcGraph);
                        n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_RETURN_FORWARD); // normal forward
                    }
                }
            } else if (c_direction == Constant.DIRECTION_BACKWARD) {
                for (Unit n_unit : c_unitGraph.getPredsOf(c_currentUnit)) {
                    // c_unitGraph is the n_unitGraph
                    Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(n_unit, c_unitGraph);
                    n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_BACKWARD);
                }
            }
            processContextSwitch(c_currentUnit, n_unitGraphType);
            return n_unitGraphType;
        }

        if (c_currentUnit instanceof AssignStmt) {
            // Note that the assignStmt will not be the first stmt
            // Here we want to process the field
            Value rop = ((AssignStmt) c_currentUnit).getRightOp();
            Value lop = ((AssignStmt) c_currentUnit).getLeftOp();
            // instance_field_ref | local | static_field_ref

            if (lop instanceof InstanceFieldRef || lop instanceof StaticFieldRef) {
                boolean baseTypeMaintain = true;
                if (lop instanceof InstanceFieldRef) {
                    Type baseType = ((InstanceFieldRef) lop).getBase().getType();
                    for (String prefix : Constant.specialMethods2Skip) {
                        if (baseType.toString().startsWith(prefix)) {
                            baseTypeMaintain = false;
                        }
                    }
                }

                boolean fieldTypeMaintain = false;
                fieldTypeMaintain = TypeHelper.isType2Maintain(((FieldRef) lop).getField().getType()) || (((FieldRef) lop).getField().getType() instanceof ArrayType
                        && TypeHelper.isType2Maintain(TypeHelper.getBaseType((ArrayType) ((FieldRef) lop).getField().getType())));

                if (baseTypeMaintain && fieldTypeMaintain) {
                    if (c_direction == Constant.DIRECTION_FORWARD) {
                        if (!visitedFields.get(Constant.DIRECTION_FORWARD).contains(((FieldRef) lop).getField())) {
                            HashMap<Integer, ArrayList<Unit>> nextUnits = FieldHelper.chooseField(((FieldRef) lop).getField(), Constant.DIRECTION_FORWARD);

                            if (nextUnits.containsKey(Constant.DIRECTION_FORWARD) && nextUnits.get(Constant.DIRECTION_FORWARD).size() != 0) {
                                visitedFields.get(Constant.DIRECTION_FORWARD).add(((FieldRef) lop).getField());
                                for (Unit u : nextUnits.get(Constant.DIRECTION_FORWARD)) {
                                    Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(u,
                                            method2Graph.get(allUnits2Methods.get(u)));
                                    n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_GLOBAL_FORWARD);
                                }
                            }

                            if (nextUnits.containsKey(Constant.DIRECTION_BACKWARD) && nextUnits.get(Constant.DIRECTION_BACKWARD).size() != 0) {
                                visitedFields.get(Constant.DIRECTION_BACKWARD).add(((FieldRef) lop).getField());
                                for (Unit u : nextUnits.get(Constant.DIRECTION_BACKWARD)) {
                                    Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(u, method2Graph.get(allUnits2Methods.get(u)));
                                    n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_GLOBAL_BACKWARD);
                                }
                            }
                        }
                    } else if (c_direction == Constant.DIRECTION_BACKWARD) {
                        for (Unit nextUnit : c_unitGraph.getPredsOf(c_currentUnit)) {
                            Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(nextUnit, c_unitGraph);
                            n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_BACKWARD);
                        }
                    }
                }
            } else if (rop instanceof InstanceFieldRef || rop instanceof StaticFieldRef) {
                boolean baseTypeMaintain = true;
                if (rop instanceof InstanceFieldRef) {
                    Type baseType = ((InstanceFieldRef) rop).getBase().getType();
                    for (String prefix : Constant.specialMethods2Skip) {
                        if (baseType.toString().startsWith(prefix)) {
                            baseTypeMaintain = false;
                        }
                    }
                }

                boolean fieldTypeMaintain = false;
                fieldTypeMaintain = TypeHelper.isType2Maintain(((FieldRef) rop).getField().getType()) || (((FieldRef) rop).getField().getType() instanceof ArrayType
                        && TypeHelper.isType2Maintain(TypeHelper.getBaseType((ArrayType) ((FieldRef) rop).getField().getType())));

                if (baseTypeMaintain && fieldTypeMaintain) {
                    if (c_direction == Constant.DIRECTION_FORWARD) {
                        for (Unit nextUnit : c_unitGraph.getSuccsOf(c_currentUnit)) {
                            Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(nextUnit, c_unitGraph);
                            n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_FORWARD);
                        }
                    } else if (c_direction == Constant.DIRECTION_BACKWARD) {
                        if (!visitedFields.get(Constant.DIRECTION_BACKWARD).contains(((FieldRef) rop).getField())) {
                            HashMap<Integer, ArrayList<Unit>> nextUnits = FieldHelper.chooseField(((FieldRef) rop).getField(), Constant.DIRECTION_BACKWARD);

                            if (!nextUnits.get(Constant.DIRECTION_FORWARD).isEmpty()) {
                                visitedFields.get(Constant.DIRECTION_BACKWARD).add(((FieldRef) rop).getField());
                                for (Unit u : nextUnits.get(Constant.DIRECTION_FORWARD)) {
                                    Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(u, method2Graph.get(allUnits2Methods.get(u)));
                                    n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_GLOBAL_FORWARD);
                                }
                            }

                            if (!nextUnits.get(Constant.DIRECTION_BACKWARD).isEmpty()) {
                                visitedFields.get(Constant.DIRECTION_BACKWARD).add(((FieldRef) rop).getField());
                                for (Unit u : nextUnits.get(Constant.DIRECTION_BACKWARD)) {
                                    Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(u, method2Graph.get(allUnits2Methods.get(u)));
                                    n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_GLOBAL_BACKWARD);
                                }
                            }
                        }
                    }
                }
            }

            if (c_direction == Constant.DIRECTION_BACKWARD && rop instanceof LengthExpr && ((LengthExpr) rop).getOp().getType() instanceof ArrayType && !TypeHelper.isType2Maintain(
                    TypeHelper.getBaseType((ArrayType) ((LengthExpr) rop).getOp().getType()))) {
                ArrayList<Unit> nextUnits = _chooseNewArrayUnits(((LengthExpr) rop).getOp().getType());
                for (Unit u : nextUnits) {
                    Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(u, method2Graph.get(allUnits2Methods.get(u)));
                    n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_GLOBAL_BACKWARD);
                    length2ArrayPair.put(((LengthExpr) rop).getOp().getType(), u);
                }
            }

            // no matter whether there is an field, we must add the normal backward and
            // forward
            if (c_direction == Constant.DIRECTION_BACKWARD) { // unit in unitGraph
                for (Unit unit : c_unitGraph.getPredsOf(c_currentUnit)) {
                    Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(unit, c_unitGraph);
                    n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_BACKWARD);
                }
            } else if (c_direction == Constant.DIRECTION_FORWARD) { // unit in unitGraph
                for (Unit unit : c_unitGraph.getSuccsOf(c_currentUnit)) {
                    Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(unit, c_unitGraph);
                    n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_FORWARD);
                }
            }
            processContextSwitch(c_currentUnit, n_unitGraphType);
            return n_unitGraphType;
        }

        if (c_direction == Constant.DIRECTION_BACKWARD && c_unitGraph.getHeads().contains(c_currentUnit)
                && c_currentUnit instanceof IdentityStmt
                && !(((IdentityStmt) c_currentUnit).getRightOp() instanceof CaughtExceptionRef)) {
            // if (c_unitGraph.getBody().getMethod().getSignature()
            //         .equals("<uk.tensorzoom.c: void <init>(android.content.Context,java.lang.String)>")) {
            //     System.out.println();
            // }
            // return to the caller point
            // && ((IdentityStmt) c_currentUnit).getRightOp() instanceof ParameterRef
            if (caller2Callee.containsValue(c_unitGraph.getBody().getMethod())) {
                for (Unit unit : caller2Callee.keySet()) {
                    if (caller2Callee.get(unit).equals(c_unitGraph.getBody().getMethod())) {
                        Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(unit, method2Graph.get(allUnits2Methods.get(unit)));
                        n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_RETURN_BACKWARD);
                    }
                }
            } else {
                ArrayList<Edge> edges = new ArrayList<>();
                for (Iterator<Edge> it = callGraph.edgesInto(c_unitGraph.getBody().getMethod()); it.hasNext(); ) {
                    Edge edge = it.next();
                    SootMethod srcMethod = edge.src();
                    if (edge.srcUnit() == null) {
                        continue;
                    }

                    if (!srcMethod.isConcrete() || !srcMethod.hasActiveBody() || MethodHelper.isSkippedMethod(srcMethod)
                            || _methodContainsUnit(edge.srcUnit(), c_unitGraph.getBody().getMethod()))
                        continue;

                    if (!edge.srcUnit().toString().contains(c_unitGraph.getBody().getMethod().getSubSignature())) {
                        continue;
                    }

                    edges.add(edge);
                }

                if (!edges.isEmpty()) {
                    int index = 0;
                    Edge tgtEdge = edges.get(index);
                    caller2Callee.put(tgtEdge.srcUnit(), tgtEdge.tgt());
                    Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(tgtEdge.srcUnit(), method2Graph.get(tgtEdge.getSrc().method()));
                    n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_RETURN_BACKWARD);
                }
            }
            processContextSwitch(c_currentUnit, n_unitGraphType);
            return n_unitGraphType;
        }

        if (c_direction == Constant.DIRECTION_BACKWARD) { // unit in unitGraph
            for (Unit unit : c_unitGraph.getPredsOf(c_currentUnit)) {
                Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(unit, c_unitGraph);
                n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_BACKWARD);
            }
        }

        if (c_direction == Constant.DIRECTION_FORWARD) { // unit in unitGraph
            for (Unit unit : c_unitGraph.getSuccsOf(c_currentUnit)) {
                Pair<Unit, BriefUnitGraph> nextPair = new Pair<>(unit, c_unitGraph);
                n_unitGraphType.put(nextPair, Constant.EDGE_TYPE_FORWARD);
            }
        }
        processContextSwitch(c_currentUnit, n_unitGraphType);
        return n_unitGraphType;
    }


    private ArrayList<Unit> _chooseNewArrayUnits(Type type) {
        ArrayList<Unit> units = new ArrayList<>();

        for (Unit unit : allUnits2Methods.keySet()) {
            if (unit instanceof AssignStmt) {
                Value rop = ((AssignStmt) unit).getRightOp();
                Value lop = ((AssignStmt) unit).getLeftOp();

                if (rop instanceof NewArrayExpr && lop.getType().equals(type)) {
                    units.add(unit);
                }
            }
        }
        return units;
    }

    private void _buildPath(BriefUnitGraph n_unitGraph, Unit c_currentUnit, Unit n_nextUnit, int c_n_edgeType, ArrayList<Value> c_values) {
        if (buildPathDepth++ > maxDepth) {
            maxDepth = buildPathDepth;
        }
        _Path p = new _Path(n_unitGraph, c_currentUnit, n_nextUnit, c_n_edgeType);
        if (_isVisited(p, c_values)) {
            buildPathDepth--;
            return;
        }

        ArrayList<Unit> possibleIfUnitCurrent = _getIfUnitsInRelatedLoop(c_currentUnit, method2Graph.get(allUnits2Methods.get(c_currentUnit)));

        if (!possibleIfUnitCurrent.isEmpty() && Tools.isCallingSink(c_currentUnit, new HashSet<>(sinks.keySet()))) {
            return;
        }

        // different edge type.
        if (!resultJimpleGraph.containsNode(c_currentUnit)) {
            resultJimpleGraph.addNode(c_currentUnit);
        }

        HashMap<String, ArrayList<Value>> n_value2Process = _extractValues(n_nextUnit, c_n_edgeType);

//        info.parseValuesForInput(new ArrayList<>(), n_value2Process.get(Constant.CONSTANT), c_values);
//        if(n_value2Process.get(Constant.WRITE).size()> 0){
//            info.parseValuesForInput(n_value2Process.get(Constant.READ), n_value2Process.get(Constant.CONSTANT), n_value2Process.get(Constant.WRITE));
//        }else {
            info.parseValuesForInput(n_value2Process.get(Constant.READ), n_value2Process.get(Constant.CONSTANT), c_values);
//        }
        info.parseOp(n_value2Process.get(Constant.OP), c_values);
        info.updateValues(n_value2Process.get(Constant.ASSIGN), n_value2Process.get(Constant.ARRAY_INDEX), c_values);

        String cValuesHashString = "";
        for (Value v : c_values) {
            cValuesHashString = cValuesHashString + v.toString() + " (" + v.hashCode() + ");";
        }
        String rValuesHashString = "";
        for (Value v : n_value2Process.get(Constant.READ)) {
            rValuesHashString = rValuesHashString + v.toString() + " (" + v.hashCode() + ");";
        }
        String wValuesHashString = "";
        for (Value v : n_value2Process.get(Constant.WRITE)) {
            wValuesHashString = wValuesHashString + v.toString() + " (" + v.hashCode() + ");";
        }
        String constantsHashString = "";
        for (Value v : n_value2Process.get(Constant.CONSTANT)) {
            constantsHashString = constantsHashString + v.toString() + " (" + v.hashCode() + ");";
        }
        System.out.println(String.format(
                "======================== \nCurrent Method: \n\t%s\nCurrent Unit: \n\t%s (%d) \nNext Method: \n\t%s\nNext Unit: \n\t%s (%d) \n"
                        + "Current2Next EdgeType: \n\t%s \nCurrent Values: \n\t%s \nCurrent Values Hash: \n\t%s \n"
                        + "Next Read Values: \n\t%s \nRead Values Hash: \n\t%s \n"
                        + "Next Write Values: \n\t%s \nWrite Values Hash: \n\t%s \n"
                        + "Next Constants: \n\t%s \nConstants Hash: \n\t%s \n"
                        + "======== stack deep : %d; Max deep : %d =========\n",
                allUnits2Methods.get(c_currentUnit), c_currentUnit.toString(), c_currentUnit.hashCode(), allUnits2Methods.get(n_nextUnit), n_nextUnit.toString(), n_nextUnit.hashCode(),
                Constant.EDGE_LABEL[c_n_edgeType], c_values.toString(), cValuesHashString,
                n_value2Process.get(Constant.READ).toString(), rValuesHashString,
                n_value2Process.get(Constant.WRITE).toString(), wValuesHashString,
                n_value2Process.get(Constant.CONSTANT).toString(), constantsHashString,
                buildPathDepth, maxDepth));

        if (c_n_edgeType == Constant.EDGE_TYPE_FORWARD) {

            InvokeExpr ie = UnitHelper.GetInvokeExpr(c_currentUnit);
            if (ie != null) {
                for (String endMethod : Constant.forwardEndPointValues2Remove.keySet()) {
                    if (ie.getMethod().getSignature().equals(endMethod)) {
                        c_values.clear();
                        return;
                    }
                }
            }

            if (c_values.isEmpty()) {
                buildPathDepth--;
                return;
            }

            if (n_nextUnit instanceof IfStmt
                    && ListOperator._valueListIntersection(c_values, _extractValues(n_nextUnit, c_n_edgeType).get(Constant.READ)).isEmpty()
                    && loopRecord.containsKey(n_unitGraph.getBody().getMethod())) {
                boolean trackIf = false;
                for (Pair<Pair<Unit, Integer>, Pair<Unit, Integer>> loopPair : loopRecord.get(n_unitGraph.getBody().getMethod())) {
                    Unit ifUnit = loopPair.getO1().getO1();
                    // Unit ifTgtUnit = loopPair.getO2().getO1();
                    int ifIdx = loopPair.getO1().getO2();
                    ArrayList<Unit> units = new ArrayList<>(n_unitGraph.getBody().getUnits());
                    int targetUnitIdx = units.indexOf(((IfStmt) ifUnit).getTarget());
                    // int ifTgtIdx = loopPair.getO2().getO2();

                    if (ifUnit.equals(n_nextUnit)) {

                        ArrayList<Unit> n_methodUnits = new ArrayList<>(n_unitGraph.getBody().getUnits());
                        for (int i = ifIdx; i <= targetUnitIdx; i++) {
                            if (!ListOperator._valueListIntersection(_extractValues(n_methodUnits.get(i), c_n_edgeType).get(Constant.READ), c_values).isEmpty()) {
                                trackIf = true;
                                break;
                            }
                        }
                        if (trackIf) {
                            _safeAddNode(c_currentUnit);
                            _safeAddNode(n_nextUnit);
                            _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_FORWARD);
                            for (Unit u : n_unitGraph.getPredsOf(n_nextUnit)) {
                                _buildPath(n_unitGraph, n_nextUnit, u, Constant.EDGE_TYPE_BACKWARD,
                                        ListOperator._valueListUnion(_extractValues(n_nextUnit, c_n_edgeType).get(Constant.READ), c_values));
                            }
                            if (!_isStdSrc(c_currentUnit)) {
                                for (Unit u : n_unitGraph.getSuccsOf(n_nextUnit)) {
                                    _buildPath(n_unitGraph, n_nextUnit, u, Constant.EDGE_TYPE_FORWARD, ListOperator
                                            ._valueListUnion(c_values, _extractValues(n_nextUnit, c_n_edgeType).get(Constant.READ)));
                                }
                            }
                        }
                    }
                }
                if (!trackIf) {
                    for (Unit u : n_unitGraph.getSuccsOf(n_nextUnit)) {
                        _buildPath(n_unitGraph, c_currentUnit, u, Constant.EDGE_TYPE_FORWARD, c_values);
                    }
                }
            } else if (n_nextUnit instanceof IfStmt && !ListOperator._valueListIntersection(c_values, _extractValues(n_nextUnit, c_n_edgeType).get(Constant.READ)).isEmpty()) {

                // if(!((IfStmt) n_nextUnit).getTarget().equals(c_currentUnit)){

                _safeAddNode(c_currentUnit);
                _safeAddNode(n_nextUnit);
                _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_FORWARD);

                for (Unit u : n_unitGraph.getPredsOf(n_nextUnit)) {
                    _buildPath(n_unitGraph, n_nextUnit, u, Constant.EDGE_TYPE_BACKWARD,
                            ListOperator._valueListUnion(_extractValues(n_nextUnit, c_n_edgeType).get(Constant.READ), c_values));
                }
                if (!_isStdSrc(c_currentUnit)) {
                    for (Unit u : n_unitGraph.getSuccsOf(n_nextUnit)) {
                        _buildPath(n_unitGraph, n_nextUnit, u, Constant.EDGE_TYPE_FORWARD,
                                ListOperator._valueListUnion(c_values, _extractValues(n_nextUnit, c_n_edgeType).get(Constant.READ)));
                    }
                }
                // }

            } else {
                // we don't use index to taint the value when tracking forwards.
                if (n_nextUnit instanceof AssignStmt) {
                    Value rop = ((AssignStmt) n_nextUnit).getRightOp();
                    if (rop instanceof ArrayRef) {
                        // $r6[2]
                        // $r6[$i1]
                        Value index = ((ArrayRef) rop).getIndex();

                        if (index instanceof Local) {
                            n_value2Process.get(Constant.READ).remove(index);
                        }
                    }
                }

                ArrayList<Value> A_and_B = ListOperator._valueListIntersection(c_values, n_value2Process.get(Constant.READ));
                ArrayList<Value> A_and_C = ListOperator._valueListIntersection(c_values, n_value2Process.get(Constant.WRITE));

                if (A_and_B.isEmpty() && A_and_C.isEmpty()) {
                    if (n_unitGraph.getSuccsOf(n_nextUnit).isEmpty()) {
                        _safeAddNode(c_currentUnit);

                        // HashMap<Pair<Unit, BriefUnitGraph>, Integer> nextNextUnitMapFor = _findNextNextUnit(n_unitGraph,
                        //         n_nextUnit, Constant.DIRECTION_FORWARD);

                        // if (!nextNextUnitMapFor.isEmpty()) {
                        //     _safeAddNode(n_nextUnit);
                        //     _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_FORWARD);
                        //     for (Pair<Unit, BriefUnitGraph> unitPair : nextNextUnitMapFor.keySet()) {
                        //         Unit nextNextUnit = unitPair.getO1();
                        //         BriefUnitGraph nextNextUnitGraph = unitPair.getO2();
                        //         // n_nextUnit is return
                        //         // we add the edge to the return stmt and build the path for nextUnit and
                        //         // nextNextUnit

                        //         int edge_type = nextNextUnitMapFor.get(unitPair);
                        //         if (!_isStdSrc(c_currentUnit) || edge_type != Constant.Constant.EDGE_TYPE_FORWARD) {
                        //             _buildPath(nextNextUnitGraph, n_nextUnit, nextNextUnit, edge_type, c_values);
                        //         }
                        //     }
                        // }
                    } else {
                        if (!_isStdSrc(c_currentUnit)) {
                            for (Unit nextNextUnit : n_unitGraph.getSuccsOf(n_nextUnit)) { // c_currentUnit
                                _buildPath(n_unitGraph, c_currentUnit, nextNextUnit, Constant.EDGE_TYPE_FORWARD, c_values);
                            }
                        }
                    }
                } else if (A_and_B.isEmpty()) {
                    // record this unit
//                    _safeAddNode(c_currentUnit);
//                    _safeAddNode(n_nextUnit);
//                    _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_FORWARD);
                    // if(_isVisited(n_nextUnit, c_n_edgeType, c_values)) return;
                    if (!(n_nextUnit instanceof IdentityStmt)) {
                        HashMap<Pair<Unit, BriefUnitGraph>, Integer> nextNextUnitMapFor = _findNextNextUnit(n_unitGraph, n_nextUnit, Constant.DIRECTION_FORWARD);

                        for (Pair<Unit, BriefUnitGraph> unitPair : nextNextUnitMapFor.keySet()) {
                            Unit nextNextUnit = unitPair.getO1();
                            BriefUnitGraph nextNextUnitGraph = unitPair.getO2();

                            int edge_type = nextNextUnitMapFor.get(unitPair);
                            // if (!_isStdSrc(c_currentUnit) || edge_type != Constant.Constant.EDGE_TYPE_FORWARD) {

                            if (n_nextUnit instanceof AssignStmt && !A_and_C.isEmpty()
                                    && ((AssignStmt) n_nextUnit).getLeftOp() instanceof Local
                                    && ((AssignStmt) n_nextUnit).getRightOp() instanceof FieldRef
                                    && TypeHelper.isArrayType(((AssignStmt) n_nextUnit).getLeftOp().getType())) {
                                _safeAddNode(c_currentUnit);
                                _safeAddNode(n_nextUnit);
                                _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_FORWARD);
                                _buildPath(nextNextUnitGraph, n_nextUnit, nextNextUnit, edge_type, c_values);
                            } else {
                                for (Unit n_n_Unit : n_unitGraph.getSuccsOf(n_nextUnit)) {
                                    _buildPath(nextNextUnitGraph, c_currentUnit, n_n_Unit, Constant.EDGE_TYPE_FORWARD,
                                            ListOperator._valueListComplement(c_values, n_value2Process.get(Constant.WRITE)));
                                }
                            }
//                                _buildPath(nextNextUnitGraph, n_nextUnit, nextNextUnit, edge_type,
//                                        Utils._valueListComplement(c_values, n_value2Process.get(Constant.WRITE)));
                            // }
                        }

//                        HashMap<Pair<Unit, BriefUnitGraph>, Integer> nextNextUnitMapBack = _findNextNextUnit(
//                                n_unitGraph, n_nextUnit, Constant.DIRECTION_BACKWARD);
//
//                        for (Pair<Unit, BriefUnitGraph> unitPair : nextNextUnitMapBack.keySet()) {
//                            Unit nextNextUnit = unitPair.getO1();
//                            BriefUnitGraph nextNextUnitGraph = unitPair.getO2();
//                            int edge_type = nextNextUnitMapBack.get(unitPair);
//                            if (!_isStdSrc(c_currentUnit) || edge_type != Constant.Constant.EDGE_TYPE_FORWARD) {
//                                _buildPath(nextNextUnitGraph, n_nextUnit, nextNextUnit, edge_type,
//                                        n_value2Process.get(Constant.READ));
//                            }
//                        }

                    } else {
                        if (!_isStdSrc(c_currentUnit)) {
                            for (Unit nextNextUnit : n_unitGraph.getSuccsOf(n_nextUnit)) {
                                _buildPath(n_unitGraph, n_nextUnit, nextNextUnit, Constant.EDGE_TYPE_FORWARD, c_values);
                            }
                        }
                    }
                }
                // the following two conditions are the same.
                else { // if (!A_and_B.isEmpty() && A_and_C.isEmpty())
                    _safeAddNode(c_currentUnit);
                    _safeAddNode(n_nextUnit);
                    _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_FORWARD);

                    // if(_isVisited(n_nextUnit, c_n_edgeType, c_values)) return;

                    HashMap<Pair<Unit, BriefUnitGraph>, Integer> nextNextUnitMapFor = _findNextNextUnit(n_unitGraph, n_nextUnit, Constant.DIRECTION_FORWARD);
                    HashMap<Pair<Unit, BriefUnitGraph>, Integer> nextNextUnitMapBack = _findNextNextUnit(n_unitGraph, n_nextUnit, Constant.DIRECTION_BACKWARD);

                    for (Pair<Unit, BriefUnitGraph> unitPair : nextNextUnitMapFor.keySet()) {
                        Unit nextNextUnit = unitPair.getO1();
                        BriefUnitGraph nextNextUnitGraph = unitPair.getO2();

                        int edge_type = nextNextUnitMapFor.get(unitPair);
                        if (!_isStdSrc(c_currentUnit) || edge_type != Constant.EDGE_TYPE_FORWARD) {
                            _buildPath(nextNextUnitGraph, n_nextUnit, nextNextUnit, edge_type, ListOperator._valueListUnion(c_values, n_value2Process.get(Constant.WRITE)));
                        }
                    }

                    for (Pair<Unit, BriefUnitGraph> unitPair : nextNextUnitMapBack.keySet()) {
                        Unit nextNextUnit = unitPair.getO1();
                        BriefUnitGraph nextNextUnitGraph = unitPair.getO2();

                        int edge_type = nextNextUnitMapBack.get(unitPair);
                        if (!_isStdSrc(c_currentUnit) || edge_type != Constant.EDGE_TYPE_FORWARD) {
                            _buildPath(nextNextUnitGraph, n_nextUnit, nextNextUnit, nextNextUnitMapBack.get(unitPair), ListOperator._valueListComplement(n_value2Process.get(Constant.READ), c_values));
                        }
                    }

                }
            }

        } else if (c_n_edgeType == Constant.EDGE_TYPE_BACKWARD) {
            if (c_values.isEmpty()) {
                buildPathDepth--;
                return;
            }

            ArrayList<Unit> units = new ArrayList<>(n_unitGraph.getBody().getUnits());

            // process the object's init at the stmt new.
            if (c_currentUnit instanceof AssignStmt) {
                Value lop = ((AssignStmt) c_currentUnit).getLeftOp();
                Value rop = ((AssignStmt) c_currentUnit).getRightOp();

                if (rop instanceof NewExpr && ListOperator._ListFuzzy2Maintain(lop.getType().toString(), Constant.specialTypeNotFilter, Constant.specialType2Filter)) {
                    SootMethod sootMethod = allUnits2Methods.get(c_currentUnit);
                    ArrayList<Unit> allMethodUnits = new ArrayList<>(sootMethod.retrieveActiveBody().getUnits());
                    int beginIdx = allMethodUnits.indexOf(c_currentUnit);
                    ArrayList<Unit> possibleAliasUnits = new ArrayList<>();
                    for (int i = beginIdx; ; i++) {
                        // success of current.

                        Unit possibleInitUnit = allMethodUnits.get(i);

                        // find the alias of the object between the newStmt and the initStmt

                        if (possibleInitUnit instanceof AssignStmt && ((AssignStmt) possibleInitUnit).getRightOp().equals(lop)) {
                            possibleAliasUnits.add(possibleInitUnit);
                        }
                        InvokeExpr invokeExpr = UnitHelper.GetInvokeExpr(possibleInitUnit);
                        if (invokeExpr instanceof InstanceInvokeExpr
                                && ((InstanceInvokeExpr) invokeExpr).getBase().equals(lop)
                                && invokeExpr.getMethod().isConstructor()) {

                            // possibleInitUnit is realInitUnit

                            // c_currentUnit is the {A = new X.X.X}
                            if (newObjects2Alias.containsKey(possibleInitUnit)
                                    && !newObjects2Alias.get(possibleInitUnit).contains(c_currentUnit)) {
                                newObjects2Alias.get(possibleInitUnit).addAll(possibleAliasUnits);
                            } else {
                                newObjects2Alias.put(possibleInitUnit, possibleAliasUnits);
                            }

                            for (Unit n_predsOfNewStmt : n_unitGraph.getPredsOf(possibleInitUnit)) {
                                // n_unitGraph equals c_unitGraph
                                _buildPath(n_unitGraph, possibleInitUnit, n_predsOfNewStmt, Constant.EDGE_TYPE_BACKWARD, _extractValues(possibleInitUnit, c_n_edgeType).get(Constant.READ));
                            }
                            break;
                        }
                    }
                }
            }

            // (units.indexOf(c_currentUnit) - 1) >= 0 &&
            // units.get(units.indexOf(c_currentUnit) - 1) instanceof GotoStmt
            // from loop_exit_unit to loop_begin_unit
            if (n_nextUnit instanceof IfStmt && loopRecord.containsKey(n_unitGraph.getBody().getMethod())) {
                // _isExitOfLoop(c_currentUnit) &&

                boolean isRealLoop = false;
                for (Pair<Pair<Unit, Integer>, Pair<Unit, Integer>> loopPair : loopRecord.get(n_unitGraph.getBody().getMethod())) {

                    Unit ifUnit = loopPair.getO1().getO1();
                    // Unit ifTgtUnit = loopPair.getO2().getO1();
                    int ifIdx = loopPair.getO1().getO2();
                    // int ifTgtIdx = loopPair.getO2().getO2();

                    int targetUnitIdx = units.indexOf(((IfStmt) ifUnit).getTarget());

                    if (ifUnit.equals(n_nextUnit)) {
                        isRealLoop = true;
                        boolean trackLoop = false;

                        ArrayList<Unit> n_methodUnits = new ArrayList<>(n_unitGraph.getBody().getUnits());
                        // if (!n_methodUnits.get(ifIdx).equals(ifUnit))
                        // System.out.println(String.format("Mismatch!! ifIdx's unit -> %s, ifUnit ->
                        // %s",
                        // n_methodUnits.get(ifIdx).toString(), ifUnit));
                        // if (!n_methodUnits.get(ifTgtIdx).equals(ifTgtUnit))
                        // System.out.println(String.format("Mismatch!! ifTgtIdx's unit -> %s, ifTgtUnit
                        // -> %s",
                        // n_methodUnits.get(ifTgtIdx).toString(), ifTgtUnit));

                        for (int i = targetUnitIdx; i >= ifIdx; i--) {
                            if (!ListOperator._valueListIntersection(_extractValues(n_methodUnits.get(i), c_n_edgeType).get(Constant.WRITE), c_values).isEmpty()) {
                                trackLoop = true;
                                break;
                            }
                        }
                        if (trackLoop) {
                            _safeAddNode(c_currentUnit);
                            _safeAddNode(n_nextUnit);
                            _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_BACKWARD);

                            for (Unit nextNextUnit : n_unitGraph.getPredsOf(n_nextUnit)) {
                                _buildPath(n_unitGraph, n_nextUnit, nextNextUnit, Constant.EDGE_TYPE_BACKWARD,
                                        ListOperator._valueListUnion(_extractValues(n_nextUnit, c_n_edgeType).get(Constant.READ), c_values));
                            }
                        } else {
                            for (Unit nextNextUnit : n_unitGraph.getPredsOf(n_nextUnit)) {
                                _buildPath(n_unitGraph, c_currentUnit, nextNextUnit, Constant.EDGE_TYPE_BACKWARD, c_values);
                            }
                        }
                    }
                }
                if (!isRealLoop) {
                    for (Unit nextNextUnit : n_unitGraph.getPredsOf(n_nextUnit)) {
                        _buildPath(n_unitGraph, c_currentUnit, nextNextUnit, Constant.EDGE_TYPE_BACKWARD, c_values);
                    }
                }
            } else {
                if (!(n_nextUnit instanceof IfStmt) || !((IfStmt) n_nextUnit).getTarget().equals(c_currentUnit)
                        || n_unitGraph.getPredsOf(c_currentUnit).size() == 1) {

                    ArrayList<Value> A_and_C = ListOperator._valueListIntersection(c_values, n_value2Process.get(Constant.WRITE));

                    if (A_and_C.isEmpty()) {
                        if (n_unitGraph.getPredsOf(n_nextUnit).isEmpty()) {

                            HashMap<Pair<Unit, BriefUnitGraph>, Integer> nextNextUnitMap = _findNextNextUnit(n_unitGraph, n_nextUnit, Constant.DIRECTION_BACKWARD);

                            for (Pair<Unit, BriefUnitGraph> unitPair : nextNextUnitMap.keySet()) {
                                Unit nextNextUnit = unitPair.getO1();
                                BriefUnitGraph nextNextUnitGraph = unitPair.getO2();

                                ArrayList<Integer> idx = new ArrayList<>();
                                for (Value v : c_values) {
                                    if (v instanceof ParameterRef) {
                                        idx.add(((ParameterRef) v).getIndex());
                                    }
                                }

                                if (idx.size() != 0) {
                                    _safeAddNode(c_currentUnit);
                                    _safeAddNode(nextNextUnit);
                                    _safeAddEdge(c_currentUnit, nextNextUnit, Constant.EDGE_LABEL_RETURN_BACKWARD);

                                    ArrayList<Value> values2Track = new ArrayList<>();
                                    if (nextNextUnit instanceof InvokeStmt) {
                                        for (int i : idx) {
                                            Value v = ((InvokeStmt) nextNextUnit).getInvokeExpr().getArg(i);
                                            if (!values2Track.contains(v))
                                                values2Track.add(v);
                                        }
                                    }

                                    if (nextNextUnit instanceof AssignStmt) {
                                        for (int i : idx) {
                                            Value v = ((AssignStmt) nextNextUnit).getInvokeExpr().getArg(i);
                                            if (!values2Track.contains(v))
                                                values2Track.add(v);
                                        }
                                    }

                                    info.parseValuesForInput(values2Track, new ArrayList<>(), c_values);//for control

                                    for (Pair<Unit, BriefUnitGraph> pair : methodValueContext.keySet()) {
                                        if (pair.getO1().equals(nextNextUnit)) {
                                            for (Value vStored : methodValueContext.get(pair)) {
                                                InvokeExpr invokeExpr = null;
                                                if (nextNextUnit instanceof AssignStmt && ((AssignStmt) nextNextUnit).getRightOp() instanceof InvokeExpr) {
                                                    invokeExpr = (InvokeExpr) ((AssignStmt) nextNextUnit).getRightOp();
                                                } else if (nextNextUnit instanceof InvokeStmt) {
                                                    invokeExpr = ((InvokeStmt) nextNextUnit).getInvokeExpr();
                                                }
                                                if (invokeExpr != null) {
                                                    if (invokeExpr.getArgs().contains(vStored)) {
                                                        if (!idx.contains(invokeExpr.getArgs().indexOf(vStored)))
                                                            idx.add(invokeExpr.getArgs().indexOf(vStored));
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (methodValueSwitchIndex.containsKey(nextNextUnit)) {
                                        for (int idxStored : methodValueSwitchIndex.get(nextNextUnit)) {
                                            if (!idx.contains(idxStored)) {
                                                idx.add(idxStored);
                                            }
                                        }
                                    }

                                    if (!methodValueSwitchIndex.containsKey(nextNextUnit)) {
                                        methodValueSwitchIndex.put(nextNextUnit, new HashSet<Integer>() {
                                            {
                                                addAll(idx);
                                            }
                                        }); // for translation
                                    } else {
                                        methodValueSwitchIndex.get(nextNextUnit).addAll(idx);
                                    }

                                    // for translation
                                    for (Unit n_n_n_Unit : nextNextUnitGraph.getPredsOf(nextNextUnit)) {
                                        _buildPath(nextNextUnitGraph, nextNextUnit, n_n_n_Unit, Constant.EDGE_TYPE_BACKWARD, values2Track);
                                    }
                                }
                            }
                        } else {
                            boolean conditionflag = true;

                            if (loopRecord.containsKey(n_unitGraph.getBody().getMethod())) {
                                if (n_nextUnit instanceof IfStmt) {
                                    if (resultJimpleGraph.containsNode(n_nextUnit)) {
                                        conditionflag = false;
                                        _safeAddNode(c_currentUnit);
                                        _safeAddNode(n_nextUnit);
                                        _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_BACKWARD);

                                        for (Unit nextNextUnit : n_unitGraph.getPredsOf(n_nextUnit)) {
                                            _buildPath(n_unitGraph, n_nextUnit, nextNextUnit, Constant.EDGE_TYPE_BACKWARD, c_values);
                                        }
                                    }
                                }

                                if (c_currentUnit instanceof IfStmt) {
                                    conditionflag = false;
                                    _safeAddNode(c_currentUnit);
                                    _safeAddNode(n_nextUnit);
                                    _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_BACKWARD);

                                    for (Unit nextNextUnit : n_unitGraph.getPredsOf(n_nextUnit)) {
                                        _buildPath(n_unitGraph, n_nextUnit, nextNextUnit, Constant.EDGE_TYPE_BACKWARD, c_values);
                                    }
                                }
                            }

                            if (conditionflag) {
                                for (Unit nextNextUnit : n_unitGraph.getPredsOf(n_nextUnit)) {
                                    if (!(nextNextUnit instanceof IfStmt)
                                            || !((IfStmt) nextNextUnit).getTarget().equals(n_nextUnit)
                                            || n_unitGraph.getPredsOf(n_nextUnit).size() == 1) {
                                        _buildPath(n_unitGraph, c_currentUnit, nextNextUnit, Constant.EDGE_TYPE_BACKWARD, c_values);
                                    }
                                }
                            }
                        }
                    } else {
                        // record this unit
                        _safeAddNode(c_currentUnit);
                        _safeAddNode(n_nextUnit);
                        _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_BACKWARD);

                        HashMap<Pair<Unit, BriefUnitGraph>, Integer> nextNextUnitMap = _findNextNextUnit(n_unitGraph, n_nextUnit, Constant.DIRECTION_BACKWARD);

                        if (n_nextUnit instanceof AssignStmt
                                && UnitHelper.GetInvokeExpr(n_nextUnit) != null
                                && !ListOperator._ListFuzzy2Maintain(UnitHelper.GetInvokeExpr(n_nextUnit).getMethod().getDeclaringClass().getName(),
                                Constant.specialTypeNotFilter, Constant.specialType2Filter)) {
                            n_value2Process.get(Constant.READ).clear();
                        }


                        for (Pair<Unit, BriefUnitGraph> unitPair : nextNextUnitMap.keySet()) {
                            Unit nextNextUnit = unitPair.getO1();
                            BriefUnitGraph nextNextUnitGraph = unitPair.getO2();

                            _buildPath(nextNextUnitGraph, n_nextUnit, nextNextUnit, nextNextUnitMap.get(unitPair),
                                    ListOperator._valueListUnion(
                                            ListOperator._valueListComplement(c_values, n_value2Process.get(Constant.WRITE)),
                                            n_value2Process.get(Constant.READ)));
                        }
                    }
                } else {
                    for (Unit nextNextUnit : n_unitGraph.getPredsOf(n_nextUnit)) {
                        _buildPath(n_unitGraph, c_currentUnit, nextNextUnit, Constant.EDGE_TYPE_BACKWARD, c_values);
                    }
                }
            }

        } else if (c_n_edgeType == Constant.EDGE_TYPE_CALL_FORWARD) {

            // rename the param
            _safeAddNode(c_currentUnit);
            _safeAddNode(n_nextUnit);
            _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_CALL_FORWARD);


            HashMap<Pair<Unit, BriefUnitGraph>, Integer> nextNextUnitMap = _findNextNextUnit(n_unitGraph, n_nextUnit, Constant.DIRECTION_FORWARD);

            InvokeExpr invokeExpr = UnitHelper.GetInvokeExpr(c_currentUnit);

            // get arg index to analyze.
            HashSet<Integer> idx4Trans = new HashSet<>();
            if (invokeExpr != null) {
                for (int i = 0; i < invokeExpr.getArgs().size(); i++) {
                    if (c_values.contains(invokeExpr.getArg(i))) {
                        idx4Trans.add(i);
                    }
                }
                if (methodValueSwitchIndex.containsKey(c_currentUnit)) {
                    for (int idxStored : methodValueSwitchIndex.get(c_currentUnit)) {
                        idx4Trans.add(idxStored);
                    }
                }

                if (!methodValueSwitchIndex.containsKey(c_currentUnit)) {
                    methodValueSwitchIndex.put(c_currentUnit, idx4Trans); // for translation
                } else {
                    methodValueSwitchIndex.get(c_currentUnit).addAll(idx4Trans);
                }

                // methodValueSwitchIndex.put(c_currentUnit, idx4Trans); // data structure 3

                for (Pair<Unit, BriefUnitGraph> unitPair : nextNextUnitMap.keySet()) {
                    Unit nextNextUnit = unitPair.getO1();
                    BriefUnitGraph nextNextUnitGraph = unitPair.getO2();

                    for (Unit callerUnit : caller2Callee.keySet()) {
                        if (callerUnit.equals(c_currentUnit)) { // It must be found
                            Pair<Unit, BriefUnitGraph> caller = new Pair<>();
                            caller.setPair(callerUnit, method2Graph.get(allUnits2Methods.get(callerUnit)));
                            methodValueContext.put(caller, c_values); // data structure 1
                        }
                    }
                    assert nextNextUnitMap.get(unitPair) == Constant.EDGE_TYPE_FORWARD;
                    _buildPath(nextNextUnitGraph, n_nextUnit, nextNextUnit, nextNextUnitMap.get(unitPair),
                            _getValuesFromIdx(idx4Trans, n_unitGraph.getBody().getMethod()));

                }
                BriefUnitGraph nextNextUnitGraph = method2Graph.get(allUnits2Methods.get(c_currentUnit));
                // BriefUnitGraph nextNextUnitGraph = new BriefUnitGraph(
                // allUnits2Methods.get(c_currentUnit).retrieveActiveBody()); // n_unitGraph;
                for (Unit nextNextUnit : nextNextUnitGraph.getSuccsOf(c_currentUnit)) {
                    c_values = ListOperator._valueListUnion(c_values, _extractValues(c_currentUnit, c_n_edgeType).get(Constant.WRITE));
                    _buildPath(nextNextUnitGraph, c_currentUnit, nextNextUnit, Constant.EDGE_TYPE_FORWARD, c_values);
                }

            } else {
                // System.out.println("1208 invoke null current unit -> " + c_currentUnit.toString());
            }


        } else if (c_n_edgeType == Constant.EDGE_TYPE_CALL_BACKWARD) {
            _safeAddNode(c_currentUnit);
            _safeAddNode(n_nextUnit);
            _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_CALL_BACKWARD);

            if (!methodValueSwitchIndex.containsKey(c_currentUnit)) {
                methodValueSwitchIndex.put(c_currentUnit, new HashSet<>());
            }

            HashMap<Pair<Unit, BriefUnitGraph>, Integer> nextNextUnitMap = _findNextNextUnit(n_unitGraph, n_nextUnit, Constant.DIRECTION_BACKWARD);

            // from return stmt to the first stmt.
            for (Unit callerUnit : caller2Callee.keySet()) {
                if (callerUnit.equals(c_currentUnit)) {
                    Pair<Unit, BriefUnitGraph> caller = new Pair<>();
                    caller.setPair(callerUnit, method2Graph.get(allUnits2Methods.get(callerUnit)));
                    methodValueContext.put(caller, c_values);
                }
            }

            for (Pair<Unit, BriefUnitGraph> unitPair : nextNextUnitMap.keySet()) {
                Unit nextNextUnit = unitPair.getO1();
                BriefUnitGraph nextNextUnitGraph = unitPair.getO2();

                _buildPath(nextNextUnitGraph, n_nextUnit, nextNextUnit, nextNextUnitMap.get(unitPair), _extractValues(n_nextUnit, c_n_edgeType).get(Constant.READ));
            }

            BriefUnitGraph nextNextUnitGraph = method2Graph.get(allUnits2Methods.get(c_currentUnit));

            // BriefUnitGraph nextNextUnitGraph = new BriefUnitGraph(
            // allUnits2Methods.get(c_currentUnit).retrieveActiveBody()); // n_unitGraph;
            for (Unit nextNextUnit : nextNextUnitGraph.getPredsOf(c_currentUnit)) {
                c_values = ListOperator._valueListComplement(c_values, _extractValues(c_currentUnit, c_n_edgeType).get(Constant.WRITE));
                InvokeExpr currentInvokeExpr = UnitHelper.GetInvokeExpr(c_currentUnit);

                for (Unit unit : methodValueSwitchIndex.keySet()) {
                    InvokeExpr expr = UnitHelper.GetInvokeExpr(unit);
                    if (expr != null && currentInvokeExpr != null
                            && expr.getMethod().equals(currentInvokeExpr.getMethod())) {
                        for (int idx : methodValueSwitchIndex.get(unit)) {
                            c_values.add(currentInvokeExpr.getArg(idx));
                        }
                    }
                }
                _buildPath(nextNextUnitGraph, c_currentUnit, nextNextUnit, Constant.EDGE_TYPE_BACKWARD, c_values);
            }
        } else if (c_n_edgeType == Constant.EDGE_TYPE_RETURN_FORWARD) {
            if (!(c_currentUnit instanceof ReturnStmt) && !(c_currentUnit instanceof ReturnVoidStmt)) {
                buildPathDepth--;
                return;
            }


            ArrayList<Unit> possibleIfUnit = _getIfUnitsInRelatedLoop(n_nextUnit, n_unitGraph);

            if (!possibleIfUnit.isEmpty() && Tools.isCallingSink(n_nextUnit, new HashSet<>(sinks.keySet()))) {
                return;
            }

            ArrayList<Value> valuesOfIfUnit = new ArrayList<>();
            for (Unit u : possibleIfUnit) {
                valuesOfIfUnit = ListOperator._valueListUnion(valuesOfIfUnit, _extractValues(u, c_n_edgeType).get(Constant.READ));
            }

            for (Unit nextNextUnit : n_unitGraph.getSuccsOf(n_nextUnit)) {
                ArrayList<Value> nextValueContext;
                Pair<Unit, BriefUnitGraph> possibleCaller = new Pair<>(n_nextUnit, n_unitGraph);

                if (methodValueContext.containsKey(possibleCaller)) {

                    _safeAddNode(c_currentUnit);
                    _safeAddNode(n_nextUnit);
                    _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_RETURN_FORWARD);

                    if (!methodValueSwitchIndex.containsKey(n_nextUnit)) {
                        methodValueSwitchIndex.put(n_nextUnit, new HashSet<>());
                    }

                    nextValueContext = methodValueContext.get(possibleCaller);
                    if (c_currentUnit instanceof ReturnStmt && c_values.contains(((ReturnStmt) c_currentUnit).getOp())) {
                        nextValueContext = ListOperator._valueListUnion(nextValueContext, n_value2Process.get(Constant.WRITE));

                        nextValueContext = ListOperator._valueListUnion(nextValueContext, valuesOfIfUnit);

                        //we do not track the sink's caller that are in loop.
                        if (possibleIfUnit.isEmpty() || !Tools.isCallingSink(n_nextUnit, new HashSet<>(sinks.keySet()))) {
                            _buildPath(n_unitGraph, n_nextUnit, nextNextUnit, Constant.EDGE_TYPE_FORWARD, nextValueContext); // nextNextUnitMap.get(unitPair)
                        }
                    } else {
                        nextValueContext = ListOperator._valueListUnion(nextValueContext, valuesOfIfUnit);
                        //we do not track the sink's caller that are in loop.
                        if (possibleIfUnit.isEmpty() || !Tools.isCallingSink(n_nextUnit, new HashSet<>(sinks.keySet()))) {
                            _buildPath(n_unitGraph, n_nextUnit, nextNextUnit, Constant.EDGE_TYPE_FORWARD, nextValueContext); // nextNextUnitMap.get(unitPair)
                        }
                    }
                } else {
                    boolean needBuildPath = (c_currentUnit instanceof ReturnVoidStmt);
                    if (c_currentUnit instanceof ReturnStmt) {
                        Value returnValue = ((ReturnStmt) c_currentUnit).getOp();
                        if (c_values.contains(returnValue)) {
                            needBuildPath = true;
                        } else if (localAlias.containsKey(allUnits2Methods.get(c_currentUnit).getSignature() + "@" + returnValue.toString())) {
                            if (c_values.contains(localAlias.get(allUnits2Methods.get(c_currentUnit).getSignature() + "@" + returnValue.toString()))) {
                                needBuildPath = true;
                            }
                            System.out.println("log local alias " + allUnits2Methods.get(c_currentUnit).getSignature() + "@" + returnValue.toString());
                            System.out.println(localAlias);
                            System.out.println();
                        }
                    }

                    if (needBuildPath) {
                        _safeAddNode(c_currentUnit);
                        _safeAddNode(n_nextUnit);
                        _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_RETURN_FORWARD);

                        if (!methodValueSwitchIndex.containsKey(n_nextUnit)) {
                            methodValueSwitchIndex.put(n_nextUnit, new HashSet<>());
                        }

                        //we do not track the sink's caller that are in loop.
                        if (possibleIfUnit.isEmpty() || !Tools.isCallingSink(n_nextUnit, new HashSet<>(sinks.keySet()))) {
                            _buildPath(n_unitGraph, n_nextUnit, nextNextUnit, Constant.EDGE_TYPE_FORWARD,
                                    ListOperator._valueListUnion(n_value2Process.get(Constant.WRITE), valuesOfIfUnit)); //
                        }
                    } else {
                        System.out.println(String.format(
                                "[c_n_edgeType == Constant.EDGE_TYPE_RETURN_FORWARD] ReturnStmt -> %s, c_values -> %s",
                                c_currentUnit.toString(), c_values.toString()));
                    }


//                    if(!c_values.contains(returnValue) && localAlias.containsKey(returnValue)){
//                        returnValue = localAlias.get(returnValue);
//                    }
//
//                    if ((c_currentUnit instanceof ReturnStmt && c_values.contains(returnValue))
//                            || c_currentUnit instanceof ReturnVoidStmt) {
//
//                    } else {
//                    }
                }
            }

        } else if (c_n_edgeType == Constant.EDGE_TYPE_RETURN_BACKWARD) {
            _safeAddNode(c_currentUnit);
            _safeAddNode(n_nextUnit);
            _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_RETURN_BACKWARD);

            ArrayList<Unit> possibleIfUnit = _getIfUnitsInRelatedLoop(n_nextUnit, n_unitGraph);
            ArrayList<Value> valuesOfIfUnit = new ArrayList<>();

            if (!possibleIfUnit.isEmpty() && Tools.isCallingSink(n_nextUnit, new HashSet<>(sinks.keySet()))) {
                return;
            }

            for (Unit u : possibleIfUnit) {
                valuesOfIfUnit = ListOperator._valueListUnion(valuesOfIfUnit, _extractValues(u, c_n_edgeType).get(Constant.READ));
            }


            ArrayList<Integer> idx = new ArrayList<>();

            for (Value v : c_values) {
                if (v instanceof ParameterRef) {
                    idx.add(((ParameterRef) v).getIndex());
                }
            }


            if (methodValueSwitchIndex.containsKey(n_nextUnit)) {
                for (int idxStored : methodValueSwitchIndex.get(n_nextUnit)) {
                    if (!idx.contains(idxStored))
                        idx.add(idxStored);
                }
            }

            if (!methodValueSwitchIndex.containsKey(n_nextUnit)) {
                methodValueSwitchIndex.put(n_nextUnit, new HashSet<Integer>() {
                    {
                        addAll(idx);
                    }
                });
                // methodValueSwitchIndex.put(n_nextUnit, idx); // for translation
            } else {
                methodValueSwitchIndex.get(n_nextUnit).addAll(idx);
            }

            // methodValueSwitchIndex.put(n_nextUnit, idx); // for translation

            for (Unit nextNextUnit : n_unitGraph.getPredsOf(n_nextUnit)) {
                Pair<Unit, BriefUnitGraph> nextKey = new Pair<>(n_nextUnit, n_unitGraph); // nextNextUnitGraph

                ArrayList<Value> values2Track = new ArrayList<>();
                if (methodValueContext.containsKey(nextKey)) {
                    values2Track = methodValueContext.get(nextKey);
                }
                values2Track = ListOperator._valueListComplement(values2Track, n_value2Process.get(Constant.WRITE));


                if (n_nextUnit instanceof InvokeStmt) {
                    for (int i : idx) {
                        Value v = ((InvokeStmt) n_nextUnit).getInvokeExpr().getArg(i);
                        if (!values2Track.contains(v))
                            values2Track.add(v);
                    }
                }

                if (n_nextUnit instanceof AssignStmt) {
                    for (int i : idx) {
                        Value v = ((AssignStmt) n_nextUnit).getInvokeExpr().getArg(i);
                        if (!values2Track.contains(v))
                            values2Track.add(v);
                    }
                }

                values2Track = ListOperator._valueListUnion(values2Track, valuesOfIfUnit);
                info.parseValuesForInput(values2Track, new ArrayList<>(), c_values);//for control

                //we do not track the unit call sinks in loop
                if (possibleIfUnit.isEmpty() || !Tools.isCallingSink(n_nextUnit, new HashSet<>(sinks.keySet()))) {
                    _buildPath(n_unitGraph, n_nextUnit, nextNextUnit, Constant.EDGE_TYPE_BACKWARD, values2Track);
                }

                // nextNextUnitMap.get(unitPair)
            }
        } else if (c_n_edgeType == Constant.EDGE_TYPE_GLOBAL_FORWARD) {
            _safeAddNode(c_currentUnit);
            _safeAddNode(n_nextUnit);
            _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_GLOBAL_FORWARD);


            ArrayList<Unit> possibleIfUnit = _getIfUnitsInRelatedLoop(n_nextUnit, n_unitGraph);//to check whether the field is in a branch.

            ArrayList<Value> toRemove = new ArrayList<>();
            for (Value value : n_value2Process.get(Constant.READ)) {
                if (value instanceof FieldRef) {
                    for (Value v : c_values) {
                        if (v instanceof FieldRef && ((FieldRef) v).getField().equals(((FieldRef) value).getField())) {
                            toRemove.add(v);
                        }
                    }
                }
            }
            c_values.removeAll(toRemove);

            ArrayList<Value> field2Track = new ArrayList<>(n_value2Process.get(Constant.WRITE));

            if (possibleIfUnit.isEmpty()) {
                HashMap<Pair<Unit, BriefUnitGraph>, Integer> nextNextUnitMap = _findNextNextUnit(n_unitGraph, n_nextUnit, Constant.DIRECTION_FORWARD);

                for (Pair<Unit, BriefUnitGraph> unitPair : nextNextUnitMap.keySet()) {
                    Unit nextNextUnit = unitPair.getO1();
                    BriefUnitGraph nextNextUnitGraph = unitPair.getO2();

                    _buildPath(nextNextUnitGraph, n_nextUnit, nextNextUnit, nextNextUnitMap.get(unitPair), field2Track); //

                }

            } else {

                ArrayList<Value> valuesOfIfUnit = new ArrayList<>();

                for (Unit u : possibleIfUnit) {
                    valuesOfIfUnit = ListOperator._valueListUnion(valuesOfIfUnit, _extractValues(u, c_n_edgeType).get(Constant.READ));
                }


                for (Unit u : possibleIfUnit) {
                    HashMap<Pair<Unit, BriefUnitGraph>, Integer> nextNextUnitMap = _findNextNextUnit(n_unitGraph, u, Constant.DIRECTION_FORWARD);

                    for (Pair<Unit, BriefUnitGraph> unitPair : nextNextUnitMap.keySet()) {
                        Unit nextNextUnit = unitPair.getO1();
                        BriefUnitGraph nextNextUnitGraph = unitPair.getO2();

                        field2Track = ListOperator._valueListUnion(field2Track, valuesOfIfUnit);
                        _buildPath(nextNextUnitGraph, u, nextNextUnit, nextNextUnitMap.get(unitPair), field2Track); //

                    }
                }
            }

            //track in original function
            BriefUnitGraph nextNextUnitGraph = method2Graph.get(allUnits2Methods.get(c_currentUnit));
            // BriefUnitGraph nextNextUnitGraph = new BriefUnitGraph(
            // allUnits2Methods.get(c_currentUnit).retrieveActiveBody()); // n_unitGraph;
            for (Unit nextNextUnit : nextNextUnitGraph.getSuccsOf(c_currentUnit)) {

//                c_values = Utils._valueListUnion(c_values, valuesOfIfUnit);
                _buildPath(nextNextUnitGraph, c_currentUnit, nextNextUnit, Constant.EDGE_TYPE_FORWARD, c_values);
            }


        } else if (c_n_edgeType == Constant.EDGE_TYPE_GLOBAL_BACKWARD) {
            _safeAddNode(c_currentUnit);
            _safeAddNode(n_nextUnit);
            _safeAddEdge(c_currentUnit, n_nextUnit, Constant.EDGE_LABEL_GLOBAL_BACKWARD);

            ArrayList<Unit> possibleIfUnit = _getIfUnitsInRelatedLoop(n_nextUnit, n_unitGraph);
            ArrayList<Value> valuesOfIfUnit = new ArrayList<>();
            for (Unit u : possibleIfUnit) {
                valuesOfIfUnit = ListOperator._valueListUnion(valuesOfIfUnit, _extractValues(u, c_n_edgeType).get(Constant.READ));
            }

            HashMap<Pair<Unit, BriefUnitGraph>, Integer> nextNextUnitMap = _findNextNextUnit(n_unitGraph, n_nextUnit, Constant.DIRECTION_BACKWARD);

            ArrayList<Value> field2Track = new ArrayList<>();
            ArrayList<Value> toRemove = new ArrayList<>();

            for (Value value : n_value2Process.get(Constant.WRITE)) {
                if (value instanceof FieldRef) {
                    for (Value v : c_values) {
                        if (v instanceof FieldRef && ((FieldRef) v).getField().equals(((FieldRef) value).getField())) {
                            toRemove.add(v);
                        }
                    }
                }
            }
            c_values.removeAll(toRemove);
            field2Track.addAll(n_value2Process.get(Constant.READ));

            for (Pair<Unit, BriefUnitGraph> unitPair : nextNextUnitMap.keySet()) {
                Unit nextNextUnit = unitPair.getO1();
                BriefUnitGraph nextNextUnitGraph = unitPair.getO2();

                field2Track = ListOperator._valueListUnion(field2Track, valuesOfIfUnit);
                _buildPath(nextNextUnitGraph, n_nextUnit, nextNextUnit, nextNextUnitMap.get(unitPair), field2Track); //
            }

            BriefUnitGraph nextNextUnitGraph = method2Graph.get(allUnits2Methods.get(c_currentUnit));
            // BriefUnitGraph nextNextUnitGraph = new BriefUnitGraph(
            // allUnits2Methods.get(c_currentUnit).retrieveActiveBody()); // n_unitGraph;
            for (Unit nextNextUnit : nextNextUnitGraph.getPredsOf(c_currentUnit)) {
                c_values = ListOperator._valueListUnion(c_values, valuesOfIfUnit);
                _buildPath(nextNextUnitGraph, c_currentUnit, nextNextUnit, Constant.EDGE_TYPE_BACKWARD, c_values);
            }

        }
        buildPathDepth--;
    }


    private ArrayList<Unit> _getIfUnitsInRelatedLoop(Unit queryUnit, BriefUnitGraph queryGraph) {

        ArrayList<Unit> ifUnitIdxes = new ArrayList<>();
        SootMethod queryMethod = queryGraph.getBody().getMethod();

        ArrayList<Unit> queryUnitsList = new ArrayList<>(queryGraph.getBody().getUnits());
        int queryUnitIdx = queryUnitsList.indexOf(queryUnit);

        if (loopRecord.containsKey(queryMethod)) {
            for (Pair<Pair<Unit, Integer>, Pair<Unit, Integer>> loopPair : loopRecord.get(queryMethod)) {
                Unit ifUnit = loopPair.getO1().getO1();
                // Unit ifTgtUnit = loopPair.getO2().getO1();
                int ifIdx = loopPair.getO1().getO2();
                ArrayList<Unit> units = new ArrayList<>(queryGraph.getBody().getUnits());
                int targetUnitIdx = units.indexOf(((IfStmt) ifUnit).getTarget());

                if (queryUnitIdx <= targetUnitIdx && queryUnitIdx >= ifIdx) {
                    ifUnitIdxes.add(ifUnit);
                }
            }
        }

        return ifUnitIdxes;
    }

    private boolean _methodContainsUnit(Unit target, SootMethod method) {
        if (target == null) {
            return false;
        }
        for (Unit u : method.retrieveActiveBody().getUnits()) {

            if (u.toString().equals(target.toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean _srcContainsUnit(SootMethod srcMethod, SootMethod method) {
        for (Unit u : srcMethod.retrieveActiveBody().getUnits()) {
            if (u.toString().contains(method.getSignature())) {
                return true;
            }
        }
        return false;
    }


    private void _safeAddNode(Unit o) {
        if (!resultJimpleGraph.containsNode(o)) {
            resultJimpleGraph.addNode(o);
        }

        if (!resultJimpleGraph4Visual.containsVertex(o)) {
            resultJimpleGraph4Visual.addVertex(o);
        }
    }


    private void _safeAddEdge(Unit c, Unit n, String l) {
        if (c.equals(n)) {
            return;
        }
        if (_isForward(l)) {
            if (!resultJimpleGraph.containsEdge(c, n, l)) {
                resultJimpleGraph.addEdge(c, n, l);
            }
            GraphEdge e = new GraphEdge(l);
            if (!resultJimpleGraph4Visual.containsEdge(c, n)) {
                resultJimpleGraph4Visual.addEdge(c, n, e);
            }
        } else {
            if (!resultJimpleGraph.containsEdge(n, c, l)) {
                resultJimpleGraph.addEdge(n, c, l);
            }
            GraphEdge e = new GraphEdge(l);
            if (!resultJimpleGraph4Visual.containsEdge(n, c)) {
                resultJimpleGraph4Visual.addEdge(n, c, e);
            }
        }

    }

    private void processContextSwitch(Unit c, HashMap<Pair<Unit, BriefUnitGraph>, Integer> n_unitGraphType) {
        for (Pair<Unit, BriefUnitGraph> key : n_unitGraphType.keySet()) {
            Unit n = key.getO1();
            String l = Constant.EDGE_LABEL[n_unitGraphType.get(key)];

            //print the access history
            if (l.equals(Constant.EDGE_LABEL_CALL_FORWARD) || l.equals(Constant.EDGE_LABEL_CALL_BACKWARD)
                    || l.equals(Constant.EDGE_LABEL_RETURN_FORWARD) || l.equals(Constant.EDGE_LABEL_RETURN_BACKWARD) ||
                    l.equals(Constant.EDGE_LABEL_GLOBAL_BACKWARD) || l.equals(Constant.EDGE_LABEL_GLOBAL_FORWARD)) {

                if (!allUnits2Methods.get(c).equals(allUnits2Methods.get(n))) {
//                    if(allUnits2Methods.get(n) == null){
//                        System.out.println("         " + n.toString());
//                        System.out.println("         " + c.toString());
////                        System.out.println("         " + allUnits2Methods.get(n).toString());
//                        System.out.println("         " + allUnits2Methods.get(c).toString());
//                    }

                    if (!methodAccessRecords.keySet().contains(allUnits2Methods.get(n))) {
                        methodAccessRecords.put(allUnits2Methods.get(n), 1);

                        // System.out.println(String.format("[Switch Context]\n\t Current Method: %s (%d),\n\t Next Method: %s (%d),\n\t Change Method: %s,\n\t Times: %d",
                        //         allUnits2Methods.get(c).toString(), allUnits2Methods.get(c).hashCode(), allUnits2Methods.get(n).toString(), allUnits2Methods.get(n).hashCode(), l, 1));
                    } else {
                        methodAccessRecords.put(allUnits2Methods.get(n), methodAccessRecords.get(allUnits2Methods.get(n)) + 1);

                        // System.out.println(String.format("[Switch Context]\n\t Current Method: %s (%d),\n\t Next Method: %s (%d),\n\t Change Method: %s,\n\t Times: %d",
                        //         allUnits2Methods.get(c).toString(), allUnits2Methods.get(c).hashCode(), allUnits2Methods.get(n).toString(), allUnits2Methods.get(n).hashCode(),
                        //         l, methodAccessRecords.get(allUnits2Methods.get(n)) + 1));
                    }
                }
            }
        }
    }

    private boolean _isForward(int edgeType) {
        return edgeType % 2 == 0;
    }

    private boolean _isForward(String nextDirection) {
        return (nextDirection.equals(Constant.EDGE_LABEL_FORWARD)
                || nextDirection.equals(Constant.EDGE_LABEL_CALL_FORWARD)
                || nextDirection.equals(Constant.EDGE_LABEL_RETURN_FORWARD)
                || nextDirection.equals(Constant.EDGE_LABEL_GLOBAL_FORWARD));
    }

    private ArrayList<Value> _getValuesFromIdx(HashSet<Integer> idx, SootMethod tgtMethod) {
        ArrayList<Value> values = new ArrayList<>();
        if (!tgtMethod.isConcrete() || !tgtMethod.hasActiveBody())
            return values;

        for (Unit u : tgtMethod.retrieveActiveBody().getUnits()) {
            if (u instanceof IdentityStmt && ((IdentityStmt) u).getRightOp() instanceof ParameterRef) {
                int id = ((ParameterRef) ((IdentityStmt) u).getRightOp()).getIndex();
                if (idx.contains(id)) {
                    values.add(((IdentityStmt) u).getLeftOp());
                }
            }
        }
        return values;
    }

    private boolean _isVisited(_Path path, ArrayList<Value> c_values) {
        if (visitedPathMap.containsKey(path)) {
            if (visitedPathMap.get(path).containsAll(new HashSet<>(c_values))) {
                return true;
            } else {
                visitedPathMap.get(path).addAll(new HashSet<>(c_values));
            }
        } else {
            Set<Value> v = new HashSet<>(c_values);
            visitedPathMap.put(path, v);
        }
        return false;
    }

    class _Path {
        BriefUnitGraph n_unitGraph;
        Unit c_currentUnit;
        Unit n_nextUnit;
        int c_n_edgeType;

        _Path(BriefUnitGraph n_unitGraph, Unit c_currentUnit, Unit n_unit, int c_n_edgeType) {
            this.n_unitGraph = n_unitGraph;
            this.c_currentUnit = c_currentUnit;
            this.n_nextUnit = n_unit;
            this.c_n_edgeType = c_n_edgeType;
        }

        @Override
        public boolean equals(Object obj) {
            _Path newPath = (_Path) obj;
            return n_unitGraph.getBody().getMethod().equals(newPath.n_unitGraph.getBody().getMethod())
                    && c_currentUnit.equals(newPath.c_currentUnit) && n_nextUnit.equals(newPath.n_nextUnit)
                    && c_n_edgeType == newPath.c_n_edgeType;

        }

        @Override
        public String toString() {
            return "c_unit -> " + c_currentUnit.toString() + "; n_unit -> " + n_nextUnit.toString() + "; c_n_type -> "
                    + c_n_edgeType;
        }

        @Override
        public int hashCode() {
            String hash = "" + n_unitGraph.getBody().getMethod().getSignature() + "" + c_currentUnit.hashCode() + ""
                    + n_nextUnit.hashCode() + "" + c_n_edgeType;
            return hash.hashCode();
        }
    }
}
