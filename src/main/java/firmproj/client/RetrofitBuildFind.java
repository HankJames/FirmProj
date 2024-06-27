package firmproj.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class RetrofitBuildFind {

    private static final HashMap<String,List<RetrofitBuildPoint>> findResult = new HashMap<>();

    private static HashSet<SootClass>  RetrofitClasses = new HashSet<>();

    private static final Logger LOGGER = LogManager.getLogger(RetrofitBuildFind.class);

    public static void findAllRetrofitBuildMethod(){
        Chain<SootClass> classes = Scene.v().getClasses();
        for (SootClass sootClass : classes) {
            String headString = sootClass.getName().split("\\.")[0];
            if (headString.contains("android") || headString.contains("kotlin") || headString.contains("java"))
                continue;
            //LOGGER.info(sootClass.getName().toString());
            for (SootMethod sootMethod : clone(sootClass.getMethods())) {
                if (!sootMethod.isConcrete())
                    continue;
                Body body = null;
                try {
                    body = sootMethod.retrieveActiveBody();
                } catch (Exception e) {
                    //LOGGER.error("Could not retrieved the active body {} because {}", sootMethod, e.getLocalizedMessage());
                }
                if (body == null)
                    continue;
                Type ret = sootMethod.getReturnType();
                if(!checkReturnType(ret)) continue;
                for (Unit unit : body.getUnits()) {
                    Stmt stmt = (Stmt) unit;
                    if(stmt instanceof AssignStmt){
                        Value rightOp = ((AssignStmt) stmt).getRightOp();
                        if(rightOp instanceof NewExpr){
                            NewExpr newExpr = (NewExpr) rightOp;
                            String clsName = newExpr.getBaseType().getClassName();
                            if(clsName.equals("retrofit2.Retrofit$Builder")){
                                findResult.put(sootMethod.getSignature(), List.of(new RetrofitBuildPoint()));
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public static void findRetrofitBuildMethod(SootMethod sootMethod){
        if (!sootMethod.isConcrete()) return;
        Body body = null;
        try {
            body = sootMethod.retrieveActiveBody();
        } catch (Exception e) {
            //LOGGER.error("Could not retrieved the active body {} because {}", sootMethod, e.getLocalizedMessage());
        }
        if (body == null)
            return;
        Type ret = sootMethod.getReturnType();
        if(!checkReturnType(ret)) return;
        for (Unit unit : body.getUnits()) {
            Stmt stmt = (Stmt) unit;
            if(stmt instanceof AssignStmt){
                Value rightOp = ((AssignStmt) stmt).getRightOp();
                if(rightOp instanceof NewExpr){
                    NewExpr newExpr = (NewExpr) rightOp;
                    String clsName = newExpr.getBaseType().getClassName();
                    if(clsName.equals("retrofit2.Retrofit$Builder")){
                        findResult.put(sootMethod.getSignature(), List.of(new RetrofitBuildPoint()));
                        break;
                    }
                }
            }
        }
    }

    public static boolean checkReturnType(Type v){
        String typeClz = v.toString();
        for(SootClass clz : RetrofitClasses){
            if(typeClz.equals(clz.getName()))
                return true;
        }
        return typeClz.equals("java.lang.Object") || typeClz.equals("retrofit2.Retrofit");
    }



    public static void setRetrofitClasses(HashSet<SootClass> clz){
        RetrofitClasses = clz;
    }

    public static <T> List<T> clone(List<T> ls) {
        return new ArrayList<T>(ls);
    }


}
