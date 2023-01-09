package hunter.Utils;

import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.FieldRef;

import java.util.ArrayList;

public class ListOperator {
    // one && two
    public static ArrayList<Value> _valueListIntersection(ArrayList<Value> one, ArrayList<Value> two) {
        ArrayList<Value> intersection = new ArrayList<>();
        for (Value v : one) {
            if (v instanceof FieldRef) {
                for (Value vt : two) {
                    if (vt instanceof FieldRef) {
                        if (((FieldRef) vt).getField().equals(((FieldRef) v).getField()) && !intersection.contains(v)) {
                            intersection.add(v);
                        }
                    }
                }
            } else if (two.contains(v) && !intersection.contains(v)) {
                intersection.add(v);
            }
        }
        return intersection;
    }

    // one && two
    public static ArrayList<Unit> _ListIntersection(ArrayList<Unit> one, ArrayList<Unit> two) {
        ArrayList<Unit> intersection = new ArrayList<>();
        for (Unit v : one) {
            if (two.contains(v) && !intersection.contains(v)) {
                intersection.add(v);
            }
        }
        return intersection;
    }

    // one || two
    public static ArrayList<Value> _valueListUnion(ArrayList<Value> one, ArrayList<Value> two) {
        ArrayList<Value> union = new ArrayList<>();
        for (Value v : one) {
            if (!union.contains(v))
                union.add(v);
        }
        for (Value v : two) {
            if (!union.contains(v))
                union.add(v);
        }
        ArrayList<SootField> existField = new ArrayList<>();
        ArrayList<Value> toRemove = new ArrayList<>();

        for (Value v : union) {
            if (v instanceof FieldRef) {
                if (existField.contains(((FieldRef) v).getField())) {
                    toRemove.add(v);
                } else {
                    existField.add(((FieldRef) v).getField());
                }
            }
        }
        union.removeAll(toRemove);
        return union;
    }

    // one - (one && two)
    public static ArrayList<Value> _valueListComplement(ArrayList<Value> one, ArrayList<Value> two) {
        ArrayList<Value> complement = new ArrayList<>();
        for (Value v : one) {
            if (v instanceof FieldRef) {
                boolean flag = false;
                for (Value vt : two) {
                    if (vt instanceof FieldRef) {
                        if (((FieldRef) vt).getField().equals(((FieldRef) v).getField())) {
                            flag = true;
                        }
                    }
                }
                if (!flag)
                    complement.add(v);
            } else if (!two.contains(v) && !complement.contains(v)) {
                complement.add(v);
            }

        }
        return complement;
    }

    public static boolean _ListFuzzyContains(String str, ArrayList<String> toCheck) {
        for (String s : toCheck) {
            if (str.startsWith(s))
                return true;
        }
        return false;
    }

    public static boolean _ListFuzzy2Maintain(String str, ArrayList<String> toMaintain, ArrayList<String> toRemove) {
        // System.out.println("")
        boolean result = false;
        for (String s : toMaintain) {
            if (str.startsWith(s))
                result = true;
        }
        for (String s : toRemove) {
            if (str.startsWith(s))
                result = false;
        }
        return result;
    }
}
