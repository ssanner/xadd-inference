package hgm.poly.gm;

import java.util.Collection;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 3/07/14
 * Time: 4:02 AM
 *
 * Graphical model used for symbolic integral
 */
public interface GraphicalModel {
    /**
     * It should be guaranteed that [at least in the case of deterministic factors] parents come before children
     */
    List<Factor> allInferenceRelevantFactors(Collection<String> vars);

    List<String> allDeterministicVars();

}
