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

    private boolean isNeedRequestContent = false;

    public List<Integer> params = new ArrayList<>();

    public final HashMap<String, List<String>> requestContentFromParams = new HashMap<>(); //Body, MediaType, method.

    public CustomHttpClient(SootMethod method, Unit unit){
        this.sootMethod = method;
        this.unit = unit;
    }

    public CustomHttpClient(){}

    public void setNeedRequestContent(boolean need){
        this.isNeedRequestContent = need;
    }

    public boolean isNeedRequestContent() {
        return isNeedRequestContent;
    }

    @Override
    public String toString() {
        return "CustomHttpClient{" +
                ", isNeedRequestContent=" + isNeedRequestContent +
                ", params=" + params +
                ", requestContentFromParams=" + requestContentFromParams +
                '}';
    }
}
