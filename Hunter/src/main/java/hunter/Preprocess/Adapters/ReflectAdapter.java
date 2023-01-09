package hunter.Preprocess.Adapters;

import hunter.Constant;
import hunter.Utils.ListOperator;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JNewArrayExpr;

import java.util.ArrayList;
import java.util.HashMap;

public class ReflectAdapter extends Adapter {
    public ReflectAdapter(boolean verbose){
        super(verbose);
        this.adapterName = "ReflectAdapter";
    }
    public void go() {
        PrintAdapterLog("Start");

        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (ListOperator._ListFuzzyContains(sootClass.getName(), Constant.specialMethods2Skip)) {
                continue;
            }

            for (int si = 0; si < sootClass.getMethods().size(); ++si) {
                SootMethod sootMethod = sootClass.getMethods().get(si);

                if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody()) {
                    continue;
                }

                if(verbose){
                    PrintAdapterLog("=============old==============");
                    int ii = 0;
                    for (SootMethod ssss : sootClass.getMethods()) {
                        PrintAdapterLog(ii + " reflect " + ssss.getSignature());
                        ii++;
                    }
                    PrintAdapterLog("=============old==============");

                    PrintAdapterLog("=============old==============");
                    for (Unit u : sootMethod.retrieveActiveBody().getUnits()) {
                        PrintAdapterLog("reflect " + u.toString());
                    }
                    PrintAdapterLog("=============old==============");
                }


                // BriefUnitGraph briefUnitGraph = method2Graph.get(sootMethod);
                // BriefUnitGraph briefUnitGraph = new
                // BriefUnitGraph(sootMethod.retrieveActiveBody());
                Body body = sootMethod.retrieveActiveBody();
                PatchingChain<Unit> unitsChain = body.getUnits();
                ArrayList<Unit> units = new ArrayList<>(body.getUnits());
                for (int i = 0; i < units.size(); i++) {
                    if(verbose){
                        PrintAdapterLog(i + " class method num: " + sootClass.getMethods().size());
                        PrintAdapterLog(units.get(i).toString());
                    }

                    Unit u = units.get(i);
                    int processType = 0;
                    if (u instanceof AssignStmt && ((AssignStmt) u).getRightOp() instanceof InvokeExpr) {
                        SootMethod callee = ((InvokeExpr) ((AssignStmt) u).getRightOp()).getMethod();
                        if (callee.getSignature().equals("<java.lang.reflect.Constructor: java.lang.Object newInstance(java.lang.Object[])>")) {
                            processType = 1;
                        } else if (callee.getSignature().equals("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>")) {
                            processType = 2;
                        }
                    } else if (u instanceof InvokeStmt) {
                        SootMethod callee = ((InvokeStmt) u).getInvokeExpr().getMethod();
                        if (callee.getSignature().equals("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>")) {
                            processType = 3;
                        }
                    }

                    if (processType == 1) {
                        // <java.lang.reflect.Constructor: java.lang.Object
                        // newInstance(java.lang.Object[])>
                        Value lop = ((AssignStmt) u).getLeftOp();
                        SootMethod callee = ((InvokeExpr) ((AssignStmt) u).getRightOp()).getMethod();

                        // newInstance: lop -> obj; base -> constructor; arg0 -> param_list
                        AssignStmt newInstanceUnit = (AssignStmt) u;
                        Local obj = (Local) lop;
                        ArrayList<AssignStmt> paramUnits = new ArrayList<>();
                        InstanceInvokeExpr invokeExpr = (InstanceInvokeExpr) ((AssignStmt) u).getInvokeExpr();
                        Value initParamList = invokeExpr.getArg(0);
                        Value rConstructor = invokeExpr.getBase();

                        // getConstructor: lop -> constructor; base -> class; arg0 -> type_list
                        AssignStmt getConstructorUnit = null;
                        ArrayList<AssignStmt> typeUnits = new ArrayList<>();
                        Value rClass = null;
                        Value initTypeList = null;

                        // forName: lop -> class; arg0 -> class_name
                        AssignStmt forNameUnit = null;
                        String className = "";

                        boolean collectArgsType = false;
                        boolean collectArgs = true;

                        for (int j = i - 1; j >= 0; j--) {
                            Unit uj = units.get(j);
                            if (uj instanceof AssignStmt) {
                                if (((AssignStmt) uj).getRightOp() instanceof InvokeExpr) {
                                    SootMethod calleeuj = ((AssignStmt) uj).getInvokeExpr().getMethod();
                                    if (getConstructorUnit == null && calleeuj.getSignature().equals("<java.lang.Class: java.lang.reflect.Constructor getConstructor(java.lang.Class[])>")) {
                                        if (((AssignStmt) uj).getLeftOp().equals(rConstructor)) {
                                            getConstructorUnit = (AssignStmt) uj;
                                            rClass = ((InstanceInvokeExpr) getConstructorUnit.getInvokeExpr()).getBase();
                                            initTypeList = ((InvokeExpr) getConstructorUnit.getRightOp()).getArg(0);
                                            collectArgsType = true;
                                        }
                                    } else if (forNameUnit == null && calleeuj.getSignature().equals("<java.lang.Class: java.lang.Class forName(java.lang.String)>") && rClass != null) {
                                        if (((AssignStmt) uj).getLeftOp().equals(rClass) && ((InvokeExpr) ((AssignStmt) uj).getRightOp()).getArg(0) instanceof StringConstant) {
                                            forNameUnit = (AssignStmt) uj;
                                            className = ((StringConstant) forNameUnit.getInvokeExpr().getArg(0)).value;
                                        }
                                    }
                                } else if (collectArgs && ((AssignStmt) uj).getLeftOp() instanceof ArrayRef && ((ArrayRef) ((AssignStmt) uj).getLeftOp()).getBase().equals(initParamList)) {
                                    paramUnits.add(0, (AssignStmt) uj);
                                } else if (collectArgsType && ((AssignStmt) uj).getLeftOp() instanceof ArrayRef && ((ArrayRef) ((AssignStmt) uj).getLeftOp()).getBase().equals(initTypeList)) {
                                    // typeUnits.add(0, (AssignStmt) uj);
                                    Value ropj = ((AssignStmt) uj).getRightOp();
                                    if (ropj instanceof ClassConstant) {
                                        typeUnits.add(0, (AssignStmt) uj);
                                    } else {
                                        for (int k = j - 1; k >= 0; k--) {
                                            Unit uk = units.get(k);
                                            if (uk instanceof AssignStmt && ((AssignStmt) uk).getLeftOp().equals(ropj)) {
                                                typeUnits.add(0, (AssignStmt) uk);
                                                break;
                                            }
                                        }
                                    }
                                } else if (((AssignStmt) uj).getLeftOp().equals(initParamList) && ((AssignStmt) uj).getRightOp() instanceof JNewArrayExpr) {
                                    collectArgs = false;
                                } else if (((AssignStmt) uj).getLeftOp().equals(initTypeList) && ((AssignStmt) uj).getRightOp() instanceof JNewArrayExpr) {
                                    collectArgsType = false;
                                }
                            }
                        }

                        if (getConstructorUnit != null && forNameUnit != null) {
                            ArrayList<String> argsType = new ArrayList<>();
                            ArrayList<Value> args = new ArrayList<>();

                            for (AssignStmt tu : typeUnits) {
                                Value rop = tu.getRightOp();
                                if (rop instanceof ClassConstant) {
                                    String type = ((ClassConstant) rop).getValue();
                                    type = type.substring(1, type.length() - 1).replaceAll("/", ".");
                                    argsType.add(type);
                                } else if (rop instanceof FieldRef) {
                                    argsType.add(_getPrimType(RefType.v(((FieldRef) rop).getField().getDeclaringClass())).toString());
                                }
                            }

                            for (AssignStmt pu : paramUnits) {
                                args.add(pu.getRightOp());
                            }

                            String paramTypeList = "";
                            for (int t = 0; t < argsType.size(); t++) {
                                paramTypeList += argsType.get(t);
                                if (t < argsType.size() - 1) {
                                    paramTypeList += ",";
                                }
                            }
                            // // com.class.name obj;
                            // Local obj = Jimple.v().newLocal("obj"+hash, RefType.v(className));
                            // body.getLocals().add(obj);

                            if (!ListOperator._ListFuzzyContains(className, Constant.specialMethods2Skip)) {
                                // obj.<init>()
                                if (Scene.v().containsMethod("<" + className + ": void <init>(" + paramTypeList + ")>")) {
                                    SootMethod init = Scene.v().getMethod("<" + className + ": void <init>(" + paramTypeList + ")>");
                                    Unit newInit = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(obj, init.makeRef(), args));
                                    // PrintAdapterLog("reflectionAdapter Init: " + newInit.toString());
                                    unitsChain.insertAfter(newInit, u);

                                    // obj = new com.class.name
                                    Unit newNew = Jimple.v().newAssignStmt(obj, Jimple.v().newNewExpr(RefType.v(className)));
                                    // PrintAdapterLog("reflectionAdapter New: " + newNew.toString());
                                    unitsChain.insertAfter(newNew, u);
                                }
                            }
                        }
                    } else if (processType == 2 || processType == 3) {
                        // <java.lang.reflect.Method: java.lang.Object
                        // invoke(java.lang.Object,java.lang.Object[])>

                        // invoke: lop -> return_value(optional); base -> method; arg0 -> obj; arg1 ->
                        // param_list
                        Value returnValue = null;
                        InstanceInvokeExpr invokeExpr = null;
                        if (processType == 2) {
                            returnValue = ((AssignStmt) u).getLeftOp();
                            invokeExpr = (InstanceInvokeExpr) ((AssignStmt) u).getRightOp();
                        } else if (processType == 3) {
                            invokeExpr = (InstanceInvokeExpr) ((InvokeStmt) u).getInvokeExpr();
                        }
                        Unit invokeUnit = u;
                        SootMethod callee = invokeExpr.getMethod();
                        Value rMethod = invokeExpr.getBase();
                        Value obj = invokeExpr.getArg(0);
                        Value invokeParamList = invokeExpr.getArg(1);
                        ArrayList<AssignStmt> paramUnits = new ArrayList<>();

                        // getMethod: lop -> method; base -> class; arg0 -> method_name; arg1 ->
                        // type_list
                        AssignStmt getMethodUnit = null;
                        Value rClass = null;
                        Value invokeTypeList = null;
                        ArrayList<AssignStmt> typeUnits = new ArrayList<>();
                        String methodName = "";

                        // forName: lop -> class; arg0 -> class_name
                        AssignStmt forNameUnit = null;
                        String className = "";

                        boolean collectArgsType = false;
                        boolean collectArgs = true;

                        for (int j = i - 1; j >= 0; j--) {
                            Unit uj = units.get(j);
                            if (uj instanceof AssignStmt) {
                                if (((AssignStmt) uj).getRightOp() instanceof InvokeExpr) {
                                    SootMethod calleeuj = ((AssignStmt) uj).getInvokeExpr().getMethod();
                                    if (getMethodUnit == null && calleeuj.getSignature().equals("<java.lang.Class: java.lang.reflect.Method getMethod(java.lang.String,java.lang.Class[])>")) {
                                        if (((AssignStmt) uj).getLeftOp().equals(rMethod) && ((InvokeExpr) ((AssignStmt) uj).getRightOp()).getArg(0) instanceof StringConstant) {
                                            getMethodUnit = (AssignStmt) uj;
                                            rClass = ((InstanceInvokeExpr) getMethodUnit.getInvokeExpr()).getBase();
                                            invokeTypeList = ((InvokeExpr) getMethodUnit.getRightOp()).getArg(1);
                                            methodName = ((StringConstant) ((InvokeExpr) getMethodUnit.getRightOp()).getArg(0)).value;
                                            collectArgsType = true;
                                        }
                                    } else if (forNameUnit == null && calleeuj.getSignature().equals("<java.lang.Class: java.lang.Class forName(java.lang.String)>") && rClass != null) {
                                        if (((AssignStmt) uj).getLeftOp().equals(rClass) && ((InvokeExpr) ((AssignStmt) uj).getRightOp()).getArg(0) instanceof StringConstant) {
                                            forNameUnit = (AssignStmt) uj;
                                            className = ((StringConstant) forNameUnit.getInvokeExpr().getArg(0)).value;
                                        }
                                    }
                                } else if (collectArgs && ((AssignStmt) uj).getLeftOp() instanceof ArrayRef && ((ArrayRef) ((AssignStmt) uj).getLeftOp()).getBase().equals(invokeParamList)) {
                                    paramUnits.add(0, (AssignStmt) uj);
                                } else if (collectArgsType && ((AssignStmt) uj).getLeftOp() instanceof ArrayRef && ((ArrayRef) ((AssignStmt) uj).getLeftOp()).getBase().equals(invokeTypeList)) {
                                    Value ropj = ((AssignStmt) uj).getRightOp();
                                    if (ropj instanceof ClassConstant) {
                                        typeUnits.add(0, (AssignStmt) uj);
                                    } else {
                                        for (int k = j - 1; k >= 0; k--) {
                                            Unit uk = units.get(k);
                                            if (uk instanceof AssignStmt && ((AssignStmt) uk).getLeftOp().equals(ropj)) {
                                                typeUnits.add(0, (AssignStmt) uk);
                                                break;
                                            }
                                        }
                                    }
                                } else if (((AssignStmt) uj).getLeftOp().equals(invokeParamList) && ((AssignStmt) uj).getRightOp() instanceof JNewArrayExpr) {
                                    collectArgs = false;
                                } else if (((AssignStmt) uj).getLeftOp().equals(invokeTypeList) && ((AssignStmt) uj).getRightOp() instanceof JNewArrayExpr) {
                                    collectArgsType = false;
                                }
                            }
                        }

                        if (getMethodUnit != null && forNameUnit != null) {
                            ArrayList<Type> argsType = new ArrayList<>();
                            ArrayList<Value> args = new ArrayList<>();

                            for (AssignStmt tu : typeUnits) {
                                Value rop = tu.getRightOp();
                                if (rop instanceof ClassConstant) {
                                    String type = ((ClassConstant) rop).getValue();
                                    type = type.substring(1, type.length() - 1).replaceAll("/", ".");
                                    argsType.add(RefType.v(type));
                                } else if (rop instanceof FieldRef) {
                                    argsType.add(_getPrimType(RefType.v(((FieldRef) rop).getField().getDeclaringClass())));
                                }
                            }

                            for (AssignStmt pu : paramUnits) {
                                args.add(pu.getRightOp());
                            }

                            String paramTypeList = "";
                            for (int t = 0; t < argsType.size(); t++) {
                                paramTypeList += argsType.get(t);
                                if (t < argsType.size() - 1) {
                                    paramTypeList += ",";
                                }
                            }

                            // 2:
                            // return_value = obj.method()
                            // return_value = method()
                            // 3:
                            // obj.method()
                            // method()
                            SootMethod invoke = null;
                            Unit newInvoke = null;
                            if (!ListOperator._ListFuzzyContains(className, Constant.specialMethods2Skip)) {
                                invoke = Scene.v().getSootClass(className).getMethod(methodName, argsType);
                            }
                            if (invoke != null) {
                                if (processType == 2) {
                                    if (invoke.isStatic()) {
                                        newInvoke = Jimple.v().newAssignStmt(returnValue, Jimple.v().newStaticInvokeExpr(invoke.makeRef(), args));
                                    } else {
                                        newInvoke = Jimple.v().newAssignStmt(returnValue, Jimple.v().newVirtualInvokeExpr((Local) obj, invoke.makeRef(), args));
                                    }
                                } else if (processType == 3) {
                                    if (invoke.isStatic()) {
                                        newInvoke = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(invoke.makeRef(), args));
                                    } else {
                                        newInvoke = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr((Local) obj, invoke.makeRef(), args));
                                    }
                                }

                                if(verbose){
                                    PrintAdapterLog("  Invoke: " + newInvoke.toString());
                                }
                                unitsChain.insertAfter(newInvoke, u);
                            }
                        }

                    }

                    // unitsChain.remove(u);
                }

                if(verbose){
                    PrintAdapterLog("===========================");
                    for (Unit u : sootMethod.retrieveActiveBody().getUnits()) {
                        PrintAdapterLog("reflect " + u.toString());
                    }
                    PrintAdapterLog("===========================");

                    PrintAdapterLog("===========================");
                    int i = 0;
                    for (SootMethod ssss : sootClass.getMethods()) {
                        PrintAdapterLog(i + " reflect " + ssss.getSignature());
                        i++;
                    }
                    PrintAdapterLog("===========================");
                }

            }
        }

        PrintAdapterLog("Finish");
    }

    private Type _getPrimType(RefType t) {
        HashMap<RefType, Type> typeMap = new HashMap<RefType, Type>() {
            {
                put(RefType.v("java.lang.Boolean"), BooleanType.v());
                put(RefType.v("java.lang.Byte"), ByteType.v());
                put(RefType.v("java.lang.Character"), CharType.v());
                put(RefType.v("java.lang.Double"), DoubleType.v());
                put(RefType.v("java.lang.Float"), FloatType.v());
                put(RefType.v("java.lang.Integer"), IntType.v());
                put(RefType.v("java.lang.Long"), LongType.v());
                put(RefType.v("java.lang.Short"), ShortType.v());
            }
        };
        if (typeMap.containsKey(t)) {
            return typeMap.get(t);
        } else {
            return t;
        }
    }
}
