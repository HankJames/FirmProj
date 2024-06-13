package firmproj.base;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;

import java.util.ArrayList;
import java.util.List;

public class MethodLocal {
    private static final Logger LOGGER = LogManager.getLogger(MethodString.class);
    private final SootMethod sootMethod;
    private final List<String> unsolvedLocals = new ArrayList<>();

    public MethodLocal(SootMethod Method){
        this.sootMethod = Method;
    }

    public List<MethodLocal> doAnalysis(){
        return new ArrayList<>();
    }



}
