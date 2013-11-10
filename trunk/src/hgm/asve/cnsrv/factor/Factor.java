package hgm.asve.cnsrv.factor;

import xadd.XADD;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Hadi Afshar.
 * Date: 2/10/13
 * Time: 12:17 AM
 */
public class Factor implements IFactor{
    public int _xadd;
    public HashSet<String> _vars;
    public XADD _localContext;

    private String _helpingText;

    public Factor(int xadd, XADD context, String helpingText) {
        _xadd = xadd;
        _localContext = context;
        _vars = context.collectVars(_xadd);
        _helpingText = helpingText;
    }

    public String toString() {
        return "<"+_helpingText + ">: " + _vars + ":\n" + _localContext.getString(_xadd);
    }

    @Override
    public Set<String> getScopeVars() {
        return _vars;
    }


    @Override
    public String getHelpingText() {
        return _helpingText;
    }

    public XADD.XADDNode getNode(){
        return _localContext.getExistNode(_xadd);
    }

    public int getLeafCount() {
        return _localContext.getLeafCount(_xadd);
    }

    public int getNodeCount() {
        return _localContext.getNodeCount(_xadd);
    }

    public int getBranchCount() {
        return _localContext.getBranchCount(_xadd);
    }

    // NOTE: instantiated factors associated with different variables can point to a same xadd.
    //So equality based on xadd is not possible.

    //Alternatively, factors may contain associated variables however, joint/marginalized factors are not associated to a single variable.
    //Therefore, we do not define, data-based hash codes for Factors.

    /*@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Factor factor = (Factor) o;

        if (_xadd != factor._xadd) return false;
        if (!_localContext.equals(factor._localContext)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = _xadd;
        result = 31 * result + _localContext.hashCode();
        return result;
    }*/
}
