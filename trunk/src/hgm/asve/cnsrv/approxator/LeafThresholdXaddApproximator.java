package hgm.asve.cnsrv.approxator;

import xadd.ExprLib;
import xadd.XADD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 16/1/14
 * Time: 12:00 PM
 */

/**
 * A very simple approximator that only works on XADDs with constant leaves and merges leafs with value less than a threshold.
 */
public class LeafThresholdXaddApproximator implements Approximator {
    private XADD context = null;
    private double minAcceptableValueToMaxLeafValueRatio;

    public LeafThresholdXaddApproximator(XADD context, double minAcceptableValueToMaxLeafValueRatio) {
        setupWithContext(context);
        this.minAcceptableValueToMaxLeafValueRatio = minAcceptableValueToMaxLeafValueRatio;
    }

    @Override
    public void setupWithContext(XADD context) {
        this.context = context;
    }

    @Override
    public XADD.XADDNode approximateXadd(XADD.XADDNode node) {
        List<Double> positiveLeafValues = collectSortedPositiveLeafValues(node);
        if (positiveLeafValues.size()<3) return node; // if there are only two values then nothing is changed

        Double maxValue = positiveLeafValues.get(positiveLeafValues.size() - 1);
        Double minValue = positiveLeafValues.get(0);
        double acceptanceThreshold = maxValue * minAcceptableValueToMaxLeafValueRatio;  //values above which are not changed and values below which are replaced with minValue

        if (minValue >= acceptanceThreshold) return node; //nothing can be simplified
        XADD.XADDNode replacingLeaf = context.getExistNode(context.getTermNode(new ExprLib.DoubleExpr(minValue)));

        return approximate(node, acceptanceThreshold, replacingLeaf);
    }

    /**
     *
     * @param node the "constant-leaf" node to be replaced
     * @param replaceThreshold threshold below which all positive leaves are replaced with...
     * @param replacingLeaf ... the replacing leaf...
     * @return and returned.
     */
    private XADD.XADDNode approximate(XADD.XADDNode node, double replaceThreshold, XADD.XADDNode replacingLeaf){
        if (node instanceof XADD.XADDTNode) {
            ExprLib.DoubleExpr leafExpr = (ExprLib.DoubleExpr) ((XADD.XADDTNode) node)._expr; // only works for constant leaves

            if (leafExpr._dConstVal <= 0.0) {
                return node; //only positive leaves are approximated
            }

            if (leafExpr._dConstVal < replaceThreshold) {
                return replacingLeaf;
            }

            return node;
        }
        else {
            XADD.XADDINode iNode = (XADD.XADDINode) node;
            XADD.XADDNode approxLow = approximate(iNode.getLowChild(), replaceThreshold, replacingLeaf);
            XADD.XADDNode approxHigh = approximate(iNode.getHighChild(), replaceThreshold, replacingLeaf);

            if (approxLow == approxHigh) {
                return approxLow;
            }

            return context.getExistNode(context.getINode(
                    iNode._var, context._hmNode2Int.get(approxLow), context._hmNode2Int.get(approxHigh)));
        }
    }

    private List<Double> collectSortedPositiveLeafValues(XADD.XADDNode root) {
        HashSet<XADD.XADDNode> nodes = root.collectNodes();
        List<Double> leafValues = new ArrayList<Double>();
        for (XADD.XADDNode node : nodes) {
            if (node instanceof XADD.XADDTNode) {

                double v = 0; // only works for constant leaves
                try {
                    v = ((ExprLib.DoubleExpr) ((XADD.XADDTNode) node)._expr)._dConstVal;
                } catch (Exception e) {
                    throw new RuntimeException("LeafThresholdXaddApproximator only works with 'constant-leaf' XADDs " +
                            "which is not the case for " + node);
                }
                if (v>0.0) {
                    leafValues.add(v);
                }
            }
        }
        Collections.sort(leafValues);
        return leafValues;
    }
}