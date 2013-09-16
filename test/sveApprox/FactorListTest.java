package sveApprox;

import junit.framework.Assert;
import org.junit.Test;
import sve.SVE;
import xadd.XADDUtils;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by Hadi M Afshar
 * Date: 13/09/13
 * Time: 2:43 PM
 */
public class FactorListTest {
    public static void main(String[] args) throws Exception {
        FactorListTest instance = new FactorListTest();
        instance.testInferOnQUERY1();
    }

    @Test
    public void testInferOnQUERY1() throws Exception {
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
        XADDFactory factory = new XADDFactory();
        factory.putContinuousVariable("d", -10, 20);
        factory.putContinuousVariable("x1", -10, 20);
        factory.putContinuousVariable("x2", -10, 20);
        HFactor df = factory.putNewFactorWithContinuousVars("U(d,0,0,10)");
        HFactor x1f = factory.putNewFactorWithContinuousVars("0.05*U(x1,0,0,10) + 0.85*N(x1,d,2,2.5) + 0.1*T(x1,10,1,0)");
        HFactor x2f = factory.putNewFactorWithContinuousVars("0.05*U(x2,0,0,10) + 0.85*N(x2,d,2,2.5) + 0.1*T(x2,10,1,0)");
        System.out.println("df = " + df);
        System.out.println("x1f = " + x1f);
        System.out.println("x2f = " + x2f);
        System.out.println("df.getScopeVars() = " + df.getScopeVars());
        System.out.println("x1f.getScopeVars() = " + x1f.getScopeVars());
        System.out.println("x2f.getScopeVars() = " + x2f.getScopeVars());

        //********
        HQuery q = new HQuery(
                new HashSet<VariableValue>(Arrays.asList(
                        new VariableValue("x1", "5"),     //todo comment this line
                        new VariableValue("x2", "6")
                )),
                Arrays.asList("d"));


        FactorList l = new FactorList(df, x1f, x2f);

        HFactor resultF = l.infer(q, Arrays.asList("d", "x2", "x1"));
        System.out.println("resultF = " + resultF);
        System.out.println("resultF.getXADDNodeString() = " + resultF.getXADDNodeString());

        Assert.assertEquals(resultF.getScopeVars().size(), 1);
        factory.visualize1DFactor(resultF, "non normalized result");

        HFactor normalResultF = resultF.normalize();
        System.out.println("normalResultF = " + normalResultF);
        System.out.println("normalResultF.getXADDNodeString() = " + normalResultF.getXADDNodeString());

        if (normalResultF.getScopeVars().size() == 1) {
            factory.visualize1DFactor(normalResultF, "normalized result");
//            SVE.ExportData(norm_result, q._sFilename + ".txt");
        }
    }



}
