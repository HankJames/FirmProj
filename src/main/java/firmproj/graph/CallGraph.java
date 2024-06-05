package firmproj.graph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.*;

/**
 * Creates the CallGraph of the application under analysis
 */
public class CallGraph {

    private static final Logger LOGGER = LogManager.getLogger(CallGraph.class);

    //Key the string of the soot method
    // map with the sootMethod name and the CallGraphNode of the sootMethod
    private static final Hashtable<String, CallGraphNode> nodes = new Hashtable<>();

    // Maps Soot Field String to the soot Methods it is referenced
    private static final Hashtable<String, HashSet<SootMethod>> fieldSetters = new Hashtable<>();

    private static final HashMap<SootMethod, String> methodToFieldString = new HashMap<>();

    private static final HashMap<String, List<String>> fieldToString = new HashMap<>();

    private static final HashMap<SootMethod, List<String>> methodToString = new HashMap<>();

    public static void init() {
        long startTime = System.currentTimeMillis();

        Chain<SootClass> classes = Scene.v().getClasses();
        try {
            //init the nodes map
            for (SootClass sootClass : classes) {
                List<SootMethod> methods = new ArrayList<>(sootClass.getMethods());
                for (SootMethod sootMethod : methods) {
                    CallGraphNode tmpNode = new CallGraphNode(sootMethod);
                    nodes.put(sootMethod.toString(), tmpNode);
                    if (sootMethod.isConcrete()) {
                        try {
                            sootMethod.retrieveActiveBody();
                        } catch (Exception e) {
                            LOGGER.error("Could not retrieved the active body of {} because {}", sootMethod, e.getLocalizedMessage());
                        }
                    }
                }
            }

            LOGGER.debug("[CG time]: " + (System.currentTimeMillis() - startTime));
            for (SootClass sootClass : classes) {
                for (SootMethod sootMethod : clone(sootClass.getMethods())) {
                    if (!sootMethod.isConcrete())
                        continue;
                    Body body = null;
                    try {
                        body = sootMethod.retrieveActiveBody();
                    } catch (Exception e) {
                        LOGGER.error("Could not retrieved the active body {} because {}", sootMethod, e.getLocalizedMessage());
                    }
                    if (body == null)
                        continue;
                    for (Unit unit : body.getUnits()) {
                        if(sootMethod.getDeclaringClass().getName()== "com.liesheng.haylou.net.HttpUrl"){
                            LOGGER.info(unit.toString());
                        }
                        if (unit instanceof ReturnStmt){
                            ReturnStmt returnStmt = (ReturnStmt) unit;
                            Value retValue = returnStmt.getOp();
                            if(retValue instanceof Local){
                                Local local = (Local) retValue;
                                for (Unit u : body.getUnits()) {
                                    if (u instanceof AssignStmt) {
                                        AssignStmt assignStmt = (AssignStmt) u;
                                        if (assignStmt.getLeftOp().equals(local)) {
                                            Value rightOp = assignStmt.getRightOp();
                                            if (rightOp instanceof StaticFieldRef) {
                                                StaticFieldRef fieldRef = (StaticFieldRef) rightOp;
                                                if (fieldRef.getField() == null || fieldRef.getField().getDeclaringClass() == null) {
                                                    continue;
                                                }
                                                if (fieldRef.getField().getDeclaringClass().isApplicationClass()) {
                                                    SootField field = fieldRef.getField();
                                                    if (!methodToFieldString.containsKey(sootMethod)) {
                                                        methodToFieldString.put(sootMethod, field.toString());
                                                        LOGGER.info("New Method ret field: " + sootMethod.toString() + ":" + field.toString());
                                                        if(fieldToString.containsKey(field.toString())){
                                                            List<String> strs = fieldToString.get(field.toString());
                                                            methodToString.put(sootMethod, strs);
                                                            LOGGER.info("New Method To String: " + sootMethod.getName() + ":" + strs.toString() + ";" + methodToString.get(sootMethod).toString());
                                                        }
                                                    }
                                                }
                                            }
                                            else if(rightOp instanceof InstanceFieldRef){
                                                //TODO
                                                //addValue(methodToString, sootMethod, "$"+((InstanceFieldRef) rightOp).getField().getName());
                                            }
                                        }
                                    }
                                }
                            }
                            if(retValue instanceof Constant){
                                addValue(methodToString, sootMethod, ((Constant)retValue).toString());
                            }

                        }
                        if (unit instanceof Stmt) {
                            if (((Stmt) unit).containsInvokeExpr()) {
                                try {
                                    addCall(sootMethod, ((Stmt) unit).getInvokeExpr().getMethod());
                                } catch (Exception e) {
                                    LOGGER.error(e.getMessage());
                                }
                            }
                            if (unit instanceof AssignStmt && ((AssignStmt) unit).getLeftOp() instanceof FieldRef){

                                FieldRef fieldRef = (FieldRef) ((AssignStmt) unit).getLeftOp();
                                SootField field = fieldRef.getField();
                                Value rightOP = ((AssignStmt) unit).getRightOp();
                                //LOGGER.info("Find new:"+ field.getName()+rightOP.toString());
                                if (field == null || field.getDeclaringClass() == null) {
                                    continue;
                                }
                                if (field.getDeclaringClass().isApplicationClass()) {
                                    if(rightOP instanceof Constant) {
                                        if(rightOP.toString()!="null") {
                                            addValue(fieldToString, field.toString(), rightOP.toString());
                                            LOGGER.info("New Field with String: " + field.toString() + ":" + rightOP.toString() + ";" + fieldToString.get(field.toString()).toString());
                                            if (methodToFieldString.containsValue(field.toString())) {
                                                SootMethod method = getKeyByValue(methodToFieldString, field.toString());
                                                if (!methodToString.containsKey(method)) {
                                                    addValue(methodToString, method, rightOP.toString());
                                                    LOGGER.info("New Method To String: " + method.getName() + ":" + rightOP.toString() + ";" + methodToString.get(method).toString());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            for (ValueBox valueBox : unit.getDefBoxes()) {
                                Value temporaryValue = valueBox.getValue();
                                if (temporaryValue instanceof FieldRef) {
                                    FieldRef fieldRef = (FieldRef) temporaryValue;
                                    if (fieldRef.getField() == null || fieldRef.getField().getDeclaringClass() == null) {
                                        continue;
                                    }
                                    if (fieldRef.getField().getDeclaringClass().isApplicationClass()) {
                                        String str = fieldRef.getField().toString();
                                        if (!fieldSetters.containsKey(str)) {
                                            fieldSetters.put(str, new HashSet<>());
                                        }
                                        fieldSetters.get(str).add(sootMethod);
                                    }
                                }
                            }
                        }

                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.error("error init call graph");
        }
        //TODO If there is a Method to field But no field to string, just Method to field string "$fieldName";
        LOGGER.info("SIZE: " + methodToFieldString.size() + " "+ fieldToString.size());
        LOGGER.info("[CG time]:" + (System.currentTimeMillis() - startTime));
    }

    /**
     * Add to the call graph nodes the information about the callee and caller
     *
     * @param from add to the from node the call information
     * @param to   add to the to node the caller information
     */
    private static void addCall(SootMethod from, SootMethod to) {
        CallGraphNode fromNode, toNode;
        fromNode = getNode(from);
        toNode = getNode(to);
        if (fromNode == null || toNode == null) {
            LOGGER.debug("Can't add call because from or to node is null");
            return;
        }

        fromNode.addCallTo(toNode);
        toNode.addCallBy(fromNode);

    }

    public List<String> GetMethodToString(SootMethod sootMethod){
        List<String> mstrs = new ArrayList<>();
        Body body = null;
        try {
            body = sootMethod.retrieveActiveBody();
        } catch (Exception e) {
            LOGGER.error("Could not retrieved the active body {} because {}", sootMethod, e.getLocalizedMessage());
        }
        for(Unit unit: body.getUnits()) {
            if (unit instanceof ReturnStmt) {
                ReturnStmt returnStmt = (ReturnStmt) unit;
                Value retValue = returnStmt.getOp();
                if (retValue instanceof Local) {
                    Local local = (Local) retValue;
                    for (Unit u : body.getUnits()) {
                        if (u instanceof AssignStmt) {
                            AssignStmt assignStmt = (AssignStmt) u;
                            if (assignStmt.getLeftOp().equals(local)) {
                                Value rightOp = assignStmt.getRightOp();
                                if (rightOp instanceof StaticFieldRef) {
                                    StaticFieldRef fieldRef = (StaticFieldRef) rightOp;
                                    if (fieldRef.getField() == null || fieldRef.getField().getDeclaringClass() == null) {
                                        continue;
                                    }
                                    if (fieldRef.getField().getDeclaringClass().isApplicationClass()) {
                                        SootField field = fieldRef.getField();
                                        if (!methodToFieldString.containsKey(sootMethod)) {
                                            methodToFieldString.put(sootMethod, field.toString());
                                            LOGGER.info("New Method ret field: " + sootMethod.toString() + ":" + field.toString());
                                            if (fieldToString.containsKey(field.toString())) {
                                                List<String> strs = fieldToString.get(field.toString());
                                                methodToString.put(sootMethod, strs);
                                                mstrs.addAll(strs);
                                                LOGGER.info("New Method To String: " + sootMethod.getName() + ":" + strs.toString() + ";" + methodToString.get(sootMethod).toString());
                                                return mstrs;
                                            }
                                        }
                                    }
                                } else if (rightOp instanceof InstanceFieldRef) {
                                    //TODO
                                    //addValue(methodToString, sootMethod, "$"+((InstanceFieldRef) rightOp).getField().getName());
                                    return mstrs;
                                } else if (rightOp instanceof InvokeExpr) {
                                    mstrs.addAll(GetMethodToString(((InvokeExpr) rightOp).getMethod()));
                                    if(mstrs.size() > 0){
                                        for(String item : mstrs) {
                                            addValue(methodToString, sootMethod, item);
                                        }
                                    }
                                    return mstrs;
                                }
                            }
                        }
                    }
                }
                if (retValue instanceof Constant) {
                    String str = ((Constant) retValue).toString();
                    addValue(methodToString, sootMethod, str);
                    mstrs.add(str);
                    return mstrs;
                }

            }
        }
        return mstrs;
    }

    /**
     * get CallGraphNode from Soot Method
     *
     * @param from to get CallGraphNode from
     * @return the corresponding node
     */
    public static CallGraphNode getNode(SootMethod from) {
        return getNode(from.toString());
    }

    /**
     * get CallGraphNode from Soot Method
     *
     * @param from SootMethodString to get the CallGraphNode from
     * @return the corresponding node
     */
    public static CallGraphNode getNode(String from) {
        return nodes.get(from);
    }

    public static HashSet<SootMethod> getSetter(SootField sootField) {
        return fieldSetters.get(sootField.toString());
    }

    public static <K, V> K getKeyByValue(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static HashMap<SootMethod,List<String>> getMethodToString(){
        return methodToString;
    }

    public static HashMap<SootMethod,String> getMethodToFieldString(){
        return methodToFieldString;
    }

    public static HashMap<String,List<String>> getFieldToString(){
        return fieldToString;
    }

    public static <K, V> void addValue(Map<K, List<V>> map, K key, V value) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }


    public static <T> List<T> clone(List<T> ls) {
        return new ArrayList<T>(ls);
    }
}
