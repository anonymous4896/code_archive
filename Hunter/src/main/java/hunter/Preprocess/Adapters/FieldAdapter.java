package hunter.Preprocess.Adapters;

import hunter.Constant;
import hunter.Utils.ListOperator;
import hunter.Utils.MethodHelper;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.BriefUnitGraph;

import java.util.ArrayList;
import java.util.HashMap;

public class FieldAdapter extends Adapter {
    private boolean getterAdapterOn = true;
    private boolean setterAdapterOn = true;

    public FieldAdapter(boolean verbose) {
        super(verbose);
        this.adapterName = "FieldAdapter";
    }

    public FieldAdapter(boolean verbose, boolean getterAdapterOn, boolean setterAdapterOn) {
        this(verbose);
        this.getterAdapterOn = getterAdapterOn;
        this.setterAdapterOn = setterAdapterOn;
    }

    public void go() {
        PrintAdapterLog("Start");

        if (getterAdapterOn) {
            _processGetter();
        }
        if (setterAdapterOn) {
            _processSetter();
        }
        PrintAdapterLog("Finish");
    }

    private void _processGetter() {
        HashMap<SootMethod, SootField> fieldGetters = new HashMap<>();
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (ListOperator._ListFuzzyContains(sootClass.getName(), Constant.specialMethods2Skip)) {
                continue;
            }

            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || MethodHelper.isSkippedMethod(sootMethod)) {
                    continue;
                }

                // only1Return
                ArrayList<ReturnStmt> returns = new ArrayList<>();
                for (Unit u : sootMethod.retrieveActiveBody().getUnits()) {
                    if (u instanceof ReturnStmt) {
                        returns.add((ReturnStmt) u);
                    }
                }
                boolean only1Return = returns.size() == 1;
                if (!only1Return) {
                    continue;
                }

                // return1Field
                BriefUnitGraph briefUnitGraph = new BriefUnitGraph(sootMethod.retrieveActiveBody());
                boolean return1Field = false;
                ReturnStmt returnStmt = returns.get(0);
                SootField fieldToGet = null;
                for (Unit u : briefUnitGraph.getPredsOf(returnStmt)) {
                    if (u instanceof AssignStmt) {
                        Value lop = ((AssignStmt) u).getLeftOp();
                        Value rop = ((AssignStmt) u).getRightOp();
                        if (lop.equals(returnStmt.getOp()) && rop instanceof FieldRef) {
                            if (fieldToGet == null || ((FieldRef) rop).getField().equals(fieldToGet)) {
                                fieldToGet = ((FieldRef) rop).getField();
                                return1Field = true;
                            } else {
                                return1Field = false;
                                break;
                            }
                        }
                    }
                }


                boolean isGetter = only1Return && return1Field && sootMethod.retrieveActiveBody().getUnits().size() < 5;
                if (isGetter) {
                    fieldToGet.setModifiers(fieldToGet.getModifiers() / 8 * 8 | Modifier.PUBLIC);
                    fieldGetters.put(sootMethod, fieldToGet);

                    if(verbose){
                        PrintAdapterLog(" " + sootMethod.retrieveActiveBody().getUnits().size() + "    " + sootMethod.getSignature());
                        for (Unit u : sootMethod.retrieveActiveBody().getUnits()) {
                            PrintAdapterLog("                " + u.toString());
                        }                        
                    }
                }
            }
        }

        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (ListOperator._ListFuzzyContains(sootClass.getName(), Constant.specialMethods2Skip)) {
                continue;
            }

            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || MethodHelper.isSkippedMethod(sootMethod)) {
                    continue;
                }

                Body body = sootMethod.retrieveActiveBody();
                for (Unit u : body.getUnits()) {
                    if (u instanceof AssignStmt) {
                        Value lop = ((AssignStmt) u).getLeftOp();
                        Value rop = ((AssignStmt) u).getRightOp();
                        if (rop instanceof InvokeExpr) {
                            SootMethod method = ((InvokeExpr) rop).getMethod();
                            if (fieldGetters.containsKey(method)) {
                                SootField field = fieldGetters.get(method);
                                if (field.isStatic()) {
                                    ((AssignStmt) u).setRightOp(Jimple.v().newStaticFieldRef(field.makeRef()));
                                } else {
                                    Local base = Jimple.v().newLocal("base" + u.hashCode(), RefType.v(field.getDeclaringClass()));
                                    ((AssignStmt) u).setRightOp(Jimple.v().newInstanceFieldRef(base, field.makeRef()));
                                    body.getLocals().add(base);
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    private void _processSetter() {
        HashMap<SootMethod, SootField> fieldSetters = new HashMap<>();
        HashMap<SootMethod, Integer> fieldSetterParams = new HashMap<>();
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (ListOperator._ListFuzzyContains(sootClass.getName(), Constant.specialMethods2Skip)) {
                continue;
            }

            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || MethodHelper.isSkippedMethod(sootMethod)) {
                    continue;
                }

//                // return void
//                boolean returnVoid = sootMethod.getReturnType().equals(VoidType.v());
//                if(!returnVoid){
//                    continue;
//                }

                // lessThan1Return
                ArrayList<Unit> returns = new ArrayList<>();
                for (Unit u : sootMethod.retrieveActiveBody().getUnits()) {
                    if (u instanceof ReturnVoidStmt || u instanceof ReturnStmt) {
                        returns.add(u);
                    }
                }
                boolean lessThan1Return = returns.size() <= 1;
                if (!lessThan1Return) {
                    continue;
                }

//                // only1Param
//                boolean only1Param = sootMethod.getParameterCount() == 1;
//                if(!only1Param){
//                    continue;
//                }

                // set1Field
                boolean set1Field = false;
                SootField fieldToSet = null;
                int fieldValueParamIndex = -1;
                BriefUnitGraph briefUnitGraph = new BriefUnitGraph(sootMethod.retrieveActiveBody());
                Body body = sootMethod.retrieveActiveBody();
                for (Unit u : body.getUnits()) {
                    if (u instanceof AssignStmt && ((AssignStmt) u).getLeftOp() instanceof FieldRef) {
                        Value lop = ((AssignStmt) u).getLeftOp();
                        Value rop = ((AssignStmt) u).getRightOp();
                        for (Unit u1 : briefUnitGraph.getPredsOf(u)) {
                            if (u1 instanceof IdentityStmt && ((IdentityStmt) u1).getRightOp() instanceof ParameterRef && ((IdentityStmt) u1).getLeftOp().equals(rop)) {
                                if (fieldToSet == null || ((FieldRef) lop).getField().equals(fieldToSet)) {
                                    fieldToSet = ((FieldRef) lop).getField();
                                    fieldValueParamIndex = ((ParameterRef) ((IdentityStmt) u1).getRightOp()).getIndex();
                                    set1Field = true;
                                } else {
                                    set1Field = false;
                                    break;
                                }
                            }
                        }
                    }
                }


                boolean isGetter = lessThan1Return && set1Field && sootMethod.retrieveActiveBody().getUnits().size() < 5;
                if (isGetter) {
                    fieldToSet.setModifiers(fieldToSet.getModifiers() / 8 * 8 | Modifier.PUBLIC);
                    fieldSetters.put(sootMethod, fieldToSet);
                    fieldSetterParams.put(sootMethod, fieldValueParamIndex);

                    if(verbose){
                         PrintAdapterLog(" " + sootMethod.retrieveActiveBody().getUnits().size() + "    " + sootMethod.getSignature());
                         for (Unit u : sootMethod.retrieveActiveBody().getUnits()) {
                             PrintAdapterLog("                " + u.toString());
                         }
                    }
                }
            }
        }

//        PrintAdapterLog(fieldSetters.toString());
//        PrintAdapterLog(fieldSetterParams.toString());

        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (ListOperator._ListFuzzyContains(sootClass.getName(), Constant.specialMethods2Skip)) {
                continue;
            }

            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || MethodHelper.isSkippedMethod(sootMethod)) {
                    continue;
                }

                Body body = sootMethod.retrieveActiveBody();
                PatchingChain<Unit> unitsChain = body.getUnits();
                ArrayList<Unit> units = new ArrayList<>(body.getUnits());
                for (int i = 0; i < units.size(); i++) {
                    Unit u = units.get(i);
                    if (u instanceof InvokeStmt) {
                        SootMethod method = ((InvokeStmt) u).getInvokeExpr().getMethod();
                        if (fieldSetters.containsKey(method)) {
                            SootField fieldToSet = fieldSetters.get(method);
                            Value fieldValue = ((InvokeStmt) u).getInvokeExpr().getArg(fieldSetterParams.get(method));

                            Unit newAssign = null;
                            if (fieldToSet.isStatic()) {
                                newAssign = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(fieldToSet.makeRef()), fieldValue);
                            } else {
                                Local base = Jimple.v().newLocal("base" + u.hashCode(), RefType.v(fieldToSet.getDeclaringClass()));
                                newAssign = Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(base, fieldToSet.makeRef()), fieldValue);
                                body.getLocals().add(base);
                            }

                            unitsChain.insertBefore(newAssign, u);
                            unitsChain.remove(u);
                        }

//                        Value lop = ((AssignStmt) u).getLeftOp();
//                        Value rop = ((AssignStmt) u).getRightOp();
//                        if (rop instanceof InvokeExpr) {
//                            SootMethod method = ((InvokeExpr) rop).getMethod();
//                            if (fieldGetters.containsKey(method)) {
//                                SootField field = fieldGetters.get(method);
//                                if (field.isStatic()) {
//                                    ((AssignStmt) u).setRightOp(Jimple.v().newStaticFieldRef(field.makeRef()));
//                                } else {
//                                    Local base = Jimple.v().newLocal("base" + u.hashCode(), RefType.v(field.getDeclaringClass()));
//                                    ((AssignStmt) u).setRightOp(Jimple.v().newInstanceFieldRef(base, field.makeRef()));
//                                    body.getLocals().add(base);
//                                }
//
//                            }
//                        }
                    }
                }
            }
        }
    }
}
