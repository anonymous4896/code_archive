package hunter.Utils;

import soot.Value;
import soot.jimple.*;
import soot.jimple.internal.*;

public class ValueHelper {
    public static Value getNegCondition(Value condition){
        Value result = condition;

        if (!(condition instanceof BinopExpr)){
            return result;
        }

        Value lop = ((BinopExpr) condition).getOp1();
        Value rop = ((BinopExpr) condition).getOp2();

        if(condition instanceof EqExpr){
            // == -> !=
            result = new JNeExpr(lop , rop);
        }else if(condition instanceof GeExpr){
            // >= -> <
            result = new JLtExpr(lop , rop);
        }else if(condition instanceof GtExpr){
            // > -> <=
            result = new JLeExpr(lop , rop);
        }else if(condition instanceof LeExpr){
            // <= -> >
            result = new JGtExpr(lop , rop);
        }else if(condition instanceof LtExpr){
            // < -> >=
            result = new JGeExpr(lop , rop);
        }else if(condition instanceof NeExpr){
            // != -> ==
            result = new JEqExpr(lop , rop);
        }

        return result;
    }
}
