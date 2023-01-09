package hunter.Preprocess.Adapters;

import hunter.Constant;
import hunter.Utils.ListOperator;
import hunter.Utils.MethodHelper;
import hunter.Utils.UnitHelper;
import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;

import java.util.ArrayList;

public class SuperClassMethodInvokeAdapter extends Adapter {

    public SuperClassMethodInvokeAdapter(boolean verbose){
        super(verbose);
        this.adapterName = "SuperClassMethodInvokeAdapter";
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


                Body body = sootMethod.retrieveActiveBody();
                PatchingChain<Unit> unitsChain = body.getUnits();
                ArrayList<Unit> units = new ArrayList<>(body.getUnits());
                for (int i = 0; i < units.size(); i++) {
                    Unit u = units.get(i);
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

                            if(verbose) {
                                PrintAdapterLog("before -> " + u.toString() + "  " + u.hashCode());
                            }

                            invokeExpr.setMethodRef(realMethod.makeRef());

                            if(verbose){
                                 PrintAdapterLog("signature -> " + fakeMethod.getSignature());
                                 PrintAdapterLog("subsignature -> " + fakeMethod.getSubSignature());
                                 PrintAdapterLog("fakeclass -> " + fakeClass);
                                 PrintAdapterLog("after -> " + u.toString() + "  " + u.hashCode());
                            }
                        }
                    }
                }

                if(verbose){
                     PrintAdapterLog("=============new===========");
                     PrintAdapterLog(sootMethod.getSignature());
                     for (Unit u : sootMethod.retrieveActiveBody().getUnits()) {
                         PrintAdapterLog("_superClassMethodInvokeAdapter " + u.toString());
                     }
                     PrintAdapterLog("=============new===========");
                }
            }
        }

        PrintAdapterLog("Finish");
    }
}
