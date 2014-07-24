package hgm.poly;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Hadi Afshar.
 * Date: 5/07/14
 * Time: 12:18 AM
 */
public class CaseStatementConstraints extends HashSet<Polynomial> {
    public CaseStatementConstraints(Collection<Polynomial> constraints) {
        super(constraints);
    }

    public CaseStatementConstraints(int initialCapacity) {
        super(initialCapacity);
    }

    public CaseStatementConstraints substitute(Map<String, Double> assign) {
        CaseStatementConstraints result = new CaseStatementConstraints(this.size());
        for (Polynomial polynomial : this) {
            result.add(polynomial.substitute(assign));
        }
        return result;
    }

    //todo complete by semantic tests such as e.g. x>2 entails x>1
    public boolean isEntailedBy(CaseStatementConstraints other) {
//        if (this.size() > other.size()) return false;

        for (Polynomial otherCn : other) {
            boolean isInThis = false;
            for (Polynomial cn : this) {
                if (otherCn.equals(cn)) {
                    isInThis = true;
                    break;
                }
            }
            if (!isInThis) return false;
        }
        return true;
    }
}
