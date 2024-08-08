package firmproj.client;

import firmproj.base.MethodString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.SootMethod;
import soot.Unit;
import soot.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class okHttpClient implements AbstractHttpClient {

    private final List<Interceptor> interceptors = new ArrayList<>();

    private static final Logger LOGGER = LogManager.getLogger(okHttpClient.class);

    public SootMethod sootMethod;

    public Unit unit;

    private Value localValue;

    private boolean isNeedRequestContent = false;

    public List<Integer> params = new ArrayList<>();

    public  final HashMap<String, List<String>> requestContentFromParams = new HashMap<>(); //Body, MediaType.

    public okHttpClient(SootMethod method, Unit unit){
        this.sootMethod = method;
        this.unit = unit;
    }

    public okHttpClient(okHttpClient old){
        this.interceptors.addAll(old.interceptors);
        this.isNeedRequestContent = old.isNeedRequestContent();
        this.requestContentFromParams.putAll(old.requestContentFromParams);
    }

    public void setInterceptors(Interceptor interceptor){
        if(!interceptors.contains(interceptor))
            this.interceptors.add(interceptor);
    }

    public void setLocalValue(Value localValue) {
        this.localValue = localValue;
    }

    public Value getLocalValue() {
        return localValue;
    }

    public void setNeedRequestContent(boolean need){
        this.isNeedRequestContent = need;
    }

    public boolean isNeedRequestContent() {
        return isNeedRequestContent;
    }

    @Override
    public String toString() {
        return "okHttpClient{" +
                "params=" + params +
                ", interceptors=" + interceptors +
                ", isNeedRequestContent=" + isNeedRequestContent +
                ", requestContentFromParams=" + MethodString.getContent(requestContentFromParams) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        okHttpClient that = (okHttpClient) o;
        return isNeedRequestContent == that.isNeedRequestContent && Objects.equals(interceptors, that.interceptors) && Objects.equals(sootMethod, that.sootMethod) && Objects.equals(params, that.params) && Objects.equals(requestContentFromParams, that.requestContentFromParams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interceptors, sootMethod, isNeedRequestContent, params, requestContentFromParams);
    }
}
