package hgm.reports;

import hgm.preference.StringBasedXaddGenerator;
import hgm.utils.vis.XaddVisualizer;
import org.junit.Test;
import xadd.XADD;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 4/02/14
 * Time: 7:12 AM
 */
public class ReportSamplingComparisonOnQuadraticDistribution extends StringBasedXaddGenerator{
    public ReportSamplingComparisonOnQuadraticDistribution(XADD context) {
        super(context);
    }

    public static void main(String[] args) {
        ReportSamplingComparisonOnQuadraticDistribution instance = new ReportSamplingComparisonOnQuadraticDistribution(new XADD());
        instance.test1();
    }


    private void test1() {
        XaddVisualizer.visualize(makeU2Function("w", 2, 50, 0d, 10), "", context);

    }

    //U2 is a U-shape func. in all pos or all neg regions
    private XADD.XADDNode makeU2Function(String varPrefix, int varNum, double surroundingCubeParam, double centerShift, double cadre) {
        //make vars
        List<String> vars = new ArrayList<String>();
        for (int i = 0; i < varNum; i++) {
            vars.add(varPrefix + "_" + i);
        }

        //make Cube:
        XADD.XADDNode cube = context.getExistNode(context.ONE);
        for (String var : vars) {
            cube = super.multiply(cube, super.uniformDistribution(var, -surroundingCubeParam, surroundingCubeParam));
        }

        //fix var limits:
        StringBasedXaddGenerator.fixVarLimits(context, cube, -surroundingCubeParam - cadre, surroundingCubeParam + cadre);


        //all positive region:
        XADD.XADDNode pos = context.getExistNode(context.ONE);
        for (String var : vars) {
            pos = multiply(pos,
                    indicator(var + ">" + super.doubleToString(centerShift), 0d /*no noise*/));
        }
        XaddVisualizer.visualize(cube, "", context);

        //all negative region:
        XADD.XADDNode neg = context.getExistNode(context.ONE);
        for (String var : vars) {
            neg = super.multiply(neg,
                    super.indicator(var + "<" + super.doubleToString(centerShift), 0d /*no noise*/));
        }

        XADD.XADDNode mask = multiply(sum(neg, pos), cube);

        //main func:
        StringBuilder sb = new StringBuilder("([");
        for (String var : vars) {
            sb.append(var + "*" + var + "+");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("])");
        System.out.println("sb = " + sb);
        XADD.XADDNode func = context.getExistNode(context.buildCanonicalXADDFromString(sb.toString()));

        return super.multiply(mask, func);

    }

}
