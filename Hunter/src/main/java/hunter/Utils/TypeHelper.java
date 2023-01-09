package hunter.Utils;

import hunter.Constant;
import soot.ArrayType;
import soot.PrimType;
import soot.RefType;
import soot.Type;

public class TypeHelper {
    public static boolean isType2Maintain(Type type) {
        return ListOperator._ListFuzzy2Maintain(type.toString(), Constant.specialTypeNotFilter, Constant.specialType2Filter)
                || type instanceof PrimType;
    }

    public static boolean isArrayType(Type type) {
        return (type instanceof ArrayType || type.equals(RefType.v("java.nio.ByteBuffer"))
                || type.equals(RefType.v("java.nio.IntBuffer")) || type.equals(RefType.v("java.nio.FloatBuffer"))
                || type.equals(RefType.v("java.nio.DoubleBuffer")) || type.equals(RefType.v("java.nio.ByteBuffer"))
                || type.equals(RefType.v("java.util.List")) || type.equals(RefType.v("java.util.Map"))
                || type.equals(RefType.v("org.json.JSONArray")) || type.equals(RefType.v("java.io.InputStream")));
    }

    public static Type getBaseType(ArrayType at) {
        // return at.getElementType();
        if (at.getElementType() instanceof ArrayType) {
            return getBaseType((ArrayType) at.getElementType());
        } else {
            return at.getElementType();
        }
    }
}
