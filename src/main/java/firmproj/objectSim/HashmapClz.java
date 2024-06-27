package firmproj.objectSim;

import firmproj.base.ValueContext;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;

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
        HashMap<List<String>,List<String>> tmpResult = new HashMap<>();
        for(ValueContext vc : valueContexts){
            Unit u = vc.getCurrentUnit();
            if(u instanceof AssignStmt){

            }
            else if (u instanceof InvokeStmt){
                InvokeExpr invokeExpr = ((InvokeStmt) u).getInvokeExpr();
                SootMethod method = ((InvokeStmt) u).getInvokeExpr().getMethod();
                if(method.getSignature().contains("Map: java.lang.Object put(java.lang.Object,java.lang.Object)>")){
                    HashMap<Value, List<String>> currentValues = vc.getCurrentValues();
                    int argIndex = 0;
                    List<List<String>> args = new ArrayList<>();
                    for(Value value: invokeExpr.getArgs()){
                        if(value instanceof Constant) {
                            Object constObj = SimulateUtil.getConstant(value);
                            if(constObj != null)
                                args.set(argIndex,List.of(constObj.toString()));
                        }
                        else{
                            args.set(argIndex,currentValues.get(value));
                        }
                        argIndex++;
                    }
                    tmpResult.put(args.get(0), args.get(1));
                }
            }
        }
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
    public HashMap<String, String> getResult() {
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
