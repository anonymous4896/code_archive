package hunter.Preprocess.Adapters;

import hunter.Constant;
import hunter.Utils.ListOperator;
import hunter.Utils.MethodHelper;
import hunter.Utils.UnitHelper;
import soot.*;
import soot.jimple.*;

import java.util.ArrayList;

public class KotlinIteratorAdapter extends Adapter {

    public KotlinIteratorAdapter(boolean verbose){
        super(verbose);
        this.adapterName = "KotlinIteratorAdapter";
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

                if(verbose){
                    PrintAdapterLog("=============old==============");
                    for (Unit u : sootMethod.retrieveActiveBody().getUnits()) {
                        PrintAdapterLog("  " + u.toString());
                    }
                    PrintAdapterLog("=============old==============");
                }

                try {
                    boolean modified = false;
                    Body body = sootMethod.retrieveActiveBody();
                    PatchingChain<Unit> unitsChain = body.getUnits();
                    ArrayList<Unit> units = new ArrayList<>(body.getUnits());
                    Value iterator = null;// --> java.util.ListIterator
                    Local listIterator = Jimple.v().newLocal("listIterator", RefType.v("java.util.ListIterator"));
                    Value iterable = null;// --> java.util.List
                    Local list = Jimple.v().newLocal("list", RefType.v("java.util.List"));
                    body.getLocals().add(list);
                    body.getLocals().add(listIterator);
                    for (int i = 0; i < units.size(); i++) {
                        Unit u = units.get(i);
                        if (u instanceof AssignStmt) {
                            Value lop = ((AssignStmt) u).getLeftOp();
                            InvokeExpr invokeExpr = UnitHelper.GetInvokeExpr(u);
                            if (invokeExpr != null && invokeExpr.getMethod().getSignature().equals(
                                    "<kotlin.collections.ArraysKt___ArraysKt: java.lang.Iterable withIndex(int[])>")) {
                                // r3 = kotlin.collections.ArraysKt___ArraysKt.withIndex(r1) --> list =
                                // kotlin.collections.ArraysKt___ArraysKt.withIndex(r1)
                                iterable = lop;
                                for (int j = i + 1; j < units.size(); j++) {
                                    Unit uj = units.get(j);
                                    if (uj instanceof AssignStmt
                                            && ((AssignStmt) uj).getRightOp() instanceof InstanceInvokeExpr
                                            && ((InstanceInvokeExpr) ((AssignStmt) uj).getRightOp()).getMethod()
                                            .getName().equals("iterator")
                                            && ((InstanceInvokeExpr) ((AssignStmt) uj).getRightOp()).getBase()
                                            .equals(lop)) {
                                        // r4 = r3.iterator() --> listIterator = list.listIterator()
                                        ((InstanceInvokeExpr) ((AssignStmt) uj).getRightOp()).setBase(list);
                                        ((InstanceInvokeExpr) ((AssignStmt) uj).getRightOp()).setMethodRef(Scene.v()
                                                .getMethod("<java.util.List: java.util.ListIterator listIterator()>")
                                                .makeRef());
                                        iterator = ((AssignStmt) uj).getLeftOp();
                                        ((AssignStmt) uj).setLeftOp(listIterator);
                                    }
                                }
                                ((AssignStmt) u).setLeftOp(list);
                            } else if (lop.getType().equals(RefType.v("kotlin.collections.IndexedValue"))) {
                                // r6 = (kotlin.collections.IndexedValue) --> delete
                                unitsChain.remove(u);
                            } else if (invokeExpr != null && invokeExpr.getMethod().getDeclaringClass().getName()
                                    .equals("kotlin.collections.IndexedValue")) {
                                if (invokeExpr.getMethod().getName().equals("component2")) {
                                    // r5 = r6.component2() --> delete
                                    unitsChain.remove(u);
                                } else if (invokeExpr.getMethod().getName().equals("component1")) {
                                    // i1 = r6.component1() --> i1 = li.nextIndex()
                                    if (iterator != null) {
                                        InvokeExpr newInvoke = Jimple.v().newInterfaceInvokeExpr((Local) listIterator,
                                                Scene.v().getMethod("<java.util.ListIterator: int nextIndex()>")
                                                        .makeRef());
                                        ((AssignStmt) u).setRightOp(newInvoke);
                                    }
                                }
                            } else if (invokeExpr != null && invokeExpr instanceof InterfaceInvokeExpr
                                    && ((InterfaceInvokeExpr) invokeExpr).getBase().equals(iterator)) {
                                // $z0 = interfaceinvoke $r4.<java.util.Iterator: boolean hasNext()>() --> $z0 =
                                // interfaceinvoke listIterator.<java.util.Iterator: boolean hasNext()>()
                                // $r5 = interfaceinvoke $r4.<java.util.Iterator: java.lang.Object next()>() -->
                                // $r5 = interfaceinvoke listIterator.<java.util.Iterator: java.lang.Object
                                // next()>()
                                ((InterfaceInvokeExpr) invokeExpr).setBase(listIterator);
                            }
                        }
                    }

                    if(verbose){
                        PrintAdapterLog("=============new===========");
                        PrintAdapterLog(sootMethod.getSignature());
                        for (Unit u : sootMethod.retrieveActiveBody().getUnits()) {
                            PrintAdapterLog("  " + u.toString());
                        }
                        PrintAdapterLog("=============new===========");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

        PrintAdapterLog("Finish");
    }
}
