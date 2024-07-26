package firmproj.client;


import soot.SootClass;

public class CallFactory implements AbstractFactory{
    public SootClass currentClass;

    public CallFactory(SootClass clz){
        this.currentClass = clz;
    }

    public void init(){}
}
