package logic.kb.prop;

import java.io.*;
import java.util.*;

public class SimpleDPLL {

	///////////////////////////////////////////////////////////////////////////
	// Member variables
	///////////////////////////////////////////////////////////////////////////
	
	public static final int EVAL_TRUE  = 1;
	public static final int EVAL_FALSE = 2;
	public static final int EVAL_INDET = 3;
	
	public static final Boolean BOOL_TRUE  = new Boolean(true);
	public static final Boolean BOOL_FALSE = new Boolean(false);
	
	public static final boolean USE_IMPROVED_VAR_SELECTION_HEURISTIC = false;
	
	public String _sDIMACSFile;
	public int _nClauses;
	public int _nVars;
	public int _nDepth; 
	public int _nMaxDepth;

	// An array of clauses (represented as sets of Integers)
	public ArrayList<HashSet<Integer>> _clauses;

	// Truth assignment to each var (or null)
	public Boolean[] _assign;
	
	// A counter of how often each literal appears in an active clause
	public int[]     _assignCount;
	
	///////////////////////////////////////////////////////////////////////////
	// Constructor and helper methods
	///////////////////////////////////////////////////////////////////////////

	// Constructor
	public SimpleDPLL() {
		_nDepth = -1;
		_nClauses = -1;
		_nVars = -1;
		_clauses = new ArrayList<HashSet<Integer>>();;
	}
	
	// Read the DIMACS format into internal data structures
	public void readDIMACSFile(String dimacs_file) {
		_sDIMACSFile = dimacs_file;
		try {
			BufferedReader br = new BufferedReader(new FileReader(dimacs_file));
			String line = null;
			int clause_counter = 0;
			while ((line = br.readLine()) != null) {
				
				// Discard comments
				if (line.startsWith("c"))
					continue;
				
				// Read the 'p' line and allocate space for clauses and var assignments
				String[] split = line.split("[\\s]");
				if (split[0].equals("p")) {
					if (!split[1].equals("cnf")) {
						System.out.println("Cannot handle non-cnf: '" + split[1] + "'");
						System.exit(1); // Fail ungracefully
					}
					
					_nVars = new Integer(split[2]); // Hope this is really an integer
					
					// Note: using 1-offset arrays so can directly index into var array
					_assign = new Boolean[_nVars + 1]; // 0 index is a dummy index
					_assignCount = new int[_nVars + 1];
					
					_nClauses = new Integer(split[3]); // Hope this is really an integer
					_clauses.clear();
					for (int i = 0; i < _nClauses; i++)
						_clauses.add(new HashSet<Integer>());
					
					continue;
				}
				
				// Must be a CNF line
				HashSet<Integer> cur_clause = _clauses.get(clause_counter);
				for (int index = 0; index < split.length - 1; index++)
					cur_clause.add(new Integer(split[index]));
				
				if (!split[split.length - 1].equals("0")) {
					System.out.println("Clause line '" + line + "' did not end in 0.");
					System.exit(1); // Fail ungracefully					
				}
				++clause_counter;
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error reading '" + dimacs_file + "':\n" + e);
			System.exit(1); // Fail ungracefully
		}
	}

	// Display CNF info and current variable assignment
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\nDIMACS File: '" + _sDIMACSFile + "'\n");
		sb.append("Variables: " + _nVars + "\n");
		sb.append("Clauses:   " + _nClauses + "\n");
		sb.append("Current assignment: " + getAssignString() + "\n");
		int clause_id = 0;
		for (HashSet<Integer> clause : _clauses) {
			sb.append("#" + clause_id++ + ": [");
			for (Integer literal : clause)
				sb.append(" " + literal);
			sb.append(" ]\n");
			
		}
		return sb.toString();
	}

	// Prints the current non-null assignments
	// NOTE: var_id's start at 1!
	public String getAssignString() {
		StringBuilder sb = new StringBuilder("[");
		for (int var = 1; var < _assign.length; var++)
			if (_assign[var] != null) 
				sb.append(" " + var + "=" + (_assign[var] ? "T" : "F"));
		sb.append(" ]");
		return sb.toString();
	}
		
	// A printing debug helper function (indents by DPLL depth)
	public String indent(int depth) {
		StringBuilder sb = new StringBuilder();
		for (int d = 1; d < depth; d++)
			sb.append("   ");
		return sb.toString();
	}

	///////////////////////////////////////////////////////////////////////////
	// TODO: Main DPLL methods
	///////////////////////////////////////////////////////////////////////////

	// The main entry point to the sat solver... returns true if
	// clauses are unsatisfiable.
	public boolean unsat() {
		//System.out.println(this);
		_nDepth = 0;
		_nMaxDepth = 0;
		HashSet<Integer> active_clauses = new HashSet<Integer>();
		for (int i = 0; i < _nClauses; i++)
			active_clauses.add(new Integer(i));
		int var_branch = chooseBranchVarSimple();
		return dpll(var_branch, true, active_clauses)
			&& dpll(var_branch, false, active_clauses);
	}
	
	// See if a clause evaluates to true or false (or neither = null) 
	// given current assignment in _assign.
	public int evaluateClause(HashSet<Integer> clause) {
		
		// Rule: if any literal is true, return EVAL_TRUE
		//       else if all literals false, return EVAL_FALSE
		//       else return EVAL_INDET
		boolean all_literals_false = true;
		for (Integer lit : clause) {
			boolean neg = lit < 0;
			Boolean assign = _assign[neg ? -lit : lit];
			if (assign == null) {
				all_literals_false = false;
				continue;
			} else if (assign.booleanValue() == !neg) { // assign true, neg false -> satisfied
				return EVAL_TRUE;
			} 
		}
		return all_literals_false ? EVAL_FALSE : EVAL_INDET;
	}

	// Returns single literal if clause is a unit clause (or 0 if none)
	// Watch variables would help efficiency here (see zChaff paper)
	public int findUnit(HashSet<Integer> clause) {
		int cur_unit = 0;
		
		// Because previous units may not have been propagated (yet), we
		// should check for satisfied clauses and ignore them here
		for (Integer literal : clause) {
			int var_id = literal > 0 ? literal : -literal;
			if (_assign[var_id] == null) {
				if (cur_unit == 0)
					cur_unit = literal;
				else
					return 0; // two unassigned vars found
			} else if (_assign[var_id] == (literal > 0 ? true : false)) {
				return 0; // This clause is satisfied, ignore it!
			}
		}
		return cur_unit;
	}
	
	// Copies assignments from _assign to a new Boolean array.
	// Could be more efficient by preallocating these arrays for each
	// level of the DPLL solution.
	public Boolean[] copyAssignments() {
		Boolean[] new_assign = new Boolean[_assign.length];
		System.arraycopy(_assign, 0, new_assign, 0, _assign.length);
		return new_assign;
	}
	
	// Choose the next unassigned variable to branch on
	public int chooseBranchVarSimple() {
		for (int var = 1; var <= _nVars; var++)
			if (_assign[var] == null)
				return var;
		return -1;
	}

	// Choose the variable participating in the most clauses 
	// (a refinement of this would choose the most constrained literal
	//  and potentially first explore the branch most likely to be satisfied)
	public int chooseBranchVarMostConstrained(HashSet<Integer> active_clauses) {

		for (int v = 1; v < _assign.length; v++)
			_assignCount[v] = 0;
		
		for (Integer clause_id : active_clauses) {
			HashSet<Integer> clause = _clauses.get(clause_id);
			for (Integer literal : clause)
				_assignCount[literal > 0 ? literal : -literal]++;
		}
		
		int max_v = -1;
		int max_count = -1;
		for (int v = 1; v < _assign.length; v++) {
			if (_assign[v] == null && _assignCount[v] > max_count) {
				max_v = v;
				max_count = _assignCount[v];
			}
		}
		
		return max_v;
	}
	
	// Determines new assignments due to unit clauses and updates _assign.
	// Repeats until no changes.  Modifies the set of active_clauses.
	// Returns *true* if any clause was found to be unsatisfiable (UNSAT).
	public boolean propagateUnit(HashSet<Integer> active_clauses) {
		
		int prop_units = 0;
		
		// Find all unit clauses and set their assignment 
		for (Integer clause_id : active_clauses) {
			int literal = findUnit(_clauses.get(clause_id));
			//System.out.println("Found unit: " + _clauses.get(clause_id) + " : " + literal);
			if (literal != 0) {
				int prop_id = literal > 0 ? literal : -literal;
				_assign[prop_id] = literal > 0 ? BOOL_TRUE : BOOL_FALSE;
				//System.out.println(indent(_nDepth) + "Assign " + prop_id + 
				//		"=" + _assign[prop_id] + " b/c of " + _clauses.get(clause_id));
				prop_units++;
			}
		}		
		
		// Filter active_clauses... clone the set to be iterated since
		// we might modify it
		for (Integer clause_id : (HashSet<Integer>)active_clauses.clone()) {
			int result = evaluateClause(_clauses.get(clause_id));
			switch (result) {
			case EVAL_FALSE: //System.out.println(indent(_nDepth) + "UNSAT: " + 
					//_clauses.get(clause_id) + " under " + getAssignString()); 
					return true; // Found UNSAT clause
			case EVAL_TRUE:  active_clauses.remove(clause_id); 
					break;
			}
		}
		
		// If unit assignments were made, could have created new unit
		// clauses, so need to repeat
		if (prop_units > 0)
			return propagateUnit(active_clauses);
		else
			return false; // Did not find an UNSAT clause
	}

	// The standard DPLL interface, takes the next variable assignment and the
	// set of currently unsatisfied clauses and returns whether they are 
	// satisfiable (true) or not (false).
	//
	// Note: One should typically take care *not* to call new() in this code,
	//       but I have done so since I am aiming for clarity over efficiency.
	public boolean dpll(int var_id, boolean var_assign, HashSet<Integer> active_clauses) {
		
		++_nDepth;
		if (_nDepth > _nMaxDepth)
			_nMaxDepth = _nDepth;
		
		//System.out.println(indent(_nDepth) + var_id + " = " + var_assign + " : " + active_clauses);
		Boolean unsat = null;
		active_clauses = (HashSet<Integer>)active_clauses.clone();
		
		// Make new assignment for var_id so we can undo before returning
		Boolean[] temp_assign = _assign;
		_assign = copyAssignments();
		_assign[var_id] = var_assign ? BOOL_TRUE : BOOL_FALSE;
		
		// Propagate unit will modify the set of active_clauses and return
		// true if a clause was found to be unsatisfiable.
		if (propagateUnit(active_clauses)) { 
			// Propagate unit clauses and filter out SAT clauses; detect UNSAT
			unsat = BOOL_TRUE;
		} else if (active_clauses.isEmpty()) {
			// Clause set can only be empty because all clauses were satisfied 
			// and removed
			unsat = BOOL_FALSE;
		}
		
		// If indeterminate, need to branch again
		if (unsat == null) {
			int var_to_branch_on = -1;
			if (USE_IMPROVED_VAR_SELECTION_HEURISTIC)
				var_to_branch_on = chooseBranchVarMostConstrained(active_clauses);
			else
				var_to_branch_on = chooseBranchVarSimple();
			
			// Only unsatisfiable if both branches are unsatisfiable
			unsat = dpll(var_to_branch_on, true, active_clauses)
			        && dpll(var_to_branch_on, false, active_clauses);
		}
			
		// Undo assignment for var_id
		_assign = temp_assign;
		
		--_nDepth;
		//System.out.println(indent(_nDepth) + "--> " + unsat);
		return unsat == null ? false : (unsat.booleanValue());
	}
	
	///////////////////////////////////////////////////////////////////////////
	// Test routine
	///////////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		if (args.length <= 1) {
			System.out.println("No DIMACS files provided on command line");
			System.exit(1); 
		}

		SimpleDPLL dpll = new SimpleDPLL();

		for (String filename : args) {
			dpll.readDIMACSFile(filename);
			//System.out.println(dpll); // Show DIMACS file
			long cur_time = System.currentTimeMillis();
			System.out.print(filename + " -> " 
					+ (dpll.unsat() ? "UNSATISFIABLE" : "SATISFIABLE"));
			System.out.println(" (" + (System.currentTimeMillis() - cur_time) + 
					" ms, depth: " + dpll._nMaxDepth + ")");
		}
	}
}
