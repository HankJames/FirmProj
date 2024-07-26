package firmproj.graph;

import firmproj.base.MethodString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;
import soot.jimple.*;
import soot.tagkit.InnerClassTag;
import soot.tagkit.Tag;
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

    public static final HashMap<String, List<SootClass>> OuterInnerClasses = new HashMap<>();

    // Maps Soot Field String to the soot Methods it is referenced
    private static final Hashtable<String, HashSet<SootMethod>> fieldSetters = new Hashtable<>();


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
                //LOGGER.warn(sootClass.getName());
                if(sootClass.hasTag("InnerClassTag") && !MethodString.isStandardLibraryClass(sootClass)){
                    Tag tag = sootClass.getTag("InnerClassTag");
                    if(tag instanceof InnerClassTag){
                        if(((InnerClassTag) tag).getOuterClass() != null){
                            String outerClass = ((InnerClassTag) tag).getOuterClass().replace('/','.');
                            if(!OuterInnerClasses.containsKey(outerClass))
                                OuterInnerClasses.put(outerClass, new ArrayList<>());
                            if(!OuterInnerClasses.get(outerClass).contains(sootClass)) {
                                OuterInnerClasses.get(outerClass).add(sootClass);
                                if(!MethodString.classWithOuterInnerFields.containsKey(outerClass))
                                    MethodString.classWithOuterInnerFields.put(outerClass, new ArrayList<>());
                                List<SootField> fields = MethodString.classWithOuterInnerFields.get(outerClass);
                                for(SootField field: sootClass.getFields()) {
                                    if(!fields.contains(field))
                                        fields.add(field);
                                }
                            }
                        }
                    }
                }
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
                        if (unit instanceof Stmt) {
                            if (((Stmt) unit).containsInvokeExpr()) {
                                try {
                                    addCall(sootMethod, ((Stmt) unit).getInvokeExpr().getMethod());
                                } catch (Exception e) {
                                    LOGGER.error(e.getMessage());
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
            LOGGER.error( e.getLocalizedMessage()+"error init call graph");
        }


        LOGGER.info("[CG time]:" + (System.currentTimeMillis() - startTime));
        //LOGGER.info("total: " + visitedMethod.toString());
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


    public static <T> List<T> clone(List<T> ls) {
        return new ArrayList<T>(ls);
    }
}
