package hgm.poly;

import hgm.sampling.VarAssignment;

/**
 * Created by Hadi Afshar.
 * Date: 24/02/14
 * Time: 4:38 PM
 */
public interface Function {

    public double evaluate(VarAssignment fullVarAssign);

    public String[] collectContinuousVars();

//    public String[] collectBinaryVars();
}
