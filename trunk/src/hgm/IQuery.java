package hgm;

import java.util.List;
import java.util.Set;

/**
 * Created by Hadi Afshar.
 * Date: 19/09/13
 * Time: 4:55 PM
 */
public interface IQuery {
    Set<InstantiatedVariable> getEvidence();

    Set<Variable> getNonInstantiatedEvidenceVariables();

    List<Variable> getQueries();
}
