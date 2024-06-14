package firmproj.base;
import firmproj.objectSim.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;

import soot.jimple.internal.AbstractFloatBinopExpr;

import java.util.*;

public class MethodLocal {
    private static final Logger LOGGER = LogManager.getLogger(MethodString.class);
    private final SootMethod sootMethod;

    private final HashSet<Value> InterestingVariables = new HashSet<>();
    private final HashMap<Value, AbstractClz> LocalToClz = new HashMap<>();
    private final HashMap<Value, List<String>> LocalToString = new HashMap<>();
    private final HashMap<Value, Integer> LocalFromParams = new HashMap<>();

    private final HashMap<Value, Integer> ArrayRefLocal = new HashMap<>();

    private ValueContext ParentValueContext;
    private HashMap<Integer, List<String>> ParamsToString = new HashMap<>();

    private final List<ValueContext> ChildValueContext = new ArrayList<>();
    private final List<String> returnValues = new ArrayList<>();


    //private List<String> unsolvedLocals = new ArrayList<>();

    public MethodLocal(SootMethod Method){
        this.sootMethod = Method;
    }

    public MethodLocal(SootMethod Method, ValueContext Values){
        this(Method);
        ParentValueContext = Values;
        if(ParentValueContext != null){
            InvokeExpr invokeExpr = (InvokeExpr) ParentValueContext.getCurrentUnit();
            if(invokeExpr != null){
                HashMap<Value,List<String>> currentValues =  ParentValueContext.getCurrentValues();
                Integer index = 0;
                HashMap<Integer, List<String>> ParamValue = new HashMap<>();
                for(Value param: invokeExpr.getArgs()){
                    if(currentValues.containsKey(param)){
                        ParamValue.put(index, currentValues.get(param));
                    }
                    else if(param instanceof Constant){
                        ParamValue.put(index, List.of(param.toString()));
                    }
                    index++;
                }
                if (!ParamValue.isEmpty()) ParamsToString = ParamValue;
            }
        }
    }

    public List<MethodLocal> doAnalysis(){
        Body body = null;
        try{
            body = this.sootMethod.retrieveActiveBody();
        }
        catch(Exception e){
            LOGGER.error(e);
        }
        if(body == null) return new ArrayList<>();
        for(Unit unit : body.getUnits()){
            Stmt stmt = (Stmt) unit;
            if(stmt instanceof AssignStmt){
                caseAssignStmt((AssignStmt) stmt);
            }
            else if (stmt instanceof IdentityStmt){
                //caseIdentityStmt((IdentityStmt) stmt);
            }
            else if(stmt instanceof InvokeExpr){
                //caseInvokeExpr((InvokeExpr) stmt);
            }
            else if(stmt instanceof ReturnStmt){
                caseReturnStmt((ReturnStmt) stmt);
            }
        }
        return new ArrayList<>();
    }

    private void caseReturnStmt(ReturnStmt stmt) {
        Value value = stmt.getOp();
        if(LocalToString.containsKey(value)) returnValues.addAll(LocalToString.get(value));
        else if(value instanceof Constant) returnValues.add(value.toString());
    }

    public void caseAssignStmt(AssignStmt stmt) {
        Value leftOperation = stmt.getLeftOp();
        Value rightOperation = stmt.getRightOp();
        HashSet<?> result = null;
        boolean arrayRef = false;
        int arrayIndex = -1;
        if (leftOperation instanceof Local || leftOperation instanceof ArrayRef) {
            if(leftOperation instanceof ArrayRef) {
                arrayRef = true;
                ArrayRef array = (ArrayRef) leftOperation;
                Integer index = (Integer) SimulateUtil.getConstant(array.getIndex());
                if(index!=null) arrayIndex = index;
                //TODO array Ref.
            }
            if (rightOperation instanceof InstanceInvokeExpr) {
                boolean InterestingTransfer = false;
                InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) rightOperation;
                Value base = instanceInvokeExpr.getBase();
                SootMethod invokeMethod = instanceInvokeExpr.getMethod();

                HashMap<Value, List<String>> valueString = getInvokeExprValues(instanceInvokeExpr);
                ValueContext valueContext = new ValueContext(this.sootMethod,stmt,valueString);

                if(getInterestingVariables().contains(base)){
                    InterestingTransfer = true;
                    if(LocalToClz.containsKey(base)){
                        AbstractClz clz = LocalToClz.get(base);
                        clz.addValueContexts(valueContext);
                    }
                }
                if(MethodString.getMethodToString().containsKey(invokeMethod)) {
                    InterestingTransfer = true;
                    addLocalTOString(leftOperation, MethodString.GetMethodToString(invokeMethod));
                }
                else{
                    //TODO Invoke Return.
                    MethodLocal nextMethod = new MethodLocal(invokeMethod, valueContext);
                    nextMethod.doAnalysis();
                    List<String> ret = nextMethod.getReturnValues();
                    if(!ret.isEmpty()) {
                        addLocalTOString(leftOperation, ret);
                        InterestingTransfer = true;
                    }
                }

                if(InterestingTransfer)
                    InterestingVariables.add(leftOperation);

            } else if (rightOperation instanceof NewExpr) {
                NewExpr newExpr = (NewExpr) rightOperation;
                String className = newExpr.getClass().getName();
                if(isCommonClz(className)){
                    AbstractClz NewClz = CreateCommonClz(newExpr.getBaseType().getSootClass(), this.sootMethod);
                    LocalToClz.put(leftOperation, NewClz);
                    InterestingVariables.add(leftOperation);
                }
                else{
                    AbstractClz NewClz = new CustomClz(newExpr.getBaseType().getSootClass(), this.sootMethod);
                    LocalToClz.put(leftOperation, NewClz);
                    InterestingVariables.add(leftOperation);
                }

            } else if( rightOperation instanceof StaticInvokeExpr){
                //TODO process normal method, like toJson, toString..; also the encrypt method.
                InvokeExpr invokeExpr = (InvokeExpr) rightOperation;
                HashMap<Value, List<String>> valueString = getInvokeExprValues(invokeExpr);
                ValueContext valuecontext = new ValueContext(this.sootMethod, stmt, valueString);
                if(SimulateUtil.hasSimClass(invokeExpr.getMethod())){
                    String simResult = SimulateUtil.getSimClassValue(valuecontext);
                    if(!simResult.isEmpty()) addLocalTOString(leftOperation, List.of(simResult));
                }
                addChildValueContext(valuecontext);
                MethodLocal nextMethodLocal = new MethodLocal(invokeExpr.getMethod(), valuecontext);

            }
            else if (rightOperation instanceof ArrayRef){

            }
            else if (rightOperation instanceof FieldRef) {

            }
            else if (rightOperation instanceof NewArrayExpr) {
                NewArrayExpr newArray = (NewArrayExpr)rightOperation;
                Integer index = (Integer) SimulateUtil.getConstant(newArray.getSize());
                if(index==null) index = 0;
                List<String> arrayList = new ArrayList<>();
                while(arrayList.size()< index){
                    arrayList.add("");
                }
                if(!LocalToString.containsKey(leftOperation))LocalToString.put(leftOperation, arrayList);
                ArrayRefLocal.put(leftOperation,index);
                this.InterestingVariables.add(leftOperation);
            }
            else if (rightOperation instanceof BinopExpr) {

            }
            else if (rightOperation instanceof ParameterRef){
                //Impossible, just goto identityStmt.
            }
            else if (rightOperation instanceof Local){
                if(!LocalFromParams.containsKey(leftOperation)){
                    if(LocalToString.containsKey(rightOperation)){
                        addLocalTOString(leftOperation, LocalToString.get(rightOperation));
                        this.InterestingVariables.add(leftOperation);
                    }
                }
            }
            else if(rightOperation instanceof CastExpr || rightOperation instanceof ArrayRef) {

            }
            else if(rightOperation instanceof Constant){

            }
        } else {
            LOGGER.warn(String.format("[%s] [SIMULATE][left unknown]: %s (%s)", this.hashCode(), stmt, leftOperation.getClass()));
        }

    }

    public void caseInvokeStmt(InvokeStmt stmt) {
        String signature = stmt.getInvokeExpr().getMethod().toString();
        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        Value base = null;
        if (invokeExpr instanceof InstanceInvokeExpr) {
            base = ((InstanceInvokeExpr) invokeExpr).getBase();
            HashMap<Value, List<String>> valueString = getInvokeExprValues(invokeExpr);
            ValueContext valueContext = new ValueContext(this.sootMethod, stmt, valueString);
            if(LocalToClz.containsKey(base)) LocalToClz.get(base).addValueContexts(valueContext);
        }
        return;
    }

    public void caseIdentityStmt(IdentityStmt stmt){
        Value leftOperation = stmt.getLeftOp();
        Value rightOperation = stmt.getRightOp();
        HashSet<?> result = null;
        if (leftOperation instanceof Local || leftOperation instanceof ArrayRef) {
            if (rightOperation instanceof ParameterRef) {
                ParameterRef ref = (ParameterRef) rightOperation;
                if(ParamsToString.containsKey(ref.getIndex())){
                    List<String> paramStr = ParamsToString.get(ref.getIndex());
                    addLocalTOString(leftOperation, paramStr);
                    if(paramStr.get(0).equals("@Param")){
                        LocalFromParams.put(leftOperation, ref.getIndex());
                    }
                    InterestingVariables.add(leftOperation);
                }
                else if (MethodString.isCommonType(ref.getType())) {
                    addLocalTOString(leftOperation, List.of("@Param", ref.getType().toString()));
                    LocalFromParams.put(leftOperation, ref.getIndex());
                    InterestingVariables.add(leftOperation);
                }
            }
        }
    }

    private void addChildValueContext(ValueContext vc){
        this.ChildValueContext.add(vc);
    }

    private void addLocalTOString(Value local, List<String> string){
        if(ArrayRefLocal.containsKey(local)){
            LocalToString.get(local).addAll(string);
        }
        else{
            LocalToString.put(local, string);
        }
    }

    public HashMap<Value, List<String>> getInvokeExprValues(InvokeExpr invokeExpr){
        HashMap<Value, List<String>> valueString = new HashMap<>();
        for(Value value: invokeExpr.getArgs()){
            if(value instanceof Constant) continue;
            if(value instanceof Local) {
                if(LocalToClz.containsKey(value)){
                    String Clzstring = LocalToClz.get(value).toString();
                    addLocalTOString(value, List.of(Clzstring));
                    valueString.put(value, LocalToString.get(value));
                }
                else if(LocalToString.containsKey(value)) valueString.put(value,LocalToString.get(value));

            }
        }
        return valueString;
    }


    public boolean isCommonClz(String className){
        //TODO
        return false;
    }

    public ValueContext getParentValueContext() {
        return ParentValueContext;
    }

    public List<String> getReturnValues() {
        return returnValues;
    }

    public HashMap<Integer, List<String>> getParamsToString() {
        return ParamsToString;
    }

    public List<ValueContext> getChildValueContext() {
        return ChildValueContext;
    }

    public AbstractClz CreateCommonClz(SootClass clz, SootMethod currentMethod){
        return new HashmapClz(clz, currentMethod);
    }

    public SootMethod getSootMethod(){
        return this.sootMethod;
    }

    public HashMap<Value, AbstractClz> getLocalToClz() {
        return LocalToClz;
    }

    public HashMap<Value, List<String>> getLocalToString() {
        return LocalToString;
    }

    public HashMap<Value, Integer> getLocalFromParams() {
        return LocalFromParams;
    }

//    public List<String> getUnsolvedLocals() {
//        return unsolvedLocals;
//    }

    public HashSet<Value> getInterestingVariables() {
        return InterestingVariables;
    }
}
