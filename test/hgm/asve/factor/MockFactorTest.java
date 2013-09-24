package hgm.asve.factor;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Hadi Afshar.
 * Date: 21/09/13
 * Time: 5:54 PM
 */
public class MockFactorTest {
    @Test
    public void testGetScopeVars() throws Exception {
        MockFactor f = new MockFactor("([X]^3 + [Y]*[X] + [Z]*[Z])* [Z]");
        System.out.println("f.getScopeVars() = " + f.getScopeVars());
        Assert.assertEquals(3, f.getScopeVars().size());
        Assert.assertTrue(f.getScopeVars().contains(MockFactor.X));
        Assert.assertTrue(f.getScopeVars().contains(MockFactor.Y));
        Assert.assertTrue(f.getScopeVars().contains(MockFactor.Z));
    }
}
