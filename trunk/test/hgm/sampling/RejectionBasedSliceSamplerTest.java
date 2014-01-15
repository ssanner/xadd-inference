package hgm.sampling;


import hgm.utils.vis.XaddVisualizer;
import org.junit.Test;
import xadd.XADD;

import java.util.HashSet;

/**
 * Created by Hadi Afshar.
 * Date: 18/12/13
 * Time: 6:50 PM
 */
public class RejectionBasedSliceSamplerTest {
    public static void main(String[] args) {
        RejectionBasedSliceSamplerTest instance = new RejectionBasedSliceSamplerTest();
        instance.basicTest();
    }

    @Test
    public void basicTest() {
        XADD context = new XADD();
        int rootId = context.buildCanonicalXADDFromString("([N(n, 10, 666,3)])");//("([1.0 * x])");
        XADD.XADDNode root = context._hmInt2Node.get(rootId);//("([N(n, 5, 666,3)])"));
        HashSet<String> vars = root.collectVars();
        for (String var : vars) {
            context._hmMinVal.put(var, -10d);
            context._hmMaxVal.put(var, 20d);
        }

//        context.getGraph(rootId).launchViewer("test");
        XaddVisualizer.visualize(root, "test", context);

        RejectionBasedSliceSampler sampler = new RejectionBasedSliceSampler(context, root);
        for (int i = 0; i < 10; i++) {
            VarAssignment assign= sampler.sample();
            System.out.println("t = " + assign);
        }
    }

    @Test
    public void basicTest2D() {
        XADD context = new XADD();
        int id1 = context.buildCanonicalXADDFromString("([N(x, 10, 666,3)])");//("([1.0 * x])");
        int id2 = context.buildCanonicalXADDFromString("([N(y, 10, 666,3)])");//("([1.0 * x])");
        int rootId = context.apply(id1, id2, XADD.PROD);
        XADD.XADDNode root = context._hmInt2Node.get(rootId);//("([N(n, 5, 666,3)])"));
        HashSet<String> vars = root.collectVars();
        for (String var : vars) {
            context._hmMinVal.put(var, -10d);
            context._hmMaxVal.put(var, 20d);
        }

        context.getGraph(rootId).launchViewer("test");
        XaddVisualizer.visualize(root, "test", context);

        Sampler sampler = new RejectionBasedSliceSampler(context, root);
        for (int i = 0; i < 10; i++) {
            VarAssignment assign= sampler.sample();
            System.out.println("t = " + assign);
        }
    }
}
