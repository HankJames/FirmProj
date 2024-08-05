package firmproj.utility;

import firmproj.base.MethodParamInvoke;
import firmproj.base.MethodString;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.StaticInvokeExpr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class LLMQuery {
    public static final List<QueryJson> queries = new ArrayList<>();
    private static final int RETRIEVE_STEP = 2;

    public static QueryJson generate(MethodParamInvoke methodParamInvoke){
        return new QueryJson();
    }

    public static QueryJson generate(SootMethod sootMethod, HashMap<Integer, List<String>> paramValues){
        QueryJson queryJson = new QueryJson();
        SootClass sootClass = sootMethod.getDeclaringClass();
        if(MethodString.isStandardLibraryClass(sootClass)) return queryJson;

        List<List<String>> parameterValues = flattenParamValue(sootMethod, paramValues);
        queryJson.setTargetClass(sootClass.getName());
        queryJson.setTargetMethodSubsSig(sootMethod.getSubSignature());
        queryJson.setParameterValues(parameterValues);

        List<String> methodSig = retrieveMethod(sootMethod);
        queryJson.setRelatedMethodsSig(methodSig);
        //queryJson.doGenerate();

        queries.add(queryJson);
        return queryJson;
    }

    public static QueryJson generateHttp(SootMethod sootMethod){
        QueryJson queryJson = new QueryJson();
        SootClass sootClass = sootMethod.getDeclaringClass();

        queryJson.setTargetClass(sootClass.getName());
        queryJson.setTargetMethodSubsSig(sootMethod.getSubSignature());
        queryJson.setParameterValues(new ArrayList<>());

        List<String> methodSig = retrieveMethod(sootMethod);
        queryJson.setRelatedMethodsSig(methodSig);
        queryJson.setHttp(true);
        queryJson.doGenerate();

        queries.add(queryJson);
        return queryJson;
    }


    public static List<List<String>> flattenParamValue(SootMethod sootMethod, HashMap<Integer, List<String>> paramValues){
        List<List<String>> result = new ArrayList<>();
        int num = sootMethod.getParameterCount();
        for(int i=0; i < num; i++){
            if(paramValues.containsKey(i)){
                result.add(paramValues.get(i));
            }
            else{
                result.add(new ArrayList<>());
            }
        }
        return result;
    }

    public static List<String> retrieveMethod(SootMethod method){
        return retrieveMethod(method, 0);
    }

    public static List<String> retrieveMethod(SootMethod method, int step){
        if(step > RETRIEVE_STEP) return new ArrayList<>();
        Body body = method.getActiveBody();
        SootClass sootClass = method.getDeclaringClass();
        HashSet<String> result = new HashSet<>();
        for(Unit unit : body.getUnits()){
            if(unit instanceof AssignStmt){
                Value rightOp = ((AssignStmt) unit).getRightOp();
                if(rightOp instanceof InvokeExpr){
                    SootMethod invokeMethod = ((InvokeExpr) rightOp).getMethod();
                    String invokeMethodSig = invokeMethod.getSignature();
                    String sigLower = invokeMethodSig.toLowerCase();
                    SootClass declaringClz = invokeMethod.getDeclaringClass();
                    if(!MethodString.isStandardLibraryClass(declaringClz)){
                        if(sootClass.equals(declaringClz)){
                            result.add(invokeMethodSig);
                            result.addAll(retrieveMethod(invokeMethod, step + 1));
                        }
                        else if(rightOp instanceof StaticInvokeExpr){
                            if(sigLower.contains("log:") || sigLower.contains("log(") || sigLower.contains(".log.")) continue;
                            result.add(invokeMethodSig);
                            if(!sootClass.equals(declaringClz))
                                result.addAll(retrieveMethod(invokeMethod, step + 1));
                        }
                        else if(sigLower.contains("encode") || sigLower.contains("decode") || sigLower.contains("crypt") || sigLower.contains("util")) {
                            result.add(invokeMethodSig);
                            if (!sootClass.equals(declaringClz))
                                result.addAll(retrieveMethod(invokeMethod, step + 1));
                        }
                    }
                }
            }
            else if(unit instanceof InvokeStmt){

            }
        }
        return new ArrayList<>(result);
    }



}
