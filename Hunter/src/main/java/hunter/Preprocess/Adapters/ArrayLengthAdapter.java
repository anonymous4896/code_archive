package hunter.Preprocess.Adapters;

import hunter.Constant;
import hunter.Utils.ListOperator;
import hunter.Utils.MethodHelper;
import hunter.Utils.UnitHelper;
import soot.*;
import soot.jimple.*;

import java.util.ArrayList;

public class ArrayLengthAdapter extends Adapter {
    public ArrayLengthAdapter(boolean verbose) {
        super(verbose);
        this.adapterName = "ArrayLengthAdapter";
    }

    public void go() {
        PrintAdapterLog("Start");
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (ListOperator._ListFuzzyContains(sootClass.getName(), Constant.specialMethods2Skip)) {
                continue;
            }

            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || MethodHelper.isSkippedMethod(sootMethod)) {
                    continue;
                }

                if (verbose) {
                    PrintAdapterLog("old method start");
                    for (Unit u : sootMethod.retrieveActiveBody().getUnits()) {
                        PrintAdapterLog("  " + u.toString());
                    }
                    PrintAdapterLog("old method end");
                }

                Body body = sootMethod.retrieveActiveBody();
                PatchingChain<Unit> unitsChain = body.getUnits();
                ArrayList<Unit> units = new ArrayList<>(body.getUnits());
                for (int i = 0; i < units.size(); i++) {
                    Unit u = units.get(i);
                    // if (u instanceof AssignStmt && ((AssignStmt) u).getLeftOp() instanceof StaticFieldRef) {
                    //     PrintAdapterLog("" + u.toString());
                    //     PrintAdapterLog(u.getUseAndDefBoxes().toString());
                    // }
                    if (u instanceof AssignStmt && ((AssignStmt) u).getRightOp() instanceof NewArrayExpr) {
                        NewArrayExpr newArrayExpr = (NewArrayExpr) ((AssignStmt) u).getRightOp();
                        Local size = Jimple.v().newLocal("size", IntType.v());
                        body.getLocals().add(size);

                        Unit newAssign = Jimple.v().newAssignStmt(size, newArrayExpr.getSize());
                        unitsChain.insertBefore(newAssign, u);
                        newArrayExpr.setSize(size);

                        if (verbose) {
                            PrintAdapterLog("ArrayLengthAdapter Result");
                            PrintAdapterLog("signature -> " + sootMethod.getSignature());
                            PrintAdapterLog("newAssign -> " + newAssign.toString() + "  " + newAssign.hashCode());
                            PrintAdapterLog("newarray -> " + u.toString() + "  " + u.hashCode());
                        }
                    }

                    InvokeExpr invokeExpr = UnitHelper.GetInvokeExpr(u);
                    if (invokeExpr != null && invokeExpr instanceof InstanceInvokeExpr) {
                        Value base = ((InstanceInvokeExpr) invokeExpr).getBase();
                        Type baseType = base.getType();
                        SootMethod fakeMethod = invokeExpr.getMethod();
                        if (!fakeMethod.isConcrete() || !fakeMethod.hasActiveBody()
                                || MethodHelper.isSkippedMethod(fakeMethod) || fakeMethod.isConstructor()) {
                            continue;
                        }
                        SootClass methodClass = fakeMethod.getDeclaringClass();
                        String invokeString = invokeExpr.toString();
                        String fakeClass = invokeString.substring(invokeString.indexOf("<") + 1,
                                invokeString.indexOf(":"));

                        if (!RefType.v(fakeClass).equals(baseType)) {
                            SootMethod realMethod = Scene.v().getSootClass(baseType.toString())
                                    .getMethod(fakeMethod.getSubSignature());
                            if (!realMethod.isConcrete() || !realMethod.hasActiveBody()
                                    || MethodHelper.isSkippedMethod(realMethod)) {
                                continue;
                            }

                            invokeExpr.setMethodRef(realMethod.makeRef());
                        }
                    }
                }

                if (verbose) {
                    PrintAdapterLog("new method start");
                    PrintAdapterLog(sootMethod.getSignature());
                    for (Unit u : sootMethod.retrieveActiveBody().getUnits()) {
                        PrintAdapterLog(" " + u.toString());
                    }
                    PrintAdapterLog("new method end");
                }
            }
        }

        PrintAdapterLog("Finish");
    }
}