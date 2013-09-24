package hgm.asve.engine;

import hgm.InstantiatedVariable;
import hgm.QueryImpl;
import hgm.Variable;
import hgm.asve.FactorParentsTuple;
import hgm.asve.factor.MockFactor;
import hgm.asve.factory.MockFactory;
import hgm.asve.model.SimpleBayesianGraphicalModel;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by Hadi Afshar.
 * Date: 21/09/13
 * Time: 8:45 PM
 */
public class ApproxSveInferenceEngineTest {

    Variable vA = MockFactor.A;
    Variable vB = MockFactor.B;
    Variable vC = MockFactor.C;
    Variable vD = MockFactor.D;
    Variable vE = MockFactor.E;
    Variable vF = MockFactor.F;

    MockFactor fA = new MockFactor(vA, "pr([A])");
    MockFactor fB = new MockFactor(vB, "pr([B])");
    MockFactor fC = new MockFactor(vC, "pr([C]|[A][B])");
    MockFactor fD = new MockFactor(vD, "pr([D])");
    MockFactor fE = new MockFactor(vE, "pr([E]|[C][D])");
    MockFactor fF = new MockFactor(vF, "pr([F]|[E])");

    SimpleBayesianGraphicalModel<MockFactor> model = new SimpleBayesianGraphicalModel<MockFactor>(
            new FactorParentsTuple<MockFactor>(fA, new MockFactor[]{}),
            new FactorParentsTuple<MockFactor>(fB, new MockFactor[]{}),
            new FactorParentsTuple<MockFactor>(fC, new MockFactor[]{fA, fB}),
            new FactorParentsTuple<MockFactor>(fD, new MockFactor[]{}),
            new FactorParentsTuple<MockFactor>(fE, new MockFactor[]{fC, fD}),
            new FactorParentsTuple<MockFactor>(fF, new MockFactor[]{fE}));

    @Test
    public void testInferOnMocks() throws Exception {

        InferenceEngine<MockFactor> aSveEngine = new ApproxSveInferenceEngine<MockFactor>(model, new MockFactory(), 10);

        MockFactor result = aSveEngine.infer(new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(vF)));
        //todo If var ordering should never be given then the class can be misused. Also what about partial ordering?

        Assert.assertEquals("[([DEF_INT_{E}<" +
                "[(pr([F]|E)).([DEF_INT_{D}<[DEF_INT_{C}<" +
                "[(pr(D)).(pr(E|CD)).([DEF_INT_{B}<[DEF_INT_{A}<" +
                "[(pr(A)).(pr(B)).(pr(C|AB))]" +
                ">]>])]" +
                ">]>])]" +
                ">])]", result.getText());
        Assert.assertEquals(1, result.getScopeVars().size());
        Assert.assertEquals(vF, result.getScopeVars().iterator().next());

/*
        try {
            result = aSveEngine.infer(
                    new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(MockFactor.Y, MockFactor.X)),
                    Arrays.asList(MockFactor.X, MockFactor.Y));
            Assert.fail("ordered list of variables should contain all variables in the relevant scope.");
        } catch (Exception e) {
        }


        //check take factors:
        aSveEngine.takeFactors(Arrays.asList(
                new MockFactor("pr([X])"),
                new MockFactor("pr([Y]|[X])"),
                new MockFactor("pr([Z]|[Y])"),
                new MockFactor("pr([W]|[Z])")));

        try {
            result = aSveEngine.infer(
                    new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(MockFactor.W)),
                    Arrays.asList(MockFactor.X, MockFactor.Y, MockFactor.Z));

            Assert.fail();
        } catch (Exception e) {
        }

        result = aSveEngine.infer(
                new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(MockFactor.W)),
                Arrays.asList(MockFactor.X, MockFactor.Y, MockFactor.Z, MockFactor.W));

        Assert.assertEquals("[DEF_INT_{[Z]}<[(pr([W]|[Z])).([DEF_INT_{[Y]}<[(pr([Z]|[Y]))." +
                "([DEF_INT_{[X]}<[(pr([X])).(pr([Y]|[X]))]>])]>])]>]", result.getText());

        result = aSveEngine.infer(
                new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(MockFactor.W)),
                Arrays.asList(MockFactor.W, MockFactor.Z, MockFactor.Y, MockFactor.X));

        Assert.assertEquals("[DEF_INT_{[X]}<[(pr([X])).([DEF_INT_{[Y]}<[(pr([Y]|[X]))." +
                "([DEF_INT_{[Z]}<[(pr([W]|[Z])).(pr([Z]|[Y]))]>])]>])]>]", result.getText());

*/
    }

    @Test
    public void testInferOnMocks2() throws Exception {

        InferenceEngine<MockFactor> aSveEngine = new ApproxSveInferenceEngine<MockFactor>(model, new MockFactory(), 10);

        MockFactor result = aSveEngine.infer(new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(vC, vD)));

        Assert.assertEquals("[(pr([D]))." +
                "([DEF_INT_{B}<[DEF_INT_{A}<[(pr(A)).(pr(B)).(pr([C]|AB))]>]>])]", result.getText());
        Assert.assertEquals(2, result.getScopeVars().size());
        Assert.assertEquals(new HashSet<Variable>(Arrays.asList(vC, vD)), result.getScopeVars());

        //-----

        result = aSveEngine.infer(new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(vC, vF)));

        Assert.assertEquals("[([DEF_INT_{E}<" +
                "[(pr([F]|E)).([DEF_INT_{D}<" +
                "[(pr(D)).(pr(E|[C]D)).([DEF_INT_{B}<[DEF_INT_{A}<[(pr(A)).(pr(B)).(pr([C]|AB))]>]>])]" +
                ">])]" +
                ">])]", result.getText());
        Assert.assertEquals(2, result.getScopeVars().size());
        Assert.assertEquals(new HashSet<Variable>(Arrays.asList(vC, vF)), result.getScopeVars());
    }

    private String OOOO = ""; //just used for spacing...

    @Test
    public void testApproximation() {
        MockFactor fA = new MockFactor(vA, "pr([A])");
        MockFactor fB = new MockFactor(vB, "pr([B])");
        MockFactor fC = new MockFactor(vC, "pr([C]|[A][B])");
        MockFactor fD = new MockFactor(vD, "pr([D])");
        MockFactor fE = new MockFactor(vE, "pr([E]|[A][B][C][D])");
        MockFactor fF = new MockFactor(vF, "pr([F]|[D][E])");

        SimpleBayesianGraphicalModel<MockFactor> model2 = new SimpleBayesianGraphicalModel<MockFactor>(
                new FactorParentsTuple<MockFactor>(fA, new MockFactor[]{}),
                new FactorParentsTuple<MockFactor>(fB, new MockFactor[]{}),
                new FactorParentsTuple<MockFactor>(fC, new MockFactor[]{fA, fB}),
                new FactorParentsTuple<MockFactor>(fD, new MockFactor[]{}),
                new FactorParentsTuple<MockFactor>(fE, new MockFactor[]{fA, fB, fC, fD}),
                new FactorParentsTuple<MockFactor>(fF, new MockFactor[]{fD, fE}));
        InferenceEngine<MockFactor> aSveEngine = new ApproxSveInferenceEngine<MockFactor>(model2, new MockFactory(), 1);

        MockFactor result = aSveEngine.infer(new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(vF)));

        Assert.assertEquals(
                "[(APPROX(\"" +
                        "[([DEF_INT_{D}<[DEF_INT_{E}<" +
                        OOOO + "[(pr([F]|DE)).(APPROX(\"" +
                        OOOO + OOOO + "[([DEF_INT_{C}<[DEF_INT_{B}<[DEF_INT_{A}<" +
                        OOOO + OOOO + "[(pr(E|ABCD)).(APPROX(\"[(pr(D))]\")).(APPROX(\"" +
                        OOOO + OOOO + OOOO + "[(pr(C|AB)).(APPROX(\"[(pr(B))]\")).(APPROX(\"[(pr(A))]\"))]" +
                        OOOO + OOOO + OOOO + "\"))]" +
                        OOOO + OOOO + ">]>]>])]" +
                        OOOO + OOOO + "\"))]" +
                        ">]>])]" +
                        "\"))]", result.getText());
        Assert.assertEquals(1, result.getScopeVars().size());
        Assert.assertEquals(vF, result.getScopeVars().iterator().next());
    }
        @Test
        public void testApproximation2() {
            MockFactor fA = new MockFactor(vA, "pr([A])");
            MockFactor fB = new MockFactor(vB, "pr([B])");
            MockFactor fC = new MockFactor(vC, "pr([C]|[A][B])");
            MockFactor fD = new MockFactor(vD, "pr([D])");
            MockFactor fE = new MockFactor(vE, "pr([E]|[A][B][C][D])");
            MockFactor fF = new MockFactor(vF, "pr([F]|[C][D][E])");

            SimpleBayesianGraphicalModel<MockFactor> model2 = new SimpleBayesianGraphicalModel<MockFactor>(
                    new FactorParentsTuple<MockFactor>(fA, new MockFactor[]{}),
                    new FactorParentsTuple<MockFactor>(fB, new MockFactor[]{}),
                    new FactorParentsTuple<MockFactor>(fC, new MockFactor[]{fA, fB}),
                    new FactorParentsTuple<MockFactor>(fD, new MockFactor[]{}),
                    new FactorParentsTuple<MockFactor>(fE, new MockFactor[]{fA, fB, fC, fD}),
                    new FactorParentsTuple<MockFactor>(fF, new MockFactor[]{fC, fD, fE}));
            InferenceEngine<MockFactor> aSveEngine = new ApproxSveInferenceEngine<MockFactor>(model2, new MockFactory(), 2);

            MockFactor result = aSveEngine.infer(new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(vF)));
            Assert.assertEquals(
                    "[([DEF_INT_{D}<[DEF_INT_{E}<[DEF_INT_{C}<" +
                            "[(pr([F]|CDE)).(APPROX(\"" +
                            "[([DEF_INT_{B}<[DEF_INT_{A}<[(pr(D)).(pr(E|ABCD)).(APPROX(\"[(pr(A)).(pr(B)).(pr(C|AB))]\"))]>]>])]" +
                            "\"))]" +
                            ">]>]>])]", result.getText());

    }
}
