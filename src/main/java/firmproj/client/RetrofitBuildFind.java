package firmproj.client;

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

    private static final Logger LOGGER = LogManager.getLogger(RetrofitBuildFind.class);

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
                findRetrofitBuildMethod(sootMethod);
            }
        }
    }

    public static List<RetrofitBuildPoint> findRetrofitBuildMethod(SootMethod sootMethod){
        List<RetrofitBuildPoint> result = new ArrayList<>();
        if (!sootMethod.isConcrete()) return result;
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
        if(!checkReturnType(ret)) return result;
        for (Unit unit : body.getUnits()) {
            Stmt stmt = (Stmt) unit;
            if(stmt instanceof AssignStmt){
                Value leftOp = ((AssignStmt) stmt).getLeftOp();
                Value rightOp = ((AssignStmt) stmt).getRightOp();
                if(rightOp instanceof NewExpr){
                    NewExpr newExpr = (NewExpr) rightOp;
                    String clsName = newExpr.getBaseType().getClassName();
                    if(clsName.equals("retrofit2.Retrofit$Builder")){
                        RetrofitBuildPoint point = new RetrofitBuildPoint(unit);
                        addValue(localToPoint, leftOp, point);
                        result.add(point);
                        tryToAddResult(sootMethod, point);
                    }
                }
                if(rightOp instanceof InvokeExpr) {
                    SootMethod invokeMethod = ((InvokeExpr) rightOp).getMethod();
                    Type invokeRet = invokeMethod.getReturnType();
                    if(checkReturnType(invokeRet)){
                        List<RetrofitBuildPoint> points = findRetrofitBuildMethod(invokeMethod);
                        if(!points.isEmpty()){
                            result.addAll(points);
                            for(RetrofitBuildPoint pt : points)
                                addValue(localToPoint, leftOp, pt);
                            tryToAddResult(sootMethod, points);
                        }
                    }

                    if (rightOp instanceof InstanceInvokeExpr) {
                        Value base = ((InstanceInvokeExpr) rightOp).getBase();
                        InvokeExpr invokeExpr = (InstanceInvokeExpr) rightOp;
                        String sig = ((InstanceInvokeExpr) rightOp).getMethod().getSignature();
                        if (localToPoint.containsKey(base)) {
                            if (sig.contains("retrofit2.Retrofit: java.lang.Object create(java.lang.Class)")) {
                                Value arg = invokeExpr.getArg(0);
                                if (arg instanceof Constant) {
                                    String clsName = (String) SimulateUtil.getConstant(arg);
                                    List<RetrofitBuildPoint> localPoints = localToPoint.get(base);
                                    for(RetrofitBuildPoint lp: localPoints){
                                        if(lp.getCreateClass()==null) {
                                            lp.setCreateClass(clsName);
                                            addValue(RetrofitClassToBuildPoint, clsName, lp);
                                        }
                                    }

                                }
                                tryToAddResult(sootMethod, localToPoint.get(base));
                                //TODO local
                            } else if (sig.contains("retrofit2.Retrofit$Builder: retrofit2.Retrofit$Builder baseUrl(java.lang.String)")) {
                                Value arg = invokeExpr.getArg(0);
                                if (arg instanceof Constant) {
                                    String baseUrl = (String) SimulateUtil.getConstant(arg);
                                    List<RetrofitBuildPoint> localPoints = localToPoint.get(base);
                                    for(RetrofitBuildPoint lp: localPoints){
                                        if(lp.getBaseUrl()==null)
                                            lp.setBaseUrl(baseUrl);
                                    }
                                }
                                //TODO local
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

    public static boolean checkReturnType(Type v){
        String typeClz = v.toString();
        for(SootClass clz : RetrofitClassesWithMethods.keySet()){
            if(typeClz.equals(clz.getName()))
                return true;
        }
        return typeClz.equals("java.lang.Object") || typeClz.equals("retrofit2.Retrofit");
    }


    public static <T> List<T> clone(List<T> ls) {
        return new ArrayList<T>(ls);
    }


}
