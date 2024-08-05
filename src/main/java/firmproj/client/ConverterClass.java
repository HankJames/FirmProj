package firmproj.client;

import firmproj.utility.LLMQuery;
import firmproj.utility.QueryJson;
import soot.SootClass;
import soot.SootMethod;

import java.util.HashMap;

public class ConverterClass {
    public SootClass currentClass;
    public SootMethod convertMethod;
    public QueryJson queryJson;

    public ConverterClass(SootClass sootClass){
        this.currentClass = sootClass;
    }

    public void init(){
        for(SootMethod sootMethod: currentClass.getMethods()){
            String sig = sootMethod.getSignature();
            String str1 = currentClass.getName() + ": java.lang.Object convert";
            String str2 = currentClass.getName() + ": okhttp3.RequestBody convert";
            String str3 = currentClass.getName() + ": okhttp3.ResponseBody convert";
            if(sig.contains(str1) || sig.contains(str2) || sig.contains(str3)){
                this.convertMethod = sootMethod;
            }
        }
        if(this.convertMethod != null){
            QueryJson queryJson1 = LLMQuery.generateHttp(convertMethod);
            if(queryJson1.getTargetMethodSubsSig().contains(convertMethod.getName())){
                this.queryJson = queryJson1;
            }
        }
    }

    @Override
    public String toString() {
        return "ConverterClass{" +
                "currentClass=" + currentClass +
                ", convertMethod=" + convertMethod +
                '}';
    }
}
