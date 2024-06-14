package firmproj.objectSim;

import firmproj.base.ValueContext;
import soot.SootMethod;
import soot.Value;
import soot.jimple.*;

import java.util.HashSet;

public class SimulateUtil {

    public static boolean hasSimClass(SootMethod method){
        return false;
    }

    public static String getSimClassValue(ValueContext vc){
        return "";
    }

    public static Object getConstant(Value value) {
        if (value instanceof StringConstant) {
            return ((StringConstant) value).value;
        } else if (value instanceof FloatConstant) {
            return ((FloatConstant) value).value;
        } else if (value instanceof IntConstant) {
            return ((IntConstant) value).value;
        } else if (value instanceof DoubleConstant) {
            return ((DoubleConstant) value).value;
        } else if (value instanceof LongConstant) {
            return ((LongConstant) value).value;
        } else if (value instanceof NullConstant) {
            return null;
        }
        return null;
    }

}
