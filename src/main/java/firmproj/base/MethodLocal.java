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


    private final HashMap<Value, AbstractClz> LocalToClz = new HashMap<>();
    private final HashMap<Value, List<String>> LocalToString = new HashMap<>();
    private final HashMap<Value, List<Integer>> LocalFromParams = new HashMap<>();

    private final HashMap<Value, MethodParamInvoke> localFromParamInvoke = new HashMap<>();
    private final HashMap<Value, SootField> localToFieldMap = new HashMap<>();
    private final HashMap<Value, List<List<String>>> localArray = new HashMap<>();

    private ValueContext ParentValueContext;
    private HashMap<Integer, List<String>> ParamsToString = new HashMap<>();

    private final List<String> returnValues = new ArrayList<>();

    private final HashMap<String, List<Integer>> InterestingInvoke = new HashMap<>();
    private final HashMap<String, HashMap<Integer, List<String>> > InterestingParamString = new HashMap<>();
    private Integer invokeStep = 0;
    private final Integer MAX_STEP = 4;

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
                        Object obj = SimulateUtil.getConstant(param);
                        if(obj != null) {
                            ParamValue.put(index, new ArrayList<>(List.of(param.toString())));
                        }
                    }
                    index++;
                }
                if (!ParamValue.isEmpty()) ParamsToString = ParamValue;
            }
        }
    }

    //backward new
    public MethodLocal(SootMethod Method, HashMap<String, List<Integer>> IntereInvoke, int step){
        this(Method);
        this.InterestingInvoke.putAll(IntereInvoke);
        invokeStep = step + 1;

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
        if(invokeStep > MAX_STEP) return;
        if(MethodString.isStandardLibraryClass(sootMethod.getDeclaringClass())) return;
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
        else if(value instanceof Constant) {
            Object obj = SimulateUtil.getConstant(value);
            if(obj != null)
                returnValues.add(value.toString());
        }
        //TODO localTOClz
    }

    public void caseAssignStmt(AssignStmt stmt) {
        Value leftOp = stmt.getLeftOp();
        Value rightOp = stmt.getRightOp();
        HashSet<?> result = null;
        boolean arrayRef = false;
        int arrayIndex = -1;
        Value arrayBase = null;

        if (leftOp instanceof Local) {
            if (rightOp instanceof NewExpr) {
                //Todo NewExpr params.
                NewExpr newExpr = (NewExpr) rightOp;
                String className = newExpr.getClass().getName();
                if(isCommonClz(className)){
                    AbstractClz NewClz = CreateCommonClz(newExpr.getBaseType().getSootClass(), this.sootMethod);
                    LocalToClz.put(leftOp, NewClz);
                }
                else{
                    AbstractClz NewClz = new CustomClz(newExpr.getBaseType().getSootClass(), this.sootMethod);
                    LocalToClz.put(leftOp, NewClz);
                }
                //TODO retrofit, okhttp, other.

            }
            else if (rightOp instanceof InstanceInvokeExpr) {
                boolean InterestingTransfer = false;
                InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) rightOp;
                Value base = instanceInvokeExpr.getBase();
                SootMethod invokeMethod = instanceInvokeExpr.getMethod();

                HashMap<Value, List<String>> valueString = getInvokeExprValues(instanceInvokeExpr);
                ValueContext valueContext = new ValueContext(this.sootMethod,stmt,valueString);


                if(LocalToClz.containsKey(base)){
                    AbstractClz clz = LocalToClz.get(base);
                    clz.addValueContexts(valueContext);
                }

                if(MethodString.getMethodToString().containsKey(invokeMethod)) {
                    InterestingTransfer = true;
                    addLocalTOString(leftOp, MethodString.getMethodToString().get(invokeMethod));
                }
                else{
                    if(MethodString.classMaybeCache.containsKey(invokeMethod.getDeclaringClass())){
                        HashMap<String,List<String>> cache = MethodString.classMaybeCache.get(invokeMethod.getDeclaringClass());
                        if(instanceInvokeExpr.getArgs().size() ==1 && instanceInvokeExpr.getArg(0) instanceof Constant){
                            Object obj = SimulateUtil.getConstant(instanceInvokeExpr.getArg(0));
                            if(obj!=null) {
                                String stringKey = obj.toString();
                                if (cache.containsKey(stringKey)) {
                                    addLocalTOString(leftOp, cache.get(stringKey));
                                    LOGGER.warn("[FIND CACHE VALUE] : {} - > {}", leftOp, cache.get(stringKey));
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

            }  else if( rightOp instanceof StaticInvokeExpr){
                //TODO process normal method, like toJson, toString..; also the encrypt method.
                InvokeExpr invokeExpr = (InvokeExpr) rightOp;
                HashMap<Value, List<String>> valueString = getInvokeExprValues(invokeExpr);
                ValueContext valuecontext = new ValueContext(this.sootMethod, stmt, valueString);
                if(SimulateUtil.hasSimClass(invokeExpr.getMethod())){
                    String simResult = SimulateUtil.getSimClassValue(valuecontext);
                    if(!simResult.isEmpty()) addLocalTOString(leftOp, new ArrayList<>(List.of(simResult)));
                }
                else{
                    //TODO localFromParam Transfer.
                }
                //addChildValueContext(valuecontext);
                //MethodLocal nextMethodLocal = new MethodLocal(invokeExpr.getMethod(), valuecontext);
            }
            else if (rightOp instanceof ArrayRef){

            }
            else if (rightOp instanceof FieldRef) {
                SootField field = ((FieldRef) rightOp).getField();
                if(rightOp instanceof InstanceFieldRef){
                    Value base = ((InstanceFieldRef) rightOp).getBase();
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

                        }
                        else{
                            addLocalTOString(leftOp, fieldStr);
                        }
                    }
                }
            }
            else if (rightOp instanceof NewArrayExpr) {
                NewArrayExpr newArray = (NewArrayExpr)rightOp;
                Integer index = (Integer) SimulateUtil.getConstant(newArray.getSize());
                if(index==null) index = 0;
                List<String> arrayList = new ArrayList<>();
                while(arrayList.size()< index){
                    arrayList.add("");
                }
                if(!LocalToString.containsKey(leftOp))LocalToString.put(leftOp, arrayList);

            }
            else if (rightOp instanceof BinopExpr) {

            }
            else if (rightOp instanceof Local){
                if(arrayRef) leftOp = arrayBase;
                if(LocalFromParams.containsKey(rightOp)){
                    LocalFromParams.put(leftOp, LocalFromParams.get(rightOp));
                }
                if(LocalToClz.containsKey(rightOp)){
                    LocalToClz.put(leftOp, LocalToClz.get(rightOp));
                }
                else if(LocalToString.containsKey(rightOp)){
                    if(arrayRef) {

                    }
                    else{
                        addLocalTOString(leftOp, LocalToString.get(rightOp));
                    }
                }
            }
            else if(rightOp instanceof CastExpr) {
                if(arrayRef) leftOp = arrayBase;
                Value localOp =((CastExpr) rightOp).getOp();
                if(LocalToString.containsKey(localOp)){
                    if(arrayRef){

                    }
                    else{
                        addLocalTOString(leftOp, LocalToString.get(localOp));
                    }
                }
                //TODO cast type is retrofit class.
            }
            else if(rightOp instanceof Constant){
                Object constValue = SimulateUtil.getConstant(rightOp);
                if(arrayRef && arrayBase !=null){
                    Integer Len = 0;

                }
                else{
                    if(constValue != null)
                        addLocalTOString(leftOp, new ArrayList<>(List.of(constValue.toString())));
                }
            }
        } else {
            LOGGER.warn(String.format("[%s] [SIMULATE][left unknown]: %s (%s)", this.hashCode(), stmt, leftOp.getClass()));
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
        if (leftOperation instanceof Local || leftOperation instanceof ArrayRef) {
            if (rightOperation instanceof ParameterRef) {
                ParameterRef ref = (ParameterRef) rightOperation;
                if(ParamsToString.containsKey(ref.getIndex())){
                    List<String> paramStr = ParamsToString.get(ref.getIndex());
                    addLocalTOString(leftOperation, paramStr);
                }
                else
                {
                    addValue(LocalFromParams,leftOperation, ref.getIndex());
                }
            }
        }
    }


    private void addLocalTOString(Value local, List<String> string){
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

    public HashMap<Value, List<String>> getInvokeExprValues(InvokeExpr invokeExpr){
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
        HashMap<Integer, List<Integer>> invokeArgToMethodArg = new HashMap<>();
        HashMap<Value, List<String>> paramValue = new HashMap<>();
        for(Value value: invokeExpr.getArgs()){
            if(value instanceof Constant) {
                Object ob = SimulateUtil.getConstant(value);
                if (isInterestingInvoke && interestingParam.contains(i)) {
                    if(ob!= null) {
                    LOGGER.warn("[SIMULATE]: Find Interesting Constant invoke value: {}=>{}", i, ob.toString());
                    addValue(interestingParamString, i,ob.toString());
                    }
                    else{
                        addValue(interestingParamString, i, "null");
                    }
                }
            }
            else if(value instanceof Local) {
                if(LocalToClz.containsKey(value)){
                    LocalToClz.get(value).solve();
                    String Clzstring = LocalToClz.get(value).toString();
                    if(LocalFromParams.containsKey(value)) {
                        localFromParamInvoke.put(value, new MethodParamInvoke(sootMethod, LocalFromParams.get(value), Clzstring));
                        addValue(nextInvokeParam, sootMethod.getSignature(), LocalFromParams.get(value));
                        invokeArgToMethodArg.put(i, LocalFromParams.get(value));
                    }
                    else{
                        addValue(paramValue, value, Clzstring);
                        if (isInterestingInvoke && interestingParam.contains(i)) {
                            //LOGGER.warn("[SIMULATE]: Find Interesting Local invoke value: {}=>{}", i, paramValue.toString());
                            addValue(interestingParamString, i, paramValue.get(value));
                        }
                    }
                }
                //LOGGER.warn("[SIMULATE]: Find Local Value for Invoke: {}", LocalToString.get(value));
                else if(LocalFromParams.containsKey(value)){
                    //LOGGER.warn("[SIMULATE]: Find Local Value From Param: {}", LocalToString.get(value));
                    addValue(nextInvokeParam, sootMethod.getSignature(), LocalFromParams.get(value));
                    invokeArgToMethodArg.put(i, LocalFromParams.get(value));
                }
                else if(localFromParamInvoke.containsKey(value)){
                    List<Integer> params = localFromParamInvoke.get(value).param;
                    addValue(nextInvokeParam, sootMethod.getSignature(), params);
                    invokeArgToMethodArg.put(i, params);
                }
                else if(LocalToString.containsKey(value)){
                    LOGGER.warn("[SIMULATE]: Find Interesting Solved Local Value: {}", LocalToString.get(value));
                    addValue(paramValue, value, LocalToString.get(value));
                    if (isInterestingInvoke && interestingParam.contains(i)) {
                        LOGGER.warn("[SIMULATE]: Find Interesting Local invoke value: {}=>{}", i, paramValue.toString());
                        addValue(interestingParamString, i, paramValue.get(value));
                    }
                }
                else{
                    addValue(paramValue, value, "UNKNOWN FROM " + invokeSIg + "($" + i + ")");
                    if (isInterestingInvoke && interestingParam.contains(i)) {
                        LOGGER.warn("[SIMULATE]: CANT FIND invoke value: {}=>{}, {}", i, value, invokeExpr);
                        addValue(interestingParamString, i, paramValue.get(value));
                    }
                }
            }
            i++;
        }
        if(!nextInvokeParam.isEmpty()) {
            HashSet<CallGraphNode> callByNodes = CallGraph.getNode(sootMethod.toString()).getCallBy();
            if(callByNodes.isEmpty()){
                for(int p : nextInvokeParam.get(sootMethod.getSignature())) {
                    addValue(paramValue, invokeExpr.getArg(p), "UNKNOWN FROM "+ sootMethod.getSubSignature() + "($" + p + ")");
                }
            }
            else {
                for (CallGraphNode node : callByNodes) {
                    //LOGGER.warn("[SIMULATE]: Get Call By Nodes, {}==>{}", node.getSootMethod(), nextInvokeParam);
                    MethodLocal newMethodLocal = new MethodLocal(node.getSootMethod(), nextInvokeParam, this.invokeStep);
                    newMethodLocal.doAnalysis();
                    if (newMethodLocal.getInterestingParamString().containsKey(sootMethod.getSignature())) {
                        HashMap<Integer, List<String>> invokeResult = newMethodLocal.getInterestingParamString().get(sootMethod.getSignature());
                        for (Integer index : invokeResult.keySet()) {
                            for(MethodParamInvoke methodParamInvoke : localFromParamInvoke.values()) { //update the method param value of methodParamInvoke
                                if (methodParamInvoke.param.contains(index)) {
                                    methodParamInvoke.paramValue.put(index, invokeResult.get(index));
                                }
                            }
                            for(Integer invokeArg : getInvokeArgFromMethodArg(invokeArgToMethodArg, index)) {
                                Value value = invokeExpr.getArg(invokeArg);
                                if(localFromParamInvoke.containsKey(value))
                                    addValue(paramValue, value, localFromParamInvoke.get(value).toString()); //if param value is methodParamInvoke
                                else {
                                    addValue(paramValue, value, invokeResult.get(index));   //if param value if just local or other.
                                }
                                if (isInterestingInvoke && interestingParam.contains(invokeArg)) {
                                    LOGGER.warn("[SIMULATE]: Find Interesting From invoke: {}=>{}", i, paramValue.toString());
                                    addValue(interestingParamString, invokeArg, paramValue.get(value));
                                }
                            }
                        }
                    }
                }
            }
        }

        if(isInterestingInvoke && !interestingParamString.isEmpty()) {
            if (!getInterestingParamString().containsKey(invokeSIg))
                getInterestingParamString().put(invokeSIg, new HashMap<>());
            getInterestingParamString().get(invokeSIg).putAll(interestingParamString);

        }
        return paramValue;//TODO valueString add
    }

    public List<Integer> getInvokeArgFromMethodArg(HashMap<Integer, List<Integer>> invokeArgToMethodArg, Integer methodArg){
        List<Integer> result = new ArrayList<>();
        for(Integer key : invokeArgToMethodArg.keySet()){
            List<Integer> value = invokeArgToMethodArg.get(key);
            if(value.contains(methodArg))
                addValue(result, methodArg);
        }
        return result;
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

    public static boolean isCommonClz(String className){
        if(className.contains("Map") || className.contains("json") || className.contains("java.lang.StringBuilder") || className.contains("java.lang.StringBuffer") || className.contains("java.net.URL"))
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

    public static AbstractClz CreateCommonClz(SootClass clz, SootMethod currentMethod){
        if(clz.getName().contains("Map"))
            return new HashmapClz(clz, currentMethod);
        else if(clz.getName().contains("StringBuilder") || clz.getName().contains("StringBuffer"))
            return new StringClz(clz, currentMethod);
        else if (clz.getName().contains("java.net.URL")) {
            return new UrlClz(clz, currentMethod);
        } else
            return new CustomClz(clz, currentMethod);
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

    public HashMap<Value, List<Integer>> getLocalFromParams() {
        return LocalFromParams;
    }

//    public List<String> getUnsolvedLocals() {
//        return unsolvedLocals;
//    }
}
