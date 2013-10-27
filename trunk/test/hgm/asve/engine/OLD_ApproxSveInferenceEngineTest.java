package hgm.asve.engine;

import hgm.InstantiatedVariable;
import hgm.QueryImpl;
import hgm.Variable;
import hgm.asve.FactorParentsTuple;
import hgm.asve.factor.OLD_MockFactor;
import hgm.asve.factor.OLD_XADDFactor;
import hgm.asve.factory.MockFactory;
import hgm.asve.factory.OLD_XADDFactory;
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
@Deprecated
public class OLD_ApproxSveInferenceEngineTest {
    public static void main(String[] args) throws Exception {
        OLD_ApproxSveInferenceEngineTest instance = new OLD_ApproxSveInferenceEngineTest();
        instance.testInferOnXaddWithQuery1();
    }


    Variable vA = OLD_MockFactor.A;
    Variable vB = OLD_MockFactor.B;
    Variable vC = OLD_MockFactor.C;
    Variable vD = OLD_MockFactor.D;
    Variable vE = OLD_MockFactor.E;
    Variable vF = OLD_MockFactor.F;

    OLD_MockFactor fA = new OLD_MockFactor(vA, "pr([A])");
    OLD_MockFactor fB = new OLD_MockFactor(vB, "pr([B])");
    OLD_MockFactor fC = new OLD_MockFactor(vC, "pr([C]|[A][B])");
    OLD_MockFactor fD = new OLD_MockFactor(vD, "pr([D])");
    OLD_MockFactor fE = new OLD_MockFactor(vE, "pr([E]|[C][D])");
    OLD_MockFactor fF = new OLD_MockFactor(vF, "pr([F]|[E])");

    SimpleBayesianGraphicalModel<OLD_MockFactor> model = new SimpleBayesianGraphicalModel<OLD_MockFactor>(
            new FactorParentsTuple<OLD_MockFactor>(fA, new OLD_MockFactor[]{}),
            new FactorParentsTuple<OLD_MockFactor>(fB, new OLD_MockFactor[]{}),
            new FactorParentsTuple<OLD_MockFactor>(fC, new OLD_MockFactor[]{fA, fB}),
            new FactorParentsTuple<OLD_MockFactor>(fD, new OLD_MockFactor[]{}),
            new FactorParentsTuple<OLD_MockFactor>(fE, new OLD_MockFactor[]{fC, fD}),
            new FactorParentsTuple<OLD_MockFactor>(fF, new OLD_MockFactor[]{fE}));

    @Test
    public void testInferOnMocks() throws Exception {

        InferenceEngine<OLD_MockFactor> aSveEngine = new OLD_ApproxSveInferenceEngine<OLD_MockFactor>(model, new MockFactory(), 10);

        OLD_MockFactor result = aSveEngine.infer(new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(vF)));
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

    }

    @Test
    public void testInferOnMocks2() throws Exception {

        InferenceEngine<OLD_MockFactor> aSveEngine = new OLD_ApproxSveInferenceEngine<OLD_MockFactor>(model, new MockFactory(), 10);

        OLD_MockFactor result = aSveEngine.infer(new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(vC, vD)));

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

    @Test
    public void testApproximationOnMock() {
        OLD_MockFactor fA = new OLD_MockFactor(vA, "pr([A])");
        OLD_MockFactor fB = new OLD_MockFactor(vB, "pr([B])");
        OLD_MockFactor fC = new OLD_MockFactor(vC, "pr([C]|[A][B])");
        OLD_MockFactor fD = new OLD_MockFactor(vD, "pr([D])");
        OLD_MockFactor fE = new OLD_MockFactor(vE, "pr([E]|[A][B][C][D])");
        OLD_MockFactor fF = new OLD_MockFactor(vF, "pr([F]|[D][E])");

        SimpleBayesianGraphicalModel<OLD_MockFactor> model2 = new SimpleBayesianGraphicalModel<OLD_MockFactor>(
                new FactorParentsTuple<OLD_MockFactor>(fA, new OLD_MockFactor[]{}),
                new FactorParentsTuple<OLD_MockFactor>(fB, new OLD_MockFactor[]{}),
                new FactorParentsTuple<OLD_MockFactor>(fC, new OLD_MockFactor[]{fA, fB}),
                new FactorParentsTuple<OLD_MockFactor>(fD, new OLD_MockFactor[]{}),
                new FactorParentsTuple<OLD_MockFactor>(fE, new OLD_MockFactor[]{fA, fB, fC, fD}),
                new FactorParentsTuple<OLD_MockFactor>(fF, new OLD_MockFactor[]{fD, fE}));
        InferenceEngine<OLD_MockFactor> aSveEngine = new OLD_ApproxSveInferenceEngine<OLD_MockFactor>(model2, new MockFactory(), 1);

        OLD_MockFactor result = aSveEngine.infer(new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(vF)));

        Assert.assertEquals(
                ("[(APPROX(\"" +
                        "[([DEF_INT_{D}<[DEF_INT_{E}<" +
                        "   [(pr([F]|DE)).(APPROX(\"" +
                        "       [([DEF_INT_{C}<[DEF_INT_{B}<[DEF_INT_{A}<" +
                        "           [(pr(E|ABCD)).(APPROX(\"[(pr(D))]\")).(APPROX(\"" +
                        "               [(pr(C|AB)).(APPROX(\"[(pr(B))]\")).(APPROX(\"[(pr(A))]\"))]" +
                        "           \"))]" +
                        "       >]>]>])]" +
                        "   \"))]" +
                        ">]>])]" +
                        "\"))]").replaceAll("\\s+", ""), result.getText().replaceAll("\\s+", ""));
        Assert.assertEquals(1, result.getScopeVars().size());
        Assert.assertEquals(vF, result.getScopeVars().iterator().next());
    }

    @Test
    public void testApproximationOnMock2() {
        OLD_MockFactor fA = new OLD_MockFactor(vA, "pr([A])");
        OLD_MockFactor fB = new OLD_MockFactor(vB, "pr([B])");
        OLD_MockFactor fC = new OLD_MockFactor(vC, "pr([C]|[A][B])");
        OLD_MockFactor fD = new OLD_MockFactor(vD, "pr([D])");
        OLD_MockFactor fE = new OLD_MockFactor(vE, "pr([E]|[A][B][C][D])");
        OLD_MockFactor fF = new OLD_MockFactor(vF, "pr([F]|[C][D][E])");

        SimpleBayesianGraphicalModel<OLD_MockFactor> model2 = new SimpleBayesianGraphicalModel<OLD_MockFactor>(
                new FactorParentsTuple<OLD_MockFactor>(fA, new OLD_MockFactor[]{}),
                new FactorParentsTuple<OLD_MockFactor>(fB, new OLD_MockFactor[]{}),
                new FactorParentsTuple<OLD_MockFactor>(fC, new OLD_MockFactor[]{fA, fB}),
                new FactorParentsTuple<OLD_MockFactor>(fD, new OLD_MockFactor[]{}),
                new FactorParentsTuple<OLD_MockFactor>(fE, new OLD_MockFactor[]{fA, fB, fC, fD}),
                new FactorParentsTuple<OLD_MockFactor>(fF, new OLD_MockFactor[]{fC, fD, fE}));
        InferenceEngine<OLD_MockFactor> aSveEngine = new OLD_ApproxSveInferenceEngine<OLD_MockFactor>(model2, new MockFactory(), 2);

        OLD_MockFactor result = aSveEngine.infer(new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(vF)));
        Assert.assertEquals(
                "[([DEF_INT_{D}<[DEF_INT_{E}<[DEF_INT_{C}<" +
                        "[(pr([F]|CDE)).(APPROX(\"" +
                        "[([DEF_INT_{B}<[DEF_INT_{A}<[(pr(D)).(pr(E|ABCD)).(APPROX(\"[(pr(A)).(pr(B)).(pr(C|AB))]\"))]>]>])]" +
                        "\"))]" +
                        ">]>]>])]", result.getText());

    }

    @Test
    public void testInferOnXaddWithQuery1() throws Exception {
        double approximationVolumeThreshold = 3000.5d;
        double approximationMassThreshold = 1000.5d;
        int numberOfFactorsLeadingToJointFactorApproximation = 1;

        /*
        [TRACKING.GM]
        -------------------------------

        indices (
                (i = 1 -- 2)
        )

        cevidence (
                (x_1 = 5)
        (x_2 = 6)
        )

        bevidence (
        )
        query (d)
                */

        Variable d = new Variable("d");
        Variable x1 = new Variable("x1");
        Variable x2 = new Variable("x2");

        OLD_XADDFactory factory = new OLD_XADDFactory(approximationMassThreshold, approximationVolumeThreshold);
        factory.putContinuousVariable(d, -10, 20);
        factory.putContinuousVariable(x1, -10, 20);
        factory.putContinuousVariable(x2, -10, 20);
        OLD_XADDFactor df = factory.putNewFactorWithContinuousAssociatedVar(d, "U(d,0,0,10)");
        OLD_XADDFactor x1f = factory.putNewFactorWithContinuousAssociatedVar(x1, "0.05*U(x1,0,0,10) + 0.85*N(x1,d,2,2.5) + 0.1*T(x1,10,1,0)");
        OLD_XADDFactor x2f = factory.putNewFactorWithContinuousAssociatedVar(x2, "0.05*U(x2,0,0,10) + 0.85*N(x2,d,2,2.5) + 0.1*T(x2,10,1,0)");
        System.out.println("df = " + df);
        System.out.println("x1f = " + x1f);
        System.out.println("x2f = " + x2f);
        System.out.println("df.getScopeVars() = " + df.getScopeVars());
        System.out.println("x1f.getScopeVars() = " + x1f.getScopeVars());
        System.out.println("x2f.getScopeVars() = " + x2f.getScopeVars());

        SimpleBayesianGraphicalModel<OLD_XADDFactor> bModel = new SimpleBayesianGraphicalModel<OLD_XADDFactor>(
                new FactorParentsTuple<OLD_XADDFactor>(df, new OLD_XADDFactor[]{}),
                new FactorParentsTuple<OLD_XADDFactor>(x1f, new OLD_XADDFactor[]{df}),
                new FactorParentsTuple<OLD_XADDFactor>(x2f, new OLD_XADDFactor[]{df})
        );


        QueryImpl q = new QueryImpl(
                new HashSet<InstantiatedVariable>(
                        //Arrays.asList(
//                        new InstantiatedVariable(x1, "5"),     //todo WHAT IF WE HAVE INSTANTIATION????
//                        new InstantiatedVariable(x2, "6")
                ),
                Arrays.asList(d));


        //----------
        OLD_ApproxSveInferenceEngine<OLD_XADDFactor> approxEngine = new OLD_ApproxSveInferenceEngine<OLD_XADDFactor>(
                bModel, factory, numberOfFactorsLeadingToJointFactorApproximation);

        OLD_XADDFactor resultF = approxEngine.infer(q/*, Arrays.asList(d, x2, x1)*/);
        System.out.println("resultF = " + resultF);
        System.out.println("resultF.getXADDNodeString() = " + resultF.getXADDNodeString());

//        Assert.assertEquals(resultF.getScopeVars().size(), 1);
        if (resultF.getScopeVars().size() == 1) {
            factory.visualize1DFactor(resultF, ("approx non-normalized result"));
        }

/*
        XADDFactor normalResultF = resultF.normalize();
        System.out.println("normalResultF = " + normalResultF);
        System.out.println("normalResultF.getXADDNodeString() = " + normalResultF.getXADDNodeString());
        if (normalResultF.getScopeVars().size() == 1) {
            factory.visualize1DFactor(normalResultF, "approx normalized result");
        }
*/

       //-------------
        //Comparing with simple SVE:
        OLD_ExactSveInferenceEngine<OLD_XADDFactor> exactEngine = new OLD_ExactSveInferenceEngine<OLD_XADDFactor>(factory,
                Arrays.asList(df, x1f, x2f));
        OLD_XADDFactor exactResultF = exactEngine.infer(q, Arrays.asList(d, x1, x2));
        if (resultF.getScopeVars().size() == 1) {
            factory.visualize1DFactor(exactResultF, "non normalized exact result");
        }



    }


}
