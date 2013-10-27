package hgm.asve;

import xadd.XADD;

import java.util.ArrayList;

/**
 * Created by Hadi Afshar.
 * Date: 23/10/13
 * Time: 6:52 PM
 */

/**
 * A path from a node of an XADD down to its high or low child. If the last child is a terminal then the path is complete.
 * Root is XaddPath.get(0).
 */
public class XaddPath extends ArrayList<XADD.XADDNode> {
    private XADD context;

    public XaddPath(XADD context) {
        super();
        this.context = context;
    }

    /**
     * throws exception is the path is not complete.
     */
    public XADD.XADDTNode getLeaf() {
        return (XADD.XADDTNode) this.get(this.size() - 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");

        for (XADD.XADDNode node : this) {
              if (node instanceof XADD.XADDINode) {
                  XADD.XADDINode iNode = (XADD.XADDINode) node;
                  XADD.Decision decision = context._alOrder.get(iNode._var);
                  sb.append(decision.toString()).append(" --> ");
              } else {
                  XADD.XADDTNode tNode = (XADD.XADDTNode) node;
                  sb.append(tNode._expr.toString()).append(" |]");
              }
        }
        return sb.toString();
    }
}
