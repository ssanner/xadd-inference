package hgm.asve.factor;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Hadi Afshar.
 * Date: 21/09/13
 * Time: 5:54 PM
 */
@Deprecated
public class OLD_MockFactorTest {
    @Test
    public void testGetScopeVars() throws Exception {
        OLD_MockFactor f = new OLD_MockFactor("([X]^3 + [Y]*[X] + [Z]*[Z])* [Z]");
        System.out.println("f.getScopeVars() = " + f.getScopeVars());
        Assert.assertEquals(3, f.getScopeVars().size());
        Assert.assertTrue(f.getScopeVars().contains(OLD_MockFactor.X));
        Assert.assertTrue(f.getScopeVars().contains(OLD_MockFactor.Y));
        Assert.assertTrue(f.getScopeVars().contains(OLD_MockFactor.Z));
    }
}
