package sveApprox;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Created by Hadi M Afshar
 * Date: 9/09/13
 * Time: 9:01 PM
 */
@Deprecated
public class OldFactorTest {
    @Test
    public void f1() {
        Old_Factor f = new Old_Factor("([X]^3 + [Y]*[X] + [Z]*[Z])* [Z]");
        System.out.println("f.getScope() = " + f.getScope());
        Assert.assertEquals(f.getScope().size(), 3);
    }

    @Test
    public void marginalize() {
        FactorSet factors = new FactorSet(new Old_Factor[]{
                new Old_Factor("A([Y])"),
                new Old_Factor("B([X])"),
                new Old_Factor("C([X],[Z])"),
                new Old_Factor("D([Z])")
        });

        String[] scope = factors.getScope();
        Assert.assertEquals(scope.length,3);

        Old_Factor f1 = factors.marginalize(new String[]{"[Y]", "[X]"});
        System.out.println("f1 = " + f1.getValue());


    }


}
