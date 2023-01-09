package hunter.Utils;

import soot.*;
import soot.jimple.*;

import java.util.ArrayList;
import java.util.HashMap;

public class FieldHelper {
    public static HashMap<Integer, ArrayList<Unit>> chooseField(SootField sootField, int direction) {
        // Unit result;
        HashMap<Integer, ArrayList<Unit>> results = new HashMap<>();

        results.put(hunter.Constant.DIRECTION_BACKWARD, new ArrayList<>());
        results.put(hunter.Constant.DIRECTION_FORWARD, new ArrayList<>());

        if (direction == hunter.Constant.DIRECTION_BACKWARD) {
            ArrayList<Unit> candidatesConstant = new ArrayList<>();
            ArrayList<Unit> candidatesVariable = new ArrayList<>();
            ArrayList<Unit> candidatesIllegalConstant = new ArrayList<>();

            ArrayList<Unit> forwardList = new ArrayList<>();

            for (SootClass sootClass : Scene.v().getApplicationClasses()) {
                for (SootMethod sootMethod : sootClass.getMethods()) {
                    if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || MethodHelper.isSkippedMethod(sootMethod))
                        continue;
                    for (Unit gUnit : sootMethod.retrieveActiveBody().getUnits()) {
                        if (gUnit instanceof AssignStmt && ((AssignStmt) gUnit).getLeftOp() instanceof FieldRef
                                && sootField.equals(((FieldRef) ((AssignStmt) gUnit).getLeftOp()).getField())) {
                            Value op = ((AssignStmt) gUnit).getRightOp();
                            if (op instanceof soot.jimple.Constant) {
                                if (op instanceof StringConstant) {
                                    // result = "return \"" + ((StringConstant) op).value + "\"";

                                    if (!((StringConstant) op).value.equals("")) {
                                        candidatesConstant.add(gUnit);
                                    } else {
                                        candidatesIllegalConstant.add(gUnit);
                                    }

                                } else if (op instanceof IntConstant) {
                                    // result = "return " + ((IntConstant) op).value;

                                    if (((IntConstant) op).value != 0 && ((IntConstant) op).value != -1) {
                                        candidatesConstant.add(gUnit);
                                    } else {
                                        candidatesIllegalConstant.add(gUnit);
                                    }
                                } else if (op instanceof LongConstant) {
                                    // result = "return " + ((LongConstant) op).value;
                                    // result = "return " + op.toString();
                                    if (((LongConstant) op).value != 0 && ((LongConstant) op).value != -1) {
                                        candidatesConstant.add(gUnit);
                                    } else {
                                        candidatesIllegalConstant.add(gUnit);
                                    }
                                } else if (op instanceof DoubleConstant) {
                                    // result = "return " + ((DoubleConstant) op).value;
                                    // result = "return " + op.toString();
                                    if (!(((DoubleConstant) op).value >= -0.0000001
                                            && ((DoubleConstant) op).value <= 0.0000001)) {
                                        candidatesConstant.add(gUnit);
                                    } else {
                                        candidatesIllegalConstant.add(gUnit);
                                    }

                                } else if (op instanceof FloatConstant) {
                                    // result = "return " + ((FloatConstant) op).value;
                                    // result = "return " + op.toString();
                                    if (!(((FloatConstant) op).value >= -0.0000001
                                            && ((FloatConstant) op).value <= 0.0000001)) {
                                        candidatesConstant.add(gUnit);
                                    } else {
                                        candidatesIllegalConstant.add(gUnit);
                                    }
                                }
                            } else {
                                // result = "return " + translateImmediate(((ReturnStmt) u).getOp());
                                candidatesVariable.add(gUnit);
                            }
                        }

                        if (TypeHelper.isArrayType(sootField.getType()) && gUnit instanceof AssignStmt
                                && ((AssignStmt) gUnit).getRightOp() instanceof FieldRef
                                && sootField.equals(((FieldRef) ((AssignStmt) gUnit).getRightOp()).getField())) {
                            forwardList.add(gUnit);
                        }
                    }
                }
            }

            results.get(hunter.Constant.DIRECTION_BACKWARD).addAll(candidatesConstant);
            results.get(hunter.Constant.DIRECTION_BACKWARD).addAll(candidatesVariable);
            results.get(hunter.Constant.DIRECTION_BACKWARD).addAll(candidatesIllegalConstant);

            results.get(hunter.Constant.DIRECTION_FORWARD).addAll(forwardList);
        } else if (direction == hunter.Constant.DIRECTION_FORWARD) {
            ArrayList<Unit> forwardList = new ArrayList<>();
            for (SootClass sootClass : Scene.v().getApplicationClasses()) {
                for (SootMethod sootMethod : sootClass.getMethods()) {
                    if (!sootMethod.isConcrete() || !sootMethod.hasActiveBody() || MethodHelper.isSkippedMethod(sootMethod))
                        continue;
                    for (Unit gUnit : sootMethod.retrieveActiveBody().getUnits()) {
                        if (gUnit instanceof AssignStmt && ((AssignStmt) gUnit).getRightOp() instanceof FieldRef
                                && sootField.equals(((FieldRef) ((AssignStmt) gUnit).getRightOp()).getField())) {
                            forwardList.add(gUnit);
                        }
                    }
                }
            }
            if (!forwardList.isEmpty()) {
//                int index = (int) (Math.random() * forwardList.size());
//                 results.get(Constant.DIRECTION_FORWARD).add(forwardList.get(index));
                results.get(hunter.Constant.DIRECTION_FORWARD).addAll(forwardList);
            }
        }

        return results;
    }
}
