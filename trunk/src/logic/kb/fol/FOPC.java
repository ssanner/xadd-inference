//////////////////////////////////////////////////////////////////////
//
// First-Order Logic Package
//
// Class:  FOPC - Representation for **standard FOPC syntax**
// Author: Scott Sanner (ssanner@cs.toronto.edu)
// Date:   11/08/03
//
// TODO: I don't think CNF conversion does standardize apart!
//
// Note: Terms currently not copied... considered immutable, like
//       Strings.  May need to update this later.
//
//       Looks like equals() on FOPC.Node returns true when not
//       a match... why is this?  Have to use String equivalence for
//       perfect results.
//
//////////////////////////////////////////////////////////////////////

package logic.kb.fol;

import java.io.*;
import java.util.*;

import logic.kb.*;
import logic.kb.fol.kif.HierarchicalParser;
import logic.kb.fol.parser.*;

public class FOPC {

	// ////////////////////////////////////////////////////////////////////
	// Static vars and methods
	// ////////////////////////////////////////////////////////////////////

	// Use Otter variant for FOLFormula printing
	public static boolean USE_OTTER = false;

	// Use Otter variant for FOLFormula printing (TPTP CNF)
	public static boolean USE_VAMPIRE = false;

	// Use Otter variant for FOLFormula printing (TPTP FOF)
	public static boolean USE_TPTP = false;

	// Show free variables during printing?
	public static boolean SHOW_FREE = false;

	// Allow DNF conversion for EXISTS pushdown?
	public static boolean ALLOW_DNF = false;

	// A cache for simplified formulae
	public static HashMap _hmString2SimpFormula = new HashMap();

	// A static method for keeping track of exclusions
	// Entries are "pred_arity" -> Integer indicating fun arg
	// (For now only allowing one)
	public static HashMap _hmFunctionalArgMap = new HashMap();

	// A static method for keeping track of exclusions
	public static synchronized void AddFunctionalRestr(String pred, int arity, int fun_arg) {

		_hmFunctionalArgMap
				.put(pred + "_" + arity, new Integer(fun_arg - 1) /*
																	 * 0-indexed
																	 * positions!
																	 */);
	}

	// A technique for filtering Otter strings
	public static String FilterOtterName(String s) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '#') {
				if (i == 0) {
					continue;
				}
				c = '_';
			}
			if (Character.isLetterOrDigit(c) || c == '_' || c == '$'
					|| c == '#') {
				sb.append(Character.toLowerCase(c));
			}
		}
		return sb.toString();
	}

	public static long EstimateCNF(Node n) {

		if (n instanceof TNode) {
			return 1l;
		} else if (n instanceof PNode) {
			return 1l;
		} else if (n instanceof ConnNode) {
			ConnNode c = (ConnNode) n;
			switch (c._nType) {
			case ConnNode.AND: {
				long count = 0l;
				Iterator i = c._alSubNodes.iterator();
				while (i.hasNext()) {
					count += EstimateCNF((Node) i.next());
				}
				return count;
			}
			case ConnNode.OR: {
				long count = 1l;
				Iterator i = c._alSubNodes.iterator();
				while (i.hasNext()) {
					count *= EstimateCNF((Node) i.next());
				}

				// if (count < 0 || count > 5000) {
				// System.out.println("FOPC: Large CNF!");
				// i = c._alSubNodes.iterator();
				// while (i.hasNext()) {
				// Node n2 = (Node)i.next();
				// System.out.println("** DISJ: [" + EstimateCNF(n2) +
				// "] " + n2.toFOLString());
				// }
				// System.exit(1);
				// }

				return count;
			}
			case ConnNode.IMPLY: {
				FOPC.Node n1 = (Node) c._alSubNodes.get(0);
				FOPC.Node nn1 = n1.copy().convertNNF(true);
				FOPC.Node n2 = (Node) c._alSubNodes.get(1);
				return EstimateCNF(n2) * EstimateCNF(nn1);
			}
			case ConnNode.EQUIV: {
				FOPC.Node n1 = (Node) c._alSubNodes.get(0);
				FOPC.Node n2 = (Node) c._alSubNodes.get(1);
				FOPC.Node nn1 = n1.copy().convertNNF(true);
				FOPC.Node nn2 = n2.copy().convertNNF(true);
				return EstimateCNF(n1) * EstimateCNF(nn2) + EstimateCNF(n2)
						* EstimateCNF(nn1);
			}
			default: {
				System.out.println("ERROR in EstimateCNF: " + n.toFOLString());
				System.exit(1);
				return -1;
			}
			}
		} else if (n instanceof NNode) {
			return EstimateCNF(((NNode) n)._subnode);
		} else if (n instanceof QNode) {
			return EstimateCNF(((QNode) n)._subnode);
		} else {
			System.out.println("ERROR in EstimateCNF: " + n.toFOLString());
			System.exit(1);
			return -1l;
		}
	}

	// A basic command to simplify formula
	public static Node simplify(Node node) {
		return simplify(node, true);
	}

	public static synchronized Node simplify(Node node, boolean simp_eq) {

		node = node.convertNNF(false);
		String str_prev = "";
		String str_init = node.toFOLString();
		String str_cur = str_init;

		// Check cache
		Node cache_node;
		if ((cache_node = (Node) _hmString2SimpFormula.get(str_init + "_"
				+ simp_eq)) != null) {
			return (Node) cache_node.copy();
		}

		// Simplify until no changes
		while (!str_prev.equals(str_cur)) {

			node.setFreeVars();
			node = node.pushDownQuant();
			if (simp_eq) {
				node = node.simplifyEquality(); // convertNNF will reduce T/F
			}

			// TODO: Use this?
			// Now add in redundancy removal
			// SimpResult res = node.removeRedundancy(new ArrayList());
			// node = res._result;

			node = node.convertNNF(false); // Simplifies also!
			str_prev = str_cur;
			str_cur = node.toFOLString();
		}

		// Store in cache
		_hmString2SimpFormula.put(str_init + "_" + simp_eq, node.copy());

		return node;
	}

	// Convert formula to DNF - must deal with FORALL quantification.
	public static Node convertDNF(Node n) {

		// System.out.println("Enter convertDNF: " + n.toFOLString());

		if (n instanceof ConnNode) {

			// Normally, we don't need to push down internal quantifiers but in
			// this
			// case, it may make a DNF conversion possible... redundant with
			// ConnNode
			// collapsing code... should just make new ConnNode here.
			ConnNode c = (ConnNode) n;
			ArrayList new_subnodes = new ArrayList();
			for (int i = 0; i < c._alSubNodes.size(); i++) {
				Node s = (FOPC.Node) c._alSubNodes.get(i);
				s = convertDNF(s.pushDownQuant());

				if ((s instanceof ConnNode)
						&& ((ConnNode) s)._nType == c._nType) {
					new_subnodes.addAll(((ConnNode) s)._alSubNodes);
				} else {
					new_subnodes.add(s);
				}
			}
			c._alSubNodes = new_subnodes;

			// At this point, subnodes will have OR at top if present
			if (c._nType == ConnNode.AND) {

				// Node must be a1 ^ a2 ^ ... ^ an, where ai is either PNode,
				// TNode, Disjunctive
				// Collect all disjuncts in a list
				ArrayList disjuncts = new ArrayList();
				ArrayList others = new ArrayList();
				for (int i = 0; i < c._alSubNodes.size(); i++) {
					Node s = (FOPC.Node) c._alSubNodes.get(i);
					if ((s instanceof ConnNode)
							&& ((ConnNode) s)._nType == ConnNode.OR) {
						disjuncts.add(s);
					} else {
						others.add(s);
					}
				}

				// Perform DNF conversion if necessary
				if (disjuncts.size() > 0) {

					// Distribute AND over OR for disjuncts
					ConnNode conn_disj = (ConnNode) disjuncts.remove(0);
					while (!disjuncts.isEmpty()) {

						ConnNode new_disj = new ConnNode(ConnNode.OR);
						ConnNode conn_disj2 = (ConnNode) disjuncts.remove(0);

						Iterator djs1 = conn_disj._alSubNodes.iterator();
						while (djs1.hasNext()) {
							Node dj1 = (Node) djs1.next();
							Iterator djs2 = conn_disj2._alSubNodes.iterator();
							while (djs2.hasNext()) {
								Node dj2 = (Node) djs2.next();

								ConnNode new_conj = new ConnNode(ConnNode.AND);
								new_conj.addSubNode(dj1);
								new_conj.addSubNode(dj2);
								new_disj.addSubNode(new_conj);
							}
						}

						conn_disj = new_disj;
					}

					// Now distribute conn_disj disjuncts to others
					ConnNode final_disj = new ConnNode(ConnNode.OR);
					Iterator djs = conn_disj._alSubNodes.iterator();
					while (djs.hasNext()) {
						Node dj = (Node) djs.next();
						ConnNode new_conj = new ConnNode(ConnNode.AND);
						new_conj.addSubNodes(others);
						new_conj.addSubNode(dj);
						final_disj.addSubNode(new_conj);
					}

					// Now set c to new value
					c = final_disj;
				}
			}
			return c;
		} else if (n instanceof QNode) {
			// System.out.println("Before QNode: " + n.toFOLString());
			n = n.pushDownQuant();
			((QNode) n)._subnode = convertDNF(((QNode) n)._subnode);
			// System.out.println("After QNode: " + n.toFOLString());
			return n;
		} else if ((n instanceof PNode) || (n instanceof TNode)) {
			return n;
		} else {
			System.out.println("Invalid node for DNF conversion");
			System.exit(1);
			return null;
		}
	}

	// Convert formula to DNF - must deal with FORALL quantification.
	public static Node convertCNF_Old(Node n) {

		if (n instanceof ConnNode) {

			// Normally, we don't need to push down internal quantifiers but in
			// this
			// case, it may make a DNF conversion possible... redundant with
			// ConnNode
			// collapsing code... should just make new ConnNode here. TODO:
			// Think about
			// this code some more.
			ConnNode c = (ConnNode) n;
			ArrayList new_subnodes = new ArrayList();
			for (int i = 0; i < c._alSubNodes.size(); i++) {
				Node s = (FOPC.Node) c._alSubNodes.get(i);
				s = convertCNF_Old(s.pushDownQuant());

				if ((s instanceof ConnNode)
						&& ((ConnNode) s)._nType == c._nType) {
					new_subnodes.addAll(((ConnNode) s)._alSubNodes);
				} else {
					new_subnodes.add(s);
				}
			}
			c._alSubNodes = new_subnodes;

			// At this point, subnodes will have AND at top and we can only
			// perform
			// CNF conversion if this node has OR at top
			if (c._nType == ConnNode.OR) {

				// Node must be a1 ^ a2 ^ ... ^ an, where ai is either PNode,
				// TNode, Disjunctive
				// Collect all disjuncts in a list
				ArrayList conjuncts = new ArrayList();
				ArrayList others = new ArrayList();
				for (int i = 0; i < c._alSubNodes.size(); i++) {
					Node s = (FOPC.Node) c._alSubNodes.get(i);
					if ((s instanceof ConnNode)
							&& ((ConnNode) s)._nType == ConnNode.AND) {
						conjuncts.add(s);
					} else {
						others.add(s);
					}
				}

				// Perform CNF conversion if necessary
				if (conjuncts.size() > 0) {

					// Distribute OR over AND for conjuncts
					// System.out.println("Conjunct > 0");
					// Iterator i = conjuncts.iterator();
					// while (i.hasNext()) {
					// System.out.println( ((Node)i.next()).toFOLString());
					// }
					ConnNode conn_conj = (ConnNode) conjuncts.remove(0);
					while (!conjuncts.isEmpty()) {

						ConnNode new_conj = new ConnNode(ConnNode.AND);
						ConnNode conn_conj2 = (ConnNode) conjuncts.remove(0);

						Iterator cjs1 = conn_conj._alSubNodes.iterator();
						while (cjs1.hasNext()) {
							Node cj1 = (Node) cjs1.next();
							Iterator cjs2 = conn_conj2._alSubNodes.iterator();
							while (cjs2.hasNext()) {
								Node cj2 = (Node) cjs2.next();

								ConnNode new_disj = new ConnNode(ConnNode.OR);
								new_disj.addSubNode(cj1);
								new_disj.addSubNode(cj2);
								new_conj.addSubNode(new_disj);
							}
						}

						conn_conj = new_conj;
					}

					// Now distribute conn_disj disjuncts to others
					ConnNode final_conj = new ConnNode(ConnNode.AND);
					Iterator cjs = conn_conj._alSubNodes.iterator();
					while (cjs.hasNext()) {
						Node cj = (Node) cjs.next();
						ConnNode new_disj = new ConnNode(ConnNode.OR);
						new_disj.addSubNodes(others);
						new_disj.addSubNode(cj);
						final_conj.addSubNode(new_disj);
					}

					// Now set c to new value
					c = final_conj;
				}
			}
			return c;
		} else if (n instanceof QNode) {
			n = n.pushDownQuant();
			if (n instanceof QNode) {
				((QNode) n)._subnode = convertCNF_Old(((QNode) n)._subnode);
				return n;
			} else {
				return convertCNF_Old(n);
			}
		} else if ((n instanceof PNode) || (n instanceof TNode)) {
			return n;
		} else {
			System.out.println("Invalid node for DNF conversion");
			System.exit(1);
			return null;
		}
	}

	public static Node convertCNF(Node n) {

		if (n instanceof ConnNode) {

			ConnNode c = (ConnNode) n;
			if (c._nType == ConnNode.OR) {

				ArrayList new_subnodes = new ArrayList();
				for (int i = 0; i < c._alSubNodes.size(); i++) {
					Node s = convertCNF((FOPC.Node) c._alSubNodes.get(i));

					if (s instanceof ConnNode
							&& ((ConnNode) s)._nType == ConnNode.OR) {
						new_subnodes.addAll(((FOPC.ConnNode) s)._alSubNodes);
					} else {
						new_subnodes.add(s);
					}
				}

				// All below are now in CNF, now distribute AND over OR
				// (AND *must* be next-level)
				// System.out.println("GenDisj: \n" + n.toFOLString() + "\n" +
				// new_subnodes + " --> \n");
				c._alSubNodes.clear();
				GenDisjuncts(new_subnodes, 0, new LinkedList(), c._alSubNodes);

				// Now all subnodes will be OR, this is now an AND
				c._nType = ConnNode.AND;

				return c;
			} else if (c._nType == ConnNode.AND) { // This level is AND => OK

				ArrayList new_subnodes = new ArrayList();
				for (int i = 0; i < c._alSubNodes.size(); i++) {
					Node s = convertCNF((FOPC.Node) c._alSubNodes.get(i));
					if (s instanceof ConnNode
							&& ((ConnNode) s)._nType == ConnNode.AND) {
						new_subnodes.addAll(((FOPC.ConnNode) s)._alSubNodes);
					} else {
						new_subnodes.add(s);
					}
				}
				c._alSubNodes = new_subnodes;

				return c;
			} else {
				System.out.println("Invalid node for CNF conversion: "
						+ n.toFOLString());
				Object o = null;
				o.toString();
				return null;
			}
		} else if (n instanceof QNode) {
			System.out.println("Invalid node for CNF conversion: "
					+ n.toFOLString());
			Object o = null;
			o.toString();
			return null;
		} else if ((n instanceof PNode) || (n instanceof TNode)) {
			return n;
		} else {
			System.out.println("Invalid node for CNF conversion: "
					+ n.toFOLString());
			Object o = null;
			o.toString();
			return null;
		}
	}

	public static void GenDisjuncts(ArrayList conj, int index,
			LinkedList stack, ArrayList out) {

		if (index == conj.size()) {
			// Generate disjunct for stack
			ConnNode disj = new ConnNode(ConnNode.OR);
			for (Iterator i = stack.iterator(); i.hasNext();) {
				FOPC.Node cons = (FOPC.Node) i.next();

				if (cons instanceof ConnNode
						&& ((ConnNode) cons)._nType != ConnNode.OR) {
					System.out
							.println("Improper non-OR node for FOPC.GenDisjuncts[1]: "
									+ cons.toFOLString());
					Object o = null;
					o.toString();
				}

				// Merge any disjuncts at this level
				if (cons instanceof ConnNode) {
					for (Iterator j = ((ConnNode) cons)._alSubNodes.iterator(); j
							.hasNext();) {
						FOPC.Node cons_disj = (FOPC.Node) j.next();
						disj.addSubNode(cons_disj.copy());
					}
				} else {
					disj.addSubNode(cons.copy());
				}
			}
			out.add(disj);
			return;
		}

		Node n = (Node) conj.get(index);
		if (n instanceof ConnNode) {

			ConnNode c = (ConnNode) n;
			if (c._nType != ConnNode.AND) {
				System.out.println("Improper node for FOPC.GenDisjuncts[2]: "
						+ c.toFOLString());
				Object o = null;
				o.toString();
			}

			for (Iterator i = c._alSubNodes.iterator(); i.hasNext();) {
				FOPC.Node cons = (FOPC.Node) i.next();

				if (cons instanceof ConnNode
						&& ((ConnNode) cons)._nType != ConnNode.OR) {
					System.out
							.println("No non-OR ConnNodes at this level FOPC.GenDisjuncts: "
									+ cons.toFOLString());
					Object o = null;
					o.toString();
				}

				stack.addLast(cons);
				GenDisjuncts(conj, index + 1, stack, out);
				stack.removeLast();
			}

		} else {
			stack.addLast(n);
			GenDisjuncts(conj, index + 1, stack, out);
			stack.removeLast();
		}
	}

	// Returns a HashMap that will unify these two PNodes or null if
	// no unification is possible
	//
	// procedure unify(p, q, theta)
	// Scan p and q left-to-right and find the first corresponding
	// terms where p and q "disagree" ; where p and q not equal
	// If there is no disagreement, return theta ; success
	// Let r and s be the terms in p and q, respectively,
	// where disagreement first occurs
	// If variable(r) then
	// theta = union(theta, {r/s})
	// unify(subst(theta, p), subst(theta, q), theta)
	// else if variable(s) then
	// theta = union(theta, {s/r})
	// unify(subst(theta, p), subst(theta, q), theta)
	// else return "failure"
	// end
	//
	// Note: union means you have to also substitute in for
	// current substitutions (in case ?x=f(?y) and later
	// add in ?y=?z)
	public static HashMap unify(PNode p1, PNode p2) {
		return unify(p1, p2, false, false);
	}
		
	public static HashMap unify(PNode p1, PNode p2, boolean ignore_predname, boolean ignore_overlap) {

		// First ensure same predicate name and arity
		if ((!ignore_predname && p1._nPredID != FOPC.PNode.INVALID && p1._nPredID != p2._nPredID)
		 || (!ignore_predname && p1._nPredID == FOPC.PNode.INVALID && !p1._sPredName.equals(p2._sPredName)) 
		 || p1._nArity != p2._nArity) {
			return null;
		}

		// Do we match?
		HashMap theta = new HashMap();
		if (p1.equals(p2)) {
			return theta;
		}

		// Now perform unification
		p1 = (PNode) p1.copy();
		p2 = (PNode) p2.copy();
		p1.setFreeVars();
		p2.setFreeVars();

		// Determine if we need to standardize apart (i.e., shared variables)
		if (!ignore_overlap) {
			boolean share_vars = false;
			Iterator it = p1._hsFreeVarsOut.iterator();
			while (it.hasNext() && !share_vars) {
				share_vars = p2._hsFreeVarsOut.contains(it.next());
			}
			if (share_vars) {
				System.out
						.println("WARNING: Unification nodes have overlapping variables!");
				System.out.println("Node 1: " + p1.toFOLString());
				System.out.println("Node 2: " + p2.toFOLString());
				try {throw new Exception("");} catch (Exception e) { e.printStackTrace(); };
			}
		}

		// Proceed until mismatch found, unify and continue if not failure
		for (int i = 0; i < p1._nArity; i++) {
			Term st1 = (Term) p1._alTermBinding.get(i);
			Term st2 = (Term) p2._alTermBinding.get(i);

			// Continue unifying until failure or equal
			while (!st1.equals(st2)) {
				HashMap theta2 = unify(st1, st2);
				if (theta2 == null) {
					return null;
				} else {
					if (theta2.isEmpty()) {
						System.out.println("Error in unification!");
						System.exit(1);
					}
					p1.substitute(theta2);
					p2.substitute(theta2);
					union(theta, theta2);
					st1 = (Term) p1._alTermBinding.get(i);
					st2 = (Term) p2._alTermBinding.get(i);
				}
			}
		}

		// Return all unifications needed to perform unify(p1,p2)
		return theta;
	}

	// Unify the first two mismatching terms
	public static HashMap unify(Term t1, Term t2) {

		HashMap theta = new HashMap();
		if (t1.equals(t2)) {
			return new HashMap();
		} else if (t1 instanceof TVar) {
			if (occurs((TVar) t1, t2)) {
				return null;
			} else {
				theta.put(t1, t2);
				return theta;
			}
		} else if (t2 instanceof TVar) {
			if (occurs((TVar) t2, t1)) {
				return null;
			} else {
				theta.put(t2, t1);
				return theta;
			}
		} else if (t1 instanceof TFunction && t2 instanceof TFunction) {

			TFunction f1 = (TFunction) t1;
			TFunction f2 = (TFunction) t2;
			if (!f1._sFunName.equals(f2._sFunName) || f1._nArity != f2._nArity) {
				return null;
			}

			for (int i = 0; i < f1._nArity; i++) {
				Term st1 = (Term) f1._alTermBinding.get(i);
				Term st2 = (Term) f2._alTermBinding.get(i);

				// Proceed until mismatch
				if (!st1.equals(st2)) {
					return unify(st1, st2);
				}
			}
			return new HashMap();
		} else {
			return null;
		}
	}

	// Will update theta1 with entries in theta2
	// ... not subbing in theta1 to theta2 because we assume
	// theta2 is acting with theta1 subs already made (e.g. in unify)
	public static HashMap _temp_sub = new HashMap();

	public static synchronized void union(HashMap theta1, HashMap theta2) {

		// Add theta2 to theta1 by adding in theta2 one at a time
		// and updating theta1 if necessary
		Iterator i2 = theta2.entrySet().iterator();
		while (i2.hasNext()) {
			Map.Entry sub2 = (Map.Entry) i2.next();

			// Do this one by one in case a theta has {?x=?y,?y=?x} to
			// get all substitutions
			_temp_sub.clear();
			_temp_sub.put(sub2.getKey(), sub2.getValue());

			// Now, go through all theta1 making substitution if necessary
			Iterator i1 = ((HashMap) theta1.clone()).entrySet().iterator();
			while (i1.hasNext()) {
				Map.Entry sub1 = (Map.Entry) i1.next();
				theta1.put(sub1.getKey(), ((FOPC.Term) sub1.getValue())
						.substitute(_temp_sub));
			}
		}

		// Now ok to put together
		theta1.putAll(theta2);
	}

	public static boolean occurs(TVar v, Term sub) {
		return sub.collectVars().contains(v);
	}

	// Standardize variables apart (substitute all vars with *new* vars)
	// (Will modify original formula!)
	// **Synchronized because relies on var count.
	public static synchronized HashMap standardizeApartNode(Node n) {

		// Standardize apart vars in subnode
		HashMap subst = new HashMap();
		n.setFreeVars();
		Iterator j = n._hsFreeVarsOut.iterator();
		while (j.hasNext()) {
			FOPC.TVar v = (FOPC.TVar) j.next();
			subst.put(v, new FOPC.TVar());
		}

		// Now update this clause
		return subst;
	}

	// Standardize variables apart (substitute all vars with *new* vars)
	// (Will modify original formula!)
	// ... this version for ConnNodes specifically
	// **Synchronized because relies on var count.
	public static synchronized HashMap standardizeApartClause(ConnNode n) {

		// Standardize apart vars in subnode
		HashMap subst = new HashMap();
		n.setFreeVars();
		Iterator i = n._alSubNodes.iterator();
		while (i.hasNext()) {
			PNode p = (PNode) i.next();
			Iterator j = p._alTermBinding.iterator();
			while (j.hasNext()) {
				Term t = (Term) j.next();
				Set vars = t.collectVars();
				Iterator k = vars.iterator();
				while (k.hasNext()) {
					TVar v = (TVar) k.next();
					if (!subst.containsKey(v)) { // Don't overwrite subst!
						subst.put(v, new FOPC.TVar());
					}
				}
			}
		}

		// if (!n._hsFreeVarsOut.equals(subst.keySet())) {
		// System.out.println("Standardize apart error: ");
		// System.out.println(" - " + n._hsFreeVarsOut);
		// System.out.println(" - " + subst.keySet());
		// System.exit(1);
		// }

		// Now update this clause
		return subst;
	}

	// This modifies original formula
	// **Synchronized because relies on var count.
	public static synchronized void renameAllVars(Node n) {

		if (n instanceof ConnNode) {
			ConnNode c = (ConnNode) n;
			for (int index = 0; index < c._alSubNodes.size(); index++) {
				renameAllVars((Node) c._alSubNodes.get(index));
			}
			return;
		} else if (n instanceof QNode) {
			QNode q = (QNode) n;
			renameAllVars(q._subnode);
			HashMap subs = new HashMap();
			for (int index = 0; index < q._alQuantBinding.size(); index++) {
				TVar var = (TVar) q._alQuantBinding.get(index);
				FOPC.TVar var_repl = new FOPC.TVar();
				subs.put(var, var_repl);
				q._alQuantBinding.set(index, var_repl);
			}
			q._subnode.substitute(subs);
			return;
		} else if (n instanceof TNode || n instanceof PNode) {
			return;
		} else {
			System.out.println("Invalid node for skolemization");
			System.exit(1);
			return;
		}
	}

	// Skolemize formula - creates a copy
	public static Node skolemize(Node n) {
		n = n.copy();
		renameAllVars(n);
		// System.out.println("\nSkolemizing: " + n.toFOLString() + "\n");
		Node sk = skolemize(n, new HashSet(), new HashMap());
		// System.out.println("\nResult: " + sk.toFOLString() + "\n");
		sk.setFreeVars();
		return sk;
	}

	// Performs skolemization in place
	public static Node skolemize(Node n, Set forall_ctxt, Map exists_ctxt) {

		if (n instanceof ConnNode) {
			ConnNode tc = (ConnNode) n;
			ConnNode nc = new ConnNode(tc._nType);
			Iterator i = tc._alSubNodes.iterator();
			while (i.hasNext()) {
				nc.addSubNode(skolemize((Node) i.next(), forall_ctxt,
						exists_ctxt));
			}
			return nc;
		} else if (n instanceof QNode) {
			QNode q = (QNode) n;

			// Add vars to context
			for (int index = 0; index < q._alQuantType.size(); index++) {
				Integer type = (Integer) q._alQuantType.get(index);
				TVar var = (TVar) q._alQuantBinding.get(index);
				if (type.intValue() == QNode.EXISTS) {
					exists_ctxt.put(var, new TFunction(new ArrayList(
							forall_ctxt)));
				} else if (type.intValue() == QNode.FORALL) {
					if (forall_ctxt.contains(var)) {
						// System.out.println("\nOverlapping vars: " + var + ",
						// replacing.\n");

						// Standardize apart var since forall already contains
						// it
						HashMap subs = new HashMap();
						FOPC.TVar var_repl = new FOPC.TVar();
						subs.put(var, var_repl);
						q._subnode = q._subnode.substitute(subs);
						q._alQuantBinding.set(index, var_repl);
						var = var_repl;
					}
					forall_ctxt.add(var);
				} // Do nothing if free
			}

			// Skolemize *subnode*
			Node sk = skolemize(q._subnode, forall_ctxt, exists_ctxt);

			// Remove vars from context
			Iterator types = q._alQuantType.iterator();
			Iterator vars = q._alQuantBinding.iterator();
			while (types.hasNext()) {
				Integer type = (Integer) types.next();
				TVar var = (TVar) vars.next();
				if (type.intValue() == QNode.EXISTS) {
					exists_ctxt.remove(var);
				} else if (type.intValue() == QNode.FORALL) {
					forall_ctxt.remove(var);
				} // Do nothing if free
			}

			// Return subnode
			return sk;
		} else if (n instanceof PNode) {
			PNode newp = (PNode) n.copy();
			ArrayList new_term_binding = new ArrayList();
			Iterator i = newp._alTermBinding.iterator();
			while (i.hasNext()) {
				Term t2 = (Term) i.next();
				new_term_binding.add(skolemize(t2, forall_ctxt, exists_ctxt));
			}
			newp._alTermBinding = new_term_binding;
			return newp;
		} else if (n instanceof TNode) {
			return n.copy();
		} else {
			System.out.println("Invalid node for skolemization");
			System.exit(1);
			return null;
		}
	}

	public static Term skolemize(Term t, Set forall_ctxt, Map exists_ctxt) {

		if (t instanceof TInteger) {
			return t;
		} else if (t instanceof TScalar) {
			return t;
		} else if (t instanceof TVar) {
			TVar v = (TVar) t;
			if (exists_ctxt.containsKey(v)) {
				return (TFunction) exists_ctxt.get(v);
			} else { // FORALL or free var
				return v;
			}
		} else if (t instanceof TFunction) {
			TFunction f = (TFunction) t;
			ArrayList new_term_binding = new ArrayList();
			Iterator i = f._alTermBinding.iterator();
			while (i.hasNext()) {
				Term t2 = (Term) i.next();
				new_term_binding.add(skolemize(t2, forall_ctxt, exists_ctxt));
			}
			return new TFunction(f._sFunName, new_term_binding);
		} else {
			System.out.println("Invalid term for skolemization");
			System.exit(1);
			return null;
		}
	}

	// ////////////////////////////////////////////////////////////////////
	// Internal FOL Structures for FOPC
	// ////////////////////////////////////////////////////////////////////

	// If a ParseException is thrown, just returns null
	public static synchronized Node parse(String s) {
		try {
			Node head = parser.parse(s);
			return head;
		} catch (ParseException p) {
			System.out.println(p);
			return null;
		}
	}

	public static synchronized ArrayList parseFile(String filename) {
		
		ArrayList ret = new ArrayList();
		String line = null;
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
		    while ((line = in.readLine()) != null) {
				ret.add(FOPC.parse(line));
		    }
		    in.close();
			return ret;
		} catch (IOException e) {
			System.err.println(e);
			return null;
		}
	}
	
	// ====================================================================
	// Formula Node Objects
	// ====================================================================

	// This is the root Node
	public static abstract class Node {

		public HashSet _hsFreeVarsIn;

		public HashSet _hsFreeVarsOut;

		public Node() {
			_hsFreeVarsIn = null;
			_hsFreeVarsOut = null;
		}

		/*
		 * By default, toKIFString() should be KIF (for JTP), but this version
		 * can be user defined (e.g. FOPC)
		 */
		public abstract String toKIFString();

		public abstract String toFOLString();

		public String toOtterString() {
			boolean USE_OTTER_PREV = USE_OTTER;
			USE_OTTER = true;
			String ret = toFOLString();
			USE_OTTER = USE_OTTER_PREV;
			return ret;
		}

		public String toVampireString() {
			boolean USE_VAMPIRE_PREV = USE_VAMPIRE;
			USE_VAMPIRE = true;
			String ret = toFOLString();
			USE_VAMPIRE = USE_VAMPIRE_PREV;
			return ret;
		}

		public String toTPTPString() {
			boolean USE_TPTP_PREV = USE_TPTP;
			USE_TPTP = true;
			String ret = toFOLString();
			USE_TPTP = USE_TPTP_PREV;
			return ret;
		}

		/* Methods required for all nodes */

		/*
		 * Obvious effect - negations will be on PNodes when done, no NNodes,
		 * IMPLY, EQUIV will be used, only AND/OR
		 */
		public abstract Node convertNNF(boolean negate);

		/* Set the free variables at each node */
		public abstract HashSet setFreeVars();

		/* Push all !E through | and !A through ^ on an NNF formula */
		public abstract Node pushDownQuant();

		/* Simplify equality expressions */
		public abstract Node simplifyEquality();

		/* Remove redundancy among subformulae */
		public abstract SimpResult removeRedundancy(ArrayList parinfo);

		/* Substitute vars in the HashMap, QNode safe :) */
		public abstract Node substitute(HashMap subs);

		/*
		 * Regress a formula using the provided mapping - assumes already in
		 * NNF, but can change the current formula
		 */
		public abstract Node regress(HashMap regr_map);

		/* For recursively copying an object */
		public abstract Node copy();

		/*
		 * Remaps any String-based vars to ID-based vars - ensures variable
		 * names are canonical @param str2tvar - Current map of String-ID
		 * entries @param start_zero - Whether to start var IDs at zero, or with
		 * next available global var
		 */
		public abstract void setVarID(HashMap str2tvar, boolean start_zero);

		/* Will assume we want to see the FOL String for now */
		public String toString() {
			return toFOLString();
		}
	}

	/**
	 * Useful when keeping pairs of nodes, e.g. caching resolutions.
	 * 
	 */
	public static class NodePair {
		public FOPC.Node _n1;

		public FOPC.Node _n2;

		public NodePair(FOPC.Node n1, FOPC.Node n2) {
			_n1 = n1;
			_n2 = n2;
		}

		public int hashCode() {
			return _n1.hashCode() - (_n2.hashCode() << 1);
		}

		public boolean equals(Object o) {
			if (o instanceof NodePair) {
				NodePair p = (NodePair) o;
				// return (_n1 == p._n1 && _n2 == p._n2); // Actually slower
				// with this line!
				return (_n1.equals(p._n1) && _n2.equals(p._n2));
			} else {
				return false;
			}
		}

		public String toString() {
			return "[ PAIR: " + _n1.toFOLString() + " ; " + _n2.toFOLString()
					+ " ]";
		}
	}

	// --------------------------------------------------------------------
	// Quantified Node
	// --------------------------------------------------------------------

	// This is the formula taxonomy (only interfaces which need to
	// be instantiated!)
	public static class QNode extends Node {

		public final static int INVALID = 0;

		public final static int EXISTS = 1;

		public final static int FORALL = 2;

		public final static int COUNT = 3;

		public ArrayList _alQuantSort;

		public ArrayList _alQuantType;

		public ArrayList _alQuantBinding;

		public Node _subnode;

		public QNode() {
			_alQuantSort = null;
			_alQuantType = null;
			_alQuantBinding = null;
			_hsFreeVarsIn = null;
			_hsFreeVarsOut = null;
			_subnode = null;

			// May catch more bugs without...
			// _hsFreeVarsIn = new HashSet();
			// _hsFreeVarsOut = new HashSet();
		}

		public QNode(Node n) {
			_subnode = n;
			_alQuantSort = new ArrayList();
			_alQuantType = new ArrayList();
			_alQuantBinding = new ArrayList();
			if (((Node) n)._hsFreeVarsIn != null) {
				_hsFreeVarsIn = (HashSet) ((FOPC.Node) n)._hsFreeVarsIn.clone();
			} else {
				_hsFreeVarsIn = new HashSet();
			}
			if (((Node) n)._hsFreeVarsOut != null) {
				_hsFreeVarsOut = (HashSet) ((FOPC.Node) n)._hsFreeVarsOut
						.clone();
			} else {
				_hsFreeVarsOut = new HashSet();
			}
		}

		public QNode(Quant q, Node n) {
			_subnode = n;
			_alQuantSort = new ArrayList();
			_alQuantType = new ArrayList();
			_alQuantBinding = new ArrayList();
			_alQuantSort.addAll(q._alQuantSort);
			_alQuantType.addAll(q._alQuantType);
			_alQuantBinding.addAll(q._alQuantBinding);
			if (((Node) n)._hsFreeVarsIn != null) {
				_hsFreeVarsIn = (HashSet) ((FOPC.Node) n)._hsFreeVarsIn.clone();
			} else {
				_hsFreeVarsIn = new HashSet();
			}
			if (((Node) n)._hsFreeVarsOut != null) {
				_hsFreeVarsOut = (HashSet) ((FOPC.Node) n)._hsFreeVarsOut
						.clone();
			} else {
				_hsFreeVarsOut = new HashSet();
			}
		}

		public Node copy() {
			QNode q = new QNode();
			q._alQuantSort = (ArrayList) _alQuantSort.clone();
			q._alQuantType = (ArrayList) _alQuantType.clone();
			q._alQuantBinding = (ArrayList) _alQuantBinding.clone();
			q._hsFreeVarsIn = (HashSet) _hsFreeVarsIn.clone();
			q._hsFreeVarsOut = (HashSet) _hsFreeVarsOut.clone();
			if (_subnode != null) {
				q._subnode = (Node) _subnode.copy();
			}
			return q;
		}

		public int hashCode() {
			int h = 0;
			int num_terms = _alQuantType.size();
			for (int i = 0; i < num_terms; i++) {
				h += _alQuantType.get(i).hashCode();
				h -= _alQuantBinding.get(i).hashCode();
			}
			return h;
		}

		public boolean equals(Object o) {
			if (o instanceof QNode) {
				QNode q = (QNode) o;
				int num_terms = _alQuantType.size();
				if (num_terms != _alQuantType.size()) {
					return false;
				}
				for (int i = 0; i < num_terms; i++) {
					if (!_alQuantSort.get(i).equals(q._alQuantSort.get(i))
							|| !_alQuantType.get(i).equals(
									q._alQuantType.get(i))
							|| !_alQuantBinding.get(i).equals(
									q._alQuantBinding.get(i))) {
						return false;
					}
				}
				return true;
			} else {
				return false;
			}
		}

		public Node getSubNode() {
			return _subnode;
		}

		/** TODO: Sorts * */

		public void addQuantifier(int type, TVar var) {
			_alQuantSort.add("Top");
			_alQuantType.add(new Integer(type));
			_alQuantBinding.add(var);
		}

		public void insertFront(int type, TVar var) {
			_alQuantSort.add("Top");
			_alQuantType.add(0, new Integer(type));
			_alQuantBinding.add(0, var);
		}

		public int getNumQuantifiers() {
			return _alQuantType.size();
		}

		public String getQuantSort(int index) {
			return (String) _alQuantSort.get(index);
		}

		public int getQuantType(int index) {
			return ((Integer) _alQuantType.get(index)).intValue();
		}

		public TVar getQuantVar(int index) {
			return (TVar) _alQuantBinding.get(index);
		}

		/*
		 * Obvious effect - negations will be on PNodes when done, no NNodes,
		 * IMPLY, EQUIV will be used, only AND/OR
		 */
		public Node convertNNF(boolean negate) {
			if (negate) {
				for (int i = 0; i < _alQuantType.size(); i++) {
					int type = ((Integer) _alQuantType.get(i)).intValue();
					_alQuantType.set(i, (type == EXISTS) ? new Integer(FORALL)
							: new Integer(EXISTS));
				}
			}

			// Recursively apply NNF
			_subnode = _subnode.convertNNF(negate);

			// If subnode is QNode, collapse
			while (_subnode instanceof FOPC.QNode) {
				FOPC.QNode sq = (FOPC.QNode) _subnode;
				int num_quant = sq.getNumQuantifiers();
				for (int i = 0; i < num_quant; i++) {
					addQuantifier(sq.getQuantType(i), sq.getQuantVar(i));
				}
				_subnode = sq._subnode;
			}

			// Also take into account that no QNode under this and no
			// quantifiers in this node
			if (getNumQuantifiers() <= 0) {
				return _subnode;
			} else {
				return this; // No simplification
			}
		}

		// If the subnode is a connective, return true if the
		// TVar is only free in the out-sets of one of the elements
		public int countVarUse(TVar v) {

			if (_subnode instanceof FOPC.ConnNode) {

				int used = 0;
				FOPC.ConnNode c = (FOPC.ConnNode) _subnode;
				for (int j = 0; j < c._alSubNodes.size(); j++) {
					FOPC.Node s = (FOPC.Node) c._alSubNodes.get(j);
					if (s._hsFreeVarsOut == null) {
						System.out.println("Null subnode: " + s.toFOLString());
					}
					if (s._hsFreeVarsOut.contains(v)) {
						// System.out.println(" -- used: " + s);
						used++;
					}
				}

				return used;

			} else {
				return Integer.MAX_VALUE;
			}

		}

		/***********************************************************************
		 * If last quant type cannot be directly pushed through ConnNode then
		 * subdivide subnodes based on those with and without the free var --
		 * requires formula to be in NNF and have free vars set.
		 **********************************************************************/
		public void subdivideConnNodes() {

			if (!(_subnode instanceof ConnNode)) {
				return;
			}

			ConnNode c = (ConnNode) _subnode; // free vars assumed to be set!
			int num_quant = _alQuantType.size();
			Integer last_quant_type = (Integer) _alQuantType.get(num_quant - 1);
			if (last_quant_type.intValue() == ((c._nType == ConnNode.AND) ? FORALL
					: EXISTS)) {
				return; // Can already push down quant, no need to subdivide
			}

			// Divide up subnodes and put them in their own ConnNodes
			TVar qvar = (TVar) _alQuantBinding.get(num_quant - 1);
			ConnNode c_with = new ConnNode(c._nType);
			ConnNode c_without = new ConnNode(c._nType);
			for (int i = 0; i < c._alSubNodes.size(); i++) {
				Node n = (Node) c._alSubNodes.get(i);
				if (n._hsFreeVarsOut.contains(qvar)) {
					c_with.addSubNode(n);
				} else {
					c_without.addSubNode(n);
				}
			}
			c_with.setFreeVars();
			c_without.setFreeVars();

			// System.out.println("C_WITH: " + c_with._hsFreeVarsOut + " : " +
			// c_with);
			// System.out.println("C_WITHOUT: " + c_without._hsFreeVarsOut + " :
			// " + c_without);

			// Replace the subnode if both c_with and c_without are non-empty
			if (c_with._alSubNodes.size() != 0
					&& c_without._alSubNodes.size() != 0) {
				c._alSubNodes.clear();
				c._alSubNodes.add(c_with);
				c._alSubNodes.add(c_without);
				c.setFreeVars();
			}

			// System.out.println("C: " + c._hsFreeVarsOut + " : " + c + "\n");
		}

		/*
		 * Reorder quantifiers so that the least used come first. Assuming for
		 * reordering purposes that the node below this must be a connective
		 * node of type AND/OR.
		 */
		public void optimalQuantReorder() {

			// Build parallel array of counts
			int num_vars = _alQuantType.size();
			int[] sz = new int[_alQuantBinding.size()];
			int i;
			for (i = 0; i < num_vars; i++) {
				sz[i] = countVarUse((TVar) _alQuantBinding.get(i));
			}

			// To verify, show input
			// System.out.println("In quant: " + _alQuantType);
			// System.out.println("In bind: " + _alQuantBinding);
			// System.out.print( "In sz: [ ");
			// for (i = 0; i < num_vars; i++) { System.out.print(sz[i] + " "); }
			// System.out.println("]");

			// Starting from end, look for first quant <= 1 and
			// rearrange quantifiers within boundaries so that <=1
			// used vars go last (best chance of being pushed down).
			for (i = num_vars - 1; i >= 0; i--) {

				// Starting at current i, set j to the greatest
				// index which has val > 1, if none, j will exit
				// with -1.
				int j;
				for (j = i; j >= 0 && sz[j] <= 1; j--)
					;

				if (j >= 0) {

					// REMOVE
					if (sz[j] <= 1) {
						System.out
								.println("ERROR in optimalQuantReorder... exiting");
						System.exit(1);
					}

					// Now, go back within contiguous quantifier, looking for
					// a value <= 1 to swap with. If not found, set next j = k.
					int quant = ((Integer) _alQuantType.get(j)).intValue();
					int k = j;
					boolean contiguous;
					do {
						--k;
						contiguous = (k >= 0) ? (((Integer) _alQuantType.get(k))
								.intValue() == quant)
								: false;

					} while (contiguous && sz[k] > 1); // will short-circuit if
														// k < 0

					// If not contiguous, set j = k (no more vals less than 1)
					if (!contiguous) {
						j = k + 1; // Will decrement at top of loop
						continue;
					}

					// REMOVE
					if (sz[k] > 1) {
						System.out
								.println("ERROR in optimalQuantReorder... exiting");
						System.exit(1);
					}

					// Contiguous ( => k > 0 as well), so swap j with k
					Object quant_temp = _alQuantType.get(j);
					Object binding_temp = _alQuantBinding.get(j);
					int sz_temp = sz[j];
					_alQuantType.set(j, _alQuantType.get(k));
					_alQuantBinding.set(j, _alQuantBinding.get(k));
					sz[j] = sz[k];
					_alQuantType.set(k, quant_temp);
					_alQuantBinding.set(k, binding_temp);
					sz[k] = sz_temp;

					// j resumes where it is
				}

				i = j; // Set i to next previous index from swap (or -1
				// if at end)
			}

			// To verify, print out final
			// System.out.println("Out quant: " + _alQuantType);
			// System.out.println("Out bind: " + _alQuantBinding);
			// System.out.print( "Out sz: [ ");
			// for (i = 0; i < num_vars; i++) { System.out.print(sz[i] + " "); }
			// System.out.println("]\n");
		}

		/* Push all !E through | and !A through ^ on an NNF formula */
		public Node pushDownQuant() {

			// if (fodt.foalp.Case.PUSHDOWN) {
			// fodt.foalp.Case.ps_comp.println("Starting pushdown");
			// }

			// System.out.println("Entering QNode pushDownQuant: " +
			// toFOLString());

			// If there are quantifiers of variables that are not in
			// the free-var-in set, they can be removed (they do nothing!)
			setFreeVars();
			for (int i = 0; i < _alQuantType.size(); i++) {
				if (!_hsFreeVarsIn.contains(_alQuantBinding.get(i))) {
					// if (fodt.foalp.Case.PUSHDOWN) {
					// fodt.foalp.Case.ps_comp.println("Removing: " +
					// _alQuantBinding.get(i) + ", does not occur in " +
					// _hsFreeVarsIn);
					// }
					_alQuantType.remove(i);
					_alQuantBinding.remove(i);
					// System.out.println("Removing unused var: " + i);
				}
			}

			// If QNode empty, just return pushDownQuant of subnode
			if (_alQuantType.isEmpty()) {
				return _subnode.pushDownQuant();
			}

			// TODO: Need to subdivide subnodes and separate those with free
			// vars for last
			// quantified var and those lacking free vars.

			// Now see if quantifiers can be pushed down
			if (_subnode instanceof FOPC.ConnNode) {
				FOPC.ConnNode c = (FOPC.ConnNode) _subnode;
				if (c._nType == ConnNode.AND || c._nType == ConnNode.OR) {

					// First, let's reorder vars to maximize chance of pushing
					// down
					optimalQuantReorder();

					// Now, let's subdivide sub-ConnNodes to separate those with
					// and without free var
					subdivideConnNodes();

					// Now, we have an AND/OR Node below this QNode...
					// Can we push down any quantifiers? If so,
					// ensure that a QNode below each element of c
					int num_quant = _alQuantType.size();
					if (num_quant == 0) {
						System.out.println("\nERROR: Empty QNode: "
								+ this.toFOLString());

						// Since we are empty, just return pushDown of node
						// below us
						return c.pushDownQuant();
					}

					// If we can push-down the last var, make sure a QNode
					// beneath
					// every ConnNode child
					boolean isAND = (c._nType == ConnNode.AND);

					boolean lastQuantPushThroughConn = ((Integer) _alQuantType
							.get(num_quant - 1)).intValue() == (isAND ? FORALL
							: EXISTS);

					int var_use = countVarUse((TVar) _alQuantBinding
							.get(num_quant - 1));
					boolean lastQuantUsedMaxOnce = var_use <= 1;
					// System.out.println("Var use [" +
					// (TVar)_alQuantBinding.get(num_quant - 1) + "]: " +
					// var_use + " : " + this + "\n\n");

					// TODO: Not doing CNF Convestion for now!!!!!
					// ***************
					if (false && !lastQuantPushThroughConn
							&& !lastQuantUsedMaxOnce) {

						// System.out.println("Trying " + (isAND ? "DNF":"CNF")
						// + " conversion");
						// If used once, we can always push it down. If used
						// more than
						// once, but connective/quantifier mismatch, then we can
						// attempt
						// a DNF/CNF conversion based on the connective.
						_subnode = c = (ConnNode) (isAND ? (ALLOW_DNF ? convertDNF(c)
								: c)
								: convertCNF(c));
						isAND = (c._nType == ConnNode.AND);
						lastQuantPushThroughConn = ((Integer) _alQuantType
								.get(num_quant - 1)).intValue() == (isAND ? FORALL
								: EXISTS);
						// System.out.println("Result: " + this.toFOLString());
					}

					// If can perform pushdown, make sure Qnodes below ConnNodes
					if (lastQuantPushThroughConn || lastQuantUsedMaxOnce) {

						// Ensure QNode below every subnode of c
						for (int j = 0; j < c._alSubNodes.size(); j++) {
							Node s = (Node) c._alSubNodes.get(j);
							if (!(s instanceof QNode)) {
								FOPC.QNode q = new FOPC.QNode(s);
								c._alSubNodes.set(j, q);
							}
						}

					} else {

						// Last quantifier could not be pushed down.
						// Continue with rest of structure (for purposes of
						// canonical form).
						_subnode = _subnode.pushDownQuant();
						return this;
					}

					// Next go through all quantifiers/vars from end to
					// beginning and attempt to push down. Halt when at end
					// or first EXISTS-AND/FORALL-OR reached.
					for (int k = num_quant - 1; k >= 0; k--) {
						int type = ((Integer) _alQuantType.get(k)).intValue();
						if ((type == (isAND ? QNode.FORALL : QNode.EXISTS))
								|| (countVarUse((TVar) _alQuantBinding.get(k)) <= 1)) {

							TVar var = (TVar) _alQuantBinding.get(k);
							_alQuantType.remove(k);
							_alQuantBinding.remove(k);

							// if (fodt.foalp.Case.PUSHDOWN) {
							// fodt.foalp.Case.ps_comp.println("Pushing down: "
							// + var + ", " + this);
							// fodt.foalp.Case.ps_comp.flush();
							// }

							for (int m = 0; m < c._alSubNodes.size(); m++) {
								QNode q = (QNode) c._alSubNodes.get(m);
								q.insertFront(type, var);
							}

						} else {
							break;
						}
					}

					// Set new free vars
					_subnode.setFreeVars();

					// Finally return
					if (_alQuantType.isEmpty()) {
						Node ret = _subnode.pushDownQuant();
						// System.out.println("Returning: " +
						// ret.toFOLString());
						return ret;
					} else {
						// Need to keep these quantifiers around
						_subnode = _subnode.pushDownQuant();
						// System.out.println("Returning: " + _subnode);
						return this;
					}

				} else {

					System.out
							.println("Illegal non-AND/OR node below QNode for push down..."
									+ " in NNF form?");
					System.exit(1);
					return null;
				}

			} else {

				// Not a connective node below this QNode... verify that it is
				// PNode or TNode... if not, error in algorithm or NNF
				// flattening
				if ((_subnode instanceof PNode) || (_subnode instanceof TNode)) {

					// No need to push down further
					return this;
				} else {

					System.out
							.println("Illegal node below QNode for push down... in NNF form?");
					System.exit(1);
					return null;
				}
			}

		}

		/* Set the free variables at each node */
		public HashSet setFreeVars() {

			// First recurse
			HashSet free_vars = _subnode.setFreeVars();

			// Save free var out set
			_hsFreeVarsIn = (HashSet) free_vars.clone();

			// Remove vars quantified here and pass back
			Iterator qvars = _alQuantBinding.iterator();
			while (qvars.hasNext()) {
				TVar var = (TVar) qvars.next();
				if (free_vars.contains(var)) {
					free_vars.remove(var);
				}
			}

			// Save free var out set
			_hsFreeVarsOut = (HashSet) free_vars.clone();

			return free_vars;
		}

		/* Simplify equality expressions */
		public Node simplifyEquality() {

			// If subnode is Litnode/Equality and one of free vars is
			// quantified EXISTS or Inequality and FORALL
			if (_subnode instanceof PNode) {
				PNode l = (PNode) _subnode;

				if (l._nPredID == PNode.EQUALS) {

					Term lhs = (Term) l._alTermBinding.get(0);
					Term rhs = (Term) l._alTermBinding.get(1);

					// _bIsNegated => inequality
					for (int i = 0; i < _alQuantType.size(); i++) {

						if (((Integer) _alQuantType.get(i)).intValue() == (l._bIsNegated ? FORALL
								: EXISTS)) {

							// OK, PNode and quantifier match
							if (((Term) _alQuantBinding.get(i)).equals(lhs)
									|| ((Term) _alQuantBinding.get(i))
											.equals(rhs)) {

								return (l._bIsNegated ? new TNode(false)
										: new TNode(true));
							}
						}
					}
				}
			}

			// Otherwise, cannot simplify so continue
			_subnode = _subnode.simplifyEquality();
			return this;
		}

		/* Remove redundancy among subformulae (for QNode) */
		public SimpResult removeRedundancy(ArrayList parinfo) {

			// System.out.println("RR-QNode: " + toFOLString());

			// Recursively call
			parinfo.add(this);
			SimpResult r = _subnode.removeRedundancy(parinfo);
			_subnode = r._result;

			// Remove any blocked unifications
			HashMap unify_map = r._hmUnifyMap;
			Iterator i = _alQuantBinding.iterator();
			while (i.hasNext()) {

				// Get the next quantified var
				TVar v = (TVar) i.next();
				if (unify_map.get(v) != null) {

					// Mapped var is quantified here, so can't unify above it
					unify_map.remove(v);
				}
			}

			// Remove the info added to the parinfo "stack"
			parinfo.remove(parinfo.size() - 1);

			// Return the updated SimpResult
			return new SimpResult(this, SimpResult.UNKNOWN, unify_map);
		}

		/* Substitute vars in the HashMap */
		public Node substitute(HashMap subs) {

			HashMap temp_subs = new HashMap(subs);

			Iterator i = _alQuantBinding.iterator();
			while (i.hasNext()) {
				Term t = ((Term) i.next());
				if (t instanceof TVar) {
					if (subs.keySet().contains(((TVar) t)._sName)) {
						temp_subs.remove(((TVar) t)._sName);
					}
				}
			}
			_subnode = _subnode.substitute(temp_subs);

			return this;
		}

		/* Regress a formula using the provided mapping */
		public Node regress(HashMap regr_map) {

			// Cannot regress quantifiers
			_subnode = _subnode.regress(regr_map);

			return this;
		}

		public void setVarID(HashMap str2tvar, boolean start_zero) {
			Iterator i = _alQuantBinding.iterator();
			while (i.hasNext()) {
				((Term) i.next()).setVarID(str2tvar, start_zero);
			}

			_subnode.setVarID(str2tvar, start_zero);
		}

		public String toKIFString() {

			// Need to wrap in quantifiers
			StringBuffer sb = new StringBuffer();
			int i, s = getNumQuantifiers();
			for (i = 0; i < s; i++) {
				int qtype = getQuantType(i);
				if (qtype != QNode.EXISTS && qtype != QNode.FORALL) {
					return "*INVALID QUANT TYPE [" + i + "]*";
				}

				// ToString should take var_id n and return "?v-n"
				String var = getQuantVar(i).toKIFString();
				sb.append("(");
				sb.append((qtype == QNode.EXISTS) ? "exists" : "forall");
				sb.append(" (" + var + ") ");
			}

			// Insert internal node
			sb.append(_subnode.toKIFString());

			// Insert closing quantifiers
			for (i = 0; i < s; i++) {
				sb.append(")");
			}

			return sb.toString();
		}

		public String toFOLString() {

			StringBuffer sb = new StringBuffer("(");

			// REMOVE for general purposes, just for testing
			// if (!_hsFreeVars.isEmpty()) {
			// sb.append("%FV: " + _hsFreeVars.toString() + "% ");
			// }

			// First need to wrap in quantifiers and then
			// insert (and/or with each subnode recursively
			// evaluated).
			int i, s = getNumQuantifiers();
			if (s <= 0) {
				sb.append("[QNODE EMPTY]");
			}
			for (i = 0; i < s; i++) {
				int qtype = getQuantType(i);
				String qsort = getQuantSort(i);
				if (qtype != QNode.EXISTS && qtype != QNode.FORALL
						&& qtype != QNode.COUNT) {
					return "*INVALID QUANT TYPE [" + i + "]*";
				}
				// ToString should take var_id n and return "?v-n"
				String var = getQuantVar(i).toFOLString();
				if (USE_OTTER) {
					sb.append((qtype == QNode.EXISTS) ? "exists "
							: (qtype == QNode.FORALL ? "all "
									: "*INVALID QUANT TYPE [" + i + "]*"));
				} else if (USE_VAMPIRE) {
					System.out.println("No quantifiers in VAMPIRE!");
					System.exit(1);
				} else if (USE_TPTP) {
					sb.append((qtype == QNode.EXISTS) ? "? ["
							: (qtype == QNode.FORALL ? "! ["
									: "*INVALID QUANT TYPE [" + i + "]*"));
				} else {
					sb.append((qtype == QNode.EXISTS) ? "!E"
							: (qtype == QNode.FORALL ? "!A" : "#"));
				}
				// sb.append("." + var + " ");
				if (qsort.equals("Top")) {
					sb.append(" " + var + " ");
				} else {
					if (USE_OTTER || USE_VAMPIRE || USE_TPTP) {
						System.out
								.println("ERROR: Sorts in quantifiers, cannot export!");
						System.exit(1);
					}
					sb.append(" " + var + ":" + qsort + " ");
				}

				if (USE_TPTP) {
					sb.append("] : ");
				}
			}

			// Insert internal node
			sb.append(_subnode.toFOLString());

			// Insert closing quantifier
			sb.append(")");

			return sb.toString();
		}
	}

	/** Note: We'll have to convert the Strings to TVars later */
	public static class Quant {

		public ArrayList _alQuantSort;

		public ArrayList _alQuantType;

		public ArrayList _alQuantBinding;

		public Quant(int type, String var) {

			_alQuantSort = new ArrayList();
			_alQuantType = new ArrayList();
			_alQuantBinding = new ArrayList();

			_alQuantSort.add("Top");
			_alQuantType.add(new Integer(type));
			_alQuantBinding.add(new TVar(var));
		}

		public Quant(int type, String var, String sort) {

			_alQuantSort = new ArrayList();
			_alQuantType = new ArrayList();
			_alQuantBinding = new ArrayList();

			_alQuantSort.add(sort);
			_alQuantType.add(new Integer(type));
			_alQuantBinding.add(new TVar(var));
		}

		public Quant(Quant outer, Quant inner) {

			_alQuantSort = outer._alQuantSort;
			_alQuantType = outer._alQuantType;
			_alQuantBinding = outer._alQuantBinding;

			// Add the rest
			_alQuantSort.addAll(inner._alQuantSort);
			_alQuantType.addAll(inner._alQuantType);
			_alQuantBinding.addAll(inner._alQuantBinding);
		}
	}

	// --------------------------------------------------------------------
	// Negation Node
	// --------------------------------------------------------------------

	public static class NNode extends Node {

		public Node _subnode;

		public NNode() {
			_subnode = null;
			_hsFreeVarsIn = new HashSet();
			_hsFreeVarsOut = new HashSet();
		}

		public NNode(Node n) {
			_subnode = n;
			_hsFreeVarsIn = new HashSet();
			_hsFreeVarsOut = new HashSet();
		}

		public Node copy() {
			NNode n = new NNode();
			n._subnode = (Node) _subnode.copy();
			n._hsFreeVarsIn = (HashSet) _hsFreeVarsIn.clone();
			n._hsFreeVarsOut = (HashSet) _hsFreeVarsOut.clone();
			return n;
		}

		public Node getSubNode() {
			return _subnode;
		}

		/*
		 * Obvious effect - negations will be on PNodes when done, no NNodes,
		 * IMPLY, EQUIV will be used, only AND/OR
		 */
		public Node convertNNF(boolean negate) {

			// We reversed polarity for below, so can remove this node
			// and return NNF subnode!
			return _subnode.convertNNF(!negate);
		}

		/* Push all !E through | and !A through ^ on an NNF formula */
		public Node pushDownQuant() {
			System.out.println("For pushDownQuant, should be in NNF!");
			System.exit(1);
			return null;
		}

		/* Set the free variables at each node */
		public HashSet setFreeVars() {
			HashSet free_vars = _subnode.setFreeVars();
			_hsFreeVarsIn = (HashSet) free_vars.clone();
			_hsFreeVarsOut = (HashSet) free_vars.clone();
			return free_vars;
		}

		/* Simplify equality expressions */
		public Node simplifyEquality() {

			// Cannot simplify so continue
			_subnode = _subnode.simplifyEquality();
			return this;
		}

		/* Remove redundancy among subformulae */
		public SimpResult removeRedundancy(ArrayList parinfo) {
			System.out
					.println("Cannot removeRedundancy() in a non-NNF formula!");
			System.exit(1);
			return null;
		}

		/* Substitute vars in the HashMap */
		public Node substitute(HashMap subs) {
			_subnode = _subnode.substitute(subs);
			return this;
		}

		/* Regress a formula using the provided mapping */
		public Node regress(HashMap regr_map) {
			_subnode = _subnode.regress(regr_map);
			return this;
		}

		public void setVarID(HashMap str2tvar, boolean start_zero) {
			_subnode.setVarID(str2tvar, start_zero);
		}

		public String toKIFString() {
			return "(not " + _subnode.toKIFString() + ")";
		}

		public String toFOLString() {
			if (USE_OTTER) {
				return "-(" + _subnode.toFOLString() + ")";
			} else if (USE_VAMPIRE) {
				System.out
						.println("Non-atomic negation disallowed for VAMPIRE!");
				System.exit(1);
				return null;
			} else if (USE_TPTP) {
				return "~ (" + _subnode.toFOLString() + ")";
			} else {
				return "~(" + _subnode.toFOLString() + ")";
			}
		}
	}

	// --------------------------------------------------------------------
	// Connective Node
	// --------------------------------------------------------------------

	public static class ConnNode extends Node {

		public final static int INVALID = 0;

		public final static int AND = 1;

		public final static int OR = 2;

		public final static int IMPLY = 3; // (LHS => RHS_1 ... RHS_n)

		public final static int EQUIV = 4; // (should be binary only)

		public int _nType;

		public ArrayList _alSubNodes;

		public ConnNode() {
			_nType = INVALID;
			_alSubNodes = new ArrayList();
			_hsFreeVarsIn = new HashSet();
			_hsFreeVarsOut = new HashSet();
		}

		public ConnNode(int type) {
			_nType = type;
			_alSubNodes = new ArrayList();
			_hsFreeVarsIn = new HashSet();
			_hsFreeVarsOut = new HashSet();
		}

		public ConnNode(int type, Node n1, Node n2) {
			_nType = type;
			_alSubNodes = new ArrayList();

			// See if n1 can be collapsed
			if (n1 instanceof ConnNode && ((ConnNode) n1)._nType == _nType
					&& _nType <= OR) {
				_alSubNodes.addAll(((ConnNode) n1)._alSubNodes);
			} else {
				_alSubNodes.add(n1);
			}

			// See if n1 can be collapsed
			if (n2 instanceof ConnNode && ((ConnNode) n2)._nType == _nType
					&& _nType <= OR) {
				_alSubNodes.addAll(((ConnNode) n2)._alSubNodes);
			} else {
				_alSubNodes.add(n2);
			}

			_hsFreeVarsIn = new HashSet();
			_hsFreeVarsOut = new HashSet();
		}

		public Node copy() {
			ConnNode c = new ConnNode();
			c._nType = _nType;
			Iterator i = _alSubNodes.iterator();
			while (i.hasNext()) {
				c._alSubNodes.add(((Node) i.next()).copy());
			}
			c._hsFreeVarsIn = (HashSet) _hsFreeVarsIn.clone();
			c._hsFreeVarsOut = (HashSet) _hsFreeVarsOut.clone();

			return c;
		}

		public void addSubNode(Node n) {
			if (n instanceof ConnNode && ((ConnNode) n)._nType == _nType) {
				_alSubNodes.addAll(((ConnNode) n)._alSubNodes);
			} else {
				_alSubNodes.add(n);
			}
		}

		public void addSubNodes(ArrayList subnodes) {
			Iterator i = subnodes.iterator();
			while (i.hasNext()) {
				Node sn = (Node) i.next();
				if (sn instanceof ConnNode && ((ConnNode) sn)._nType == _nType) {
					_alSubNodes.addAll(((ConnNode) sn)._alSubNodes);
				} else {
					_alSubNodes.add(sn);
				}
			}
		}

		public int getType() {
			return _nType;
		}

		public int getNumSubNodes() {
			return _alSubNodes.size();
		}

		public Node getNode(int index) {
			return (Node) _alSubNodes.get(index);
		}

		public int hashCode() {
			int h = -_nType;
			Iterator i = _alSubNodes.iterator();
			while (i.hasNext()) {
				h += i.next().hashCode();
			}
			return h;
		}

		public boolean equals(Object o) {
			if (this == o) {
				return true;
			} else if (o instanceof ConnNode) {
				ConnNode c = (ConnNode) o;
				int this_size = _alSubNodes.size();
				if (_nType != c._nType || this_size != c._alSubNodes.size()) {
					return false;
				}

				// Just do straightforward equality comparison...
				// should rely on a **lexical** ordering here...
				for (int i = 0; i < this_size; i++) {
					Node t1 = (Node) _alSubNodes.get(i);
					Node t2 = (Node) c._alSubNodes.get(i);
					if (!t1.equals(t2)) {
						return false;
					}
				}
				return true;
			} else {
				return false;
			}
		}

		/*
		 * Obvious effect - negations will be on PNodes when done, no NNodes,
		 * IMPLY, EQUIV will be used, only AND/OR
		 */
		public Node convertNNF(boolean negate) {
			ArrayList alNewSubNodes = new ArrayList();
			switch (_nType) {
			case AND:
			case OR:
				// Switch the operator?
				if (negate) {
					_nType = (_nType == AND) ? OR : AND;
				}

				// Process nodes below, preserve polarity
				for (int i = 0; i < _alSubNodes.size(); i++) {
					Node n = (Node) _alSubNodes.get(i);
					n = n.convertNNF(negate);
					if (n instanceof FOPC.ConnNode
							&& ((FOPC.ConnNode) n)._nType == _nType) {

						// Add all children, thus bypassing ConnNode
						alNewSubNodes.addAll(((FOPC.ConnNode) n)._alSubNodes);

					} else if (n instanceof FOPC.TNode) {

						// AND FALSE and OR TRUE cause short-circuit
						if (_nType == AND && ((FOPC.TNode) n)._bValue == false) {
							return new TNode(false);
						} else if (_nType == OR
								&& ((FOPC.TNode) n)._bValue == true) {
							return new TNode(true);
						}

						// If fell through then AND TRUE or OR FALSE, so can
						// just
						// discard, i.e. continue without adding it

					} else {

						// Cannot collapse/simplify
						alNewSubNodes.add(n);
					}
				}
				_alSubNodes = alNewSubNodes;

				if (_alSubNodes.size() <= 0) {
					// If empty, AND empty = true; OR empty = false
					return (_nType == AND) ? new TNode(true) : new TNode(false);
				} else if (_alSubNodes.size() == 1) {
					return (Node) this._alSubNodes.get(0);
				} else {
					return this; // Could not remove this node
				}

			case IMPLY:

				// Change node type
				_nType = negate ? AND : OR;
				boolean return_rhs = false;

				// Process LHS
				Node lhs = getNode(0);
				Node nlhs = lhs.convertNNF(!negate);

				if (nlhs instanceof FOPC.ConnNode
						&& ((FOPC.ConnNode) nlhs)._nType == _nType) {

					// Can collapse this OR
					alNewSubNodes.addAll(((FOPC.ConnNode) nlhs)._alSubNodes);

				} else if (false && nlhs instanceof FOPC.TNode) { // TODO:
																	// REMOVED!!!!!

					if (((FOPC.TNode) nlhs)._bValue == false) {

						// nlhs == false so return rhs - depends if negate!!!
						// TODO: REMOVED FOR NOW, depends on negate!!!
						return_rhs = true;

					} else if (((FOPC.TNode) nlhs)._bValue == true) {

						// nlhs == true so return true
						return new TNode(true);

					} // Can be no else :)

				} else {

					// Cannot collapse/simplify
					alNewSubNodes.add(nlhs);
				}

				// Process RHS
				int num_nodes = getNumSubNodes();
				for (int i = 1; i < num_nodes; i++) {
					Node rhs = getNode(i);
					rhs = rhs.convertNNF(negate);
					if (rhs instanceof FOPC.ConnNode
							&& ((FOPC.ConnNode) rhs)._nType == _nType) {
						alNewSubNodes.addAll(((FOPC.ConnNode) rhs)._alSubNodes);
					} else {
						alNewSubNodes.add(rhs);
					}
				}

				// Reset list and return
				_alSubNodes = alNewSubNodes;

				// TODO: Should revise this... some cases are valid???
				if (return_rhs) {

					// TODO: Assuming here that rhs is non-empty, should check
					// for
					// empty and for true/false
					System.out
							.println("ERROR: FOPC.ConnNode.convertNNF: Unhandled simplfication case: "
									+ this.toFOLString());
					System.exit(1);
					return this;
				} else if (_alSubNodes.size() <= 0) {
					// If empty, AND empty = true; OR empty = false
					System.out
							.println("ERROR: FOPC.ConnNode.convertNNF: Empty =>");
					System.exit(1);
					return null;
				} else if (_alSubNodes.size() == 1) {
					System.out
							.println("ERROR: FOPC.ConnNode.convertNNF: Unary =>");
					System.exit(1);
					return null;
				} else {
					return this; // Could not remove this node
				}

			case EQUIV:

				// Change this to an AND or OR if negated
				_nType = negate ? OR : AND;

				// Make two implication nodes and process them
				Node elhs = getNode(0);
				Node erhs = getNode(1);
				FOPC.ConnNode c1 = new FOPC.ConnNode(IMPLY, elhs, erhs);
				FOPC.ConnNode c2 = new FOPC.ConnNode(IMPLY, erhs.copy(), elhs
						.copy());
				alNewSubNodes.add(c1.convertNNF(negate));
				alNewSubNodes.add(c2.convertNNF(negate));

				// Subnodes must be OR so cannot collapse.
				// Just reset list and return
				_alSubNodes = alNewSubNodes;

				// TODO: Should revise this... some cases are valid??? Simplify?
				if (_alSubNodes.size() <= 0) {
					// If empty, AND empty = true; OR empty = false
					System.out
							.println("ERROR: FOPC.ConnNode.convertNNF: Empty <=>");
					System.exit(1);
					return null;
				} else if (_alSubNodes.size() == 1) {
					System.out
							.println("ERROR: FOPC.ConnNode.convertNNF: Unary <=>");
					System.exit(1);
					return null;
				} else {
					return this; // Could not remove this node
				}

			default:
				System.out.println("Illegal ConnNode type!");
				System.exit(1);
				return null;
			}
		}

		/* Push all !E through | and !A through ^ on an NNF formula */
		public Node pushDownQuant() {
			for (int i = 0; i < _alSubNodes.size(); i++) {
				Node n = (Node) _alSubNodes.get(i);
				_alSubNodes.set(i, n.pushDownQuant());
			}
			return this; // Could not remove this node
		}

		/* Set the free variables at each node */
		public HashSet setFreeVars() {

			Iterator i = _alSubNodes.iterator();
			HashSet all_free_vars = new HashSet();
			while (i.hasNext()) {
				all_free_vars.addAll(((Node) i.next()).setFreeVars());
			}

			_hsFreeVarsIn = (HashSet) all_free_vars.clone();
			_hsFreeVarsOut = (HashSet) all_free_vars.clone();

			return all_free_vars;
		}

		/* Simplify equality expressions */
		public Node simplifyEquality() {

			// Cannot simplify so continue
			for (int i = 0; i < _alSubNodes.size(); i++) {
				Node n = (Node) _alSubNodes.get(i);
				Node sn = n.simplifyEquality();

				// Just add simpified node back in
				_alSubNodes.set(i, sn);
			}

			return this; // Could not remove this node
		}

		/* Remove redundancy among subformulae (for ConnNode) */
		public SimpResult removeRedundancy(ArrayList parinfo) {

			// System.out.println("RR-ConnNode: " + toFOLString());

			HashMap new_unify_map = new HashMap();

			// Can only recurse for conjunctions
			if (_nType == AND) {

				// Put copy of ConnNode on parinfo stack (will systematically
				// call
				// with one element missing)
				FOPC.ConnNode temp_c = new ConnNode(_nType);
				temp_c.addSubNodes((ArrayList) _alSubNodes.clone());
				parinfo.add(temp_c);
				FOPC.Node saved = null;
				for (int i = 0; i < temp_c._alSubNodes.size(); i++) {

					// Save the current node (always at front) and remove it
					// from list
					saved = (FOPC.Node) temp_c._alSubNodes.remove(0);

					// Now call alg recursively with this leave-one-out list
					SimpResult r = saved.removeRedundancy(parinfo);
					saved = (FOPC.Node) r._result;
					new_unify_map.putAll(r._hmUnifyMap);

					// Put updated saved node at end (so next node is at
					// beginning)
					// By the time we finish, everything will be back where it
					// started!
					temp_c._alSubNodes.add(saved);

					// Need to check for unified variables
					if (!r._hmUnifyMap.isEmpty()) {

						// Go through these and perform any replacements on
						// other nodes
						// (All nodes are referenced via parinfo so changes will
						// be propagated)
						for (int j = 0; j < temp_c._alSubNodes.size(); j++) {
							((FOPC.Node) temp_c._alSubNodes.get(j))
									.substitute(r._hmUnifyMap);
						}
					}

					// TODO: REMOVE this assertion
					if (temp_c._alSubNodes.lastIndexOf(saved) != (temp_c._alSubNodes
							.size() - 1)) {
						System.out.println("ERROR: Element not at end of list");
						System.exit(1);
					}
				}
				parinfo.remove(parinfo.size() - 1);

				return new SimpResult(temp_c, SimpResult.UNKNOWN, new_unify_map);

			} else {

				// TODO: Actually more simplification *can* be done here
				// (inconsistency/tautology pruning but will worry about this
				// later.)

				// For now, recurse on each disjunct as if a new formula
				// (ignoring
				// current context down to this point)
				for (int i = 0; i < _alSubNodes.size(); i++) {

					// Call remove redundancy with *empty* context, ignore
					// unifications
					SimpResult r = ((FOPC.Node) _alSubNodes.get(i))
							.removeRedundancy(new ArrayList());
					_alSubNodes.set(i, r._result);

				}

				return new SimpResult(this, SimpResult.UNKNOWN, new_unify_map);
			}

		}

		/* Substitute vars in the HashMap */
		public Node substitute(HashMap subs) {

			Iterator i = _alSubNodes.iterator();
			ArrayList new_subnodes = new ArrayList();
			while (i.hasNext()) {
				new_subnodes.add(((Node) i.next()).substitute(subs));
			}
			_alSubNodes = new_subnodes;

			return this;
		}

		/* Regress a formula using the provided mapping */
		public Node regress(HashMap regr_map) {

			Iterator i = _alSubNodes.iterator();
			ArrayList new_subnodes = new ArrayList();
			while (i.hasNext()) {
				new_subnodes.add(((Node) i.next()).regress(regr_map));
			}
			_alSubNodes = new_subnodes;

			return this;
		}

		public void setVarID(HashMap str2tvar, boolean start_zero) {
			Iterator i = _alSubNodes.iterator();
			while (i.hasNext()) {
				((Node) i.next()).setVarID(str2tvar, start_zero);
			}
		}

		public String toKIFString() {

			StringBuffer sb = new StringBuffer();

			// Here need to go through and print and/or followed by subnodes
			switch (_nType) {
			case AND:
				sb.append("(and");
				break;
			case OR:
				sb.append("(or");
				break;
			case IMPLY:
				sb.append("(=>");
				break;
			case EQUIV:
				sb.append("(<=>");
				break;
			default:
				sb.append("*INVALID CONNECTIVE*");
			}
			int s2 = _alSubNodes.size();
			for (int i = 0; i < s2; i++) {
				sb.append(" " + ((Node) _alSubNodes.get(i)).toKIFString());
			}
			sb.append(")");

			return sb.toString();
		}

		public String toFOLString() {

			StringBuffer sb = new StringBuffer();

			// Here need to go through and print and/or followed by subnodes
			// ConnNode.AND/OR
			int s2 = _alSubNodes.size();
			if (USE_VAMPIRE) {
				sb.append("[ ");
			} else if (USE_TPTP) {
				sb.append(" (");
			} else {
				sb.append("(");
			}

			// REMOVE for general purposes, just for testing
			// if (!_hsFreeVars.isEmpty()) {
			// sb.append("%C:FV: " + _hsFreeVars.toString() + "% ");
			// }

			for (int i = 0; i < s2; i++) {
				if (i > 0) {
					if (USE_OTTER) {
						switch (_nType) {
						case AND:
							sb.append(" & ");
							break;
						case OR:
							sb.append(" | ");
							break;
						case IMPLY:
							sb.append(" -> ");
							break;
						case EQUIV:
							sb.append(" <-> ");
							break;
						default:
							sb.append("*INVALID CONNECTIVE*");
						}
					} else if (USE_VAMPIRE) {
						switch (_nType) {
						case OR:
							sb.append(" , ");
							break;
						case AND:
						case IMPLY:
						case EQUIV:
						default:
							sb.append("*INVALID VAMPIRE CONNECTIVE*");
							System.exit(1);
						}
					} else if (USE_TPTP) {
						switch (_nType) {
						case AND:
							sb.append(" & ");
							break;
						case OR:
							sb.append(" | ");
							break;
						case IMPLY:
							sb.append(" => ");
							break;
						case EQUIV:
							sb.append(" <=> ");
							break;
						default:
							sb.append("*INVALID CONNECTIVE*");
							System.exit(1);
						}
					} else {
						switch (_nType) {
						case AND:
							sb.append(" ^ ");
							break;
						case OR:
							sb.append(" | ");
							break;
						case IMPLY:
							sb.append(" => ");
							break;
						case EQUIV:
							sb.append(" <=> ");
							break;
						default:
							sb.append("*INVALID CONNECTIVE*");
						}
					}
				}
				Node app_node = (Node) _alSubNodes.get(i);
				sb.append(app_node.toFOLString());
			}
			if (USE_VAMPIRE) {
				sb.append(" ]");
			} else if (USE_TPTP) {
				sb.append(")");
			} else {
				sb.append(")");
			}

			return sb.toString();
		}
	}

	// --------------------------------------------------------------------
	// Literal (Predicate) Node
	// --------------------------------------------------------------------

	public static class PNode extends Node {

		// Can get >, >= through negation of this node
		public final static int INVALID = 0;

		public final static int EQUALS = 1;

		public final static int LESS = 2;

		public final static int LESSEQ = 3;

		// If override is true, the actual value of this PNode is
		// the referent. This will be simplified in convertNNF().
		public boolean _bOverride;

		public FOPC.Node _referent;

		public boolean _bIsNegated;

		public String _sPredName;

		public int _nPredID;

		public int _nArity;

		public ArrayList _alTermBinding;

		public PNode() {
			_bOverride = false;
			_referent = null;
			_bIsNegated = false;
			_sPredName = null;
			_nPredID = INVALID;
			_nArity = -1;
			_alTermBinding = null;
			_hsFreeVarsIn = new HashSet();
			_hsFreeVarsOut = new HashSet();
		}

		public PNode(boolean is_neg, String predname) {

			_bOverride = false;
			_referent = null;
			_bIsNegated = is_neg;
			_sPredName = predname;
			_nPredID = INVALID;
			_nArity = 0;
			_alTermBinding = new ArrayList();
			_hsFreeVarsIn = new HashSet();
			_hsFreeVarsOut = new HashSet();
		}

		public PNode(boolean is_neg, String predname, TermList l) {

			_bOverride = false;
			_referent = null;
			_bIsNegated = is_neg;
			_sPredName = predname;
			_nPredID = INVALID;
			_alTermBinding = l._alTerms;
			_nArity = _alTermBinding.size();
			_hsFreeVarsIn = new HashSet();
			_hsFreeVarsOut = new HashSet();
		}

		public PNode(boolean is_neg, int pred_id, TermList lhs, TermList rhs) {

			_bOverride = false;
			_referent = null;
			if (lhs._alTerms.size() != 1 && rhs._alTerms.size() != 1) {
				System.out.println("Parser error in comparison predicates");
				System.exit(1);
			}

			if (pred_id <= INVALID || pred_id > LESSEQ) {
				System.out.println("Invalid comparison predicate id");
				System.exit(1);
			}

			_bIsNegated = is_neg;
			_sPredName = null;
			_nPredID = pred_id;
			_nArity = 2;
			_alTermBinding = new ArrayList();
			_alTermBinding.add(lhs._alTerms.get(0));
			_alTermBinding.add(rhs._alTerms.get(0));
			_hsFreeVarsIn = new HashSet();
			_hsFreeVarsOut = new HashSet();
		}

		public Node copy() {
			PNode l = new PNode();
			l._bOverride = _bOverride;
			if (l._bOverride) {
				l._referent = (FOPC.Node) _referent.copy();
			} else {
				l._referent = null;
			}
			l._bIsNegated = _bIsNegated;
			l._nPredID = _nPredID;
			l._nArity = _nArity;
			l._alTermBinding = (ArrayList) _alTermBinding.clone();
			l._sPredName = _sPredName;
			l._hsFreeVarsIn = (HashSet) _hsFreeVarsIn.clone();
			l._hsFreeVarsOut = (HashSet) _hsFreeVarsOut.clone();
			return l;
		}

		// Assume that override will also override the potential
		// negation on this PNode... see convertNNF() below to
		// see how this is handled.
		public void override(FOPC.Node n) {
			_bOverride = true;
			_referent = n;
		}

		public void addBinding(Term t) {
			_alTermBinding.add(t);
			_nArity++;
		}

		public boolean isNegated() {
			return _bIsNegated;
		}

		public String getPredName() {
			return _sPredName;
		}

		public int getArity() {
			return _nArity;
		}

		public Term getBinding(int index) {
			return (Term) _alTermBinding.get(index);
		}

		/*
		 * Obvious effect - negations will be on PNodes when done, no NNodes,
		 * IMPLY, EQUIV will be used, only AND/OR
		 */
		public Node convertNNF(boolean negate) {
			if (_bOverride) {
				return _referent.convertNNF(negate);
			} else if (negate) {
				_bIsNegated = !_bIsNegated;
			}

			return this;
		}

		/* Push all !E through | and !A through ^ on an NNF formula */
		public Node pushDownQuant() {
			return this;
		}

		/* Set the free variables at each node */
		public HashSet setFreeVars() {

			HashSet free_vars = new HashSet();
			Iterator i = _alTermBinding.iterator();
			while (i.hasNext()) {
				Term t = (Term) i.next();
				if (t instanceof TVar) {
					free_vars.add(t);
				} else if (t instanceof TFunction) {
					free_vars.addAll(((TFunction) t).collectVars());
				}
			}

			_hsFreeVarsIn = (HashSet) free_vars.clone();
			_hsFreeVarsOut = (HashSet) free_vars.clone();
			return free_vars;
		}

		/* Simplify equality expressions */
		public Node simplifyEquality() {

			return this;
		}

		/* Remove redundancy among subformulae (for PNode) */
		/*
		 * TODO: Handle more complex simplifications involving inconsistency and
		 * tautology, including OR nodes as mentioned above
		 */
		/* TODO: This code may be unsound for functions with free variables */
		public SimpResult removeRedundancy(ArrayList parinfo) {

			System.out.println("removeRedundancy(.) may be unsound");
			Object o = null;
			o.toString();

			// PNode: This is the base case for redundancy removal

			// Climbs the context stack looking to see if n is
			//
			// 1) EQUIVALENT to something already in the context
			// (if shares all vars and conj => true/false based on
			// polarity)
			//
			// 2) SUBSUMED by something already in the context
			// (matches conj pos parent but exist var in child
			// replaced with functionally inferred constant
			// and mapping added to unify_map)

			// System.out.println("**RR-PNode: " + toFOLString());

			HashMap new_unify_vars = new HashMap();
			HashMap blocked_vars = new HashMap();

			// Get all free variables for this PNode
			Iterator i = _alTermBinding.iterator();
			HashSet free_vars = new HashSet();
			while (i.hasNext()) {
				Term t = (Term) i.next();
				if (t instanceof TVar) {
					free_vars.add(t);
				}
			}

			// Now traverse the context stack
			for (int level = (parinfo.size() - 1); level >= 0; level--) {

				FOPC.Node n_level = (FOPC.Node) parinfo.get(level);
				if (n_level instanceof QNode) {

					// We're searching above this quantifier
					QNode q = (QNode) n_level;
					for (int k = 0; k < q._alQuantBinding.size(); k++) {
						TVar qv = (TVar) q._alQuantBinding.get(k);
						if (free_vars.contains(qv)) {

							// If the var is blocked, note the quantifier that
							// blocked it
							free_vars.remove(qv);
							blocked_vars.put(qv, q._alQuantType.get(k));
						}
					}

				} else if (n_level instanceof ConnNode) {

					// Everything here is conjoined with the current
					// PNode. Look for single PNode children that
					// are implied by this PNode or that unify
					// due to functional restrictions
					ConnNode c = (ConnNode) n_level;
					// System.out.println("*Examinining context: " +
					// c.toFOLString());

					for (int j = 0; j < c._alSubNodes.size(); j++) {
						FOPC.Node parent = c;
						FOPC.Node sn = (FOPC.Node) c._alSubNodes.get(j);
						HashMap ln_quant_vars = new HashMap();
						while (sn instanceof FOPC.QNode) {
							FOPC.QNode qn = (FOPC.QNode) sn;

							// Save all of the local bindings for ln
							for (int k = 0; k < qn._alQuantBinding.size(); k++) {
								ln_quant_vars.put(qn._alQuantBinding.get(k),
										qn._alQuantType.get(k));
							}
							sn = (FOPC.Node) qn._subnode;
						}
						if (!(sn instanceof PNode)) {
							continue;
						}
						PNode ln = (PNode) sn;
						// System.out.println("Found PNode: " + ln.toFOLString()
						// + " / " + toFOLString());

						// For now, don't allow unification for equals - TODO:
						// Future update?
						// should be easy
						if (ln._sPredName == null || _sPredName == null
								|| !ln._sPredName.equals(_sPredName)
								|| ln._nArity != _nArity
								|| ln._bIsNegated != _bIsNegated) {
							continue;
						}

						// We have a single conjoined PNode that matches
						// 'this'
						// System.out.println("Found match: " + ln.toFOLString()
						// + " / " + toFOLString());

						// First check to see if functional variable
						// shared
						Integer fvar_id = (Integer) _hmFunctionalArgMap
								.get(_sPredName + "_" + _nArity);
						// System.out.println("FVar id: " + fvar_id);
						if (fvar_id != null) {
							Term fterm1 = (FOPC.Term) _alTermBinding
									.get(fvar_id.intValue());
							Term fterm2 = (FOPC.Term) ln._alTermBinding
									.get(fvar_id.intValue());
							// System.out.println("Term1/2: " +
							// fterm1.toFOLString() + " / " +
							// fterm2.toFOLString());
							if (fterm1.equals(fterm2)
									&& (!(fterm1 instanceof TVar) || free_vars
											.contains(fterm1))) {

								// We know the functional term matches (and
								// if a var was not blocked by a QNode so in
								// fact
								// is the same).

								// System.out.println("Functional ID: " +
								// fterm1.toFOLString() + ": " +
								// toFOLString() + "/" + ln.toFOLString());

								// TODO: Not detecting functional clash =>
								// false!!!
								// We need to do two things here:
								// 1) Unify this with ln and return the unify
								// map
								for (int k = 0; k < _alTermBinding.size(); k++) {
									Term tf = (Term) _alTermBinding.get(k);
									if (tf instanceof TVar) {
										new_unify_vars.put(((TVar) tf)._sName,
												ln._alTermBinding.get(k));
									}
								}

								// 2) Override ln to indicate that it is
								// implied.
								ln.override(new FOPC.TNode(true));

							}

						}

						// If the implication via functional implication could
						// not
						// be made, see if any non-common vars are directly
						// quantified
						// for ln. If so, we can still imply this node (although
						// no unifications are possible).
						//
						// We have... HashSet free_vars: Free vars for this
						// HashMap blocked_vars: Blocked var/quant for this
						// at the current context level
						// HashMap ln_quant_vars: Vars/quant appearing in QNodes
						// leading from context to ln
						// ArrayList _alTermBinding: Term bindings for this
						// ArrayList ln._alTermBinding: Term bindings for ln
						if (!ln._bOverride) {

							// System.out.println("*Checking for implication");
							boolean preds_match = true;
							for (int k = 0; k < _alTermBinding.size()
									&& preds_match; k++) {

								Term this_term = (Term) _alTermBinding.get(k);
								Term ln_term = (Term) ln._alTermBinding.get(k);
								if (!this_term.getClass().equals(
										ln_term.getClass())) {

									// Classes don't match, can't possibly equal
									preds_match = false;

								} else if (this_term instanceof TVar) {

									// Make sure variables are same or are
									// blocked
									// and same quantifier
									Integer qtype_this = null;
									if ((qtype_this = (Integer) blocked_vars
											.get(this_term)) != null) {

										// Var is blocked so ensure same type
										Integer qtype_ln = (Integer) ln_quant_vars
												.get(ln_term);
										if (qtype_ln != null) {
											preds_match = (qtype_ln
													.equals(qtype_this));
											if (preds_match) {
												// System.out.println("Blocked
												// term " + k + ": " +
												// this_term.toFOLString() + " /
												// " +
												// ln_term.toFOLString() +
												// " have same quantifier");
											}
										} else {
											preds_match = false;
										}

									} else {

										// Var not blocked so make sure they
										// match
										preds_match = this_term.equals(ln_term);
										if (preds_match) {
											// System.out.println("Nonblocked
											// terms " + k + ": " +
											// this_term.toFOLString() + " / " +
											// ln_term.toFOLString() +
											// " are equal");

										}

									}

								} else {

									// Non variables... do a straightforward
									// equality check
									// (to see if they are matching constants or
									// functions)
									preds_match = this_term.equals(ln_term);
									if (preds_match) {
										// System.out.println("Non-var terms " +
										// k + ": " +
										// this_term.toFOLString() + " / " +
										// ln_term.toFOLString() +
										// " match");
									}
								}

							}

							// If preds_match then ln is implied and we can
							// override
							if (preds_match) {
								// System.out.println("*Implied - pruning: " +
								// ln.toFOLString());
								ln.override(new FOPC.TNode(true));
							}
						}

					}
				}

			}

			return new SimpResult(this, SimpResult.UNKNOWN, new_unify_vars);
		}

		/* Substitute vars in the HashMap */
		public Node substitute(HashMap subs) {
			Iterator i = _alTermBinding.iterator();
			ArrayList new_termb = new ArrayList();
			while (i.hasNext()) {
				new_termb.add(((Term) i.next()).substitute(subs));
			}
			_alTermBinding = new_termb;

			return this;
		}

		/* Regress a formula using the provided mapping */
		public Node regress(HashMap regr_map) {

			if (_nPredID == INVALID) {

				// If the final term is not the situation ?s, don't worry about
				// regressing
				// this node.
				if (_alTermBinding.size() == 0) {
					return this;
				}
				Term last = (Term) _alTermBinding.get(_nArity - 1);
				if (last instanceof TVar) {
					if (((TVar) last)._nID != TVar._sit._nID) {

						// Not a fluent!!!
						return this;
					}
				} else {
					// Last PNode is something other than a situation variable
					return this;
				}

				// Retrieve the predicate and get the formula and var mappings
				Node rform = (Node) regr_map.get(_sPredName + "_" + _nArity);
				ArrayList vars = (ArrayList) regr_map.get("vars_" + _sPredName
						+ "_" + _nArity);
				if (rform == null || vars == null) {
					System.out.println("ERROR: Could not regress: "
							+ _sPredName + "_" + _nArity);
					System.exit(1);
				}

				// Copy rform, negate it if required (NNF down to this node so
				// OK)
				rform = (Node) rform.copy();
				if (_bIsNegated) {
					rform = rform.convertNNF(true);
				}

				// Substitite the correct variable names... keys are variables
				// from
				// vars, values are the corresponding terms for this PNode
				// binding
				HashMap subs = new HashMap();
				int nvars = _nArity - 1; // Don't convert situation
				for (int i = 0; i < nvars; i++) {
					subs.put((TVar) vars.get(i), (Term) _alTermBinding.get(i));
				}
				rform = rform.substitute(subs);

				// Now, can return this regressed node in place of PNode
				return rform;

			} else {

				// Cannot regress a comparison or equivalence predicate
				return this;
			}
		}

		public void setVarID(HashMap str2tvar, boolean start_zero) {
			Iterator i = _alTermBinding.iterator();
			while (i.hasNext()) {
				((Term) i.next()).setVarID(str2tvar, start_zero);
			}
		}

		public static String GetPredName(boolean neg, int ID) {
			switch (ID) {
			case EQUALS:
				return neg ? "~=" : "=";
			case LESS:
				return neg ? ">=" : "<";
			case LESSEQ:
				return neg ? ">" : "<=";
			default:
				return "INVALID";
			}
		}

		public static String GetOtterPredName(boolean neg, int ID) {
			switch (ID) {
			case EQUALS:
				return neg ? "!=" : "=";
			case LESS:
				return neg ? ">=" : "<";
			case LESSEQ:
				return neg ? ">" : "<=";
			default:
				return "INVALID";
			}
		}

		public static String GetVampirePredName(boolean neg, int ID) {
			switch (ID) {
			case EQUALS:
				return neg ? "--equal" : "++equal";
			case LESS:
			case LESSEQ:
			default:
				return "INVALID VAMPIRE PREDICATE";
			}
		}

		public static String GetTPTPPredName(boolean neg, int ID) {
			switch (ID) {
			case EQUALS:
				return neg ? "~equal" : "equal";
			case LESS:
			case LESSEQ:
			default:
				return "NEED TO INSERT TPTP LESS, GREATER PRED NAMES!";
			}
		}

		public String toKIFString() {

			StringBuffer sb = new StringBuffer();

			// Insert negation if needed
			if (_bIsNegated) {
				sb.append("(not ");
			}

			if (_nPredID == INVALID) {
				// Here need to go through and print predicate/term
				// bindings followed by subnodes
				sb.append("(" + _sPredName);
				for (int i = 0; i < _nArity; i++) {
					sb.append(" "
							+ ((Term) _alTermBinding.get(i)).toKIFString());
				}
				sb.append(")");

			} else {
				sb.append("(");
				sb.append(GetPredName(_bIsNegated, _nPredID));
				sb.append(" ");
				sb.append(((Term) _alTermBinding.get(0)).toKIFString());
				sb.append(" ");
				sb.append(((Term) _alTermBinding.get(1)).toKIFString());
				sb.append(")");
			}

			// Insert negation close paren if needed
			if (_bIsNegated) {
				sb.append(")");
			}

			return sb.toString();
		}

		public String toFOLString() {

			StringBuffer sb = new StringBuffer();

			// REMOVE for general purposes, just for testing
			// if (!_hsFreeVarsOut.isEmpty() && SHOW_FREE) {
			// sb.append("[FV: " + _hsFreeVarsOut.toString() + "] ");
			// }

			if (_nPredID == INVALID) {

				// Here need to go through and print and/or followed by subnodes
				if (USE_OTTER) {
					sb.append((_bIsNegated ? "-" : "")
							+ /* "P:" + */FilterOtterName(_sPredName)
							+ (_nArity > 0 ? "(" : ""));
				} else if (USE_VAMPIRE) {
					sb.append((_bIsNegated ? "--" : "++")
							+ /* "P:" + */FilterOtterName(_sPredName)
									.toLowerCase() + (_nArity > 0 ? "(" : ""));

				} else if (USE_TPTP) {
					sb.append((_bIsNegated ? "~" : "")
							+ /* "P:" + */FilterOtterName(_sPredName)
									.toLowerCase() + (_nArity > 0 ? "(" : ""));

				} else {
					sb.append((_bIsNegated ? "~" : "") + /* "P:" + */_sPredName
							+ (_nArity > 0 ? "(" : ""));
				}
				for (int i = 0; i < _nArity; i++) {
					sb.append(((i == 0) ? "" : ",")
							+ ((Term) _alTermBinding.get(i)).toFOLString());
				}
				if (_nArity > 0) {
					sb.append(")");
				}
				if (USE_TPTP) {
					sb.append(" ");
				}

			} else {

				if (USE_OTTER) {
					sb.append(((Term) _alTermBinding.get(0)).toFOLString());
					sb.append(GetOtterPredName(_bIsNegated, _nPredID));
					sb.append(((Term) _alTermBinding.get(1)).toFOLString());
				} else if (USE_VAMPIRE) {
					sb.append(GetVampirePredName(_bIsNegated, _nPredID));
					sb.append("("
							+ ((Term) _alTermBinding.get(0)).toFOLString()
							+ ",");
					sb.append(((Term) _alTermBinding.get(1)).toFOLString()
							+ ")");
				} else if (USE_TPTP) {
					sb.append(GetTPTPPredName(_bIsNegated, _nPredID));
					sb.append("("
							+ ((Term) _alTermBinding.get(0)).toFOLString()
							+ ",");
					sb.append(((Term) _alTermBinding.get(1)).toFOLString()
							+ ")");
				} else {
					sb.append(((Term) _alTermBinding.get(0)).toFOLString());
					sb.append(GetPredName(_bIsNegated, _nPredID));
					sb.append(((Term) _alTermBinding.get(1)).toFOLString());
				}
			}

			return sb.toString();
		}

		public int hashCode() {
			int h = (_nPredID == INVALID ? _sPredName.hashCode() : _nPredID)
					- _nArity;
			Iterator i = _alTermBinding.iterator();
			while (i.hasNext()) {
				h += i.next().hashCode();
			}
			return h;
		}

		public boolean equalsUnused(Object o) {
			if (o instanceof PNode) {
				PNode p = (PNode) o;
				if (!_sPredName.equals(p._sPredName) || _nArity != p._nArity
						|| _bIsNegated != p._bIsNegated) {
					return false;
				}
				// int sz = _alTermBinding.size();
				// for (int i = 0; i < sz; i++) {
				// Term t1 = (Term)_alTermBinding.get(i);
				// Term t2 = (Term)p._alTermBinding.get(i);
				// if (!t1.equals(t2)) {
				// return false;
				// }
				// }

				// NOTE: THIS IS UNSOUND BECAUSE IT DOES NOT TAKE INTO ACCOUNT
				// OTHER VARIABLE CONSTRAINTS ON PNODES... IT IS ONLY LOCAL!
				// THIS COULD LEAD TO MAJOR PROBLEMS IF NOT WATCHED!
				HashMap theta = unify(this, p);
				if (theta != null && allVarSubs(theta)) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}

		public boolean equals(Object o) {
			if (o instanceof PNode) {
				PNode p = (PNode) o;
				if (_nPredID == INVALID && p._nPredID == INVALID) {
					if (!_sPredName.equals(p._sPredName)
							|| _nArity != p._nArity
							|| _bIsNegated != p._bIsNegated) {
						return false;
					}
				} else if (_nPredID != p._nPredID) {
					return false;
				}
				int sz = _alTermBinding.size();
				for (int i = 0; i < sz; i++) {
					Term t1 = (Term) _alTermBinding.get(i);
					Term t2 = (Term) p._alTermBinding.get(i);
					if (!t1.equals(t2)) {
						return false;
					}
				}
				return true;
			} else {
				return false;
			}
		}

		public boolean allVarSubs(HashMap subst) {
			Iterator i = subst.values().iterator();
			while (i.hasNext()) {
				if (!(i.next() instanceof FOPC.TVar)) {
					return false;
				}
			}
			return true;
		}
	}

	// --------------------------------------------------------------------
	// True/False Node
	// --------------------------------------------------------------------

	public static class TNode extends Node {

		public boolean _bValue;

		public TNode(boolean bval) {
			_bValue = bval;

			// These will never have any free vars, just
			// placeholders
			_hsFreeVarsIn = new HashSet();
			_hsFreeVarsOut = new HashSet();
		}

		public Node copy() {
			return new TNode(_bValue);
		}

		// If true (top), else false (bottom)
		public boolean isTrue() {
			return _bValue;
		}

		/*
		 * Obvious effect - negations will be on PNodes when done, no NNodes,
		 * IMPLY, EQUIV will be used, only AND/OR
		 */
		public Node convertNNF(boolean negate) {
			if (negate) {
				return new TNode(!_bValue);
			} else {
				return this;
			}
		}

		/* Push all !E through | and !A through ^ on an NNF formula */
		public Node pushDownQuant() {
			return this;
		}

		/* Substitute vars in the HashMap */
		public Node substitute(HashMap subs) {
			return this;
		}

		/* Set the free variables at each node */
		public HashSet setFreeVars() {
			return new HashSet();
		}

		/* Simplify equality expressions */
		public Node simplifyEquality() {

			// Cannot simplify and cannot go any further
			return this;
		}

		/* Remove redundancy among subformulae */
		public SimpResult removeRedundancy(ArrayList parinfo) {

			// Nothing to simplify here
			return new SimpResult(this, SimpResult.UNKNOWN, new HashMap());
		}

		/* Regress a formula using the provided mapping */
		public Node regress(HashMap regr_map) {
			return this;
		}

		public void setVarID(HashMap str2tvar, boolean start_zero) {
		}

		// A hack but only way to get true/false to work with JTP since
		// these KIF constants are not implemented
		public String toKIFString() {
			return _bValue ? "(or (a) (not (a)))" : "(and (b) (not (b)))";
		}

		public String toFOLString() {
			if (USE_OTTER) {
				System.out.println("Warning: Otter using true/false");
				return _bValue ? "$T" : "$F";
			} else if (USE_VAMPIRE) {
				System.out.println("Warning: Vampire using true/false");
				return _bValue ? "$true" : "$false";
			} else if (USE_TPTP) {
				System.out.println("Warning: TPTP using true/false");
				return _bValue ? "$true" : "$false";
			} else {
				return _bValue ? "true" : "false";
			}
		}

		public int hashCode() {
			return _bValue ? 1 : 0;
		}

		public boolean equals(Object o) {
			if (o instanceof TNode) {
				return ((TNode) o)._bValue == _bValue;
			} else {
				return false;
			}
		}
	}

	// ====================================================================
	// Term Objects
	// ====================================================================

	/** Note: We'll have to convert the Strings to TVars later */
	public static class TermList {

		public ArrayList _alTerms;

		public TermList() {
			_alTerms = new ArrayList();
		}

		public TermList(Term t) {

			_alTerms = new ArrayList();
			_alTerms.add(t);
		}

		public TermList(TermList outer, TermList inner) {

			_alTerms = outer._alTerms;
			_alTerms.addAll(inner._alTerms);
		}
	}

	// --------------------------------------------------------------------
	// Integer Term
	// --------------------------------------------------------------------

	public abstract static class Term {

		/*
		 * By default, toKIFString() should be KIF (for JTP), but this version
		 * can be user defined (e.g. FOPC)
		 */
		public abstract String toKIFString();

		public abstract String toFOLString();

		/*
		 * Remaps any String-based vars to ID-based vars - ensures variable
		 * names are canonical @param str2tvar - Current map of String-ID
		 * entries @param start_zero - Whether to start var IDs at zero or with
		 * next available global var
		 */
		public abstract void setVarID(HashMap str2tvar, boolean start_zero);

		/* Substitute vars in the HashMap */
		public abstract Term substitute(HashMap subs);

		/* Determine what vars are mentioned in this term */
		public abstract Set collectVars();

		public abstract ArrayList getListOfTerms();
	}

	public static class TInteger extends Term {

		public int _nVal;

		public TInteger(int n) {
			_nVal = n;
		}

		public TInteger(Integer n) {
			_nVal = n.intValue();
		}

		public int getValue() {
			return _nVal;
		}

		/* Substitute vars in the HashMap */
		public Term substitute(HashMap subs) {
			return this;
		}

		/* Methods required for all terms */
		public void setVarID(HashMap str2tvar, boolean start_zero) {
		}

		public Set collectVars() {
			return new HashSet();
		}

		public ArrayList getListOfTerms() {
			return new ArrayList();
		}

		public String toKIFString() {
			return Integer.toString(_nVal);
		}

		public String toFOLString() {
			return Integer.toString(_nVal);
		}

		public int hashCode() {
			return _nVal;
		}

		public boolean equals(Object o) {
			if (o instanceof TInteger) {
				return (((TInteger) o)._nVal == _nVal);
			} else {
				return false;
			}
		}

	}

	// --------------------------------------------------------------------
	// Integer Term
	// --------------------------------------------------------------------

	public static class TScalar extends Term {

		public double _dVal;

		public TScalar(double d) {
			_dVal = d;
		}

		public TScalar(Double d) {
			_dVal = d.doubleValue();
		}

		public double getValue() {
			return _dVal;
		}

		/* Substitute vars in the HashMap */
		public Term substitute(HashMap subs) {
			return this;
		}

		/* Methods required for all terms */
		public void setVarID(HashMap str2tvar, boolean start_zero) {
		}

		public Set collectVars() {
			return new HashSet();
		}

		public ArrayList getListOfTerms() {
			return new ArrayList();
		}

		public String toKIFString() {
			return Double.toString(_dVal);
		}

		public String toFOLString() {
			return Double.toString(_dVal);
		}

		public int hashCode() {
			long l = Double.doubleToLongBits(_dVal);
			return ((int) (l >> 32) - (int) l);
		}

		public boolean equals(Object o) {
			if (o instanceof TScalar) {
				return (((TScalar) o)._dVal == _dVal);
			} else {
				return false;
			}
		}
	}

	// --------------------------------------------------------------------
	// Integer Term
	// --------------------------------------------------------------------

	// We'll create variables so often that it is better
	// to reference them by id
	public static class TVar extends Term {

		public static int _nVarCount = 0;

		public static HashMap _hmName2ID = new HashMap();

		public static TVar _sit = new TVar("s");

		public int _nID;

		public String _sName = null;

		public TVar() {
			_nID = _nVarCount++;
		}

		public TVar(String s) {
			Integer ID = (Integer) _hmName2ID.get(s);
			if (ID == null) {
				ID = new Integer(_nVarCount++);
				_hmName2ID.put(s, ID);
			}
			_nID = ID.intValue();
			_sName = s;
		}

		public Set collectVars() {
			Set s = new HashSet();
			s.add(this);
			return s;
		}

		public ArrayList getListOfTerms() {
			ArrayList a = new ArrayList();
			a.add("" + _nID);
			return a;
		}

		public int getVarID() {
			return _nID;
		}

		public static int GetNextFreeVar(HashMap varmap) {

			System.out.println("Why did I ever make GetNextFreeVar(.)?");
			System.exit(1);

			// Find current max var and return value+1
			// If no entries, will return 0. :)
			int max_val = -1;
			Iterator i = varmap.values().iterator();
			while (i.hasNext()) {
				TVar t = (TVar) i.next();
				if (max_val < t._nID) {
					max_val = t._nID;
				}
			}

			return max_val + 1;
		}

		/* Substitute vars in the HashMap */
		public Term substitute(HashMap subs) {

			// Can only substitute in for a variable (this using
			// var String name)... substitution itself could be a
			// var, function, or *any* term
			Term remap = (Term) subs.get(this);
			if (remap != null) {
				return remap;
			} else {
				return this;
			}
		}

		/* Methods required for all terms */
		public void setVarID(HashMap str2tvar, boolean start_zero) {

			System.out.println("Why did I ever make setVarID(.)?");
			System.exit(1);

			// If this var has no ID (TODO: or it's id has already been
			// remapped), find an ID matching the String or create a
			// new ID and put this in the HashMap

			// TODO!!!
			// boolean already_remapped = false;
			// Iterator i = str2tvar.iterator();

			if (_nID == -1) {
				FOPC.TVar v = (FOPC.TVar) str2tvar.get(_sName);
				if (v == null) {
					if (start_zero) {
						_nID = GetNextFreeVar(str2tvar);
					} else {
						_nID = _nVarCount++;
					}
					str2tvar.put(_sName, this);
				} else {
					_nID = v._nID;
				}
			}
		}

		public int hashCode() {
			return _nID;
		}

		public boolean equals(Object o) {
			if (o instanceof TVar) {
				return (_nID == ((TVar) o)._nID);
			} else {
				return false;
			}
		}

		public String toKIFString() {
			if (_sName != null) {
				return "?" + _sName;
			} else {
				return "?v" + _nID;
			}
		}

		public String toFOLString() {
			if (_sName != null) {
				if (USE_OTTER) {
					return "_" + FilterOtterName(_sName);
				} else if (USE_VAMPIRE) {
					return "X" + _nID;
					// return FilterOtterName(_sName).toUpperCase();
				} else if (USE_TPTP) {
					return "X" + _nID;
					// return FilterOtterName(_sName).toUpperCase();
				} else {
					return "?" + _sName;
				}
			} else {
				if (USE_OTTER) {
					return "_v" + _nID;
				} else if (USE_VAMPIRE) {
					return "X" + _nID;
				} else if (USE_TPTP) {
					return "X" + _nID;
				} else {
					return "?v" + _nID;
				}
			}
		}

		public String toString() {
			return toFOLString();
		}
	}

	// --------------------------------------------------------------------
	// Function Term
	// --------------------------------------------------------------------

	public static HashMap _hmSpecFuns = new HashMap();
	static {
		_hmSpecFuns.put("f_mul", "*");
		_hmSpecFuns.put("f_add", "+");
		_hmSpecFuns.put("f_sub", "-");
		_hmSpecFuns.put("f_div", "/");
		_hmSpecFuns.put("f_mod", "%");
	}
	
	public static class TFunction extends Term {

		public static int FUN_ID_CNT = 0;

		public String _sFunName;

		public int _nArity;

		public ArrayList _alTermBinding;

		public TFunction(String fun_name) {
			_sFunName = fun_name;
			_nArity = 0;
			_alTermBinding = new ArrayList();
		}

		public TFunction(String fun_name, TermList l) {
			_sFunName = fun_name;
			_alTermBinding = l._alTerms;
			_nArity = _alTermBinding.size();
		}

		public TFunction(String fun_name, Term l, Term r) {
			_sFunName = fun_name;
			_alTermBinding = new ArrayList();
			_alTermBinding.add(l);
			_alTermBinding.add(r);
			_nArity = 2;
		}

		public TFunction(String fun_name, TermList l, TermList r) {
			if (l._alTerms.size() != 1 && r._alTerms.size() != 1) {
				System.out.println("Cannot build a function function from non-singular term lists!");
			}
			_sFunName = fun_name;
			_alTermBinding = new ArrayList();
			_alTermBinding.add(l._alTerms.get(0));
			_alTermBinding.add(r._alTerms.get(0));
			_nArity = 2;
		}

		public TFunction(String fun_name, ArrayList term_bindings) {
			_sFunName = fun_name;
			_alTermBinding = term_bindings;
			_nArity = _alTermBinding.size();
		}

		public TFunction(ArrayList term_bindings) {
			_sFunName = "sk" + FUN_ID_CNT++;
			_alTermBinding = term_bindings;
			_nArity = _alTermBinding.size();
		}

		public void addBinding(Term t) {
			_alTermBinding.add(t);
			_nArity++;
		}

		public String getFunName() {
			return _sFunName;
		}

		public int getArity() {
			return _nArity;
		}

		public Term getBinding(int index) {
			return (Term) _alTermBinding.get(index);
		}

		public Set collectVars() {
			Set vars = new HashSet();
			Iterator i = _alTermBinding.iterator();
			while (i.hasNext()) {
				Term t = (Term) i.next();
				if (t instanceof TVar) {
					vars.add(t);
				} else if (t instanceof TFunction) {
					vars.addAll(t.collectVars());
				}
			}

			return vars;
		}

		public ArrayList getListOfTerms() {
			ArrayList a = new ArrayList();
			a.add(_sFunName);
			Iterator i = _alTermBinding.iterator();
			while (i.hasNext()) {
				Term t = (Term) i.next();
				a.addAll(t.getListOfTerms());
			}
			return a;
		}

		/* Substitute vars in the HashMap */
		public Term substitute(HashMap subs) {

			// Allowing substitution of one term (not just var) for another
			Term remap = (Term) subs.get(this);
			if (remap != null) {
				return remap;
			} 

			Iterator i = _alTermBinding.iterator();
			ArrayList new_termb = new ArrayList();
			while (i.hasNext()) {
				new_termb.add(((Term) i.next()).substitute(subs));
			}

			// Other formula may use this function so have to copy
			// this function and return the copy since it may have it's
			// subterms changed.
			TFunction tf = new TFunction(_sFunName, new_termb);
			return tf;
		}

		/* Methods required for all terms */
		public void setVarID(HashMap str2tvar, boolean start_zero) {
			Iterator i = _alTermBinding.iterator();
			while (i.hasNext()) {
				((Term) i.next()).setVarID(str2tvar, start_zero);
			}
		}

		public int hashCode() {
			int h = (_sFunName + _nArity).hashCode();
			for (int i = 0; i < _alTermBinding.size(); i++) {
				h += (i + 1) * _alTermBinding.get(i).hashCode();
			}
			return h;
		}

		public boolean equals(Object o) {
			if (o instanceof TFunction) {
				TFunction f = (TFunction) o;
				if (!_sFunName.equals(f._sFunName) || _nArity != f._nArity) {
					return false;
				}
				int sz = _alTermBinding.size();
				for (int i = 0; i < sz; i++) {
					Term t1 = (Term) _alTermBinding.get(i);
					Term t2 = (Term) f._alTermBinding.get(i);
					if (!t1.equals(t2)) {
						return false;
					}
				}
				return true;
			} else {
				return false;
			}
		}

		public String toKIFString() {

			// Here need to go through and print and/or followed by subnodes
			StringBuffer sb = new StringBuffer();

			if (_nArity <= 0) {
				// Just a constant symbol
				sb.append(_sFunName);
			} else {
				sb.append("(" + _sFunName);
				for (int i = 0; i < _nArity; i++) {
					sb.append(" "
							+ ((Term) _alTermBinding.get(i)).toKIFString());
				}
				sb.append(")");
			}

			return sb.toString();
		}

		public String toFOLString() {

			String spec_symbol = (String)_hmSpecFuns.get(_sFunName);
			boolean APPEND_TERMS = true;
			
			// Here need to go through and print and/or followed by subnodes
			StringBuffer sb = new StringBuffer();
			if (USE_OTTER) {
				sb.append(/* "F:" + */FilterOtterName(_sFunName)
						+ (_nArity > 0 ? "(" : ""));
			} else if (USE_VAMPIRE) {
				sb.append(/* "F:" + */"f"
						+ FilterOtterName(_sFunName).toLowerCase()
						+ (_nArity > 0 ? "(" : ""));
			} else if (USE_TPTP) {
				sb.append(/* "F:" + */"f"
						+ FilterOtterName(_sFunName).toLowerCase()
						+ (_nArity > 0 ? "(" : ""));
			} else {
				if (spec_symbol != null) {
					APPEND_TERMS = false;
					sb.append("(" + ((Term) _alTermBinding.get(0)).toFOLString() + " " + 
							spec_symbol + " " + ((Term) _alTermBinding.get(1)).toFOLString() + ")");
				} else {
					sb.append(/* "F:" + */_sFunName + (_nArity > 0 ? "(" : ""));
				}
			}
			
			if (APPEND_TERMS) {
				for (int i = 0; i < _nArity; i++) {
					sb.append(((i == 0) ? "" : ",")
							+ ((Term) _alTermBinding.get(i)).toFOLString());
				}
				if (_nArity > 0) {
					sb.append(")");
				}
			}

			return sb.toString();
		}

		public String toString() {
			return toFOLString();
		}
	}

}