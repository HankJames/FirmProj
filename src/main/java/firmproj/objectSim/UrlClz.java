package firmproj.objectSim;

import firmproj.base.ValueContext;
import firmproj.client.CustomHttpClient;
import soot.SootClass;
import soot.SootMethod;

import java.util.HashMap;
import java.util.List;

public class UrlClz implements AbstractClz {

    HashMap<List<String>, List<String>> RequestProperty = new HashMap<>();

    private boolean isUrlClient = false;
    private final CustomHttpClient clientResult = new CustomHttpClient();


    public UrlClz(){

    }

    @Override
    public void solve() {
        isUrlClient = true;
    }

    @Override
    public void init() {

    }

    @Override
    public void addValueContexts(ValueContext valueContext) {

    }

    @Override
    public SootMethod getParentMethod() {
        return null;
    }

    @Override
    public SootClass getCurrentClass() {
        return null;
    }

    @Override
    public List<ValueContext> getCurrentValues() {
        return List.of();
    }

    @Override
    public Object getResult() {
        return clientResult;
    }

    @Override
    public boolean isSolved() {
        return isUrlClient;
    }
}
