package firmproj.client;

import firmproj.base.MethodLocal;
import firmproj.base.MethodString;
import firmproj.base.RetrofitPoint;
import firmproj.objectSim.SimulateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.*;

public class RetrofitBuildFind {

    public static final HashMap<String,List<RetrofitBuildPoint>> findResult = new HashMap<>(); //Return Api method's Name To RetrofitBuildPoint
    public static final HashMap<String,List<RetrofitBuildPoint>> RetrofitClassToBuildPoint = new HashMap<>(); //Retrofit create api -> class.
    public static final HashMap<SootClass, List<RetrofitPoint>> RetrofitClassesWithMethods = new HashMap<>(); //Retrofit Interface Class -> all the class Retrofit Point.
    public static final HashMap<SootField, List<RetrofitBuildPoint>> FieldToBuildPoint = new HashMap<>(); //SootField related to the RetrofitBuildPoint
    public static final HashMap<String, AbstractFactory> allFactoryClass = new HashMap<>();

    private static final Logger LOGGER = LogManager.getLogger(RetrofitBuildFind.class);
    private static final HashSet<String> visitedMethod = new HashSet<>();

    public static void findAllRetrofitBuildMethod(){
        Chain<SootClass> classes = Scene.v().getClasses();
        for (SootClass sootClass : classes) {
            String headString = sootClass.getName().split("\\.")[0];
            if (headString.contains("android") || headString.contains("kotlin") || headString.contains("java") || headString.contains("thingClips"))
                continue;
            //LOGGER.info(sootClass.getName().toString());
            for (SootMethod sootMethod : clone(sootClass.getMethods())) {
                if (!sootMethod.isConcrete())
                    continue;
                findRetrofitBuildMethod(sootMethod);
            }
        }
        LOGGER.info("All retrofitResult: {}",findResult.toString());
        LOGGER.info("ALL Field with retrofitBuildPoint: {}", FieldToBuildPoint);
    }

    public static void findAllFactoryClasses(){
        Chain<SootClass> classes = Scene.v().getClasses();
        for (SootClass sootClass : classes) {
            String headString = sootClass.getName().split("\\.")[0];
            String CONVERTER_FACTORY = "retrofit2.Converter$Factory";
            String CALL_FACTORY = "okhttp3.Call$Factory";
            if (headString.contains("android") || headString.contains("kotlin") || headString.contains("java") || headString.contains("thingClips"))
                continue;
            SootClass superClz = sootClass.getSuperclass();
            if(superClz.getName().equals(CONVERTER_FACTORY)) {
                ConverterFactory converterFactory = new ConverterFactory(sootClass);
                converterFactory.init();
                allFactoryClass.put(sootClass.getName(), converterFactory);
            }
            else if(superClz.getName().equals(CALL_FACTORY)){
                CallFactory callFactory = new CallFactory(sootClass);
                callFactory.init();
                allFactoryClass.put(sootClass.getName(), callFactory);
            }

        }
        LOGGER.info("All Interceptor Classes Result : {}",allFactoryClass.toString());

        //LOGGER.info("ALL Field with retrofitBuildPoint: {}", );
    }


    public static List<RetrofitBuildPoint> findRetrofitBuildMethod(SootMethod sootMethod){
        String methodSig = sootMethod.getSignature();
        if(findResult.containsKey(methodSig)) return findResult.get(methodSig);

        List<RetrofitBuildPoint> result = new ArrayList<>();

        if(visitedMethod.contains(methodSig)) return result;

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

        HashMap<Value, List<RetrofitBuildPoint>> localToPoint = new HashMap<>();
        HashMap<String, HashMap<Integer, List<String>>> currentValues = new HashMap<>();
        HashMap<Value, List<AbstractHttpClient>> localToClient = new HashMap<>();

        if(!checkReturnType(ret)) return result;
        visitedMethod.add(methodSig);
        if(methodSig.equals("<com.liesheng.module_login.repository.LoginRepository$customerCheckRegister$2: java.lang.Object invokeSuspend(java.lang.Object)>"))
            LOGGER.warn("GOT!");
        for (Unit unit : body.getUnits()) {
            Stmt stmt = (Stmt) unit;
            if(stmt instanceof AssignStmt){
                Value leftOp = ((AssignStmt) stmt).getLeftOp();
                Value rightOp = ((AssignStmt) stmt).getRightOp();
                if(rightOp instanceof NewExpr){
                    NewExpr newExpr = (NewExpr) rightOp;
                    String clsName = newExpr.getBaseType().getClassName();
                    if(clsName.equals("retrofit2.Retrofit$Builder")){
                        RetrofitBuildPoint point = new RetrofitBuildPoint(sootMethod, unit);
                        addValue(localToPoint, leftOp, point);
                        result.add(point);
                        tryToAddResult(sootMethod, point);
                        LOGGER.info("72: Method: {} , new Point: {}", methodSig, point.toString());
                    }
                }
                if(rightOp instanceof InvokeExpr) {
                    SootMethod invokeMethod = ((InvokeExpr) rightOp).getMethod();
                    Type invokeRet = invokeMethod.getReturnType();
                    if(checkReturnType(invokeRet) && !invokeMethod.getDeclaringClass().getName().equals("retrofit2.Retrofit")){
                        if(!invokeMethod.getSignature().contains("retrofit2.Retrofit$Builder: retrofit2.Retrofit build()")) {
                            List<RetrofitBuildPoint> points = findRetrofitBuildMethod(invokeMethod);
                            if (!points.isEmpty()) {
                                for (RetrofitBuildPoint pt : points) {
                                    RetrofitBuildPoint newRetrofitBuildPoint = new RetrofitBuildPoint(pt);
                                    newRetrofitBuildPoint.setCurrentMethod(sootMethod);
                                    newRetrofitBuildPoint.setCreateUnit(unit);
                                    addValue(localToPoint, leftOp, newRetrofitBuildPoint);
                                    tryToSolveParam(newRetrofitBuildPoint, pt, invokeMethod.getSignature(), (InvokeExpr) rightOp);
                                    if(newRetrofitBuildPoint.getCreateClass() != null)
                                        addValue(RetrofitClassToBuildPoint, newRetrofitBuildPoint.getCreateClass(), newRetrofitBuildPoint);
                                    tryToAddResult(sootMethod, newRetrofitBuildPoint);
                                    result.add(newRetrofitBuildPoint);
                                    LOGGER.info("86: Method: {} , From Method: {}, new Point: {}", methodSig, invokeMethod.getSignature(), newRetrofitBuildPoint.toString());
                                }
                            }
                        }
                        //TODO return okhttp client method.
                    }
                    if(HttpClientFind.findResult.containsKey(invokeMethod.getSignature()))
                        localToClient.put(leftOp, HttpClientFind.findResult.get(invokeMethod.getSignature()));

                    if (rightOp instanceof InstanceInvokeExpr) {
                        Value base = ((InstanceInvokeExpr) rightOp).getBase();
                        InvokeExpr invokeExpr = (InstanceInvokeExpr) rightOp;
                        String sig = ((InstanceInvokeExpr) rightOp).getMethod().getSignature();
                        if (localToPoint.containsKey(base)) {
                            List<RetrofitBuildPoint> localPoints = localToPoint.get(base);
                            if (sig.contains("retrofit2.Retrofit: java.lang.Object create(java.lang.Class)")) {
                                Value arg = invokeExpr.getArg(0);
                                if (arg instanceof Constant) {
                                    String clsName = (String) SimulateUtil.getConstant(arg);
                                    for(RetrofitBuildPoint lp: localPoints){
                                        if(lp.getCreateClass()==null) {
                                            lp.setCreateClass(clsName);
                                            addValue(RetrofitClassToBuildPoint, clsName, lp);
                                        }
                                    }
                                }
                                else if(arg instanceof Local){
                                    //TODO local
                                    boolean fromParam = false;
                                    HashMap<String, List<Integer>> intereInvoke = new HashMap<>();
                                    intereInvoke.put(sig, new ArrayList<>(List.of(0)));
                                    LOGGER.warn("[DO LOCAL TO FIND CLASS]: {} -> {}", sootMethod, invokeExpr);
                                    MethodLocal methodLocal = new MethodLocal(sootMethod, intereInvoke);
                                    methodLocal.doAnalysis();
                                    if(methodLocal.getLocalFromParams().containsKey(invokeExpr.getArg(0))) {
                                        fromParam = true;
                                        for (RetrofitBuildPoint lp : localPoints) {
                                            lp.classFromParam = true;
                                            lp.classParam = methodLocal.getLocalFromParams().get(invokeExpr.getArg(0));
                                        }
                                    }
                                    currentValues.putAll(methodLocal.getInterestingParamString());

                                    if (currentValues.containsKey(sig) && !fromParam){
                                        if(currentValues.get(sig).containsKey(0)) {
                                            List<String> clsNames = currentValues.get(sig).get(0);
                                            for (String clsName : clsNames) {
                                                for (RetrofitBuildPoint lp : localPoints) {
                                                    lp.setCreateClass(clsName);
                                                    addValue(RetrofitClassToBuildPoint, clsName, lp);
                                                }
                                            }
                                        }
                                    }
                                }
                                if(leftOp instanceof Local){
                                    localToPoint.put(leftOp, localToPoint.remove(base));
                                }
                                else if(leftOp instanceof FieldRef){
                                    FieldToBuildPoint.put(((FieldRef) leftOp).getField(), localToPoint.get(base));
                                }

                            } else if (sig.contains("retrofit2.Retrofit$Builder: retrofit2.Retrofit$Builder baseUrl(java.lang.String)")) {
                                Value arg = invokeExpr.getArg(0);
                                if (arg instanceof Constant) {
                                    String baseUrl = (String) SimulateUtil.getConstant(arg);
                                    for(RetrofitBuildPoint lp: localPoints){
                                        lp.setBaseUrl(baseUrl);
                                    }
                                }
                                else if(arg instanceof Local){
                                    boolean fromParam = false;
                                    HashMap<String, List<Integer>> intereInvoke = new HashMap<>();
                                    intereInvoke.put(sig, new ArrayList<>(List.of(0)));
                                    MethodLocal methodLocal = new MethodLocal(sootMethod, intereInvoke);
                                    methodLocal.doAnalysis();
                                    if(methodLocal.getLocalFromParams().containsKey(invokeExpr.getArg(0))) {
                                        fromParam = true;
                                        for (RetrofitBuildPoint lp : localPoints) {
                                            lp.urlFromParam = true;
                                            lp.urlParam = methodLocal.getLocalFromParams().get(invokeExpr.getArg(0));
                                        }
                                    }
                                    currentValues.putAll(methodLocal.getInterestingParamString());
                                    LOGGER.info("166: Do MethodLocal result: {}", currentValues.get(sig));

                                    if (currentValues.containsKey(sig) && !fromParam){
                                        if(currentValues.get(sig).containsKey(0)) {
                                            List<String> baseUrls = currentValues.get(sig).get(0);
                                            LOGGER.info("169: Do MethodLocal result: {}", currentValues.get(sig));
                                            for (String baseUrl : baseUrls) {
                                                for (RetrofitBuildPoint lp : localPoints) {
                                                    lp.setBaseUrl(baseUrl);
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if(sig.contains("retrofit2.Retrofit$Builder: retrofit2.Retrofit build()")){
                                localToPoint.put(leftOp, localToPoint.remove(base));

                            } else if (sig.contains("retrofit2.Retrofit$Builder: retrofit2.Retrofit$Builder client")) {
                                //TODO client
                                Value arg = invokeExpr.getArg(0);
                                if (arg instanceof Local) {
                                    if(localToClient.containsKey(arg)){
                                        for (RetrofitBuildPoint lp : localPoints) {
                                            lp.setOkHttpClients(localToClient.get(arg));
                                        }
                                        LOGGER.info("GET LOCAL CLIENT: {}->{}",arg, localToClient.get(arg));
                                    } else if (HttpClientFind.findResult.containsKey(methodSig)) {
                                        List<AbstractHttpClient> tempClients = new ArrayList<>();
                                        for(AbstractHttpClient client: HttpClientFind.findResult.get(methodSig)){
                                            okHttpClient okClient = (okHttpClient) client;
                                            if (okClient.getLocalValue().equals(arg)){
                                                tempClients.add(okClient);
                                            }
                                            LOGGER.info("GET LOCAL CLIENT FROM CURRENT METHOD: {}->{}: {}", okClient.getLocalValue(), arg, okClient.toString());
                                        }
                                        if(!tempClients.isEmpty()){
                                            for (RetrofitBuildPoint lp : localPoints) {
                                                lp.setOkHttpClients(tempClients);
                                            }
                                        }
                                        else{
                                            LOGGER.info("NO LOCAL CLIENT: {}-> {}", arg, invokeExpr);
                                        }
                                    }
                                    // if local from okhttp build, then get result of this method
                                    // else if local from return okhttp client method, then get result of the return method.
                                    
                                }
                            } else if (sig.contains("retrofit2.Retrofit$Builder: retrofit2.Retrofit$Builder") && (sig.contains("ConverterFactory") || sig.contains("callFactory"))) {

                                //TODO converterFactory

                            }
                        } else if (localToClient.containsKey(base)) {
                            localToClient.put(leftOp, localToClient.get(base));
                        }
                    }
                } else if (rightOp instanceof CastExpr) {
                    Value op = ((CastExpr) rightOp).getOp();
                    if(localToPoint.containsKey(op)){
                        localToPoint.put(leftOp, localToPoint.get(op));
                    }
                    if(localToClient.containsKey(op)){
                        localToClient.put(leftOp, localToClient.get(op));
                    }
                } else if(rightOp instanceof Local && localToPoint.containsKey(rightOp)){
                    if(leftOp instanceof FieldRef){
                        FieldToBuildPoint.put(((FieldRef) leftOp).getField(), localToPoint.get(rightOp));
                    }
                } else if(rightOp instanceof FieldRef && FieldToBuildPoint.containsKey(((FieldRef) rightOp).getField())){
                    if(leftOp instanceof Local){
                        localToPoint.put(leftOp, FieldToBuildPoint.get(((FieldRef) rightOp).getField()));
                    }
                } else if(rightOp instanceof FieldRef && HttpClientFind.FieldToClientPoint.containsKey(((FieldRef) rightOp).getField())){
                    if(leftOp instanceof Local){
                        localToClient.put(leftOp, HttpClientFind.FieldToClientPoint.get(((FieldRef) rightOp).getField()));
                    }
                }
            }
        }
        return result;
    }

    private static void tryToSolveParam(RetrofitBuildPoint newPoint, RetrofitBuildPoint oldPoint, String sig, InvokeExpr invokeExpr){
        HashMap<String, List<Integer>> interestInvoke = new HashMap<>();
        interestInvoke.put(sig, new ArrayList<>());
        if(oldPoint.urlFromParam){
            interestInvoke.get(sig).add(oldPoint.urlParam);
        }
        if(oldPoint.classFromParam){
            interestInvoke.get(sig).add(oldPoint.classParam);
        }
        if(!interestInvoke.get(sig).isEmpty()) {
            MethodLocal methodLocal = new MethodLocal(newPoint.getCurrentMethod(), interestInvoke);
            methodLocal.doAnalysis();
            HashMap<Value, Integer> localFromParams = methodLocal.getLocalFromParams();
            HashMap<String, HashMap<Integer, List<String>>> result = methodLocal.getInterestingParamString();
            if(result.containsKey(sig)) {
                if(oldPoint.urlFromParam) {
                    if (localFromParams.containsKey(invokeExpr.getArg(oldPoint.urlParam))) {
                        newPoint.urlFromParam = true;
                        newPoint.urlParam = localFromParams.get(invokeExpr.getArg(oldPoint.urlParam));
                    } else if (result.get(sig).containsKey(oldPoint.urlParam)) {
                        List<String> urlResult = result.get(sig).get(oldPoint.urlParam);
                        for (String url : urlResult)
                            newPoint.setBaseUrl(url);
                    }
                }
                if(oldPoint.classFromParam) {
                    if (localFromParams.containsKey(invokeExpr.getArg(oldPoint.classParam))) {
                        newPoint.classFromParam = true;
                        newPoint.classParam = localFromParams.get(invokeExpr.getArg(oldPoint.classParam));
                    } else if (result.get(sig).containsKey(oldPoint.classParam)) {
                        List<String> classResult = result.get(sig).get(oldPoint.classParam);
                        if (!classResult.isEmpty()) {
                            newPoint.setCreateClass(classResult.get(0));

                        }
                    }
                }
            }

        }
    }

    private static void tryToAddResult(SootMethod sootMethod, RetrofitBuildPoint point){
        addValue(findResult, sootMethod.getSignature(), point);
    }

    private static void tryToAddResult(SootMethod sootMethod, List<RetrofitBuildPoint> points){
        for (RetrofitBuildPoint point : points)
            addValue(findResult, sootMethod.getSignature(), point);
    }

    public static <K, V> void addValue(Map<K, List<V>> map, K key, V value) {
        map.computeIfAbsent(key, k -> new ArrayList<>());
        List<V> values = map.get(key);
        if(!values.contains(value)){
            values.add(value);
        }
    }

//    public static HashMap<Integer, String> checkArgType(InvokeExpr invokeExpr){
//        List<Value> args = invokeExpr.getArgs();
//        HashMap<Integer, String> result = new HashMap<>();
//        String clsString = "";
//        int i = 0;
//        for(Value arg : args){
//            if(arg instanceof ClassConstant){
//                clsString = (String)SimulateUtil.getConstant(arg);
//                result.put(i,clsString);
//            }
//            i++;
//        }
//        return result;
//    }

    public static boolean checkReturnType(Type v){
        String typeClz = v.toString();
        for(SootClass clz : RetrofitClassesWithMethods.keySet()){
            if(typeClz.equals(clz.getName()))
                return true;
        }
        return typeClz.equals("void") ||typeClz.equals("java.lang.Object") || typeClz.contains("retrofit2.Retrofit");
    }


    public static <T> List<T> clone(List<T> ls) {
        return new ArrayList<T>(ls);
    }


}
