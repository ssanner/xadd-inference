//////////////////////////////////////////////////////////////////////
//
// Extended Algebraic Decision Diagrams Package
// Class defining local variables to one XADD DAG
// and Methods for using the Linear Programming solver LPsolve
//
// @author Scott Sanner (ssanner@gmail.com)
// @author Zahra Zamani
// @author Luis Vianna
//////////////////////////////////////////////////////////////////////

package xadd;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import lpsolve.LP;
import lpsolve.LpSolve;

import util.DevNullPrintStream;
import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.ArithOperation;
import xadd.ExprLib.CompOperation;
import xadd.ExprLib.OperExpr;
import xadd.ExprLib.CompExpr;
import xadd.XADD.Decision;
import xadd.ExprLib.DoubleExpr;
import xadd.XADD.ExprDec;
import xadd.ExprLib.VarExpr;
import xadd.XADD.XADDINode;
import xadd.XADD.XADDNode;
import xadd.XADD.XADDTNode;

public class LinearXADDMethod {

    protected XADD context = null;
    //Local Variables Maintenance
    protected int nLocalCVars = -1;
    protected int nContextCVars = -1;
    protected int localID2cVarID[];
    protected int cVarID2localID[];
    protected ArrayList<String> globalContinuousVarList = null;

    //Linear Flags
    private static final boolean ADD_EXPLICIT_BOUND_CONSTRAINTS_TO_LP = false; //Add bounds as explicit constraints (should not be necessary)
    private static final boolean WARN_INFEASIBLE_REGIONS = true; //Add bounds as explicit constraints (should not be necessary)

    public LinearXADDMethod(int localRoot, XADD global) {
        context = global;
        globalContinuousVarList = context.getContinuousVarList();
        XADDNode r = context.getExistNode(localRoot);
        HashSet<String> pruneVars = r.collectVars();
        pruneVars.removeAll(context._alBooleanVars);
        nLocalCVars = pruneVars.size();
        nContextCVars = context._cvar2ID.size();
        localID2cVarID = new int[nLocalCVars];
        cVarID2localID = new int[nContextCVars];
        for (int k = 0; k < nContextCVars; k++) {
            cVarID2localID[k] = -1;
        } //Flag invalid Vars
        int i = 0;
        for (String var : pruneVars) {
            localID2cVarID[i] = context._cvar2ID.get(var);
            cVarID2localID[context._cvar2ID.get(var)] = i;
            i++;
        }
    }

    public double[] assign2Local(double assign[]) {
        return assign2Local(assign, false);
    }

    public double[] assign2Local(double assign[], boolean ignore) {
        double local[] = new double[nLocalCVars];
        for (int i = 0; i < nContextCVars; i++) {
            int ind = cVarID2localID[i];
            if (ind >= 0) {
                local[ind] = assign[i];
            } else if (Math.abs(assign[i]) > XADD.PRECISION && !ignore) {
                System.err.println("Warning: Transforming assign to local ignores some nonzero variables!");
            }
        }
        return local;
    }

    public double[] local2Assign(double local[]) {
        double assign[] = new double[nContextCVars];
        for (int i = 0; i < nContextCVars; i++) {
            int ind = cVarID2localID[i];
            if (ind > 0) {
                assign[i] = local[ind];
            } else {
                assign[i] = 0d;
            }
        }
        return local;
    }

    public double localEvaluateExpr(ArithExpr e, double localAssign[]) {
        HashMap<String, Double> cont_assign = new HashMap<String, Double>();
        for (int i = 0; i < nContextCVars; i++) {
            cont_assign.put(globalContinuousVarList.get(i), 0d);
        }
        for (int i = 0; i < nLocalCVars; i++) {
            cont_assign.put(globalContinuousVarList.get(localID2cVarID[i]), localAssign[i]);
        }
        return e.evaluate(cont_assign);
    }

    // Lp usage methods
    protected void addDecision(LP lp, int dec) {
        if (dec > 0)
            addConstraint(lp, dec, true);
        else
            addConstraint(lp, -dec, false);
    }

    protected void addConstraint(LP lp, int constraint_id, boolean dec) {

//	      if (DEBUG_CONSTRAINTS)
//	          System.out.println("Adding constraint id [" + constraint_id+ "] = " + dec);

        Decision d = context._alOrder.get(constraint_id);
        if (d instanceof ExprDec) {
            ExprDec e = (ExprDec) d;
            if (!(e._expr._rhs instanceof DoubleExpr)
                    || ((DoubleExpr) e._expr._rhs)._dConstVal != 0d) {
                System.out.println("WARNING: Unexpected RHS constraint value: "
                        + e._expr._rhs + " skipping...");
                return;
            }
            // take these out so it does not terminal upon bilinear
            // decisions
            // new Exception().printStackTrace(System.out);
            // System.exit(1);
            //}
            // From here we just need convert LHS to coefficients and construct
            // correct constraint from CompExpr type
            double[] coefs = new double[nLocalCVars];

            double const_coef = setCoefficientsLocal(e._expr._lhs, coefs); // move to
            // RHS => -
            CompOperation type = dec ? e._expr._type : CompExpr.flipCompOper(e._expr._type);

//	          if (DEBUG_CONSTRAINTS){
//	              System.out.println("- adding "+type+" cons: " + const_coef + " + "
//	                      + LP.PrintVector(coefs) + " <=> "
//	                      + (dec ? "" : "!") + e._expr);
//	          }

            switch (type) {
                case GT:
                    lp.addGTConstraint(coefs, -const_coef);
                    break;
                case GT_EQ:
                    lp.addGeqConstraint(coefs, -const_coef);
                    break;
                case LT:
                    lp.addLTConstraint(coefs, -const_coef);
                    break;
                case LT_EQ:
                    lp.addLeqConstraint(coefs, -const_coef);
                    break;
                case EQ:
                    lp.addEqConstraint(coefs, -const_coef);
                    break;
                case NEQ:
                    break; // Can't add an NEQ constraint
                default:
                    break; // Unknown constraint type
            }
        }
    }

    @SuppressWarnings("unused")
    protected void addLocalBoundConstraints(LP lp) {
        if (!ADD_EXPLICIT_BOUND_CONSTRAINTS_TO_LP) return;
        int nvars = nLocalCVars;
        double var[] = new double[nvars];
        double lb[] = assign2Local(context.lowerBounds, true);
        double ub[] = assign2Local(context.upperBounds, true);

        for (int i = 0; i < nvars; i++) {
            var[i] = 0;
        }
        for (int i = 0; i < nvars; i++) {
            var[i] = 1;
            lp.addGeqConstraint(var, lb[i]);
            lp.addLeqConstraint(var, ub[i]);
            var[i] = 0;
        }
    }

    protected double[] silentSolvelp(LP lp) {
        //Debuging Streams
        //PrintStream ignoreStream = new DevNullPrintStream(); //used to ignore lpSolve output
        //PrintStream outStream=System.out;

        double[] soln = lp.solve();
        return soln;
    }

    // Convert an expression to an array of coefficients and a constant
    //Set Coefficients PrunVar
    protected double setCoefficientsLocal(ArithExpr e, double[] coefs) {
        int error = 0;
        int index = 0;
        double accum = 0d;
        if (e instanceof OperExpr) {
            OperExpr o = ((OperExpr) e);
            if (o._type == ArithOperation.PROD) {
                if (o._terms.size() != 2)
                    error = 1;
                else {
                    index = cVarID2localID[context.getCVarIndex(((VarExpr) o._terms.get(1))._sVarName)];
                    if (index < 0) {
                        System.err.println("WARNING: XADD.SetCoefPrunVar ERROR: Unexpected Variable " + ((VarExpr) o._terms.get(1))._sVarName + " in Expr " + e);
                        System.err.print("Expected Vars: ");
                        for (int k = 0; k < nLocalCVars; k++) {
                            System.err.print(globalContinuousVarList.get(localID2cVarID[k]) + " ");
                        }
                    }
                    coefs[index] = ((DoubleExpr) o._terms.get(0))._dConstVal;
                }
            } else if (o._type == ArithOperation.SUM) {
                for (ArithExpr e2 : o._terms)
                    accum += setCoefficientsLocal(e2, coefs);
            } else
                error = 2;
        } else if (e instanceof DoubleExpr) {
            accum += ((DoubleExpr) e)._dConstVal;
        } else
            error = 3;

        // This error is really important to flag... should not disable.
        // If it occurs, the resulting constraint could be used improperly.
        if (error > 0) {
            System.err.println("WARNING: XADD.SetCoefPrunVar ERROR [" + error + "] -- unexpected LHS constraint term: " + e);
            System.err.println("BOGUS CONSTRAINT MAY BE RETURNED");
            new Exception().printStackTrace(System.err);
        }

        return accum;
    }

    // Converts an array of coefficients and a constant to an expression
    protected ArithExpr getExprFromCoefficientsLocal(double dCoef, double[] coefs) {
        ArithExpr constExpr = new DoubleExpr(dCoef);
        ArrayList<ArithExpr> varTerms = new ArrayList<ArithExpr>();
        varTerms.add(constExpr);
        for (int i = 0; i < coefs.length; i++) {
            double c = coefs[i];
            if (c == 0) continue;
            ArithExpr coef = new DoubleExpr(c);
            ArithExpr var = new VarExpr(globalContinuousVarList.get(localID2cVarID[i]));
            varTerms.add(new OperExpr(ArithOperation.PROD, coef, var));
        }
        return new OperExpr(ArithOperation.SUM, varTerms);
    }

    //Linear Optimization Functions//
    public double linMaxVal(int id) {
        return linMaxMinVal(id, true);
    }

    public double linMinVal(int id) {
        return linMaxMinVal(id, false);
    }

    public double linMaxDiff(int id1, int id2) {
        int dif1 = context.reduceLP(context.apply(id1, id2, XADD.MINUS));
        int dif2 = context.reduceLP(context.apply(id2, id1, XADD.MINUS));
        return Math.max(linMaxVal(dif1), linMaxVal(dif2));
    }

    public double linMaxMinVal(int id, boolean isMax) {
//      while( id != reduceLP(id) ){
//          System.err.println("linMaxMin WARNING: " + id + " different from reduceLP");
//          id=reduceLP(id);
//      }
        return linMaxMinValInt(id, new HashSet<Integer>(), isMax);
    }

    public double linMaxMinValInt(int id, HashSet<Integer> domain, boolean isMax) {
        XADDNode r = context.getExistNode(id);
        if (r instanceof XADDTNode) {
            ArithExpr expr = ((XADDTNode) r)._expr;
            return (restrictedMax(expr, domain, isMax)).sol_value; //nothing to prune on single leaf
        } else { //Inode
            XADDINode node = (XADDINode) r;
            double lowM = XADD.DEFAULT_LOWER_BOUND;
            double highM = XADD.DEFAULT_LOWER_BOUND;
            if (context._alOrder.get(node._var) instanceof ExprDec) {
                domain.add(-1 * node._var);
                lowM = linMaxMinValInt(node._low, domain, isMax);
                domain.remove(-1 * node._var);
                domain.add(node._var);
                highM = linMaxMinValInt(node._high, domain, isMax);
                domain.remove(node._var);
            } else {
                lowM = linMaxMinValInt(node._low, domain, isMax);
                highM = linMaxMinValInt(node._high, domain, isMax);
            }

            return isMax ? Math.max(lowM, highM) : Math.min(lowM, highM);
        }
    }

    // Linear Optimization Argument Functions 
    public NamedOptimResult linMaxArg(int id) {
        return linMaxMinArg(id, true);
    }

    public NamedOptimResult linMinArg(int id) {
        return linMaxMinArg(id, false);
    }

    public NamedOptimResult linMaxMinArg(int id, boolean isMax) {
//      while( id != reduceLP(id) ){
//          System.err.println("linMaxMin WARNING: " + id + " different from reduceLP");
//          id=reduceLP(id);
//      }
        OptimResult res = linMaxMinArgInt(id, new HashSet<Integer>(), isMax);
        HashMap<String, Double> sol = new HashMap<String,Double>();
        for(int i=0; i<res.solution.length;i++){
            sol.put(globalContinuousVarList.get(localID2cVarID[i]), res.solution[i]);
        }
        return new NamedOptimResult(res.sol_value,sol);
    }

    public OptimResult linMaxMinArgInt(int id, HashSet<Integer> domain, boolean isMax) {
        XADDNode r = context.getExistNode(id);
        if (r instanceof XADDTNode) {
            ArithExpr expr = ((XADDTNode) r)._expr;
            return (restrictedMax(expr, domain, isMax)); //nothing to prune on single leaf
        } else { //Inode
            XADDINode node = (XADDINode) r;
            OptimResult lowR = null;
            OptimResult highR = null;
            if (context._alOrder.get(node._var) instanceof ExprDec) {
                domain.add(-1 * node._var);
                lowR = linMaxMinArgInt(node._low, domain, isMax);
                domain.remove(-1 * node._var);
                domain.add(node._var);
                highR = linMaxMinArgInt(node._high, domain, isMax);
                domain.remove(node._var);
            } else {
                lowR = linMaxMinArgInt(node._low, domain, isMax);
                highR = linMaxMinArgInt(node._high, domain, isMax);
            }
            return isMax ? (lowR.sol_value >= highR.sol_value? lowR: highR): (lowR.sol_value >= highR.sol_value? highR:lowR);
        }
    }
    
    
    //Maximize a Linear function
    private OptimResult restrictedMax(ArithExpr e, HashSet<Integer> domain, boolean isMax) {
        double[] coefs = new double[nLocalCVars];
        //if (e instanceof DoubleExpr) return new OptimResult( ((DoubleExpr)e)._dConstVal, coefs); 
        try {
            double const_coef = setCoefficientsLocal(e, coefs); // move to
            return restrictedMax(coefs, const_coef, domain, isMax);
        } catch (Exception e2) {
            System.err.println("Error on restrictMax: Expr" + e + "in " + domain + " error" + e2);
            return null;
        }
    }

    protected OptimResult restrictedMax(double f[], double c, HashSet<Integer> domain, boolean isMax) {
        int nvars = nLocalCVars;
        LP lp = new LP(nvars, assign2Local(context.lowerBounds, true), assign2Local(context.upperBounds, true), f, isMax ? LP.MAXIMIZE : LP.MINIMIZE);
        //Now add all constraints
        for (Integer decision : domain) {
            addDecision(lp, decision);
        }
        addLocalBoundConstraints(lp);

        // Solve and get decision
        double[] soln = silentSolvelp(lp);
        double opt_val = lp._dObjValue;

        if (lp._status == LpSolve.INFEASIBLE) {
        	if (WARN_INFEASIBLE_REGIONS) {
        		System.err.println("Warning: Infeasible region found maximizing linear XADD.");
		        System.err.println("Decisions: "+domain);
		        showDecList(domain);
        	}
            opt_val = isMax? Double.NEGATIVE_INFINITY: Double.POSITIVE_INFINITY;
        }
        if (lp._status == LpSolve.UNBOUNDED) {
            if (WARN_INFEASIBLE_REGIONS) System.err.println("Warning: Unbounded region found maximizing linear XADD.");
//          System.err.println("Decisions: "+domain);
//          showDecList(domain);
            opt_val = isMax? Double.POSITIVE_INFINITY: Double.NEGATIVE_INFINITY;
        }
        lp.free();
        return new OptimResult(opt_val + c, soln);
    }


    //Decision and Contraint Debugging

    private void showDec(int dec) {
        Decision temp = context._alOrder.get(Math.abs(dec));
        System.out.println("dec = " + dec + " : " + temp);
    }

    protected void showDecList(HashSet<Integer> test_dec) {
        System.out.println("Showing decisions " + test_dec);
        for (Integer dec : test_dec)
            showDec(dec);
    }

    private void showDecEval(int dec, double soln[]) {
        Decision temp = context._alOrder.get(Math.abs(dec));
        System.out.print("dec = " + dec + " : " + temp);
        if (temp instanceof ExprDec)
            System.out.println(", LHS evals to :" + localEvaluateExpr(((ExprDec) temp)._expr._lhs, soln));
        else System.out.println();
    }

    protected void showDecListEval(HashSet<Integer> test_dec, double soln[]) {
        String assignment = getExprFromCoefficientsLocal(0, soln).toString().replace('*', '=');
        System.out.println("Showing decisions " + test_dec + " evaluated at " + assignment);
        for (Integer dec : test_dec)
            showDecEval(dec, soln);
    }

    //Helper Class for optimization problems
    public class OptimResult {
        double sol_value;
        double solution[];

        OptimResult(double val, double point[]) {
            sol_value = val;
            solution = point.clone();
        }
    }
    
    public class NamedOptimResult{
        public double sol_value;
        public HashMap<String, Double> assignment;
       
        NamedOptimResult(double val, HashMap<String, Double> sol) {
            sol_value = val;
            assignment = (HashMap<String,Double>) sol.clone();
        }
    }
}




