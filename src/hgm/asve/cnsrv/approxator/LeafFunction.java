package hgm.asve.cnsrv.approxator;

import xadd.XADD;

import java.util.List;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 16/10/13
 * Time: 12:58 AM
 */
public abstract class LeafFunction {

    /**
     * @param leaf       leaf for which the function is calculated
     * @param pathToLeaf path does not include the leaf node itself
     * @return function of the leaf (and its path)
     */
    public abstract XADD.XADDNode func(XADD.XADDTNode leaf, List<XADD.XADDINode> pathToLeaf);

    public static LeafFunction identityFunction() {
        return new LeafFunction() {
            @Override
            public XADD.XADDNode func(XADD.XADDTNode leaf, List<XADD.XADDINode> pathToLeaf) {
                return leaf;
            }
        };
    }

    public static LeafFunction oneFunction(final XADD context) {
        return new LeafFunction() {
            XADD.XADDNode one = context.getExistNode(context.ONE);

            @Override
            public XADD.XADDNode func(XADD.XADDTNode leaf, List<XADD.XADDINode> pathToLeaf) {
                return one;
            }
        };
    }

    public static LeafFunction squaredErrorWithSibling(final XADD context) {
        /**
         * Returns infinity if the sibling does not exist or is not a leaf
         */
        return new LeafFunction() {
            @Override
            public XADD.XADDNode func(XADD.XADDTNode leaf, List<XADD.XADDINode> pathToLeaf) {
                XADD.XADDNode infinity = context.getExistNode(context.POS_INF);

                if (pathToLeaf.size() < 1) return infinity; //no parent hence no sibling

                XADD.XADDINode parent = pathToLeaf.get(pathToLeaf.size() - 1);
//                System.out.println("leaf = " + leaf);
//                System.out.println("parent = " + parent);
                int siblingId = (parent._high == context._hmNode2Int.get(leaf)) ? parent._low : parent._high;
                XADD.XADDNode siblingNode = context.getExistNode(siblingId);
//                System.out.println("siblingNode = " + siblingNode);
                if (!(siblingNode instanceof XADD.XADDTNode)) return infinity; //sibling is not a leaf

                int differenceId = context.applyInt(context._hmNode2Int.get(leaf), siblingId, XADD.MINUS);
//                System.out.println("context.getExistNode(differenceId) = " + context.getExistNode(differenceId));

                int squaredDifferenceId = context.applyInt(differenceId, differenceId, XADD.PROD);
//                System.out.println("context.getExistNode(squaredDifferenceId) = " + context.getExistNode(squaredDifferenceId));

                return context.getExistNode(squaredDifferenceId);
            }
        };
    }
}
