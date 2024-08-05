package firmproj.client;


import firmproj.utility.LLMQuery;
import firmproj.utility.QueryJson;
import soot.SootClass;
import soot.SootMethod;

import java.util.HashMap;

public class CallFactory implements AbstractFactory{
    public SootClass currentClass;
    public SootMethod currentMethod;
    public QueryJson queryJson;

    public CallFactory(SootClass clz){
        this.currentClass = clz;
    }

    public void init(){
        for(SootMethod sootMethod: currentClass.getMethods()){
            if(sootMethod.getName().contains("newCall")){
                this.currentMethod = sootMethod;
            }
        }
        if(this.currentMethod != null){
            QueryJson queryJson1 = LLMQuery.generateHttp(currentMethod);
            if(queryJson1.getTargetMethodSubsSig().contains(currentMethod.getName())){
                this.queryJson = queryJson1;
            }
        }
    }

    @Override
    public void generateQuery() {
        this.queryJson.doGenerate();
    }

    @Override
    public String toString() {
        return "CallFactory{" +
                "currentClass=" + currentClass +
                ", currentMethod=" + currentMethod +
                '}';
    }
}
