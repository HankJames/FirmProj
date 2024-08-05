package firmproj.client;

import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.NeExpr;
import soot.jimple.NewExpr;
import soot.jimple.ReturnStmt;

import java.util.HashMap;
import java.util.List;

public class ConverterFactory implements AbstractFactory{
    public SootClass factoryClass;
    public ConverterClass requestBodyConverter;
    public ConverterClass responseBodyConverter;
    public static final String REQUEST_BODY_CONVERTER = "retrofit2.Converter requestBodyConverter";
    public static final String RESPONSE_BODY_CONVERTER = "retrofit2.Converter responseBodyConverter";

    public ConverterFactory(){}

    public ConverterFactory(SootClass clz){
        this.factoryClass = clz;
    }

    public void init(){
        List<SootMethod> methods = factoryClass.getMethods();
        for(SootMethod sootMethod : methods){
            if (!sootMethod.isConcrete())
                continue;
            if(sootMethod.getSubSignature().contains(REQUEST_BODY_CONVERTER)){
                SootClass sootClass = getConvertClass(sootMethod);
                if(sootClass != null) {
                    requestBodyConverter = new ConverterClass(sootClass);
                    requestBodyConverter.init();
                }
            } else if (sootMethod.getSubSignature().contains(RESPONSE_BODY_CONVERTER)) {
                SootClass sootClass = getConvertClass(sootMethod);
                if(sootClass != null) {
                    responseBodyConverter = new ConverterClass(sootClass);
                    responseBodyConverter.init();
                }
            }
        }
    }

    public void generateQuery(){
        if(responseBodyConverter != null)
            responseBodyConverter.queryJson.doGenerate();
        if(requestBodyConverter != null)
            requestBodyConverter.queryJson.doGenerate();
    }

    private SootClass getConvertClass(SootMethod sootMethod){
        Body body = sootMethod.getActiveBody();
        HashMap<Value, SootClass> localToClass = new HashMap<>();
        for(Unit unit : body.getUnits()){
            if(unit instanceof AssignStmt){
                Value rightOp = ((AssignStmt) unit).getRightOp();
                Value leftOp = ((AssignStmt) unit).getLeftOp();
                if(rightOp instanceof NewExpr){
                    SootClass newSootClass = ((NewExpr) rightOp).getBaseType().getSootClass();
                    localToClass.put(leftOp, newSootClass);
                }
            }
            if(unit instanceof ReturnStmt){
                Value op = ((ReturnStmt) unit).getOp();
                return localToClass.getOrDefault(op, null);
            }
        }
        return null;
    }



    public void setRequestBodyConverter(ConverterClass requestBodyConverter) {
        this.requestBodyConverter = requestBodyConverter;
    }

    public void setFactoryClass(SootClass factoryClass) {
        this.factoryClass = factoryClass;
    }

    public void setResponseBodyConverter(ConverterClass responseBodyConverter) {
        this.responseBodyConverter = responseBodyConverter;
    }

    @Override
    public String toString() {
        return "ConverterFactory{" +
                "currentClass=" + factoryClass +
                ", requestBodyConverter=" + requestBodyConverter +
                ", responseBodyConverter=" + responseBodyConverter +
                '}';
    }
}
