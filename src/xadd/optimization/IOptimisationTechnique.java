package xadd.optimization;

import java.util.*;

/**
 *
 */
public interface IOptimisationTechnique {


    /**
     *
     * @param objective
     * @param constraints
     * @param lowerBounds
     * @param upperBounds
     */
    public double run(String objective, Collection<String> constraints,
                         Collection<String> lowerBounds, Collection<String> upperBounds);
}
