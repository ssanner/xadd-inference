package hgm.poly.reports.sg.journal;

import hgm.poly.integral.OneDimFunction;
import hgm.poly.vis.FunctionVisualizer;

/**
 * Created by Hadi M Afshar.
 * Date: 10/23/15
 * Time: 2:40 AM
 */
public class IrwinHallDistribution implements OneDimFunction {
    double normalMean;
    double normalStdDev;

    public IrwinHallDistribution(double normalMean, double normalStdDev) {
        this.normalMean = normalMean;
        this.normalStdDev = normalStdDev;
    }

    public static void main(String[] args) {
        IrwinHallDistribution irwin = new IrwinHallDistribution(-1, 0.5);
        FunctionVisualizer.visualize(irwin, -10, 10, 0.1, "");
    }


    public double irwinHall(double x) {
        //Pure Irwin-Hall roughly Normal with mean 1.5 and sigma 0.5
        if (x>=0 && x<1) return 0.5*x*x;
        if (x>=1 && x<2) return 0.5*(-2.0*x*x + 6.0*x - 3.0);
        if (x>=2 && x<=3) return 0.5*(x*x - 6.0*x + 9);
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //Irwin-Hall 3 parameters:
    private double innerSigma = 0.5; //Math.sqrt(n/12.0)
    private double innerMean = 1.5;//n/2

    /*public double pseudoStandardNormal(double z) {
//        return //standardNormal(v);
//        innerEval(v*innerSigma + innerMean);

        // (1/s) * standardNormal((x - m)/s) = normal(x, m, s)   // NOTE: normal(v, innerMean, innerSigma) - innerEval(v) is very small [OK]
        // therefore,
        // standardNormal(z) = s * normal(s*z + m, m, s)

        // deriving standard normal form Irwin-Hall
        return innerSigma * irwinHall(innerSigma * z + innerMean);
    }


    public double pseudoNormal(double v, double normalMean, double normalStdDev) {
        //NOTE: pseudoStandardNormal(v) - standardNormal(v) is small [OK]
        return (1.0 / normalStdDev) * pseudoStandardNormal((v - normalMean)/normalStdDev);
    }


    public double eval1(double v) {
        //NOTE pseudoNormal(v, normalMean, normalStdDev) - normal(v, normalMean, normalStdDev) is small [OK]
        return pseudoNormal(v, normalMean, normalStdDev);
    }*/

    //@Override
    public double eval(double v) {

        double oldToNewStdDev = innerSigma / normalStdDev;
        return oldToNewStdDev * irwinHall(oldToNewStdDev * (v - normalMean) + innerMean);
    }

    //////// For test
    private double normal(double x, double mean, double stdDev) {
        return  (1.0 / stdDev)*standardNormal((x- mean)/(stdDev));
    }
    public double standardNormal(double x) {
        return (1.0 / (double)Math.sqrt(2.0 * Math.PI)) * Math.exp(-0.5*x*x);
    }

    /////////////////////////////////////
}
