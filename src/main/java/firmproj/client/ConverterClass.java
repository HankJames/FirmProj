package firmproj.client;

import soot.SootMethod;

import java.util.ArrayList;
import java.util.List;

public class ConverterClass {
    public SootMethod convertMethod;
    public List<SootMethod> relatedMethod = new ArrayList<>();
    public String converterResult;

    public ConverterClass(SootMethod method){
        this.convertMethod = method;
    }

    public void init(){

    }
}
