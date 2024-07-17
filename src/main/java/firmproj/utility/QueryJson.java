package firmproj.utility;

import soot.*;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryJson {

    private String targetClass;
    private String targetMethodSubsSig;
    private List<List<String>> parameterValues = new ArrayList<>();
    private List<String> relatedMethodsSig = new ArrayList<>();

    public QueryJson(String className, String methodSubsSig, List<List<String>> parameters, List<String> relatedMethodsSig){
        this.targetClass = className;
        this.targetMethodSubsSig = methodSubsSig;
        this.parameterValues = parameters;
        this.relatedMethodsSig = relatedMethodsSig;
    }

    public QueryJson(){}

    public void doGenerate(){
        GenerateJson(targetClass, targetMethodSubsSig, parameterValues, relatedMethodsSig);
    }

    public static void test(){
        String clsName = "com.ruochan.dabai.netcore.encrypt.B.CustomResponseBodyConverter";
        String methodSubSig = "java.lang.Object convert(okhttp3.ResponseBody)";
        List<List<String>> params= new ArrayList<>(List.of(List.of("RequestBody")));
        List<String> otherMethods = new ArrayList<>(List.of("<com.ruochan.dabai.netcore.encrypt.B.CustomResponseBodyConverter: java.lang.String generateResponse(okhttp3.MediaType,java.lang.String)>","<com.ruochan.utils.CXAESUtil: java.lang.String Decrypt(java.lang.String,java.lang.String,java.lang.String)>"));
        GenerateJson(clsName, methodSubSig, params, otherMethods);
    }

    public static void GenerateJson(String targetClassName, String targetMethodSubSIg, List<List<String>> parameter, List<String> relatedMethods) {
        List<List<String>> parameters = new ArrayList<>(parameter);
        List<String> relatedMethodNames = new ArrayList<>(relatedMethods);
        // 添加相关方法名，例如：
        // relatedMethodNames.add("relatedMethod1");
        // relatedMethodNames.add("relatedMethod2");

        // 获取目标类
        SootClass targetClass = Scene.v().loadClassAndSupport(targetClassName);
        targetClass.setApplicationClass();

        // 找到目标方法
        SootMethod targetMethod = targetClass.getMethod(targetMethodSubSIg);

        // 获取目标方法的Body
        Body targetBody = targetMethod.retrieveActiveBody();

        // 获取目标方法所有的语句
        List<String> targetStatements = new ArrayList<>();
        for (Unit unit : targetBody.getUnits()) {
            targetStatements.add(unit.toString() + ";");
        }
        String targetCode = String.join(" ", targetStatements);

        // 获取相关方法的代码
        Map<String, String> relatedMethodsCode = new HashMap<>();
        for (String methodName : relatedMethodNames) {
            String className = parseClassNameFromSignature(methodName);
            String subSignature = parseSubSignatureFromSignature(methodName);
            System.out.println(subSignature);

            SootClass otherClass = Scene.v().loadClassAndSupport(className);
            SootMethod method = otherClass.getMethod(subSignature);

            Body methodBody = method.retrieveActiveBody();
            List<String> methodStatements = new ArrayList<>();
            for (Unit unit : methodBody.getUnits()) {
                methodStatements.add(unit.toString() + ";");
            }
            relatedMethodsCode.put(methodName, String.join(" ", methodStatements));
        }

        // 构建JSON对象
        JSONObject jsonObject = new JSONObject();
        JSONObject targetMethodObject = new JSONObject();

        targetMethodObject.put("Body", targetCode);
        targetMethodObject.put("Parameters", parameters);  // 可以为外部传入的参数列表

        JSONObject relatedMethodsObject = new JSONObject();
        for (Map.Entry<String, String> entry : relatedMethodsCode.entrySet()) {
            relatedMethodsObject.put(entry.getKey(), entry.getValue());
        }
        targetMethodObject.put("Related Methods", relatedMethodsObject);

        jsonObject.put(targetMethodSubSIg, targetMethodObject);

        // 输出JSON字符串到文件
        String fileName = "./Query/query_" +
                targetClassName +
                "_" +
                targetMethodSubSIg +
                ".json";

        try (FileWriter file = new FileWriter(fileName)) {
                file.write(jsonObject.toString(4)); // 格式化输出
                System.out.println("JSON内容已成功写入到" + fileName + "文件中！");
            } catch (IOException ignored) {
            }
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }

    public void setTargetMethodSubsSig(String targetMethodSubsSig) {
        this.targetMethodSubsSig = targetMethodSubsSig;
    }

    public void addParameters(List<String> params){
        this.parameterValues.add(params);
    }

    public void setParameterValues(List<List<String>> parameterValues) {
        this.parameterValues = parameterValues;
    }

    public void setRelatedMethodsSig(List<String> relatedMethodsSig) {
        this.relatedMethodsSig = relatedMethodsSig;
    }

    public void addRelatedMethodSig(String relatedMethodSig){
        if(!this.relatedMethodsSig.contains(relatedMethodSig))
            this.relatedMethodsSig.add(relatedMethodSig);
    }

    private static String parseClassNameFromSignature(String signature) {
        int start = signature.indexOf('<') + 1;
        int end = signature.indexOf(':');
        return signature.substring(start, end);
    }

    private static String parseSubSignatureFromSignature(String signature) {
        int start = signature.indexOf(':') + 2;
        int end = signature.indexOf('>');
        return signature.substring(start, end);
    }
}