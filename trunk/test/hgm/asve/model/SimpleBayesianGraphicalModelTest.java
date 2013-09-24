package hgm.asve.model;

import hgm.Variable;
import hgm.asve.FactorParentsTuple;
import hgm.asve.factor.IFactor;
import hgm.asve.factor.MockFactor;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Created by Hadi Afshar.
 * Date: 19/09/13
 * Time: 3:43 PM
 */
public class SimpleBayesianGraphicalModelTest {
//    Variable a = new Variable("A");
//    Variable b = new Variable("B");
//    Variable c = new Variable("C");
//    Variable d = new Variable("D");
//    Variable e = new Variable("E");
//    Variable f = new Variable("F");

    IFactor fA = new MockFactor("A");
    IFactor fB = new MockFactor("B");
    IFactor fC = new MockFactor("C");
    IFactor fD = new MockFactor("D");
    IFactor fE = new MockFactor("E");
    IFactor fF = new MockFactor("F");

    SimpleBayesianGraphicalModel model = new SimpleBayesianGraphicalModel(
            new FactorParentsTuple(fA, new IFactor[]{}),
            new FactorParentsTuple(fB, new IFactor[]{}),
            new FactorParentsTuple(fC, new IFactor[]{}),
            new FactorParentsTuple(fD, new IFactor[]{fB, fC}),
            new FactorParentsTuple(fE, new IFactor[]{fA, fD}),
            new FactorParentsTuple(fF, new IFactor[]{fE}));

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
