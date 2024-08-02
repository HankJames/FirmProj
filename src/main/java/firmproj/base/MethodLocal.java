package firmproj.base;
import firmproj.graph.CallGraph;
import firmproj.graph.CallGraphNode;
import firmproj.objectSim.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;

import java.util.*;

public class MethodLocal {
    private static final Logger LOGGER = LogManager.getLogger(MethodLocal.class);
    private final SootMethod sootMethod;

    private final HashSet<Value> InterestingVariables = new HashSet<>();
    private final HashMap<Value, AbstractClz> LocalToClz = new HashMap<>();
    private final HashMap<Value, List<String>> LocalToString = new HashMap<>();
    private final HashMap<Value, Integer> LocalFromParams = new HashMap<>();

    private final HashMap<Value, Integer> ArrayRefLocal = new HashMap<>();// ArrayValue and Size

    private ValueContext ParentValueContext;
    private HashMap<Integer, List<String>> ParamsToString = new HashMap<>();

    private final List<String> returnValues = new ArrayList<>();

    private final HashMap<String, List<Integer>> InterestingInvoke = new HashMap<>();
    private final HashMap<String, HashMap<Integer, List<String>> > InterestingParamString = new HashMap<>();

    //private List<String> unsolvedLocals = new ArrayList<>();

    public MethodLocal(SootMethod Method){
        this.sootMethod = Method;
    }

    //forward new
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
                        ParamValue.put(index, new ArrayList<>(List.of(param.toString())));
                    }
                    index++;
                }
                if (!ParamValue.isEmpty()) ParamsToString = ParamValue;
            }
        }
    }

    //backward new
    public MethodLocal(SootMethod Method, HashMap<String, List<Integer>> IntereInvoke){
        this(Method);
        this.InterestingInvoke.putAll(IntereInvoke);
    }

    public void doAnalysis(){
        Body body = null;
        try{
            body = this.sootMethod.retrieveActiveBody();
        }
        catch(Exception e){
            LOGGER.error(e);
        }
        if(body == null) return;
        for(Unit unit : body.getUnits()){
            Stmt stmt = (Stmt) unit;
            if(stmt instanceof AssignStmt){
                caseAssignStmt((AssignStmt) stmt);
            }
            else if (stmt instanceof IdentityStmt){
                caseIdentityStmt((IdentityStmt) stmt);
            }
            else if(stmt instanceof InvokeStmt){
                caseInvokeStmt((InvokeStmt) stmt);
            }
            else if(stmt instanceof ReturnStmt){
                caseReturnStmt((ReturnStmt) stmt);
            }
        }
    }

    private void caseReturnStmt(ReturnStmt stmt) {
        Value value = stmt.getOp();
        if(LocalToString.containsKey(value)) addValue(returnValues,LocalToString.get(value));
        else if(value instanceof Constant) returnValues.add(value.toString());
        //TODO localTOClz
    }

    public void caseAssignStmt(AssignStmt stmt) {
        Value leftOperation = stmt.getLeftOp();
        Value rightOperation = stmt.getRightOp();
        HashSet<?> result = null;
        boolean arrayRef = false;
        int arrayIndex = -1;
        Value arrayBase = null;
        List<String> arrayString = null;
        if (leftOperation instanceof Local || leftOperation instanceof ArrayRef) {
            if(leftOperation instanceof ArrayRef) {
                arrayRef = true;
                ArrayRef array = (ArrayRef) leftOperation;
                arrayBase = array.getBase();
                Integer index = (Integer) SimulateUtil.getConstant(array.getIndex());
                if(index!=null) arrayIndex = index;
                if(ArrayRefLocal.containsKey(arrayBase)) arrayString = LocalToString.get(arrayBase);
                //TODO List<List<String>>
                else arrayString = new ArrayList<>();
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
                    addLocalTOString(leftOperation, MethodString.getMethodToString().get(invokeMethod));
                }
                else{
                    if(MethodString.classMaybeCache.containsKey(invokeMethod.getDeclaringClass())){
                        HashMap<String,List<String>> cache = MethodString.classMaybeCache.get(invokeMethod.getDeclaringClass());
                        if(instanceInvokeExpr.getArgs().size() ==1 && instanceInvokeExpr.getArg(0) instanceof Constant){
                            Object obj = SimulateUtil.getConstant(instanceInvokeExpr.getArg(0));
                            if(obj!=null) {
                                String stringKey = obj.toString();
                                if (cache.containsKey(stringKey)) {
                                    addLocalTOString(leftOperation, cache.get(stringKey));
                                    LOGGER.warn("[FIND CACHE VALUE] : {} - > {}", leftOperation, cache.get(stringKey));
                                }
                            }
                        }
                    }
                    //TODO Invoke Return. client, retrofit, encryption.
//                    MethodLocal nextMethod = new MethodLocal(invokeMethod, valueContext);
//                    nextMethod.doAnalysis();
//                    List<String> ret = nextMethod.getReturnValues();
//                    if(!ret.isEmpty()) {
//                        addLocalTOString(leftOperation, ret);
//                        InterestingTransfer = true;
//                    }
                }

                if(InterestingTransfer)
                    InterestingVariables.add(leftOperation);

            } else if (rightOperation instanceof NewExpr) {
                //Todo NewExpr params.
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
                //TODO retrofit, okhttp, other.

            } else if( rightOperation instanceof StaticInvokeExpr){
                //TODO process normal method, like toJson, toString..; also the encrypt method.
                InvokeExpr invokeExpr = (InvokeExpr) rightOperation;
                HashMap<Value, List<String>> valueString = getInvokeExprValues(invokeExpr);
                ValueContext valuecontext = new ValueContext(this.sootMethod, stmt, valueString);
                if(SimulateUtil.hasSimClass(invokeExpr.getMethod())){
                    String simResult = SimulateUtil.getSimClassValue(valuecontext);
                    if(!simResult.isEmpty()) addLocalTOString(leftOperation, new ArrayList<>(List.of(simResult)));
                }
                else{
                    //TODO localFromParam Transfer.
                }
                //addChildValueContext(valuecontext);
                //MethodLocal nextMethodLocal = new MethodLocal(invokeExpr.getMethod(), valuecontext);
            }
            else if (rightOperation instanceof ArrayRef){

            }
            else if (rightOperation instanceof FieldRef) {
                SootField field = ((FieldRef) rightOperation).getField();
                if(rightOperation instanceof InstanceFieldRef){
                    Value base = ((InstanceFieldRef) rightOperation).getBase();
                    //TODO field TO Client.
                    if(LocalToClz.containsKey(base)){

                    }
                    else{

                    }
                }
                else{
                    if(MethodString.getFieldToString().containsKey(field.toString())){
                        List<String> fieldStr = MethodString.getFieldToString().get(field.toString());
                        if(arrayRef){
                            arrayString.set(arrayIndex, fieldStr.get(0));
                        }
                        else{
                            addLocalTOString(leftOperation, fieldStr);
                        }
                    }
                }
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
            else if (rightOperation instanceof Local){
                if(arrayRef) leftOperation = arrayBase;
                if(LocalFromParams.containsKey(rightOperation)){
                    LocalFromParams.put(leftOperation, LocalFromParams.get(rightOperation));
                }
                if(LocalToClz.containsKey(rightOperation)){
                    LocalToClz.put(leftOperation, LocalToClz.get(rightOperation));
                }
                else if(ArrayRefLocal.containsKey(rightOperation)){
                    ArrayRefLocal.put(leftOperation, ArrayRefLocal.get(rightOperation));
                    addLocalTOString(leftOperation, new ArrayList<>(LocalToString.get(rightOperation)));
                }
                else if(LocalToString.containsKey(rightOperation)){
                    if(arrayRef) arrayString.set(arrayIndex, LocalToString.get(rightOperation).get(0));
                    else{
                        addLocalTOString(leftOperation, LocalToString.get(rightOperation));
                    }
                    this.InterestingVariables.add(leftOperation);
                }
            }
            else if(rightOperation instanceof CastExpr) {
                if(arrayRef) leftOperation = arrayBase;
                Value localOp =((CastExpr) rightOperation).getOp();
                if(LocalToString.containsKey(localOp)){
                    if(arrayRef){
                        arrayString.set(arrayIndex, LocalToString.get(localOp).get(0));
                    }
                    else{
                        addLocalTOString(leftOperation, LocalToString.get(localOp));
                    }
                }
                //TODO cast type is retrofit class.
            }
            else if(rightOperation instanceof Constant){
                Object constValue = SimulateUtil.getConstant(rightOperation);
                if(arrayRef && arrayBase !=null){
                    Integer Len = 0;
                    if(ArrayRefLocal.containsKey(arrayBase)) Len = ArrayRefLocal.get(arrayBase);
                    if(arrayIndex >=0 && arrayIndex < Len){
                        if(constValue != null && arrayString != null)
                            arrayString.set(arrayIndex, constValue.toString());
                    }
                }
                else{
                    if(constValue != null)
                        addLocalTOString(leftOperation, new ArrayList<>(List.of(constValue.toString())));
                }
            }
        } else {
            LOGGER.warn(String.format("[%s] [SIMULATE][left unknown]: %s (%s)", this.hashCode(), stmt, leftOperation.getClass()));
        }

    }

    public void caseInvokeStmt(InvokeStmt stmt) {
        String signature = stmt.getInvokeExpr().getMethod().toString();
        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        Value base;
        if (invokeExpr instanceof InstanceInvokeExpr) {
            base = ((InstanceInvokeExpr) invokeExpr).getBase();
            HashMap<Value, List<String>> valueString = getInvokeExprValues(invokeExpr);
            ValueContext valueContext = new ValueContext(this.sootMethod, stmt, valueString);
            if(LocalToClz.containsKey(base)) LocalToClz.get(base).addValueContexts(valueContext);
        }
        else if( invokeExpr instanceof StaticInvokeExpr){
            //TODO process normal method, like toJson, toString..; also the encrypt method.
            HashMap<Value, List<String>> valueString = getInvokeExprValues(invokeExpr);
            ValueContext valuecontext = new ValueContext(this.sootMethod, stmt, valueString);
            //TODO localFromParam Transfer.

            //addChildValueContext(valuecontext);
            //MethodLocal nextMethodLocal = new MethodLocal(invokeExpr.getMethod(), valuecontext);
        }
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
                else
                    //if (MethodString.isCommonType(ref.getType()))
                {
                    addLocalTOString(leftOperation, new ArrayList<>(List.of("@Param", ref.getType().toString())));
                    LocalFromParams.put(leftOperation, ref.getIndex());
                    InterestingVariables.add(leftOperation);
                }
            }
        }
    }


    private void addLocalTOString(Value local, List<String> string){
        if(ArrayRefLocal.containsKey(local)){
            addValue(LocalToString.get(local),string);
        }
        else{
            List<String> LocalStr = new ArrayList<>();
            if(LocalToString.containsKey(local)) LocalStr = LocalToString.get(local);
            if(LocalStr.isEmpty())
                LocalToString.put(local, string);
            else if(LocalStr.get(0).length() < string.get(0).length())
                LocalToString.put(local, string);
            else{
                //LOGGER.info("341: List: {}", string);
                addValue(LocalToString.get(local),string);
            }
        }
    }

    public HashMap<Value, List<String>> getInvokeExprValues(InvokeExpr invokeExpr){
        HashMap<Value, List<String>> valueString = new HashMap<>();
        HashMap<Integer, List<String>> interestingParamString = new HashMap<>();
        String invokeSIg = invokeExpr.getMethod().getSignature();
        LOGGER.warn("[SIMULATE]: Get Values for Invoke: {}",invokeSIg);
        List<Integer> interestingParam = new ArrayList<>();

        //HashMap<String, List<Integer>> intereInvoke = new HashMap<>();

        boolean isInterestingInvoke = false;
        if(getInterestingInvoke().containsKey(invokeSIg)){
            isInterestingInvoke = true;
            interestingParam = getInterestingInvoke().get(invokeSIg);
            LOGGER.warn("[SIMULATE]: Find Interesting Invoke sig: {}=>{}", invokeSIg, interestingParam);
        }
        int i = 0;
        HashMap<String, List<Integer>> nextInvokeParam = new HashMap<>();
        nextInvokeParam.put(sootMethod.getSignature(), new ArrayList<>());
        for(Value value: invokeExpr.getArgs()){
            if(value instanceof Constant) {
                Object ob = SimulateUtil.getConstant(value);
                if(ob!= null) {
                    if (isInterestingInvoke && interestingParam.contains(i)) {
                        LOGGER.warn("[SIMULATE]: Find Interesting Constant invoke value: {}=>{}", i, ob.toString());
                        if(!interestingParamString.containsKey(i))
                            interestingParamString.put(i,new ArrayList<>());
                        addValue(interestingParamString,i,ob.toString());
                    }
                }
            }
            else if(value instanceof Local) {
                if(LocalToClz.containsKey(value)){
                    String Clzstring = LocalToClz.get(value).toString();
                    addLocalTOString(value, new ArrayList<>(List.of(Clzstring)));
                }
                if(LocalToString.containsKey(value)) {
                    valueString.put(value, LocalToString.get(value));
                    //LOGGER.warn("[SIMULATE]: Find Local Value for Invoke: {}", LocalToString.get(value));

                    List<String> paramValue = new ArrayList<>();
                    if(!LocalToString.get(value).isEmpty() && LocalToString.get(value).get(0).equals("@Param")){
                        //LOGGER.warn("[SIMULATE]: Find Local Value From Param: {}", LocalToString.get(value));

                        //TODO delete @Param;
                        HashSet<CallGraphNode> callByNodes = CallGraph.getNode(sootMethod.toString()).getCallBy();
                        if(!nextInvokeParam.get(sootMethod.getSignature()).contains(LocalFromParams.get(value))){
                            nextInvokeParam.get(sootMethod.getSignature()).add(LocalFromParams.get(value));
                            for(CallGraphNode node : callByNodes){
                                //LOGGER.warn("[SIMULATE]: Get Call By Nodes, {}==>{}", node.getSootMethod(), nextInvokeParam);
                                MethodLocal newMethodLocal = new MethodLocal(node.getSootMethod(), nextInvokeParam);
                                newMethodLocal.doAnalysis();
                                //TODO put all intere param, then do methodlocal.
                                if(newMethodLocal.getInterestingParamString().containsKey(sootMethod.getSignature())) {
                                    if (newMethodLocal.getInterestingParamString().get(sootMethod.getSignature()).containsKey(LocalFromParams.get(value))) {
                                        addValue(paramValue, newMethodLocal.getInterestingParamString().get(sootMethod.getSignature()).get(LocalFromParams.get(value)));
                                        valueString.put(value, paramValue);
                                    }
                                }
                            }
                        }
                    }
                    else {
                        LOGGER.warn("[SIMULATE]: Find Interesting Solved Local Value: {}", LocalToString.get(value));
                        addValue(paramValue,LocalToString.get(value));
                        valueString.put(value, paramValue);
                    }

                    if (isInterestingInvoke && interestingParam.contains(i)) {
                        LOGGER.warn("[SIMULATE]: Find Interesting Local invoke value: {}=>{}", i, paramValue.toString());
                        if(!interestingParamString.containsKey(i))
                            interestingParamString.put(i,new ArrayList<>());
                        addValue(interestingParamString,i,paramValue);
                    }
                }
            }
            i++;
        }
        if(isInterestingInvoke) {
            if (!getInterestingParamString().containsKey(invokeSIg))
                getInterestingParamString().put(invokeSIg, new HashMap<>());
            if (!interestingParamString.isEmpty()) {
                getInterestingParamString().get(invokeSIg).putAll(interestingParamString);
            }
        }
        return valueString;//TODO valueString add
    }

    public static List<String> getStaticInvokeReturn(InvokeExpr invokeExpr, HashMap<Integer, List<String>> paramValues){
        SootMethod sootMethod = invokeExpr.getMethod();
        String sig = sootMethod.getSignature();
        SootClass sootClass = sootMethod.getDeclaringClass();
        List<String> result = new ArrayList<>();
        String value0 = "";
        if(paramValues.containsKey(0))
            value0 = paramValues.get(0).toString();
        else if(invokeExpr.getArg(0) instanceof Constant) {
            Object obj = SimulateUtil.getConstant(invokeExpr.getArg(0));
            if(obj != null)
                value0 = obj.toString();
        }
        else return result;

        if(MethodString.classMaybeCache.containsKey(sootClass)){
            HashMap<String, List<String>> cache = MethodString.classMaybeCache.get(sootClass);
            if(cache.containsKey(value0))
                result.addAll(cache.get(value0));
            else{
                String str = sootMethod.getSignature() + '(' + value0 + ')';
                result.add(str);
            }
            return result;
        } else if(sig.contains("kotlin.TuplesKt: kotlin.Pair 'to'") || sig.contains("stringPlus")){
            String value1 = "";
            if(paramValues.containsKey(1))
                value1 = paramValues.get(1).toString();
            else if(invokeExpr.getArg(1) instanceof Constant) {
                Object obj = SimulateUtil.getConstant(invokeExpr.getArg(0));
                if (obj != null)
                    value1 = obj.toString();
            }
            else return result;

            if(sig.contains("kotlin.TuplesKt: kotlin.Pair 'to'"))
                result.add(value0 + '=' + value1);
            else if(sig.contains("stringPlus"))
                result.add(value0 + value1);
            return result;
        } else if (sig.contains("listOf") || sig.contains("valueOf") || sig.contains("boxing")) {
            result.add(value0);
            return result;
        } else if( sig.contains("currentTimeMillis")){
            result.add("currentTimeMills: " + System.currentTimeMillis());
            return result;
        } else{
            //TODO try to do methodlocal to get return.

        }

        return result;
    }

    public static <K, V> void addValue(Map<K, List<V>> map, K key, V value) {
        map.computeIfAbsent(key, k -> new ArrayList<>());
        List<V> values = map.get(key);
        if(!values.contains(value)){
            values.add(value);
        }
    }

    public static <V> void addValue(List<V> list,  V value) {
        if(!list.contains(value)){
            list.add(value);
        }
    }

    public static <V> void addValue(List<V> list1, List<V> list2){
        for(V value: list2){
            addValue(list1, value);
        }
    }

    public static <K, V> void addValue(Map<K, List<V>> map, K key, List<V> values) {
        map.computeIfAbsent(key, k -> new ArrayList<>());
        List<V> key_values = map.get(key);
        for(V value: values) {
            if (!key_values.contains(value)) {
                key_values.add(value);
            }
        }
    }

    public boolean isCommonClz(String className){
        if(className.contains("Map") || className.contains("json"))
            return true;
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

    public HashMap<String, HashMap<Integer, List<String>> > getInterestingParamString() {
        return InterestingParamString;
    }

    public HashMap<String, List<Integer>> getInterestingInvoke() {
        return InterestingInvoke;
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
