package firmproj.base;
import firmproj.objectSim.SimulateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.util.Chain;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodString {
    private static final Logger LOGGER = LogManager.getLogger(MethodString.class);

    private static final HashMap<SootMethod, String> methodToFieldString = new HashMap<>();

    private static final HashMap<String, List<String>> fieldToString = new HashMap<>();

    private static final HashMap<SootMethod, List<String>> methodToString = new HashMap<>();

    private static final HashMap<SootClass, HashMap<SootMethod, HashMap<Integer, SootField>>> allClzWithSetFieldMethod = new HashMap<>();

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
                                else{
                                    //TODO Static hashmap
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

        GetAllMethodSetField();

        LOGGER.info("SIZE: " + methodToFieldString.size() + " "+ fieldToString.size() + " " + allClzWithSetFieldMethod.size());
        //TODO If there is a Method to field But no field to string, just Method to field string "$fieldName";
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
                                    if (rightOp instanceof FieldRef) {
                                        FieldRef fieldRef = (FieldRef) rightOp;
                                        if (fieldRef.getField() == null || fieldRef.getField().getDeclaringClass() == null) {
                                            continue;
                                        }
                                        if (fieldRef.getField().getDeclaringClass().isApplicationClass()) {
                                            SootField field = fieldRef.getField();
                                            if (!methodToFieldString.containsKey(sootMethod)) {
                                                methodToFieldString.put(sootMethod, field.toString());
                                                //LOGGER.info("New Method ret field: " + sootMethod + ":" + field);
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

    public static void GetAllMethodSetField(){
        //TODO
        Chain<SootClass> classes = Scene.v().getClasses();
        for (SootClass sootClass : classes) {
            HashMap<SootMethod, HashMap<Integer, SootField>> clzResult;
            clzResult = GetMethodSetField(sootClass);
            if(!clzResult.isEmpty()){
                //LOGGER.info("GetMethodSetField: {}--{}", sootClass, clzResult);
            }
        }
        return ;
    }

    public static HashMap<SootMethod, HashMap<Integer, SootField>> GetMethodSetField(SootClass clz){
        String headString = clz.getName().split("\\.")[0];
        if (headString.contains("android") || headString.contains("kotlin") || headString.contains("java"))
            return new HashMap<>();

        if(!allClzWithSetFieldMethod.containsKey(clz))
            allClzWithSetFieldMethod.put(clz, new HashMap<>());
        else{
            return allClzWithSetFieldMethod.get(clz);
        }
        HashMap<SootMethod, HashMap<Integer, SootField>> result = allClzWithSetFieldMethod.get(clz);
        for(SootMethod sootMethod : clz.getMethods()){
            HashMap<Integer, SootField> fieldWithParam = GetSetField(sootMethod);
            if(!fieldWithParam.isEmpty()) {
                result.put(sootMethod, fieldWithParam);
                //LOGGER.info("220: GetSetField: {}--{}--{}",clz, sootMethod, fieldWithParam);
            }

        }
        return result;
    }

    public static HashMap<Integer, SootField> GetSetField(SootMethod sootMethod){

        SootClass clz = sootMethod.getDeclaringClass();
        HashMap<SootMethod, HashMap<Integer, SootField>> clzMethod = allClzWithSetFieldMethod.get(clz);

        if(!clzMethod.containsKey(sootMethod))
            clzMethod.put(sootMethod, new HashMap<>());
        else
            return clzMethod.get(sootMethod);

        HashMap<Integer, SootField> fieldWithParam = clzMethod.get(sootMethod);
        if(isStandardLibraryClass(clz)) return fieldWithParam;

        HashMap<Integer, Value> localFromParam = new HashMap<>();
        HashMap<Value, List<String>> localToString = new HashMap<>();

        //if(visitedMethod.contains(sootMethod.getSignature())) return mstrs;
        //visitedMethod.add(sootMethod.getSignature());

        Body body = null;


        try {
            body = sootMethod.retrieveActiveBody();
        } catch (Exception e) {
            //LOGGER.error("Could not retrieved the active body {} because {}", sootMethod, e.getLocalizedMessage());
        }
        //LOGGER.info("THE METHOD NOW: {}", sootMethod);
        if(body != null) {
            HashMap<Value, List<List<String>>> localArray = new HashMap<>();
            for (Unit unit : body.getUnits()) {
                Stmt stmt = (Stmt) unit;
                if(stmt instanceof IdentityStmt){
                    Value rightOp = ((IdentityStmt) stmt).getRightOp();
                    Value leftOp = ((IdentityStmt) stmt).getLeftOp();
                    if(rightOp instanceof ParameterRef){
                        int index = ((ParameterRef) rightOp).getIndex();
                        localFromParam.put(index, leftOp);
                       // LOGGER.info("275: localFromParam : {}-{}", leftOp, index);
                    }
                }
                else if(stmt instanceof AssignStmt){
                    Value rightOp = ((AssignStmt) stmt).getRightOp();
                    Value leftOp = ((AssignStmt) stmt).getLeftOp();
                    if(rightOp instanceof NewArrayExpr){
                        NewArrayExpr arrayExpr = (NewArrayExpr) rightOp;
                        Integer index = (Integer) SimulateUtil.getConstant(arrayExpr.getSize());
                        if(index != null && index > 0 && index < 100){
                            List<List<String>> arrayList = new ArrayList<>();
                            int i = 0;
                            while(i < index) {
                                arrayList.add(new ArrayList<>());
                                i++;
                            }
                            localArray.put(leftOp, arrayList);
                            //LOGGER.info("297: New local Array: {}, {}, {},{}", stmt, leftOp, arrayList, index);
                        }
                    }
                    else if(leftOp instanceof Local){
                        if(localFromParam.containsValue(rightOp)) {
                            List<Integer> keys = getAllKeyByValue(localFromParam, rightOp);
                            for(int key: keys)
                                localFromParam.put(key, leftOp);
                        }
                        if(rightOp instanceof ArrayRef) {
                            ArrayRef arrayRef = (ArrayRef) rightOp;
                            Value arrayBase = arrayRef.getBase();
                            Object obj = SimulateUtil.getConstant(arrayRef.getIndex());
                            if (obj != null) {
                                int arrayIndex = (Integer) obj;
                                if (localArray.containsKey(arrayBase)) {
                                    try{
                                    List<String> arrayValue = localArray.get(arrayBase).get(arrayIndex);
                                    int arrayID = getArrayFieldIndex(arrayValue);
                                    if (arrayID > -1) {
                                        int fromId = Integer.parseInt(arrayValue.get(arrayID).substring(1));
                                        localFromParam.put(fromId, leftOp);
                                    } else {
                                        localToString.put(leftOp, arrayValue);
                                    }}
                                    catch (Exception e){
                                        //LOGGER.error("325: array index over, {},{},{},{}", clz, sootMethod, unit, localArray.get(arrayBase));
                                    }
                                }
                            }
                        }
                        else if(rightOp instanceof Constant){
                            Object obj = SimulateUtil.getConstant(rightOp);
                            if(obj != null) {
                                if (!localToString.containsKey(leftOp))
                                    localToString.put(leftOp, new ArrayList<>());
                                localToString.get(leftOp).add(obj.toString());
                            }
                        }
                        if(localArray.containsKey(leftOp)){ // localArray Value assigned new value
                            localArray.remove(leftOp);
                            continue;
                            //todo right is fieldREF, right is staticinvoke,.
                        }

                    }
                    else if(leftOp instanceof FieldRef){
                        FieldRef leftFiledRef = (FieldRef)leftOp;
                        SootField leftField = leftFiledRef.getField();
                        if(localFromParam.containsValue(rightOp)){
                            List<Integer> indexes = getAllKeyByValue(localFromParam, rightOp);
                            for(Integer index: indexes)
                                fieldWithParam.put(index, leftField);
                        }
                        if(localArray.containsKey(rightOp)){
                            if(!fieldToString.containsKey(leftField.toString()))
                                fieldToString.put(leftField.toString(), new ArrayList<>());
                            addArrayStringTo(localArray.get(rightOp), fieldToString.get(leftField.toString()));
                            LOGGER.info("337: New Field with Array String: {}==>{}--{}", localArray.get(rightOp), leftField, fieldToString.get(leftField.toString()));
                        }
                    }
                    else if(leftOp instanceof ArrayRef){
                        ArrayRef arrayRef = (ArrayRef) leftOp;
                        Integer arrayIndex = (Integer) SimulateUtil.getConstant(arrayRef.getIndex());
                        Value base = arrayRef.getBase();
                        if(localArray.containsKey(base)) {
                            if (arrayIndex != null) {
                                try {List<String> arrayValue = localArray.get(base).get(arrayIndex);

                                if (rightOp instanceof Constant) {
                                    Object obj = SimulateUtil.getConstant(rightOp);
                                    if(obj != null)
                                        arrayValue.add(obj.toString());
                                }
                                if (localFromParam.containsValue(rightOp)) {
                                    arrayValue.add("$"+getKeyByValue(localFromParam, rightOp));
                                    localFromParam.put(getKeyByValue(localFromParam, rightOp), base);
                                }}
                                catch (Exception e){
                                    LOGGER.error("348: array index over, {},{},{},{}", clz, sootMethod, unit, localArray.get(base));
                                }
                            }
                        }
                    }
                }
                else if(stmt instanceof InvokeStmt){
                    //LOGGER.info("291: Invoke: {}", stmt);
                    boolean isNeedInvoke = false;
                    HashMap<Integer, Constant> paramWithConstant = new HashMap<>();
                    //TODO check method solved.
                    HashMap<Integer, Value> localToParam = new HashMap<>();
                    int index = 0;
                    for(Value param: stmt.getInvokeExpr().getArgs()){
                        if(localFromParam.containsValue(param)) {
                            isNeedInvoke = true;
                            localToParam.put(index, param);
                            //LOGGER.info("301: localFromParam: {},{}", param, index);
                        }
                        else if (localToString.containsKey(param)){
                            isNeedInvoke = true;
                            localToParam.put(index, param);
                            paramWithConstant.put(index, null);
                        }
                        if(param instanceof Constant){
                            paramWithConstant.put(index, (Constant) param);
                            //LOGGER.info("304: param constant: {}--{}-{}",stmt.getInvokeExpr(),param, index);
                            isNeedInvoke = true;
                        }
                        index++;
                    }

                    if(isNeedInvoke) {
                        SootMethod invokeMethod = stmt.getInvokeExpr().getMethod();
                        HashMap<Integer, SootField> invokeResult;
                        if (invokeMethod.getDeclaringClass().equals(clz)) {
                            //LOGGER.info("311: Go to Invoke: {}", invokeMethod);
                            invokeResult = GetSetField(invokeMethod);
                            //LOGGER.info("313: Get Invoke result: {}", invokeResult);
                        } else {
                            HashMap<SootMethod, HashMap<Integer, SootField>> invokeClzResult;
                            //LOGGER.info("316: Go to Clz :{}", invokeMethod.getDeclaringClass().getName());
                            invokeClzResult = GetMethodSetField(invokeMethod.getDeclaringClass());
                            invokeResult = invokeClzResult.get(invokeMethod);
                            //LOGGER.info("319: Get Clz result:{}", invokeResult);
                        }
                        if(invokeResult != null){
                            if (!invokeResult.isEmpty() && !localToParam.isEmpty()){
                                for(Integer id: localToParam.keySet()){
                                    if(invokeResult.containsKey(id)){
                                        List<Integer> indexes = getAllKeyByValue(localFromParam, localToParam.get(id));
                                        for(Integer paramIndex : indexes)
                                            fieldWithParam.put(paramIndex, invokeResult.get(id));
                                        //LOGGER.info("309: GetSetField: {}--{}--{}",clz, invokeMethod, fieldWithParam);
                                    }
                                }
                            }
                            if(!paramWithConstant.isEmpty()){
                                //LOGGER.info("331: Constant param: Method: {}-- Result: {}-- constant: {}", invokeMethod,invokeResult, paramWithConstant);
                                for(Integer constIndex: paramWithConstant.keySet()){
                                    if(invokeResult.containsKey(constIndex)){
                                        SootField field = invokeResult.get(constIndex);
                                        if (paramWithConstant.get(constIndex) == null) {
                                            for (String str : localToString.get(localToParam.get(constIndex))) {
                                                addValue(fieldToString, field.toString(), str);
                                                LOGGER.info("434: New Array Constant To field: {}, {}", field.toString(), str);
                                            }
                                        } else {
                                            Object constObj = SimulateUtil.getConstant(paramWithConstant.get(constIndex));
                                            if (field != null && constObj != null) {
                                                if(!fieldToString.containsKey(field.toString()))
                                                    fieldToString.put(field.toString(), new ArrayList<>());
                                                List<String> fieldValues = fieldToString.get(field.toString());
                                                int arrayID = getArrayFieldIndex(fieldValues);
                                                if (arrayID > -1) {
                                                    int fromId = Integer.parseInt(fieldValues.get(arrayID).substring(1));
                                                    if (constIndex.equals(fromId)) {
                                                        fieldValues.set(arrayID, constObj.toString());
                                                        LOGGER.info("445: New Array Param To field: {}, {}", fieldValues, arrayID);
                                                    }
                                                } else addValue(fieldToString, field.toString(), constObj.toString());
                                                LOGGER.info("320: New Field with String: " + field + ":" + constObj + ";" + fieldToString.get(field.toString()).toString());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return fieldWithParam;

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

    public static boolean isStandardLibraryClass(SootClass sc) {
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

    public static <K, V> List<K> getAllKeyByValue(Map<K, V> map, V value) {
        List<K> result = new ArrayList<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public static int getArrayFieldIndex(List<String> values){
        String regex = "^\\$[1-9]\\d*$|^\\$0$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher;
        int i = 0;
        for(String value: values) {
            matcher = pattern.matcher(value);
            if(matcher.matches())
                return i;
            i++;
        }
        return -1;
    }

    public static void addArrayStringTo(List<List<String>> arrayString, List<String> value){
        int max = 0;
        for(List<String> list: arrayString){
            if(list.size() > max)
                max = list.size();
        }
        for(int i=0; i< max; i++){
            for(List<String> list: arrayString){
                if(i<list.size()){
                    value.add(list.get(i));
                }
                else{
                    value.add("");
                }
            }
        }
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
