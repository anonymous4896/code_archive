package hunter.Track;

import hunter.Utils.BranchHelper;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.HashMutableEdgeLabelledDirectedGraph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MaintainUnitOrderTask implements Runnable {
    SootMethod sootMethod;
    ConcurrentHashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> method2UnitOrderResult;

    MaintainUnitOrderTask(SootMethod sootMethod, ConcurrentHashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> method2UnitOrderResult) {
        this.sootMethod = sootMethod;
        this.method2UnitOrderResult = method2UnitOrderResult;
    }

    @Override
    public void run() {

//        System.out.println(String.format("%s start", sootMethod.getSignature()));

        HashMutableEdgeLabelledDirectedGraph<Unit, String>  unitGraph = new HashMutableEdgeLabelledDirectedGraph<>();


        BriefUnitGraph briefUnitGraph = new BriefUnitGraph(sootMethod.retrieveActiveBody());

        HashSet<Unit> visitedUnits = new HashSet<>();

        for (Unit unit : briefUnitGraph.getHeads()) {
            if(unit instanceof IdentityStmt && ((IdentityStmt) unit).getRightOp() instanceof CaughtExceptionRef){
                continue;
            }
            dfsCollection(unit, briefUnitGraph, visitedUnits, unitGraph, sootMethod);
        }

        // if(sootMethod.getSignature().equals(debugMethod)){
        //     printGraph(unitGraph);
        // }

        if(method2UnitOrderResult.containsKey(sootMethod)) {
            method2UnitOrderResult.remove(sootMethod);
        }
        method2UnitOrderResult.put(sootMethod, buildEfficientPath(unitGraph, sootMethod));

    }

    public void printGraph(HashMutableEdgeLabelledDirectedGraph<Unit, String>  unitGraph) {
        Iterator var1 = unitGraph.iterator();

        while(var1.hasNext()) {
            Unit node = (Unit) var1.next();
            System.out.println(("Node = " + node));
            System.out.println("Preds:");
            Iterator var3 = unitGraph.getPredsOf(node).iterator();

            Object succ;
//            HashMutableEdgeLabelledDirectedGraph.DGEdge edge;
//            List labels;
            while(var3.hasNext()) {
                succ = var3.next();
//                edge = new HashMutableEdgeLabelledDirectedGraph.DGEdge(succ, node);

                System.out.println("     " + succ );
            }

            System.out.println("Succs:");
            var3 = unitGraph.getSuccsOf(node).iterator();

            while(var3.hasNext()) {
                succ = var3.next();
//                edge = new HashMutableEdgeLabelledDirectedGraph.DGEdge(node, succ);
//                labels = unitGraph.edgeToLabels.get(edge);
                System.out.println("     " + succ );
            }
        }

    }


    private static void dfsCollection(Unit currentUnit, BriefUnitGraph briefUnitGraph, HashSet<Unit> visitedUnits, HashMutableEdgeLabelledDirectedGraph<Unit, String> results, SootMethod sootMethod) {

        if (visitedUnits.contains(currentUnit)) return;

        HashSet<Unit> innerVisitedUnits = new HashSet<>();

        ArrayList<Unit> succses = new ArrayList<>();
        if(currentUnit instanceof IfStmt){
            succses.add(BranchHelper._getIfTrue((IfStmt) currentUnit));
            succses.add(BranchHelper._getIfFalse((IfStmt) currentUnit, briefUnitGraph));
        }else{
            succses.addAll(briefUnitGraph.getSuccsOf(currentUnit));
        }

        for (Unit nextUnit : succses) {

            HashSet<Unit> newVisitedUnits = new HashSet<>();
            newVisitedUnits.addAll(visitedUnits);
            newVisitedUnits.add(currentUnit);

            if (results.containsAnyEdge(currentUnit, nextUnit))
                return;

            if (!newVisitedUnits.contains(nextUnit) && !isThereAPath(nextUnit, currentUnit, briefUnitGraph, newVisitedUnits, innerVisitedUnits, sootMethod)) {
//                if (!isThereAPath(nextUnit, currentUnit, briefUnitGraph, newVisitedUnits, innerVisitedUnits)) {

                if(!results.containsNode(currentUnit)) results.addNode(currentUnit);
                if(!results.containsNode(nextUnit)) results.addNode(nextUnit);
                if(!results.containsAnyEdge(currentUnit, nextUnit)) {
                    results.addEdge(currentUnit, nextUnit, "" + currentUnit.hashCode() + nextUnit.hashCode());
                }

                dfsCollection(nextUnit, briefUnitGraph, newVisitedUnits, results, sootMethod);

            }
        }
    }


    private static boolean isThereAPath(Unit from, Unit to, BriefUnitGraph briefUnitGraph, HashSet<Unit> visitedUnits, HashSet<Unit> innerVisitedUnits, SootMethod sootMethod){
        boolean result = false;

        if(!visitedUnits.contains(from)) return false;

        for (Unit u : briefUnitGraph.getSuccsOf(from)) {
            if (visitedUnits.contains(u) && !innerVisitedUnits.contains(u)) {
                if (u.equals(to)) return true;
                else {
                    innerVisitedUnits.add(u);
                    result = result || isThereAPath(u, to, briefUnitGraph, visitedUnits, innerVisitedUnits, sootMethod);
                }
            }
        }
        return result;
    }


    private static HashMap<Unit, HashSet<Unit>> buildEfficientPath(HashMutableEdgeLabelledDirectedGraph<Unit, String> unitGraph, SootMethod sootMethod) {

        HashMap<Unit, HashSet<Unit>> unitOrder = new HashMap<>();

        int counter = 0;

        HashSet<Unit> visitedUnits = new HashSet<>();

        for(Unit uH : unitGraph.getHeads()){
            if(uH instanceof IdentityStmt && ((IdentityStmt) uH).getRightOp() instanceof CaughtExceptionRef){
                continue;
            }
            if(!unitOrder.containsKey(uH))  {
                unitOrder.put(uH, new HashSet<>());
            }
            if(!visitedUnits.contains(uH)){
                collectChildren(unitGraph, unitOrder, uH, sootMethod, counter, visitedUnits);
            }
        }


        return unitOrder;
    }

    private static void collectChildren(HashMutableEdgeLabelledDirectedGraph<Unit, String> unitGraph, HashMap<Unit, HashSet<Unit>> unitOrder, Unit uP, SootMethod sootMethod, int counter, HashSet<Unit> visitedUnits){

        if(!unitGraph.getSuccsOf(uP).isEmpty() && !unitOrder.containsKey(uP))  {
            unitOrder.put(uP, new HashSet<>());
        }

        for(Unit uC : unitGraph.getSuccsOf(uP)){

            unitOrder.get(uP).add(uC);
            if(!visitedUnits.contains(uC)){
                collectChildren(unitGraph, unitOrder, uC, sootMethod, counter+1, visitedUnits);
            }
            if(unitOrder.containsKey(uC)){
                unitOrder.get(uP).addAll(unitOrder.get(uC));
            }
        }
        visitedUnits.add(uP);
    }
}
