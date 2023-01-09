package hunter.Utils;

import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.SwitchStmt;
import soot.toolkits.graph.BriefUnitGraph;

import java.util.ArrayList;

public class BranchHelper {
    public static Unit _getIfTrue(IfStmt ifUnit) {
        Unit nowBranchTrue = ifUnit.getTarget();
        return nowBranchTrue;
    }

    public static Unit _getIfFalse(IfStmt ifUnit, BriefUnitGraph method_cfg) {
        Unit nowBranchTrue = _getIfTrue(ifUnit);
        Unit nowBranchFalse = nowBranchTrue;
        if (method_cfg.getSuccsOf(ifUnit).size() > 1) {
            nowBranchFalse = method_cfg.getSuccsOf(ifUnit).get(0).equals(nowBranchTrue)
                    ? method_cfg.getSuccsOf(ifUnit).get(1)
                    : method_cfg.getSuccsOf(ifUnit).get(0);
        }
        return nowBranchFalse;
    }

    public static Unit _findIfExit(ArrayList<Unit> visited, IfStmt ifUnit, BriefUnitGraph method_cfg) {
        ArrayList<Unit> newVisited = new ArrayList<>();
        newVisited.addAll(visited);
        Unit now = ifUnit;
        while (now != null) {
            newVisited.add(now);
            System.out.println("_findIfExit FALSE " + now.toString());
            if (method_cfg.getTails().contains(now)) {
                break;
            }

            if (now instanceof IfStmt) {
//                Unit nowBranchTrue = ((IfStmt) now).getTarget();
//                Unit nowBranchFalse = nowBranchTrue;
//                if (method_cfg.getSuccsOf(now).size() > 1) {
//                    nowBranchFalse = method_cfg.getSuccsOf(now).get(0).equals(nowBranchTrue)
//                            ? method_cfg.getSuccsOf(now).get(1)
//                            : method_cfg.getSuccsOf(now).get(0);
//                }
                now = _getIfFalse((IfStmt) now, method_cfg);
            } else if (now instanceof SwitchStmt) {
                // TODO
                now = ((SwitchStmt) now).getDefaultTarget();
            } else {
                now = method_cfg.getSuccsOf(now) == null ? null : method_cfg.getSuccsOf(now).get(0);
                if (newVisited.contains(now)) {
                    break;
                }
            }
        }

        now = ifUnit;
        while (!method_cfg.getTails().contains(now) && now != null) {
            System.out.println("_findIfExit TRUE " + now.toString());
            if (newVisited.contains(now) && !now.equals(ifUnit)) {
                return now;
            }
            if (now instanceof IfStmt) {
                Unit nowBranchTrue = ((IfStmt) now).getTarget();
                now = nowBranchTrue;
            } else if (now instanceof SwitchStmt) {
                // TODO
                now = ((SwitchStmt) now).getDefaultTarget();
            } else {
                now = method_cfg.getSuccsOf(now) == null ? null : method_cfg.getSuccsOf(now).get(0);
            }
        }
        return null;
    }
}
