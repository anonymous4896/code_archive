package hunter.Utils;

import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;

public class UnitHelper {
    public static InvokeExpr GetInvokeExpr(Unit u) {
        InvokeExpr result = null;
        if (u instanceof AssignStmt) {
            Value rop = ((AssignStmt) u).getRightOp();
            if (rop instanceof InvokeExpr) {
                result = ((InvokeExpr) rop);
            }
        } else if (u instanceof InvokeStmt) {
            result = ((InvokeStmt) u).getInvokeExpr();
        }
        return result;
    }
}
