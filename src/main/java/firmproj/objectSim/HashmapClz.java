package firmproj.objectSim;

import firmproj.base.ValueContext;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HashmapClz implements AbstractClz{

    private final SootClass currentClass;
    private final SootMethod ParentMethod;
    private final HashMap<SootField, String> result = new HashMap<>();
    private final List<ValueContext> valueContexts = new ArrayList<>();

    public HashmapClz(SootClass currentClass, SootMethod method){
        this.currentClass = currentClass;
        this.ParentMethod = method;
    }

    public HashmapClz(SootClass currentClass, SootMethod method, List<ValueContext> values){
        this(currentClass, method);
        this.valueContexts.addAll(values);
    }

    @Override
    public void solve() {

    }

    @Override
    public void init() {

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
    public HashMap<?, ?> getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "";
    }
}
