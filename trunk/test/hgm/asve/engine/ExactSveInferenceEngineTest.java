package hgm.asve.engine;

import hgm.InstantiatedVariable;
import hgm.QueryImpl;
import hgm.Variable;
import hgm.asve.factor.MockFactor;
import hgm.asve.factor.XADDFactor;
import hgm.asve.factory.MockFactory;
import hgm.asve.factory.XADDFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by Hadi Afshar.
 * Date: 21/09/13
 * Time: 5:39 PM
 */
public class ExactSveInferenceEngineTest {
    @Test
    public void testInferOnMocks() throws Exception {
        FactorSetBasedInferenceEngine<MockFactor> sveEngine = new ExactSveInferenceEngine<MockFactor>(new MockFactory(),
                Arrays.asList(
                        new MockFactor("p([Y])"),
                        new MockFactor("p([X])"),
                        new MockFactor("p([X],[Z])"),
                        new MockFactor("D([Z])")
                ));

        MockFactor result = sveEngine.infer(
                new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(MockFactor.Y, MockFactor.X)),
                Arrays.asList(MockFactor.X, MockFactor.Y, MockFactor.Z));
        Assert.assertEquals("[(p([X])).(p([Y])).([DEF_INT_{[Z]}<[(D([Z])).(p([X],[Z]))]>])]", result.getText());

        try {
            result = sveEngine.infer(
                    new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(MockFactor.Y, MockFactor.X)),
                    Arrays.asList(MockFactor.X, MockFactor.Y));
            Assert.fail("ordered list of variables should contain all variables in the relevant scope.");
        } catch (Exception e) {
        }


        //check take factors:
        sveEngine.takeFactors(Arrays.asList(
                new MockFactor("pr([X])"),
                new MockFactor("pr([Y]|[X])"),
                new MockFactor("pr([Z]|[Y])"),
                new MockFactor("pr([W]|[Z])")));

        try {
            result = sveEngine.infer(
                    new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(MockFactor.W)),
                    Arrays.asList(MockFactor.X, MockFactor.Y, MockFactor.Z));

            Assert.fail();
        } catch (Exception e) {
        }

        result = sveEngine.infer(
                new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(MockFactor.W)),
                Arrays.asList(MockFactor.X, MockFactor.Y, MockFactor.Z, MockFactor.W));

        Assert.assertEquals("[DEF_INT_{[Z]}<[(pr([W]|[Z])).([DEF_INT_{[Y]}<[(pr([Z]|[Y]))." +
                "([DEF_INT_{[X]}<[(pr([X])).(pr([Y]|[X]))]>])]>])]>]", result.getText());

        result = sveEngine.infer(
                new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(MockFactor.W)),
                Arrays.asList(MockFactor.W, MockFactor.Z, MockFactor.Y, MockFactor.X));

        Assert.assertEquals("[DEF_INT_{[X]}<[(pr([X])).([DEF_INT_{[Y]}<[(pr([Y]|[X]))." +
                "([DEF_INT_{[Z]}<[(pr([W]|[Z])).(pr([Z]|[Y]))]>])]>])]>]", result.getText());

    }

    //The rest is with XADDFactors.
    //todo in order to use visualizer I should use main rather than routine test since it is multi-thread. Find a way!
    public static void main(String[] args) throws Exception {
        ExactSveInferenceEngineTest instance = new ExactSveInferenceEngineTest();
        instance.testInferOnXADDwithQuery1();
    }

    @Test
    public void testInferOnXADDwithQuery1() throws Exception {
        /*
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

        XADDFactory factory = new XADDFactory();
        factory.putContinuousVariable(d, -10, 20);
        factory.putContinuousVariable(x1, -10, 20);
        factory.putContinuousVariable(x2, -10, 20);
        XADDFactor df = factory.putNewFactorWithContinuousVars("U(d,0,0,10)");
        XADDFactor x1f = factory.putNewFactorWithContinuousVars("0.05*U(x1,0,0,10) + 0.85*N(x1,d,2,2.5) + 0.1*T(x1,10,1,0)");
        XADDFactor x2f = factory.putNewFactorWithContinuousVars("0.05*U(x2,0,0,10) + 0.85*N(x2,d,2,2.5) + 0.1*T(x2,10,1,0)");
        System.out.println("df = " + df);
        System.out.println("x1f = " + x1f);
        System.out.println("x2f = " + x2f);
        System.out.println("df.getScopeVars() = " + df.getScopeVars());
        System.out.println("x1f.getScopeVars() = " + x1f.getScopeVars());
        System.out.println("x2f.getScopeVars() = " + x2f.getScopeVars());


        QueryImpl q = new QueryImpl(
                new HashSet<InstantiatedVariable>(
                //Arrays.asList(
//                        new InstantiatedVariable(x1, "5"),     //todo WHAT IF WE HAVE INSTANTIATION????
//                        new InstantiatedVariable(x2, "6")
                ),
                Arrays.asList(d));


        ExactSveInferenceEngine<XADDFactor> l = new ExactSveInferenceEngine<XADDFactor>(factory, Arrays.asList(df, x1f, x2f));

        XADDFactor resultF = l.infer(q, Arrays.asList(d, x2, x1));
        System.out.println("resultF = " + resultF);
        System.out.println("resultF.getXADDNodeString() = " + resultF.getXADDNodeString());

        Assert.assertEquals(resultF.getScopeVars().size(), 1);
        factory.visualize1DFactor(resultF, "non normalized result");

        XADDFactor normalResultF = resultF.normalize();
        System.out.println("normalResultF = " + normalResultF);
        System.out.println("normalResultF.getXADDNodeString() = " + normalResultF.getXADDNodeString());

        if (normalResultF.getScopeVars().size() == 1) {
            factory.visualize1DFactor(normalResultF, "normalized result");
//            SVE.ExportData(norm_result, q._sFilename + ".txt");
        }
    }
}
