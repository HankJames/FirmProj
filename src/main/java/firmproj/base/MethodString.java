package firmproj.base;
import firmproj.graph.CallGraph;
import firmproj.objectSim.AbstractClz;
import firmproj.objectSim.HashmapClz;
import firmproj.objectSim.SimulateUtil;
import firmproj.utility.FirmwareRelated;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.tagkit.ConstantValueTag;
import soot.tagkit.Tag;
import soot.util.Chain;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodString {
    private static final Logger LOGGER = LogManager.getLogger(MethodString.class);

    public static final HashMap<SootMethod, String> methodToFieldString = new HashMap<>();

    public static final HashMap<String, List<String>> fieldToString = new HashMap<>();

    public static final HashMap<SootMethod, List<String>> methodToString = new HashMap<>();

    public static final HashMap<SootField, List<MethodParamInvoke>> FieldSetByMethodInvoke = new HashMap<>();// field set by Param Invoke

    public static final HashMap<SootMethod, HashSet<SootField>> methodToInvokeSetFields = new HashMap<>();

    public static final HashMap<SootMethod, MethodParamInvoke> methodReturnParamInvoke = new HashMap<>();

    public static final HashMap<String, List<SootField>> classWithOuterInnerFields = new HashMap<>();

    public static final HashMap<SootClass, HashMap<String, List<String>>> classMaybeCache = new HashMap<>();

    public static final HashMap<SootClass, HashMap<SootMethod, HashMap<Integer, SootField>>> allClzWithSetFieldMethod = new HashMap<>();

    public static void init(){
        Chain<SootClass> classes = Scene.v().getClasses();
        for (SootClass sootClass : classes) {
            String headString = sootClass.getName().split("\\.")[0];
            if (headString.contains("android") || headString.contains("kotlin") || headString.contains("java"))
                continue;
            //LOGGER.info(sootClass.getName().toString());
            if(isStandardLibraryClass(sootClass)) continue;
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
        MergeMethodAndFieldString();
        ReplaceFieldString();

        LOGGER.info("SIZE: " + methodToFieldString.size() + " "+ fieldToString.size() + " " + allClzWithSetFieldMethod.size() + " " + classWithOuterInnerFields);
        LOGGER.warn("MethodToField: {}\nMethodToString: {}\nFieldToString: {}\nallClzWithSetFieldMethod: {}\nFieldSetByMethodInvoke: {}\nMethodToInvokeSetFields: {}\nclassWithOuterInnerFields: {}\nmethodReturnParamInvoke: {}", methodToFieldString, methodToString, fieldToString,
                allClzWithSetFieldMethod, FieldSetByMethodInvoke, methodToInvokeSetFields,classWithOuterInnerFields, methodReturnParamInvoke);

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
                        Object obj = SimulateUtil.getConstant(retValue);
                        if(obj != null) {
                            String str = obj.toString();
                            if (str.equals("\"\"") || str.equals("null") || str.equals("0") || str.isEmpty()) continue;
                            addValue(methodToString, sootMethod, str);
                            TryAddStringToInterface(sootMethod);
                            LOGGER.info("266:New Method To String: " + sootMethod.getSignature() + ":" + str + ";" + methodToString.get(sootMethod).toString());
                            mstrs.add(str);
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
                                    if (rightOp instanceof FieldRef) {
                                        FieldRef fieldRef = (FieldRef) rightOp;
                                        if (fieldRef.getField() == null || fieldRef.getField().getDeclaringClass() == null) {
                                            continue;
                                        }
                                        if (fieldRef.getField().getDeclaringClass().isApplicationClass()) {
                                            SootField field = fieldRef.getField();
                                            if (!methodToFieldString.containsKey(sootMethod)) {
                                                methodToFieldString.put(sootMethod, field.toString());
                                                LOGGER.info("New Method ret field: " + sootMethod.getSignature() + ":" + field);
                                                if (fieldToString.containsKey(field.toString())) {
                                                    List<String> strs = fieldToString.get(field.toString());
                                                    if (!methodToString.containsKey(sootMethod))
                                                        methodToString.put(sootMethod, strs);
                                                    else {
                                                        for (String item : strs) {
                                                            addValue(methodToString, sootMethod, item);
                                                        }
                                                    }
                                                    for(String str: strs) {
                                                        if(!mstrs.contains(str))
                                                            mstrs.add(str);
                                                    }
                                                    TryAddStringToInterface(sootMethod);
                                                    LOGGER.info("238:New Method To String: " + sootMethod.getSignature() + ":" + strs + ";" + methodToString.get(sootMethod).toString());
                                                    //return mstrs;
                                                }
                                            }
                                        }
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
                                            for(String str: strs) {
                                                if(!mstrs.contains(str))
                                                    mstrs.add(str);
                                            }
                                            for (String item : mstrs) {
                                                addValue(methodToString, sootMethod, item);
                                            }
                                            TryAddStringToInterface(sootMethod);
                                            LOGGER.info("255:New Method To String: " + sootMethod.getSignature() + ":" + mstrs + ";" + methodToString.get(sootMethod).toString());
                                        }
                                        //return mstrs;
                                    }
                                    else if(rightOp instanceof Constant){
                                        Object obj = SimulateUtil.getConstant(rightOp);
                                        if(obj!=null) {
                                            addValue(methodToString, sootMethod, rightOp.toString());
                                            mstrs.add(rightOp.toString());
                                            TryAddStringToInterface(sootMethod);
                                            LOGGER.info("314:New Method To String: " + sootMethod.getSignature() + ":" + mstrs + ";" + methodToString.get(sootMethod).toString());
                                        }
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
        visitedMethod.clear();
        Chain<SootClass> classes = Scene.v().getClasses();
        for (SootClass sootClass : classes) {
            HashMap<SootMethod, HashMap<Integer, SootField>> clzResult;
            if(isStandardLibraryClass(sootClass)) continue;
            clzResult = GetMethodSetField(sootClass);
            if(!clzResult.isEmpty()){
                //LOGGER.info("GetMethodSetField: {}--{}", sootClass, clzResult);
            }
        }
        return ;
    }

    public static HashMap<SootMethod, HashMap<Integer, SootField>> GetMethodSetField(SootClass clz){
        String headString = clz.getName().split("\\.")[0];
        if (headString.contains("android") || headString.contains("kotlin") || headString.contains("java") || headString.contains("thingClips"))
            return new HashMap<>();

        if(!allClzWithSetFieldMethod.containsKey(clz))
            allClzWithSetFieldMethod.put(clz, new HashMap<>());
        else{
            return allClzWithSetFieldMethod.get(clz);
        }
        HashMap<SootMethod, HashMap<Integer, SootField>> result = allClzWithSetFieldMethod.get(clz);
        if(clz.getName().equals("com.haihe.lianlianujia.constant.Constants2"))
            LOGGER.warn("GOTIT");
        processStaticField(clz);
        for(SootMethod sootMethod : clz.getMethods()){
            HashMap<Integer, SootField> fieldWithParam = GetSetField(sootMethod);
            if(!fieldWithParam.isEmpty()) {
                result.put(sootMethod, fieldWithParam);
                //LOGGER.info("220: GetSetField: {}--{}--{}",clz, sootMethod, fieldWithParam);
            }
            else result.remove(sootMethod);

        }
        return result;
    }

    public static void TryAddStringToInterface(SootMethod method){
        for(SootClass interfaceClz: method.getDeclaringClass().getInterfaces()){
            try{
                SootMethod interfaceMethod = interfaceClz.getMethodByName(method.getName());
                addValue(methodToString, interfaceMethod, methodToString.get(method));
                LOGGER.info("222: Add String To Interface: {} -> {} - {}", methodToString.get(method), interfaceMethod.getSignature(), method.getSignature());
            }
            catch (Exception ignore){}
        }
    }

    public static void processStaticField(SootClass sc){
        for (SootField field : sc.getFields()) {
            if (field.isStatic()) {
                for (Tag tag : field.getTags()) {
                    if (tag instanceof ConstantValueTag) {
                        ConstantValueTag constantValueTag = (ConstantValueTag) tag;
                        String str = constantValueTag.getConstant().toString();
                        addValue(fieldToString, field.toString(), str);
                    }
                }
            }
        }
    }



    public static void MergeMethodAndFieldString(){
        for(SootMethod method : methodToFieldString.keySet()){
            String fieldStr = methodToFieldString.get(method);
            if(fieldToString.containsKey(fieldStr))
                addValue(methodToString, method, fieldToString.get(fieldStr));
        }
    }

    public static void ReplaceFieldString(){
        HashSet<MethodParamInvoke> visited = new HashSet<>();
        HashSet<MethodParamInvoke> allMethodParamInvoke = new HashSet<>();
        for(List<MethodParamInvoke> methodParamInvokes : FieldSetByMethodInvoke.values()){
            allMethodParamInvoke.addAll(methodParamInvokes);
        }
        allMethodParamInvoke.addAll(methodReturnParamInvoke.values());

        for(MethodParamInvoke methodParamInvoke : allMethodParamInvoke){
            if(visited.contains(methodParamInvoke)) continue;
            visited.add(methodParamInvoke);
            List<String> invokes = methodParamInvoke.InvokeMethodSig;
            if(invokes.isEmpty()) continue;
            int index = 0;
            for(String invoke : invokes) {
                if(invoke.isEmpty()) continue;
//                String tmpResult = invoke;
//                HashSet<String> fields = FirmwareRelated.matchFields(invoke);
//                if(fields.isEmpty()) continue;
//                for(String field : fields){
//                    if(fieldToString.containsKey(field)) {
//                        List<String> fieldStr = fieldToString.get(field);
//                        fieldStr.replaceAll(s -> s.replace(field, ""));
//                        tmpResult = tmpResult.replace(field, fieldStr.toString());
//                    }
//                }
                String tmpResult = retrieveAllfieldString(invoke, invoke);
                if(!tmpResult.equals(invoke)){
                    invokes.set(index, tmpResult);
                }
                index++;
            }
        }
    }

    public static String retrieveAllfieldString(String startField, String str){
        String tmpResult = str;
        HashSet<String> fields = FirmwareRelated.matchFields(str);
        if(fields.isEmpty()) return tmpResult;
        for(String field : fields){
            if(fieldToString.containsKey(field)) {
                List<String> fieldStr = fieldToString.get(field);

                if(fieldStr.toString().contains(startField)) return tmpResult; //ring
                fieldStr.replaceAll(s -> s.replace(field, ""));
                String resultStr = retrieveAllfieldString(field, fieldStr.toString());

                tmpResult = tmpResult.replace(field, resultStr);
            }
        }
        return tmpResult;
    }


    public static HashMap<Integer, SootField> GetSetField(SootMethod sootMethod){

        if(sootMethod.getSignature().contains("void getNewVersion(java.lang.String)"))
            LOGGER.info("TARGET");
        SootClass clz = sootMethod.getDeclaringClass();
        HashMap<SootMethod, HashMap<Integer, SootField>> clzMethod = allClzWithSetFieldMethod.get(clz);

        if(!clzMethod.containsKey(sootMethod))
            clzMethod.put(sootMethod, new HashMap<>());
        else
            return clzMethod.get(sootMethod);

        HashMap<Integer, SootField> fieldWithParam = clzMethod.get(sootMethod);
        if(isStandardLibraryClass(clz)) return fieldWithParam;

        HashMap<Value, List<Integer>> localFromParam = new HashMap<>();
        HashMap<Value, MethodParamInvoke> localFromParamInvoke = new HashMap<>();
        HashMap<Value, SootField> localToFieldMap = new HashMap<>();
        HashMap<Value, List<String>> localToString = new HashMap<>();
        HashMap<Value, AbstractClz> localToCLz = new HashMap<>();
        HashMap<Value, List<List<String>>> localArray = new HashMap<>();

        if(visitedMethod.contains(sootMethod.getSignature())) return fieldWithParam;
        visitedMethod.add(sootMethod.getSignature());

        Body body = null;


        try {
            body = sootMethod.retrieveActiveBody();
        } catch (Exception e) {
            //LOGGER.error("Could not retrieved the active body {} because {}", sootMethod, e.getLocalizedMessage());
        }
        //LOGGER.info("THE METHOD NOW: {}", sootMethod);
        if(body != null) {

            for (Unit unit : body.getUnits()) {
                if(unit.toString().contains("$r1 = staticinvoke <com.gooclient.anycam.activity.settings.update.MyHttp: java.lang.String encryption(java.lang.String,java.lang.String)>"))
                    LOGGER.info("ggg");
                Stmt stmt = (Stmt) unit;
                if(stmt instanceof IdentityStmt){
                    Value rightOp = ((IdentityStmt) stmt).getRightOp();
                    Value leftOp = ((IdentityStmt) stmt).getLeftOp();
                    if(rightOp instanceof ParameterRef){
                        int index = ((ParameterRef) rightOp).getIndex();
                        addValue(localFromParam,leftOp, index);
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
                        else{
                            localArray.remove(leftOp);
                        }
                    }
                    else if(leftOp instanceof Local){
                        // localArray Value assigned new value
                        //todo right is fieldREF, right is staticinvoke,.

                        if(localFromParam.containsKey(rightOp)) {
                            List<Integer> keys = localFromParam.get(rightOp);
                            localFromParam.put(leftOp, keys);
                        }

                        if(localFromParamInvoke.containsKey(rightOp)){
                            localFromParamInvoke.put(leftOp, localFromParamInvoke.get(rightOp));
                            localArray.remove(leftOp);
                            localToString.remove(leftOp);
                            localFromParam.remove(leftOp); //update the value
                        }

                        if(rightOp instanceof Local){
                            if(localToCLz.containsKey(rightOp))
                                localToCLz.put(leftOp, localToCLz.get(rightOp));
                        }
                        else if(rightOp instanceof CastExpr){
                            Value op = ((CastExpr) rightOp).getOp();
                            if(localToCLz.containsKey(op))
                                localToCLz.put(leftOp, localToCLz.get(op));
                            if(localFromParam.containsKey(op)){
                                if(!leftOp.equals(op))
                                    localFromParam.remove(leftOp);
                                addValue(localFromParam, leftOp, localFromParam.get(op));
                            }
                        }
                        else if(rightOp instanceof ArrayRef) {
                            ArrayRef arrayRef = (ArrayRef) rightOp;
                            Value arrayBase = arrayRef.getBase();
                            localArray.remove(leftOp);

                            Object obj = SimulateUtil.getConstant(arrayRef.getIndex());
                            if (obj != null) {
                                int arrayIndex = (Integer) obj;
                                if (localArray.containsKey(arrayBase)) {
                                    try{
                                        List<List<String>> array = localArray.get(arrayBase);
                                        if(array.size() - 1 < arrayIndex) continue;
                                        List<String> arrayValue = array.get(arrayIndex);
                                        if(arrayValue.isEmpty()) continue;
                                        String invokeSigStr = null;
                                        List<Integer> arrayID = getArrayFieldIndex(arrayValue);
                                        if (!arrayID.isEmpty()) {
                                            if(arrayValue.get(0).startsWith("MethodParamInvoke")){
                                                invokeSigStr = getInvokeSigOfString(arrayValue.get(0));
                                                List<String> tmp = new ArrayList<>(arrayValue);
                                                tmp.set(0, invokeSigStr);
                                                MethodParamInvoke methodParamInvoke = new MethodParamInvoke(sootMethod, arrayID, tmp);
                                                localFromParamInvoke.put(leftOp, methodParamInvoke);

                                                localToString.remove(leftOp);
                                                localFromParam.remove(leftOp);
                                                localToCLz.remove(leftOp);
                                            }
                                            else {
                                                List<Integer> params = new ArrayList<>();
                                                for (int fromId : arrayID) {
                                                    if (localFromParam.containsKey(arrayBase)) {
                                                        if (localFromParam.get(arrayBase).contains(fromId)) {
                                                            addValue(params, fromId);
                                                        }
                                                    }
                                                }
                                                if(arrayValue.get(0).startsWith("<") && !params.isEmpty()){
                                                    MethodParamInvoke methodParamInvoke = new MethodParamInvoke(sootMethod, params, arrayValue);
                                                    localFromParamInvoke.put(leftOp, methodParamInvoke);

                                                    localToString.remove(leftOp);
                                                    localFromParam.remove(leftOp);
                                                    localToCLz.remove(leftOp);
                                                }
                                                else{
                                                    localFromParam.remove(leftOp);
                                                    addValue(localFromParam, leftOp, params);
                                                }
                                            }
                                        }
                                        else {
                                            localFromParam.remove(leftOp); //update the value
                                            localToString.put(leftOp, arrayValue);
                                        }

                                    }
                                    catch (Exception e){
                                        //LOGGER.error("325: array index over, {},{},{},{}", clz, sootMethod, unit, localArray.get(arrayBase));
                                    }
                                }
                                else if(localFromParamInvoke.containsKey(arrayBase)){
                                    MethodParamInvoke methodParamInvoke = new MethodParamInvoke(localFromParamInvoke.get(arrayBase));
                                    methodParamInvoke.addMethodInvoke(arrayRef.toString());
                                    localFromParamInvoke.put(leftOp, methodParamInvoke);
                                    localFromParam.remove(leftOp); //update the value
                                }
                            }
                        } else if (rightOp instanceof NewExpr) {
                            SootClass sootClz = ((NewExpr) rightOp).getBaseType().getSootClass();
                            String clzName = ((NewExpr) rightOp).getBaseType().getClassName();
                            if(MethodLocal.isCommonClz(clzName)) {
                                AbstractClz abstractClz = MethodLocal.CreateCommonClz(sootClz, sootMethod);
                                localToCLz.put(leftOp, abstractClz);

                                localFromParamInvoke.remove(leftOp);
                                localArray.remove(leftOp);
                                localToString.remove(leftOp);
                                localFromParam.remove(leftOp);
                                //TODO cla simulate
                            }

                        } else if(rightOp instanceof Constant){
                            Object obj = SimulateUtil.getConstant(rightOp);
                            if(obj != null) {
                                addValue(localToString,leftOp,obj.toString());
                                localFromParamInvoke.remove(leftOp);

                                SootField sootField = localToFieldMap.remove(leftOp);
                                AbstractClz abstractClz = localToCLz.remove(leftOp);
                                if(abstractClz != null && sootField != null){
                                    abstractClz.solve();
                                    if(abstractClz.isSolved())
                                        addValue(fieldToString, sootField.toString(), abstractClz.toString());
                                }
                                localFromParam.remove(leftOp); //update the value
                                localToCLz.remove(leftOp);

                            }
                        }
                        else if(rightOp instanceof FieldRef){
                            SootField field = ((FieldRef) rightOp).getField();
                            if(fieldToString.containsKey(field.toString())) {
                                addValue(localToString, leftOp, fieldToString.get((field.toString())));
                            }
                            else{
                                localToString.put(leftOp, new ArrayList<>(List.of(field.toString())));
                            }
                            localFromParamInvoke.remove(leftOp);
                            localFromParam.remove(leftOp); //update the value
                            localArray.remove(leftOp);

                            SootField sootField = localToFieldMap.remove(leftOp);
                            AbstractClz abstractClz = localToCLz.remove(leftOp);
                            if(abstractClz != null && sootField != null){
                                abstractClz.solve();
                                if(abstractClz.isSolved())
                                    addValue(fieldToString, sootField.toString(), abstractClz.toString());
                            }

                            if(field.getType() instanceof RefType){
                                RefType refType = (RefType) field.getType();
                                if(refType.getClassName().contains("Map")) {
                                    localToFieldMap.put(leftOp, field);
                                    //Todo UPDATE FIELD STRING
                                    HashmapClz hashmapClz = new HashmapClz(refType.getSootClass(), sootMethod);
                                    localToCLz.put(leftOp, hashmapClz);

                                    localToString.remove(leftOp);
                                }
                            }

                        }
                        else if(rightOp instanceof InvokeExpr){
                            try{
                                SootMethod method = ((InvokeExpr) rightOp).getMethod();
                                if(methodToFieldString.containsKey(method) || MethodString.getMethodToString().containsKey(method)) {
                                    if (methodToString.containsKey(method)) {

                                        localToString.put(leftOp, MethodString.getMethodToString().get(method));
                                    } else if (methodToFieldString.containsKey(method)) {
                                        if (fieldToString.containsKey(methodToFieldString.get(method))) {
                                            List<String> tmp = fieldToString.get(methodToFieldString.get(method));
                                            localToString.put(leftOp, tmp);
                                            addValue(methodToString, method, tmp);
                                        } else {
                                            localToString.put(leftOp, new ArrayList<>(List.of(methodToFieldString.get(method))));
                                        }
                                    }
                                    localFromParam.remove(leftOp);
                                    localFromParamInvoke.remove(leftOp);
                                    localToCLz.remove(leftOp);
                                }
                                else if(rightOp instanceof InstanceInvokeExpr){
                                    Value rightBase = ((InstanceInvokeExpr) rightOp).getBase();

                                    HashMap<Value, List<String>>  paramValueWithStrings = new HashMap<>();
                                    HashMap<Integer, List<String>> paramIndexWithStrings = new HashMap<>();

                                    int index = 0;
                                    for(Value arg : ((InstanceInvokeExpr) rightOp).getArgs()){
                                        if(localToCLz.containsKey(arg)){
                                            AbstractClz abstractClz = localToCLz.get(arg);
                                            abstractClz.solve();
                                            if(abstractClz.isSolved()){
                                                paramValueWithStrings.put(arg, new ArrayList<>(List.of(abstractClz.toString())));

                                            }
                                            if(localFromParam.containsKey(arg)){
                                                addValue(localFromParam, rightBase, localFromParam.get(arg));
                                            }
                                        }
                                        else if(arg instanceof Constant){
                                            Object obj = SimulateUtil.getConstant(arg);
                                            if(obj != null){
                                                String objString = obj.toString();
                                                paramValueWithStrings.put(arg, new ArrayList<>(List.of(objString)));
                                            }
                                        }
                                        else if(localArray.containsKey(arg)){
                                            if(localFromParam.containsKey(arg)){
                                               addValue(localFromParam, rightBase, localFromParam.get(arg));
                                            }
                                            paramValueWithStrings.put(arg, new ArrayList<>(List.of(localArray.get(arg).toString())));
                                        }
                                        else if(localToString.containsKey(arg) ){
                                            paramValueWithStrings.put(arg, localToString.get(arg));
                                        }
                                        else if(localFromParamInvoke.containsKey(arg)){
                                            addValue(localFromParam, rightBase, localFromParamInvoke.get(arg).param);
                                            MethodParamInvoke methodParamInvoke = new MethodParamInvoke(localFromParamInvoke.get(arg));
                                            paramValueWithStrings.put(arg, new ArrayList<>(List.of(methodParamInvoke.InvokeMethodSig.toString())));
                                        }
                                        else if(localFromParam.containsKey(arg)){
                                            addValue(localFromParam, rightBase, localFromParam.get(arg));
                                            paramValueWithStrings.put(arg, new ArrayList<>(List.of("$" + localFromParam.get(arg))));
                                        }
                                        if(paramValueWithStrings.containsKey(arg))
                                            paramIndexWithStrings.put(index, paramValueWithStrings.get(arg));
                                        index++;

                                    }
                                    if(localToCLz.containsKey(rightBase)){
                                        ValueContext valueContext = new ValueContext(sootMethod, unit, paramValueWithStrings);
                                        AbstractClz abstractClz = localToCLz.get(rightBase);
                                        abstractClz.addValueContexts(valueContext);
                                        abstractClz.solve();
                                        if(localToFieldMap.containsKey(rightBase) && abstractClz.isSolved())
                                            addValue(fieldToString, localToFieldMap.get(rightBase).toString(), new ArrayList<>(List.of(abstractClz.toString())));

                                        localToCLz.put(leftOp, localToCLz.get(rightBase));
                                        localFromParamInvoke.remove(leftOp);
                                        localArray.remove(leftOp);

                                        if(!leftOp.equals(rightBase)) {// assignment , leftOp = rightBase
                                            localFromParam.remove(leftOp);
                                            if (localFromParam.containsKey(rightBase)) {
                                                addValue(localFromParam, leftOp, localFromParam.get(rightBase));
                                            }
                                        }

                                    } else if(localFromParam.containsKey(rightBase)){
                                        String clzName = method.getDeclaringClass().getName();
                                        if(!clzName.contains("ViewBinding") && !clzName.contains("Dialog") && !clzName.contains("panel") && !clzName.contains("widget")) {
                                            localFromParamInvoke.put(leftOp, new MethodParamInvoke(sootMethod, localFromParam.get(rightBase), method.getSignature() + paramIndexWithStrings));
                                            localArray.remove(leftOp);
                                            localFromParam.remove(leftOp);
                                            localToString.remove(leftOp);
                                            localToCLz.remove(leftOp);
                                            localToFieldMap.remove(leftOp);
                                        }
                                    } else if(localFromParamInvoke.containsKey(rightBase)){
                                        MethodParamInvoke methodParamInvokeBase = localFromParamInvoke.get(rightBase);
                                        MethodParamInvoke methodParamInvoke = new MethodParamInvoke(sootMethod, methodParamInvokeBase.param, methodParamInvokeBase.InvokeMethodSig);
                                        if(localFromParam.containsKey(rightBase))
                                            addValue(methodParamInvoke.param, localFromParam.get(rightBase));
                                        methodParamInvoke.addMethodInvoke(method.getSignature() + paramIndexWithStrings);
                                        localFromParamInvoke.put(leftOp, methodParamInvoke);
                                        localArray.remove(leftOp);
                                        localToString.remove(leftOp);
                                        localFromParam.remove(leftOp); //update the value
                                        localToCLz.remove(leftOp);
                                        localToFieldMap.remove(leftOp);
                                    }
                                }
                                else if(rightOp instanceof StaticInvokeExpr){
                                    StaticInvokeExpr staticInvokeExpr = (StaticInvokeExpr) rightOp;
                                    HashMap<Integer, List<String>>  paramIndexWithStrings = new HashMap<>();
                                    List<Integer> fromParamArg = new ArrayList<>();
                                    int index = 0;
                                    for(Value arg : staticInvokeExpr.getArgs()){
                                        if(localToCLz.containsKey(arg)){
                                            AbstractClz abstractClz = localToCLz.get(arg);
                                            abstractClz.solve();
                                            if(abstractClz.isSolved()){
                                                paramIndexWithStrings.put(index, new ArrayList<>(List.of(abstractClz.toString())));
                                            }
                                            if(localFromParam.containsKey(arg)){
                                                 addValue(fromParamArg, localFromParam.get(arg));
                                            }
                                        }
                                        else if(arg instanceof Constant){
                                            Object obj = SimulateUtil.getConstant(arg);
                                            if(obj != null){
                                                String objString = obj.toString();
                                                paramIndexWithStrings.put(index, new ArrayList<>(List.of(objString)));
                                            }
                                        }
                                        else if(localArray.containsKey(arg)){
                                            if(localFromParam.containsKey(arg)){
                                                addValue(fromParamArg, localFromParam.get(arg));
                                            }
                                            paramIndexWithStrings.put(index, new ArrayList<>(List.of(localArray.get(arg).toString())));
                                        }
                                        else if(localToString.containsKey(arg) ){
                                            paramIndexWithStrings.put(index, localToString.get(arg));
                                        }
                                        else if(localFromParamInvoke.containsKey(arg)){
                                            addValue(fromParamArg, localFromParamInvoke.get(arg).param);
                                            MethodParamInvoke methodParamInvoke = new MethodParamInvoke(localFromParamInvoke.get(arg));
                                            paramIndexWithStrings.put(index, new ArrayList<>(methodParamInvoke.InvokeMethodSig));
                                        }
                                        else if(localFromParam.containsKey(arg)){
                                            addValue(fromParamArg, localFromParam.get(arg));
                                            paramIndexWithStrings.put(index, new ArrayList<>(List.of("$" + localFromParam.get(arg))));
                                        }
                                        index++;
                                    }
                                    List<String> returnResult = MethodLocal.getStaticInvokeReturn(staticInvokeExpr, paramIndexWithStrings);
                                    MethodParamInvoke methodParamInvoke;
                                    if(!returnResult.isEmpty()) {
                                        if(fromParamArg.isEmpty()) {
                                            localToString.put(leftOp, returnResult);
                                            localFromParamInvoke.remove(leftOp);
                                            localArray.remove(leftOp);
                                            localFromParam.remove(leftOp);
                                            continue;
                                        }
                                        else{
                                            methodParamInvoke = new MethodParamInvoke(sootMethod, fromParamArg, returnResult);
                                        }
                                    }
                                    else{
                                        methodParamInvoke = new MethodParamInvoke(sootMethod, fromParamArg, method.getSignature() + paramIndexWithStrings);
                                        if(method.getReturnType() instanceof ArrayType){
                                            ArrayType arrayType = (ArrayType) method.getReturnType();
                                            if(arrayType.toString().toLowerCase().contains("byte")) continue;
                                            int LEN = 10;
                                            List<List<String>> arrayList = new ArrayList<>();
                                            List<String> arrayValue = new ArrayList<>();
                                            if(fromParamArg.isEmpty())
                                                arrayValue.addAll(methodParamInvoke.InvokeMethodSig);
                                            else {
                                                arrayValue.add(methodParamInvoke.toString());
                                                localFromParam.put(leftOp, fromParamArg);
                                            }
                                            int i = 0;
                                            while(i < LEN) {
                                                List<String> tmpValue = new ArrayList<>(arrayValue);
                                                tmpValue.add("get(" + i + ")");
                                                arrayList.add(tmpValue);
                                                i++;
                                            }
                                            localArray.put(leftOp, arrayList);

                                            localFromParamInvoke.remove(leftOp);
                                            localToString.remove(leftOp);
                                            localToCLz.remove(leftOp);
                                            localToFieldMap.remove(leftOp);
                                            //LOGGER.info("297: New local Array: {}, {}, {},{}", stmt, leftOp, arrayList, index);

                                        }
                                    }
                                    if(!(method.getReturnType() instanceof ArrayType)) {
                                        localFromParamInvoke.put(leftOp, methodParamInvoke);
                                        localArray.remove(leftOp);
                                        localToString.remove(leftOp);
                                        localToCLz.remove(leftOp);
                                        localToFieldMap.remove(leftOp);
                                        localFromParam.remove(leftOp);
                                    }
                                    //update the value
                                }

                            }
                            catch (Exception ignored){};

                        }
                    }
                    else if(leftOp instanceof FieldRef){
                        FieldRef leftFiledRef = (FieldRef)leftOp;
                        SootField leftField = leftFiledRef.getField();

                        if(localToCLz.containsKey(rightOp) && !localFromParam.containsKey(rightOp)){
                            AbstractClz abstractClz = localToCLz.get(rightOp);
                            abstractClz.solve();
                            if(abstractClz.isSolved())
                                addValue(fieldToString, leftField.toString(), abstractClz.toString());
                        }
                        else if(localToString.containsKey(rightOp)){
                            addValue(fieldToString,leftField.toString(),localToString.get(rightOp));
                            LOGGER.info("331:New Field with String: {}:{}; {}", leftField, localToString.get(rightOp), fieldToString.get(leftField.toString()).toString());
                            //localToString.remove(rightOp);
                            if(methodToFieldString.containsValue(leftField.toString())){
                                SootMethod method = getKeyByValue(methodToFieldString, leftField.toString());
                                if(method !=null) {
                                    methodToString.put(method, fieldToString.get(leftField.toString()));
                                    TryAddStringToInterface(method);
                                    LOGGER.info("335:New Method To String: " + method.getSignature() + ":" + fieldToString.get(leftField.toString()) + ";" + methodToString.get(method).toString());
                                }
                            }
                        }
                        else if(rightOp instanceof Constant){
                            if(rightOp instanceof NullConstant) continue;
                            Object obj = SimulateUtil.getConstant(rightOp);
                            if(obj == null) continue;
                            if(obj.toString().isEmpty()) continue;
                            addValue(fieldToString, leftField.toString(), obj.toString());
                            //LOGGER.info("New Field with String: " + leftField + ":" + rightOp + ";" + fieldToString.get(leftField.toString()).toString());
                            if (methodToFieldString.containsValue(leftField.toString())) {
                                SootMethod method = getKeyByValue(methodToFieldString, leftField.toString());
                                if(method != null){
                                    addValue(methodToString, method, rightOp.toString());
                                    TryAddStringToInterface(method);
                                    LOGGER.info("141:New Method To String: " + method.getSignature() + ":" + rightOp + ";" + methodToString.get(method).toString());
                                }
                            }
                        }

                        if(localFromParam.containsKey(rightOp)){
                            List<Integer> indexes = localFromParam.get(rightOp);
                            for(Integer index: indexes) {
                                if(leftField.toString().contains("panel") || leftField.toString().contains("ViewBinding")) continue;
                                fieldWithParam.put(index, leftField);
                            }
                            MethodParamInvoke methodParamInvoke = new MethodParamInvoke(sootMethod, indexes);
                            if(localToCLz.containsKey(rightOp)){
                                methodParamInvoke.addMethodInvoke(localToCLz.get(rightOp).toString());
                            }
                            addMethodInvokeSetField(leftField, methodParamInvoke);

                            if(!indexes.isEmpty() && leftField.getType() instanceof RefType ){
                                String clzName = ((RefType) leftField.getType()).getClassName();
                                if(CallGraph.outerInnerClasses.containsKey(clzName)){
                                    List<SootField> allFields = classWithOuterInnerFields.get(clzName);
                                    for(SootField field : allFields){
                                        if(isStandardLibraryClass(field.getDeclaringClass())) continue;
                                        if(field.toString().contains("panel") || field.toString().contains("ViewBinding") || field.toString().contains("this$0")) continue;
                                        addMethodInvokeSetField(field, methodParamInvoke);
                                    }
                                }
                            }

                        }

                        if(localFromParamInvoke.containsKey(rightOp)){
                            MethodParamInvoke methodParamInvoke = localFromParamInvoke.get(rightOp);
                            addMethodInvokeSetField(leftField, methodParamInvoke);
                            if(leftField.getType() instanceof RefType){ //check class instance field
                                String clzName = ((RefType) leftField.getType()).getClassName();
                                if(CallGraph.outerInnerClasses.containsKey(clzName)){
                                    List<SootField> allFields = classWithOuterInnerFields.get(clzName);
                                    for(SootField field : allFields){
                                        addMethodInvokeSetField(field, new MethodParamInvoke(methodParamInvoke));
                                    }
                                }
                            }
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
                                try {
                                    List<String> arrayValue = localArray.get(base).get(arrayIndex);

                                    if (rightOp instanceof Constant) {
                                        Object obj = SimulateUtil.getConstant(rightOp);
                                        if(obj != null)
                                            addValue(arrayValue, obj.toString());
                                    }
                                    else if(localToString.containsKey(rightOp)){
                                        addValue(arrayValue, localToString.get(rightOp));
                                    }
                                    else if(localToCLz.containsKey(rightOp)){
                                        AbstractClz abstractClz = localToCLz.get(rightOp);
                                        abstractClz.solve();
                                        if(abstractClz.isSolved()) {
                                            arrayValue.clear();
                                            arrayValue.add(abstractClz.toString());
                                        }
                                        if(localFromParam.containsKey(rightOp)){
                                            addValue(localFromParam, base, localFromParam.get(rightOp));
                                        }
                                    }
                                    else if (localFromParam.containsKey(rightOp)) {
                                        arrayValue.clear();
                                        for(int i : localFromParam.get(rightOp)) {
                                            arrayValue.add("$[" + i + "]");
                                            addValue(localFromParam, base, i);
                                        }
                                    }
                                    else if (localFromParamInvoke.containsKey(rightOp)){
                                        MethodParamInvoke methodParamInvoke = new MethodParamInvoke(localFromParamInvoke.get(rightOp));
                                        addValue(localFromParam, base, methodParamInvoke.param);
                                        arrayValue.clear();
                                        arrayValue.addAll(new ArrayList<>(methodParamInvoke.InvokeMethodSig));

                                    }
                                }
                                catch (Exception e){
                                    LOGGER.error("348: array index over, {},{},{},{}", clz, sootMethod, unit, localArray.get(base));
                                }
                            }
                        }
                        else {
                            //TODO
                        }
                    }
                }
                else if(stmt instanceof InvokeStmt){
                    //LOGGER.info("291: Invoke: {}", stmt);
                    boolean isNeedInvoke = false;
                    Value base = null;
                    if(stmt.getInvokeExpr() instanceof InstanceInvokeExpr)
                        base = ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase();
                    HashMap<Integer, Constant> paramWithConstant = new HashMap<>();
                    ValueContext valueContext = new ValueContext(sootMethod, unit);
                    //TODO check method solved.
                    HashMap<Integer, Value> localToParam = new HashMap<>();
                    int index = 0;
                    for(Value param: stmt.getInvokeExpr().getArgs()){
                        if(param instanceof Constant){
                            paramWithConstant.put(index, (Constant) param);
                            //LOGGER.info("304: param constant: {}--{}-{}",stmt.getInvokeExpr(),param, index);
                            isNeedInvoke = true;
                        }else if (localToString.containsKey(param)){
                            isNeedInvoke = true;
                            localToParam.put(index, param);
                            paramWithConstant.put(index, null);
                            addValue(valueContext.getCurrentValues(), param, localToString.get(param));
                        }else if(localArray.containsKey(param)){
                            isNeedInvoke = true;
                            List<String> paramList = new ArrayList<>();
                            for (List<String> strs : localArray.get(param)) {
                                paramList.add(strs.toString());
                            }
                            localToParam.put(index, param);
                            localToString.put(param, paramList);
                            paramWithConstant.put(index, null);
                            addValue(valueContext.getCurrentValues(), param, localToString.get(param));
                        }
                        else if(localToCLz.containsKey(param)){
                            isNeedInvoke = true;
                            addValue(valueContext.getCurrentValues(), param, localToCLz.get(param).toString());
                            if(localFromParam.containsKey(param)){
                                if(base != null && localToCLz.containsKey(base)) {
                                    addValue(localFromParam, base, localFromParam.get(param));
                                }
                            }
                        }
                        else if(localFromParam.containsKey(param)) {
                            isNeedInvoke = true;
                            localToParam.put(index, param);
                            addValue(valueContext.getCurrentValues(), param, "$" + localFromParam.get(param));

                            if(base != null && localToCLz.containsKey(base)) {
                                addValue(localFromParam, base, localFromParam.get(param));
                            }
                            //LOGGER.info("301: localFromParam: {},{}", param, index);
                        } else if (localFromParamInvoke.containsKey(param)) {
                            isNeedInvoke = true;
                            localToParam.put(index, param);

                            if(base != null && localToCLz.containsKey(base)) {
                                addValue(valueContext.getCurrentValues(), param, localFromParamInvoke.get(param).InvokeMethodSig.toString());
                                addValue(localFromParam, base, localFromParamInvoke.get(param).param);
                            }
                            else{
                                addValue(valueContext.getCurrentValues(), param, localFromParamInvoke.get(param).toString());
                            }
                        }

                        index++;
                    }
                    if(isNeedInvoke) {
                        SootMethod invokeMethod = stmt.getInvokeExpr().getMethod();
                        HashMap<Integer, SootField> invokeResult;
                        if (invokeMethod.getDeclaringClass().equals(clz)) {
                            //LOGGER.info("311: Go to Invoke: {}", invokeMethod);
                            invokeResult = GetSetField(invokeMethod);
                            if(invokeResult.isEmpty()){
                                clzMethod.remove(invokeMethod);
                            }
                            //LOGGER.info("313: Get Invoke result: {}", invokeResult);
                        } else if(base != null && localToCLz.containsKey(base)){
                            invokeResult = null;
                            AbstractClz abstractClz = localToCLz.get(base);
                            abstractClz.addValueContexts(valueContext);
                            abstractClz.solve();
                            if(localToFieldMap.containsKey(base) && abstractClz.isSolved())
                                addValue(fieldToString, localToFieldMap.get(base).toString(), new ArrayList<>(List.of(abstractClz.toString())));
                        }  else {
                            HashMap<SootMethod, HashMap<Integer, SootField>> invokeClzResult;
                            //LOGGER.info("316: Go to Clz :{}", invokeMethod.getDeclaringClass().getName());
                            invokeClzResult = GetMethodSetField(invokeMethod.getDeclaringClass());
                            invokeResult = invokeClzResult.get(invokeMethod);
                            //LOGGER.info("319: Get Clz result:{}", invokeResult);
                        }
                        HashMap<SootField, List<MethodParamInvoke>> tempMethodInvoke = new HashMap<>();
                        //Check if invoke method has been analyzed, and merge the method with invoke method by param transfer.
                        if(methodToInvokeSetFields.containsKey(invokeMethod)){
                            for(SootField field : methodToInvokeSetFields.get(invokeMethod)){
                                List<MethodParamInvoke> methodParamInvokes = FieldSetByMethodInvoke.get(field);
                                for(MethodParamInvoke methodParamInvoke: methodParamInvokes) {
                                    if(!methodParamInvoke.sootMethod.equals(invokeMethod)) continue;
                                    for(Integer paramIndex : methodParamInvoke.param) {
                                        if(paramWithConstant.containsKey(paramIndex)){
                                            if(paramWithConstant.get(paramIndex) != null){
                                                Object obj = SimulateUtil.getConstant(paramWithConstant.get(paramIndex));
                                                if(obj != null)
                                                    addValue(methodParamInvoke.paramValue, paramIndex, obj.toString());
                                            }
                                        }
                                        if (localToParam.containsKey(paramIndex)) {
                                            Value localValue = localToParam.get(paramIndex);
                                            if (localFromParamInvoke.containsKey(localValue)) {
                                                MethodParamInvoke methodInvoke = localFromParamInvoke.get(localValue);
                                                methodInvoke.addMethodInvoke(methodParamInvoke.InvokeMethodSig);
                                                //todo more analysis. maybe del the original methodParamInvoke.
                                                addValue(tempMethodInvoke, field, methodInvoke);
                                                //LOGGER.warn("509: GetMethodInvokeSetField: {} -> {}", field, methodInvokeSetField.get(field).toString());
                                            }
                                            else if(localToString.containsKey(localValue)){
                                                addValue(methodParamInvoke.paramValue, paramIndex, localToString.get(localValue));
                                            }
                                            else if(localArray.containsKey(localValue)){
                                                addValue(methodParamInvoke.paramValue, paramIndex, localArray.get(localValue).toString());
                                            }
                                            else if(localToCLz.containsKey(localValue)){
                                                addValue(methodParamInvoke.paramValue, paramIndex, localToCLz.get(localValue).toString());
                                            }
                                            else {
                                                List<Integer> indexes = localFromParam.get(localValue);
                                                if (indexes.isEmpty()) continue;
                                                String clzName = sootMethod.getDeclaringClass().getName();
                                                if (!clzName.contains("ViewBinding") && !clzName.contains("Dialog") && !clzName.contains("panel") && !clzName.contains("widget")) {
                                                    MethodParamInvoke newMethodParamInvoke = new MethodParamInvoke(sootMethod, indexes, methodParamInvoke.InvokeMethodSig);
                                                    addValue(tempMethodInvoke, field, newMethodParamInvoke);
                                                    //LOGGER.warn("515: GetMethodInvokeSetField: {} -> {}", field, methodInvokeSetField.get(field).toString());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        for(SootField field :tempMethodInvoke.keySet()){
                            for(MethodParamInvoke methodParamInvoke: tempMethodInvoke.get(field))
                                addMethodInvokeSetField(field, methodParamInvoke);
                        }

                        //invoke Result only contains that param to field directly
                        if(invokeResult != null){
                            if (!invokeResult.isEmpty() && !localToParam.isEmpty()){
                                for(Integer id: localToParam.keySet()){
                                    if(invokeResult.containsKey(id)) {
                                        SootField field = invokeResult.get(id);
                                        if (localFromParamInvoke.containsKey(localToParam.get(id))) {
                                            MethodParamInvoke methodParamInvoke = localFromParamInvoke.get(localToParam.get(id));
                                            addMethodInvokeSetField(field, methodParamInvoke);
                                            // LOGGER.warn("532: GetMethodInvokeSetField: {} -> {}", field, methodInvokeSetField.get(field).toString());
                                        } else if(localFromParam.containsKey(localToParam.get(id))){
                                            List<Integer> indexes = localFromParam.get(localToParam.get(id));
                                            for (Integer paramIndex : indexes) {
                                                if (field.toString().contains("panel") || field.toString().contains("ViewBinding"))
                                                    continue;
                                                fieldWithParam.put(paramIndex, field);
                                            }
                                            //LOGGER.info("309: GetSetField: {}--{}--{}",clz, invokeMethod, fieldWithParam);
                                        }
                                    }

                                }

                            }
                            if(!paramWithConstant.isEmpty()){
                                //LOGGER.info("331: Constant param: Method: {}-- Result: {}-- constant: {}", invokeMethod,invokeResult, paramWithConstant);
                                for(Integer constIndex: paramWithConstant.keySet()) {
                                    if (invokeResult.containsKey(constIndex)) {
                                        SootField field = invokeResult.get(constIndex);
                                        if (paramWithConstant.get(constIndex) == null) {
                                            for (String str : localToString.get(localToParam.get(constIndex))) {
                                                if(str.equals(field.toString())) continue;
                                                addValue(fieldToString, field.toString(), str);
                                                //LOGGER.info("434: New local param To field: {} <= {}", field.toString(), str);
                                            }
                                        } else {
                                            Object constObj = SimulateUtil.getConstant(paramWithConstant.get(constIndex));
                                            if (field != null && constObj != null) {
                                                if (!fieldToString.containsKey(field.toString()))
                                                    fieldToString.put(field.toString(), new ArrayList<>());
                                                List<String> fieldValues = fieldToString.get(field.toString());
                                                List<Integer> arrayID = getArrayFieldIndex(fieldValues);
                                                if (!arrayID.isEmpty()) {
                                                    for(int fromId : arrayID) {
                                                        if (constIndex.equals(fromId)) {
                                                            fieldValues.replaceAll(s -> s.replace("$[" + fromId + "]", constObj.toString()));
                                                            //LOGGER.info("445: New Array Param To field: {}, {}", fieldValues, arrayID);
                                                        }
                                                    }
                                                } else {
                                                    addValue(fieldToString, field.toString(), constObj.toString());
                                                    if (methodToFieldString.containsValue(field.toString())) {
                                                        SootMethod method = getKeyByValue(methodToFieldString, field.toString());
                                                        if (method != null) {
                                                            methodToString.put(method, fieldToString.get(field.toString()));
                                                            TryAddStringToInterface(method);
                                                            //LOGGER.info("488:New Method To String: " + method.getSignature() + ":" + fieldToString.get(field.toString()) + ";" + methodToString.get(method).toString());
                                                        }
                                                    }
                                                }
                                                //LOGGER.info("320: New Field with String: " + field + ":" + constObj + ";" + fieldToString.get(field.toString()).toString());
                                            }
                                        }
                                    }
                                }
                            }
                        }else if(index == 2 && !paramWithConstant.isEmpty()){

                            boolean maybeCache = false;
                            String[] str = new String[2];
                            int num = 0;
                            Constant ob0 = paramWithConstant.get(0);

                            if(ob0!=null) {
                                maybeCache = true;
                                Object ob = SimulateUtil.getConstant(ob0);
                                if(ob!= null)
                                    str[0] = ob.toString();
                                else
                                    maybeCache = false;
                            }else continue;

                            if(paramWithConstant.containsKey(1)) {
                                Constant ob1 = paramWithConstant.get(1);
                                if (ob1 == null)
                                    str[1] = localToString.get(localToParam.get(1)).toString();
                                else {
                                    Object ob = SimulateUtil.getConstant(ob1);
                                    if (ob != null)
                                        str[1] = ob.toString();
                                    else
                                        maybeCache = false;
                                }
                            }
                            else if(localToParam.containsKey(1)){
                                MethodParamInvoke methodParamInvoke = localFromParamInvoke.get(localToParam.get(1));
                                if(methodParamInvoke != null && methodToFieldString.containsKey(methodParamInvoke.sootMethod)){
                                    maybeCache = true;
                                    str[1] = methodParamInvoke.toString();
                                }
                                else maybeCache = false;
                            }


                            if(maybeCache) {
                                SootClass invokeClz = invokeMethod.getDeclaringClass();
                                if(!isStandardLibraryClass(invokeClz)) {
                                    if (!classMaybeCache.containsKey(invokeClz))
                                        classMaybeCache.put(invokeClz, new HashMap<>());
                                    addValue(classMaybeCache.get(invokeClz), str[0], str[1]);
                                    LOGGER.warn("[ClassMaybeCache]: {}", classMaybeCache.get(invokeClz));
                                }
                            }
                        }
                    }
                }
                else if (stmt instanceof ReturnStmt) {
                    Value returnOP = ((ReturnStmt) stmt).getOp();
                    if(localFromParamInvoke.containsKey(returnOP)){
                        //TODO methodReturnParamInvoke.
                        if(methodReturnParamInvoke.containsKey(sootMethod)) {
                            if(methodReturnParamInvoke.get(sootMethod).InvokeMethodSig.toString().length() < localFromParamInvoke.get(returnOP).InvokeMethodSig.toString().length())
                                methodReturnParamInvoke.put(sootMethod, localFromParamInvoke.get(returnOP));
                        }
                        if(isCommonType(sootMethod.getReturnType())){
                            String returnStr = localFromParamInvoke.get(returnOP).toString();
                            if(isFirmRelatedUrl(returnStr))
                                LOGGER.debug("FIND URL: {} ,from method {} ", returnStr, sootMethod);
                        }
                    }
                    else if(localFromParam.containsKey(returnOP) && localToCLz.containsKey(returnOP)){
                        AbstractClz abstractClz = localToCLz.get(returnOP);
                        abstractClz.solve();
                        if(abstractClz.isSolved()) {
                            MethodParamInvoke methodParamInvoke = new MethodParamInvoke(sootMethod, localFromParam.get(returnOP), abstractClz.toString());
                            if(methodReturnParamInvoke.containsKey(sootMethod)) {
                                if(methodReturnParamInvoke.get(sootMethod).InvokeMethodSig.toString().length() < methodParamInvoke.InvokeMethodSig.toString().length())
                                    methodReturnParamInvoke.put(sootMethod, methodParamInvoke);
                            }
                        }
                    }
                    if(localToString.containsKey(returnOP)){
                        if(isCommonType(sootMethod.getReturnType())){
                            String returnStr = localToString.get(returnOP).toString();
                            if(isFirmRelatedUrl(returnStr))
                                LOGGER.debug("FIND local URL: {} ,from method {} ", returnStr, sootMethod);
                        }
                    }
                }
            }
        }
        return fieldWithParam;
    }

    public static boolean isFirmRelatedUrl(String strParam){
        int i = 0;
        String str = strParam.toLowerCase();
        if(str.contains(".com") || str.contains("url") || str.contains(".cn") || str.contains("://"))
            i++;
        if(str.contains("firmware") || str.contains("upgrade") || str.contains("update") || (str.contains("version") && (str.contains("match") || str.contains("new")))){
            i++;
        }
        if(str.contains(".bin") || str.contains(".fw") || str.contains(".ota") || str.contains(".img"))
            i = i + 2;
        if(str.contains("appupdate") || str.contains("app_update") || str.contains("!"))
            i--;

        return i>1;
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
                    className.contains("Map") ||
                    className.equals("java.util.List") ||
                    className.equals("java.util.Set") ||
                    className.contains("Collection") ||
                    className.toLowerCase().contains("url");
        }

        return false;
    }

    public static boolean isStandardLibraryClass(SootClass sc) {
        String className = sc.getName();
        for(String str: library) {
            if (className.toLowerCase().contains(str))
                return true;
        }
        return className.startsWith("java.") || className.startsWith("javax.")
                || className.startsWith("android.") || className.startsWith("kotlin.") || className.startsWith("rx.")
                 || className.startsWith("okhttp3") || className.startsWith("retrofit2.");
    }

    public static List<String> library = List.of("bumptech", "com.xiaomi", "com.android","com.google", "com.amazonaws", "bumptech", "thingClips", "okio", "alipay.sdk", "com.huawei", "com.tencent",
            "org.apache","io.reactivex", "facebook", "crashsdk", "cn.jpush", "cn.jiguang", "eventbus", "bouncycastle", "spongycastle", "OpenSSL", "com.stripe", "com.blankj", "com.alibaba", "com.greenrobot",
            "com.amap.","com.scwang","mikephil", "dialog","umeng", "panel", "airbnb", "freshchat", "webview", "widget", "com.baidu", ".video.", "com.meizu", "cn.sharesdk", "greenrobot", ".push.", ".channel.", ".android.",".crash.");

    public static <K, V> K getKeyByValue(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void addMethodInvokeSetField(SootField field, MethodParamInvoke methodParamInvoke){
        boolean needAdd = true;
        if(isStandardLibraryClass(field.getDeclaringClass())) return;
        if(field.toString().contains("panel") || field.toString().contains("ViewBinding") || field.toString().contains("this$0")) return;
        if(!FieldSetByMethodInvoke.containsKey(field))
            FieldSetByMethodInvoke.put(field, new ArrayList<>());
        for(MethodParamInvoke m : FieldSetByMethodInvoke.get(field))
            if(m.equals(methodParamInvoke)) {
                needAdd = false;
                break;
            }
        if(needAdd) {
            FieldSetByMethodInvoke.get(field).add(methodParamInvoke);
            SootMethod method = methodParamInvoke.sootMethod;
            if(!methodToInvokeSetFields.containsKey(method)){
                methodToInvokeSetFields.put(method, new HashSet<>());
            }
            methodToInvokeSetFields.get(method).add(field);
        }
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

    public static List<Integer> getArrayFieldIndex(List<String> values){
        String regex = "\\$\\[(\\d+)]|param=\\[(\\d+)(?:,\\s*(\\d+))*]";
        List<Integer> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(values.toString());

        while (matcher.find()) {
            // 将匹配的结果添加到列表中
            for (int i = 1 ; i <= matcher.groupCount(); i++) {
                if(matcher.group(i) != null) {
                    String str = matcher.group(i);
                    addValue(result, Integer.parseInt(str));
                }
            }
        }
        return result;
    }

    public static String getInvokeSigOfString(String str){
        String regex = "InvokeMethodSig=\\[(.*?)]}(, |$)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);

        if (matcher.find()) {
            // 将匹配的结果添加到列表中
            return matcher.group(1);
        }
        return null;
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

    public static <T> List<T> clone(List<T> ls) {
        return new ArrayList<T>(ls);
    }
}
