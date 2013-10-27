package hgm.asve.model;

import hgm.asve.FactorParentsTuple;
import hgm.asve.factor.OLD_IFactor;
import hgm.asve.factor.OLD_MockFactor;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Created by Hadi Afshar.
 * Date: 19/09/13
 * Time: 3:43 PM
 */
public class SimpleBayesianGraphicalModelTest {

    OLD_IFactor fA = new OLD_MockFactor("A");
    OLD_IFactor fB = new OLD_MockFactor("B");
    OLD_IFactor fC = new OLD_MockFactor("C");
    OLD_IFactor fD = new OLD_MockFactor("D");
    OLD_IFactor fE = new OLD_MockFactor("E");
    OLD_IFactor fF = new OLD_MockFactor("F");

    SimpleBayesianGraphicalModel model = new SimpleBayesianGraphicalModel(
            new FactorParentsTuple(fA, new OLD_IFactor[]{}),
            new FactorParentsTuple(fB, new OLD_IFactor[]{}),
            new FactorParentsTuple(fC, new OLD_IFactor[]{}),
            new FactorParentsTuple(fD, new OLD_IFactor[]{fB, fC}),
            new FactorParentsTuple(fE, new OLD_IFactor[]{fA, fD}),
            new FactorParentsTuple(fF, new OLD_IFactor[]{fE}));

    @Test
    public void testCalcMaxDistanceFromLeaf() {

        Assert.assertEquals(model.calcMaxDistanceFromLeaf(fB), 0);
        Assert.assertEquals(model.calcMaxDistanceFromLeaf(fE), 2);
        Assert.assertEquals(model.calcMaxDistanceFromLeaf(fF), 3);
    }

    @Test
    public void testSortedVariables() {
        Assert.assertEquals(model.getSortedFactors().toString(), "[A, B, C, D, E, F]");

    }

}
