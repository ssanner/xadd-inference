package xadd.optimization;

import freemarker.core.PlainTextOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import matlabcontrol.*;

import java.io.*;
import java.util.*;

public class MATLABNonLinear implements IOptimisationTechnique {

    private Configuration cfg;
    private static MatlabProxyFactory factory;
    private MatlabProxy proxy;

    public MATLABNonLinear() {

        // TODO: Should we just use an instance of the Optimise class instead of registering this with the entire class?
        //Optimise.RegisterOptimisationMethod(this);

        this.cfg = new Configuration(Configuration.VERSION_2_3_25);

        // Load the template directory
        try {
            this.cfg.setDirectoryForTemplateLoading(new
                    File("/Users/skin/repository/xadd-inference-1/src/xadd/optimization/templates"));
            this.cfg.setOutputFormat(PlainTextOutputFormat.INSTANCE);
            this.cfg.setDefaultEncoding("UTF-8");
            this.cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            this.cfg.setLogTemplateExceptions(false);
        }
        catch(IOException ioException) {
            ioException.printStackTrace();
            System.exit(1);
        }

        // Create a MatlabProxyFactory instance
        if(MATLABNonLinear.factory == null) {
            MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
                    .setHidden(true)
                    .setProxyTimeout(30000L)
                    .build();
            MATLABNonLinear.factory = new MatlabProxyFactory(options);
        }

        // Get a MatlabProxy
        try {
            this.proxy = MATLABNonLinear.factory.getProxy();
        } catch (MatlabConnectionException e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * @param objective
     * @param variables
     * @param constraints
     * @param lowerBounds
     * @param upperBounds
     * @return
     */
    @Override
    public double run(String objective, Set<String> variables, Collection<String> constraints, Collection<String> lowerBounds,
                      Collection<String> upperBounds) {

        double valueObjectiveFunction = 0.0;
        double solutionX;

        // convert the variables Set into a HashMap
        ArrayList<HashMap<String, Object>> variableList = new ArrayList<HashMap<String, Object>>();

        Integer varCount = 1;
        for(String var : variables) {
            HashMap<String, Object> tmpMap = new HashMap<String, Object>();
            tmpMap.put("symbol", var);
            tmpMap.put("index", varCount++);

            variableList.add(tmpMap);
        }

        // Create the objective and constraints *.m files
        this.createObjectiveFile(objective, variableList);
        this.createConstraintsFile(constraints, lowerBounds, upperBounds, variableList);

        // Run MATLAB's fmincon function with the objective and constraint files
        try {

            this.proxy.eval("cd('/Users/skin/repository/xadd-inference-1/src/cmomdp/')");
//            this.proxy.eval("res = fmincon(@sir_path_2_h3, [0 0], [], [], [], [], [0 0], [100 0.1], @sir_path_2_h3_constraints)");
            this.proxy.eval("res = fmincon(@obj_test, [0 0 0 0], [], [], [], [], [-10000 -10000 -10000 -10000], [10000 10000 10000 10000], @constraints_test)");

            // Returns the value of the objective function FUN at the solution X.
            valueObjectiveFunction = ((double[]) proxy.getVariable("res"))[0];
            solutionX = ((double[]) proxy.getVariable("res"))[1];

            System.out.println("Result: " + valueObjectiveFunction + " at x = " + solutionX);

            this.proxy.exit();
            this.proxy.disconnect();

        } catch (MatlabInvocationException e) {
            e.printStackTrace();
        }

        return valueObjectiveFunction;
    }

    /**
     *  Write data to a templated file
     *
     * @param data
     * @param templateFileName
     * @param outFileName
     */
    private void writeDataToTemplateFile(HashMap<String, Object> data, String templateFileName, String outFileName) {

        Writer outFile;
        Template objectiveTemp;

        try {

            objectiveTemp = this.cfg.getTemplate(templateFileName);
            outFile = new FileWriter (new File("/Users/skin/repository/xadd-inference-1/src/cmomdp/" + outFileName));

            objectiveTemp.process(data, outFile);

            outFile.flush();
            outFile.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (TemplateException e) {
            e.printStackTrace();
            System.err.println("Unable to write data to " + templateFileName);
            System.exit(1);
        }

    }

    /**
     *
     * @param objective
     * @param variableList
     * @return  File name of the objective file
     */
    private void createObjectiveFile(String objective, ArrayList<HashMap<String, Object>> variableList) {

        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("objective", objective);
        data.put("variables", variableList);



//        ArrayList<HashMap<String, Integer>> constantList = new ArrayList<HashMap<String, Integer>>();
//        data.put("constants", constantList);

        this.writeDataToTemplateFile(data, "MATLAB_objective.ftlh", "obj_test.m");
    }

    /**
     *
     * @param constraints
     * @param lowerBounds
     * @param upperBounds
     * @param variableList
     * @return  File name of the constraints file
     */
    private void createConstraintsFile(Collection<String> constraints, Collection<String> lowerBounds,
                                         Collection<String> upperBounds, ArrayList<HashMap<String, Object>> variableList) {

        // Nonlinear equality constraints have to be in the form c(x) <= 0
        // Nonlinear equality constraints are of the form ceq(x) = 0.

        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("variables", variableList);

        // Constraints
        ArrayList<HashMap<String, Object>> constraintList = new ArrayList<HashMap<String, Object>>();
        Integer constraintCounter = 1;

        for(String constraint : constraints) {
            HashMap<String, Object> tmpMap = new HashMap<String, Object>();

            tmpMap.put("index", constraintCounter);
            tmpMap.put("function", "-1 * (" + constraint + ")");

            constraintList.add(tmpMap);
            constraintCounter++;
        }

//        lowerBounds.addAll(upperBounds);
//
//        for(String bound : lowerBounds) {
//            HashMap<String, Object> tmpMap = new HashMap<String, Object>();
//
//            tmpMap.put("index", constraintCounter);
//            tmpMap.put("function", "-1 * (" + bound + ")");
//
//            constraintList.add(tmpMap);
//            constraintCounter++;
//        }

        data.put("constraints", constraintList);

        this.writeDataToTemplateFile(data, "MATLAB_constraints.ftlh", "constraints_test.m");
    }
}
