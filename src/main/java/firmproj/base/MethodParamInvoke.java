package firmproj.base;

import soot.SootMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MethodParamInvoke {
    public SootMethod sootMethod;
    public Integer param;
    public final List<String> InvokeMethodSig = new ArrayList<>();

    public MethodParamInvoke(SootMethod method, Integer para, String sig){
        sootMethod = method;
        param = para;
        InvokeMethodSig.add(sig);
    }

    public MethodParamInvoke(SootMethod method, Integer para, List<String> sig){
        sootMethod = method;
        param = para;
        InvokeMethodSig.addAll(sig);
    }

    public MethodParamInvoke(MethodParamInvoke OldmethodParamInvoke){
        this.sootMethod = OldmethodParamInvoke.sootMethod;
        this.param = OldmethodParamInvoke.param;
        this.InvokeMethodSig.addAll(OldmethodParamInvoke.InvokeMethodSig);
    }

    public void addMethodInvoke(String methodInvoke){
        for(String str : InvokeMethodSig){
            if(str.equals(methodInvoke))
                return;
        }
        InvokeMethodSig.add(methodInvoke);
    }

    @Override
    public String toString() {
        return "MethodParamInvoke{" +
                "sootMethod=" + sootMethod +
                ", param=" + param +
                ", InvokeMethodSig=" + InvokeMethodSig +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodParamInvoke that = (MethodParamInvoke) o;
        return Objects.equals(sootMethod, that.sootMethod) && Objects.equals(param, that.param) && Objects.equals(InvokeMethodSig, that.InvokeMethodSig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sootMethod, param, InvokeMethodSig);
    }
}
