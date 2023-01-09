package hunter.Track;

import heros.solver.Pair;
import hunter.Utils.BranchHelper;
import hunter.Utils.MethodHelper;
import hunter.Utils.Tools;
import hunter.Utils.ValueHelper;
import soot.*;
import soot.jimple.BinopExpr;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.SwitchStmt;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.internal.JIfStmt;
import soot.toolkits.graph.BriefUnitGraph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class LoopHelper {
    private ConcurrentHashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> unitOrder;
    private boolean verbose = false;

    public HashSet<SootMethod> methodHasTypeB;
    public HashMap<SootMethod, ArrayList<Pair<Pair<Unit, Integer>, Pair<Unit, Integer>>>> loopRecord;

    public LoopHelper(ConcurrentHashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> unitOrder, boolean verbose) {
        this.unitOrder = unitOrder;
        this.verbose = verbose;

        methodHasTypeB = new HashSet<>();
        loopRecord = new HashMap<>();
    }

    public void maintain() {
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || MethodHelper.isSkippedMethod(sootMethod)) {
                    continue;
                }


                ArrayList<Unit> visitedGoto = new ArrayList<>();
                ArrayList<Unit> units = new ArrayList<>(sootMethod.retrieveActiveBody().getUnits());
                for (int i = 0; i < units.size(); i++) {
                    Pair<Pair<Unit, Integer>, Pair<Unit, Integer>> loopElement = new Pair<>();
                    boolean foundLoop = false;
                    if (units.get(i) instanceof IfStmt) {
                        IfStmt ifUnit = (IfStmt) units.get(i);
                        Unit ifTgtUnit = ifUnit.getTarget();
                        int j = units.indexOf(ifTgtUnit);
                        for (int m = i; m < j; m++) {
                            if (units.get(m) instanceof GotoStmt) {
                                GotoStmt gotoUnit = (GotoStmt) units.get(m);
//                                BriefUnitGraph method_cfg = new BriefUnitGraph(sootMethod.retrieveActiveBody());

//                                HashSet<Unit> unitsBeforeIf = new HashSet<>();

//                                _getUnitsBeforeIfUnit(unitsBeforeIf, gotoUnit.getTarget(), ifUnit, ifUnit, method_cfg);
                                // if (ifUnit.toString().equals("if $z0 != 0 goto $r5 = interfaceinvoke $r16.<java.util.List: java.util.Iterator iterator()>()")) {
                                //     boolean b1 = Utils.hasPath(ifUnit, ifTgtUnit, sootMethod, unitOrder);
                                //     boolean b2 = !Utils.hasPath(gotoUnit, gotoUnit.getTarget(), sootMethod, unitOrder);
                                //     boolean b3 = Utils.hasPath(gotoUnit.getTarget(), ifUnit, sootMethod, unitOrder);
                                //     boolean b4 = !visitedGoto.contains(gotoUnit);
                                //     System.out.println();
                                // }


                                if (Tools.hasPath(ifUnit, ifTgtUnit, sootMethod, unitOrder) && !Tools.hasPath(gotoUnit, gotoUnit.getTarget(), sootMethod, unitOrder) && Tools.hasPath(gotoUnit.getTarget(), ifUnit, sootMethod, unitOrder) && !visitedGoto.contains(gotoUnit)) {
                                    loopElement.setPair(new Pair<>(ifUnit, i), new Pair<>(gotoUnit, m));
                                    visitedGoto.add(gotoUnit);
                                    foundLoop = true;
                                }
//                                if (unitsBeforeIf.contains(gotoUnit.getTarget())
//                                        && _isForwardBeforeBackward(sootMethod, units, ifUnit, gotoUnit, method_cfg)
//                                        && !_crossVisitedGoto(i, j, visitedGoto)) {
//                                    loopElement.setPair(new Pair<>(units.get(i), i), new Pair<>(units.get(m), m));
//                                    visitedGoto.add(new Pair<>(units.get(m), m));
//                                    foundLoop = true;
//                                }
                            }
                        }

                        // if (units.get(j - 1) instanceof GotoStmt) { //The unit before ifTgt must be
                        // GOTO
                        // Unit unitOutofLoop = ((GotoStmt) units.get(j - 1)).getTarget();
                        // for (int k = 0; k <= i; k++) {
                        // if (units.get(k).equals(unitOutofLoop)) {
                        // foundLoop = true;
                        // System.out.println(String.format("SootMethod -> %s, If Stmt -> %s",
                        // sootMethod.getSignature(), units.get(i).toString()));
                        // }
                        // }
                        // }

                    }
                    if (foundLoop) {
                        if (!loopRecord.containsKey(sootMethod))
                            loopRecord.put(sootMethod, new ArrayList<>(Arrays.asList(loopElement)));
                        loopRecord.get(sootMethod).add(loopElement);
                    }
                }
            }
        }
    }

    public void print() {
        for (SootMethod loopKey : loopRecord.keySet()) {
            System.out.println("loop key: " + loopKey.getSignature());
            for (Pair<Pair<Unit, Integer>, Pair<Unit, Integer>> ifGotoPair : loopRecord.get(loopKey)) {
                System.out.println("->");
                System.out.println(
                        "ifStmt : " + ifGotoPair.getO1().getO1() + " : " + ifGotoPair.getO1().getO1().hashCode());
                System.out.println("paired Goto Stmt : " + ifGotoPair.getO2().getO1() + " : "
                        + ifGotoPair.getO2().getO1().hashCode());
                System.out.println("========");
            }
        }
    }

    public void typeBAdapter() {
        // IN MEMORY OF [com.aiworks.android.moji_40104.apk]'s <c.c: int hashCode()>
        System.out.println("into _loopTypeBAdapter");
        int cnt = 0;
        int cntall = 0;
        HashMap<String, Integer> typeCnt = new HashMap<String, Integer>();

        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                cntall++;
                if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || MethodHelper.isSkippedMethod(sootMethod)) {
                    continue;
                }
                cnt++;

                BriefUnitGraph briefUnitGraph = new BriefUnitGraph(sootMethod.retrieveActiveBody());
                PatchingChain<Unit> unitsChain = sootMethod.retrieveActiveBody().getUnits();
                ArrayList<Unit> units = new ArrayList<>(sootMethod.retrieveActiveBody().getUnits());
                for (int i = 0; i < units.size(); i++) {
                    Unit u = units.get(i);
                    if (u instanceof IfStmt) {
                        Unit branchTrue = BranchHelper._getIfTrue((IfStmt) u);
                        Unit branchFalse = BranchHelper._getIfFalse((IfStmt) u, briefUnitGraph);

//                        System.out.println("find: "+sootMethod.getSignature()+" FROM: "+u.toString()+" TO: "+branchTrue.toString());

                        Value cond = ((IfStmt) u).getCondition();
                        String symb = "unknown";
                        if (cond instanceof BinopExpr) {
                            symb = ((BinopExpr) cond).getSymbol();
                        }
                        int c = 1;
                        if (typeCnt.containsKey(symb)) {
                            c += typeCnt.get(symb);
                        }
                        typeCnt.put(symb, c);

                        if (!Tools.hasPath(u, branchTrue, sootMethod, unitOrder)) {
//                            System.out.println("_loopTypeBAdapter: "+sootMethod.getSignature()+" FROM: "+u.toString()+" TO: "+branchTrue.toString());

                            // System.out.println("loop type b if true: " + branchTrue.toString());
                            // System.out.println("loop type b if: " + u.toString());
                            List<Unit> l = briefUnitGraph.getSuccsOf(u);
                            // System.out.println("l " + l.toString());
                            int s = l.size();
                            // System.out.println(s);
                            Unit u0 = l.get(0);
                            // System.out.println("u0 " + u0.toString());
                            Unit u1 = l.get(briefUnitGraph.getSuccsOf(u).size() - 1);
                            // System.out.println("u1 " + u1.toString());


                            IfStmt ifUnit = new JIfStmt(ValueHelper.getNegCondition(((IfStmt) u).getCondition()), branchFalse);
                            GotoStmt gotoUnit = new JGotoStmt(branchTrue);

                            unitsChain.insertAfter(gotoUnit, u);
                            unitsChain.insertAfter(ifUnit, u);
                            unitsChain.remove(u);

                            System.out.println("_loopBAdapter if: " + u.toString());
                            System.out.println("          new if: " + ifUnit.toString());
                            System.out.println("        new goto: " + gotoUnit.toString());

                            methodHasTypeB.add(sootMethod);
                        }
                    }

                    // && units.indexOf(((IfStmt) u).getTarget()) < i
                    //                            && _isForwardBeforeBackward(sootMethod, units, i)
//                    if (u instanceof IfStmt) {
//                        Unit branchTrue = Utils._getIfTrue((IfStmt) u);
//                        Unit branchFalse = Utils._getIfFalse((IfStmt) u, briefUnitGraph);
//                        ArrayList<Unit> unitsFromTrue2False = new ArrayList<>();
//                        Utils._findAllUnitsFromA2B(branchTrue, branchFalse, briefUnitGraph, unitsFromTrue2False);
//
//                        boolean isTypeB = false;
//                        Unit now = branchTrue;
//                        HashSet<Unit> visited = new HashSet<>();
//                        while (now != null && !visited.contains(now)) {
//                            visited.add(now);
//                            if (briefUnitGraph.getTails().contains(now) || now.equals(branchFalse)) {
//                                break;
//                            }
//
//                            if (u.equals(now)) {
//                                isTypeB = true;
//                                break;
//                            }
//
//                            if (now instanceof IfStmt) {
//                                now = Utils._getIfFalse((IfStmt) now, briefUnitGraph);
//                            } else if (now instanceof SwitchStmt) {
//                                // TODO
//                                now = ((SwitchStmt) now).getDefaultTarget();
//                            } else {
//                                now = briefUnitGraph.getSuccsOf(now) == null ? null : briefUnitGraph.getSuccsOf(now).get(0);
//                            }
//                        }
//
//
//                        if (isTypeB) {
//                            // System.out.println("loop type b if true: " + branchTrue.toString());
//                            // System.out.println("loop type b if: " + u.toString());
//                            List<Unit> l = briefUnitGraph.getSuccsOf(u);
//                            // System.out.println("l " + l.toString());
//                            int s = l.size();
//                            // System.out.println(s);
//                            Unit u0 = l.get(0);
//                            // System.out.println("u0 " + u0.toString());
//                            Unit u1 = l.get(briefUnitGraph.getSuccsOf(u).size() - 1);
//                            // System.out.println("u1 " + u1.toString());
//
//                            IfStmt ifUnit = new JIfStmt(((IfStmt) u).getCondition(), branchFalse);
//                            GotoStmt gotoUnit = new JGotoStmt(branchTrue);
//
//                            unitsChain.insertAfter(gotoUnit, u);
//                            unitsChain.insertAfter(ifUnit, u);
//                            unitsChain.remove(u);
//
//                            System.out.println("_loopAdapter if: " + u.toString());
//                            System.out.println("         new if: " + ifUnit.toString());
//                            System.out.println("       new goto: " + gotoUnit.toString());
//                        }
//
////                        if (isTypeB) {
//////                            boolean isTypeB = true;
////                            for (Unit uft2f : unitsFromTrue2False) {
////                                if (uft2f instanceof IfStmt && !uft2f.equals(u)) {
////                                    if (!unitsFromTrue2False.contains(Utils._getIfTrue((IfStmt) uft2f))) {
////                                        isTypeB = false;
////                                        break;
////                                    }
////                                    if (!unitsFromTrue2False.contains(Utils._getIfFalse((IfStmt) uft2f, briefUnitGraph))) {
////                                        isTypeB = false;
////                                        break;
////                                    }
////                                }
////                            }
////                        }
//                    }
                }
            }
        }

        System.out.println(typeCnt);

        // System.out.println("all method cnt "+cntall);
        // System.out.println("useful method cnt "+cnt);
    }

    // A.K.A Type_C
    public void pigtailLoopAdapter() {
        // IN MEMORY OF [com.peace.SilentCamera.apk]'s <a.a.b.c: void a(java.util.List,android.util.Size,int,int)>
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            boolean skip = false;
            for (String prefix : hunter.Constant.specialMethods2Skip) {
                if (sootClass.getName().startsWith(prefix)) {
                    skip = true;
                }
            }
            if (skip) {
                continue;
            }
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || MethodHelper.isSkippedMethod(sootMethod)) {
                    continue;
                }
                // BriefUnitGraph briefUnitGraph = method2Graph.get(sootMethod);
                BriefUnitGraph briefUnitGraph = new BriefUnitGraph(sootMethod.retrieveActiveBody());
                PatchingChain<Unit> unitsChain = sootMethod.retrieveActiveBody().getUnits();
                ArrayList<Unit> units = new ArrayList<>(sootMethod.retrieveActiveBody().getUnits());
                for (int i = 0; i < units.size(); i++) {
                    Unit u = units.get(i);
                    if (u instanceof IfStmt && units.indexOf(((IfStmt) u).getTarget()) > i) {
                        IfStmt ifUnit = (IfStmt) u;
                        ArrayList<Unit> unitsBeforeIf = MethodHelper.getAllUnitsBefore(ifUnit, briefUnitGraph);
                        Unit branchTrue = ifUnit.getTarget();
                        Unit branchFalse = briefUnitGraph.getSuccsOf(ifUnit).get(0).equals(branchTrue)
                                ? briefUnitGraph.getSuccsOf(ifUnit).get(briefUnitGraph.getSuccsOf(ifUnit).size() - 1)
                                : briefUnitGraph.getSuccsOf(ifUnit).get(0);
                        IfStmt pigTailIfUnit = null;
                        GotoStmt pigTailGotoUnit = null;

                        ArrayList<Unit> visited = new ArrayList<>();
                        Unit now = branchFalse;
                        boolean isTail = false;
                        while (!isTail && now != null) {
                            if (visited.contains(now)) {
                                break;
                            }
                            visited.add(now);
                            for (Unit tail : briefUnitGraph.getTails()) {
                                if (now.equals(tail)) {
                                    isTail = true;
                                    break;
                                }
                            }
                            if (isTail || now.equals(ifUnit.getTarget())) {
                                // IN MEMORY OF [com.urbandroid.sleep_2019-07-19.apk]'s <>
                                break;
                            }
                            if (now instanceof IfStmt) {
                                Unit nowBranchTrue = ((IfStmt) now).getTarget();
                                Unit nowBranchFalse = nowBranchTrue;
                                if (briefUnitGraph.getSuccsOf(now).size() > 1) {
                                    nowBranchFalse = briefUnitGraph.getSuccsOf(now).get(0).equals(nowBranchTrue)
                                            ? briefUnitGraph.getSuccsOf(now).get(1)
                                            : briefUnitGraph.getSuccsOf(now).get(0);
                                }
                                if (unitsBeforeIf.contains(nowBranchTrue)) {
                                    pigTailIfUnit = (IfStmt) now;
                                }
                                now = nowBranchFalse;
                            } else if (now instanceof SwitchStmt) {
                                // TODO
                            } else if (now instanceof GotoStmt) {
                                if (visited.contains(((GotoStmt) now).getTarget())) {
                                    pigTailGotoUnit = (GotoStmt) now;
                                    break;
                                }
                            } else {
                                now = briefUnitGraph.getSuccsOf(now) == null ? null
                                        : briefUnitGraph.getSuccsOf(now).get(0);
                            }
                        }

                        if (pigTailIfUnit != null && pigTailGotoUnit != null) {
                            i = units.indexOf(ifUnit.getTarget());

                            GotoStmt newGotoUnit = new JGotoStmt(pigTailIfUnit.getTarget());
                            IfStmt newIfUnit = new JIfStmt(pigTailIfUnit.getCondition(), newGotoUnit);

                            // System.out.println(sootMethod.getSignature());
                            // System.out.println("     pigtail while if: " + u.toString());
                            // System.out.println("pigtailLoopAdapter if: " + pigTailIfUnit.toString());
                            // System.out.println("               new if: " + newIfUnit.toString());
                            // System.out.println("             new goto: " + newGotoUnit.toString());

                            unitsChain.insertAfter(newIfUnit, pigTailIfUnit);
                            unitsChain.insertAfter(newGotoUnit, pigTailGotoUnit);
                            unitsChain.remove(pigTailIfUnit);
                        }
                    }
                }
            }
        }
    }

    private boolean _isForwardBeforeBackward(SootMethod sootMethod, ArrayList<Unit> units, IfStmt ifUnit, GotoStmt gotoUnit, BriefUnitGraph briefUnitGraph) {
        boolean res = true;
        int forwardIdx = units.indexOf(ifUnit.getTarget());
        int ifIdx = units.indexOf(ifUnit);
        for (Unit backwardUnit : briefUnitGraph.getPredsOf(ifUnit)) {
            int backwardIdx = units.indexOf(backwardUnit);
            if ((backwardIdx - ifIdx) * (forwardIdx - ifIdx) > 0 && !backwardUnit.equals(gotoUnit)) {
                res = false;
                break;
            }
        }
        return res;
    }

    // pred unit is before success unit
    private boolean _isForwardBeforeBackward(SootMethod sootMethod, ArrayList<Unit> units, int ifIdx) {
        BriefUnitGraph briefUnitGraph = new BriefUnitGraph(sootMethod.retrieveActiveBody());
        Unit ifStmt = units.get(ifIdx);
        boolean res = true;
        int targetIdx = units.indexOf(((IfStmt) ifStmt).getTarget());
        for (Unit backwardUnit : briefUnitGraph.getPredsOf(ifStmt)) {
            int predIdx = units.indexOf(backwardUnit);
            if ((predIdx - ifIdx) * (targetIdx - ifIdx) < 0) {
                // IN MEMORY OF [com.aiworks.android.moji_40104.apk]'s <c.c: int hashCode()>
                res = false;
                break;
            }
        }
        return res;
    }

    private boolean _crossVisitedGoto(int ifIdx, int tgtIdx, ArrayList<Pair<Unit, Integer>> visitedGoto) {
        for (Pair<Unit, Integer> gotoPair : visitedGoto) {
            if (ifIdx < gotoPair.getO2() && tgtIdx > gotoPair.getO2())
                return true;
        }
        return false;
    }

    private void _getUnitsBeforeIfUnit(HashSet<Unit> visitedSet, Unit gotoTargetUnit, Unit now, IfStmt ifUnit, BriefUnitGraph briefUnitGraph) {
        // for (Unit unit : briefUnitGraph.getPredsOf(now)) {
        // if (visitedSet.contains(unit) || (now instanceof IfStmt &&
        // unit.equals(((IfStmt) now).getTarget()))) return;
        // if(unit instanceof GotoStmt){
        // continue;
        // }else if(unit instanceof IfStmt){
        // if(visitedSet.contains(((IfStmt) unit).getTarget())){
        // continue;
        // }
        // }
        // visitedSet.add(unit);
        // if (unit.equals(gotoTargetUnit)) {
        // return;
        // }
        // _getUnitsBeforeIfUnit(visitedSet, gotoTargetUnit, unit, ifUnit,
        // briefUnitGraph);
        // }
        while (now != null) {
//            System.out.println("_getUnitsBeforeIfUnit : " + now.toString());

            if (briefUnitGraph.getPredsOf(now).size() > 1 && briefUnitGraph.getPredsOf(now).get(0) instanceof IfStmt
                    && !visitedSet.contains(now)) {
                visitedSet.add(now);
                now = briefUnitGraph.getPredsOf(now).get(1);
            } else if (briefUnitGraph.getPredsOf(now).size() > 0 && !visitedSet.contains(now)) {
                visitedSet.add(now);
                now = briefUnitGraph.getPredsOf(now).get(0);
            } else {
                visitedSet.add(now);
                break;
            }
        }
    }
}
