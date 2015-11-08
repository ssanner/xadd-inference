package hgm.poly.reports.sg.journal;

import hgm.poly.Function;
import hgm.poly.vis.FunctionVisualizer;
import hgm.sampling.VarAssignment;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.SortedSet;

/**
 * Created by Hadi M Afshar.
 * Date: 11/6/15
 * Time: 1:14 AM
 */
public class ConditionalRandomFieldModelTest {
    public static void main(String[] args) {
        ConditionalRandomFieldModelTest inst = new ConditionalRandomFieldModelTest();
        inst.test2D();
    }


    public void test2D(){
        ConditionalRandomFieldModel crf = new ConditionalRandomFieldModel(2, 1);
        final MultiFactorJoint j = crf.makeJoint(Arrays.asList("x_1", "x_2"), new HashMap<String, Double>());
        FunctionVisualizer.visualize(new Function() {
            @Override
            public double evaluate(VarAssignment fullVarAssign) {
                Double[] assign = new Double[] {fullVarAssign.getContinuousVar("x_1"), fullVarAssign.getContinuousVar("x_2")};
                 return j.evaluate(assign);
            }

            @Override
            public String[] collectContinuousVars() {
                /*SortedSet<String> scopeVars = j.getScopeVars();
                String[] out = new String[scopeVars.size()];
                int i=0;
                for (String var : scopeVars) {
                   out[i++]=var;
                }
                return out;*/
                return new String[]{"x_1", "x_2"};
            }
        }, -5, 15, 0.1, "");
    }
}
