package firmproj.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.SootMethod;
import soot.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class okHttpClient implements AbstactHttpClient {

    private final List<Interceptor> interceptors = new ArrayList<>();

    private static final Logger LOGGER = LogManager.getLogger(okHttpClient.class);

    private boolean isNeedRequestContent = false;

    public  final HashMap<String, List<String>> requestContentFromParams = new HashMap<>(); //Body, MediaType.

    public okHttpClient(SootMethod method, Unit unit){

    }

    public okHttpClient(okHttpClient old){
        this.interceptors.addAll(old.interceptors);
        this.isNeedRequestContent = old.isNeedRequestContent();
    }

    public void setInterceptors(Interceptor interceptor){
        if(!interceptors.contains(interceptor))
            this.interceptors.add(interceptor);
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
                "interceptors=" + interceptors +
                ", isNeedRequestContent=" + isNeedRequestContent +
                ", requestContentFromParams=" + requestContentFromParams +
                '}';
    }
}
