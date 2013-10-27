package hgm.asve.cnsrv;

import hgm.asve.cnsrv.factor.Factor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Hadi Afshar.
 * Date: 7/10/13
 * Time: 3:12 AM
 */
public class FactorSet extends HashSet<Factor> {
    public FactorSet() {
        super();
    }

    public FactorSet(Collection<? extends Factor> c) {
        super(c);
    }

    public Set<String> getScopeVars() {
        Set<String> vars = new HashSet<String>();
        for (Factor f : this) {
            vars.addAll(f.getScopeVars());
        }
        return vars;
    }

    public int predictedJointPathCount() {
        int c = 1;
        for (Factor f : this) {
            c *= f.getBranchCount();
        }
        return c;
    }
}
