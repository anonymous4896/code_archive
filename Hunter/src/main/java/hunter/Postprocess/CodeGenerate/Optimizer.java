package hunter.Postprocess.CodeGenerate;

import heros.solver.Pair;
import hunter.Constant;
import hunter.Preprocess.FeatureBuilder;
import hunter.Utils.*;
import hunter.Track.Tracker;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.HashMutableEdgeLabelledDirectedGraph;

import java.util.*;

public class Optimizer {
    // inputs
    private HashMap<SootMethod, ArrayList<Unit>> sinks;
    private HashMutableEdgeLabelledDirectedGraph<Unit, String> resultJimpleGraph;
    private HashMap<SootMethod, ArrayList<Pair<Pair<Unit, Integer>, Pair<Unit, Integer>>>> loopRecord;
    private HashMap<Type, Unit> length2ArrayPair;
    public HashMap<Unit, SootMethod> caller2Callee;
    public HashMap<Unit, HashSet<Integer>> methodValueSwitchIndex;

    // outputs
    public HashMap<SootMethod, ArrayList<Unit>> jimpleResults; // all results units
    public HashMap<SootMethod, Integer> orderedMethods;
    public HashMap<SootMethod, String> methodAlias;
    public HashMap<SootMethod, String> nameAlias;
    public HashMap<SootField, String> fieldAlias;
    public HashMap<SootMethod, Type> methodRetType;
    public HashMap<SootMethod, ArrayList<Integer>> methodSwitchIndex;
    public HashMap<SootMethod, ArrayList<Unit>> originalJimpleResults;

    // variables
    private HashMap<Unit, SootMethod> unit2MethodMap;
    private HashMap<SootMethod, HashSet<SootField>> fields2ReadOfHeadMethods; //fields to read of head methods
    private HashMap<SootMethod, HashSet<SootField>> fields2WriteOfHeadMethods; //fields to write of head methods

    public Optimizer(Tracker tracker, HashMap<SootMethod, ArrayList<Unit>> sinks) {
        // inputs
        this.sinks = sinks;
        this.resultJimpleGraph = tracker.resultJimpleGraph;
        this.loopRecord = tracker.loopRecord;
        this.length2ArrayPair = tracker.length2ArrayPair;
        this.caller2Callee = tracker.caller2Callee;
        this.methodValueSwitchIndex = tracker.methodValueSwitchIndex;

        // outputs
        this.jimpleResults = new HashMap<>();
        this.orderedMethods = new HashMap<>();
        this.methodAlias = new HashMap<>();
        this.nameAlias = new HashMap<>();
        this.fieldAlias = new HashMap<>();
        this.methodRetType = new HashMap<>();
        this.methodSwitchIndex = new HashMap<>();
        this.originalJimpleResults = new HashMap<>();

        // variables
        this.unit2MethodMap = new HashMap<>();
        this.fields2ReadOfHeadMethods = new HashMap<>();
        this.fields2WriteOfHeadMethods = new HashMap<>();
    }

    private void prepare(){
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody()) continue;
                for (Unit unit : sootMethod.retrieveActiveBody().getUnits()) {
                    unit2MethodMap.put(unit, sootMethod);
                }
            }
        }

        for (Unit unit : resultJimpleGraph) {
            SootMethod currentMethod = unit2MethodMap.get(unit);
            if (!jimpleResults.containsKey(currentMethod)) {
                jimpleResults.put(currentMethod, new ArrayList<>(Arrays.asList(unit)));
            } else {
                jimpleResults.get(currentMethod).add(unit);
            }
        }

        arrayLengthAdapter();

        this.originalJimpleResults.putAll(jimpleResults);

        for (Unit unit : methodValueSwitchIndex.keySet()) {
            ArrayList<Integer> sortIndexes = new ArrayList<>();
            sortIndexes.addAll(methodValueSwitchIndex.get(unit));

            if (methodSwitchIndex.containsKey(caller2Callee.get(unit))) {
                for (int idx : sortIndexes) {
                    if (!methodSwitchIndex.get(caller2Callee.get(unit)).contains(idx)) {
                        methodSwitchIndex.get(caller2Callee.get(unit)).add(idx);
                    }
                }
            } else {

                methodSwitchIndex.put(caller2Callee.get(unit), sortIndexes);
            }
            methodSwitchIndex.get(caller2Callee.get(unit)).sort(Comparator.naturalOrder());
        }
    }

    void arrayLengthAdapter() {
        SootClass lengthClass = new SootClass("lengthClass4654314437", Modifier.PUBLIC);
        lengthClass.setSuperclass(Scene.v().loadClassAndSupport("java.lang.Object"));

        for (Type t : length2ArrayPair.keySet()) {
            SootField lengthField = new SootField(TypeHelper.getBaseType((ArrayType) t).toString().replace(".", "_") + ((ArrayType) t).numDimensions, IntType.v(), Modifier.STATIC | Modifier.PUBLIC);
            lengthClass.addField(lengthField);
            for (SootMethod sm : jimpleResults.keySet()) {
                HashMap<Unit, Unit> replace = new HashMap<>();
                for (Unit unit : jimpleResults.get(sm)) {
                    if (unit instanceof AssignStmt) {
                        Value rop = ((AssignStmt) unit).getRightOp();
                        if (rop instanceof LengthExpr && ((LengthExpr) rop).getOp().getType().equals(t)) {
                            // lop = r1.length --> lop = lengthField
                            ((AssignStmt) unit).setRightOp(Jimple.v().newStaticFieldRef(lengthField.makeRef()));
                        } else if (rop instanceof NewArrayExpr && unit.equals(length2ArrayPair.get(t))) {
                            // r0 = newarray (type)[size] --> length = size; lengthField = length

                            // need insert new unit
                            // length = 2
                            // a_a_b_f_1 = length0

                            // length = size

                            // lengthField = length
                            ((AssignStmt) unit).setRightOp(((NewArrayExpr) rop).getSize());
                            ((AssignStmt) unit).setLeftOp(Jimple.v().newStaticFieldRef(lengthField.makeRef()));

                        }
                    }
                }

            }

        }
    }

    // SootMethod and its alias
    // Field and its alias
    // All "this" should be removed
    // Call Order ...
    public void go() {
        System.out.println("into code generator go");
        prepare();

        for (SootMethod sootMethod : jimpleResults.keySet()) {
            String smName = sootMethod.getName().replaceAll("<", "").replaceAll(">", "").replaceAll("\\$", "");

            Type ret = VoidType.v();
            if (_need2Maintain(sootMethod.getReturnType()) && _methodHasRetStmt(sootMethod)) {
                ret = sootMethod.getReturnType();
            }
            methodRetType.put(sootMethod, ret);

            String retType = ret.toString();
            retType = retType + " ";
            retType = retType + smName + Math.abs(sootMethod.hashCode()) + "(";
            nameAlias.put(sootMethod, smName + Math.abs(sootMethod.hashCode()));
            List<Type> paramTypes = sootMethod.getParameterTypes();

            // to check whether all callee methods are added.
            int counter = 0;

            if (methodSwitchIndex.containsKey(sootMethod)) {
                for (int idx : methodSwitchIndex.get(sootMethod)) {
                    if (_need2Maintain(paramTypes.get(idx))) {
                        retType = retType + paramTypes.get(idx).toString() + " p" + idx;
                        counter++;
                        if (counter < methodSwitchIndex.get(sootMethod).size()) {
                            retType = retType + ", ";
                        }
                    }
                }
            }
            retType += ")";
            // System.out.println(String.format("method's alias -> %s (%s)", retType,
            // sootMethod.getSignature()));

            methodAlias.put(sootMethod, retType);

            for (Unit unit : jimpleResults.get(sootMethod)) {
                if (unit instanceof AssignStmt) {
                    if (((AssignStmt) unit).getLeftOp() instanceof FieldRef) {
                        SootField sootField = ((FieldRef) ((AssignStmt) unit).getLeftOp()).getField();
                        if (!_ListFuzzyContains(sootField.getDeclaringClass().getName(), hunter.Constant.specialTypeNotFilter, hunter.Constant.specialType2Filter)) {
                            fieldAlias.put(sootField, "f" + Math.abs(sootField.hashCode()) + "_" + sootField.getName());
                        }
                    } else if (((AssignStmt) unit).getRightOp() instanceof FieldRef) {
                        SootField sootField = ((FieldRef) ((AssignStmt) unit).getRightOp()).getField();
                        if (!_ListFuzzyContains(sootField.getDeclaringClass().getName(), hunter.Constant.specialTypeNotFilter, hunter.Constant.specialType2Filter)) {
                            fieldAlias.put(sootField, "f" + Math.abs(sootField.hashCode()) + "_" + sootField.getName());
                        }
                    }
                }
            }
        }

        ArrayList<SootMethod> calleeMethods = new ArrayList<>();
        Iterator<Unit> it = resultJimpleGraph.iterator();
//         if (it.hasNext()) {
//             int counter = 0;
//             System.out.println("Counter !!!!");
//             for (Unit u = it.next(); it.hasNext(); u = it.next()) {
//                 if(tracker.caller2Callee.get(u)!=null){
//                     calleeMethods.add(tracker.caller2Callee.get(u));
//                     counter++;
//                 }
//             }
//             System.out.println(String.format("Callee Methods Number -> %d", counter));
//         }
        int counter = 0;
        while (it.hasNext()) {

            Unit u = it.next();

            if (caller2Callee.get(u) != null) {
                calleeMethods.add(caller2Callee.get(u));
                counter++;
            }
        }

        // System.out.println(String.format("Callee Methods Number -> %d; value set -> %d", counter, tracker.caller2Callee.keySet().size()));

//        calleeMethods.addAll(tracker.caller2Callee.values());

        for (SootMethod sootMethod : jimpleResults.keySet()) {
            if (!calleeMethods.contains(sootMethod)) {
                orderedMethods.put(sootMethod, Integer.MAX_VALUE);
            }
        }

        // for(SootMethod sMethod : calleeMethods){
        //     System.out.println(String.format("Callee Methods -> %s", sMethod.getSignature()));
        // }
        // All methods -> jimpleResults.keySet()
        // Ordered methods results -> head methods and their order
        // Sink methods -> sinks
        // sink methods' order -> sinkOrderList

        for (SootMethod sootMethod : sinks.keySet()) {
            for (Unit unit : sinks.get(sootMethod)) {
                int c_sinkOrder = Integer.MAX_VALUE;

                if (UnitHelper.GetInvokeExpr(unit) != null) {
                    InvokeExpr invokeExpr = UnitHelper.GetInvokeExpr(unit);
                    SootMethod sm = invokeExpr.getMethod();
                    if (FeatureBuilder.isSinkPoint(sm)) {
                        c_sinkOrder = FeatureBuilder.getSinkOrder(sm);
                    }
                }


                ArrayList<Unit> visitedUnit = new ArrayList<>();
                if (c_sinkOrder != Integer.MAX_VALUE) {
                    _taintHeads(sootMethod, unit, c_sinkOrder, visitedUnit);
                }
            }
        }

        for (SootMethod sootMethod : orderedMethods.keySet()) {

            fields2WriteOfHeadMethods.put(sootMethod, new HashSet<>());
            fields2ReadOfHeadMethods.put(sootMethod, new HashSet<>());
            ArrayList<Unit> visitedUnits = new ArrayList<>();
            for (Unit unit : jimpleResults.get(sootMethod)) {
                _collectFields(sootMethod, unit, visitedUnits);
            }
        }

        ArrayList<SootMethod> headMethod4Field = new ArrayList<>();

        for (SootMethod sootMethod : _sortByValue(orderedMethods).keySet()) {
            // System.out.println(String.format("Method: %s (%s), Order: %d", methodAlias.get(sootMethod),
            //         sootMethod.getSignature(), orderedMethods.get(sootMethod)));
            if (orderedMethods.get(sootMethod) == Integer.MAX_VALUE) {
                headMethod4Field.add(sootMethod);
            }
        }

        for (SootMethod sootMethod : orderedMethods.keySet()) {

            fields2ReadOfHeadMethods.get(sootMethod).removeIf(s -> (fields2WriteOfHeadMethods.get(sootMethod).contains(s) && !TypeHelper.isArrayType(s.getType())));

            // System.out.println(String.format("Method -> %s, read -> %s, write -> %s", sootMethod.getSignature(),
            //         Arrays.toString(fields2ReadOfHeadMethods.get(sootMethod).toArray()),
            //         Arrays.toString(fields2WriteOfHeadMethods.get(sootMethod).toArray())));
        }

        _iterate(headMethod4Field, 10);

        // _reductUnnecessaryUnits();

    }

    private void _reductUnnecessaryUnits() {
        for (SootMethod sootMethod : jimpleResults.keySet()) {
            ArrayList<Unit> ifUnits2Filter = new ArrayList<>();
            ArrayList<Unit> whileIfUnits = new ArrayList<>();
            ArrayList<Unit> resultIfUnits = new ArrayList<>();

            if (loopRecord.containsKey(sootMethod)) {
                for (Pair<Pair<Unit, Integer>, Pair<Unit, Integer>> pair : loopRecord.get(sootMethod)) {
                    whileIfUnits.add(pair.getO1().getO1());
                }
            }

            for (Unit resultUnit : jimpleResults.get(sootMethod)) {
                if (resultUnit instanceof IfStmt) {
                    resultIfUnits.add(resultUnit); // record those units don't belong to the while units.
                }
            }

            for (Unit originalUnit : sootMethod.retrieveActiveBody().getUnits()) {
                if (originalUnit instanceof IfStmt && !resultIfUnits.contains(originalUnit) && !whileIfUnits.contains(originalUnit)) {
                    ifUnits2Filter.add(originalUnit);

                }
            }

            BriefUnitGraph briefUnitGraph = new BriefUnitGraph(sootMethod.retrieveActiveBody());
            for (Unit ifUnit : ifUnits2Filter) {
                ArrayList<Unit> units2Maintain = new ArrayList<>();
                ArrayList<Unit> visitedUnits = new ArrayList<>();
                Unit ifExitUnit = BranchHelper._findIfExit(new ArrayList<>(), (IfStmt) ifUnit, briefUnitGraph);
                Unit unit2Maintain = briefUnitGraph.getSuccsOf(ifUnit).get(0).equals(((IfStmt) ifUnit).getTarget()) ? briefUnitGraph.getSuccsOf(ifUnit).get(1) : briefUnitGraph.getSuccsOf(ifUnit).get(0);

                while (!unit2Maintain.equals(ifExitUnit) && !briefUnitGraph.getTails().contains(unit2Maintain)) {

                    if (!units2Maintain.contains(unit2Maintain)) units2Maintain.add(unit2Maintain);
                    if (visitedUnits.contains(unit2Maintain) && briefUnitGraph.getSuccsOf(unit2Maintain).size() > 1) {
                        if (!visitedUnits.contains(unit2Maintain)) visitedUnits.add(unit2Maintain);
                        if (unit2Maintain instanceof IfStmt) {
                            unit2Maintain = ((IfStmt) unit2Maintain).getTarget();
                        }

                    } else if (briefUnitGraph.getSuccsOf(unit2Maintain) != null && briefUnitGraph.getSuccsOf(unit2Maintain).size() == 1) {
                        if (!visitedUnits.contains(unit2Maintain)) visitedUnits.add(unit2Maintain);
                        unit2Maintain = briefUnitGraph.getSuccsOf(unit2Maintain).get(0);
                    } else if (!visitedUnits.contains(unit2Maintain) && briefUnitGraph.getSuccsOf(unit2Maintain) != null && briefUnitGraph.getSuccsOf(unit2Maintain).size() == 2) {
                        if (!visitedUnits.contains(unit2Maintain)) visitedUnits.add(unit2Maintain);
                        if (unit2Maintain instanceof IfStmt) {
                            Unit nowBranchTrue = ((IfStmt) unit2Maintain).getTarget();
                            unit2Maintain = briefUnitGraph.getSuccsOf(unit2Maintain).get(0).equals(nowBranchTrue) ? briefUnitGraph.getSuccsOf(unit2Maintain).get(1) : briefUnitGraph.getSuccsOf(unit2Maintain).get(0);
                            // = briefUnitGraph.getSuccsOf(unit2Maintain).get(0);
                        }

                    }
                }

                ArrayList<Unit> units2Remove = new ArrayList<Unit>();
                MethodHelper.getAllUnitsBetween(ifUnit, ifExitUnit, briefUnitGraph, units2Remove);
                units2Remove.remove(ifUnit);

                if (!units2Remove.isEmpty() && !ListOperator._ListIntersection(units2Maintain, jimpleResults.get(sootMethod)).isEmpty()) {
                    units2Remove.removeAll(units2Maintain);

                    jimpleResults.get(sootMethod).removeAll(units2Remove);
                }
            }
        }
    }

    private boolean _methodHasRetStmt(SootMethod sootMethod) {
        for (Unit unit : jimpleResults.get(sootMethod)) {
            if (unit instanceof ReturnStmt) return true;
        }
        return false;
    }

    private boolean _ListFuzzyContains(String str, ArrayList<String> toMaintain, ArrayList<String> toRemove) {
        boolean result = false;
        for (String s : toMaintain) {
            if (str.startsWith(s)) result = true;
        }
        for (String s : toRemove) {
            if (str.startsWith(s)) result = false;
        }
        return result;
    }

    private void _iterate(ArrayList<SootMethod> headMethod4Field, int order) {
        ArrayList<SootMethod> iteratedSms = new ArrayList<>();
        if (headMethod4Field.size() == 0) return;

        // System.out.println(String.format("There are still %d methods to process", headMethod4Field.size()));

        for (SootMethod head : headMethod4Field) {
            if (fields2ReadOfHeadMethods.get(head).size() == 0) {
                if (orderedMethods.get(head) >= order) {
                    orderedMethods.put(head, order);
                    iteratedSms.add(head);
                    // System.out.println(String.format("Method: %s (%s), Order: %d, Write: %s", methodAlias.get(head),
                    //         head.getSignature(), order,
                    //         Arrays.toString(fields2WriteOfHeadMethods.get(head).toArray())));
                    order++;
                }
            }
        }

        for (SootMethod head : headMethod4Field) {
            for (SootMethod ht : iteratedSms) {
                fields2ReadOfHeadMethods.get(head).removeAll(fields2WriteOfHeadMethods.get(ht));
            }
        }

        headMethod4Field.removeAll(iteratedSms);

        if (iteratedSms.size() == 0) {
            for (SootMethod t : headMethod4Field) {
                // System.out.println("!!Dead Loop!!");
                // System.out.println(String.format("Method %s (%s) has some problems. it reads %s and writes %s",
                //         t.getSignature(), nameAlias.get(t), Arrays.toString(fields2ReadOfHeadMethods.get(t).toArray()),
                //         Arrays.toString(fields2WriteOfHeadMethods.get(t).toArray())));
            }
            return;
        }
        _iterate(headMethod4Field, order);

    }

    private void _collectFields(SootMethod sootMethod, Unit unit, ArrayList<Unit> visitedUnits) {
        if (visitedUnits.contains(unit)) return;
        visitedUnits.add(unit);

        if (unit instanceof AssignStmt) {
            if (((AssignStmt) unit).getLeftOp() instanceof FieldRef) {
                fields2WriteOfHeadMethods.get(sootMethod).add(((FieldRef) ((AssignStmt) unit).getLeftOp()).getField());

            } else if (((AssignStmt) unit).getRightOp() instanceof FieldRef) {
                SootField sootField = ((FieldRef) ((AssignStmt) unit).getRightOp()).getField();
                if (!_ListFuzzyContains(sootField.getDeclaringClass().getName(), hunter.Constant.specialTypeNotFilter, hunter.Constant.specialType2Filter) || !_ListFuzzyContains(sootField.getType().toString(), hunter.Constant.specialTypeNotFilter, hunter.Constant.specialType2Filter)) {

                    fields2ReadOfHeadMethods.get(sootMethod).add(sootField);
                    if (TypeHelper.isArrayType(((AssignStmt) unit).getRightOp().getType())) {
                        fields2WriteOfHeadMethods.get(sootMethod).add(sootField);
                    }
                }
            }
        }

        if (resultJimpleGraph.getSuccsOf(unit) != null && resultJimpleGraph.getSuccsOf(unit).size() != 0) {
            for (Unit sU : resultJimpleGraph.getSuccsOf(unit)) {
                if (!resultJimpleGraph.getLabelsForEdges(unit, sU).get(0).equals(hunter.Constant.EDGE_LABEL_GLOBAL_FORWARD) && !resultJimpleGraph.getLabelsForEdges(unit, sU).get(0).equals(hunter.Constant.EDGE_LABEL_GLOBAL_BACKWARD)) {
                    _collectFields(sootMethod, sU, visitedUnits);
                }
            }
        }

        if (resultJimpleGraph.getPredsOf(unit) != null && resultJimpleGraph.getPredsOf(unit).size() != 0) {
            for (Unit pU : resultJimpleGraph.getPredsOf(unit)) {
                if (!resultJimpleGraph.getLabelsForEdges(pU, unit).get(0).equals(hunter.Constant.EDGE_LABEL_GLOBAL_FORWARD) && !resultJimpleGraph.getLabelsForEdges(pU, unit).get(0).equals(hunter.Constant.EDGE_LABEL_GLOBAL_BACKWARD)) {
                    _collectFields(sootMethod, pU, visitedUnits);
                }
            }
        }
    }

    private boolean _shouldNotBeFiltered(SootField field) {
        boolean result = false;
        for (String head : hunter.Constant.specialTypeNotFilter) {
            if (field.getDeclaringClass().getName().startsWith(head)) {
                result = true;
            }
        }
        for (String head : hunter.Constant.specialType2Filter) {
            if (field.getDeclaringClass().getName().startsWith(head)) {
                result = false;
            }
        }
        return result;
    }

    public static HashMap<SootMethod, Integer> _sortByValue(HashMap<SootMethod, Integer> heads) {
        // Create a list from elements of HashMap
        List<Map.Entry<SootMethod, Integer>> list = new LinkedList<>(heads.entrySet());

        // Sort the list
        list.sort(Comparator.comparing(Map.Entry::getValue));

        // put data from sorted list to hashmap
        HashMap<SootMethod, Integer> temp = new LinkedHashMap<>();
        for (Map.Entry<SootMethod, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    private void _taintHeads(SootMethod sootMethod, Unit unit, int c_sinkOrder, ArrayList<Unit> visitedUnits) {
        if (visitedUnits.contains(unit)) return;
        visitedUnits.add(unit);
        if (orderedMethods.containsKey(sootMethod) && orderedMethods.get(sootMethod) >= c_sinkOrder) {
            orderedMethods.put(sootMethod, c_sinkOrder);
            return;
        }
        // resultJimpleGraph.getHeads().contains(unit) ||
        // resultJimpleGraph.getTails().contains(unit)
        if (resultJimpleGraph.getPredsOf(unit) == null || resultJimpleGraph.getPredsOf(unit).size() == 0 || resultJimpleGraph.getSuccsOf(unit) == null || resultJimpleGraph.getSuccsOf(unit).size() == 0) {
            return;
        }

        for (Unit pU : resultJimpleGraph.getPredsOf(unit)) {
            if (resultJimpleGraph.getLabelsForEdges(pU, unit).get(0).equals(hunter.Constant.EDGE_LABEL_GLOBAL_FORWARD) || resultJimpleGraph.getLabelsForEdges(pU, unit).get(0).equals(hunter.Constant.EDGE_LABEL_GLOBAL_BACKWARD)) {
                // System.out.println(String.format("Global Edge [%s]: %s (%s) -> %s (%s)",
                //         resultJimpleGraph.getLabelsForEdges(pU, unit).get(0), pU.toString(),
                //         unit2MethodMap.get(pU).getSignature(), unit.toString(), sootMethod.getSignature()));
            } else {
                _taintHeads(unit2MethodMap.get(pU), pU, c_sinkOrder, visitedUnits);
            }

        }

        for (Unit sU : resultJimpleGraph.getSuccsOf(unit)) {

            if (resultJimpleGraph.getLabelsForEdges(unit, sU).get(0).equals(hunter.Constant.EDGE_LABEL_GLOBAL_FORWARD) || resultJimpleGraph.getLabelsForEdges(unit, sU).get(0).equals(hunter.Constant.EDGE_LABEL_GLOBAL_BACKWARD)) {
                // System.out.println(String.format("Global Edge [%s]: %s (%s) -> %s (%s)",
                //         resultJimpleGraph.getLabelsForEdges(unit, sU).get(0), unit.toString(),
                //         sootMethod.getSignature(), sU.toString(), unit2MethodMap.get(sU).getSignature()));
            } else {
                _taintHeads(unit2MethodMap.get(sU), sU, c_sinkOrder, visitedUnits);
            }
        }
    }

    private boolean _need2Maintain(Type returnType) {
        boolean flag = false;

        if (returnType instanceof PrimType || returnType instanceof VoidType || returnType instanceof ArrayType) {
            flag = true;
        } else {
            for (String t : hunter.Constant.specialTypeNotFilter) {
                if (returnType.toString().startsWith(t)) {
                    flag = true;
                    break;
                }
            }
            for (String t : Constant.specialType2Filter) {
                if (returnType.toString().startsWith(t)) {
                    flag = false;
                    break;
                }
            }
        }
        return flag;
    }
}
