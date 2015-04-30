//////////////////////////////////////////////////////////////////////
//
// Extended Algebraic Decision Diagrams Package
// Linear Approximation, merges linear leaves 
// creating a new linear function that minimize error
// union of the constrained regions
//
// @author Luis Vianna
//////////////////////////////////////////////////////////////////////

package xadd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;

import lpsolve.LP;
import lpsolve.LpSolve;

import util.IntPair;
import xadd.LinearXADDMethod;
import xadd.XADD.ExprDec;
import xadd.XADD.XADDINode;
import xadd.XADD.XADDNode;
import xadd.XADD.XADDTNode;

public class LinearApproximationMethod extends LinearXADDMethod {

    //Prunning Precision constants
    private final static double PRUNE_MIN_ITER_IMP = 1e-10; //Stop Condition for linear pruning algorithm
    private static final double UNDERCONSTRAINED_ALLOW_REL_ERROR = 0.05; //Error difference allowed for underconstrained solution
    private static final double SINGLE_MERGE_PART = 0.2; //Error difference allowed for underconstrained solution

    //Prunning Flags
    private static final boolean UNDERCONSTRAINED_REFINEMENT = false;//true; //solve underconstrained problem in linear approx

    //Debug Flags
    private final static boolean PRUNE_PATH_DBG = false;
    private final static boolean PRUNE_MERGE_DBG = false;
    private final static boolean PRUNE_UNION_DBG = false;
    private final static boolean PRUNE_REMAP_DBG = false;
    private final static boolean UNDERCONSTRAINED_DBG = false;
    private final static boolean REPORT_UNBOUNDED = false;
    
    //Pruning Caches
    public HashMap<Integer, Integer> _hmRemap = new HashMap<Integer, Integer>();
    public HashMap<Integer, ArrayList<HashSet<Integer>>> _hmDecList = new HashMap<Integer, ArrayList<HashSet<Integer>>>();
    Comparator<IntPair> queueComp = new IntPair12Comparator();
    public PriorityQueue<IntPair> _pqOpenNodes = new PriorityQueue<IntPair>(10, queueComp);

    public LinearApproximationMethod(int localRoot, XADD global) {
        super(localRoot, global);
    }

    
    
    ///////////////////
    //Linear Pruning//
    //////////////////

    // Remap for prunning   
    public int remap(int id) {
        Integer ret = _hmRemap.get(id);
        if (ret != null) {
            while (id != ret) { // remaps until loop
                id = ret;
                ret = _hmRemap.get(id);
            }
            return ret;     //already checked node
        }

        XADDNode n1 = context.getExistNode(id);
        if (n1 instanceof XADDTNode) {
            //System.out.println("Not remaped leaf! "+id+" keep equal");
            _hmRemap.put(id, id);
            return id;
        }
        XADDINode i1 = (XADDINode) n1;
        if (PRUNE_REMAP_DBG) {
            System.out.println("RemapInode:" + id + " " + (i1._var));
        }

        int low = remap(i1._low);
        int high = remap(i1._high);

        //getINode checks if low == high or if the trio already exists (no changes)
        int new_id = context.getINode(i1._var, low, high);
        _hmRemap.put(new_id, new_id);
        _hmRemap.put(id, new_id);

        return new_id;
    }

    public int linPruneRel(int id, double relError) {
        if (relError <= XADD.PRECISION) return id;
        double allowErr = relError * linMaxVal(id);
        return linPrune(id, allowErr);
    }

    public int linUpperPruneRel(int id, double relError) {
        if (relError <= XADD.PRECISION) return id;
        double allowErr = relError * linMaxVal(id);
        return linUpperPrune(id, allowErr);
    }

    
    @SuppressWarnings("unused")
    public int linPrune(int id, double allowError) {
        if (allowError < XADD.PRECISION) return id;
        while (id != context.reduceLP(id)) {
            //System.err.println("linPrune WARNING: " + id + " different from reduceLP");
            id = context.reduceLP(id);
        }
        if (PRUNE_MERGE_DBG || UNDERCONSTRAINED_DBG)
            System.out.println("Pruning " + id + " with allowError = " + allowError);
        pruneClear();
        pruneUnionPath(id, allowError);
        int pruned = remap(id);
        return pruned;
    }

    public int linUpperPrune(int id, double allowError) {
        if (allowError < XADD.PRECISION) return id;
        while (id != context.reduceLP(id)) {
            //System.err.println("linPrune WARNING: " + id + " different from reduceLP");
            id = context.reduceLP(id);
        }
        if (PRUNE_MERGE_DBG || UNDERCONSTRAINED_DBG)
            System.out.println("Pruning " + id + " with allowError = " + allowError);
        pruneClear();
        upperPruneUnionPath(id, allowError);
        int pruned = remap(id);
        return pruned;
    }

    
    //prune cache flush
    public void pruneClear() {
        _hmRemap.clear();
        _hmDecList.clear();
        _pqOpenNodes.clear();
    }

    //merging nodes just increase number of paths (join)
    private void mergeDec(int node, int p1, int p2) {
        if (_hmDecList.get(node) != null) System.out.println("Merging on previous node!");
        ArrayList<HashSet<Integer>> paths1 = _hmDecList.get(p1);
        ArrayList<HashSet<Integer>> paths2 = _hmDecList.get(p2);
        ArrayList<HashSet<Integer>> newPaths = new ArrayList<HashSet<Integer>>();
        newPaths.addAll(paths1);
        newPaths.addAll(paths2);
        _hmDecList.put(node, newPaths);
    }

    //add the parent decisions (and paths) to the child
    private void addParDec(int node, int dec, int par) {
        ArrayList<HashSet<Integer>> childPaths = _hmDecList.get(node);
        ArrayList<HashSet<Integer>> parentPaths = _hmDecList.get(par);
        if (childPaths == null) {
            childPaths = new ArrayList<HashSet<Integer>>();
        } //new leaf
        for (HashSet<Integer> path : parentPaths) {
            HashSet<Integer> extendPath = new HashSet<Integer>(path);
            if (context._alOrder.get(Math.abs(dec)) instanceof ExprDec) {
                extendPath.add(dec);
            }
            childPaths.add(extendPath);
        }
        _hmDecList.put(node, childPaths);
    }


    //Maximize difference of functions
    private OptimResult restrictedError(double fplus[], double cplus, double fminus[], double cminus, HashSet<Integer> domain) {
        // Setup LP
        int nvars = nLocalCVars;
        double[] obj_coef = new double[nvars]; // objective function
        for (int var_id = 0; var_id < nvars; var_id++) {
            obj_coef[var_id] = fplus[var_id] - fminus[var_id];
        }
        return restrictedMax(obj_coef, cplus - cminus, domain, true);
    }

    //Simplified error calculation, returns possibly smaller than optimal error.
    private double simpleError(double f1[], ArrayList<HashSet<Integer>> paths1, double f2[], ArrayList<HashSet<Integer>> paths2) {
        double avg[] = new double[f1.length];
        for (int i = 0; i < f1.length; i++) {
            avg[i] = (f1[i] + f2[i]) / 2.0;
        }

        OptimResult maxErrs[] = new OptimResult[paths1.size() + paths2.size()];
        OptimResult minErrs[] = new OptimResult[paths1.size() + paths2.size()];
        int pathCont = 0;
        for (HashSet<Integer> path : paths1) {
            maxErrs[pathCont] = restrictedError(f1, 0, avg, 0, path);
            minErrs[pathCont] = restrictedError(avg, 0, f1, 0, path);
            pathCont++;
        }
        for (HashSet<Integer> path : paths2) {
            maxErrs[pathCont] = restrictedError(f2, 0, avg, 0, path);
            minErrs[pathCont] = restrictedError(avg, 0, f2, 0, path);
            pathCont++;
        }
        // maxErr is maximum value of  f - avg in one extremum,
        // (-minErr1) is minimum value of f -avg in another extremum, so
        // maxErr - (-minErr) is the diference in errors (gap), the best a linear
        //function can do is make them equal, halving the greatest.
        // this is a relaxation, an error better than the best possible, because ignores the constant difference  
        // (e.g. it is always 0 for lines with the same direction.)
        
        //Using MaxRange may lead to wrong conclusions about the maximal Error, because the avg is not  
        // guaranteed to minimize the gaps, however, the smallest range is a valid lower bound on the 
        // approximation error.
        double minRange = Double.POSITIVE_INFINITY;
        for (int i = 0; i < pathCont; i++) {
            double range = Math.abs((maxErrs[i].sol_value + minErrs[i].sol_value) / 2d);
            minRange = Math.min(minRange, range);
        }
        if (PRUNE_UNION_DBG) {
            System.out.println("Simple Error: MinRange = " + minRange);
        }
        return minRange;
    }

    //searches for points of greatest error when using fstar to approximate f1 and f2
    private double greatestError(double fstar[], double dstar,
                                 double f1[], double d1, ArrayList<HashSet<Integer>> paths1,
                                 double f2[], double d2, ArrayList<HashSet<Integer>> paths2,
                                 ArrayList<HashSet<PointKey>> extremePoints) {

        //max => fi- f*, min => f* - fi
        OptimResult maxErrs[] = new OptimResult[paths1.size() + paths2.size()];
        OptimResult minErrs[] = new OptimResult[paths1.size() + paths2.size()];
        int pathCont = 0;
        for (HashSet<Integer> path : paths1) {
            maxErrs[pathCont] = restrictedError(f1, d1, fstar, dstar, path);
            minErrs[pathCont] = restrictedError(fstar, dstar, f1, d1, path);
            pathCont++;
        }
        for (HashSet<Integer> path : paths2) {
            maxErrs[pathCont] = restrictedError(f2, d2, fstar, dstar, path);
            minErrs[pathCont] = restrictedError(fstar, dstar, f2, d2, path);
            pathCont++;
        }
        boolean change = false;
        for (int i = 0; i < pathCont; i++) {
            if (extremePoints.get(i).add(new PointKey(maxErrs[i].solution))) change = true;
            if (extremePoints.get(i + pathCont).add(new PointKey(minErrs[i].solution))) change = true;
        }

        double mergeError = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < pathCont; i++) {
            mergeError = Math.max(mergeError, Math.abs(maxErrs[i].sol_value));
            mergeError = Math.max(mergeError, Math.abs(minErrs[i].sol_value));
        }
        if (PRUNE_PATH_DBG) {
            System.out.format("Change = " + change + ", Error =" + mergeError);
        }
        return mergeError;
    }
    
    //Return function that minimizes absolute error in a finite set of points
    private OptimResult bestLinApprox(double coefs1[], double _dCoef1, ArrayList<HashSet<Integer>> paths1,
                                      double coefs2[], double _dCoef2, ArrayList<HashSet<Integer>> paths2,
                                      ArrayList<HashSet<PointKey>> points) {
        int linVars = nLocalCVars + 2; //the constant of f and one extra minimization var
        double[] obj_coef = new double[linVars]; // objective function min e
        obj_coef[0] = 1;
        for (int i = 1; i < linVars; i++) {
            obj_coef[i] = 0;
        }
        double[] upBound = new double[linVars]; // objective function min e
        double[] loBound = new double[linVars]; // objective function min e
        for (int i = 0; i < linVars; i++) {
            upBound[i] = XADD.DEFAULT_UPPER_BOUND;
            loBound[i] = XADD.DEFAULT_LOWER_BOUND;
        }
        LP lp = new LP(linVars, loBound, upBound, obj_coef, LP.MINIMIZE);

        //add all points as constraints
        double coefConj[][] = new double[2][];
        double dCoefConj[] = new double[2];
        coefConj[0] = coefs1;
        coefConj[1] = coefs2;
        dCoefConj[0] = _dCoef1;
        dCoefConj[1] = _dCoef2;
        double[] constr_coef = new double[linVars];
        int nPaths = paths1.size() + paths2.size();
        for (int j = 0; j < nPaths; j++) {
            //see to which leaf function this path corresponds
            int leafFun = (j < paths1.size()) ? 0 : 1;
            //max => (e > (fi - f*) => e +f* > fi constraint 
            for (PointKey pk : (points.get(j))) {
                constr_coef[0] = 1;
                constr_coef[1] = 1;
                double rhs = dCoefConj[leafFun];
                double _dCoords[] = pk.getCoords();
                for (int i = 0; i < nLocalCVars; i++) {
                    double p_i = _dCoords[i];
                    constr_coef[i + 2] = p_i;
                    rhs += coefConj[leafFun][i] * p_i;
                }
                lp.addGeqConstraint(constr_coef, rhs);
            }
            //min => (e > (f* - fi) => e - f* > -fi constraint
            for (PointKey pk : (points.get(j + nPaths))) {
                constr_coef[0] = 1;
                constr_coef[1] = -1;
                double rhs = -dCoefConj[leafFun];
                double _dCoords[] = pk.getCoords();
                for (int i = 0; i < nLocalCVars; i++) {
                    double p_i = _dCoords[i];
                    constr_coef[i + 2] = -p_i;
                    rhs += -1 * (coefConj[leafFun][i] * p_i);
                }
                lp.addGeqConstraint(constr_coef, rhs);
            }
        }

        double[] soln = silentSolvelp(lp);
        double opt_val = lp._dObjValue;

        if (lp._status == LpSolve.INFEASIBLE) {
            System.out.println("Optimization BestLin Error: Infeasible Min!");
        }
        if (lp._status == LpSolve.UNBOUNDED) {
        	if (REPORT_UNBOUNDED){
        		System.out.println("Optimization BestLin Error: Unbounded Min!");
        	}
            opt_val = Double.POSITIVE_INFINITY;
        }
        if (PRUNE_UNION_DBG) {
            System.out.println("Minimizing optimal Error: " + (opt_val) + " with function " + LP.PrintVector(lp._x));
        }
        lp.free();
        return new OptimResult(opt_val, soln);
    }

    //Return function that minimizes upper bound error in a finite set of points
    private OptimResult bestUpperLinApprox(double coefs1[], double _dCoef1, ArrayList<HashSet<Integer>> paths1,
                                      double coefs2[], double _dCoef2, ArrayList<HashSet<Integer>> paths2,
                                      ArrayList<HashSet<PointKey>> points) {
        int linVars = nLocalCVars + 2; //the constant of f and one extra minimization var
        double[] obj_coef = new double[linVars]; // objective function min e
        obj_coef[0] = 1;
        for (int i = 1; i < linVars; i++) {
            obj_coef[i] = 0;
        }
        double[] upBound = new double[linVars]; // objective function min e
        double[] loBound = new double[linVars]; // objective function min e
        for (int i = 0; i < linVars; i++) {
            upBound[i] = XADD.DEFAULT_UPPER_BOUND;
            loBound[i] = XADD.DEFAULT_LOWER_BOUND;
        }
        LP lp = new LP(linVars, loBound, upBound, obj_coef, LP.MINIMIZE);

        //add all points as constraints
        double coefConj[][] = new double[2][];
        double dCoefConj[] = new double[2];
        coefConj[0] = coefs1;
        coefConj[1] = coefs2;
        dCoefConj[0] = _dCoef1;
        dCoefConj[1] = _dCoef2;
        double[] constr_coef = new double[linVars];
        int nPaths = paths1.size() + paths2.size();
        for (int j = 0; j < nPaths; j++) {
            //see to which leaf function this path corresponds
            int leafFun = (j < paths1.size()) ? 0 : 1;
            
            // Upper Bound constraint => f* > fi constraint  
            for (PointKey pk : (points.get(j))) {
                constr_coef[0] = 0;
                constr_coef[1] = 1;
                double rhs = dCoefConj[leafFun];
                double _dCoords[] = pk.getCoords();
                for (int i = 0; i < nLocalCVars; i++) {
                    double p_i = _dCoords[i];
                    constr_coef[i + 2] = p_i;
                    rhs += coefConj[leafFun][i] * p_i;
                }
                lp.addGeqConstraint(constr_coef, rhs);
            }
            //min => (e > (f* - fi) => e - f* > -fi constraint
            for (PointKey pk : (points.get(j + nPaths))) {
                constr_coef[0] = 1;
                constr_coef[1] = -1;
                double rhs = -dCoefConj[leafFun];
                double _dCoords[] = pk.getCoords();
                for (int i = 0; i < nLocalCVars; i++) {
                    double p_i = _dCoords[i];
                    constr_coef[i + 2] = -p_i;
                    rhs += -1 * (coefConj[leafFun][i] * p_i);
                }
                lp.addGeqConstraint(constr_coef, rhs);
            }
        }

        double[] soln = silentSolvelp(lp);
        double opt_val = lp._dObjValue;

        if (lp._status == LpSolve.INFEASIBLE) {
            System.out.println("Optimization UpperLin Error: Infeasible Min!");
        }
        if (lp._status == LpSolve.UNBOUNDED) {
        	if (REPORT_UNBOUNDED){
        		System.out.println("Optimization UpperLin Error: Unbounded Min!");
        		opt_val = Double.POSITIVE_INFINITY;
        	}
        }
        if (PRUNE_UNION_DBG) {
            System.out.println("Minimizing optimal Error: " + (opt_val) + " with function " + LP.PrintVector(lp._x));
        }
        lp.free();
        return new OptimResult(opt_val, soln);
    }
    
    //Return function that minimizes error in a finite set of points
    private OptimResult minimizeSumError(double coefs1[], double _dCoef1, ArrayList<HashSet<Integer>> paths1,
                                         double coefs2[], double _dCoef2, ArrayList<HashSet<Integer>> paths2,
                                         ArrayList<HashSet<PointKey>> points, double errorLimit) {
        //In how many points are we calculating the error 
        int nPoints = 0;

        //Must order points to associate with errorvar (maybe unnecessary)
        ArrayList<ArrayList<PointKey>> orderedPoints = new ArrayList<ArrayList<PointKey>>();
        for (HashSet<PointKey> pathPoints : points) {
            ArrayList<PointKey> pathPointList = new ArrayList<PointKey>();
            nPoints += pathPoints.size();
            for (PointKey pt : pathPoints) {
                pathPointList.add(pt);
            }
            orderedPoints.add(pathPointList);
        }

        int functionVars = nLocalCVars + 1;
        int linVars = functionVars + nPoints; //the constant of f and the error variables

        if (UNDERCONSTRAINED_DBG) {
            System.out.format("Minimize Sum Err, npoints = %d, nlinVars = %d, error lim = %f\n",
                    nPoints, linVars, errorLimit);
            System.out.println("Functions: f1 = (" + _dCoef1 + ", " + LP.PrintVector(coefs1));
            System.out.println("Functions: f2 = (" + _dCoef2 + ", " + LP.PrintVector(coefs2));
        }

        //Objective function is min sum of ErrPoints (others have 0 coeff)
        double[] obj_coef = new double[linVars]; // objective function min sum of Errors
        int obj_i = 0;
        for (; obj_i < functionVars; obj_i++) {
            obj_coef[obj_i] = 0;
        }
        for (; obj_i < linVars; obj_i++) {
            obj_coef[obj_i] = 1;
        }

        //Coefs have no Bounds, but errors must be positive
        double[] upBound = new double[linVars];
        double[] loBound = new double[linVars];
        int bound_i = 0;
        for (; bound_i < functionVars; bound_i++) {
            upBound[bound_i] = XADD.DEFAULT_UPPER_BOUND;
            loBound[bound_i] = XADD.DEFAULT_LOWER_BOUND;
        }
        for (; bound_i < linVars; bound_i++) {
            upBound[bound_i] = XADD.DEFAULT_UPPER_BOUND;
            loBound[bound_i] = 0d;
        }

        LP lp = new LP(linVars, loBound, upBound, obj_coef, LP.MINIMIZE);


        //add all points as constraints

        //create a joint double structure with info of both functions f1 and f2 to change automatically
        double coefConj[][] = new double[2][];
        double dCoefConj[] = new double[2];
        coefConj[0] = coefs1;
        coefConj[1] = coefs2;
        dCoefConj[0] = _dCoef1;
        dCoefConj[1] = _dCoef2;

        double[] temp_constr_coef = new double[linVars];
        double[] fValues = new double[linVars];

        int nPaths = paths1.size() + paths2.size();
        int errorVarID = functionVars;// the first position of errorVars
        for (int j = 0; j < 2 * nPaths; j++) //the min and max err regions, now joint
        {
            //see to which leaf function this path corresponds to 
            int leafFun = ((j % nPaths) < paths1.size()) ? 0 : 1;

            //fi - f* points 
            for (PointKey pk : (points.get(j))) {
                //for each point: 3 constraints:
                // errorVar smaller than errorLimit
                // errorVar greater than PointError
                // errorVar greater than -1*PointError (in case point error becomes neg)

                //eVar < #limit
                for (int i = 0; i < linVars; i++) temp_constr_coef[i] = 0;
                temp_constr_coef[errorVarID] = 1;
                lp.addLeqConstraint(temp_constr_coef, errorLimit);

                //eVar > (fi-f*)(p) => eVar + f*(p) > fi(p)
                //temp_constr is currently 1 on errorVar and 0 elsewhere, as wanted

                temp_constr_coef[0] = 1; // f* constant term
                double rhs = dCoefConj[leafFun]; //calculate fi(p)
                double _dCoords[] = pk.getCoords(); //point coordinates

                // other coefficients, the constant term uses position 0
                for (int i = 0; i < functionVars - 1; i++) {
                    double p_i = _dCoords[i];
                    temp_constr_coef[i + 1] = p_i; // coef of f*(p)
                    rhs += coefConj[leafFun][i] * p_i; //calculate fi(p)
                }
                fValues[errorVarID] = rhs;
                lp.addGeqConstraint(temp_constr_coef, rhs);

                //eVar > -(fi-f*)(p) => eVar - f*(p) > -fi(p)
                //temp_constr is currently 1 on errorVar and 0 on all other errorVars, as wanted
                temp_constr_coef[0] = -1; // f* constant term
                rhs = -dCoefConj[leafFun]; //calculate -fi(p)
                _dCoords = pk.getCoords(); //point coordinates

                // other coefficients, the constant term uses position 0
                for (int i = 0; i < functionVars - 1; i++) {
                    double p_i = _dCoords[i];
                    temp_constr_coef[i + 1] = -p_i; // coef of -f*(p)
                    rhs += -1 * coefConj[leafFun][i] * p_i; //calculate -fi(p)
                }
                lp.addGeqConstraint(temp_constr_coef, rhs);

                //go to next point
                errorVarID++;
            }
        }

        double[] soln = silentSolvelp(lp);
        double opt_val = lp._dObjValue;

        if (lp._status == LpSolve.INFEASIBLE) {
            System.err.println("Optimization MinimSumError Error: Infeasible Min!");
            System.err.println("Minimizing sum Errors: previous optimal Error: " + errorLimit);
            System.err.println("Minimizing sum Errors: previous fValues:" + LP.PrintVector(fValues));
            return null;
        }
        if (lp._status == LpSolve.UNBOUNDED) {
            System.err.println("Optimization MinimSumError Error: Unbounded Min!");
            opt_val = Double.NEGATIVE_INFINITY;
            return null;
        }
        if (UNDERCONSTRAINED_DBG) {
            System.out.println("Minimizing sum Errors: optimal Error: " + (opt_val) + " with function " + LP.PrintVector(soln));
            System.out.println("Minimizing sum Errors: fvalues:" + LP.PrintVector(fValues));
        }
        lp.free();
        return new OptimResult(opt_val, soln);
    }

    //attempts to merge linearly approximate leafs
    private PruneResult tryMergeLin(int id1, int id2, double error) {
        //id1 and id2 must be terminal nodes!
        XADDTNode l1 = (XADDTNode) context.getExistNode(id1);
        XADDTNode l2 = (XADDTNode) context.getExistNode(id2);
        ArrayList<HashSet<Integer>> paths1 = _hmDecList.get(id1);
        ArrayList<HashSet<Integer>> paths2 = _hmDecList.get(id2);

        int nvars = nLocalCVars;
        double coefs1[] = new double[nvars];
        double coefs2[] = new double[nvars];
        double mrgCoefs[] = new double[nvars];
        double _dCoef1;
        double _dCoef2;
        double _dMrgCoef;
        try {
            _dCoef1 = setCoefficientsLocal(l1._expr, coefs1);
            _dCoef2 = setCoefficientsLocal(l2._expr, coefs2);
            for (int i = 0; i < nvars; i++) {
                mrgCoefs[i] = (coefs1[i] + coefs2[i]) / 2d;
            }
            _dMrgCoef = (_dCoef1 + _dCoef2) / 2d;
        } catch (Exception e2) {
            System.err.println("Could not get Coefficient: " + e2
                    + "\n... hash to #");
            return null;
        }

        // If simple Error is too much dont even find optimal solution 

        if (simpleError(coefs1, paths1, coefs2, paths2) > error) {
            return null;
        }
        if (PRUNE_UNION_DBG) {
            System.out.println("Possible Merge!");
        }
        ArrayList<HashSet<PointKey>> points = new ArrayList<HashSet<PointKey>>();
        for (int i = 0; i < 2 * (paths1.size() + paths2.size()); i++) {
            points.add(new HashSet<PointKey>());
        }

        double oldMaxError, oldMinError;
        double maxError = Double.POSITIVE_INFINITY;
        double minError = Double.POSITIVE_INFINITY;

        int iterCont = 0;
        //Maximize all possible errors (f - f*) restricted each path.
        do {
            if (PRUNE_UNION_DBG) {
                iterCont++;
                System.out.println("Iter Start n= " + iterCont + " MaximizErr = " + maxError + " MinimizError = " + minError);
                System.out.println("Mrg = " + Arrays.toString(mrgCoefs) + " + " + _dMrgCoef);
            }
            oldMaxError = maxError;
            maxError = greatestError(mrgCoefs, _dMrgCoef,
                    coefs1, _dCoef1, paths1,
                    coefs2, _dCoef2, paths2, points);
            OptimResult res = bestLinApprox(coefs1, _dCoef1, paths1, coefs2, _dCoef2, paths2, points);
            oldMinError = minError;
            minError = res.sol_value;
            _dMrgCoef = res.solution[1];
            for (int i = 0; i < nvars; i++) {
                mrgCoefs[i] = res.solution[i + 2];
            }
            if (PRUNE_UNION_DBG) {
                System.out.println("Iter End n= " + iterCont + " MaxErrChange = " + Math.abs(oldMaxError - maxError) + " MinErrorChange = " + Math.abs(oldMinError - minError));
            }
        } while (Math.abs(oldMinError - minError) > PRUNE_MIN_ITER_IMP
                || Math.abs(oldMaxError - maxError) > PRUNE_MIN_ITER_IMP);

        if (maxError < error) {

            if (UNDERCONSTRAINED_REFINEMENT) {
                if (UNDERCONSTRAINED_DBG) {
                    System.out.format("Minimize Sum Err, nlinVars = %d, error lim = %f\n",
                            nvars, maxError);
                    System.out.println("Functions: 1st -Merge = (" + _dMrgCoef + ", " + Arrays.toString(mrgCoefs));
                }
                OptimResult res = minimizeSumError(coefs1, _dCoef1, paths1,
                        coefs2, _dCoef2, paths2, points, maxError * (1 + 0.5 * UNDERCONSTRAINED_ALLOW_REL_ERROR));

                if (res != null) {
                    double underMaxError = XADD.DEFAULT_LOWER_BOUND;

                    _dMrgCoef = res.solution[0];
                    int i = 0;
                    for (; i < nvars; i++) {
                        mrgCoefs[i] = res.solution[i + 1];
                    }
                    //other positions contain error in each point
                    for (i++; i < res.solution.length; i++) {
                        underMaxError = Math.max(underMaxError, res.solution[i]);
                    }

                    if (UNDERCONSTRAINED_DBG) {
                        System.out.format("Minimize Sum Err, nlinVars = %d, undeErrpr = %f, error lim = %f\n",
                                res.solution.length, underMaxError, maxError);
                        System.out.println("Functions: 2nd -Merge = (" + _dMrgCoef + ", " + Arrays.toString(mrgCoefs));
                    }

                    if (underMaxError > maxError * (1 + UNDERCONSTRAINED_ALLOW_REL_ERROR)) {
                        System.out.println("Unconstrained solution violates error:" + underMaxError + " > " + maxError);
                    }
                }
            }
            int new_node = context.getTermNode(
                    getExprFromCoefficientsLocal(_dMrgCoef, mrgCoefs));
            mergeDec(new_node, id1, id2);
            return new PruneResult(new_node, minError);
        }
        return null;
    }

    //attempts to finds a upper bound on linearly approximate leafs
    private PruneResult tryUpperMergeLin(int id1, int id2, double error) {
        //id1 and id2 must be terminal nodes!
        XADDTNode l1 = (XADDTNode) context.getExistNode(id1);
        XADDTNode l2 = (XADDTNode) context.getExistNode(id2);
        ArrayList<HashSet<Integer>> paths1 = _hmDecList.get(id1);
        ArrayList<HashSet<Integer>> paths2 = _hmDecList.get(id2);

        int nvars = nLocalCVars;
        double coefs1[] = new double[nvars];
        double coefs2[] = new double[nvars];
        double mrgCoefs[] = new double[nvars];
        double _dCoef1;
        double _dCoef2;
        double _dMrgCoef;
        try {
            _dCoef1 = setCoefficientsLocal(l1._expr, coefs1);
            _dCoef2 = setCoefficientsLocal(l2._expr, coefs2);
            //Initial coefficients can be arbitrary, so no need for an upper bound start
            for (int i = 0; i < nvars; i++) {
                mrgCoefs[i] = (coefs1[i] + coefs2[i]) / 2d;
            }
            _dMrgCoef = (_dCoef1 + _dCoef2) / 2d;
        } catch (Exception e2) {
            System.err.println("Could not get Coefficient: " + e2
                    + "\n... hash to #");
            return null;
        }

        // If simple Error is too much don't even find optimal solution 
        if (simpleError(coefs1, paths1, coefs2, paths2) > error) {
            return null;
        }
        if (PRUNE_UNION_DBG) {
            System.out.println("Possible Upper Merge!");
        }
        ArrayList<HashSet<PointKey>> points = new ArrayList<HashSet<PointKey>>();
        for (int i = 0; i < 2 * (paths1.size() + paths2.size()); i++) {
            points.add(new HashSet<PointKey>());
        }

        double oldMaxError, oldMinError;
        double maxError = Double.POSITIVE_INFINITY;
        double minError = Double.POSITIVE_INFINITY;

        int iterCont = 0;
        //Maximize all possible errors (f - f*) restricted each path.
        do {
            if (PRUNE_UNION_DBG) {
                iterCont++;
                System.out.println("Iter Start n= " + iterCont + " MaximizErr = " + maxError + " MinimizError = " + minError);
                System.out.println("Mrg = " + Arrays.toString(mrgCoefs) + " + " + _dMrgCoef);
            }
            oldMaxError = maxError;
            maxError = greatestError(mrgCoefs, _dMrgCoef,
                    coefs1, _dCoef1, paths1,
                    coefs2, _dCoef2, paths2, points);
            OptimResult res = bestUpperLinApprox(coefs1, _dCoef1, paths1, coefs2, _dCoef2, paths2, points);
            oldMinError = minError;
            minError = res.sol_value;
            _dMrgCoef = res.solution[1];
            for (int i = 0; i < nvars; i++) {
                mrgCoefs[i] = res.solution[i + 2];
            }
            if (PRUNE_UNION_DBG) {
                System.out.println("Iter End n= " + iterCont + " MaxErrChange = " + Math.abs(oldMaxError - maxError) + " MinErrorChange = " + Math.abs(oldMinError - minError));
            }
        } while (Math.abs(oldMinError - minError) > PRUNE_MIN_ITER_IMP
                || Math.abs(oldMaxError - maxError) > PRUNE_MIN_ITER_IMP);

        if (maxError < error) {

            if (UNDERCONSTRAINED_REFINEMENT) {
                if (UNDERCONSTRAINED_DBG) {
                    System.out.format("Minimize Sum Err, nlinVars = %d, error lim = %f\n",
                            nvars, maxError);
                    System.out.println("Functions: 1st -Merge = (" + _dMrgCoef + ", " + Arrays.toString(mrgCoefs));
                }
                OptimResult res = minimizeSumError(coefs1, _dCoef1, paths1,
                        coefs2, _dCoef2, paths2, points, maxError * (1 + 0.5 * UNDERCONSTRAINED_ALLOW_REL_ERROR));

                if (res != null) {
                    double underMaxError = XADD.DEFAULT_LOWER_BOUND;

                    _dMrgCoef = res.solution[0];
                    int i = 0;
                    for (; i < nvars; i++) {
                        mrgCoefs[i] = res.solution[i + 1];
                    }
                    //other positions contain error in each point
                    for (i++; i < res.solution.length; i++) {
                        underMaxError = Math.max(underMaxError, res.solution[i]);
                    }

                    if (UNDERCONSTRAINED_DBG) {
                        System.out.format("Minimize Sum Err, nlinVars = %d, undeErrpr = %f, error lim = %f\n",
                                res.solution.length, underMaxError, maxError);
                        System.out.println("Functions: 2nd -Merge = (" + _dMrgCoef + ", " + Arrays.toString(mrgCoefs));
                    }

                    if (underMaxError > maxError * (1 + UNDERCONSTRAINED_ALLOW_REL_ERROR)) {
                        System.out.println("Unconstrained solution violates error:" + underMaxError + " > " + maxError);
                    }
                }
            }
            int new_node = context.getTermNode(
                    getExprFromCoefficientsLocal(_dMrgCoef, mrgCoefs));
            mergeDec(new_node, id1, id2);
            return new PruneResult(new_node, minError);
        }
        return null;
    }
    
    //performs approximation and pruning of unnecessary decision in a XADD, assumes prune memory is clear
    public int pruneUnionPath(int root_id, double allowError) {

        XADDNode r = context.getExistNode(root_id);
        if (r instanceof XADDTNode) return root_id; //nothing to prune on single leaf

        //create the initial path, all other will extend from this (adding decisions)
        ArrayList<HashSet<Integer>> rootPathList = new ArrayList<HashSet<Integer>>();
        rootPathList.add(new HashSet<Integer>());
        _hmDecList.put(root_id, rootPathList);
        XADDINode root = (XADDINode) r;
        IntPair current = new IntPair(root._var, root_id);
        while (current != null) {
            int node_dec = current._i1;//only used for ordering (can ignore)
            int node_id = current._i2;
            if (PRUNE_UNION_DBG) {
                if (node_dec == context._alOrder.size()) {
                    System.out.println("Leaf Node: " + node_id
                            + " expr = " + ((XADDTNode) context.getExistNode(node_id))._expr
                            + " DecSet = " + _hmDecList.get(node_id));
                } else {
                    System.out.println("Node: " + node_id
                            + " dec = " + context._alOrder.get(node_dec) + " " + node_dec
                            + " DecSet = " + _hmDecList.get(node_id));
                }
            }

            XADDNode n = context.getExistNode(node_id);

            //Possibly mergeable TNode, for now, all except the Infinites
            if (n instanceof XADDTNode && isMergeable(node_id) ) {
                Iterator<IntPair> qIt = _pqOpenNodes.iterator();
                HashSet<IntPair> solved = new HashSet<IntPair>();
                double allMergeError = allowError;
                double singleMergeError = allowError * SINGLE_MERGE_PART;

                while (qIt.hasNext()) {
                    //tryMerge(n,leaf.leaf.next())
                    IntPair leaf = qIt.next();
                    if (leaf._i1 != context._alOrder.size()) System.out.println("Invalid ordering!");
                    if (PRUNE_UNION_DBG) {
                        System.out.println("n =" + node_id + " comp " + leaf._i2
                                + " " + ((XADDTNode) context.getExistNode(leaf._i2))._expr
                                + " DecSet = " + _hmDecList.get(leaf._i2));
                    }

                    PruneResult res = tryMergeLin(node_id, leaf._i2, Math.min(allMergeError, singleMergeError));


                    if (res != null) {
                        //Merge succesful

                        allMergeError -= res.mergeError;
                        solved.add(leaf);
                        _hmRemap.put(node_id, res.new_id);
                        _hmRemap.put(leaf._i2, res.new_id);
                        int old_id = node_id;
                        node_id = res.new_id; //continue merging from the new node!

                        if (PRUNE_UNION_DBG) {
                            System.out.println("Merge!\nJoin: " + leaf._i2
                                    + "expr = " + ((XADDTNode) context.getExistNode(leaf._i2))._expr
                                    + "DecSet = " + _hmDecList.get(leaf._i2));
                            System.out.println("With: " + old_id
                                    + "expr = " + ((XADDTNode) context.getExistNode(old_id))._expr
                                    + "DecSet = " + _hmDecList.get(old_id));
                            System.out.println("Merged (rem.error=" + allMergeError + " single=" + singleMergeError + ") \nNode: " + node_id
                                    + "expr = " + ((XADDTNode) context.getExistNode(node_id))._expr
                                    + "DecSet = " + _hmDecList.get(node_id));
                        }
                    }
                }
                _pqOpenNodes.removeAll(solved);
                _hmRemap.put(node_id, node_id);
            } else if (n instanceof XADDINode) { // if not TNode must be a INode
                XADDINode node = (XADDINode) n;

                addParDec(node._low, -1 * node._var, node_id);
                addParDec(node._high, node._var, node_id);

                XADDNode low_child = context.getExistNode(node._low);
                IntPair entry;
                if (low_child instanceof XADDTNode) {
                    entry = new IntPair(context._alOrder.size(), node._low);
                } else {
                    entry = new IntPair(((XADDINode) low_child)._var, node._low);
                }
                if (!_pqOpenNodes.contains(entry)) _pqOpenNodes.offer(entry);
                XADDNode high_child = context.getExistNode(node._high);
                if (high_child instanceof XADDTNode) {
                    entry = new IntPair(context._alOrder.size(), node._high);
                } else {
                    entry = new IntPair(((XADDINode) high_child)._var, node._high);
                }
                if (!_pqOpenNodes.contains(entry)) _pqOpenNodes.offer(entry);

            }
            current = _pqOpenNodes.poll();
        }

        return root_id;
    }

    //performs approximation and pruning of unnecessary decision in a XADD, assumes prune memory is clear
    public int upperPruneUnionPath(int root_id, double allowError) {

        XADDNode r = context.getExistNode(root_id);
        if (r instanceof XADDTNode) return root_id; //nothing to prune on single leaf

        //create the initial path, all other will extend from this (adding decisions)
        ArrayList<HashSet<Integer>> rootPathList = new ArrayList<HashSet<Integer>>();
        rootPathList.add(new HashSet<Integer>());
        _hmDecList.put(root_id, rootPathList);
        XADDINode root = (XADDINode) r;
        IntPair current = new IntPair(root._var, root_id);
        while (current != null) {
            int node_dec = current._i1;//only used for ordering (can ignore)
            int node_id = current._i2;
            if (PRUNE_UNION_DBG) {
                if (node_dec == context._alOrder.size()) {
                    System.out.println("Leaf Node: " + node_id
                            + " expr = " + ((XADDTNode) context.getExistNode(node_id))._expr
                            + " DecSet = " + _hmDecList.get(node_id));
                } else {
                    System.out.println("Node: " + node_id
                            + " dec = " + context._alOrder.get(node_dec) + " " + node_dec
                            + " DecSet = " + _hmDecList.get(node_id));
                }
            }

            XADDNode n = context.getExistNode(node_id);

            //Possibly mergeable TNode, for now, all except the Infinites
            if (n instanceof XADDTNode && isMergeable(node_id)) {
                Iterator<IntPair> qIt = _pqOpenNodes.iterator();
                HashSet<IntPair> solved = new HashSet<IntPair>();
                double allMergeError = allowError;
                double singleMergeError = allowError * SINGLE_MERGE_PART;

                while (qIt.hasNext()) {
                    //tryMerge(n,leaf.leaf.next())
                    IntPair leaf = qIt.next();
                    if (leaf._i1 != context._alOrder.size()) System.out.println("Invalid ordering!");
                    if (PRUNE_UNION_DBG) {
                        System.out.println("n =" + node_id + " comp " + leaf._i2
                                + " " + ((XADDTNode) context.getExistNode(leaf._i2))._expr
                                + " DecSet = " + _hmDecList.get(leaf._i2));
                    }

                    PruneResult res = tryUpperMergeLin(node_id, leaf._i2, Math.min(allMergeError, singleMergeError));


                    if (res != null) {
                        //Merge succesful
                        allMergeError -= res.mergeError;
                        solved.add(leaf);
                        _hmRemap.put(node_id, res.new_id);
                        _hmRemap.put(leaf._i2, res.new_id);
                        int old_id = node_id;
                        node_id = res.new_id; //continue merging from the new node!
 
                        if (PRUNE_UNION_DBG) {
                            System.out.println("Merge!\nJoin: " + leaf._i2
                                    + "expr = " + ((XADDTNode) context.getExistNode(leaf._i2))._expr
                                    + "DecSet = " + _hmDecList.get(leaf._i2));
                            System.out.println("With: " + old_id
                                    + "expr = " + ((XADDTNode) context.getExistNode(old_id))._expr
                                    + "DecSet = " + _hmDecList.get(old_id));
                            System.out.println("Merged (rem.error=" + allMergeError + " single=" + singleMergeError + ") \nNode: " + node_id
                                    + "expr = " + ((XADDTNode) context.getExistNode(node_id))._expr
                                    + "DecSet = " + _hmDecList.get(node_id));
                        }
                    }
                }
                _pqOpenNodes.removeAll(solved);
                _hmRemap.put(node_id, node_id);
            } else if (n instanceof XADDINode) { // if not TNode must be a INode
                XADDINode node = (XADDINode) n;

                addParDec(node._low, -1 * node._var, node_id);
                addParDec(node._high, node._var, node_id);

                XADDNode low_child = context.getExistNode(node._low);
                IntPair entry;
                if (low_child instanceof XADDTNode) {
                    entry = new IntPair(context._alOrder.size(), node._low);
                } else {
                    entry = new IntPair(((XADDINode) low_child)._var, node._low);
                }
                if (!_pqOpenNodes.contains(entry)) _pqOpenNodes.offer(entry);
                XADDNode high_child = context.getExistNode(node._high);
                if (high_child instanceof XADDTNode) {
                    entry = new IntPair(context._alOrder.size(), node._high);
                } else {
                    entry = new IntPair(((XADDINode) high_child)._var, node._high);
                }
                if (!_pqOpenNodes.contains(entry)) _pqOpenNodes.offer(entry);

            }
            current = _pqOpenNodes.poll();
        }

        return root_id;
    }

    
    public boolean isMergeable(int node_id){
    	boolean mergeable = (node_id != context.POS_INF);
    	mergeable = mergeable && (node_id != context.NEG_INF);
    	mergeable = mergeable && (node_id != context.NAN);
    	return 	mergeable;
    }
    
    
    //Data Storage Helper Classes
    public class PruneResult {
        int new_id;
        double mergeError;

        PruneResult(int id, double err) {
            new_id = id;
            mergeError = err;
        }
    }

    public class PointKey {
        static final int PK_ROUND_PRECISION = 1000000;
        int _iCoords[];
        int _hashcode;

        PointKey(double point[]) {
            _iCoords = round(point);
            int bits = 0;
            for (int i = 0; i < point.length; i++) bits = bits ^ _iCoords[i];
            _hashcode = bits;
        }

        public int[] round(double solution[]) {
            int ret[] = new int[solution.length];
            for (int i = 0; i < solution.length; i++) ret[i] = (int) Math.round(solution[i] * PK_ROUND_PRECISION);
            return ret;
        }

        public double[] getCoords() {
            double ret[] = new double[_iCoords.length];
            for (int i = 0; i < _iCoords.length; i++) ret[i] = ((double) _iCoords[i]) / PK_ROUND_PRECISION;
            return ret;
        }

        public int hashCode() {
            //System.out.println("Using hashcode: "+_hashcode);
            return _hashcode;
        }

        public boolean equals(Object o2) {
            if (o2 instanceof PointKey) {
                PointKey pk2 = (PointKey) o2;
                if (_hashcode == pk2._hashcode && _iCoords.length == pk2._iCoords.length) {
                    for (int i = 0; i < _iCoords.length; i++)
                        if (_iCoords[i] != pk2._iCoords[i]) return false;
                    return true;
                }
            }
            return false;
        }
    }

    //Comparison Helper Classes

    //Priority Queue Comparator
    public class IntPair12Comparator implements Comparator<IntPair> {
        public int compare(IntPair x, IntPair y) {
            if (x._i1 < y._i1) return -1;
            if (x._i1 > y._i1) return 1;
            if (x._i2 < y._i2) return -1;
            if (x._i2 > y._i2) return 1;
            return 0;
        }
    }

}
