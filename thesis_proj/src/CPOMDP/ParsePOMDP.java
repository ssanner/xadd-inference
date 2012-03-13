package CPOMDP;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import CPOMDP.COAction;
import CPOMDP.CPOMDP;


import xadd.XADD;
import xadd.XADD.ArithExpr;
import xadd.XADD.DoubleExpr;


public class ParsePOMDP {
	
	CPOMDP _pomdp=null;
	
	public final static ArithExpr ZERO = new DoubleExpr(0d);
	public final static ArithExpr ONE = new DoubleExpr(1d);
	ArrayList<String> CVars = new ArrayList<String>();
	public HashMap<String, Double> _minCVal = new HashMap<String, Double>();
	public HashMap<String, Double> _maxCVal = new HashMap<String, Double>();
	ArrayList<String> BVars = new ArrayList<String>();
	ArrayList<String> BOVars = new ArrayList<String>();
	ArrayList<String> OVars = new ArrayList<String>();
	ArrayList<Integer> constraints = new ArrayList<Integer>();
	BigDecimal discount ;
	Integer iterations ;
	HashMap<String, COAction> hashmap = new HashMap<String, COAction>();
	
	public ParsePOMDP(CPOMDP pomdp)
	{
		_pomdp = pomdp;

	}

	public void buildPOMDP(ArrayList input) {

		if (input == null) {
			System.out.println("Empty input file!");
			System.exit(1);
		}

		Iterator i = input.iterator();
		Object o;

		// Set up variables
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("cvariables")) {
			exit("Missing cvariable declarations: " + o);
		}
		o = i.next();
		CVars = (ArrayList<String>) ((ArrayList) o).clone();		
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("min-values")) {
			exit("Missing min-values declarations: " + o);
		}
		o = i.next();
		for (int index = 0; index < CVars.size(); index++) {
			String var = CVars.get(index);
			String val = ((ArrayList) o).get(index).toString();
			if (!val.trim().equalsIgnoreCase("x")) try {
				double min_val = Double.parseDouble(val);
				_minCVal.put(var, min_val);
			} catch (NumberFormatException nfe) {
				System.out.println("\nIllegal min-value: " + var + " = " + val + " @ index " + index);
				System.exit(1);
			}
		}
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("max-values")) {
			exit("Missing max-values declarations: " + o);
		}
		o = i.next();
		for (int index = 0; index < CVars.size(); index++) {
			String var =CVars.get(index);
			String val = ((ArrayList) o).get(index).toString();
			if (!val.trim().equalsIgnoreCase("x")) try {
				double max_val = Double.parseDouble(val);
				_maxCVal.put(var, max_val);
			} catch (NumberFormatException nfe) {
				System.out.println("\nIllegal max-value: " + var + " = " + val + " @ index " + index);
				System.exit(1);
			}
		}
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("bvariables")) {
			exit("Missing bvariable declarations: " + o);
		}
		o = i.next();
		//System.out.println(o);
		BVars= (ArrayList<String>) ((ArrayList) o).clone();
		for (String var : BVars) {
			_pomdp._context.getVarIndex(_pomdp._context.new BoolDec(var), true);
			_pomdp._context.getVarIndex(_pomdp._context.new BoolDec(var + "'"), true);
		}
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("bovariables")) {
			exit("Missing bovariable declarations: " + o);
		}
		o = i.next();
		//System.out.println(o);
		BOVars= (ArrayList<String>) ((ArrayList) o).clone();
		for (String var : BOVars) {
			_pomdp._context.getVarIndex(_pomdp._context.new BoolDec(var), true);
			_pomdp._context.getVarIndex(_pomdp._context.new BoolDec(var + "'"), true);
		}
		
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("ovariables")) {
			exit("Missing ovariable declarations: " + o);
		}
		o = i.next();
		OVars = (ArrayList<String>) ((ArrayList) o).clone();

		
		// Set up actions
		while (true) 
		{
			o = i.next();
			if (!(o instanceof String)
					|| !((String) o).equalsIgnoreCase("action")) {
				break;
			}

			// o == "action" + continuous action
			String aname = (String) i.next();
			o= i.next();
			ArrayList<String> temp=null;
			
			//for more than one continuous parameter for each action
			boolean checkNoparam = false;
			for (int k=0;k<CVars.size();k++)
			{
				while (!(o.equals(CVars.get(k)+"'")))
						{
							
							temp=(ArrayList<String>) ((ArrayList) o).clone();
							checkNoparam = true;
							break;
						}
				if (checkNoparam) break;
			}
			HashMap<String,ArrayList> cpt_map = new HashMap<String,ArrayList>();
			HashMap<String,ArrayList> cpt_obs = new HashMap<String,ArrayList>();
			
			//o = i.next();
			while (!((String) o).equalsIgnoreCase("observation")) {//endaction
				cpt_map.put((String) o, (ArrayList) i.next());
				o = i.next();
			}
			o = i.next();
			while (!((String) o).equalsIgnoreCase("reward")) {//endaction
				cpt_obs.put((String) o, (ArrayList) i.next());
				o = i.next();
			}
			// Set up reward
			if (!(o instanceof String) || !((String) o).equalsIgnoreCase("reward")) {
				System.out.println("Missing reward declaration for action: "+aname +" "+ o);
				System.exit(1);
			}
			//new parser format : has + for ANDing rewards
			o=i.next();
			ArrayList reward = (ArrayList) o;
			int _runningSum=0,reward_dd = 0;
			int reward_toGoal = _pomdp._context.buildCanonicalXADD(reward);
			//new parser format : has + for ANDing rewards
			 o = i.next();
			 while (!((String) o).equalsIgnoreCase("endaction"))
			{
				int reward_2=0;
				if (((String) o).equalsIgnoreCase("+"))
				{
					o = i.next();
					reward = (ArrayList) o; 
					reward_2 = _pomdp._context.buildCanonicalXADD(reward);
					int T_ZERO = _pomdp._context.getTermNode(ZERO);
					int T_ONE = _pomdp._context.getTermNode(ONE);
					int var = _pomdp._context.getVarIndex(_pomdp._context.new BoolDec(BVars.get(0)), false);
					int ind_true = _pomdp._context.getINode(var,  T_ZERO,  T_ONE);
					int ind_false = _pomdp._context.getINode(var,  T_ONE,  T_ZERO);
					//int true_half = _context.applyInt(ind_true, reward_toGoal, _context.PROD,-1); // Note: this enforces canonicity so
					//int reward_d = _context.apply(reward_2,reward_toGoal, _context.SUM,-1);
					int false_half = _pomdp._context.applyInt(ind_false, reward_2, _pomdp._context.PROD); // can use applyInt rather than apply
					//reward_dd = _context.applyInt(true_half, false_half, _context.SUM,-1);
					reward_dd = _pomdp._context.applyInt(reward_2, reward_toGoal, _pomdp._context.SUM);
				}
				o=i.next();
			}
				
		
			
			if (reward_dd>0)
				hashmap.put(aname, new COAction(_pomdp, aname, cpt_map,cpt_obs, reward_dd,CVars,OVars,BVars,BOVars));
			else hashmap.put(aname, new COAction(_pomdp, aname, cpt_map,cpt_obs, reward_toGoal,CVars,OVars,BVars,BOVars));
			
				//o=i.next(); // endaction
			
		}

		// Check for constraints declaration (can be multiple)
		constraints =new ArrayList<Integer>();
		while (true) {
			if (!(o instanceof String)
					|| !((String) o).equalsIgnoreCase("constraint")) {
				break;
			}

			o=i.next(); // get dd
			ArrayList next_constraint = (ArrayList) o;
			int next_constraint_dd = _pomdp._context.buildCanonicalXADD(next_constraint);
			constraints.add(next_constraint_dd);
			
			o = i.next(); // get endconstraint
			o = i.next(); // get constraint or discount
		}
		// Read discount and tolerance
		//o = i.next();
		if (!(o instanceof String)
				|| !((String) o).equalsIgnoreCase("discount")) {
			System.out.println("Missing discount declaration: " + o);
			System.exit(1);
		}
		discount = ((BigDecimal) i.next());
		o = i.next();
		if (!(o instanceof String)
				|| !((String) o).equalsIgnoreCase("iterations")) {
			System.out.println("Missing iterations declaration: " + o);
			System.exit(1);
		}
		iterations = (new Integer((String)i.next()));
		
	}
	
	public HashMap<String, Double> get_minCVal() {
		return _minCVal;
	}

	public void set_minCVal(HashMap<String, Double> _minCVal) {
		this._minCVal = _minCVal;
	}

	public HashMap<String, Double> get_maxCVal() {
		return _maxCVal;
	}

	public void set_maxCVal(HashMap<String, Double> _maxCVal) {
		this._maxCVal = _maxCVal;
	}

	
	
	public void exit(String msg) {
		System.out.println(msg);
		System.exit(1);
	}

	public XADD get_context() {
		return _pomdp._context;
	}

	public void set_context(XADD _context) {
		this._pomdp._context = _context;
	}



	public ArrayList<String> getCVars() {
		return CVars;
	}

	public ArrayList<String> getBVars() {
		return BVars;
	}
	
	public ArrayList<String> getOVars() {
		return OVars;
	}

	public ArrayList<String> getBOVars() {
		return BOVars;
	}

	public ArrayList<Integer> getConstraints() {
		return constraints;
	}

	public BigDecimal getDiscount() {
		return discount;
	}

	public Integer getIterations() {
		return iterations;
	}

	public HashMap<String, COAction> getHashmap() {
		return hashmap;
	}

	
	
	
	
	
}
