package firmproj.client;

import firmproj.base.MethodLocal;
import firmproj.base.MethodParamInvoke;
import firmproj.base.MethodString;
import firmproj.base.ValueContext;
import firmproj.objectSim.AbstractClz;
import firmproj.objectSim.SimulateUtil;
import firmproj.objectSim.UrlClz;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.*;

public class HttpClientFind {
    public static final HashMap<String, List<AbstractHttpClient>> findResult = new HashMap<>();

    private static final HashMap<String, Interceptor> allInterceptorClasses = new HashMap<>();

    private static final Logger LOGGER = LogManager.getLogger(HttpClientFind.class);

    public static final HashMap<SootField, List<AbstractHttpClient>> FieldToClientPoint = new HashMap<>();

    private static final HashSet<String> visitedMethod = new HashSet<>();

    public static void findAllHttpClientBuildMethod(){
        Chain<SootClass> classes = Scene.v().getClasses();
        for (SootClass sootClass : classes) {
            String headString = sootClass.getName().split("\\.")[0];
            if (headString.contains("android") || headString.contains("kotlin") || headString.contains("java") || headString.contains("thingClips"))
                continue;
            for (SootMethod sootMethod : clone(sootClass.getMethods())) {
                if (!sootMethod.isConcrete())
                    continue;
                findHttpClientBuildMethod(sootMethod);
            }
        }
        LOGGER.info("All HttpClient Result: {}",findResult.toString());
        //LOGGER.info("ALL Field with retrofitBuildPoint: {}", );
    }

    public static void findAllInterceptorClasses(){
        Chain<SootClass> classes = Scene.v().getClasses();
        for (SootClass sootClass : classes) {
            String headString = sootClass.getName().split("\\.")[0];
            String INTERCEPTOR = "okhttp3.Interceptor";
            if (headString.contains("android") || headString.contains("kotlin") || headString.contains("java") || headString.contains("thingClips"))
                continue;
            Chain<SootClass> interfaceCls = sootClass.getInterfaces();
            for(SootClass clz : interfaceCls){
                if(clz.getName().equals(INTERCEPTOR)) {
                    Interceptor interceptor = new Interceptor(sootClass);
                    interceptor.init();
                    allInterceptorClasses.put(sootClass.getName(), interceptor);
                    break;
                    //TODO start with okhttp3.* interceptor.
                }
            }
        }
        LOGGER.info("All Interceptor Classes Result : {}",allInterceptorClasses.toString());
        //LOGGER.info("ALL Field with retrofitBuildPoint: {}", );
    }

    public static List<AbstractHttpClient> findHttpClientBuildMethod(SootMethod sootMethod){
        String methodSig = sootMethod.getSignature();
        if(findResult.containsKey(methodSig)) return findResult.get(methodSig);
        if(methodSig.contains("java.lang.String HttpPost(java.lang.String"))
            LOGGER.info("got");

        List<AbstractHttpClient> result = new ArrayList<>();

        if(!sootMethod.isConcrete() || !sootMethod.getDeclaringClass().isApplicationClass() || MethodString.isStandardLibraryClass(sootMethod.getDeclaringClass())) return result;
        Body body = null;
        try {
            body = sootMethod.retrieveActiveBody();
        } catch (Exception e) {
            //LOGGER.error("Could not retrieved the active body {} because {}", sootMethod, e.getLocalizedMessage());
        }
        if (body == null)
            return result;
        Type ret = sootMethod.getReturnType();

        if(visitedMethod.contains(methodSig)) return result;

        HashMap<Value, List<AbstractHttpClient>> localToPoint = new HashMap<>();
        HashMap<Value, List<String>> localToString = new HashMap<>();
        HashMap<Value, Interceptor> localToInterceptor = new HashMap<>();
        HashMap<Value, HashMap<String, List<String>>> localToRequestBuilder = new HashMap<>();
        HashMap<String, HashMap<Integer, List<String>>> currentValues = new HashMap<>();
        HashMap<Value, List<Integer>> localFromParam = new HashMap<>();
        HashMap<Value, MethodParamInvoke> localFromParamInvoke = new HashMap<>();
        HashMap<Value, AbstractClz> localToClz = new HashMap<>();

        visitedMethod.add(methodSig);
        for (Unit unit : body.getUnits()) {
            Stmt stmt = (Stmt) unit;
            try {
                if (stmt instanceof IdentityStmt) {
                    IdentityStmt identityStmt = (IdentityStmt) stmt;
                    Value leftOperation = identityStmt.getLeftOp();
                    Value rightOperation = identityStmt.getRightOp();
                    if (leftOperation instanceof Local) {
                        if (rightOperation instanceof ParameterRef) {
                            ParameterRef ref = (ParameterRef) rightOperation;
                            Integer index = ref.getIndex();
                            addValue(localFromParam, leftOperation, index);
                        }
                    }
                } else if (stmt instanceof AssignStmt) {
                    Value leftOp = ((AssignStmt) stmt).getLeftOp();
                    Value rightOp = ((AssignStmt) stmt).getRightOp();
                    if (rightOp instanceof NewExpr) {
                        NewExpr newExpr = (NewExpr) rightOp;
                        String clsName = newExpr.getBaseType().getClassName();
                        SootClass cls = newExpr.getBaseType().getSootClass();
                        if (clsName.equals("okhttp3.OkHttpClient$Builder")) {
                            AbstractHttpClient point = new okHttpClient(sootMethod, unit);
                            addValue(localToPoint, leftOp, point);
                            result.add(point);
                            tryToAddResult(sootMethod, point);
                            LOGGER.info("94: Method: {} , new Client: {}", methodSig, point.toString());
                        } else if (clsName.equals("okhttp3.Request$Builder")) {
                            localToRequestBuilder.put(leftOp, new HashMap<>());
                        } else if (allInterceptorClasses.containsKey(clsName)) {
                            localToInterceptor.put(leftOp, allInterceptorClasses.get(clsName));
                        } else if(clsName.contains("java.net.URL")){
                            UrlClz urlClz = new UrlClz(cls, sootMethod);
                            localToClz.put(leftOp, urlClz);
                        } else if(MethodLocal.isCommonClz(clsName)){
                            AbstractClz abstractClz = MethodLocal.CreateCommonClz(cls, sootMethod);
                            localToClz.put(leftOp, abstractClz);
                        }
                    }
                    if (rightOp instanceof InvokeExpr) {
                        SootMethod invokeMethod = ((InvokeExpr) rightOp).getMethod();
                        Type invokeRet = invokeMethod.getReturnType();
                        if (checkReturnType(invokeRet) && !invokeMethod.getDeclaringClass().getName().equals("okhttp3.OkHttpClient")) {
                            if (!invokeMethod.getSignature().contains("okhttp3.OkHttpClient$Builder: okhttp3.OkHttpClient build()")) {
                                List<AbstractHttpClient> points = findHttpClientBuildMethod(invokeMethod);
                                if (!points.isEmpty()) {
                                    result.addAll(points);
                                    if (sootMethod.getReturnType().toString().equals("okhttp3.OkHttpClient")) {
                                        for (AbstractHttpClient pt : points) {
                                            //TODO request body param trans.
                                            okHttpClient newClient = (okHttpClient) pt;
                                            newClient.setLocalValue(leftOp);
                                            addValue(localToPoint, leftOp, newClient);

                                            LOGGER.info("108: Method: {} , From Method: {}, new Client: {}", methodSig, invokeMethod.getSignature(), pt.toString());
                                        }
                                    } else {
                                        for (AbstractHttpClient pt : points) {
                                            addValue(localToPoint, leftOp, pt);
                                            LOGGER.info("114: Method: {} , From Method: {}, new Client: {}", methodSig, invokeMethod.getSignature(), pt.toString());
                                        }
                                    }
                                    tryToAddResult(sootMethod, points);
                                }
                            } else if (checkReturnInterceptor(invokeRet)) {
                                if (leftOp instanceof Local)
                                    localToInterceptor.put(leftOp, allInterceptorClasses.get(invokeRet.toString()));
                            }
                        }

                        if (rightOp instanceof InstanceInvokeExpr) {
                            Value base = ((InstanceInvokeExpr) rightOp).getBase();
                            InvokeExpr invokeExpr = (InstanceInvokeExpr) rightOp;
                            String sig = ((InstanceInvokeExpr) rightOp).getMethod().getSignature();
                            if(localToClz.containsKey(base)){
                                AbstractClz abstractClz = localToClz.get(base);
                                HashMap<Value, List<String>>  paramValueWithStrings = new HashMap<>();

                                for(Value arg : ((InstanceInvokeExpr) rightOp).getArgs()){
                                    if(localToClz.containsKey(arg)){
                                        AbstractClz abstractClz1 = localToClz.get(arg);
                                        abstractClz1.solve();
                                        if(abstractClz1.isSolved()){
                                            paramValueWithStrings.put(arg, new ArrayList<>(List.of(abstractClz1.toString())));
                                        }
                                        if(localFromParam.containsKey(arg)){
                                            addValue(localFromParam, base, localFromParam.get(arg));
                                        }
                                    }

                                    else if(arg instanceof Constant){
                                        Object obj = SimulateUtil.getConstant(arg);
                                        if(obj != null){
                                            String objString = obj.toString();
                                            paramValueWithStrings.put(arg, new ArrayList<>(List.of(objString)));
                                        }
                                    }
                                    else if(localFromParamInvoke.containsKey(arg)){
                                        paramValueWithStrings.put(arg, new ArrayList<>(List.of(localFromParamInvoke.get(arg).toString())));
                                        if(!localFromParamInvoke.get(arg).param.isEmpty())
                                            addValue(localFromParam, base, localFromParamInvoke.get(arg).param);
                                    }
                                    else if(localToString.containsKey(arg) ){
                                        paramValueWithStrings.put(arg, localToString.get(arg));
                                    }

                                    else if(localFromParam.containsKey(arg)){
                                        addValue(localFromParam, base, localFromParam.get(arg));
                                        paramValueWithStrings.put(arg, new ArrayList<>(List.of("$" + localFromParam.get(arg))));
                                    }

                                }
                                ValueContext valueContext = new ValueContext(sootMethod, unit, paramValueWithStrings);
                                abstractClz.addValueContexts(valueContext);
                                abstractClz.solve();
                                localToClz.put(leftOp, localToClz.get(base));
                                localFromParam.remove(leftOp);

                                if(localFromParam.containsKey(base)){
                                    addValue(localFromParam, leftOp, localFromParam.get(base));
                                    if(abstractClz instanceof UrlClz)
                                        addValue(((UrlClz)abstractClz).getClientResult().params, localFromParam.get(base));
                                }
                            }
                            else if (localToPoint.containsKey(base)) {
                                List<AbstractHttpClient> localPoints = localToPoint.get(base);
                                if (sig.contains("okhttp3.OkHttpClient$Builder: okhttp3.OkHttpClient$Builder addInterceptor") ||
                                        sig.contains("okhttp3.OkHttpClient$Builder: okhttp3.OkHttpClient$Builder addNetworkInterceptor")) {
                                    Value arg = invokeExpr.getArg(0);
                                    if (localToInterceptor.containsKey(arg)) {
                                        for (AbstractHttpClient client : localPoints) {
                                            okHttpClient okHttpClient = (firmproj.client.okHttpClient) client;
                                            okHttpClient.setInterceptors(localToInterceptor.get(arg));
                                        }
                                    }
                                } else if (sig.contains("okhttp3.OkHttpClient$Builder: okhttp3.OkHttpClient build()")) {
                                    localToPoint.put(leftOp, localToPoint.remove(base));
                                    for (AbstractHttpClient client : localToPoint.get(leftOp)) {
                                        okHttpClient newClient = (okHttpClient) client;
                                        newClient.setLocalValue(leftOp);
                                    }
                                } else if (sig.contains("okhttp3.OkHttpClient: okhttp3.Call newCall")) {
                                    localToPoint.put(leftOp, localToPoint.remove(base));
                                    Value arg0 = invokeExpr.getArg(0);
                                    HashMap<String, List<String>> newCallParam = new HashMap<>();
                                    if (localFromParam.containsKey(arg0)) {
                                        newCallParam.put("Request", List.of("$" + localFromParam.get(arg0)));
                                    } else if (localToRequestBuilder.containsKey(arg0)) {
                                        newCallParam.putAll(localToRequestBuilder.get(arg0));
                                    }
                                    for (AbstractHttpClient client : localToPoint.get(leftOp)) {
                                        okHttpClient okHttpClient = (firmproj.client.okHttpClient) client;
                                        okHttpClient.setNeedRequestContent(true);
                                        okHttpClient.requestContentFromParams.putAll(newCallParam);
                                    }
                                }
                            } else if (localToRequestBuilder.containsKey(base)) {
                                if (sig.contains("okhttp3.Request$Builder: okhttp3.Request$Builder url")) {
                                    Value urlParam = invokeExpr.getArg(0);
                                    if (urlParam instanceof Constant) {
                                        Object obj = SimulateUtil.getConstant(urlParam);
                                        if (obj != null) {
                                            localToRequestBuilder.get(base).put("url", List.of(obj.toString()));
                                        }
                                    } else if (localFromParam.containsKey(urlParam)) {
                                        localToRequestBuilder.get(base).put("url", List.of("$" + localFromParam.get(urlParam)));
                                    } else if (urlParam instanceof Local) {
                                        if (!currentValues.containsKey(sig)) {
                                            HashMap<String, List<Integer>> interestingInvoke = new HashMap<>();
                                            interestingInvoke.put(sig, new ArrayList<>(List.of(0)));
                                            MethodLocal methodLocal = new MethodLocal(sootMethod, interestingInvoke, 0);
                                            methodLocal.doAnalysis();
                                            if (methodLocal.getLocalFromParams().containsKey(invokeExpr.getArg(0))) {
                                                localToRequestBuilder.get(base).put("url", List.of("$" + methodLocal.getLocalFromParams().get(invokeExpr.getArg(0))));
                                            } else {
                                                currentValues.putAll(methodLocal.getInterestingParamString());
                                            }
                                        }
                                        if (currentValues.containsKey(sig)) {
                                            List<String> urls = currentValues.get(sig).get(0);
                                            if (urls != null) {
                                                localToRequestBuilder.get(base).put("url", urls);
                                            }
                                        }
                                    }
                                } else if (sig.contains("okhttp3.Request$Builder: okhttp3.Request$Builder post")) {
                                    Value postParam = invokeExpr.getArg(0);
                                    if (postParam instanceof Constant) {
                                        Object obj = SimulateUtil.getConstant(postParam);
                                        if (obj != null) {
                                            localToRequestBuilder.get(base).put("post", List.of(obj.toString()));
                                        }
                                    } else if (localFromParam.containsKey(postParam)) {
                                        localToRequestBuilder.get(base).put("post", List.of("$" + localFromParam.get(postParam)));
                                    } else if (localToString.containsKey(postParam)) {
                                        localToRequestBuilder.get(base).put("post", localToString.get(postParam));
                                    }
                                } else if (sig.contains("okhttp3.Request$Builder: okhttp3.Request build()")) {
                                    localToRequestBuilder.put(leftOp, localToRequestBuilder.remove(base));
                                }
                            }
                            else if(localFromParam.containsKey(base)){
                                localFromParamInvoke.put(leftOp, new MethodParamInvoke(sootMethod, localFromParam.get(base), invokeMethod.getSignature()));
                            }
                            else if(localFromParamInvoke.containsKey(base)){
                                localFromParamInvoke.put(leftOp, new MethodParamInvoke(localFromParamInvoke.get(base)));
                                localFromParamInvoke.get(leftOp).addMethodInvoke(invokeMethod.getSignature());
                            }
                        } else if (rightOp instanceof StaticInvokeExpr) {
                            StaticInvokeExpr staticInvokeExpr = (StaticInvokeExpr) rightOp;
                            String invokeSig = staticInvokeExpr.getMethod().getSignature();
                            if (invokeSig.contains("okhttp3.MediaType: okhttp3.MediaType parse")) {
                                localToString.put(leftOp, new ArrayList<>());
                                Value arg0 = staticInvokeExpr.getArg(0);
                                if (arg0 instanceof Constant) {
                                    Object obj = SimulateUtil.getConstant(staticInvokeExpr.getArg(0));
                                    if (obj != null) {
                                        localToString.get(leftOp).add(obj.toString());
                                    }
                                } else if (localFromParam.containsKey(arg0)) {
                                    localToString.get(leftOp).add("$" + localFromParam.get(arg0));
                                }
                            } else if (invokeSig.contains("okhttp3.RequestBody: okhttp3.RequestBody create")) {
                                localToString.put(leftOp, new ArrayList<>());
                                for (Value value : staticInvokeExpr.getArgs()) {
                                    if (value instanceof Constant) {
                                        Object obj = SimulateUtil.getConstant(value);
                                        if (obj != null) {
                                            localToString.get(leftOp).add(obj.toString());
                                        }
                                    } else if (localFromParam.containsKey(value)) {
                                        localToString.get(leftOp).add("$" + localFromParam.get(value));
                                    } else if (localToString.containsKey(value)) {
                                        localToString.get(leftOp).addAll(localToString.get(value));
                                    }
                                }
                            }
                        }
                    } else if (rightOp instanceof CastExpr) {
                        Value op = ((CastExpr) rightOp).getOp();
                        localToClz.remove(leftOp);
                        localToString.remove(leftOp);
                        localToPoint.remove(leftOp);
                        if(localFromParam.containsKey(op)){
                            localFromParam.put(leftOp, localFromParam.get(op));
                        }
                        if (localToPoint.containsKey(op)) {
                            if (leftOp instanceof Local)
                                localToPoint.put(leftOp, localToPoint.get(op));
                        } else if (localToInterceptor.containsKey(op)) {
                            localToInterceptor.put(leftOp, localToInterceptor.get(op));
                        } else if (localToClz.containsKey(op)) {
                            localToClz.put(leftOp, localToClz.get(op));
                        }
                    } else if (rightOp instanceof Local && localToPoint.containsKey(rightOp)) {
                        if (leftOp instanceof FieldRef) {
                            FieldToClientPoint.put(((FieldRef) leftOp).getField(), localToPoint.get(rightOp));
                        }
                    } else if (localToClz.containsKey(rightOp)) {
                        localToClz.put(leftOp, localToClz.get(rightOp));
                    } else if (rightOp instanceof FieldRef) {
                        SootField field = ((FieldRef) rightOp).getField();
                        if(MethodString.fieldToString.containsKey(field.toString())){
                            localToString.put(leftOp, MethodString.fieldToString.get(field.toString()));
                            localFromParam.remove(leftOp);
                            localToClz.remove(leftOp);
                            localToPoint.remove(leftOp);
                        }
                        if (FieldToClientPoint.containsKey(field)) {
                            if (leftOp instanceof Local) {
                                localToPoint.put(leftOp, FieldToClientPoint.get(((FieldRef) rightOp).getField()));
                            }
                        } else if (field.getType() instanceof RefType) {
                            String clsName = ((RefType) field.getType()).getClassName();
                            if (allInterceptorClasses.containsKey(clsName)) {
                                localToInterceptor.put(leftOp, allInterceptorClasses.get(clsName));
                            }
                        }
                    }
                }else if(stmt instanceof InvokeStmt){
                    if(stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
                        Value base = ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase();
                        InvokeExpr invokeExpr = stmt.getInvokeExpr();
                        if(localToClz.containsKey(base)){
                            AbstractClz abstractClz = localToClz.get(base);
                            HashMap<Value, List<String>>  paramValueWithStrings = new HashMap<>();

                            for(Value arg : invokeExpr.getArgs()){
                                if(localToClz.containsKey(arg)){
                                    AbstractClz abstractClz1 = localToClz.get(arg);
                                    abstractClz1.solve();
                                    if(abstractClz1.isSolved()){
                                        paramValueWithStrings.put(arg, new ArrayList<>(List.of(abstractClz1.toString())));
                                    }
                                    if(localFromParam.containsKey(arg)){
                                        addValue(localFromParam, base, localFromParam.get(arg));
                                    }
                                }
                                else if(arg instanceof Constant){
                                    Object obj = SimulateUtil.getConstant(arg);
                                    if(obj != null){
                                        String objString = obj.toString();
                                        paramValueWithStrings.put(arg, new ArrayList<>(List.of(objString)));
                                    }
                                }
                                else if(localToString.containsKey(arg) ){
                                    paramValueWithStrings.put(arg, localToString.get(arg));
                                }
                                else if(localFromParam.containsKey(arg)){
                                    addValue(localFromParam, base, localFromParam.get(arg));
                                    paramValueWithStrings.put(arg, new ArrayList<>(List.of("$" + localFromParam.get(arg))));
                                }

                            }
                            ValueContext valueContext = new ValueContext(sootMethod, unit, paramValueWithStrings);
                            abstractClz.addValueContexts(valueContext);
                            abstractClz.solve();

                            if(abstractClz instanceof UrlClz && localFromParam.containsKey(base))
                                addValue(((UrlClz)abstractClz).getClientResult().params, localFromParam.get(base));
                        }
                    }
                }
            }catch (Exception ignore){
            }
        }
        return result;
    }

    private static void tryToAddResult(SootMethod sootMethod, AbstractHttpClient point){
        addValue(findResult, sootMethod.getSignature(), point);
    }

    private static void tryToAddResult(SootMethod sootMethod, List<AbstractHttpClient> points){
        for (AbstractHttpClient point : points)
            addValue(findResult, sootMethod.getSignature(), point);
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

    public static <K, V> void addValue(Map<K, List<V>> map, K key, V value) {
        map.computeIfAbsent(key, k -> new ArrayList<>());
        List<V> values = map.get(key);
        if(!values.contains(value)){
            values.add(value);
        }
    }

    public static <K, V> void addValue(Map<K, List<V>> map, K key, List<V> values) {
        map.computeIfAbsent(key, k -> new ArrayList<>());
        List<V> key_values = map.get(key);
        for(V value: values) {
            if(value!=null) {
                if (!key_values.contains(value)) {
                    key_values.add(value);
                }
            }
        }
    }

    public static HashMap<Integer, String> checkArgType(InvokeExpr invokeExpr){
        List<Value> args = invokeExpr.getArgs();
        HashMap<Integer, String> result = new HashMap<>();
        String clsString = "";
        int i = 0;
        for(Value arg : args){
            if(arg instanceof ClassConstant){
                clsString = (String)SimulateUtil.getConstant(arg);
                result.put(i,clsString);
            }
            i++;
        }
        return result;
    }

    public static boolean checkReturnType(Type v){
        String typeClz = v.toString();
        for(SootClass clz : RetrofitBuildFind.RetrofitClassesWithMethods.keySet()){
            if(typeClz.equals(clz.getName()))
                return true;
        }
        return typeClz.equals("void") ||typeClz.equals("java.lang.Object") || typeClz.contains("okhttp3.OkHttpClient") || typeClz.contains("retrofit2.Retrofit");
    }

    public static boolean checkReturnInterceptor(Type v){
        String typeClz = v.toString();
        for(String clz : allInterceptorClasses.keySet()){
            if(typeClz.equals(clz))
                return true;
        }
        return false;
    }


    public static <T> List<T> clone(List<T> ls) {
        return new ArrayList<T>(ls);
    }

}
