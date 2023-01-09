package hunter.Postprocess.CodeGenerate;

import heros.solver.Pair;
import hunter.Preprocess.FeatureBuilder;
import hunter.Utils.BranchHelper;
import hunter.Utils.TypeHelper;
import hunter.Utils.UnitHelper;
import hunter.Track.Tracker;
import hunter.Utils.Tools;
import soot.*;
import soot.jimple.Constant;
import soot.jimple.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.HashMutableEdgeLabelledDirectedGraph;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Translator {
    private String outputPath;
    private int indentSpaces = 4;
    private HashMap<Unit, SootMethod> allUnits2Methods = new HashMap<>();
    // graph with 8 types of edges
    private HashMutableEdgeLabelledDirectedGraph<Unit, String> resultJimpleGraph;

    private HashMap<SootMethod, ArrayList<Pair<Pair<Unit, Integer>, Pair<Unit, Integer>>>> loopRecord;

    private HashMap<Unit, Unit> loopPairs;

    // jimple results
    private HashMap<SootMethod, ArrayList<Unit>> jimpleResults;
    private HashMap<SootMethod, ArrayList<Unit>> jimpleOriginalResults;
    private HashMap<SootMethod, ArrayList<String>> javaResults;
    private HashMap<SootMethod, HashSet<Local>> localResults;
    private HashSet<Local> needLocals;
    private HashSet<String> frameworkFileds;// type name

    private HashMap<SootMethod, Integer> orderedMethods;
    // private HashMap<SootMethod, String> methodAlias;
    private HashMap<SootMethod, String> methodNameAlias;
    private HashMap<SootField, String> fieldAlias;
    private HashMap<SootMethod, Type> methodRetType;
    private HashMap<SootMethod, ArrayList<Integer>> methodSwitchIndex;
    private HashMap<Unit, ArrayList<Unit>> newObjects2Alias;
    private HashMap<Unit, SootMethod> caller2Callee;

    private HashMap<String, ArrayList<String>> apk2Models;

    private HashSet<IfStmt> preProcessedForLoop;

    private String apkName;
    private boolean falsePriority;

    private ArrayList<String> forGetResults;

    /**
     * @resultJimpleGraph -> unit : node; string : edge_type;
     * @loopRecord -> All loop pair : HashMap<Current_Method, Pair< Pair<IfUnit,
     * IfIdx>, Pair<GotoUnit, GotoIdx> >>
     */

    public Translator(Tracker tracker, Optimizer optimizer, String apkName, String outputPath) {
        this.outputPath = outputPath;
        this.resultJimpleGraph = tracker.resultJimpleGraph;
        this.jimpleResults = optimizer.jimpleResults;
        this.jimpleOriginalResults = optimizer.originalJimpleResults;
        this.javaResults = new HashMap<>();
        this.loopPairs = new HashMap<>();
        this.loopRecord = tracker.loopRecord;
        this.localResults = new HashMap<>();
        this.needLocals = new HashSet<>();
        this.preProcessedForLoop = new HashSet<>();
        this.frameworkFileds = new HashSet<>();
        this.newObjects2Alias = tracker.newObjects2Alias;
        this.allUnits2Methods = tracker.allUnits2Methods;

        this.orderedMethods = optimizer.orderedMethods; // sootMethod, order sink: [0-9], field: [10~]
        this.methodRetType = optimizer.methodRetType; // sootMethod, type
        // this.methodAlias = codeGenerater.methodAlias; // method, alias
        this.methodNameAlias = optimizer.nameAlias;
        this.fieldAlias = optimizer.fieldAlias;// field,alias
        this.methodSwitchIndex = optimizer.methodSwitchIndex; // method, paramIdxList
        this.caller2Callee = tracker.caller2Callee;

        String path = FeatureBuilder.class.getClassLoader().getResource(hunter.Constant.DEFAULT_MODEL_SETTING).getFile();
        this.apk2Models = Tools.loadModelNames(path);

        this.apkName = apkName;
        this.falsePriority = false;

        this.forGetResults = new ArrayList<>();

        _maintainProgram(); // Unit -> Method
    }

    private void _maintainProgram() {
        for (SootMethod sootMethod : jimpleResults.keySet()) {
            if (loopRecord.containsKey(sootMethod)) {
                ArrayList<Pair<Pair<Unit, Integer>, Pair<Unit, Integer>>> l = loopRecord.get(sootMethod);
                for (Pair<Pair<Unit, Integer>, Pair<Unit, Integer>> p : loopRecord.get(sootMethod)) {
                    loopPairs.put(p.getO1().getO1(), p.getO2().getO1());
                }
            }
        }

        // for(SootMethod sootMethod : jimpleResults.keySet()){
        // ArrayList<Unit> entrances = new ArrayList<>();
        // if(loopRecord.containsKey(sootMethod)){
        // for(Pair<Pair<Unit, Integer>, Pair<Unit, Integer>> p :
        // loopRecord.get(sootMethod)){
        // entrances.add(p.getO1().getO1());
        // }
        // loopEntrances.put(sootMethod, entrances);
        // }
        // }
    }

    public void go() {
        for (SootMethod sootMethod : jimpleResults.keySet()) {
            boolean skip = false;
            for (String s : hunter.Constant.methodBlockList) {
                if (sootMethod.getSignature().equals(s)) {
                    skip = true;
                    break;
                }
            }
            for (String s : hunter.Constant.methodNameBlockList) {
                if (sootMethod.getName().equals(s)) {
                    skip = true;
                    break;
                }
            }
            if (skip) {
                continue;
            }

            System.out.println("start translate method: " + sootMethod.getSignature());
            BriefUnitGraph method_cfg = new BriefUnitGraph(sootMethod.retrieveActiveBody());

            Unit start = null;
            for (Unit head : method_cfg.getHeads()) {
                System.out.println("  head: " + head.toString());
                if (head instanceof IdentityStmt && ((IdentityStmt) head).getRightOp() instanceof CaughtExceptionRef) {
                    continue;
                } else {
                    start = head;
                    break;
                }
            }

            ArrayList<String> results = new ArrayList<>();
            ArrayList<Unit> visited = new ArrayList<>();
            localResults.put(sootMethod, new HashSet<>());
            results.addAll(processNormalStmt(visited, sootMethod, start, method_cfg, 1, null));

            for (int i = 1; i < results.size(); i++) {
                String unit = results.get(i);
                if (unit.contains("tf0.fetch(")) {
                    String r = results.get(i - 1);
                    if (r.contains(" = ")) {
                        r = r.split(" = ")[1].replace(";", "");
                        forGetResults.add(r);
                    }
                }
            }

            if (!javaResults.containsKey(sootMethod)) {
                javaResults.put(sootMethod, results);
            } else {
                System.out.println("ERROR");
                // javaResults.get(sootMethod).addAll(results);
            }

        }

        int jimpleNodeNum = 0;
        int jimpleOriginalNodeNum = 0;
        for (SootMethod sootMethod : jimpleResults.keySet()) {
            jimpleNodeNum += jimpleResults.get(sootMethod).size();
        }
        for (SootMethod sootMethod : jimpleOriginalResults.keySet()) {
            jimpleOriginalNodeNum += jimpleOriginalResults.get(sootMethod).size();
        }

        System.out.println("jimpleNodeNum: " + jimpleNodeNum);
        System.out.println("jimpleOriginalNodeNum: " + jimpleOriginalNodeNum);
        //
        // for(SootMethod sootMethod : jimpleResults.keySet()){
        // ArrayList<Pair<Pair<Unit, Integer>, Pair<Unit, Integer>>> loopPairs = new
        // ArrayList<>();
        //
        // if(loopRecord.containsKey(sootMethod)){
        // loopPairs = loopRecord.get(sootMethod);
        // }
        //
        // BriefUnitGraph briefUnitGraph = new
        // BriefUnitGraph(sootMethod.retrieveActiveBody());
        // ArrayList<Unit> visitedUnits = new ArrayList<>();
        //
        // for(Unit unit : briefUnitGraph.getHeads()){
        // if((unit instanceof CaughtExceptionRef)) continue;
        //
        //
        //
        // }
        //
        //
        // ArrayList<Unit> originalUnits = new
        // ArrayList<>(sootMethod.retrieveActiveBody().getUnits());
        // Unit[] jimpleResultArray = new Unit[originalUnits.size()];
        //
        // ArrayList<Unit> savedUnits = jimpleResults.get(sootMethod);
        // for (Unit unit : savedUnits) {
        // jimpleResultArray[originalUnits.indexOf(unit)] = unit;
        // }
        //
        // ArrayList<String> results = new ArrayList<>();
        // for (int i = 0; i < originalUnits.size(); i++) {
        // if (jimpleResultArray[i] != null) {
        // results.add(translateUnit(jimpleResultArray[i]));
        // }
        // }
        //
        // if (!javaResults.containsKey(sootMethod.getSignature())) {
        // javaResults.put(sootMethod.getSignature(), results);
        // } else {
        // javaResults.get(sootMethod.getSignature()).addAll(results);
        // }
        //
        //
        // }

    }

    private ArrayList<String> processNormalStmt(ArrayList<Unit> visited, SootMethod sootMethod, Unit now,
                                                BriefUnitGraph method_cfg, int layer, Unit until) {
        ArrayList<String> result = new ArrayList<>();
        if (now == null) {
            return result;
        }

        System.out.println(String.format("now@processNormalStmt -> %s, method -> %s, until -> %s", now.toString(), sootMethod.getSignature(), until == null ? "END" : until.toString()));

        if (until != null && until.equals(now)) {
            return result;
        }

        if (visited.contains(now)) {
            return result;
        }

        if (now instanceof GotoStmt) {
            if (loopPairs.containsValue(now)) {
                System.out.println("LOOP GOTO  " + now.toString() + "; Method " + sootMethod.getSignature());
                visited.add(now);
            } else {
                System.out.println("BRANCH GOTO  " + now.toString());
                if (until instanceof GotoStmt && ((GotoStmt) now).getTarget().equals(((GotoStmt) until).getTarget())) {
                    // skip
                } else {
                    visited.add(now);
                    result.addAll(processNormalStmt(visited, sootMethod, ((GotoStmt) now).getTarget(), method_cfg, layer, until));
                }
            }
        } else if (now instanceof IfStmt) {
            if (jimpleResults.get(sootMethod).contains(now)) {
                if (loopPairs.containsKey(now)) {
                    System.out.println("LOOP  " + now.toString() + "; Method " + sootMethod.getSignature());
                    Unit whileExit = ((IfStmt) now).getTarget();
                    result.addAll(processWhile(visited, sootMethod, (IfStmt) now, method_cfg, layer, whileExit));
                    result.addAll(processNormalStmt(visited, sootMethod, whileExit, method_cfg, layer, until));
                } else {
                    System.out.println("IF  " + now.toString() + "; Method " + sootMethod.getSignature());
                    Unit ifExit = BranchHelper._findIfExit(visited, (IfStmt) now, method_cfg);
                    result.addAll(processIf(visited, sootMethod, (IfStmt) now, method_cfg, layer, ifExit));
                    result.addAll(processNormalStmt(visited, sootMethod, ifExit, method_cfg, layer, until));
                }
            } else {
                visited.add(now);
                Unit nowBranchTrue = ((IfStmt) now).getTarget();
                Unit nowBranchFalse = method_cfg.getSuccsOf(now).get(0);
                if (method_cfg.getSuccsOf(now).size() > 1 && nowBranchFalse.equals(nowBranchTrue)) {
                    nowBranchFalse = method_cfg.getSuccsOf(now).get(1);
                }
                Unit ifExit = BranchHelper._findIfExit(visited, (IfStmt) now, method_cfg);

                if (ifExit == null) {
                    ifExit = until;
                }

                ArrayList<Unit> visitedTrue = new ArrayList<>();
                visitedTrue.addAll(visited);
                ArrayList<String> branchTrueList = new ArrayList<>();
                branchTrueList.addAll(processNormalStmt(visitedTrue, sootMethod, nowBranchTrue, method_cfg, layer, ifExit));

                ArrayList<Unit> visitedFlase = new ArrayList<>();
                visitedFlase.addAll(visited);
                ArrayList<String> branchFalseList = new ArrayList<>();
                branchFalseList.addAll(processNormalStmt(visitedFlase, sootMethod, nowBranchFalse, method_cfg, layer, ifExit));

                if (falsePriority) {
                    if (!branchFalseList.isEmpty()) {
                        result.addAll(branchFalseList);
                    } else if (!branchTrueList.isEmpty()) {
                        result.addAll(branchTrueList);
                    }
                } else {
                    if (!branchTrueList.isEmpty()) {
                        result.addAll(branchTrueList);
                    } else if (!branchFalseList.isEmpty()) {
                        result.addAll(branchFalseList);
                    }
                }

                result.addAll(processNormalStmt(visited, sootMethod, ifExit, method_cfg, layer, until));
            }
        } else if (now instanceof SwitchStmt) {
            visited.add(now);
            if (jimpleResults.get(sootMethod).contains(now)) {
                // TODO
            }
            // for (Unit next : method_cfg.getSuccsOf(now)) {
            // if (!visited.contains(next)) {
            // result.addAll(processNormalStmt(visited, sootMethod, next, method_cfg, layer,
            // until));
            // }
            // }
            Unit next = ((SwitchStmt) now).getDefaultTarget();
            result.addAll(processNormalStmt(visited, sootMethod, next, method_cfg, layer, until));
        } else {
            if (!visited.contains(now)) {
                visited.add(now);
                if (jimpleResults.get(sootMethod).contains(now)) {
                    String unit = translateUnit(now);

                    if (!unit.equals("")) {
                        for (String s : unit.split(hunter.Constant.separator)) {
                            if (s.contains(".pb\"")) {
                                String modelName = "unknown_model";
                                if (apk2Models.containsKey(apkName.replace(".apk", "").replace("apk-", ""))) {
                                    if (apk2Models.get(apkName.replace(".apk", "").replace("apk-", "")).size() > 0) {
                                        modelName = apk2Models.get(apkName.replace(".apk", "").replace("apk-", "")).get(0);
                                    }
                                }

                                if (s.contains(modelName)) {
                                    orderedMethods.put(sootMethod, 9);
                                    result.add(generateIndent(layer) + s + ";");
                                } else {
                                    result.add(generateIndent(layer) + "// " + s + ";");
                                }
                            } else if (unit.contains("tflite0.runForMultipleInputsOutputs(")) {
                                String r = unit.split(", ")[1].replace(")", "");
                                result.add(generateIndent(layer) + s + ";");
                                result.add(generateIndent(layer) + "results = (java.util.HashMap)" + r + ";");
//                                result.add(generateIndent(layer) + "if(true) {");
//                                result.add(generateIndent(layer+1) + "return" + ";");
//                                result.add(generateIndent(layer) + "}");
                            } else if (unit.contains("tflite0.run(")) {
                                String r = unit.split(", ")[1].replace(")", "");
                                result.add(generateIndent(layer) + s + ";");
                                int index = forGetResults.size();
                                result.add(generateIndent(layer) + "results.put(" + index + ", " + r + ");");
                            } else if (unit.contains("Ncnn0.Detect(")) {
                                String r = unit.split(" = ")[0];
                                result.add(generateIndent(layer) + s + ";");
                                result.add(generateIndent(layer) + "results.put(0, " + r + ");");
                            } else if (unit.contains("tfClassifier0.recognizeImage(")) {
                                String r = unit.split(" = ")[0];
                                result.add(generateIndent(layer) + s + ";");
                                result.add(generateIndent(layer) + "results.put(0, " + r + ");");
                            } else {
                                result.add(generateIndent(layer) + s + ";");
                            }
                        }
                    }
                }
            }
            for (Unit next : method_cfg.getSuccsOf(now)) {
                if (!visited.contains(next)) {
                    result.addAll(processNormalStmt(visited, sootMethod, next, method_cfg, layer, until));
                }
            }
        }

        if (jimpleResults.get(sootMethod).contains(now)) {
            for (ValueBox vb : now.getUseAndDefBoxes()) {
                Value v = vb.getValue();
                if (v instanceof Local) {
                    localResults.get(sootMethod).add((Local) v);
                }
            }
        }


        return result;
    }

    private ArrayList<String> processWhile(ArrayList<Unit> visited, SootMethod sootMethod, IfStmt now,
                                           BriefUnitGraph method_cfg, int layer, Unit whileExit) {
        System.out.println(String.format("now@processWhile -> %s, method -> %s, whileExit -> %s", now.toString(),
                sootMethod.getSignature(), whileExit == null ? "null" : whileExit.toString()));

        ArrayList<String> result = new ArrayList<>();
        IfStmt ifUnit = now;
        GotoStmt gotoUnit = (GotoStmt) loopPairs.get(now);
        visited.add(gotoUnit);
        Unit branchTrue = ifUnit.getTarget();
        Unit branchFalse = method_cfg.getSuccsOf(ifUnit).get(0).equals(branchTrue)
                ? method_cfg.getSuccsOf(ifUnit).get(1)
                : method_cfg.getSuccsOf(ifUnit).get(0);
        Value condition = ifUnit.getCondition();

        if (branchFalse == null) {
            System.out.println("ERROR false null");
        }

        // if (preProcessedForLoop.contains(ifUnit)) {
        //     return result;
        // }

        Unit preStart = gotoUnit.getTarget();
        Unit preEnd = ifUnit;
        if (!gotoUnit.getTarget().equals(ifUnit)) {
            Unit now3 = preStart;
            HashSet<Unit> visitedSet = new HashSet<>();
            while (now3 != null) {
                System.out.println(String.format("now3@processWhile -> %s, method -> %s, ifUnit -> %s", now3.toString(),
                        sootMethod.getSignature(), ifUnit.toString()));
                visitedSet.add(now3);
                if (now3 instanceof IfStmt) {
                    Unit nowBranchTrue = ((IfStmt) now3).getTarget();
                    Unit nowBranchFalse = method_cfg.getSuccsOf(now3).get(0).equals(nowBranchTrue)
                            ? method_cfg.getSuccsOf(now3).get(1)
                            : method_cfg.getSuccsOf(now3).get(0);
                    now3 = nowBranchFalse;
                    if (visitedSet.contains(now3)) {
                        now3 = nowBranchTrue;
                    }
                } else if (now3 instanceof SwitchStmt) {
                    // TODO
                    now3 = ((SwitchStmt) now3).getDefaultTarget();
                } else {
                    now3 = (method_cfg.getSuccsOf(now3) == null || method_cfg.getSuccsOf(now3).size() == 0) ? null
                            : method_cfg.getSuccsOf(now3).get(0);
                    if (visitedSet.contains(now3)) {
                        break;
                    }
                }
                if (preEnd.equals(now3)) {
                    break;
                }
            }

            // while (!now2.equals(preStart)) {
            // if(now2 instanceof IfStmt && loopPairs.containsKey(now2)){
            // Unit now3 = preStart;
            //
            // }
            // if (method_cfg.getPredsOf(now2).size() > 1 &&
            // method_cfg.getPredsOf(now2).get(0) instanceof IfStmt
            // && !visitedSet.contains(now2)) {
            // visitedSet.add(now2);
            // now2 = method_cfg.getPredsOf(now2).get(1);
            // } else if (method_cfg.getPredsOf(now2).size() > 0 &&
            // !visitedSet.contains(now2)) {
            // visitedSet.add(now2);
            // now2 = method_cfg.getPredsOf(now2).get(0);
            // } else {
            // visitedSet.add(now2);
            // break;
            // }
            // }
            visited.removeAll(visitedSet);
            // preProcessedForLoop.add(ifUnit);
        }

        visited.add(ifUnit);

        // a = func();
        // while(a!=0){
        // //some stmt
        // a = func();//update a
        // }
        result.add(generateIndent(layer) + "while (!(" + translateBinopExpr((BinopExpr) condition) + ")) {");
        result.addAll(processNormalStmt(visited, sootMethod, branchFalse, method_cfg, layer + 1, whileExit));
        if (!gotoUnit.getTarget().equals(ifUnit)) {
            result.addAll(processNormalStmt(visited, sootMethod, preStart, method_cfg, layer + 1, ifUnit));
        }

        // if (!preProcess.isEmpty()) {
        // for (Unit u : preProcess) {
        // result.add(generateIndent(layer + 1) + translateUnit(u) + ";");
        // }
        // }
        result.add(generateIndent(layer) + "}");
        return result;
    }

    private boolean hasIfElse(BriefUnitGraph method_cfg, IfStmt ifUnit) {
        Unit exit;
        Unit branchTrue = ifUnit.getTarget();
        Unit branchFalse = method_cfg.getSuccsOf(ifUnit).get(0).equals(branchTrue)
                ? method_cfg.getSuccsOf(ifUnit).get(1)
                : method_cfg.getSuccsOf(ifUnit).get(0);

        ArrayList<Unit> visited = new ArrayList<>();
        Unit now = branchFalse;
        boolean isTail = false;
        while (!isTail && now != null) {
            if (visited.contains(now)) {
                break;
            }
            visited.add(now);
            for (Unit tail : method_cfg.getTails()) {
                if (now.equals(tail)) {
                    isTail = true;
                    break;
                }
            }
            if (isTail) {
                break;
            }
            if (now instanceof IfStmt) {
                Unit nowBranchTrue = ((IfStmt) now).getTarget();
                Unit nowBranchFalse = method_cfg.getSuccsOf(now).get(0).equals(nowBranchTrue)
                        ? method_cfg.getSuccsOf(now).get(1)
                        : method_cfg.getSuccsOf(now).get(0);
                now = nowBranchFalse;
            } else if (now instanceof SwitchStmt) {
                // TODO
                now = ((SwitchStmt) now).getDefaultTarget();
            } else {
                now = method_cfg.getSuccsOf(now) == null ? null : method_cfg.getSuccsOf(now).get(0);
            }

        }

        if (visited.contains(branchTrue)) {
            return false;
        } else {
            return true;
        }
    }

    // private Unit findIfExit(ArrayList<Unit> visited, IfStmt ifUnit,
    // BriefUnitGraph method_cfg) {
    // System.out.println(String.format("enter findIfExit -> %s",
    // ifUnit.toString()));
    // ArrayList<Unit> newVisited = new ArrayList<>();
    // newVisited.addAll(visited);
    // Unit now = ifUnit;
    // boolean isTail = false;
    // while (!isTail && now != null) {
    // System.out.println(String.format("enter findIfExit now -> %s",
    // now.toString()));
    // newVisited.add(now);
    // if (now instanceof IfStmt) {
    // Unit nowBranchTrue = ((IfStmt) now).getTarget();
    // Unit nowBranchFalse = method_cfg.getSuccsOf(now).get(0).equals(nowBranchTrue)
    // ? method_cfg.getSuccsOf(now).get(1)
    // : method_cfg.getSuccsOf(now).get(0);
    // now = nowBranchFalse;
    // } else if (now instanceof SwitchStmt) {
    // // TODO
    // now = ((SwitchStmt) now).getDefaultTarget();
    // } else {
    // now = method_cfg.getSuccsOf(now) == null ? null :
    // method_cfg.getSuccsOf(now).get(0);
    // if (newVisited.contains(now)) {
    // break;
    // }
    // }
    // for (Unit tail : method_cfg.getTails()) {
    // if (now.equals(tail)) {
    // isTail = true;
    // break;
    // }
    // }
    // }
    //
    // now = ifUnit;
    // while (!isTail && now != null) {
    // System.out.println(String.format("enter findIfExit now2 -> %s",
    // now.toString()));
    // if (newVisited.contains(now) && !now.equals(ifUnit)) {
    // return now;
    // }
    // if (now instanceof IfStmt) {
    // Unit nowBranchTrue = ((IfStmt) now).getTarget();
    // now = nowBranchTrue;
    // } else if (now instanceof SwitchStmt) {
    // // TODO
    // now = ((SwitchStmt) now).getDefaultTarget();
    // } else {
    // now = method_cfg.getSuccsOf(now) == null ? null :
    // method_cfg.getSuccsOf(now).get(0);
    // }
    // for (Unit tail : method_cfg.getTails()) {
    // if (now.equals(tail)) {
    // isTail = true;
    // break;
    // }
    // }
    // }
    // return null;
    // }

    private ArrayList<String> processIf(ArrayList<Unit> visited, SootMethod sootMethod, IfStmt now,
                                        BriefUnitGraph method_cfg, int layer, Unit ifExit) {
        System.out.println(String.format("now@processIf -> %s, method -> %s, ifExit -> %s", now.toString(),
                sootMethod.getSignature(), ifExit == null ? "null" : ifExit.toString()));


        ArrayList<String> result = new ArrayList<>();
        IfStmt ifUnit = now;
        visited.add(ifUnit);
        Value condition = ifUnit.getCondition();
        Unit branchTrue = ifUnit.getTarget();
        Unit branchFalse = method_cfg.getSuccsOf(ifUnit).get(0).equals(branchTrue)
                ? method_cfg.getSuccsOf(ifUnit).get(1)
                : method_cfg.getSuccsOf(ifUnit).get(0);

        // if(c){
        // // empty
        // }else{
        // some stmt
        // }
        if (condition instanceof BinopExpr && ((BinopExpr) condition).getOp2() instanceof NullConstant) {
            result.add(generateIndent(layer) + "if (false) {");
        } else {
            result.add(generateIndent(layer) + "if (!(" + translateBinopExpr((BinopExpr) condition) + ")) {");
        }
        result.addAll(processNormalStmt(visited, sootMethod, branchFalse, method_cfg, layer + 1, ifExit));
        if (hasIfElse(method_cfg, ifUnit)) {
            result.add(generateIndent(layer) + "} else {");
            result.addAll(processNormalStmt(visited, sootMethod, branchTrue, method_cfg, layer + 1, ifExit));
        }
        result.add(generateIndent(layer) + "}");
        return result;
    }

    // private boolean _loopPairContainsUnit(ArrayList<Pair<Pair<Unit, Integer>,
    // Pair<Unit, Integer>>> loopPairs, Unit unit){
    // for(Pair<Pair<Unit, Integer>, Pair<Unit, Integer>> pair : loopPairs){
    // if (pair.getO1().getO1().equals(unit)) return true;
    // }
    // return false;
    // }
    //
    // private void dfs(Unit unit, BriefUnitGraph unitGraph, ArrayList<Unit>
    // visitedUnits, ArrayList<Pair<Pair<Unit, Integer>, Pair<Unit, Integer>>>
    // loopPairs) {
    //
    //
    // }

    private String chooseReturn(SootMethod method) {

        ArrayList<String> candidatesConstant = new ArrayList<>();
        ArrayList<String> candidatesVariable = new ArrayList<>();
        ArrayList<String> candidatesIllegalConstant = new ArrayList<>();
        String result = "";
        if (!(methodRetType.get(method) instanceof VoidType)) {
            for (Unit u : jimpleResults.get(method)) {
                if (u instanceof ReturnStmt) {
                    Value op = ((ReturnStmt) u).getOp();
                    if (op instanceof Constant) {
                        if (op instanceof StringConstant) {
                            // result = "return \"" + ((StringConstant) op).value + "\"";
                            result = "return " + op.toString();
                            if (!((StringConstant) op).value.equals("")) {
                                candidatesConstant.add(result);
                            } else {
                                candidatesIllegalConstant.add(result);
                            }

                        } else if (op instanceof IntConstant) {
                            if (methodRetType.get(method) instanceof BooleanType) {
                                if (op.toString().equals("0")) {
                                    result = "return false";
                                    candidatesConstant.add(result);
                                } else {
                                    result = "return true";
                                    candidatesConstant.add(result);
                                }
                            } else {
                                // result = "return " + ((IntConstant) op).value;
                                result = "return " + op.toString();
                                if (((IntConstant) op).value != 0 && ((IntConstant) op).value != -1) {
                                    candidatesConstant.add(result);
                                } else {
                                    candidatesIllegalConstant.add(result);
                                }
                            }

                        } else if (op instanceof LongConstant) {
                            // result = "return " + ((LongConstant) op).value;
                            result = "return " + op.toString();
                            if (((LongConstant) op).value != 0 && ((LongConstant) op).value != -1) {
                                candidatesConstant.add(result);
                            } else {
                                candidatesIllegalConstant.add(result);
                            }
                        } else if (op instanceof DoubleConstant) {
                            // result = "return " + ((DoubleConstant) op).value;
                            result = "return " + op.toString();
                            if (!(((DoubleConstant) op).value >= -0.0000001
                                    && ((DoubleConstant) op).value <= 0.0000001)) {
                                candidatesConstant.add(result);
                            } else {
                                candidatesIllegalConstant.add(result);
                            }

                        } else if (op instanceof FloatConstant) {
                            // result = "return " + ((FloatConstant) op).value;
                            result = "return " + op.toString();
                            if (!(((FloatConstant) op).value >= -0.0000001
                                    && ((FloatConstant) op).value <= 0.0000001)) {
                                candidatesConstant.add(result);
                            } else {
                                candidatesIllegalConstant.add(result);
                            }
                        }
                    } else {
                        result = "return " + translateImmediate(((ReturnStmt) u).getOp());
                        candidatesVariable.add(result);
                    }

                }
            }
        }

        if (candidatesConstant.size() > 0) {
            // int index = (int) (Math.random() * candidatesConstant.size());
            return candidatesConstant.get(0);
        } else if (candidatesVariable.size() > 0) {
            // int index = (int) (Math.random() * candidatesVariable.size());
            return candidatesVariable.get(0);
        } else if (candidatesIllegalConstant.size() > 0) {
            // int index = (int) (Math.random() * candidatesIllegalConstant.size());
            return candidatesIllegalConstant.get(0);
        } else {
            return "return";
        }

    }

    public void print() {
        boolean forTest = true;
        try {
            String fileName = "Hunter_" + apkName.replace("-", "").replace(" ", "_").replace(".", "_").replace(".apk", "");
            FileWriter fileWriter = null;
            fileWriter = new FileWriter(outputPath + fileName + ".java");

            // package
            fileWriter.write("package com.test;\n");
            fileWriter.write("\n");

            // class
            String className = "MainActivity";
            if (forTest) {
                className = fileName;
            }
            fileWriter.write("// " + apkName + "\n");
            Date date = new Date();
            SimpleDateFormat dateFormat_min = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
            fileWriter.write("// " + dateFormat_min.format(date) + "\n");
            if (forTest) {
                fileWriter.write("public class " + className + " {\n");
            } else {
                fileWriter.write("public class " + className + " extends android.support.v7.app.AppCompatActivity {\n");
            }

            // field
            for (SootField sf : fieldAlias.keySet()) {
                if (!sf.getType().equals(RefType.v("android.graphics.Bitmap"))) {
                    fileWriter.write(generateIndent(1) + translateType(sf.getType()) + " " + fieldAlias.get(sf) + ";\n");
                }
            }

            // framework field
            fileWriter.write(generateIndent(1) + "// framework instance\n");
            for (String s : frameworkFileds) {
                fileWriter.write(generateIndent(1) + s + " " + FeatureBuilder.getFrameworkInstance(s) + ";\n");
            }

            // bitmap field
            fileWriter.write(generateIndent(1) + "// bitmap input\n");
            fileWriter.write(generateIndent(1) + "android.graphics.Bitmap " + hunter.Constant.bitmapFieldName + ";\n");

            // context field
            fileWriter.write(generateIndent(1) + "// context\n");
            fileWriter.write(generateIndent(1) + "android.content.Context " + "context" + ";\n");

            // results
            fileWriter.write(generateIndent(1) + "// results\n");
            fileWriter.write(generateIndent(1) + "java.util.HashMap " + "results" + ";\n");


            fileWriter.write("\n");

            if (forTest) {
                // constructor
                fileWriter.write(generateIndent(1) + "public " + className + "(android.content.Context context) {\n");
                fileWriter.write(generateIndent(2) + "try {\n");

                // get context
                fileWriter.write(generateIndent(3) + "// get context\n");
                fileWriter.write(generateIndent(3) + "this.context = context;\n\n");

                // init results hashmap
                fileWriter.write(generateIndent(3) + "// init results hashmap\n");
                fileWriter.write(generateIndent(3) + "this.results = new java.util.HashMap();\n\n");
            } else {
                // onCreate()
                fileWriter.write(generateIndent(1) + "@Override\n");
                fileWriter.write(generateIndent(1) + "protected void onCreate(android.os.Bundle bundle) {\n");
                fileWriter.write(generateIndent(2) + "try {\n");
                fileWriter.write(generateIndent(3) + "super.onCreate(bundle);\n");

                // get context
                fileWriter.write(generateIndent(3) + "// get context\n");
                fileWriter.write(generateIndent(3) + "this.context = this;\n\n");

                // load bitmap from assets
                fileWriter.write(generateIndent(3) + "// load bitmap from assets\n");
                for (List<String> stmts : hunter.Constant.src2FeedStmts.values()) {
                    for (String s : stmts) {
                        fileWriter.write(generateIndent(3) + s + ";\n");
                    }
                }
                fileWriter.write(generateIndent(3) + "\n");
            }

            String modelName = "unknown_model";
            if (apk2Models.containsKey(apkName.replace(".apk", "").replace("apk-", ""))) {
                if (apk2Models.get(apkName.replace(".apk", "").replace("apk-", "")).size() > 0) {
                    modelName = apk2Models.get(apkName.replace(".apk", "").replace("apk-", "")).get(0);
                }
            }


            // init load model
            String apkModelsPath = apkName.replace(".apk", "");
            fileWriter.write(generateIndent(3) + "// init and load model\n");
            for (String s : frameworkFileds) {
                if (hunter.Constant.frameworkInit.containsKey(s)) {
                    for (String stmt : hunter.Constant.frameworkInit.get(s)) {
                        stmt = stmt.replace("%MODEL_NAME%", apkModelsPath + "/" + modelName);
                        fileWriter.write(generateIndent(3) + stmt + ";\n");
                    }
                }
            }
            fileWriter.write(generateIndent(3) + "\n");

            if (!forTest) {
                // call head methods
                fileWriter.write(generateIndent(3) + "// methods\n");
                writeHeadMethods(fileWriter);
            }

            fileWriter.write(generateIndent(2) + "} catch (Exception e) {\n");
            fileWriter.write(generateIndent(3) + "e.printStackTrace();\n");
            fileWriter.write(generateIndent(2) + "}\n");
            fileWriter.write(generateIndent(1) + "}\n\n");

            if (forTest) {
                // go()
                fileWriter.write(generateIndent(1) + "public void go() {\n");
                fileWriter.write(generateIndent(2) + "try {\n");
                writeHeadMethods(fileWriter);
                fileWriter.write(generateIndent(2) + "} catch (Exception e) {\n");
                fileWriter.write(generateIndent(3) + "e.printStackTrace();\n");
                fileWriter.write(generateIndent(2) + "}\n");
                fileWriter.write(generateIndent(1) + "}\n\n");

                // setPicture
                fileWriter.write(generateIndent(1) + "public void setPicture(android.graphics.Bitmap picture) {\n");
                fileWriter.write(generateIndent(2) + "this.picture = picture;\n");
                fileWriter.write(generateIndent(1) + "}\n\n");

                // getPicture
                fileWriter.write(generateIndent(1) + "public android.graphics.Bitmap getPicture() {\n");
                fileWriter.write(generateIndent(2) + "return picture;\n");
                fileWriter.write(generateIndent(1) + "}\n\n");

                // getResults
                fileWriter.write(generateIndent(1) + "public java.util.HashMap getResults() {\n");
                if (!forGetResults.isEmpty()) {
                    int fgri = 0;
                    for (String fgr : forGetResults) {
                        fileWriter.write(generateIndent(2) + "results.put(" + fgri + ", " + fgr + ");\n");
                        fgri++;
                    }
                }
                fileWriter.write(generateIndent(2) + "return results;\n");
                fileWriter.write(generateIndent(1) + "}\n\n");
            }

            // methods
            for (SootMethod sootMethod : javaResults.keySet()) {
                // method defination
                fileWriter.write(generateIndent(1) + translateMethodSignature(sootMethod) + " throws Exception {\n");
                // locals
                for (Local l : localResults.get(sootMethod)) {
                    if (needLocals.contains(l)) {
                        boolean unknownType = false;
                        String init = "";
                        if (l.getType().equals(RefType.v("android.content.Intent"))) {
                            init = " = new android.content.Intent()";
                        } else if (l.getType().equals(RefType.v("org.tensorflow.lite.Tensor"))) {
                            init = " = tflite0.getInputTensor(0)";
                        } else if (l.getType().equals(RefType.v("java.lang.String"))) {
                            init = " = \"\"";
                        } else if (l.getType().equals(RefType.v("org.tensorflow.Graph"))) {
                            init = " = null";
                        } else if (l.getType().equals(IntType.v()) || l.getType().equals(LongType.v())) {
                            init = " = 0";
                        } else if (l.getType().equals(BooleanType.v())) {
                            init = " = false";
                        } else if (l.getType().equals(RefType.v("android.graphics.RectF"))) {
                            init = " = new android.graphics.RectF()";
                        } else if (l.getType().equals(RefType.v("android.content.res.AssetManager"))) {
                            init = " = context.getAssets()";
                        } else if (l.getType() instanceof RefType) {
                            unknownType = true;
                            for (String prefix : hunter.Constant.specialTypeNotFilter) {
                                if (l.getType().toString().startsWith(prefix)) {
                                    unknownType = false;
                                    break;
                                }
                            }
                        }

                        if (!unknownType) {
                            fileWriter.write(generateIndent(2) + translateType(l.getType()) + " " + translateImmediate(l));
                            fileWriter.write(init);
                        } else {
                            fileWriter.write(generateIndent(2) + "java.lang.Object " + translateImmediate(l) + " = null");
                        }
                        fileWriter.write(";" + "\n");
                    }
                }
                fileWriter.write("\n");

                // statements
                for (int i = 0; i < javaResults.get(sootMethod).size(); i++) {
                    fileWriter.write(generateIndent(1) + javaResults.get(sootMethod).get(i) + "\n");
                }


                // choose a return
                String returnStmt = chooseReturn(sootMethod);
                if (!returnStmt.equals("")) {
                    fileWriter.write(generateIndent(2) + returnStmt + ";\n");
                }

                // jimple result
                fileWriter.write("\n\n");
                for (int i = 0; i < jimpleOriginalResults.get(sootMethod).size(); i++) {
                    fileWriter.write(
                            "// " + generateIndent(2) + jimpleOriginalResults.get(sootMethod).get(i).toString() + " (" + jimpleOriginalResults.get(sootMethod).get(i).hashCode() + ") " + "\n");
                }

                fileWriter.write(generateIndent(1) + "}");

                // original method jimple
                fileWriter.write("\n\n/**" + sootMethod.getSignature());
                // SootMethod sootMethod = Scene.v().getMethod(method);
                for (Unit u : sootMethod.retrieveActiveBody().getUnits()) {
                    fileWriter.write("\n*\t\t" + u.toString() + " (" + u.hashCode() + ") " + "\n");
                }
                fileWriter.write("*/");

                fileWriter.write("\n\n");
            }
            fileWriter.write("}\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeHeadMethods(FileWriter fileWriter) throws IOException {
        for (SootMethod sm : Optimizer._sortByValue(orderedMethods).keySet()) {
            boolean skip = false;
            for (String s : hunter.Constant.methodBlockList) {
                if (sm.getSignature().equals(s)) {
                    skip = true;
                    break;
                }
            }
            for (String s : hunter.Constant.methodNameBlockList) {
                if (sm.getName().equals(s)) {
                    skip = true;
                    break;
                }
            }
            if (skip) {
                continue;
            }
            if (orderedMethods.get(sm) >= 9) {
                for (String u : generateInvoke(sm)) {
                    fileWriter.write(generateIndent(3) + u + ";\n");
                }
                // fileWriter.write(generateIndent(2) + "// " + methodRetType.get(sm) + "\n");
            }
        }
        for (SootMethod sm : Optimizer._sortByValue(orderedMethods).keySet()) {
            boolean skip = false;
            for (String s : hunter.Constant.methodBlockList) {
                if (sm.getSignature().equals(s)) {
                    skip = true;
                    break;
                }
            }
            for (String s : hunter.Constant.methodNameBlockList) {
                if (sm.getName().equals(s)) {
                    skip = true;
                    break;
                }
            }
            if (skip) {
                continue;
            }
            if (orderedMethods.get(sm) < 9) {
                for (String u : generateInvoke(sm)) {
                    fileWriter.write(generateIndent(3) + u + ";\n");
                }
                // fileWriter.write(generateIndent(2) + "// " + methodRetType.get(sm) + "\n");
                // fileWriter.write(
                // generateIndent(2) + methodNameAlias.get(sm) + "();// " +
                // methodRetType.get(sm) + "\n");
            }
        }

    }

    private ArrayList<String> generateInvoke(SootMethod method) {
        ArrayList<String> results = new ArrayList<>();
        // List<String> parameterType = new ArrayList<>();
        // for (Type t : method.getParameterTypes()) {
        // parameterType.add(t.toString());
        // }
        String methodName = methodNameAlias.get(method);
        String invoke = "";
        invoke += methodName + "(";
        int i = 0;
        int count = 0;
        for (Type t : method.getParameterTypes()) {
            if (methodSwitchIndex.get(method) != null && methodSwitchIndex.get(method).contains(i)) {
                count++;
                if (t instanceof PrimType) {
                    if (t instanceof BooleanType) {
                        results.add(t.toString() + " p" + method.hashCode() + "_" + i + " = true");
                    } else if (t instanceof ByteType) {
                        results.add(t.toString() + " p" + method.hashCode() + "_" + i + " = 0");
                    } else if (t instanceof CharType) {
                        results.add(t.toString() + " p" + method.hashCode() + "_" + i + " = ''");
                    } else if (t instanceof DoubleType) {
                        results.add(t.toString() + " p" + method.hashCode() + "_" + i + " = 1.0");
                    } else if (t instanceof FloatType) {
                        results.add(t.toString() + " p" + method.hashCode() + "_" + i + " = 1.0F");
                    } else if (t instanceof IntType) {
                        results.add(t.toString() + " p" + method.hashCode() + "_" + i + " = 300");
                    } else if (t instanceof LongType) {
                        results.add(t.toString() + " p" + method.hashCode() + "_" + i + " = 1L");
                    } else if (t instanceof ShortType) {
                        results.add(t.toString() + " p" + method.hashCode() + "_" + i + " = 1");
                    }
                } else if (t instanceof ArrayType) {
                    Type baseType = ((ArrayType) t).baseType;
                    int dimension = ((ArrayType) t).numDimensions;
                    String sizes = "";
                    for (int d = 0; d < dimension; d++) {
                        sizes += "[1]";
                    }
                    results.add(
                            baseType.toString() + sizes.replace("1", "") + " p" + method.hashCode() + "_" + i + " = new " + baseType.toString() + sizes);
                } else if (t instanceof RefType) {
                    if (t.equals(RefType.v("java.lang.String"))) {
                        results.add(t.toString() + " p" + method.hashCode() + "_" + i + " = \"\"");
                    } else if (t.equals(RefType.v("android.graphics.Bitmap"))) {
                        results.add(t.toString() + " p" + method.hashCode() + "_" + i + " = picture");
                    } else if (t.equals(RefType.v("android.content.res.AssetManager"))) {
                        results.add(t.toString() + " p" + method.hashCode() + "_" + i + " = context.getAssets()");
                    } else {
                        results.add(
                                t.toString() + " p" + method.hashCode() + "_" + i + " = new " + t.toString() + "()");
                    }
                } else {
                    results.add("Object p" + method.hashCode() + "_" + i + " = null// WARNING: Unknown type");
                }
                invoke += "p" + method.hashCode() + "_" + i;
                if (count < methodSwitchIndex.get(method).size()) {
                    invoke += ", ";
                }
            }
            i++;
        }
        invoke += ")";

        results.add(invoke);
        return results;
    }

    private String generateIndent(int layer) {
        String indent = "";
        for (int i = 0; i < layer; i++) {
            for (int j = 0; j < indentSpaces; j++) {
                indent += " ";
            }
        }
        return indent;
    }

    private String translateType(Type type) {
        for (String prefix : hunter.Constant.specialTypeNotFilter) {
            if (type.toString().startsWith(prefix)) {
                return type.toString().replaceAll("\\$", ".");
            }
        }

        // todo: temp solution
        if (type.toString().equals("org.tensorflow.lite.Tensor")) {
            return "org.tensorflow.lite.Tensor";
        }

        if (!FeatureBuilder.getSinkClassName(type).equals("")) {
            return FeatureBuilder.getSinkClassName(type);
        }

        return type.toString().replaceAll("\\$", ".");
    }

    private String translateMethodSignature(SootMethod sm) {
        String methodName = methodNameAlias.get(sm);
        System.out
                .println("translateMethodSignature " + sm.getSignature() + "  " + methodNameAlias.get(sm));
        String returnType = translateType(methodRetType.get(sm));
        List<String> parameterType = new ArrayList<>();
        for (Type t : sm.getParameterTypes()) {
            parameterType.add(translateType(t));
        }

        String result = "";
        result += returnType + " ";
        result += methodName + "(";
        int i = 0;
        int count = 0;
        for (String p : parameterType) {
            if (methodSwitchIndex.get(sm) != null && methodSwitchIndex.get(sm).contains(i)) {
                result += p + " p" + i;
                if (count < methodSwitchIndex.get(sm).size() - 1) {
                    result += ", ";
                    count++;
                }
            }
            i++;
        }
        result += ")";
        return result;

        // return methodAlias.get(sm);
    }

    // eg: $i1 --> i1
    private String translateImmediate(Value v) {
        String result = v.toString();
        if (v instanceof Local && ((Local) v).getType().equals(RefType.v("android.graphics.Bitmap"))) {
            result = hunter.Constant.bitmapFieldName;
        } else if (v instanceof Local) {
            needLocals.add((Local) v);
            result = result.replaceAll("\\$", "");
        } else if (v instanceof StringConstant) {
            String path = ((StringConstant) v).value;
            boolean isModelPath = false;
            for (String suffix : hunter.Constant.modelSuffix) {
                if (path.endsWith(suffix)) {
                    isModelPath = true;
                    break;
                }
            }
            if (isModelPath && path.contains("/")) {
                String filename = path.split("/")[path.split("/").length - 1];
                // String filename = path.substring(path.lastIndexOf("/" + 1));
                result = "\"" + filename + "\"";
            }
        }
        return result;
    }

    // eg: $r6[2] --> r6[2]
    // eg: $r6[$i1] --> r6[i1]
    private String translateArrayRef(ArrayRef arrayRef) {
        Value base = arrayRef.getBase();
        Value index = arrayRef.getIndex();
        return translateImmediate(base) + "[" + translateImmediate(index) + "]";
    }

    // eg: $r0.<uk.tensorzoom.browser.c: uk.tensorzoom.c c> --> var0
    private String translateInstanceFieldRef(InstanceFieldRef ifRef) {
        // String result = ifRef.getField().getSignature();

        String result = "";
        SootField field = ifRef.getField();
        if (field.getType().equals(RefType.v("android.graphics.Bitmap"))) {
            result = hunter.Constant.bitmapFieldName;
        } else if (fieldAlias.containsKey(field)) {
            result = fieldAlias.get(field);
        } else {
            Value base = ifRef.getBase();
            result = translateImmediate(base) + "." + field.getName();
            result = result.replaceAll("\\$", ".");
        }
        System.out.println(
                "translateInstanceFieldRef -> " + ifRef.getField().toString() + " " + fieldAlias.get(ifRef.getField()));

        return result;
    }

    // eg: <uk.tensorzoom.browser.a: uk.tensorzoom.c a> --> var0
    private String translateStaticFieldRef(StaticFieldRef sfRef) {
        // SootField field = sfRef.getField();
        // return field.getDeclaringClass().getName() + field.getName();

        // String result = sfRef.getField().getSignature();

        String result = "";
        SootField field = sfRef.getField();
        if (field.getType().equals(RefType.v("android.graphics.Bitmap"))) {
            result = hunter.Constant.bitmapFieldName;
        } else if (fieldAlias.containsKey(field)) {
            result = fieldAlias.get(field);
        } else {
            result = field.getDeclaringClass().getName() + "." + field.getName();
            result = result.replaceAll("\\$", ".");
        }
        System.out.println(
                "translateStaticFieldRef -> " + sfRef.getField().toString() + " " + fieldAlias.get(sfRef.getField()));

        return result;
    }

    private String translateInvokeExpr(InvokeExpr invokeExpr, ArrayList<String> insertBeforeUnit) {
        System.out.println("into translateInvokeExpr -> " + invokeExpr.toString());
        String result = "";
        SootMethod method = invokeExpr.getMethod();


        for (Unit u : caller2Callee.keySet()) {
            InvokeExpr ie = UnitHelper.GetInvokeExpr(u);
            if (ie != null && ie.equals(invokeExpr)) {
                method = caller2Callee.get(u);
            }
        }
        boolean noArgs = false;
        if (methodSwitchIndex.containsKey(method)) {
            if (invokeExpr instanceof StaticInvokeExpr) {
                // eg: staticinvoke <kotlin.LazyKt: kotlin.Lazy
                // lazy(kotlin.jvm.functions.Function0)>($r5)
                // --> kotlin.LazyKt.lazy(r5)
                // result += method.getDeclaringClass().getName() + "." +
                result += methodNameAlias.get(method);
            } else if (invokeExpr instanceof SpecialInvokeExpr && method.isConstructor()) {
                // eg: specialinvoke $r4.<uk.tensorzoom.c$a: void
                // <init>(android.content.Context,java.lang.String)>($r1,
                // $r2) -->
                // result += translateImmediate(((InstanceInvokeExpr) invokeExpr).getBase()) +
                // "." + nameAlias.get(method);
                result += methodNameAlias.get(method);
            } else if (invokeExpr instanceof InstanceInvokeExpr) {
                // eg: $r1.<android.os.Parcel: double readDouble()>() --> r1.readDouble()
                // result += translateImmediate(((InstanceInvokeExpr) invokeExpr).getBase()) +
                // "." + nameAlias.get(method);
                result += methodNameAlias.get(method);
            }

            result += "(";
            if (invokeExpr.getArgCount() > 0) {
                int index = 0;
                int count = 0;
                for (Value p : invokeExpr.getArgs()) {
                    if (methodSwitchIndex.get(method).contains(index)) {
                        count++;
                        if (p instanceof IntConstant && method.getParameterType(index) instanceof BooleanType) {
                            if (((IntConstant) p).value == 0) {
                                result += "false";
                            } else {
                                result += "true";
                            }
                        } else {
                            result += translateImmediate(p);
                        }
                        if (count < methodSwitchIndex.get(method).size()) {
                            result += ", ";
                        }
                    }
                    index++;
                }
            }
            result += ")";
        } else {
            String sinkName = translateStdSink(invokeExpr);
            // result += "[" + invokeExpr.getMethod().getSignature() + "] ";
            if (hunter.Constant.knownObfuscatedMethods.containsKey(method.getSignature())) {
                result += translateImmediate(((InstanceInvokeExpr) invokeExpr).getBase()) + "." + hunter.Constant.knownObfuscatedMethods.get(method.getSignature());
            } else if (hunter.Constant.src2FeedStmts.containsKey(method.getSignature())) {
                result += translateImmediate(((InstanceInvokeExpr) invokeExpr).getBase()) + "." + method.getName();
                // for (String u : hunter.Constant.src2FeedStmts.get(method.getSignature())) {
                // insertBeforeUnit.add(
                // u.replace("[BITMAP]", translateImmediate(((InstanceInvokeExpr)
                // invokeExpr).getBase())));
                // }
            } else if (method.getSignature()
                    .equals("<kotlin.collections.ArraysKt___ArraysKt: java.lang.Iterable withIndex(int[])>")) {
                Value arg0 = invokeExpr.getArg(0);
                result += "java.util.Arrays.stream(" + translateImmediate(arg0)
                        + ").boxed().collect(java.util.stream.Collectors.toList())";
                noArgs = true;
            } else if (method.getSignature().equals("<java.util.PriorityQueue: void <init>(int,java.util.Comparator)>")) {
                if (invokeExpr instanceof SpecialInvokeExpr) {
                    Value lop = ((SpecialInvokeExpr) invokeExpr).getBase();
                    result += translateImmediate(lop) + " = new java.util.PriorityQueue()";
                    noArgs = true;
                }
            } else if (invokeExpr instanceof InstanceInvokeExpr
                    && Scene.v().getSootClass(((InstanceInvokeExpr) invokeExpr).getBase().getType().toString())
                    .hasSuperclass()
                    && Scene.v().getSootClass(((InstanceInvokeExpr) invokeExpr).getBase().getType().toString())
                    .getSuperclass()
                    .equals(Scene.v().getSootClass("android.support.v7.app.AppCompatActivity"))) {
                result += "this." + method.getName();
            } else if (method.getDeclaringClass().equals(Scene.v().getSootClass("android.content.SharedPreferences"))) {
                if (invokeExpr.getArgCount() == 2) {
                    result += invokeExpr.getArg(1);
                    return result;
                }
            } else if (method.equals(
                    Scene.v().getMethod("<android.content.Context: android.content.res.AssetManager getAssets()>"))) {
                result += "getResources()." + method.getName();
            } else if (method.getName().equals("getAssets")) {
                result += method.getName();
            } else if (invokeExpr instanceof StaticInvokeExpr) {
                // eg: staticinvoke <kotlin.LazyKt: kotlin.Lazy
                // lazy(kotlin.jvm.functions.Function0)>($r5)
                // --> kotlin.LazyKt.lazy(r5)
                if (sinkName.equals("")) {
                    result += method.getDeclaringClass().getName() + "." + method.getName();
                } else {
                    result += method.getDeclaringClass().getName() + "." + sinkName;
                }
            } else if (invokeExpr instanceof SpecialInvokeExpr && method.isConstructor()) {
                // eg: specialinvoke $r1.<java.io.BufferedInputStream: void
                // <init>(java.io.InputStream)>($r4) --> java.io.BufferedInputStream r1 = new
                // java.io.BufferedInputStream(r4)
                Value base = ((SpecialInvokeExpr) invokeExpr).getBase();
                String type = translateType(base.getType());
                if (sinkName.equals("")) {
                    result += translateImmediate(base) + " = new " + type;
                } else {
                    // insertBeforeUnit.add(getFrameworkInstance(base));
                    result += getFrameworkInstance(base) + " = new " + sinkName;
                    if (result.contains("org.tensorflow.contrib.android.TensorFlowInferenceInterface") && invokeExpr.getArgCount() > 1) {
                        String modelPath = translateImmediate(invokeExpr.getArg(1));
                        insertBeforeUnit.add(modelPath + " = " + modelPath + ".split(\"/\")[" + modelPath
                                + ".split(\"/\").length - 1]");
                        insertBeforeUnit.add(modelPath + " = \"file:///android_asset/\" + " + modelPath);
                    }
                }
            } else if (invokeExpr instanceof InstanceInvokeExpr) {
                // eg: $r1.<android.os.Parcel: double readDouble()>() --> r1.readDouble()
                if (((InstanceInvokeExpr) invokeExpr).getBase().getType().toString().equals("org.tensorflow.lite.Tensor")) {
                    result += translateImmediate(((InstanceInvokeExpr) invokeExpr).getBase()) + "." + ((InstanceInvokeExpr) invokeExpr).getMethod().getName();
                } else if (sinkName.equals("")) {
                    result += translateImmediate(((InstanceInvokeExpr) invokeExpr).getBase()) + "." + method.getName();
                } else {
                    // insertBeforeUnit.add(getFrameworkInstance(((InstanceInvokeExpr)invokeExpr).getBase()));
                    result += getFrameworkInstance(((InstanceInvokeExpr) invokeExpr).getBase()) + "." + sinkName;
                }
            }

            if (!noArgs) {
                result += "(";
                if (invokeExpr.getArgCount() > 0) {
                    int index = 0;
                    for (Value p : invokeExpr.getArgs()) {
                        if (index == 1 && method.isConstructor()
                                && sinkName.equals("org.tensorflow.lite.Interpreter")) {
                            result += "new org.tensorflow.lite.Interpreter.Options()";
                        } else if (p instanceof ClassConstant) {
                            if (p.toString().equals("class \"F\"")) {
                                result += "Float.TYPE";
                            } else if (p.toString().equals("class \"B\"")) {
                                result += "Byte.TYPE";
                            } else if (p.toString().equals("class \"I\"")) {
                                result += "Integer.TYPE";
                            }
                        } else if (p instanceof IntConstant && method.getParameterType(index) instanceof BooleanType) {
                            if (((IntConstant) p).value == 0) {
                                result += "false";
                            } else {
                                result += "true";
                            }
                        } else {
                            result += translateImmediate(p);
                        }
                        index++;
                        if (index < invokeExpr.getArgCount()) {
                            result += ", ";
                        }
                    }
                }
                result += ")";
            }

            if (method.getSignature().equals("<java.util.ListIterator: int nextIndex()>")) {
                result += " - 1";
            } else if (method.getSignature().equals("<java.nio.ByteBuffer: java.nio.ByteBuffer allocateDirect(int)>")) {
                result += ".order(java.nio.ByteOrder.nativeOrder())";
            }
        }
        return result;
    }

    private String getFrameworkInstance(Value base) {
        String type = translateType(base.getType());
        if (FeatureBuilder.getFrameworkInstance(type) != null) {
            frameworkFileds.add(type);
            return FeatureBuilder.getFrameworkInstance(type);
        }

        return translateImmediate(base);
    }

    private String translateStdSink(InvokeExpr expr) {
        SootMethod sm = expr.getMethod();
        if (FeatureBuilder.isSinkPoint(sm)) {
            return FeatureBuilder.getSinkMethodName(sm);
        }
        return "";
    }


    // eg: $i3 * $i2 --> i3 * i2
    private String translateBinopExpr(BinopExpr binopExpr) {
        Value op1 = binopExpr.getOp1();
        Value op2 = binopExpr.getOp2();
        String binop = binopExpr.getSymbol();
        String javaOp1 = translateImmediate(op1);
        String javaOp2 = translateImmediate(op2);
        if (binop.contains("cmpl")) {
            // <:1 >:-1 ==:0
            return "(byte) (" + javaOp1 + " < " + javaOp2 + " ? 1 : (" + javaOp1 + " > " + javaOp2 + " ? -1 : 0))";
        } else if (binop.contains("cmpg") || binop.contains("cmp")) {
            // >:1 <:-1 ==:0
            return "(byte) (" + javaOp1 + " > " + javaOp2 + " ? 1 : (" + javaOp1 + " < " + javaOp2 + " ? -1 : 0))";
        }

        String result = "";
        if (op1.getType() instanceof BooleanType && op2 instanceof IntConstant) {
            if (((IntConstant) op2).value == 0) {
                result = javaOp1 + binop + "false";
            } else {
                result = javaOp1 + binop + "true";
            }
        } else {
            result = javaOp1 + binop + javaOp2;
        }
        return result;
    }

    private String translateAliasUnit(AssignStmt u) {
        Value lop = u.getLeftOp();
        Value rop = u.getRightOp();
        String result = "";
        if (lop instanceof Local && rop instanceof Local) {
            result = translateImmediate(lop) + " = " + translateImmediate(rop);
        }
        return result;
    }

    private String translateUnit(Unit u) {
        System.out.println(String.format("translateUnit -> %s", u.toString()));
        String result = "";
        boolean skip = false;
        ArrayList<String> insertBeforeUnit = new ArrayList<>();
        ArrayList<String> insertAfterUnit = new ArrayList<>();
        if (u instanceof DefinitionStmt) {
            // assign_stmt = variable "=" rvalue;
            if (u instanceof AssignStmt) {
                Value lop = ((AssignStmt) u).getLeftOp();
                Value rop = ((AssignStmt) u).getRightOp();
                String javaLop = "";
                String javaRop = "";
                boolean needLop = true;
                boolean needBoolean = false;

                if (lop instanceof Local && rop instanceof Local) {
                    for (Unit initUnit : newObjects2Alias.keySet()) {
                        for (Unit aliasUnit : newObjects2Alias.get(initUnit))
                            if (u.equals(aliasUnit)) {
                                skip = true;
                            }
                    }
                }

                if (rop instanceof InvokeExpr) {
                    if (((InvokeExpr) rop).getMethod().isNative()) {
                        skip = true;
                    }

                    if (!methodRetType.containsKey(((InvokeExpr) rop).getMethod())) {
                        for (String s : hunter.Constant.methodSpecialList) {
                            if (((InvokeExpr) rop).getMethod().getSignature().equals(s)) {
                                needLop = false;
                            }
                        }
                        for (SootMethod sm : methodRetType.keySet()) {
                            boolean isSubclassMethod = sm.getDeclaringClass().getSuperclass().equals(((InvokeExpr) rop).getMethod().getDeclaringClass());
                            for (SootClass itfc : sm.getDeclaringClass().getInterfaces()) {
                                if (itfc.equals(((InvokeExpr) rop).getMethod().getDeclaringClass())) {
                                    isSubclassMethod = true;
                                }
                            }
                            if (sm.getName().equals(((InvokeExpr) rop).getMethod().getName())
                                    && methodRetType.get(sm) instanceof VoidType
                                    && sm.getParameterTypes().equals(((InvokeExpr) rop).getMethod().getParameterTypes())
                                    && isSubclassMethod) {
                                needLop = false;
                            }
                        }
                    } else if (methodRetType.get(((InvokeExpr) rop).getMethod()) instanceof VoidType) {
                        needLop = false;
                    }
                    for (String s : hunter.Constant.methodBlockList) {
                        // filter opencv method
                        if (((InvokeExpr) rop).getMethod().getSignature().equals(s)) {
                            skip = true;
                            break;
                        }
                    }
                    for (String s : hunter.Constant.methodNameBlockList) {
                        if (((InvokeExpr) rop).getMethod().getName().equals(s)) {
                            skip = true;
                            break;
                        }
                    }
                }

                if (needLop && !skip) {
                    if (lop.getType() instanceof BooleanType) {
                        needBoolean = true;
                    }

                    // LEFT: variable = array_ref | instance_field_ref | static_field_ref | local;
                    if (lop instanceof Local) {
                        // eg: $i1
                        javaLop += translateImmediate(lop);
                    } else if (lop instanceof ArrayRef) {
                        // eg: $r6[2]
                        // eg: $r6[$i1]
                        javaLop += translateArrayRef((ArrayRef) lop);
                    } else if (lop instanceof InstanceFieldRef) {
                        // eg: $r0.<uk.tensorzoom.browser.c: uk.tensorzoom.c c>
                        javaLop += translateInstanceFieldRef((InstanceFieldRef) lop);
                    } else if (lop instanceof StaticFieldRef) {
                        // eg: <uk.tensorzoom.browser.a: uk.tensorzoom.c a>
                        javaLop += translateStaticFieldRef((StaticFieldRef) lop);
                    } else {
                        javaLop += "UNKNOWN left op[" + lop.toString() + "]";
                        // System.out.println(String.format("[ERROR] encounter unknown Lop -> %s @ Unit
                        // -> ", lop.toString
                        // (), u.toString()));
                    }
                }

                // RIGHT: rvalue = array_ref | constant | expr | instance_field_ref | local |
                // static_field_ref
                if (rop instanceof Expr) {
                    // expr = binop_expr | cast_expr | instance_of_expr | invoke_expr |
                    // new_array_expr | new_expr |
                    // new_multi_array_expr | unop_expr;
                    if (rop instanceof BinopExpr) {
                        // eg: $i3 * $i2 --> i3 * i2
                        javaRop += translateBinopExpr((BinopExpr) rop);
                    } else if (rop instanceof InvokeExpr) {
                        if (!skip) {
                            if (((InvokeExpr) rop).getMethod().getDeclaringClass()
                                    .equals(Scene.v().getSootClass("android.content.res.Resources"))) {
                                // skip = true;
                                javaRop += hunter.Constant.resourcesMethod2Value
                                        .get(((InvokeExpr) rop).getMethod().getSignature());
                            } else if (rop instanceof InstanceInvokeExpr
                                    && Scene.v().getSootClass(((InstanceInvokeExpr) rop).getBase().getType().toString())
                                    .hasSuperclass()
                                    && Scene.v().getSootClass(((InstanceInvokeExpr) rop).getBase().getType().toString())
                                    .getSuperclass().equals(Scene.v()
                                            .getSootClass("android.support.v7.app.AppCompatActivity"))) {
                                needLocals.remove(((InstanceInvokeExpr) rop).getBase());
                                javaRop += translateInvokeExpr((InvokeExpr) rop, insertBeforeUnit);
                            } else {
                                javaRop += translateInvokeExpr((InvokeExpr) rop, insertBeforeUnit);
                            }
                        }
                    } else if (rop instanceof AnyNewExpr) {
                        if (rop instanceof NewArrayExpr) {
                            // eg: newarray (int)[$i0] --> new int[i0]
                            Type baseType = ((NewArrayExpr) rop).getBaseType();
                            Value size = ((NewArrayExpr) rop).getSize();
                            if (baseType instanceof ArrayType) {
                                System.out.println();
                                Type baseBaseType = TypeHelper.getBaseType((ArrayType) baseType);
                                javaRop += "new " + baseBaseType.toString() + "[" + translateImmediate(size) + "]";
                                int dimension = ((ArrayType) baseType).numDimensions;
                                for (int i = 0; i < dimension; i++) {
                                    javaRop += "[0]";
                                }
                            } else {
                                javaRop += "new " + baseType.toString() + "[" + translateImmediate(size) + "]";
                            }

//                            ArrayList<Value> sizes = new ArrayList<>();
//                            sizes.addAll(((NewMultiArrayExpr) rop).getSizes());
//                            Collections.reverse(sizes);
//                            for (Value size : sizes) {
//                                javaRop += "[" + translateImmediate(size) + "]";
//                            }

                        } else if (rop instanceof NewExpr) {
                            // eg: new java.util.ArrayList --> new java.util.ArrayList()
                            // javaRop += "new " + ((NewExpr) rop).getBaseType().toString() + "()";
                            skip = true;
                        } else if (rop instanceof NewMultiArrayExpr) {
                            // eg: newmultiarray (int)[$i0][$i1] --> new int[i0][i1]
                            Type base = ((NewMultiArrayExpr) rop).getBaseType();
                            javaRop += "new " + base.toString();
                            for (Value size : ((NewMultiArrayExpr) rop).getSizes()) {
                                javaRop += "[" + translateImmediate(size) + "]";
                            }
                        }
                    } else if (rop instanceof UnopExpr) {
                        if (rop instanceof LengthExpr) {
                            // eg: lengthof $r4 --> r4.length
                            javaRop += translateImmediate(((LengthExpr) rop).getOp()) + ".length";
                        } else if (rop instanceof NegExpr) {
                            // eg: - $i1 --> -i1
                            javaRop += "-" + translateImmediate(((NegExpr) rop).getOp());
                        }
                    } else if (rop instanceof CastExpr) {
                        // eg: (kotlin.reflect.KProperty) $r3 --> (kotlin.reflect.KProperty)r3
                        javaRop += "(" + translateType(((CastExpr) rop).getCastType()) + ")"
                                + translateImmediate(((CastExpr) rop).getOp());
                    } else if (rop instanceof InstanceOfExpr) {
                        // eg: $r1 instanceof android.app.Activity --> r1 instanceof
                        // android.app.Activity
                        javaRop += translateImmediate(((InstanceOfExpr) rop).getOp()) + " instanceof "
                                + ((InstanceOfExpr) rop).getCheckType().toString();
                    } else {
                        javaRop += "UNKNOWN right Expr[" + rop.toString() + "]";
                        // System.out.println(String.format("[ERROR] encounter unknown Right Expr -> %s
                        // @ Unit -> ",
                        // rop.toString(), u.toString()));
                    }
                } else if (rop instanceof ArrayRef) {
                    // eg: $r6[2]
                    // eg: $r6[$i1]
                    javaRop += translateArrayRef((ArrayRef) rop);
                } else if (rop instanceof Local) {
                    // eg: $i0
                    javaRop += translateImmediate(rop);
                } else if (rop instanceof Constant) {
                    // eg: 3L
                    // eg: 0 -> false
                    if (needBoolean && rop instanceof IntConstant) {
                        if (((IntConstant) rop).value == 0) {
                            javaRop += "false";
                        } else {
                            javaRop += "true";
                        }
                    } else {
                        javaRop += rop.toString();
                    }
                } else if (rop instanceof InstanceFieldRef) {
                    // eg: $r0.<uk.tensorzoom.browser.c: uk.tensorzoom.c c>
                    javaRop += translateInstanceFieldRef((InstanceFieldRef) rop);
                } else if (rop instanceof StaticFieldRef) {
                    // eg: <uk.tensorzoom.browser.a: uk.tensorzoom.c a>
                    javaRop += translateStaticFieldRef((StaticFieldRef) rop);
                } else {
                    javaRop += "UNKNOWN right op[" + rop.toString() + "]";
                    // System.out.println(String.format("[ERROR] encounter unknown Rop -> %s @ Unit
                    // -> ", rop.toString
                    // (), u.toString()));
                }

                if (needLop) {
                    result += javaLop + " = " + javaRop;
                } else {
                    result += javaRop;
                }
            } else if (u instanceof IdentityStmt) {
                Value lop = ((IdentityStmt) u).getLeftOp();
                Value rop = ((IdentityStmt) u).getRightOp();
                if (rop instanceof ParameterRef) {
                    result += translateImmediate(lop) + " = p" + ((ParameterRef) rop).getIndex();
                    SootMethod method = allUnits2Methods.get(u);
                    if (!methodSwitchIndex.containsKey(method)) {
                        methodSwitchIndex.put(method, new ArrayList<>());
                    }
                    if (!methodSwitchIndex.get(method).contains(((ParameterRef) rop).getIndex()))
                        methodSwitchIndex.get(method).add(((ParameterRef) rop).getIndex());
                } else if (rop instanceof ThisRef) {
                    skip = true;
                } else {
                    result += "UNKNOWN right op[" + rop.toString() + "]";
                    // System.out.println(String.format("[ERROR] encounter unknown Rop -> %s @ Unit
                    // -> ", rop.toString(), u.toString()));
                }
            }
        } else if (u instanceof InvokeStmt) {
            for (String s : hunter.Constant.methodBlockList) {
                // filter opencv method
                if (((InvokeStmt) u).getInvokeExpr().getMethod().getSignature().equals(s)) {
                    skip = true;
                    break;
                }
            }
            for (String s : hunter.Constant.methodNameBlockList) {
                if (((InvokeStmt) u).getInvokeExpr().getMethod().getName().equals(s)) {
                    skip = true;
                    break;
                }
            }

            if (!skip) {
                result += translateInvokeExpr(((InvokeStmt) u).getInvokeExpr(), insertBeforeUnit);
                if (newObjects2Alias.containsKey(u)) {
                    for (Unit alias : newObjects2Alias.get(u)) {
                        insertAfterUnit.add(translateAliasUnit(((AssignStmt) alias)));
                    }
                }

            }
        } else if (u instanceof ReturnStmt) {
            // result += "return " + translateImmediate(((ReturnStmt) u).getOp());
            skip = true;
        } else if (u instanceof ReturnVoidStmt) {
            // result += "return";
            skip = true;
        } else {
            result += "UNKNOWN STMT: ";
            result += u.toString();
            // System.out.println(String.format("[ERROR] encounter unknown Stmt -> %s ",
            // u.toString()));
        }

        if (skip) {
            System.out.println(String.format("translateUnit result -> [skip]"));
            return "";
        } else {
            String realResult = "";
            for (String s : insertBeforeUnit) {
                realResult += s;
                realResult += hunter.Constant.separator;
            }
            realResult += result;
            for (String s : insertAfterUnit) {
                realResult += hunter.Constant.separator;
                realResult += s;
            }
            System.out.println(String.format("translateUnit result -> %s", realResult));

            return realResult;
        }
    }
}
