package firmproj.objectSim;

import firmproj.base.ValueContext;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

import java.util.*;

public class HashmapClz implements AbstractClz{

    private final SootClass currentClass;
    private final SootMethod ParentMethod;
    private final HashMap<String, String> result = new HashMap<>();
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
    public void addValueContexts(ValueContext valueContext) {
        this.valueContexts.add(valueContext);
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
        solve();
        StringBuilder result = new StringBuilder();
        result.append("HashMapClz: ");
        result.append(this.currentClass.toString());
        result.append("\nParent Method: ");
        result.append(this.ParentMethod);
        result.append("\nMap Entry: \n");
        for(Map.Entry<String, String> entry: this.result.entrySet()){
            result.append(entry.toString());
            result.append("\n");
        }
        result.append("============\n");
        return result.toString();
    }
}
