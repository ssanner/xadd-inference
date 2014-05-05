package hgm.poly.integral;

/**
 * Created by Hadi Afshar.
 * Date: 3/04/14
 * Time: 2:11 PM
 */
public interface SymbolicOneDimFunctionGenerator {
    //generates a 1-D function in which only one var. remained  parametric
    OneDimFunction makeFunction(Double[] varAssign);
}
