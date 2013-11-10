package hgm.asve.cnsrv.infer.fin;

import hgm.asve.cnsrv.approxator.Approximator;
import hgm.asve.cnsrv.approxator.EfficientPathIntegralCalculator;
import hgm.asve.cnsrv.approxator.MassThresholdXaddApproximator;
import hgm.asve.cnsrv.approxator.SiblingXaddApproximator;
import hgm.asve.cnsrv.approxator.fitting.CurveFittingBasedXaddApproximator;
import hgm.asve.cnsrv.approxator.fitting.KLDivergenceMeasure;
import hgm.asve.cnsrv.approxator.fitting.MeanSquareErrorMeasure;
import hgm.asve.cnsrv.factor.Factor;
import hgm.asve.cnsrv.factory.ModelBasedXaddFactorFactory;
import hgm.asve.cnsrv.gm.FBQuery;
import hgm.asve.cnsrv.infer.ApproxSveInferenceEngine;
import hgm.asve.cnsrv.infer.ExactSveInferenceEngine;
import hgm.asve.cnsrv.infer.Records;
import org.junit.Test;


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

    @Test
    public void produceResults() throws Exception {

        Info[] inputInfoArray = new Info[]{
                //model                                 query                       destination.path  destination.filename title           dim
                //.new Info(SOURCE_PATH + "radar.gm   ", SOURCE_PATH + "radar.query.1   ", DESTIN_PATH, "radar.query.1   ", "radar-1   "),
//                new Info(SOURCE_PATH + "radar.gm   ", SOURCE_PATH + "radar.query.3   ", DESTIN_PATH, "radar.query.3   ", "radar-3   "),  //2D
                new Info(SOURCE_PATH + "radar.gm   ", SOURCE_PATH + "radar.query.4   ", DESTIN_PATH, "radar.query.4   ", "radar-4   "),  //2D
//                new Info(SOURCE_PATH + "tracking.gm", SOURCE_PATH + "tracking.query.1", DESTIN_PATH , "tracking.query.1", "tracking-1"),  //.
//                .new Info(SOURCE_PATH + "tracking.gm", SOURCE_PATH + "tracking.query.2", DESTIN_PATH, "tracking.query.2", "tracking-2"),
//                new Info(SOURCE_PATH + "tracking.gm", SOURCE_PATH + "tracking.query.3", DESTIN_PATH, "tracking.query.3", "tracking-3"),
//                new Info(SOURCE_PATH + "tracking.gm", SOURCE_PATH + "tracking.query.4", DESTIN_PATH , "tracking.query.4", "tracking-4"),  //.
                //. new Info(SOURCE_PATH + "tracking.gm", SOURCE_PATH + "tracking.query.5", DESTIN_PATH, "tracking.query.5", "tracking-5"),
//                new Info(SOURCE_PATH + "tracking.gm", SOURCE_PATH + "tracking.query.6", DESTIN_PATH , "tracking.query.6", "tracking-6"),  //.
        };

        List<Info> outputInfo = new ArrayList<Info>();
        for (int i = 0; i < inputInfoArray.length; i++) {
//            outputInfo.addAll(Arrays.asList(generateExactSveInferenceResults(inputInfoArray[i]))); //todo commented temporarily
//            outputInfo.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
//                    new MassThresholdXaddApproximator(new EfficientPathIntegralCalculator(), 0.04/*0.04 mass threshold*/, Double.MAX_VALUE), 1, "mass-threshold-approx-"))); //todo commented tempo

//            outputInfo.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
//                    new SiblingXaddApproximator(
//                            new EfficientPathIntegralCalculator(), 100, 0.04),
//                    1, "sibling-approx-"))); //todo [Does not work! even for simple cases... since integration consumes all resources]

            outputInfo.addAll(Arrays.asList(generateApproximateSveInferenceResults(inputInfoArray[i],
                    new CurveFittingBasedXaddApproximator(
                            new MeanSquareErrorMeasure(),
//                            new KLDivergenceMeasure(),
                            3/*max power*/, 100 /*sample per continuous var*/, 0.01 /*regularization*/, 0.00000000067 /*maxx error by sibling*/, 5/*node trigger*/),
                   1 /*num factors leading to joint*/, "curved-fitting-approx-")));
        }

        makeInfoFile(DESTIN_PATH + "info.txt", outputInfo);

        System.out.println("-------------That was all folks------------------");

    }

    private void makeInfoFile(String filename, List<Info> infoArray) throws FileNotFoundException {
        PrintStream ps;

        ps = new PrintStream(new FileOutputStream(filename));

        for (Info info : infoArray) {
            ps.println(info.destinationFile + "\t" + info.title + "\t" + info.dimension);
        }
        ps.close();
    }


    public Info generateExactSveInferenceResults(Info inputInfo) throws Exception {
        String type = "exact-sve-";
        Info info = new Info(inputInfo.gmCompletePath, inputInfo.queryCompletePath,
                inputInfo.destinationPath, type + inputInfo.destinationFile, type + inputInfo.title);
        FBQuery q = new FBQuery(info.queryCompletePath);

        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(info.gmCompletePath, q, null /*I think I do not need any approximator here*/);

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
            int numberOfFactorsLeadingToJoint/*todo this parameter should be dealt with differently (?)*/,
            String typeString) throws Exception {
        Info info = new Info(inputInfo.gmCompletePath, inputInfo.queryCompletePath,
                inputInfo.destinationPath, typeString + inputInfo.destinationFile, typeString + inputInfo.title);
        FBQuery q = new FBQuery(info.queryCompletePath);

        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(info.gmCompletePath, q, approximator);
        ApproxSveInferenceEngine approxInferEngine = new ApproxSveInferenceEngine(factory, numberOfFactorsLeadingToJoint);
        Factor approxResultF = approxInferEngine.infer();

        Records approxRecords = approxInferEngine.getRecords();
        System.out.println("approx Records = " + approxRecords);

        System.out.println("----------- F I N A L   F A C T O R S --------------");
        System.out.println("approx Records.getFinalResultRecord() = " + approxRecords.getFinalResultRecord());
        System.out.println("----------- . . . . . . . . . . . . .---------------");

        factory.getVisualizer().visualizeFactor(approxResultF, typeString + info.title);
        info.dimension = factory.getVisualizer().dataExportFactor(approxResultF, info.destinationPath + info.destinationFile);
        info.records = approxRecords;

        String s = approxResultF.getNode().toString();
        System.out.println("approx node: " + s.substring(0, Math.min(100, s.length())) + "...");

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

        Info(String gmCompletePath, String queryCompletePath, String destinationPath, String destinationFile, String title) {
            this.gmCompletePath = gmCompletePath.trim();
            this.queryCompletePath = queryCompletePath.trim();

            this.destinationPath = destinationPath.trim();
            this.destinationFile = destinationFile.trim();

            this.title = title.trim();
        }
    }
}
