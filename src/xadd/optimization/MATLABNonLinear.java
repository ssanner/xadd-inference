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

        //
        if(MATLABNonLinear.factory == null) {
            MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
                    .setHidden(true)
                    .setProxyTimeout(30000L)
                    .build();
            MATLABNonLinear.factory = new MatlabProxyFactory(options);
        }

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
        double solutionX = 0.0;

        // convert the variables Set into a HashMap
        HashMap<Integer, String> variablesMap = new HashMap<Integer, String>();

        Integer varCount = 1;
        for(String var : variables) {
            variablesMap.put(varCount++, var);
        }

        this.writeObjectiveFile(objective, variablesMap);
        this.writeConstraintsFile(constraints, lowerBounds, upperBounds);

        try {
//            proxy = factory.getProxy();

            this.proxy.eval("res = fmincon(@sir_path_2_h3, [0 0], [], [], [], [], [0 0], [100 0.1], @sir_path_2_h3_constraints)");

            //returns the value of the objective function FUN at the solution X.
            valueObjectiveFunction = ((double[]) proxy.getVariable("res"))[0];
            solutionX = ((double[]) proxy.getVariable("res"))[1];
            System.out.println("Result: " + valueObjectiveFunction + " at x = " + solutionX);

            this.proxy.disconnect();
            this.proxy.exit();
        } catch (MatlabInvocationException e) {
            e.printStackTrace();
        }

        return valueObjectiveFunction;
    }

    /**
     *
     * @param objective
     * @param variablesMap
     * @return
     */
    private Boolean writeObjectiveFile(String objective, HashMap<Integer, String> variablesMap) {

        System.out.println("Minimise: " + objective);

        HashMap<String, Object> root = new HashMap<String, Object>();
        root.put("objective", objective);

        ArrayList<HashMap<String, Object>> variableList = new ArrayList<HashMap<String, Object>>();

        for(Map.Entry<Integer, String> entry : variablesMap.entrySet()) {
            HashMap<String, Object> tmpMap = new HashMap<String, Object>();
            tmpMap.put("symbol", entry.getValue());
            tmpMap.put("index", entry.getKey());

            variableList.add(tmpMap);
        }

        root.put("variables", variableList);

//        ArrayList<HashMap<String, Integer>> constantList = new ArrayList<HashMap<String, Integer>>();
//        root.put("constants", constantList);

        Template temp;
        try {
            temp = this.cfg.getTemplate("MATLAB_objective.ftlh");

            Writer out = new OutputStreamWriter(System.out);
            temp.process(root, out);

            out.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (TemplateException e) {
            e.printStackTrace();
        }

        return Boolean.TRUE;
    }

    /**
     *
     * @param constraints
     * @param lowerBounds
     * @param upperBounds
     * @return
     */
    private Boolean writeConstraintsFile(Collection<String> constraints, Collection<String> lowerBounds,
                                      Collection<String> upperBounds) {

        // Nonlinear equality constraints have to be in the form c(x) <= 0
        // Nonlinear equality constraints are of the form ceq(x) = 0.

        System.out.println("Subject to:");

        for(String constraint : constraints) {
            System.out.println("\t" + constraint);
        }

        lowerBounds.addAll(upperBounds);

        for(String bound : lowerBounds) {
            System.out.println("\t" + bound);
        }

        return Boolean.TRUE;
    }
}
