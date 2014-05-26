package hgm.preference.db;

import hgm.poly.bayesian.PriorHandler;

import java.util.Collection;

/**
 * Created by Hadi Afshar.
 * Date: 26/01/14
 * Time: 10:10 PM
 */
public abstract class DiscretePreferenceDatabase extends PreferenceDatabase {
    protected DiscretePreferenceDatabase(PriorHandler prior) {
        super(prior);
    }

    /**
     *
     * @param attribId from 0 to getNumberOfAttributes() - 1
     * @return list of discretization levels
     */
    public abstract Collection<Double> getAttribChoices(Integer attribId);
}
