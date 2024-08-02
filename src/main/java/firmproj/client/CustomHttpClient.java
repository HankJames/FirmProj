package firmproj.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.SootMethod;
import soot.Unit;
import soot.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CustomHttpClient implements AbstractHttpClient {

    private static final Logger LOGGER = LogManager.getLogger(CustomHttpClient.class);

    public SootMethod sootMethod;

    public Unit unit;

    private Value localValue;

    private boolean isNeedRequestContent = false;

    public final HashMap<String, List<String>> requestContentFromParams = new HashMap<>(); //Body, MediaType.

    public final HashMap<String, List<String>> requestContent = new HashMap<>();

    public CustomHttpClient(SootMethod method, Unit unit){
        this.sootMethod = method;
        this.unit = unit;
    }

    public CustomHttpClient(CustomHttpClient old){
        this.isNeedRequestContent = old.isNeedRequestContent();
        this.requestContentFromParams.putAll(old.requestContentFromParams);
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
        return "CustomHttpClient{" +
                ", requestContent=" + requestContent +
                ", isNeedRequestContent=" + isNeedRequestContent +
                ", requestContentFromParams=" + requestContentFromParams +
                '}';
    }
}
