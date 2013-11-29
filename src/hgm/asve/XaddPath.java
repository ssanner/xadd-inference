package hgm.asve;

import xadd.XADD;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    public XaddPath(Collection<? extends XADD.XADDNode> xaddNodes, XADD context) {
        super(xaddNodes);
        this.context = context;
    }

    public XADD.XADDNode getLastNode() {
        return this.get(this.size() - 1);
    }

    /**
     * @return sibling of this xadd path.
     *         <p> Sibling of a path is another path in which only the last node is the sibling of the last node of the original path.
     */
    public XaddPath pathSibling() throws NoSiblingException {
        if (this.size() < 2) throw new NoSiblingException();

        XADD.XADDNode lastNode = this.getLastNode();
        XADD.XADDINode lastNodeParent = (XADD.XADDINode) get(this.size() - 2);
        Integer lastNodeId = context._hmNode2Int.get(lastNode);

        Integer lastNodeSiblingId = (lastNodeParent._high == lastNodeId) ? lastNodeParent._low : lastNodeParent._high;
        XaddPath pathSibling = new XaddPath(this.subList(0, this.size() - 1), context);
        pathSibling.add(context._hmInt2Node.get(lastNodeSiblingId));
        return pathSibling;
    }

    public boolean isComplete() {
        return getLastNode() instanceof XADD.XADDTNode;
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

    public void setLastNodeTo(XADD.XADDNode newLastNode) {
        this.set(this.size() - 1, newLastNode);
    }

    public void removeLastNode() {
        this.remove(this.size() - 1);
    }

    public boolean startsWith(XaddPath subPath) {
        if (subPath.size() > this.size()) return false;
        if (subPath.isEmpty()) return this.isEmpty();
        if (this.isEmpty()) return subPath.isEmpty();

        for (int i = subPath.size() - 1; i >= 0; i--) { //in the inverse manner, the dissimilarity is more likely to be discovered sooner
            if (subPath.get(i) != this.get(i)) return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        if (!super.equals(o)) return false;

        XaddPath xaddNodes = (XaddPath) o;

        if (!context.equals(xaddNodes.context)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + context.hashCode();
        return result;
    }
}
