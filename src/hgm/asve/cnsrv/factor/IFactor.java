package hgm.asve.cnsrv.factor;

import hgm.Variable;

import java.util.Set;

/**
 * Created by Hadi Afshar.
 * Date: 19/09/13
 * Time: 2:34 PM
 */

/*
 An interface of all factors
 */
public interface IFactor {
    /**
     * @return All scope variables (INCLUDING the associated variable)
     */
    public Set<String> getScopeVars();

    public String getHelpingText();
}
