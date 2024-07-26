package firmproj.client;

import soot.SootMethod;
import soot.Unit;

import java.util.ArrayList;
import java.util.List;

public class RetrofitBuildPoint {
    private final List<String> baseUrl = new ArrayList<>();
    private final List<String> converterFactory = new ArrayList<>();
    private final List<okHttpClient> okHttpClients = new ArrayList<>();

    private SootMethod currentMethod;
    private Unit createUnit;
    private String createClass;

    public boolean classFromParam = false;
    public boolean urlFromParam = false;

    public Integer classParam = -1;
    public Integer urlParam = -1;

    public RetrofitBuildPoint(Unit unit){
        this.createUnit = unit;
    }

    public RetrofitBuildPoint(RetrofitBuildPoint old){
        this.baseUrl.addAll(old.baseUrl);
        this.converterFactory.addAll(old.converterFactory);
        this.okHttpClients.addAll(old.okHttpClients);
        this.createClass = old.createClass;
        this.createUnit = old.createUnit;
        this.currentMethod = old.currentMethod;
    }

    public RetrofitBuildPoint(SootMethod method, Unit unit){
        this(unit);
        currentMethod = method;
    }

    public Unit getCreateUnit(){
        return this.createUnit;
    }

    public List<String> getBaseUrl() {
        return baseUrl;
    }

    public SootMethod getCurrentMethod() {
        return currentMethod;
    }

    public String getCreateClass() {
        return createClass;
    }

    public void setCurrentMethod(SootMethod currentMethod) {
        this.currentMethod = currentMethod;
    }

    public void setCreateUnit(Unit createUnit) {
        this.createUnit = createUnit;
    }

    public void setBaseUrl(String url){
        if(!this.baseUrl.contains(url))
            this.baseUrl.add(url);
    }

    public List<okHttpClient> getOkHttpClients() {
        return okHttpClients;
    }

    public void setOkHttpClients(List<AbstractHttpClient> clients){
        for(AbstractHttpClient client: clients){
            okHttpClient okClient = (okHttpClient) client;
            if(!okHttpClients.contains(okClient))
                okHttpClients.add(okClient);
        }
    }

    public void setCreateClass(String createClass) {
        this.createClass = createClass;
    }

    @Override
    public String toString() {
        return "---RetrofitBuildPoint{" +
                "currentMethod=" + currentMethod +
                ", baseUrl=" + baseUrl +
                ", createClass='" + createClass + '\'' +
                ", urlParam=" + urlParam +
                ", urlFromParam=" + urlFromParam +
                ", classParam=" + classParam +
                ", classFromParam=" + classFromParam +
                ", createUnit=" + createUnit +
                ", okHttpClients=" + okHttpClients+
                "}---";
    }
}
