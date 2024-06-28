package firmproj.client;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

import java.util.ArrayList;
import java.util.List;

public class RetrofitBuildPoint {
    private final List<String> baseUrl = new ArrayList<>();
    private final List<String> converterFactory = new ArrayList<>();
    private okHttpClient client;

    private SootMethod currentMethod;
    private final Unit createUnit;
    private String createClass;

    public RetrofitBuildPoint(Unit unit){
        this.createUnit = unit;
    }

    public Unit getCreateUnit(){
        return this.createUnit;
    }

    public List<String> getBaseUrl() {
        return baseUrl;
    }

    public String getCreateClass() {
        return createClass;
    }

    public void setBaseUrl(String url){
        this.baseUrl.add(url);
    }

    public void setCreateClass(String createClass) {
        this.createClass = createClass;
    }
}
