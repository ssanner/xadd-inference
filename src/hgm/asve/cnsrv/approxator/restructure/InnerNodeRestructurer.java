package hgm.asve.cnsrv.approxator.restructure;

import xadd.XADD;

/**
 * Created by Hadi Afshar.
 * Date: 15/11/13
 * Time: 12:02 PM
 */
//todo write a class that refactors the inner nodes of an XADD by pushing up decisions that divide the space to homogeneous parts:
// 1. given an XADD and its sampleDB, extract all decisions (inner-nodes-vars), if n of them exist then 2^n combinations of decisions for space partitioning exists.
// 2. nest the samples in these 2^n partitions. remove empty partitions (or somehow do not create them in the first place)
// 3. chose a partitioning decision that partitions the space in such a way that the samples in one side of it are associated with a unique target value (or minimum target flags) (in average?) greedily.
// ...
public class InnerNodeRestructurer {
    public XADD.XADDNode restructure(XADD.XADDNode root) {
        return null;
    }

}
