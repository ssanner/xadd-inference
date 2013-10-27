package hgm.asve.cnsrv.factor;


import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Hadi Afshar.
 * Date: 15/10/13
 * Time: 7:57 AM
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
