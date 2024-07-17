package firmproj.client;

import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.List;

public class ConverterFactory{
    public SootClass currentClass;
    public ConverterClass requestBodyConverter;
    public ConverterClass responseBodyConverter;
    public static final String REQUEST_BODY_CONVERTER = "retrofit2.Converter requestBodyConverter(java.lang.reflect.Type, java.lang.'annotation'.Annotation[], java.lang.'annotation'.Annotation[], retrofit2.Retrofit)";
    public static final String RESPONSE_BODY_CONVERTER = "retrofit2.Converter responseBodyConverter(java.lang.reflect.Type, java.lang.'annotation'.Annotation[], retrofit2.Retrofit)";

    public ConverterFactory(){}

    public ConverterFactory(SootClass clz){
        this.currentClass = clz;
    }

    public void init(){
        List<SootMethod> methods = currentClass.getMethods();
        for(SootMethod sootMethod : methods){
            if (!sootMethod.isConcrete())
                continue;
            if(sootMethod.getSubSignature().equals(REQUEST_BODY_CONVERTER)){
                requestBodyConverter = new ConverterClass(sootMethod);
                requestBodyConverter.init();
            } else if (sootMethod.getSubSignature().equals(RESPONSE_BODY_CONVERTER)) {
                responseBodyConverter = new ConverterClass(sootMethod);
                responseBodyConverter.init();
            }
        }

    }

    public void setRequestBodyConverter(ConverterClass requestBodyConverter) {
        this.requestBodyConverter = requestBodyConverter;
    }

    public void setCurrentClass(SootClass currentClass) {
        this.currentClass = currentClass;
    }

    public void setResponseBodyConverter(ConverterClass responseBodyConverter) {
        this.responseBodyConverter = responseBodyConverter;
    }

    @Override
    public String toString() {
        return "ConverterFactory{" +
                "currentClass=" + currentClass +
                ", requestBodyConverter=" + requestBodyConverter +
                ", responseBodyConverter=" + responseBodyConverter +
                '}';
    }
}
