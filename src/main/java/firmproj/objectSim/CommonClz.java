package firmproj.objectSim;

import firmproj.base.ValueContext;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommonClz implements AbstractClz{

    private final SootClass currentClass;
    private final SootMethod ParentMethod;
    private final HashMap<SootField, String> fieldString = new HashMap<>();
    private final List<ValueContext> valueContexts = new ArrayList<>();

    public CommonClz(SootClass currentClass, SootMethod method){
        this.currentClass = currentClass;
        this.ParentMethod = method;
    }

    public CommonClz(SootClass currentClass, SootMethod method, List<ValueContext> values){
        this(currentClass, method);
        this.valueContexts.addAll(values);
    }

    @Override
    public void init() {

    }

    @Override
    public void solve() {

    }

    @Override
    public HashMap<?, ?> getResult() {
        return fieldString;
    }

    @Override
    public SootMethod getParentMethod() {
        return this.ParentMethod;
    }

    @Override
    public SootClass getCurrentClass(){
        return this.currentClass;
    }

    @Override
    public List<ValueContext> getCurrentValues() {
        return this.valueContexts;
    }

    @Override
    public String toString() {
        return "";
    }
}
