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
public class CaseStatementConstraints<E extends Expression> extends HashSet<E> {
    public CaseStatementConstraints(Collection<E> constraints) {
        super(constraints);
    }

    public CaseStatementConstraints(int initialCapacity) {
        super(initialCapacity);
    }

    public CaseStatementConstraints<E> substitute(Map<String, Double> assign) {
        CaseStatementConstraints<E> result = new CaseStatementConstraints<E>(this.size());
        for (E polynomial : this) {
            result.add((E)polynomial.substitute(assign));  //todo I do not know how to prevent this weird  casting!!!
        }
        return result;
    }

    public CaseStatementConstraints<E> substitute(String var, Expression value) {
        CaseStatementConstraints<E> result = new CaseStatementConstraints<E>(this.size());
        for (E expr : this) {
            result.add((E)expr.substitute(var, value));  //todo I do not know how to prevent this weird  casting!!!
        }
        return result;
    }

    //todo complete by semantic tests such as e.g. x>2 entails x>1
    public boolean isEntailedBy(CaseStatementConstraints<E> other) {
//        if (this.size() > other.size()) return false;

        for (Expression otherCn : other) {
            boolean isInThis = false;
            for (Expression cn : this) {
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
