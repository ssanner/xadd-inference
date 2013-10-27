package hgm.asve.cnsrv.approxator;

import xadd.XADD;

import java.util.List;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 14/10/13
 * Time: 10:38 PM
 */
public interface PathIntegralOnLeafFunctionCalculator {
    /**
     * @param rootNode root xadd
     * @return a map form a list of nodes from root to leaf (i.e. complete path) mapped to the integral of its leaf function
     */
    Map<List<XADD.XADDNode>, Double> calculatePathValueMap(XADD.XADDNode rootNode, LeafFunction leafFunction);
}

//These are parts I had added to XADD class and then removed:
/*

    */
/*------------------------------------------------------*//*

//PATH MASS PART:
public static final boolean DEBUG_XADD_PATH_MASS = true;

    @Deprecated
    public int approximateXADD(int xaddNodeId, double massThreshold, double volumeThreshold) {
        int rootId = xaddNodeId;
        while (getNode(rootId) instanceof XADDINode) {
            System.out.println("---*---");
            int newRootId = mergeNodes(rootId, massThreshold, volumeThreshold);
            if (newRootId == rootId) break;
            rootId = newRootId;
        }

        return rootId;
    }


    */
/**
     * @return node id of the modified (approximated) XADD.
     *//*

    @Deprecated
    public int mergeNodes(int rootXaddNodeId, double massThreshold, double volumeThreshold) {
        PathMassCalculator pathMassCalculator = computePathMass(rootXaddNodeId);
        Map<Path, Double> pathMassMap = pathMassCalculator.getPathMassMap();
        Map<Path, Double> pathVolumeMap = pathMassCalculator.getPathVolumeMap();

        if (DEBUG_XADD_PATH_MASS) System.out.println("pathVolumeMap = " + pathVolumeMap);
        if (DEBUG_XADD_PATH_MASS) System.out.println("pathMassMap = " + pathMassMap);

        Set<CompletePath> chosenCompletePathsToBeMerged = new HashSet<CompletePath>();

        Double totalMass = 0d;
        Double totalVolume = 0d;
        for (Path path : pathMassMap.keySet()) {
            Double mass = pathMassMap.get(path);
            Double volume = pathVolumeMap.get(path);

            if (mass <= massThreshold && volume <= volumeThreshold) {
                XADDINode lastDecisionNode = path.getLastDecisionNode();
                Boolean lastDecisionValue = path.getLastDecisionValue();

                XADDTNode leaf;
                if (lastDecisionValue) {
                    //high child should be approximated:
                    leaf = (XADDTNode)getExistNode(lastDecisionNode._high);
                } else {
                    leaf = (XADDTNode)getExistNode(lastDecisionNode._low);
                }

                CompletePath chosenCompletePath = new CompletePath(path, leaf, volume);//ArrayList<XADDNode>(path.getInternalNodes());

                chosenCompletePathsToBeMerged.add(chosenCompletePath);

                totalMass += mass;
                totalVolume += volume;
            }
        }

        if (chosenCompletePathsToBeMerged.size() < 2)
            return rootXaddNodeId; //nothing is merged.

        double averageMass = (totalVolume == 0) ? 0 : (totalMass / totalVolume);
        if (DEBUG_XADD_PATH_MASS) System.out.println("averageMass = " + averageMass);

        int returnedXaddNodeId;
        int substitutingTermNodeId = getTermNode(new DoubleExpr(averageMass));

        returnedXaddNodeId = _hmNode2Int.get(
                leafSubstituteXADD(
                        getExistNode(rootXaddNodeId),
                        new ArrayList<XADDINode>(), //no parent is known for the root
                        chosenCompletePathsToBeMerged,
                        (XADDTNode) getExistNode(substitutingTermNodeId)));

        return returnedXaddNodeId;
    }


    */
/**
     * @param thisXADD          The XADD in which a group of terminals should be replaced by a single terminal
     * @param completePathsToLeavesToBeSubstituted
     *                          list of terminals should be replaced
     * @param replacingTerminal replacing terminal
     * @return id of the resulting xadd, NULL if constraints (i.e. the conjunction of path decisions) is not satisfiable.
     *//*

    public XADDNode leafSubstituteXADD(XADDNode thisXADD,
                                       List<XADDINode> pathFromRootToThisXADD,
                                       Set<CompletePath> completePathsToLeavesToBeSubstituted,
                                       XADDTNode replacingTerminal) {
        XADDNode modifiedXADD = null;

        if (thisXADD instanceof XADDTNode) {
            for (CompletePath completePath : completePathsToLeavesToBeSubstituted) {
                if (completePath.getLeaf().equals(thisXADD) && completePath.getPathInternalNodes().equals(pathFromRootToThisXADD)) {
                    return completePath.pathVolume <= 0 ? null : replacingTerminal; //non positive path volume means the path is not satisfiable
                }
            }

            return thisXADD;
        }

        XADDINode iThisXADD = (XADDINode) thisXADD;

        //path from root to the children of the current node, contains the current node itself:
        pathFromRootToThisXADD.add(iThisXADD);

        XADDNode low = leafSubstituteXADD(getNode(iThisXADD._low),
                pathFromRootToThisXADD,
                completePathsToLeavesToBeSubstituted,
                replacingTerminal);
        XADDNode high = leafSubstituteXADD(getNode(iThisXADD._high),
                pathFromRootToThisXADD,
                completePathsToLeavesToBeSubstituted, replacingTerminal);

        // Guarantee that the current node is eliminated from the path before the process ends:
        pathFromRootToThisXADD.remove(pathFromRootToThisXADD.size() - 1);

        //if a child is impossible, return the other child (omit the current "decision")
        if (low == null) return high;
        if (high == null) return low;

        //If low and high are the same:
        if (low.equals(high)) return high;


        //TODO: I wonder what is the use of ids for xadd nodes (and expressions).
        //TODO: instead, I think we can always pass the XADDNodes to methods (rather than their ids)
        //TODO: For objects, java is "call by reference" i.e. pointers of objects are passed to methods which is not so different from using integers.
        //TODO: The benefits will be:
        // 1. we do not need to use _hmNode2Int and _hmInt2Node maps (so things become faster), garbage collection can be done automatically...
        // 2. due to type checking, ids of nodes and expressions and other integer values cannot be used interchangeably so it is safer
        // 3. directly working with objects is more readable, mush easier to trace and debug.
        modifiedXADD = getExistNode(getINode(iThisXADD._var */
/*i.e. expression of the node*//*
, _hmNode2Int.get(low), _hmNode2Int.get(high)));

        return modifiedXADD;
    }

    /*/
/*********************************************************************************:




    /*/
/*********************************************************************************


    public PathMassCalculator computePathMass(int xaddNodeId) {
        PathMassCalculator massCalculator = new PathMassCalculator(xaddNodeId);
        reduceProcessXADDLeaf(xaddNodeId, massCalculator, */
/* canonical_reorder *//*
false);

        //just for test:
        if (DEBUG_XADD_PATH_MASS) {
            double totalVolume = 0.0;
            for (Double vol : massCalculator.getPathVolumeMap().values()) {
                totalVolume += vol;
            }
            System.out.println("totalVolume = " + totalVolume);
        }

        return massCalculator;
    }

public class Path {
    private List<Decision> decisions;
    private List<Boolean> decisionValues;
    private List<XADDINode> decisionNodes;


//        private Map<Decision, List<XADDINode>> _pathNodesMap;

    public Path(List<Decision> decisions, List<Boolean> decisionValues, List<XADDINode> nodes) {
        this.decisions = decisions;
        this.decisionValues = decisionValues;
        this.decisionNodes = nodes;
    }

    public List<Decision> getDecisions() {
        return decisions;
    }

    public List<Boolean> getDecisionValues() {
        return decisionValues;
    }

    public List<XADDINode> getInternalNodes() {
        return decisionNodes;
    }

    @Override
    public String toString() {
        return "decisions=" + decisions + ", decisionValues=" + decisionValues;
    }

    public XADDINode getLastDecisionNode() {
        return decisionNodes.get(decisionNodes.size() - 1);
    }

    public Boolean getLastDecisionValue() {
        return decisionValues.get(decisionValues.size() - 1);
    }
} //end of inner class Path

public class CompletePath {
    private Path path;
    private XADDTNode leaf;
    private double pathVolume;

    public CompletePath(Path path, XADDTNode leaf, double pathVolume) {
        this.path = path;
        this.leaf = leaf;
        this.pathVolume = pathVolume;
    }

    public XADDTNode getLeaf() {
        return leaf;
    }

    public List<XADDINode> getPathInternalNodes() {
        return path.getInternalNodes();
    }
}

//-----------------------------



@Deprecated
public class PathMassCalculator extends XADDLeafOperation {
    private Map<Path, Double> _pathMassMap = new HashMap<Path, Double>();
    private Map<Path, Double> _pathVolumeMap = new HashMap<Path, Double>();

    private Map<List<Boolean>, List<XADDINode>> _pathNodesMap;

    public PathMassCalculator(int xaddRootNodeId) {
        _pathNodesMap = getPathDecisionMap(xaddRootNodeId);
    }

    public Map<List<Boolean>, List<XADDINode>> getPathDecisionMap(int xaddNodeId) {

        XADDNode node = getExistNode(xaddNodeId);

        if (!(node instanceof XADDINode)) {
            Map<List<Boolean>, List<XADDINode>> map = new HashMap<List<Boolean>, List<XADDINode>>();
            map.put(new ArrayList<Boolean>(), new ArrayList<XADDINode>());
            return map;
        }

        XADDINode iNode = (XADDINode) node;

//            Decision d = _alOrder.get(iNode._var);

        Map<List<Boolean>, List<XADDINode>> highChildMap = getPathDecisionMap(iNode._high);
        Map<List<Boolean>, List<XADDINode>> lowChildMap = getPathDecisionMap(iNode._low);

        for (List<Boolean> hChildPath : highChildMap.keySet()) {
            List<XADDINode> hChildDec = highChildMap.get(hChildPath);
            //adds new elements to the beginning of the lists... a more efficient way may  be used...
            hChildPath.add(0, true); //todo: make sure this does not produces a bug
            hChildDec.add(0, iNode);
        }
        for (List<Boolean> lChildPath : lowChildMap.keySet()) {
            List<XADDINode> lChildDec = lowChildMap.get(lChildPath);
            //adds new elements to the beginning of the lists... a more efficient way may  be used...
            lChildPath.add(0, false); //todo: make sure this does not produces a bug
            lChildDec.add(0, iNode);
        }

        Map<List<Boolean>, List<XADDINode>> map = new HashMap<List<Boolean>, List<XADDINode>>(highChildMap.size() + lowChildMap.size());
        map.putAll(highChildMap);
        map.putAll(lowChildMap);

        return map;
    }

    @Override
    public boolean isCacheable() {
        return false;
    }

    @Override
    // todo: Here I have done a very stupid thing. given "decisions" and "decision_values", I extract "XADDNode"s.
    // todo: ...However, it would be MUCH more efficient if I would  make a new method juxt like "reduceProcessXADDLeaf"
    // todo: ... that passes nodes as arguments.
    // todo: ... I SHOULD make it so, as soon as possible....
    public int processXADDLeaf(ArrayList<Decision> decisions,
                               ArrayList<Boolean> decision_values, ArithExpr leaf_val) {
        XADDLeafDefIntegral _leafIntegrator;
        XADDLeafDefIntegral _volumeIntegrator; //to calculate the volume of the space covered by the path
        HashSet<String> pathVars = new HashSet<String>();
        for (Decision decision : decisions) {
            decision.collectVars(pathVars);
        }

        if (DEBUG_XADD_PATH_MASS) {
            HashSet<String> leafVars = new HashSet<String>();
            leaf_val.collectVars(leafVars);
            if (!pathVars.containsAll(leafVars)) {
                throw new RuntimeException("there is a variable in leaf variables: " + leafVars + " not in path vars: " + pathVars);
            }
        }

        ArithExpr pathMass = leaf_val;
        ArithExpr pathVolume = new DoubleExpr(1d);

        // Marginalize out every variable occurred in the path:
        if (pathVars.isEmpty()) {
            throw new RuntimeException("no variable in path!");
        }

        if (DEBUG_XADD_PATH_MASS) System.out.println("leaf_val = " + leaf_val);

        //todo this is wrong !!!!!!!!!vars should be integrated in loop till no var remains
        for (String var : pathVars) {
            _leafIntegrator = new XADDLeafDefIntegral(var);
            _leafIntegrator.processXADDLeaf(decisions, decision_values, pathMass);
            XADDNode pathMassNode = getNode(_leafIntegrator._runningSum);
            if (!(pathMassNode instanceof  XADDTNode)) {
                //TODO WHAT SHOULD I DO?
                System.out.println("THIS NODE IS NOT A LEAF: pathMassNode = " + pathMassNode);
            }
            XADDTNode pathMassTNode = (XADDTNode) pathMassNode;
//                XADDNode pathMassNode = (XADDTNode) getNode(_leafIntegrator._runningSum);.....
            pathMass = pathMassTNode._expr;  // after marginalization of each variable, the result of the integral is updated

            _volumeIntegrator = new XADDLeafDefIntegral(var);
            _volumeIntegrator.processXADDLeaf(decisions, decision_values, pathVolume);
            System.out.println("getNode(_volumeIntegrator._runningSum) = " + getNode(_volumeIntegrator._runningSum));
            XADDNode pathVolumeNode = getNode(_volumeIntegrator._runningSum);
            if (!(pathVolumeNode instanceof XADDTNode)) {
                //TODO WHAT SHOULD I DO?
                System.err.println("THIS VOLUME NODE IS NOT A LEAF:" + pathVolumeNode);
            }
            XADDTNode pathVolumeTNode = (XADDTNode) pathVolumeNode;
            pathVolume = pathVolumeTNode._expr;
        }

        Double mass = ((DoubleExpr) pathMass)._dConstVal;
        Double volume = ((DoubleExpr) pathVolume)._dConstVal;

        //have to be cloned since the objects are reusable.
        ArrayList<Decision> clonedDecisions = (ArrayList<Decision>) decisions.clone();
        ArrayList<Boolean> clonedDecisionValues = (ArrayList<Boolean>) decision_values.clone();
        List<XADDINode> nodes = _pathNodesMap.get(clonedDecisionValues);
        if (nodes == null) throw new RuntimeException("an unexpected error!!!");
        Path path = new Path(clonedDecisions, clonedDecisionValues, nodes);
        _pathMassMap.put(path, mass);
        _pathVolumeMap.put(path, volume);
        return getTermNode(leaf_val);
    }

       */
/* private int computeDefiniteConditionalIntegral(int xaddId, List<String> integrationVars,
                                                       ArrayList<Decision> extraDecisions, ArrayList<Boolean> extraDecisionValues) {
            int resultId = xaddId;
            for (String integrationVar : integrationVars) {
                ConditionalIntegrator integrator = new ConditionalIntegrator(integrationVar, extraDecisions, extraDecisionValues);
                reduceProcessXADDLeaf(resultId, integrator, *//*
*/
/* canonical_reorder *//*
*/
/*false);
                resultId = integrator.getFinalResultXaddId();
            }
            return resultId;
        }*//*


    public Map<Path, Double> getPathMassMap() {
        return _pathMassMap;
    }

    public Map<Path, Double> getPathVolumeMap() {
        return _pathVolumeMap;
    }
}  //end PathMass inner class.

*/
/**
 * Same as XADDLeafDefIntegral but with extra conditions passed to it.
 *//*

    */
/*public class ConditionalIntegrator extends XADDLeafOperation {
        ArrayList<Decision> extraDecisions;
        ArrayList<Boolean> extraDecisionValues;
        XADDLeafDefIntegral integrator;

        ConditionalIntegrator(String integrationVariable, ArrayList<Decision> extraDecisions, ArrayList<Boolean> extraDecisionValues) {
            integrator = new XADDLeafDefIntegral(integrationVariable);

            this.extraDecisions = extraDecisions;
            this.extraDecisionValues = extraDecisionValues;
        }

        @Override
        public int processXADDLeaf(ArrayList<Decision> decisions, ArrayList<Boolean> decision_values, ArithExpr leaf_val) {
            ArrayList<Decision> extendedDecisionList = new ArrayList<Decision>(extraDecisions);
            extendedDecisionList.addAll(decisions);

            ArrayList<Boolean> extendedDecisionValueList = new ArrayList<Boolean>(extraDecisionValues);
            extendedDecisionValueList.addAll(decision_values);

            return integrator.processXADDLeaf(extendedDecisionList, extendedDecisionValueList, leaf_val);
        }

        @Override
        public boolean isCacheable() {
            return integrator.isCacheable();
        }

        public int getFinalResultXaddId(){
            return integrator._runningSum;
        }
    }*//*


    */
/* END OF PATH MASS PART*//*



*/
