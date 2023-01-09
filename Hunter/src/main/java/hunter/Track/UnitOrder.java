package hunter.Track;

import hunter.Utils.MethodHelper;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UnitOrder {
    public static ConcurrentHashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> maintain() {
        System.out.println("start maintain unit order");

        ExecutorService executor = Executors.newFixedThreadPool(hunter.Constant.DEFAULT_THREAD_NUM);

        ConcurrentHashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> unitOrder = new ConcurrentHashMap<>();
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || MethodHelper.isSkippedMethod(sootMethod))
                    continue;

                executor.execute(new MaintainUnitOrderTask(sootMethod, unitOrder));

            }
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        System.out.println("maintain unit order finish");
        return unitOrder;
    }

    public static ConcurrentHashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> updatePartly(ConcurrentHashMap<SootMethod, HashMap<Unit, HashSet<Unit>>> unitOrder, HashSet<SootMethod> methodSet) {
        ExecutorService executor = Executors.newFixedThreadPool(hunter.Constant.DEFAULT_THREAD_NUM);

        for (SootMethod sootMethod : methodSet) {
            if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || MethodHelper.isSkippedMethod(sootMethod))
                continue;

            executor.execute(new MaintainUnitOrderTask(sootMethod, unitOrder));
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        return unitOrder;
    }
}
