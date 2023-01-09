package hunter.Utils;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.*;
import soot.toolkits.graph.BriefUnitGraph;

import java.util.ArrayList;

public class MethodHelper {
    public static boolean isSkippedMethod(SootMethod m) {
        boolean result = false;
        if (!m.isDeclared()) {
            return true;
        }
        for (String exclude : hunter.Constant.specialMethods2Skip) {
            if (m.getDeclaringClass().getName().startsWith(exclude)) {
                result = true;
            }
        }
        for (String exclude : hunter.Constant.specialMethodsNotSkip) {
            if (m.getDeclaringClass().getName().equals(exclude)) {
                result = false;
            }
        }
        for (String exclude : hunter.Constant.specialMethodPrefixesNotSkip) {
            if (m.getDeclaringClass().getName().startsWith(exclude)) {
                result = false;
            }
        }
        return result;
    }

    public static void getAllUnitsBetween(Unit begin, Unit end, BriefUnitGraph method_cfg, ArrayList<Unit> visitedUnits) { // [A, B)
        if (begin.equals(end) || method_cfg.getSuccsOf(begin) == null || method_cfg.getSuccsOf(begin).size() == 0) {
            return;
        } else {
            visitedUnits.add(begin);
            for (Unit unit : method_cfg.getSuccsOf(begin)) {
                if (!visitedUnits.contains(unit)) {
                    getAllUnitsBetween(unit, end, method_cfg, visitedUnits);
                }
            }
        }
    }

    public static ArrayList<Unit> getAllUnitsBefore(Unit now, BriefUnitGraph method_cfg) {
        boolean isHead = false;
        ArrayList<Unit> visited = new ArrayList<>();
        while (!isHead && now != null) {
            if (visited.contains(now)) {
                break;
            }
            visited.add(now);
            for (Unit head : method_cfg.getHeads()) {
                if (now.equals(head)) {
                    isHead = true;
                    break;
                }
            }
            if (isHead) {
                break;
            }

            now = method_cfg.getPredsOf(now).get(0);
        }
        return visited;
    }

    public static Unit chooseReturn(SootMethod method) {
        ArrayList<Unit> candidatesConstant = new ArrayList<>();
        ArrayList<Unit> candidatesVariable = new ArrayList<>();
        ArrayList<Unit> candidatesIllegalConstant = new ArrayList<>();

        if (!(method.getReturnType() instanceof VoidType)) {
            for (Unit u : method.retrieveActiveBody().getUnits()) {
                if (u instanceof ReturnStmt) {
                    Value op = ((ReturnStmt) u).getOp();
                    if (op instanceof soot.jimple.Constant) {
                        if (op instanceof StringConstant) {
                            // result = "return \"" + ((StringConstant) op).value + "\"";
                            if (!((StringConstant) op).value.equals("")) {
                                candidatesConstant.add(u);
                            } else {
                                candidatesIllegalConstant.add(u);
                            }

                        } else if (op instanceof IntConstant) {
                            // result = "return " + ((IntConstant) op).value;

                            if (((IntConstant) op).value != 0 && ((IntConstant) op).value != -1) {
                                candidatesConstant.add(u);
                            } else {
                                candidatesIllegalConstant.add(u);
                            }
                        } else if (op instanceof LongConstant) {
                            // result = "return " + ((LongConstant) op).value;

                            if (((LongConstant) op).value != 0 && ((LongConstant) op).value != -1) {
                                candidatesConstant.add(u);
                            } else {
                                candidatesIllegalConstant.add(u);
                            }
                        } else if (op instanceof DoubleConstant) {
                            // result = "return " + ((DoubleConstant) op).value;

                            if (!(((DoubleConstant) op).value >= -0.0000001
                                    && ((DoubleConstant) op).value <= 0.0000001)) {
                                candidatesConstant.add(u);
                            } else {
                                candidatesIllegalConstant.add(u);
                            }

                        } else if (op instanceof FloatConstant) {
                            // result = "return " + ((FloatConstant) op).value;

                            if (!(((FloatConstant) op).value >= -0.0000001
                                    && ((FloatConstant) op).value <= 0.0000001)) {
                                candidatesConstant.add(u);
                            } else {
                                candidatesIllegalConstant.add(u);
                            }
                        }
                    } else {
                        candidatesVariable.add(u);
                    }
                }
            }
        }

        if (candidatesConstant.size() > 0) {
            // int index = (int) (Math.random() * candidatesConstant.size());
            return candidatesConstant.get(0);
        } else if (candidatesVariable.size() > 0) {
            // int index = (int) (Math.random() * candidatesVariable.size());
            return candidatesVariable.get(0);
        } else if (candidatesIllegalConstant.size() > 0) {
            // int index = (int) (Math.random() * candidatesIllegalConstant.size());
            return candidatesIllegalConstant.get(0);
        } else {
            return null;
        }
    }
}
