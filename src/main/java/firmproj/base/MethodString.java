package firmproj.base;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.*;

public class MethodString {
    private static final Logger LOGGER = LogManager.getLogger(MethodString.class);

    private static final HashMap<SootMethod, String> methodToFieldString = new HashMap<>();

    private static final HashMap<String, List<String>> fieldToString = new HashMap<>();

    private static final HashMap<SootMethod, List<String>> methodToString = new HashMap<>();

    public static void init(){
        Chain<SootClass> classes = Scene.v().getClasses();
        for (SootClass sootClass : classes) {
            String headString = sootClass.getName().split("\\.")[0];
            if (headString.contains("android") || headString.contains("kotlin") || headString.contains("java"))
                continue;
            //LOGGER.info(sootClass.getName().toString());
            for (SootMethod sootMethod : clone(sootClass.getMethods())) {
                if (!sootMethod.isConcrete())
                    continue;
                Body body = null;
                try {
                    body = sootMethod.retrieveActiveBody();
                } catch (Exception e) {
                    //LOGGER.error("Could not retrieved the active body {} because {}", sootMethod, e.getLocalizedMessage());
                }
                if (body == null)
                    continue;
                for (Unit unit : body.getUnits()) {
                    if (unit instanceof Stmt) {
                        if (unit instanceof AssignStmt && ((AssignStmt) unit).getLeftOp() instanceof FieldRef) {
                            FieldRef fieldRef = (FieldRef) ((AssignStmt) unit).getLeftOp();
                            SootField field = fieldRef.getField();
                            Value rightOP = ((AssignStmt) unit).getRightOp();
                            //LOGGER.info("Find new:"+ field.getName()+rightOP.toString());
                            if (field == null || field.getDeclaringClass() == null) {
                                continue;
                            }
                            if (field.getDeclaringClass().isApplicationClass()) {
                                if (rightOP instanceof Constant) {
                                    if (rightOP.toString().isEmpty() || rightOP.toString().equals("1") || rightOP.toString().equals("0") || rightOP.toString() == null || rightOP.toString().equals("-1"))
                                        continue;
                                    addValue(fieldToString, field.toString(), rightOP.toString());
                                    LOGGER.info("New Field with String: " + field + ":" + rightOP + ";" + fieldToString.get(field.toString()).toString());
                                    if (methodToFieldString.containsValue(field.toString())) {
                                        SootMethod method = getKeyByValue(methodToFieldString, field.toString());
                                        if(method != null){
                                            addValue(methodToString, method, rightOP.toString());
                                            LOGGER.info("141:New Method To String: " + method.getName() + ":" + rightOP + ";" + methodToString.get(method).toString());
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
                if (isCommonType(sootMethod.getReturnType()) && sootMethod.getDeclaringClass().isApplicationClass()) {
                    try {
                        unsolvedMethodString = new ArrayList<>();
                        //LOGGER.info("176: " + sootMethod.getReturnType() + ":" + sootMethod.getSignature());
                        GetMethodToString(sootMethod);
                    } catch (Throwable e) {
                        LOGGER.error("error: " + sootMethod + ":" + e);
                    }
                }
            }
        }
    }
    private static List<String> unsolvedMethodString;
    private static final HashSet<String> visitedMethod = new HashSet<>();

    public static List<String> GetMethodToString(SootMethod sootMethod){
        List<String> mstrs = new ArrayList<>();
        List<Unit> VisitedUnits = new ArrayList<>();
        if(!sootMethod.getDeclaringClass().isApplicationClass() || isStandardLibraryClass(sootMethod.getDeclaringClass())) return mstrs;

        if(methodToString.containsKey(sootMethod)) return methodToString.get(sootMethod);

        if(visitedMethod.contains(sootMethod.getSignature())) return mstrs;
        visitedMethod.add(sootMethod.getSignature());

        Body body = null;
        try {
            body = sootMethod.retrieveActiveBody();
        } catch (Exception e) {
            //LOGGER.error("Could not retrieved the active body {} because {}", sootMethod, e.getLocalizedMessage());
        }
        if(body != null) {
            for (Unit unit : body.getUnits()) {
                //LOGGER.info("234: UNIT: " + unit.toString());
                if (unit instanceof ReturnStmt) {
                    ReturnStmt returnStmt = (ReturnStmt) unit;

                    Value retValue = returnStmt.getOp();
                    if(retValue==null) continue;
                    if (retValue instanceof Constant) {
                        String str = retValue.toString();
                        addValue(methodToString, sootMethod, str);
                        LOGGER.info("266:New Method To String: " + sootMethod.getName() + ":" + str + ";" + methodToString.get(sootMethod).toString());
                        mstrs.add(str);
                        if(sootMethod.getName().contains("buildGenericInfo")){
                            LOGGER.info("Now!!!");
                        }
                        //return mstrs;
                    }
                    else if (retValue instanceof Local) {
                        Local local = (Local) retValue;
                        for (Unit u : body.getUnits()) {
                            if(VisitedUnits.contains(u)) continue;
                            //LOGGER.info("254: UNIT: " + u.toString());
                            VisitedUnits.add(u);
                            if(u == unit) break;
                            if (u instanceof AssignStmt) {
                                AssignStmt assignStmt = (AssignStmt) u;
                                if (assignStmt.getLeftOp().equals(local)) {
                                    Value rightOp = assignStmt.getRightOp();
                                    if (rightOp instanceof StaticFieldRef) {
                                        StaticFieldRef fieldRef = (StaticFieldRef) rightOp;
                                        if (fieldRef.getField() == null || fieldRef.getField().getDeclaringClass() == null) {
                                            continue;
                                        }
                                        if (fieldRef.getField().getDeclaringClass().isApplicationClass()) {
                                            SootField field = fieldRef.getField();
                                            if (!methodToFieldString.containsKey(sootMethod)) {
                                                methodToFieldString.put(sootMethod, field.toString());
                                                LOGGER.info("New Method ret field: " + sootMethod + ":" + field);
                                                if (fieldToString.containsKey(field.toString())) {
                                                    List<String> strs = fieldToString.get(field.toString());
                                                    if (!methodToString.containsKey(sootMethod))
                                                        methodToString.put(sootMethod, strs);
                                                    else {
                                                        for (String item : strs) {
                                                            addValue(methodToString, sootMethod, item);
                                                        }
                                                    }
                                                    mstrs.addAll(strs);
                                                    LOGGER.info("238:New Method To String: " + sootMethod.getName() + ":" + strs + ";" + methodToString.get(sootMethod).toString());
                                                    //return mstrs;
                                                }
                                            }
                                        }
                                    } else if (rightOp instanceof InstanceFieldRef) {
                                        //TODO
                                        //addValue(methodToString, sootMethod, "$"+((InstanceFieldRef) rightOp).getField().getName());
                                        //return mstrs;
                                    } else if (rightOp instanceof InvokeExpr) {
                                        List<String> strs = new ArrayList<>();
                                        SootMethod nextMethod = ((InvokeExpr) rightOp).getMethod();
                                        if(isCommonType(nextMethod.getReturnType())) {
                                            if (!nextMethod.toString().equals(sootMethod.toString())) { // Is Recursion function
                                                unsolvedMethodString.add(sootMethod.getSignature());
                                                if(unsolvedMethodString.contains(nextMethod.getSignature())) return mstrs;
                                                strs = GetMethodToString(nextMethod);
                                            }
                                        }
                                        if (!strs.isEmpty()) {
                                            mstrs.addAll(strs);
                                            for (String item : mstrs) {
                                                addValue(methodToString, sootMethod, item);
                                            }
                                            LOGGER.info("255:New Method To String: " + sootMethod.getName() + ":" + mstrs + ";" + methodToString.get(sootMethod).toString());
                                            if(sootMethod.getName().contains("isTitleOptional")){
                                                LOGGER.info("get the end.");
                                            }
                                        }
                                        //return mstrs;
                                    }
                                    else if(rightOp instanceof Constant){
                                        addValue(methodToString, sootMethod, rightOp.toString());
                                        mstrs.add(rightOp.toString());
                                        LOGGER.info("314:New Method To String: "  + sootMethod.getName() + ":" + mstrs + ";" + methodToString.get(sootMethod).toString());
                                    }
                                    else if(rightOp instanceof BinopExpr){
                                        //TODO
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
        return mstrs;
    }

    public static boolean isCommonType(Type type) {
        if (type.equals(IntType.v()) ||
                type.equals(BooleanType.v()) ||
                type.equals(FloatType.v()) ||
                type.equals(DoubleType.v()) ||
                type.equals(LongType.v()) ||
                type.equals(ShortType.v()) ||
                type.equals(ByteType.v())) {
            return true;
        }

        if (type instanceof RefType) {
            RefType refType = (RefType) type;
            String className = refType.getClassName();
            return className.equals("java.lang.String") ||
                    className.equals("java.util.Map") ||
                    className.equals("java.util.List") ||
                    className.equals("java.util.Set") ||
                    className.equals("java.util.Collection");
        }

        return false;
    }

    private static boolean isStandardLibraryClass(SootClass sc) {
        String packageName = sc.getPackageName();
        return packageName.startsWith("java.") || packageName.startsWith("javax.")
                || packageName.startsWith("android.") || packageName.startsWith("kotlin.");
    }

    public static <K, V> K getKeyByValue(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static HashMap<SootMethod,List<String>> getMethodToString(){
        return methodToString;
    }

    public static HashMap<SootMethod,String> getMethodToFieldString(){
        return methodToFieldString;
    }

    public static HashMap<String,List<String>> getFieldToString(){
        return fieldToString;
    }

    public static <K, V> void addValue(Map<K, List<V>> map, K key, V value) {
        map.computeIfAbsent(key, k -> new ArrayList<>());
        List<V> values = map.get(key);
        if(!values.contains(value)){
            values.add(value);
        }
    }

    public static <T> List<T> clone(List<T> ls) {
        return new ArrayList<T>(ls);
    }
}
