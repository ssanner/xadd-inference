package xadd.optimization;

import java.util.Collection;

public class MATLABNonLinear implements IOptimisationTechnique {

    @Override
    public double run(String objective, Collection<String> constraints, Collection<String> lowerBounds,
                    Collection<String> upperBounds) {

        System.out.println("Minimise: " + objective);
        System.out.println("Subject to:");

        for(String constraint : constraints) {
            System.out.println("\t" + constraint);
        }

        lowerBounds.addAll(upperBounds);

        for(String bound : lowerBounds) {
            System.out.println("\t" + bound);
        }

        return 0.0;
    }
}
