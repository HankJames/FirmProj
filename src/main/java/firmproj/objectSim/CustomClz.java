package firmproj.objectSim;

import firmproj.base.ValueContext;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.tagkit.AnnotationTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomClz implements AbstractClz{

    private final SootClass currentClass;
    private final SootMethod ParentMethod;
    private final HashMap<SootField, String> fieldString = new HashMap<>();
    private final List<ValueContext> valueContexts = new ArrayList<>();

    public CustomClz(SootClass currentClass, SootMethod method){
        this.currentClass = currentClass;
        this.ParentMethod = method;
    }

    public CustomClz(SootClass currentClass, SootMethod method, List<ValueContext> values){
        this(currentClass, method);
        this.valueContexts.addAll(values);
    }

    @Override
    public void init() {
        //TODO get all class field, and the ret field method, set field method.

    }

    @Override
    public void solve() {
// the @param solve.
    }

    @Override
    public void addValueContexts(ValueContext valueContext) {
        this.valueContexts.add(valueContext);
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
        solve();
        StringBuilder result = new StringBuilder();
        result.append("CustomClz: ");
        result.append(this.currentClass.toString());
        result.append("\nParent Method: ");
        result.append(this.ParentMethod);
        result.append("\nFieldStrings: ");
        for(Map.Entry<SootField, String> entry: this.fieldString.entrySet()){
            result.append(entry.toString());
            result.append(";");
        }
        return result.toString();
    }
}
