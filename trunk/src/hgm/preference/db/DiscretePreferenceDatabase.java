package hgm.preference.db;

import java.util.Collection;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 26/01/14
 * Time: 10:10 PM
 */
public interface DiscretePreferenceDatabase extends PreferenceDatabase {
    /**
     *
     * @param attribId from 0 to getNumberOfAttributes() - 1
     * @return list of discretization levels
     */
    public Collection<Double> getAttribChoices(Integer attribId);
}
