package hgm.DebugIntegral;

import hgm.asve.cnsrv.factory.ModelBasedXaddFactorFactory;
import hgm.asve.cnsrv.gm.FBQuery;
import sve.GraphicalModel;
import sve.Query;
import xadd.XADD;
import xadd.XADDUtils;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Hadi Afshar.
 * Date: 19/10/13
 * Time: 3:15 AM
 */
public class IntegralTest {

    public static void main(String[] args) {
        IntegralTest instant = new IntegralTest();
        instant.testIntegral();
    }

    //TODO: I do not understand!!!! the result does not match ApproxSveInferenceEngineTest!!!!
    //even on 1 as final leaf, different context produce VERY different diagrams although same result....


    public void testIntegral() {
        XADD context = null;
        String gmFile = "./src/sve/radar.gm";
        String qFile = "./src/sve/radar.query.4";

        int makeContextType = 2;
        System.out.println("makeContextType = " + makeContextType);
        switch (makeContextType) {
            case 0:  //factory
                FBQuery q = new FBQuery(qFile);
                ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(gmFile, q, null /*approximator*/);
                context = factory.getContext();

                break;
            case 1:  // gm
                GraphicalModel gm = new GraphicalModel(gmFile);
                Query q1 = new Query(qFile);
                gm.instantiateGMTemplate(q1._hmVar2Expansion);
                context = gm._context;
                break;
            case 2: //from scratch
                context = new XADD();
                String[] vars = new String[]{"o_1", "x_2"};
                for (String var : vars) {
                    context._hmMinVal.put(var, -10d);
                    context._hmMaxVal.put(var, 20d);
                }
                break;
            default:
                System.err.println("unknown type");
        }


        /////////////////////////
        String xaddStr = "( [(-1 + (0.07692308 * x_2)) > 0]\n" +
                "    ( [0] ) \n" +
                "    ( [(-1 + (0.33333333 * x_2)) > 0]\n" +
                "       ( [(1 + (0.33333333 * x_2)) > 0]\n" +
                "          ( [(1 + (-0.14285714 * x_2)) > 0]\n" +
                "             ( [0] ) \n" +
                "             ( [(1 + (0.2 * o_1)) > 0]\n" +
                "                ( [(1 + (-0.125 * x_2) + (0.125 * o_1)) > 0]\n" +
                "                   ( [(-1 + (0.1 * o_1)) > 0]\n" +
                "                      ( [(1 * o_1) > 0]\n" +
                "                         ( [(1 + (-0.2 * o_1)) > 0]\n" +
                "                            ( [0] ) \n" +
                "                            ( [(1 + (-0.33333333 * x_2) + (0.33333333 * o_1)) > 0]\n" +
                "                               ( [(-1 + (0.09090909 * o_1)) > 0]\n" +
                "                                  ( [0] ) \n" +
                "                                  ( [(-1 + (-0.25 * x_2) + (0.25 * o_1)) > 0]\n" +
                "                                     ( [0] ) \n" +
                "                                     ( [(1 + (-0.5 * x_2) + (0.5 * o_1)) > 0]\n" +
                "                                        ( [(4.49060014 + (-3.00499078 * x_2) + (0.73995114 * x_2 * x_2) + (-0.07937605 * x_2 * x_2 * x_2) + (-0.00000005 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1 * x_2) + (0.02468634 * o_1 * x_2 * x_2 * x_2) + (-0.00000048 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1) + (0.00000713 * o_1 * o_1 * o_1 * o_1 * x_2 * x_2 * x_2) + (0.00029996 * o_1 * o_1 * o_1) + (0.69608785 * o_1 * x_2) + (-0.19732854 * o_1 * x_2 * x_2) + (-0.00000243 * o_1 * o_1 * o_1 * o_1 * o_1 * x_2 * x_2) + (0.00313326 * x_2 * x_2 * x_2 * x_2) + (-0.00113937 * o_1 * x_2 * x_2 * x_2 * x_2) + (-0.00000942 * o_1 * o_1 * o_1 * x_2 * x_2 * x_2 * x_2) + (0.00001584 * o_1 * o_1 * o_1 * o_1 * o_1 * x_2) + (0.03145503 * o_1 * o_1) + (0.00047439 * o_1 * o_1 * o_1 * o_1) + (-0.00000029 * o_1 * o_1 * o_1 * o_1 * o_1 * x_2 * x_2 * x_2) + (0.00000033 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1 * x_2) + (0.00044675 * o_1 * o_1 * o_1 * x_2 * x_2) + (0.00000017 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1 * x_2 * x_2) + (0.00000021 * o_1 * o_1 * o_1 * o_1 * x_2 * x_2 * x_2 * x_2) + (-0.00004416 * o_1 * o_1 * o_1 * o_1 * x_2 * x_2) + (-0.00194949 * o_1 * o_1 * o_1 * x_2) + (-0.00241684 * o_1 * o_1 * x_2 * x_2 * x_2) + (-0.92018946 * o_1) + (-0.03060514 * o_1 * o_1 * x_2) + (-0.00003876 * o_1 * o_1 * o_1 * o_1 * x_2) + (0.00003139 * o_1 * o_1 * o_1 * x_2 * x_2 * x_2) + (0.01304359 * o_1 * o_1 * x_2 * x_2) + (-0.00000005 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1) + (0.00015537 * o_1 * o_1 * x_2 * x_2 * x_2 * x_2) + (0.00000001 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1) + (-0.0000312 * o_1 * o_1 * o_1 * o_1 * o_1))] ) \n" +
                "                                        ( [0] ) )  )  )  \n" +
                "                               ( [0] ) )  )  \n" +
                "                         ( [0] ) )  \n" +
                "                      ( [0] ) )  \n" +
                "                   ( [0] ) )  \n" +
                "                ( [0] ) )  )  \n" +
                "          ( [0] ) )  \n" +
                "       ( [0] ) )  ) ";


        xaddStr = "( [(-1 + (0.07692308 * x_2)) > 0]\n" +
                "    ( [0] ) \n" +
                "    ( [(-1 + (0.33333333 * x_2)) > 0]\n" +
                "       ( [(1 + (0.33333333 * x_2)) > 0]\n" +
                "          ( [(1 + (-0.14285714 * x_2)) > 0]\n" +
                "             ( [0] ) \n" +
                "             ( [(1 + (0.2 * o_1)) > 0]\n" +
                "                ( [(1 + (-0.125 * x_2) + (0.125 * o_1)) > 0]\n" +
                "                   ( [(-1 + (0.1 * o_1)) > 0]\n" +
                "                      ( [(1 * o_1) > 0]\n" +
                "                         ( [(1 + (-0.2 * o_1)) > 0]\n" +
                "                            ( [0] ) \n" +
                "                            ( [(1 + (-0.33333333 * x_2) + (0.33333333 * o_1)) > 0]\n" +
                "                               ( [(-1 + (0.09090909 * o_1)) > 0]\n" +
                "                                  ( [0] ) \n" +
                "                                  ( [(-1 + (-0.25 * x_2) + (0.25 * o_1)) > 0]\n" +
                "                                     ( [0] ) \n" +
                "                                     ( [(1 + (-0.5 * x_2) + (0.5 * o_1)) > 0]\n" +
                "                                        ( [(4.49060014 + (-3.00499078 * x_2) + (0.73995114 * x_2 * x_2) + (-0.07937605 * x_2 * x_2 * x_2) + (-0.00000005 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1 * x_2) + (0.02468634 * o_1 * x_2 * x_2 * x_2) + (-0.00000048 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1) + (0.00000713 * o_1 * o_1 * o_1 * o_1 * x_2 * x_2 * x_2) + (0.00029996 * o_1 * o_1 * o_1) + (0.69608785 * o_1 * x_2) + (-0.19732854 * o_1 * x_2 * x_2) + (-0.00000243 * o_1 * o_1 * o_1 * o_1 * o_1 * x_2 * x_2) + (0.00313326 * x_2 * x_2 * x_2 * x_2) + (-0.00113937 * o_1 * x_2 * x_2 * x_2 * x_2) + (-0.00000942 * o_1 * o_1 * o_1 * x_2 * x_2 * x_2 * x_2) + (0.00001584 * o_1 * o_1 * o_1 * o_1 * o_1 * x_2) + (0.03145503 * o_1 * o_1) + (0.00047439 * o_1 * o_1 * o_1 * o_1) + (-0.00000029 * o_1 * o_1 * o_1 * o_1 * o_1 * x_2 * x_2 * x_2) + (0.00000033 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1 * x_2) + (0.00044675 * o_1 * o_1 * o_1 * x_2 * x_2) + (0.00000017 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1 * x_2 * x_2) + (0.00000021 * o_1 * o_1 * o_1 * o_1 * x_2 * x_2 * x_2 * x_2) + (-0.00004416 * o_1 * o_1 * o_1 * o_1 * x_2 * x_2) + (-0.00194949 * o_1 * o_1 * o_1 * x_2) + (-0.00241684 * o_1 * o_1 * x_2 * x_2 * x_2) + (-0.92018946 * o_1) + (-0.03060514 * o_1 * o_1 * x_2) + (-0.00003876 * o_1 * o_1 * o_1 * o_1 * x_2) + (0.00003139 * o_1 * o_1 * o_1 * x_2 * x_2 * x_2) + (0.01304359 * o_1 * o_1 * x_2 * x_2) + (-0.00000005 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1) + (0.00015537 * o_1 * o_1 * x_2 * x_2 * x_2 * x_2) + (0.00000001 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1 * o_1) + (-0.0000312 * o_1 * o_1 * o_1 * o_1 * o_1))] ) \n" +
                "                                        ( [0] ) )  )  )  \n" +
                "                               ( [0] ) )  )  \n" +
                "                         ( [0] ) )  \n" +
                "                      ( [0] ) )  \n" +
                "                   ( [0] ) )  \n" +
                "                ( [0] ) )  )  \n" +
                "          ( [0] ) )  \n" +
                "       ( [0] ) )  ) ";
        ;

        int rootId = context.buildCanonicalXADDFromString(xaddStr);
        XADD.XADDNode root = context.getExistNode(rootId);

        visualize(root, "root", context);

        int marginalizedO1Id = context.computeDefiniteIntegral(rootId, "o_1");
        XADD.XADDNode marginO1 = context.getExistNode(marginalizedO1Id);

        visualize(marginO1, "marginO1", context);

        int marginalizedO1X2Id = context.computeDefiniteIntegral(marginalizedO1Id, "x_2");
        XADD.XADDTNode marginO1X2 = (XADD.XADDTNode) context.getExistNode(marginalizedO1X2Id);
        Double evaluate = marginO1X2._expr.evaluate(new HashMap<String, Double>());
        System.out.println("evaluate = " + evaluate);


//        XADDUtils.PlotXADD(context, marginalizedO1Id, -2, 0.01, 15, "x", "first var marginalized");

        context.getGraph(rootId).launchViewer("root");

        context.getGraph(marginalizedO1Id).launchViewer("marginalizedO");

    }

    //todo move this visualizer and the rest to the the XADDUtils.
    public static void visualize(XADD.XADDNode node, String title, XADD context) {
        int numVars = node.collectVars().size();
        if (numVars == 1) visualize1DimXadd(node, title, context);
        else if (numVars == 2) visualize2DimXadd(node, title, context);
        else System.err.println("a node with numVars = " + numVars + " cannot be visualized");
    }

    public static void visualize2DimXadd(XADD.XADDNode node, String title, XADD context) {

        Iterator<String> iterator = node.collectVars().iterator();
        String varX = iterator.next();
        String varY = iterator.next();
        double min_val_x = context._hmMinVal.get(varX);
        double max_val_x = context._hmMaxVal.get(varX);
        double min_val_y = context._hmMinVal.get(varY);
        double max_val_y = context._hmMaxVal.get(varY);
        XADDUtils.Plot3DSurfXADD(context, context._hmNode2Int.get(node),
                min_val_x, 0.5d, max_val_x,
                min_val_y, 0.5d, max_val_y,
                varX, varY, title);
    }

    public static void visualize1DimXadd(XADD.XADDNode node, String title, XADD context) {
        if (node.collectVars().size() != 1) throw new RuntimeException("only one variable expected!");

        String var = node.collectVars().iterator().next();

        double min_val = context._hmMinVal.get(var);
        double max_val = context._hmMaxVal.get(var);

        XADDUtils.PlotXADD(context, context._hmNode2Int.get(node), min_val, 0.1d, max_val, var, title);
    }
}
