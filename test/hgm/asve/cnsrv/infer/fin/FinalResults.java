package hgm.asve.cnsrv.infer.fin;

import hgm.asve.cnsrv.approxator.Approximator;
import hgm.asve.cnsrv.approxator.EfficientPathIntegralCalculator;
import hgm.asve.cnsrv.approxator.MassThresholdXaddApproximator;
import hgm.asve.cnsrv.approxator.regression.AgglomerativeRegressionBasedXaddApproximator;
import hgm.asve.cnsrv.approxator.regression.DivisiveRegressionBasedXaddApproximator;
import hgm.asve.cnsrv.approxator.regression.RegionAndLeafHighlighter;
import hgm.asve.cnsrv.approxator.regression.measures.DivergenceMeasure;
import hgm.asve.cnsrv.approxator.regression.measures.MeanSquareErrorMeasure;
import hgm.asve.cnsrv.approxator.regression.measures.ZeroFriendlyMeanSquareErrorMeasure;
import hgm.asve.cnsrv.factor.Factor;
import hgm.asve.cnsrv.factory.ModelBasedXaddFactorFactory;
import hgm.asve.cnsrv.gm.FBQuery;
import hgm.asve.cnsrv.infer.ApproxSveInferenceEngine;
import hgm.asve.cnsrv.infer.ExactSveInferenceEngine;
import hgm.asve.cnsrv.infer.Records;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 7/11/13
 * Time: 3:23 AM
 */
public class FinalResults {
    private static final String SOURCE_PATH = "./src/sve/";
    private static final String DESTIN_PATH = "./test/hgm/asve/cnsrv/infer/fin/";

    public static void main(String[] args) throws Exception {
        FinalResults instance = new FinalResults();
        instance.produceResults();
    }


    public void produceResults() throws Exception {

        Info[] inputInfoArray = new Info[]{
                //model                                 query                       destination.path  destination.filename title           dim
//                new Info(SOURCE_PATH + "test.gm   ", SOURCE_PATH + "test.query   ", DESTIN_PATH, "test.query   ", "test   "),  //2D
//                new Info(SOURCE_PATH + "competition.gm   ", SOURCE_PATH + "competition.query.1   ", DESTIN_PATH, "competition-query-1   ", "competition-1"),  //2D
                //.new Info(SOURCE_PATH + "radar.gm   ", SOURCE_PATH + "radar.query.1   ", DESTIN_PATH, "radar-query.1   ", "radar-1   "),
//                new Info(SOURCE_PATH + "radar.gm   ", SOURCE_PATH + "radar.query.2   ", DESTIN_PATH, "radar-query-2   ", "radar-2   "),  //2D
//                new Info(SOURCE_PATH + "radar.gm   ", SOURCE_PATH + "radar.query.3   ", DESTIN_PATH, "radar-query-3   ", "radar-3   "),  //2D
//                new Info(SOURCE_PATH + "radar.gm   ", SOURCE_PATH + "radar.query.4   ", DESTIN_PATH, "radar-query.4   ", "radar-4   "),  //2D
//                new Info(SOURCE_PATH + "tracking.gm", SOURCE_PATH + "tracking.query.2", DESTIN_PATH, "tracking-query.2", "tracking-2"),
//                new Info(SOURCE_PATH + "tracking.gm", SOURCE_PATH + "tracking.query.3", DESTIN_PATH, "tracking-query.3", "tracking-3"),
                new Info(SOURCE_PATH + "tracking.gm", SOURCE_PATH + "tracking.query.a", DESTIN_PATH, "tracking-query-a", "tracking-a"),
        };



        List<Info> infoList = new ArrayList<Info>();
        for (int i = 0; i < inputInfoArray.length; i++) {
            //************* Exact SVE
            infoList.add(generateExactSveInferenceResults(inputInfoArray[i]));



/*

// ****************** MASS-THRESHOLD *******************[for radar Q2]
            double massThreshold2 = 0.040;
            infoList.add(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new MassThresholdXaddApproximator(new EfficientPathIntegralCalculator(), massThreshold2, Double.MAX_VALUE),
                    "mass-threshold-approx-" + massThreshold2 + "-", true)); //todo commented tempo

            double massThreshold3 = 0.0185;
            infoList.add(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new MassThresholdXaddApproximator(new EfficientPathIntegralCalculator(), massThreshold3, Double.MAX_VALUE),
                    "mass-threshold-approx-" + massThreshold3 + "-", true)); //todo commented tempo

            double massThreshold1 = 0.01;
            infoList.add(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new MassThresholdXaddApproximator(new EfficientPathIntegralCalculator(), massThreshold1, Double.MAX_VALUE),
                    "mass-threshold-approx-" + massThreshold1 + "-", true)); //todo commented tempo


            double massThreshold = 0.0018;
            infoList.add(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new MassThresholdXaddApproximator(new EfficientPathIntegralCalculator(), massThreshold, Double.MAX_VALUE),
                    "mass-threshold-approx-" + massThreshold + "-", true)); //todo commented tempo


// ****************** END MASS-THRESHOLD **********[for radar Q2]

*/




     /*
            infoList.add(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new RobustMassThresholdXaddApproximator(new EfficientPathIntegralCalculator(), 14),
                    1, "robust-mass-threshold-approx-", true)); //todo commented tempo

*/


//            infoList.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
//                    new SiblingXaddApproximator(
//                            new EfficientPathIntegralCalculator(), 100, 0.04),
//                    1, "sibling-approx-"))); //todo [Does not work! even for simple cases... since integration consumes all resources]






        /*    double[] params4 = {
                    1, //max power
                    100, // sample per var
                    0.000005, // max mse with sibling
                    30 //node trigger
            };
            infoList.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new DivisiveRegressionBasedXaddApproximator(
                            new MeanSquareErrorMeasure(),
                            (int) params4[0], //max power
                            (int) params4[1], //340 sample per continuous var
                            0.01, //regularization
                            params4[2], //0.00000017 max error by sibling
                            (int) params4[3]//5 //node trigger
                    ),
                    "divisive-fitting-approx-" + "Power-" + (int)params4[0] + "-Samples-" + (int)params4[1] + "-sibling-error-" + params4[2] + "-", true)));


            double[] params6 = {
                    2, //max power
                    100, // sample per var
                    0.00000025, // max mse with sibling
                    30 //node trigger
            };
            infoList.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new DivisiveRegressionBasedXaddApproximator(
                            new MeanSquareErrorMeasure(),
                            (int) params6[0], //max power
                            (int) params6[1], //340 sample per continuous var
                            0.01, //regularization
                            params6[2], //0.00000017 max error by sibling
                            (int) params6[3]//5 //node trigger
                    ),
                    "divisive-fitting-approx-" + "Power-" + params6[0] + "-Samples-" + params6[1] + "-sibling-error-" + params6[2] + "-", true)));


            double[] params8 = {
                    2, //max power
                    50, // sample per var
                    0.00005, // max mse with sibling
                    30 //node trigger
            };
            infoList.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new DivisiveRegressionBasedXaddApproximator(
                            new MeanSquareErrorMeasure(),
                            (int) params8[0], //max power
                            (int) params8[1], //340 sample per continuous var
                            0.01, //regularization
                            params8[2], //0.00000017 max error by sibling
                            (int) params8[3]//5 //node trigger
                    ),
                    "divisive-fitting-approx-" + "Power-" + params8[0] + "-Samples-" + params8[1] + "-sibling-error-" + params8[2] + "-", true)));

            double[] params5 = {
                    2, //max power
                    50, // sample per var
                    0.000005, // max mse with sibling
                    30 //node trigger
            };
            infoList.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new DivisiveRegressionBasedXaddApproximator(
                            new MeanSquareErrorMeasure(),
                            (int) params5[0], //max power
                            (int) params5[1], //340 sample per continuous var
                            0.01, //regularization
                            params5[2], //0.00000017 max error by sibling
                            (int) params5[3]//5 //node trigger
                    ),
                    "divisive-fitting-approx-" + "Power-" + params5[0] + "-Samples-" + params5[1] + "-sibling-error-" + params5[2] + "-", true)));

*/
 /*
            double[] params2 = {
                    3, //max power
                    100, // sample per var
                    0.00000010, // max mse with sibling
                    30 //node trigger
            };
            infoList.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new DivisiveRegressionBasedXaddApproximator(
                            new MeanSquareErrorMeasure(),
                            (int) params2[0], //max power
                            (int) params2[1], //340 sample per continuous var
                            0.01, //regularization
                            params2[2], //0.00000017 max error by sibling
                            (int) params2[3]//5 //node trigger
                    ),
                    "divisive-fitting-approx-" + "Power-" + params2[0] + "-Samples-" + params2[1] + "-sibling-error-" + params2[2] + "-", true)));

            double[] params5 = {
                    2, //max power
                    100, // sample per var
                    0.00000025, // max mse with sibling
                    30 //node trigger
            };
            infoList.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new DivisiveRegressionBasedXaddApproximator(
                            new MeanSquareErrorMeasure(),
                            (int) params5[0], //max power
                            (int) params5[1], //340 sample per continuous var
                            0.01, //regularization
                            params5[2], //0.00000017 max error by sibling
                            (int) params5[3]//5 //node trigger
                    ),
                    "divisive-fitting-approx-" + "Power-" + params5[0] + "-Samples-" + params5[1] + "-sibling-error-" + params5[2] + "-", true)));


            double[] params4 = {
                    1, //max power
                    100, // sample per var
                    0.00000025, // max mse with sibling
                    30 //node trigger
            };
            infoList.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new DivisiveRegressionBasedXaddApproximator(
                            new MeanSquareErrorMeasure(),
                            (int) params4[0], //max power
                            (int) params4[1], //340 sample per continuous var
                            0.01, //regularization
                            params4[2], //0.00000017 max error by sibling
                            (int) params4[3]//5 //node trigger
                    ),
                    "divisive-fitting-approx-" + "Power-" + params4[0] + "-Samples-" + params4[1] + "-sibling-error-" + params4[2] + "-", true)));

            double[] params3 = {
                    5, //max power
                    100, // sample per var
                    0.00000025, // max mse with sibling
                    30 //node trigger
            };
            infoList.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new DivisiveRegressionBasedXaddApproximator(
                            new MeanSquareErrorMeasure(),
                            (int) params3[0], //max power
                            (int) params3[1], //340 sample per continuous var
                            0.01, //regularization
                            params3[2], //0.00000017 max error by sibling
                            (int) params3[3]//5 //node trigger
                    ),
                    "divisive-fitting-approx-" + "Power-" + params3[0] + "-Samples-" + params3[1] + "-sibling-error-" + params3[2] + "-", true)));


            double[] params1 = {
                    3, //max power
                    100, // sample per var
                    0.00000025, // max mse with sibling
                    30 //node trigger
            };
            infoList.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new DivisiveRegressionBasedXaddApproximator(
                            new MeanSquareErrorMeasure(),
                            (int) params1[0], //max power
                            (int) params1[1], //340 sample per continuous var
                            0.01, //regularization
                            params1[2], //0.00000017 max error by sibling
                            (int) params1[3]//5 //node trigger
                    ),
                    "divisive-fitting-approx-" + "Power-" + params1[0] + "-Samples-" + params1[1] + "-sibling-error-" + params1[2] + "-", true)));


            double[] params = {
                    3, //max power
                    100, // sample per var
                    0.00000040, // max mse with sibling
                    30 //node trigger
            };
            infoList.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new DivisiveRegressionBasedXaddApproximator(
                            new MeanSquareErrorMeasure(),
//                            new KLDivergenceMeasure(),
                            (int) params[0], //max power
                            (int) params[1], //340 sample per continuous var
                            0.01, //regularization
                            params[2], //0.00000017 max error by sibling
                            (int) params[3]//5 //node trigger
                    ),
                    "divisive-fitting-approx-" + "Power-" + params[0] + "-Samples-" + params[1] + "-sibling-error-" + params[2] + "-", true)));



      /*      double[] params = {
                    1, //max power
                    50, // sample per var
                    0.00000040, // max mse with sibling
                    30 //node trigger
            };
            infoList.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new DivisiveRegressionBasedXaddApproximator(
                            new MeanSquareErrorMeasure(),
//                            new KLDivergenceMeasure(),
                            (int) params[0], //max power
                            (int) params[1], //340 sample per continuous var
                            0.01, //regularization
                            params[2], //0.00000017 max error by sibling
                            (int) params[3]//5 //node trigger
                    ),
                    "divisive-fitting-approx-" + "Power-" + params[0] + "-Samples-" + params[1] + "-sibling-error-" + params[2] + "-", true)));

   */


            // ******** AGGLOMERATIVE *****************
            int maxPower = 2;
            int maxNumberOfRegions = 10;
            int sampleNumPerContinuousVar = 30;
            DivergenceMeasure divergenceMeasure =
                    new ZeroFriendlyMeanSquareErrorMeasure();
//                    new KLDivergenceMeasure();
            infoList.add(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new AgglomerativeRegressionBasedXaddApproximator(
                            divergenceMeasure,
                            maxPower, //max power
                            sampleNumPerContinuousVar, //sample per continuous var
                            0.01, //regularization
                            maxNumberOfRegions//8 //max number of regions
                    ),
                    "agglomerative-fitting-approx-" + maxPower + "-" + maxNumberOfRegions + "-" + sampleNumPerContinuousVar + "-" + divergenceMeasure.measureName(), true // compare with exact
            ));


        }
        makeInfoFile(DESTIN_PATH + "info.txt", infoList);

        System.out.println("----------- I N F O   R E P O R T ---------------");
        System.out.println("infoList = " + infoList);
        System.out.println("-------------That was all folks------------------");



    }

    private void makeInfoFile(String filename, List<Info> infoArray) throws FileNotFoundException {
        PrintStream ps;

        ps = new PrintStream(new FileOutputStream(filename));

        for (Info info : infoArray) {
            ps.println(info.destinationFile + "\t" + info.title + "\t" + info.dimension);
        }
        ps.close();


        //write table info as well:
        ps = new PrintStream(new FileOutputStream(DESTIN_PATH + "INFO-table.txt"));

        for (Info info : infoArray) {
            ps.println("title= " + info.title);
            ps.println("[#leaves:#exactLeaves] = " + info.leaves + ":" + info.exactLeaves);
            ps.println("[#nodes:#exactNodes]   = " + info.nodes + ":" + info.exactNodes);
            ps.println("mse       = " + info.mse);
            ps.println("~mse      =" + info.estimatedMse);
            ps.println("process time           = " + info.processTime);
            ps.println("* * *");
        }
        ps.close();
    }


    public Info generateExactSveInferenceResults(Info inputInfo) throws Exception {
        String type = "exact-sve-";
        Info info = new Info(inputInfo.gmCompletePath, inputInfo.queryCompletePath,
                inputInfo.destinationPath, type + inputInfo.destinationFile, type + inputInfo.title);
        FBQuery q = new FBQuery(info.queryCompletePath);

        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(info.gmCompletePath, q, Approximator.DUMMY_APPROXIMATOR /*I think I do not need any approximator here*/);

        ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
        Factor exactResultF = exact.infer(null, true);

        Records exactRecords = exact.getRecords();
        System.out.println("exactRecords = " + exactRecords);

        System.out.println("----------- F I N A L   F A C T O R S --------------");
        System.out.println("exactRecords.getFinalResultRecord() = " + exactRecords.getFinalResultRecord());
        System.out.println("----------- . . . . . . . . . . . . .---------------");

        factory.getVisualizer().visualizeFactor(exactResultF, type + info.title);
        info.dimension = factory.getVisualizer().dataExportFactor(exactResultF, info.destinationPath + info.destinationFile);
        info.records = exactRecords;

        return info;
    }

    public Info generateApproximateSveInferenceResults(
            Info inputInfo,
            Approximator approximator,
            String typeString,
            boolean compareWithExactSve) throws Exception {
        Info info = new Info(inputInfo.gmCompletePath, inputInfo.queryCompletePath,
                inputInfo.destinationPath, typeString + inputInfo.destinationFile, typeString + "-" + inputInfo.title);
        FBQuery q = new FBQuery(info.queryCompletePath);

        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(info.gmCompletePath, q, approximator);

        long startTime = System.nanoTime();
        ApproxSveInferenceEngine approxInferEngine = new ApproxSveInferenceEngine(factory);
        Factor approxResultF = approxInferEngine.infer();
        long processTime = System.nanoTime() - startTime;
        info.processTime = processTime / 1000000;

        Records approxRecords = approxInferEngine.getRecords();
        System.out.println("approx Records = " + approxRecords);

        System.out.println("----------- F I N A L   F A C T O R S --------------");
        System.out.println("approx Records.getFinalResultRecord() = " + approxRecords.getFinalResultRecord());
        System.out.println("----------- . . . . . . . . . . . . .---------------");

        factory.getVisualizer().visualizeFactor(approxResultF, typeString + info.title);
        info.dimension = factory.getVisualizer().dataExportFactor(approxResultF, info.destinationPath + info.destinationFile);
        info.records = approxRecords;

//        String s = approxResultF.getNode().toString();
//        System.out.println("approx node: " + s.substring(0, Math.min(100, s.length())) + "...");

        int approxLeaves = approxResultF.getLeafCount();
        int approxNodes = approxResultF.getNodeCount();
        info.leaves = approxLeaves;
        info.nodes = approxNodes;

        try {
            if (compareWithExactSve) {
                ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
                Factor exactResultF = exact.infer(true);
                info.exactLeaves = exactResultF.getLeafCount();
                info.exactNodes = exactResultF.getNodeCount();

                boolean calculateMseParametrically = true;
                boolean estimateMseBySampling = true;

                if (calculateMseParametrically) {
                    double mse = factory.meanSquaredError(approxResultF, exactResultF, false);
                    info.mse = mse;
                }

                if (estimateMseBySampling) {
                    double estimatedMse = factory.mseApproxDivergence(approxResultF, exactResultF, 111); //#sample per var...
                    info.estimatedMse = estimatedMse;
                }

//                RegionAndLeafHighlighter.visualizeLeaves(exactResultF, "exact leaves");
//                RegionAndLeafHighlighter.visualizeRegions(approxResultF, "approx regions");
//                RegionAndLeafHighlighter.visualizeLeaves(approxResultF, "approx leaves");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return info;
    }


    static class Info {

        String gmCompletePath;
        String queryCompletePath;

        String destinationPath;
        String destinationFile;

        String title;
        Integer dimension = null;
        Records records = null;

        int leaves = -1;
        int nodes = -1;
        int exactLeaves = -1;
        int exactNodes = -1;
        double mse = -1;
        double estimatedMse = -1;
        long processTime = -1;

        Info(String gmCompletePath, String queryCompletePath, String destinationPath, String destinationFile, String title) {
            this.gmCompletePath = gmCompletePath.trim();
            this.queryCompletePath = queryCompletePath.trim();

            this.destinationPath = destinationPath.trim();
            this.destinationFile = destinationFile.trim();

            this.title = title.trim();
        }

        @Override
        public String toString() {
            return "* Info: \n" +
                    "  gmCompletePath=          '" + gmCompletePath + "'\n" +
                    ", queryCompletePath=       '" + queryCompletePath + "'\n" +
                    ", destinationPath=         '" + destinationPath + "'\n" +
                    ", destinationFile=         '" + destinationFile + "'\n" +
                    ", title=                   '" + title + "'\n" +
                    ", dimension=               " + dimension + "\n" +
//                    ", records=" + records +
                    ", [#leaves:#exactLeaves]=  " + leaves + ":" + exactLeaves +
                    ", [#nodes:#exactNodes]=    " + nodes + ":" + exactNodes +
                    ", mse=        " + mse + "\n" +
                    ", approximated mse= " + estimatedMse + "\n" +
                    ", process time(ms)= " + processTime +
                    "}\n\n";
        }
    }
}
