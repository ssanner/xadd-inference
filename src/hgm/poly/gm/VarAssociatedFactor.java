package hgm.poly.gm;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by Hadi Afshar.
 * Date: 25/07/14
 * Time: 5:11 AM
 */
public abstract class VarAssociatedFactor implements Factor {
    protected String associatedVar;

    public String getAssociatedVar() {
        return associatedVar;
    }

    public abstract Set<String> getParentVars();
}
