package hgm.asve.engine;

import hgm.InstantiatedVariable;
import hgm.QueryImpl;
import hgm.Variable;
import hgm.asve.factor.OLD_MockFactor;
import hgm.asve.factor.OLD_XADDFactor;
import hgm.asve.factory.MockFactory;
import hgm.asve.factory.OLD_XADDFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by Hadi Afshar.
 * Date: 21/09/13
 * Time: 5:39 PM
 */
@Deprecated
public class OLD_ExactSveInferenceEngineTest {
    @Test
    public void testInferOnMocks() throws Exception {
        OLD_FactorSetBasedInferenceEngine<OLD_MockFactor> sveEngine = new OLD_ExactSveInferenceEngine<OLD_MockFactor>(new MockFactory(),
                Arrays.asList(
                        new OLD_MockFactor("p([Y])"),
                        new OLD_MockFactor("p([X])"),
                        new OLD_MockFactor("p([X],[Z])"),
                        new OLD_MockFactor("D([Z])")
                ));

        OLD_MockFactor result = sveEngine.infer(
                new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(OLD_MockFactor.Y, OLD_MockFactor.X)),
                Arrays.asList(OLD_MockFactor.X, OLD_MockFactor.Y, OLD_MockFactor.Z));
        Assert.assertEquals("[(p([X])).(p([Y])).([DEF_INT_{[Z]}<[(D([Z])).(p([X],[Z]))]>])]", result.getText());

        try {
            result = sveEngine.infer(
                    new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(OLD_MockFactor.Y, OLD_MockFactor.X)),
                    Arrays.asList(OLD_MockFactor.X, OLD_MockFactor.Y));
            Assert.fail("ordered list of variables should contain all variables in the relevant scope.");
        } catch (Exception e) {
        }


        //check take factors:
        sveEngine.takeFactors(Arrays.asList(
                new OLD_MockFactor("pr([X])"),
                new OLD_MockFactor("pr([Y]|[X])"),
                new OLD_MockFactor("pr([Z]|[Y])"),
                new OLD_MockFactor("pr([W]|[Z])")));

        try {
            result = sveEngine.infer(
                    new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(OLD_MockFactor.W)),
                    Arrays.asList(OLD_MockFactor.X, OLD_MockFactor.Y, OLD_MockFactor.Z));

            Assert.fail();
        } catch (Exception e) {
        }

        result = sveEngine.infer(
                new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(OLD_MockFactor.W)),
                Arrays.asList(OLD_MockFactor.X, OLD_MockFactor.Y, OLD_MockFactor.Z, OLD_MockFactor.W));

        Assert.assertEquals("[DEF_INT_{[Z]}<[(pr([W]|[Z])).([DEF_INT_{[Y]}<[(pr([Z]|[Y]))." +
                "([DEF_INT_{[X]}<[(pr([X])).(pr([Y]|[X]))]>])]>])]>]", result.getText());

        result = sveEngine.infer(
                new QueryImpl(new HashSet<InstantiatedVariable>(), Arrays.asList(OLD_MockFactor.W)),
                Arrays.asList(OLD_MockFactor.W, OLD_MockFactor.Z, OLD_MockFactor.Y, OLD_MockFactor.X));

        Assert.assertEquals("[DEF_INT_{[X]}<[(pr([X])).([DEF_INT_{[Y]}<[(pr([Y]|[X]))." +
                "([DEF_INT_{[Z]}<[(pr([W]|[Z])).(pr([Z]|[Y]))]>])]>])]>]", result.getText());

    }

    //The rest is with XADDFactors.
    //todo in order to use visualizer I should use main rather than routine test since it is multi-thread. Find a way!
    public static void main(String[] args) throws Exception {
        OLD_ExactSveInferenceEngineTest instance = new OLD_ExactSveInferenceEngineTest();
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

        OLD_XADDFactory factory = new OLD_XADDFactory(1, 1);
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


        QueryImpl q = new QueryImpl(
                new HashSet<InstantiatedVariable>(
                //Arrays.asList(
//                        new InstantiatedVariable(x1, "5"),     //todo WHAT IF WE HAVE INSTANTIATION????
//                        new InstantiatedVariable(x2, "6")
                ),
                Arrays.asList(x1));


        OLD_ExactSveInferenceEngine<OLD_XADDFactor> l = new OLD_ExactSveInferenceEngine<OLD_XADDFactor>(factory, Arrays.asList(df, x1f, x2f));

        OLD_XADDFactor resultF = l.infer(q, Arrays.asList(d, x2, x1));
        System.out.println("resultF = " + resultF);
        System.out.println("resultF.getXADDNodeString() = " + resultF.getXADDNodeString());

        Assert.assertEquals(resultF.getScopeVars().size(), 1);
        factory.visualize1DFactor(resultF, "non normalized result");

        OLD_XADDFactor normalResultF = resultF.normalize();
        System.out.println("normalResultF = " + normalResultF);
        System.out.println("normalResultF.getXADDNodeString() = " + normalResultF.getXADDNodeString());

        if (normalResultF.getScopeVars().size() == 1) {
            factory.visualize1DFactor(normalResultF, "normalized result");
//            SVE.ExportData(norm_result, q._sFilename + ".txt");
        }
    }
}
