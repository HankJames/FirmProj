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
    public static final HashMap<String,List<RetrofitBuildPoint>> RetrofitClassToBuildPoint = new HashMap<>();
    public static final HashMap<SootClass, List<RetrofitPoint>> RetrofitClassesWithMethods = new HashMap<>();
    public static final HashMap<FieldRef, List<RetrofitBuildPoint>> FieldToBuildPoint = new HashMap<>();

    private static final Logger LOGGER = LogManager.getLogger(RetrofitBuildFind.class);
    private static final HashSet<String> visitedMethod = new HashSet<>();

    public static void findAllRetrofitBuildMethod(){
        Chain<SootClass> classes = Scene.v().getClasses();
        for (SootClass sootClass : classes) {
            String headString = sootClass.getName().split("\\.")[0];
            if (headString.contains("android") || headString.contains("kotlin") || headString.contains("java"))
                continue;
            //LOGGER.info(sootClass.getName().toString());
            for (SootMethod sootMethod : clone(sootClass.getMethods())) {
                if (!sootMethod.isConcrete())
                    continue;
                findRetrofitBuildMethod(sootMethod, new HashMap<>());
            }
        }
        LOGGER.info("All retrofitResult: {}",findResult.toString());
    }

    public static List<RetrofitBuildPoint> findRetrofitBuildMethod(SootMethod sootMethod, HashMap<Integer,String> clsStr){
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

        if(!checkReturnType(ret)) return result;
        visitedMethod.add(methodSig);
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
                    HashMap<Integer,String> args = checkArgType((InvokeExpr) rightOp);
                    if(checkReturnType(invokeRet) && !invokeMethod.getDeclaringClass().getName().equals("retrofit2.Retrofit")){
                        if(!invokeMethod.getSignature().contains("retrofit2.Retrofit$Builder: retrofit2.Retrofit build()")) {
                            List<RetrofitBuildPoint> points = findRetrofitBuildMethod(invokeMethod,args);
                            if (!points.isEmpty()) {
                                result.addAll(points);
                                for (RetrofitBuildPoint pt : points) {
                                    addValue(localToPoint, leftOp, pt);
                                    LOGGER.info("86: Method: {} , From Method: {}, new Point: {}", methodSig, invokeMethod.getSignature(), pt.toString());
                                }
                                tryToAddResult(sootMethod, points);
                            }
                        }
                    }

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
                                    if(!currentValues.containsKey(sig)) {
                                        HashMap<String, List<Integer>> intereInvoke = new HashMap<>();
                                        intereInvoke.put(sig, List.of(0));
                                        MethodLocal methodLocal = new MethodLocal(sootMethod, intereInvoke);
                                        methodLocal.doAnalysis();
                                        currentValues.putAll(methodLocal.getInterestingParamString());
                                    }
                                    if (currentValues.containsKey(sig)){
                                        List<String> clsNames = currentValues.get(sig).get(0);
                                        int i=0;
                                        if(localPoints.size() < clsNames.size()) {
                                            for(RetrofitBuildPoint lp: localPoints){
                                                if(lp.getCreateClass() == null) {
                                                    lp.setCreateClass(clsNames.get(i));
                                                    addValue(RetrofitClassToBuildPoint, clsNames.get(i), lp);
                                                }
                                                i++;
                                            }
                                        }
                                        else{
                                            for(String clsName: clsNames){
                                                if(localPoints.get(i).getCreateClass() == null) {
                                                    localPoints.get(i).setCreateClass(clsName);
                                                    addValue(RetrofitClassToBuildPoint, clsNames.get(i), localPoints.get(i));
                                                }
                                                i++;
                                            }
                                        }
                                    }
                                }

                            } else if (sig.contains("retrofit2.Retrofit$Builder: retrofit2.Retrofit$Builder baseUrl(java.lang.String)")) {
                                Value arg = invokeExpr.getArg(0);
                                if (arg instanceof Constant) {
                                    String baseUrl = (String) SimulateUtil.getConstant(arg);
                                    for(RetrofitBuildPoint lp: localPoints){
                                        if(lp.getBaseUrl()==null)
                                            lp.setBaseUrl(baseUrl);
                                    }
                                }
                                else if(arg instanceof Local){
                                    if(!currentValues.containsKey(sig)) {
                                        HashMap<String, List<Integer>> intereInvoke = new HashMap<>();
                                        intereInvoke.put(sig, List.of(0));
                                        MethodLocal methodLocal = new MethodLocal(sootMethod, intereInvoke);
                                        methodLocal.doAnalysis();
                                        currentValues.putAll(methodLocal.getInterestingParamString());
                                    }
                                    if (currentValues.containsKey(sig)){
                                        List<String> baseUrls = currentValues.get(sig).get(0);
                                        for(String baseUrl: baseUrls){
                                            for(RetrofitBuildPoint lp: localPoints){
                                                lp.setBaseUrl(baseUrl);
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
                                    //
                                }
                            } else if (sig.contains("retrofit2.Retrofit$Builder: retrofit2.Retrofit$Builder addConverterFactory")) {
                                //TODO converterFactory
                            }
                        }
                    }
                }
                else if(rightOp instanceof Local && localToPoint.containsKey(rightOp)){
                    if(leftOp instanceof FieldRef){
                        FieldToBuildPoint.put((FieldRef) leftOp, localToPoint.get(rightOp));
                    }
                }
                else if(rightOp instanceof FieldRef && FieldToBuildPoint.containsKey((FieldRef) rightOp)){
                    if(leftOp instanceof Local){
                        localToPoint.put(leftOp, FieldToBuildPoint.get((FieldRef) rightOp));
                    }
                }
            }
        }
        return result;
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
        for(SootClass clz : RetrofitClassesWithMethods.keySet()){
            if(typeClz.equals(clz.getName()))
                return true;
        }
        return typeClz.equals("void") ||typeClz.equals("java.lang.Object") || typeClz.equals("retrofit2.Retrofit");
    }


    public static <T> List<T> clone(List<T> ls) {
        return new ArrayList<T>(ls);
    }


}
