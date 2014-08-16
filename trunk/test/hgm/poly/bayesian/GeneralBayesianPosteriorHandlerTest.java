package hgm.poly.bayesian;

import hgm.poly.PiecewiseExpression;
import org.junit.Test;
import org.testng.Assert;

import java.util.Arrays;

/**
 * Created by Hadi Afshar.
 * Date: 9/05/14
 * Time: 6:41 AM
 */
public class GeneralBayesianPosteriorHandlerTest {
    @Test
    public void testMakeActivateSubFunction() throws Exception {
        PriorHandler priorH = PriorHandler.uniformInHypercube("x", 2, 10);
        PiecewiseExpression prior = priorH.getPrior();
        PiecewiseExpression l1 = new PiecewiseExpression(
                prior.getFactory().makeConstrainedPolynomial("2", "x_0^(1)>0"),
                prior.getFactory().makeConstrainedPolynomial("3", "x_0^(1)<0")
        );


        GeneralBayesianPosteriorHandler ph = new GeneralBayesianPosteriorHandler(priorH);

        ph.addLikelihood(l1);
        PiecewiseExpression pp0 = ph.makeActivatedSubFunction(Arrays.asList(0));
        System.out.println("pp0 = " + pp0);
        String pp0Str = pp0.toString();
        PiecewiseExpression pp1 = ph.makeActivatedSubFunction(Arrays.asList(1));
        System.out.println("pp1 = " + pp1);
        PiecewiseExpression pp00 = ph.makeActivatedSubFunction(Arrays.asList(0));
        System.out.println("pp00 = " + pp00);
        Assert.assertEquals(pp0Str, pp00.toString());


    }
}
