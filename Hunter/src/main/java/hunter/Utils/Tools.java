package hunter.Utils;

import hunter.Track.TrackInfo;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Tools {
    public static boolean isCallingSink(Unit currentUnit, HashSet<SootMethod> sinks) {
        InvokeExpr invokeExpr = UnitHelper.GetInvokeExpr(currentUnit);
        if (invokeExpr == null) {
            return false;
        } else {
            HashSet<SootMethod> visitedMethods = new HashSet<>();
            SootMethod calleeMethod = invokeExpr.getMethod();
            if (sinks.contains(calleeMethod)) {
                return true;
            } else {
                return findCallee(calleeMethod, sinks, visitedMethods);
            }
        }
    }

    private static boolean findCallee(SootMethod currentMethod, HashSet<SootMethod> sinks, HashSet<SootMethod> visitedMethods) {
        if (sinks.contains(currentMethod)) return true;
        boolean result = false;
        if (!visitedMethods.contains(currentMethod) && !result) {
            visitedMethods.add(currentMethod);
            CallGraph callGraph = Scene.v().getCallGraph();
            for (Iterator<Edge> it = callGraph.edgesOutOf(currentMethod); it.hasNext(); ) {
                Edge edge = it.next();
                SootMethod calleeMethod = edge.tgt();
                result = result || findCallee(calleeMethod, sinks, visitedMethods);
            }

        }
        return result;
    }

    public static HashMap<String, ArrayList<String>> loadModelNames(String filePath) {

        HashMap<String, ArrayList<String>> results = new HashMap<>();

        try {
            BufferedReader in = new BufferedReader(new FileReader(filePath));
            String str;
            int counter = 0;
            while ((str = in.readLine()) != null) {
                String[] models = str.split(",");
                if (!results.containsKey(models[0])) {
                    results.put(models[0], new ArrayList<>(Arrays.asList(models[1])));
                    // System.out.println(String.format("#%d, apkName: %s, modelName %s.", ++counter, models[0], models[1]));
                } else if (!results.get(models[0]).contains(models[1])) {
                    results.get(models[0]).add(models[1]);
                    // System.out.println(String.format("#%d, apkName: %s, modelName %s.", ++counter, models[0], models[1]));
                }
            }
        } catch (IOException e) {
            System.out.println(String.format("Model path %s can not be found!!!", filePath));
        }

        return results;
    }


    public static HashSet<TrackInfo> buildModelMetaData(String modelPath) {
        HashSet<TrackInfo> trackInfos = new HashSet<>();

        try {
            BufferedReader in = new BufferedReader(new FileReader(modelPath));
            String str;
            int counter = 0;
            while ((str = in.readLine()) != null) {
                String[] infos = str.split(";");
                String modelName = infos[0];
                String apkName = infos[1];
                String framework = infos[2];
                String inputType = infos[3];

                TrackInfo trackInfo = new TrackInfo(apkName);
                trackInfo.setModelName(modelName);
                trackInfo.setFramework(framework);
                trackInfo.setInputType(inputType);

                //Some frameworks, e.g., TFLite, do not have input and output layers.
                if (infos.length >= 6) {
                    if (infos[4].contains(",")) {
                        String[] inputLayers = infos[4].split(",");
                        for(String s : inputLayers){
                            trackInfo.addInputLayer(s);
                        }
                    } else {
                        String inputLayer = infos[4];
                        trackInfo.addInputLayer(inputLayer);
                    }

                    if (infos[5].contains(",")) {
                        String[] outputLayers = infos[5].split(",");
                        for(String s : outputLayers){
                            trackInfo.addOutputLayer(s);
                        }
                    } else {
                        String outputLayer = infos[5];
                        trackInfo.addOutputLayer(outputLayer);
                    }
                }
                trackInfos.add(trackInfo);
            }
        } catch (IOException e) {
            System.out.println(String.format("Model path %s can not be found!!!", modelPath));
        }

        return trackInfos;
    }

    // public static boolean hasPath(Unit from, Unit to, SootMethod method, HashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> unitOrder, int layer) {
    //     if (from.equals(to)) {
    //         return true;
    //     }

    //     boolean result = false;
    //     if (!unitOrder.get(method).containsKey(from)) return false;

    //     if (unitOrder.get(method).get(from).contains(to)) return true;
    //     for (Unit t : unitOrder.get(method).get(from)) {
    //         result = result || hasPath(t, to, method, unitOrder, layer + 1);
    //     }

    //     return result;
    // }

    public static boolean hasPath(Unit from, Unit to, SootMethod method, ConcurrentHashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> unitOrderEfficient) {

        if (from.equals(to)) {
            return true;
        }

        return unitOrderEfficient.get(method).containsKey(from) && unitOrderEfficient.get(method).get(from).contains(to);
    }

//     private static HashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> buildEfficientPath(HashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> unitOrder) {

//         for (SootMethod sootMethod : unitOrder.keySet()) {
//             int sumNumb = 0;
//             while (sumNumb != sumAllUnits(unitOrder.get(sootMethod))) {
//                 sumNumb = sumAllUnits(unitOrder.get(sootMethod));

//                 for (Unit unit : unitOrder.get(sootMethod).keySet()) {
//                     HashSet<Unit> toAdd = new HashSet<>();
//                     for (Unit u : unitOrder.get(sootMethod).get(unit)) {
//                         if (unitOrder.get(sootMethod).containsKey(u)) {
//                             toAdd.addAll(unitOrder.get(sootMethod).get(u));
//                         }
//                     }
//                     unitOrder.get(sootMethod).get(unit).addAll(toAdd);
//                 }
//             }
//         }

//         return unitOrder;
//     }

//     private static int sumAllUnits(HashMap<Unit, HashSet<Unit>> unitHashSetHashMap) {

//         int sum = 0;
//         for (Unit u : unitHashSetHashMap.keySet()) {
//             sum += unitHashSetHashMap.get(u).size();
//         }
//         return sum;
//     }


// //    public static boolean hasPath(Unit from, Unit to, SootMethod method, HashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> unitOrder){
// //        return hasPath(from, to, method, unitOrder, 0);
// //    }


//     public static HashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> buildMethod2UnitOrder() {

//         HashMap<SootMethod, HashSet<Pair<Unit, Unit>>> method2UnitOrder = new HashMap<>();

//         for (SootClass sootClass : Scene.v().getApplicationClasses()) {
//             for (SootMethod sootMethod : sootClass.getMethods()) {

//                 if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || Tracker._isFunction2Skip(sootMethod))
//                     continue;

//                 method2UnitOrder.put(sootMethod, new HashSet<>());
//                 BriefUnitGraph briefUnitGraph = new BriefUnitGraph(sootMethod.retrieveActiveBody());
//                 HashSet<Unit> visitedUnits = new HashSet<>();

//                 for (Unit unit : briefUnitGraph.getHeads()) {

//                     dfsCollection(unit, briefUnitGraph, visitedUnits, method2UnitOrder.get(sootMethod));
//                 }
//             }
//         }

//         HashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> results = new HashMap<>();

//         for (SootMethod sootMethod : method2UnitOrder.keySet()) {
//             if (!results.containsKey(sootMethod)) {
//                 results.put(sootMethod, new HashMap<>());
//             }

//             for (Pair<Unit, Unit> p : method2UnitOrder.get(sootMethod)) {
//                 Unit from = p.getO1();
//                 Unit to = p.getO2();

//                 if (!results.get(sootMethod).containsKey(from)) {
//                     results.get(sootMethod).put(from, new HashSet<>());
//                 }

//                 results.get(sootMethod).get(from).add(to);
//             }
//         }

//         return buildEfficientPath(results);
//     }

//     private static void dfsCollection(Unit currentUnit, BriefUnitGraph briefUnitGraph, HashSet<Unit> visitedUnits, HashSet<Pair<Unit, Unit>> results) {
//         if (visitedUnits.contains(currentUnit)) return;

// //        System.out.println("into dfsCollection " + briefUnitGraph.getBody().getMethod().getSignature());
// //        System.out.println("                   " + currentUnit.toString());

//         HashSet<Unit> innerVisitedUnits = new HashSet<>();
//         for (Unit nextUnit : briefUnitGraph.getSuccsOf(currentUnit)) {
//             HashSet<Unit> newVisitedUnits = new HashSet<>();
//             newVisitedUnits.addAll(visitedUnits);
//             newVisitedUnits.add(currentUnit);
//             Pair<Unit, Unit> p = new Pair<>();
//             p.setPair(currentUnit, nextUnit);
//             if (results.contains(p)) return;
//             if (!newVisitedUnits.contains(nextUnit) && !isThereAPath(nextUnit, currentUnit, briefUnitGraph, newVisitedUnits, innerVisitedUnits)) {

//                 Pair<Unit, Unit> pair = new Pair<>();
//                 pair.setPair(currentUnit, nextUnit);
//                 results.add(pair);
//                 dfsCollection(nextUnit, briefUnitGraph, newVisitedUnits, results);

//             }
//         }
//     }


//     private static boolean isThereAPath(Unit from, Unit to, BriefUnitGraph briefUnitGraph, HashSet<Unit> visitedUnits, HashSet<Unit> innerVisitedUnits) {
//         boolean result = false;

// //        System.out.println("into isThereAPath " + briefUnitGraph.getBody().getMethod().getSignature());
// //        System.out.println("                   " + from.toString());
// //        System.out.println("                   " + to.toString());

//         for (Unit u : briefUnitGraph.getSuccsOf(from)) {
//             if (visitedUnits.contains(u) && !innerVisitedUnits.contains(u)) {
//                 if (u.equals(to)) return true;
//                 else {
//                     innerVisitedUnits.add(u);
//                     result = result || isThereAPath(u, to, briefUnitGraph, visitedUnits, innerVisitedUnits);
//                 }
//             }
//         }

//         return result;
//     }
}
