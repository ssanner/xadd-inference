///////////////////////////////////////////////////////////////////////////
//
// LP Package: Main LP Object
//
// Author: Scott Sanner (ssanner@cs.toronto.edu) 
// Date:   1/5/05
//
// TODO:
//
///////////////////////////////////////////////////////////////////////////

package lpsolve;

import java.text.*;

public class LP {

	public static final boolean SHOW_ADDED_CONSTRAINTS = false;
	public static final boolean SHOW_SOLVER_RESULT = false;
	
	public static final boolean USE_EPSILON_ON_INVERSION = true;
	public static final double  EPSILON = 1e-6d;
	
	public static final int MAXIMIZE = 1;
	public static final int MINIMIZE = 2;

	// Flag that determines whether last solution should be
	// used as starting point (true) or whether to resolve
	// the problem from scratch (false). Should try both
	// settings... not clear that using previous solution helps.
	public static final boolean SET_STARTING_POINT = false;

	public static long _lTime;

	public static DecimalFormat _df = new DecimalFormat("#.###");

	// Previous solution info
	public int _nVars;
	public int _nObjType;
	public double[] _obj;
	public double[] _x;
	public long _lSolnTime;
	public double _dObjValue;
	public int _nConsAdded;
	public int _status;
	public LpSolve _solver = null;

	/*
	 * Vars are numbered 0..(nvars-1), objective is coef on these vars, obj_type
	 * is LP.MINIMIZE or LP.MAXIMIZE
	 */
	public LP(int nvars, double[] objective, int obj_type) {
		this(nvars, null, null, objective, obj_type);
	}

	public LP(int nvars, double[] lower_bounds, double[] upper_bounds,
			double[] objective, int obj_type) {

		try {

			// Create a new Cplex object and get license
			_nVars = nvars;
			_solver = LpSolve.makeLp(0, nvars);
			_nConsAdded = 0;

			// Initialize the objective
			_nObjType = obj_type;
			_obj = objective;
			if (obj_type == LP.MINIMIZE) {
				_solver.setMinim();
			} else if (obj_type == LP.MAXIMIZE) {
				_solver.setMaxim();
			} else {
				System.out.println("Illegal objective type: " + obj_type);
				System.exit(1);
			}
			_solver.setObjFn(pad(_obj));

			// Initialize bounds
			double[] lb = null;
			double[] ub = null;
			if (lower_bounds != null && upper_bounds != null) {
				lb = lower_bounds;
				ub = upper_bounds;
			} else {
				lb = new double[nvars];
				ub = new double[nvars];
				for (int i = 0; i < nvars; i++) {
					lb[i] = -Double.MAX_VALUE;
					ub[i] = Double.MAX_VALUE;
				}
			}
			for (int i = 0; i < nvars; i++) {
				// addGeqConstraint(genUnitVector(i), lb[i]);
				// addLeqConstraint(genUnitVector(i), ub[i]);
				_solver.setLowbo(i + 1, lb[i]);
				_solver.setUpbo(i + 1, ub[i]);
			}

		} catch (LpSolveException e) {
			System.err.println("Concert exception '" + e + "' caught");
			System.exit(1);
		}
	}

	public void setBinary(int var_id) {
		try {
			_solver.setBinary(var_id, true);
		} catch (LpSolveException e) {
			System.err.println("Concert exception '" + e + "' caught");
			System.exit(1);
		}
	}
	
	public void addEqConstraint(double[] coefs, double rhs_value) {

		try {
			if (SHOW_ADDED_CONSTRAINTS)
				System.out.println("Added constraint: " + PrintVector(coefs) + " == " + rhs_value);
			_solver.addConstraint(pad(coefs), LpSolve.EQ, rhs_value);
			_nConsAdded++;
		} catch (LpSolveException e) {
			System.err.println("Exception '" + e + "' caught");
			System.exit(1);
		}

	}

	public void addGeqConstraint(double[] coefs, double rhs_value) {

		try {
			if (SHOW_ADDED_CONSTRAINTS)
				System.out.println("Added constraint: " + PrintVector(coefs) + " >= " + rhs_value);
			_solver.addConstraint(pad(coefs), LpSolve.GE, rhs_value);
			_nConsAdded++;
		} catch (LpSolveException e) {
			System.err.println("Exception '" + e + "' caught");
			System.exit(1);
		}

	}
	
	public void addGTConstraint(double[] coefs, double rhs_value) {

		// 2x + 3y > 5
		// (-1)(2x + 3y) <= 5(-1)
		// -2x - 3y <= -5
		try {
			if (USE_EPSILON_ON_INVERSION)
				rhs_value += EPSILON; // Makes constraint tighter to counter numerical precision
			if (SHOW_ADDED_CONSTRAINTS)
				System.out.println("Added constraint: " + PrintVector(invert(coefs)) + " <= " + -rhs_value);
			_solver.addConstraint(pad(invert(coefs)), LpSolve.LE, -rhs_value);
			_nConsAdded++;
		} catch (LpSolveException e) {
			System.err.println("Exception '" + e + "' caught");
			System.exit(1);
		}

	}

	public void addLeqConstraint(double[] coefs, double rhs_value) {

		try {
			if (SHOW_ADDED_CONSTRAINTS)
				System.out.println("Added constraint: " + PrintVector(coefs) + " <= " + rhs_value);
			_solver.addConstraint(pad(coefs), LpSolve.LE, rhs_value);
			_nConsAdded++;
		} catch (LpSolveException e) {
			System.err.println("Exception '" + e + "' caught");
			System.exit(1);
		}

	}
	
	public void addLTConstraint(double[] coefs, double rhs_value) {

		// 2x + 3y < 5
		// (-1)(2x + 3y) >= 5(-1)
		// -2x - 3y >= -5
		try {
			if (USE_EPSILON_ON_INVERSION)
				rhs_value -= EPSILON; // Makes constraint tighter to counter numerical precision
			if (SHOW_ADDED_CONSTRAINTS)
				System.out.println("Added constraint: " + PrintVector(invert(coefs)) + " >= " + -rhs_value);
			_solver.addConstraint(pad(invert(coefs)), LpSolve.GE, -rhs_value);
			_nConsAdded++;
		} catch (LpSolveException e) {
			System.err.println("Exception '" + e + "' caught");
			System.exit(1);
		}

	}

	/* Should use previous solution if resolving */
	public double[] solve() {

		ResetTimer();

		try {

			_status = _solver.solve();
			if (SHOW_SOLVER_RESULT)
				System.out.println("Solver result [" + _status + "]: "
						+ _solver.getStatustext(_status));

			// Get solution
			_x = _solver.getPtrVariables();

			// Compute and check the objective value for this solution
			_dObjValue = computeObjective();
			if (Math.abs(_dObjValue - _solver.getObjective()) > 1e-6d) {
				System.out
						.println("WARNING: Internal Calculations vs. LpSolve Mismatch");
				System.out.println("         " + _dObjValue + " vs. "
						+ _solver.getObjective());
			}

		} catch (LpSolveException e) {
			System.err.println("Exception '" + e + "' caught");
			System.exit(1);
		}

		_lSolnTime = GetElapsedTime();

		return _x;
	}

	/* Compute objective value */
	public double computeObjective() {
		double obj_val = 0.0d;
		for (int i = 0; i < _x.length; i++) {
			obj_val += _x[i] * _obj[i];
		}
		return obj_val;
	}

	/* Release license and free memory */
	public void free() {
		_solver.deleteLp();
	}

	/* LP Solve ignores vector element 0 */
	public double[] pad(double[] v) {
		double[] n = new double[v.length + 1];
		for (int i = 0; i < v.length; i++)
			n[i + 1] = v[i];
		return n;
	}
	
	public double[] invert(double[] v) {
		double[] v2 = new double[v.length];
		for (int i = 0; i < v.length; i++)
			v2[i] = -v[i];
		return v2;
	}

	public double[] genUnitVector(int i) {
		double[] v = new double[_nVars];
		// Rely on Java's default initialization of arrays to 0
		v[i] = 1d;
		return v;
	}

	////////////////////////////////////////////////////////////////////////////
	// Static Routines
	////////////////////////////////////////////////////////////////////////////

	public static void ResetTimer() {
		_lTime = System.currentTimeMillis();
	}

	// Get the elapsed time since resetting the timer
	public static long GetElapsedTime() {
		return System.currentTimeMillis() - _lTime;
	}

	////////////////////////////////////////////////////////////////////////////
	// Test Routine
	////////////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {

		// How many vars
		int nvars = 3;

		// Maximize 1x_0 + 2x_1 + 3x_2
		double[] obj_coef = { 1.0, 2.0, 3.0 };

		// Set lower bounds of -100.0 for each var
		double[] lb = { -100.0, -100.0, -100.0 };

		// Set upper bounds of 100.0 for each var
		double[] ub = { 100.0, 100.0, 100.0 };

		// Initialize LP to maximize objective
		LP lp = new LP(nvars, lb, ub, obj_coef, LP.MAXIMIZE);

		// Add constraint 1: -x1 + x2 + x3 <= 20
		double[] con1_coef = { -1.0, 1.0, 1.0 };
		lp.addLeqConstraint(con1_coef, 20.0);

		// Add constraint 2: x1 - 3 x2 + x3 <= 30
		double[] con2_coef = { 1.0, -3.0, 1.0 };
		lp.addLeqConstraint(con2_coef, 30.0);

		// Solve and print solution
		SolveAndPrint(lp);

		// Add constraint 3: x1 + x2 <= 100
		double[] con3_coef = { 1.0, 1.0, 0.0 };
		lp.addLeqConstraint(con3_coef, 100.0);

		// Make x2 binary from here out
		lp.setBinary(2);
		
		// Resolve and print solution
		SolveAndPrint(lp);

		// Add constraint 4: x1 + x2 <= 70
		double[] con4_coef = { 0.0, 1.0, 1.0 };
		lp.addLeqConstraint(con4_coef, 70.0);

		// Resolve and print solution
		SolveAndPrint(lp);
		
		// Now make it infeasible
		lp.addGeqConstraint(con4_coef, 71.0);
		
		// Resolve and print solution
		SolveAndPrint(lp);
	}

	public static void SolveAndPrint(LP lp) {

		// Now solve and print values
		System.out.println("\n========================\n");
		double[] soln = lp.solve();
		double obj_val = 0.0d;
		for (int i = 0; i < soln.length; i++) {

			// Print out settings of each variable
			System.out.println("Var x_" + i + " = " + soln[i]);
		}
		System.out.println("Objective value = " + _df.format(lp._dObjValue));
		System.out.println("Solution status = " + lp._status);
		System.out.println("Solution time   = " + lp._lSolnTime);
		// lp._solver.printSolution(lp._nVars);
		// lp._solver.printLp();
	}

	public static String PrintVector(double[] v) {
		StringBuffer sb = new StringBuffer("[ ");
		for (int i = 0; i < v.length; i++) {
			sb.append("x_" + i + "=" + _df.format(v[i]) + " ");
		}
		sb.append("]");
		return sb.toString();
	}
}
