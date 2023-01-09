package hunter.Preprocess.Adapters;

import hunter.Constant;
import hunter.Utils.ListOperator;
import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.ReturnStmt;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.ArrayList;
import java.util.Iterator;

public class AsyncTaskAdapter extends Adapter {
    public AsyncTaskAdapter(boolean verbose) {
        super(verbose);
        this.adapterName = "AsyncTaskAdapter";
    }

    public void go() {
        PrintAdapterLog("Start");
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (ListOperator._ListFuzzyContains(sootClass.getName(), Constant.specialMethods2Skip)) {
                continue;
            }
            if (sootClass.getSuperclass().equals(Scene.v().getSootClass("android.os.AsyncTask"))) {
                SootMethod onPostExecute = sootClass.getMethodUnsafe("void onPostExecute(java.lang.Object)");
                SootMethod doInBackground = sootClass
                        .getMethodUnsafe("java.lang.Object doInBackground(java.lang.Object[])");
                if (onPostExecute == null || doInBackground == null) {
                    continue;
                }
                if (onPostExecute.isConcrete() && onPostExecute.hasActiveBody() && doInBackground.isConcrete() && doInBackground.hasActiveBody()) {
                    Body body = doInBackground.retrieveActiveBody();
                    PatchingChain<Unit> unitsChain = body.getUnits();
                    ArrayList<Unit> units = new ArrayList<>(body.getUnits());
                    Local thisRef = body.getThisLocal();
                    for (int i = 0; i < units.size(); i++) {
                        Unit u = units.get(i);
                        if (u instanceof ReturnStmt) {
                            Value retValue = ((ReturnStmt) u).getOp();
                            Unit newInvoke = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(thisRef, onPostExecute.makeRef(), retValue));
                            unitsChain.insertBefore(newInvoke, u);
                        }
                    }

                    if (verbose) {
                        PrintAdapterLog("new doInBackground");
                        for (Unit u : doInBackground.retrieveActiveBody().getUnits()) {
                            PrintAdapterLog("  " + u.toString());
                        }
                        PrintAdapterLog("new doInBackground end");
                    }
                }

                SootMethod execute = Scene.v()
                        .getMethod("<android.os.AsyncTask: android.os.AsyncTask execute(java.lang.Object[])>");
                if (execute == null) {
                    continue;
                }
                String realExecute = "<" + sootClass.getName() + ": android.os.AsyncTask execute(java.lang.Object[])>";
//                if (execute == null) {
//                    PrintAdapterLog(" -> execute is null");
//                } else {
//                    PrintAdapterLog(" -> " + execute.getSignature());
//                }

                for (Iterator<Edge> it = Scene.v().getCallGraph().edgesInto(execute); it.hasNext(); ) {
                    Edge e = it.next();
                    SootMethod caller = (SootMethod) e.getSrc();
                    if (caller.isConcrete() && caller.hasActiveBody()) {
                        Body body = caller.retrieveActiveBody();
                        PatchingChain<Unit> unitsChain = body.getUnits();
                        ArrayList<Unit> units = new ArrayList<>(body.getUnits());
                        for (int i = 0; i < units.size(); i++) {
                            Unit u = units.get(i);
                            if (u instanceof InvokeStmt && u.toString().contains(realExecute) && ((InvokeStmt) u).getInvokeExpr() instanceof InstanceInvokeExpr) {
                                Value base = ((InstanceInvokeExpr) ((InvokeStmt) u).getInvokeExpr()).getBase();
                                Value arg0 = ((InvokeStmt) u).getInvokeExpr().getArg(0);
                                Unit newInvoke = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr((Local) base, doInBackground.makeRef(), arg0));
                                unitsChain.insertAfter(newInvoke, u);
                                unitsChain.remove(u);
                            }
                        }

                        if(verbose){
                            PrintAdapterLog("new caller");
                            PrintAdapterLog(caller.getSignature());
                            for (Unit u : caller.retrieveActiveBody().getUnits()) {
                                PrintAdapterLog("  " + u.toString());
                            }
                            PrintAdapterLog("new caller end");
                        }
                    }
                }
            }
        }
        PrintAdapterLog("Finish");
    }
}
