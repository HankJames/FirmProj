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

    public static final HashMap<String, List<SootClass>> outerInnerClasses = new HashMap<>();

    public static final HashMap<SootClass, List<SootMethod>> callBackClassesWithInvoke = new HashMap<>();
    // Maps Soot Field String to the soot Methods it is referenced

    public static void init() {
        long startTime = System.currentTimeMillis();

        Chain<SootClass> classes = Scene.v().getClasses();
        try {
            //init the nodes map
            for (SootClass sootClass : classes) {
                if(MethodString.isStandardLibraryClass(sootClass)) continue;
                if(sootClass.hasTag("InnerClassTag")){
                    Tag tag = sootClass.getTag("InnerClassTag");
                    if(tag instanceof InnerClassTag){
                        if(((InnerClassTag) tag).getOuterClass() != null){
                            String outerClass = ((InnerClassTag) tag).getOuterClass().replace('/','.');
                            if(!outerInnerClasses.containsKey(outerClass))
                                outerInnerClasses.put(outerClass, new ArrayList<>());
                            if(!outerInnerClasses.get(outerClass).contains(sootClass)) {
                                outerInnerClasses.get(outerClass).add(sootClass);
                                if(!MethodString.classWithOuterInnerFields.containsKey(outerClass))
                                    MethodString.classWithOuterInnerFields.put(outerClass, new ArrayList<>());
                                List<SootField> fields = MethodString.classWithOuterInnerFields.get(outerClass);
                                for(SootField field: sootClass.getFields()) {
                                    if(!fields.contains(field))
                                        fields.add(field);
                                }
                            }
                            for(SootClass clz : sootClass.getInterfaces()){
                                String clzName = clz.getName();
                                if(clzName.contains("CallBack")){
                                    callBackClassesWithInvoke.put(sootClass, new ArrayList<>());
                                }
                            }
                        }
                    }
                }
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
                boolean needCheck = !MethodString.isStandardLibraryClass(sootClass);
                if(!needCheck) continue;
                for (SootMethod sootMethod : clone(sootClass.getMethods())) {
                    if(sootMethod.getSignature().contains("com.gooclient.anycam.activity.settings.update.UpdateFirmwareActivity: void getNewVersion(java.lang.String)"))
                        LOGGER.info("GOTIT");
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
                            if(unit instanceof AssignStmt && ((AssignStmt) unit).getRightOp() instanceof NewExpr){
                                NewExpr newExpr = (NewExpr)((AssignStmt) unit).getRightOp();
                                SootClass cls = newExpr.getBaseType().getSootClass();
                                if(callBackClassesWithInvoke.containsKey(cls)){
                                    List<SootMethod> methods = callBackClassesWithInvoke.get(cls);
                                    if(!methods.contains(sootMethod)) methods.add(sootMethod);
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
        LOGGER.info("CallbackCLass: {}", callBackClassesWithInvoke);
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
            //LOGGER.debug("Can't add call because from or to node is null");
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


    public static <T> List<T> clone(List<T> ls) {
        return new ArrayList<T>(ls);
    }
}
