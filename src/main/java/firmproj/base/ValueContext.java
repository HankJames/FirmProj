package firmproj.base;

import soot.SootMethod;
import soot.Unit;
import soot.Value;

import java.util.HashMap;

public class ValueContext {
    private final SootMethod currentMethod;
    private final Unit currentUnit;
    private final HashMap<Value,String> currentValues = new HashMap<>();

    public ValueContext(SootMethod method, Unit unit){
        this(method,unit,new HashMap<>());
    }

    public ValueContext(SootMethod method, Unit unit, HashMap<Value,String> map){
        this.currentMethod = method;
        this.currentUnit = unit;
        this.currentValues.putAll(map);
    }

    public SootMethod getCurrentMethod(){
        return this.currentMethod;
    }

    public Unit getCurrentUnit(){
        return this.currentUnit;
    }

    public HashMap<Value, String> getCurrentValues() {
        return currentValues;
    }
}
