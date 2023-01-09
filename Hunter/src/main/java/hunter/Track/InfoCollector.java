package hunter.Track;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;

import java.util.HashMap;
import java.util.HashSet;

public class InfoCollector {
    public static HashMap<SootClass, HashMap<SootClass, HashSet<SootMethod>>> maintainAbstractReference() {
        HashMap<SootClass, HashMap<SootClass, HashSet<SootMethod>>> superClass2SubClass = new HashMap<>();

        for (SootClass subClass : Scene.v().getApplicationClasses()) {
            if (!subClass.getSuperclass().getName().equals("java.lang.Object")) {
                SootClass superClass = subClass.getSuperclass();
                // System.out.println(String.format("Super -> %s, Sub -> %s", superClass.getName(), subClass.getName()));

                HashSet<SootMethod> subMethods2Recode = new HashSet<>();
                for (SootMethod superMethod : superClass.getMethods()) {
                    for (SootMethod subMethod : subClass.getMethods()) {
                        if (subMethod.getName().equals(superMethod.getName()) && subMethod.isConcrete()
                                && subMethod.hasActiveBody()) {
                            subMethods2Recode.add(subMethod);
                        }
                    }
                }
                if (!subMethods2Recode.isEmpty()) {
                    if (!superClass2SubClass.containsKey(superClass)) {
                        HashMap<SootClass, HashSet<SootMethod>> class2Method = new HashMap<>();
                        class2Method.put(subClass, subMethods2Recode);
                        superClass2SubClass.put(superClass, class2Method);

                    } else {
                        superClass2SubClass.get(superClass).put(subClass, subMethods2Recode);
                    }
                }
            }
        }

        return superClass2SubClass;
    }

    public static HashMap<SootMethod, BriefUnitGraph> maintainMethod2Graph() {
        HashMap<SootMethod, BriefUnitGraph> method2Graph = new HashMap<>();
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.hasActiveBody() || !sootMethod.isConcrete())
                    continue;
                method2Graph.put(sootMethod, new BriefUnitGraph(sootMethod.retrieveActiveBody()));
            }
        }
        return method2Graph;
    }

    public static HashMap<Unit, SootMethod> maintainUnits2Method() {
        HashMap<Unit, SootMethod> allUnits2Methods = new HashMap<>();
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.hasActiveBody() || !sootMethod.isConcrete()
                ) {
                    continue;
                }

                for (Unit unit : sootMethod.retrieveActiveBody().getUnits()) {
                    allUnits2Methods.put(unit, sootMethod);
                }
            }
        }
        return allUnits2Methods;
    }

}
