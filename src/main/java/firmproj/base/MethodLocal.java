package firmproj.base;
import firmproj.objectSim.AbstractClz;
import firmproj.objectSim.CustomClz;
import firmproj.objectSim.HashmapClz;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.internal.AbstractFloatBinopExpr;

import java.util.*;

public class MethodLocal {
    private static final Logger LOGGER = LogManager.getLogger(MethodString.class);
    private final SootMethod sootMethod;

    private final HashSet<Value> InterestingVariables = new HashSet<>();
    private final HashMap<Value, AbstractClz> LocalToClz = new HashMap<>();
    private final HashMap<Value, List<String>> LocalToString = new HashMap<>();
    private final HashMap<Value, Integer> LocalToParams = new HashMap<>();

    //private List<String> unsolvedLocals = new ArrayList<>();

    public MethodLocal(SootMethod Method){
        this.sootMethod = Method;
    }

    public List<MethodLocal> doAnalysis(){
        Body body = null;
        try{
            body = this.sootMethod.retrieveActiveBody();
        }
        catch(Exception e){
            LOGGER.error(e);
        }
        if(body == null) return new ArrayList<>();
        for(Unit unit : body.getUnits()){
            Stmt stmt = (Stmt) unit;
            if(stmt instanceof AssignStmt){
                caseAssignStmt((AssignStmt) stmt);
            }
            else if (stmt instanceof IdentityStmt){
                caseIdentityStmt((IdentityStmt) stmt);
            }
            else if(stmt instanceof InvokeExpr){
                caseInvokeExpr((InvokeExpr) stmt);
            }
        }
        return new ArrayList<>();
    }

    public void caseAssignStmt(AssignStmt stmt) {
        Value leftOperation = stmt.getLeftOp();
        Value rightOperation = stmt.getRightOp();
        HashSet<?> result = null;
        if (leftOperation instanceof Local || leftOperation instanceof ArrayRef) {

            if (rightOperation instanceof InstanceInvokeExpr) {
                boolean InterestingTransfer = false;
                InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) rightOperation;
                Value base = instanceInvokeExpr.getBase();
                SootMethod invokeMethod = instanceInvokeExpr.getMethod();

                HashMap<Value, List<String>> valueString = getInvokeExprValues(instanceInvokeExpr);

                if(getInterestingVariables().contains(base)){
                    InterestingTransfer = true;
                    if(LocalToClz.containsKey(base)){
                        AbstractClz clz = LocalToClz.get(base);
                        ValueContext valueContext = new ValueContext(this.sootMethod,stmt,valueString);
                        clz.addValueContexts(valueContext);
                    }
                }
                if(MethodString.getMethodToString().containsKey(invokeMethod)) {
                    InterestingTransfer = true;
                    LocalToString.put(leftOperation, MethodString.GetMethodToString(invokeMethod));
                }
                if(InterestingTransfer)
                    InterestingVariables.add(leftOperation);

            } else if (rightOperation instanceof NewExpr) {
                NewExpr newExpr = (NewExpr) rightOperation;
                String className = newExpr.getClass().getName();
                if(isCommonClz(className)){
                    AbstractClz NewClz = CreateCommonClz(newExpr.getBaseType().getSootClass(), this.sootMethod);
                    LocalToClz.put(leftOperation, NewClz);
                    InterestingVariables.add(leftOperation);
                }
                else{
                    AbstractClz NewClz = new CustomClz(newExpr.getBaseType().getSootClass(), this.sootMethod);
                    LocalToClz.put(leftOperation, NewClz);
                    InterestingVariables.add(leftOperation);
                }

            }
            else if (rightOperation instanceof FieldRef) {

            }
            else if (rightOperation instanceof NewArrayExpr) {

            }
            else if (rightOperation instanceof BinopExpr) {

            }
            else if (rightOperation instanceof ParameterRef){
                ParameterRef ref = (ParameterRef) rightOperation;
                if(MethodString.isCommonType(ref.getType())) {
                    LocalToParams.put(leftOperation, ref.getIndex());
                    InterestingVariables.add(leftOperation);
                }
            }
            else if (rightOperation instanceof Local || rightOperation instanceof CastExpr || rightOperation instanceof ArrayRef || rightOperation instanceof Constant) {

            }
        } else {
            LOGGER.warn(String.format("[%s] [SIMULATE][left unknown]: %s (%s)", this.hashCode(), stmt, leftOperation.getClass()));
        }

    }

    public void caseInvokeStmt(InvokeStmt stmt) {
        String signature = stmt.getInvokeExpr().getMethod().toString();
        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        Value base = null;
        if (invokeExpr instanceof InstanceInvokeExpr) {
            base = ((InstanceInvokeExpr) invokeExpr).getBase();
            HashMap<Value, List<String>> valueString = getInvokeExprValues(invokeExpr);
            ValueContext valueContext = new ValueContext(this.sootMethod, stmt, valueString);
            if(LocalToClz.containsKey(base)) LocalToClz.get(base).addValueContexts(valueContext);
        }
        return;
    }

    public HashMap<Value, List<String>> getInvokeExprValues(InvokeExpr invokeExpr){
        HashMap<Value, List<String>> valueString = new HashMap<>();

        for(Value value: invokeExpr.getArgs()){
            if(value instanceof Constant) continue;
            if(value instanceof Local) {
                if(LocalToClz.containsKey(value)){
                    String Clzstring = LocalToClz.get(value).toString();
                    LocalToString.put(value, List.of(Clzstring));
                }
                if(LocalToString.containsKey(value)) valueString.put(value,LocalToString.get(value));
            }

        }
        return valueString;
    }


    public boolean isCommonClz(String className){
        return false;
    }


    public AbstractClz CreateCommonClz( SootClass clz, SootMethod currentMethod){
        return new HashmapClz(clz, currentMethod);
    }

    public SootMethod getSootMethod(){
        return this.sootMethod;
    }

    public HashMap<Value, AbstractClz> getLocalToClz() {
        return LocalToClz;
    }

    public HashMap<Value, List<String>> getLocalToString() {
        return LocalToString;
    }

    public HashMap<Value, Integer> getLocalToParams() {
        return LocalToParams;
    }

//    public List<String> getUnsolvedLocals() {
//        return unsolvedLocals;
//    }

    public HashSet<Value> getInterestingVariables() {
        return InterestingVariables;
    }
}
