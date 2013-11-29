package hgm;

import xadd.XADD;
import xadd.XADDUtils;

import java.util.Iterator;

/**
 * Created by Hadi Afshar.
 * Date: 15/11/13
 * Time: 12:10 PM
 */
//todo merge with factor visualizer
public class XaddVisualizer {
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
