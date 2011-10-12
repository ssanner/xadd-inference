package camdp;

import graph.Graph;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import xadd.XADD;
import xadd.XADD.ArithExpr;
import xadd.XADD.BoolDec;
import xadd.XADD.CompExpr;
import xadd.XADD.DoubleExpr;
import xadd.XADD.ExprDec;

public class ParseCAMDP {
	
	XADD _context=null;
	CAMDP camdp = null;
	public final static ArithExpr ZERO = new DoubleExpr(0d);
	public final static ArithExpr ONE = new DoubleExpr(1d);
	ArrayList<String> CVars = new ArrayList<String>();
	ArrayList<String> BVars = new ArrayList<String>();
	ArrayList<String> IVars = new ArrayList<String>();
	ArrayList<String> AVars = new ArrayList<String>();
	ArrayList<Double> contParam = new ArrayList<Double>(2);
	ArrayList<Integer> constraints = new ArrayList<Integer>();
	BigDecimal discount ;
	Integer iterations ;
	HashMap<String, CAction> hashmap = new HashMap<String, CAction>();
	
	public ParseCAMDP(XADD context,CAMDP camdp2)
	{
		_context = context;
		camdp =camdp2;
	}

	public void buildCAMDP(ArrayList input) {

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
				_context._hmMinVal.put(var, min_val);
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
				_context._hmMaxVal.put(var, max_val);
			} catch (NumberFormatException nfe) {
				System.out.println("\nIllegal max-value: " + var + " = " + val + " @ index " + index);
				System.exit(1);
			}
		}
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("bvariables")) {
			exit("Missing bvariable declarations: " + o);
		}
		//////////////// ADD these for the purpose of CACTION 
		camdp._alCVars = CVars;
		// TODO: Add all boolean vars to XADD
		o = i.next();
		//System.out.println(o);
		BVars= (ArrayList<String>) ((ArrayList) o).clone();
		for (String var : BVars) {
			_context.getVarIndex(_context.new BoolDec(var), true);
			_context.getVarIndex(_context.new BoolDec(var + "'"), true);
		}
		camdp._alBVars = BVars;
		// TODO: Add all intermediate boolean vars to XADD
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("ivariables")) {
			exit("Missing ivariable declarations: " + o);
		}
		o = i.next();
		IVars = (ArrayList<String>) ((ArrayList) o).clone();
		camdp._alIVars = IVars;
		
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("avariables")) {
			exit("Missing avariable declarations: " + o);
		}
		o = i.next();
		AVars = (ArrayList<String>) ((ArrayList) o).clone();
		camdp._alAVars = AVars;
		
		
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
			//if it is a continuous action (has avariables) then it either has bounds in the paranthesis or we add bounds
			if (AVars.size()>0)
				parseActionParam(temp);
			HashMap<String,ArrayList> cpt_map = new HashMap<String,ArrayList>();

			o = i.next();
			while (!((String) o).equalsIgnoreCase("reward")) {//endaction
				cpt_map.put((String) o, (ArrayList) i.next());
				o = i.next();
			}

			// Set up reward
			if (!(o instanceof String) || !((String) o).equalsIgnoreCase("reward")) {
				System.out.println("Missing reward declaration for action: "+aname +" "+ o);
				System.exit(1);
			}
			o=i.next();
			ArrayList reward = (ArrayList) o;
			ArrayList<Obsticle> obsticle = new ArrayList<Obsticle>();
			int nextObsPos = -1;
			for (int s=0;s<reward.size();s++) 
			{
				if (reward.get(s).equals("+"))
				{
					//all the paranthesis on s+1 is the obsticle
					Obsticle obs = new Obsticle();
					int counter =0;
					ArrayList obsticle1 = (ArrayList) reward.get(s+1);
					for (int t=0;t<obsticle1.size();t++)
					{
						ArrayList obsElement = (ArrayList) obsticle1.get(t);
							if (counter==0)	
							 {
								obs.setXpoint1(Double.parseDouble(obsElement.get(0).toString()));
						 		obs.setYpoint1(Double.parseDouble(obsElement.get(1).toString()));
						 		counter++;
							 }
							 else if (counter ==1)
							 {
								obs.setXpoint2(Double.parseDouble(obsElement.get(0).toString()));
							 	obs.setYpoint2(Double.parseDouble(obsElement.get(1).toString()));
							 	counter++;
							 }
							 else 
								 obs.setPenalty(Integer.parseInt(obsElement.get(0).toString()));
					}				
					
					//remove this obsticle from reward
					reward.remove(s+1);
					reward.remove(s);
					obsticle.add(obs);
				}
			}
			int _runningSum=0;
			
			int reward_toGoal = _context.buildCanonicalXADD(reward);
			//define total reward = reward_toGoal + penalty
			if (obsticle.size()>0)
			{
				for (int t=0;t<obsticle.size();t++)
					_runningSum = buildObsticleXADD(obsticle.get(t),_runningSum);
				int T_ZERO = _context.getTermNode(ZERO);
				int T_ONE = _context.getTermNode(ONE);
				int var = _context.getVarIndex(_context.new BoolDec(BVars.get(0)), false);
				int ind_true = _context.getINode(var, /* low */T_ZERO, /* high */T_ONE);
				int ind_false = _context.getINode(var, /* low */T_ONE, /* high */T_ZERO);
				int true_half = _context.applyInt(ind_true, T_ZERO, _context.PROD); // Note: this enforces canonicity so
				int reward_d = _context.apply(_runningSum,reward_toGoal, _context.SUM);
				int false_half = _context.applyInt(ind_false, reward_d, _context.PROD); // can use applyInt rather than apply
				int reward_dd = _context.applyInt(true_half, false_half, _context.SUM);
				//int reward_dd = _context.apply(_runningSum,reward_toGoal, _context.SUM);
				Graph g = _context.getGraph(reward_dd);
				g.launchViewer(1300, 770);
				reward_dd = _context.makeCanonical(reward_dd);
				/*//can't perform LP-reduce because of x*ay (bi-linear constraints)
				 * ArrayList<String> contVars = new ArrayList<String>();
				contVars.addAll(CVars);
				contVars.addAll(AVars);
				reward_dd = _context.reduceLP(reward_dd, contVars);*/
				hashmap.put(aname, new CAction(camdp, aname, contParam, cpt_map, reward_dd,CVars,AVars));
			}
			else hashmap.put(aname, new CAction(camdp, aname, contParam, cpt_map, reward_toGoal,CVars,AVars));
			
				o=i.next(); // endaction
			
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
			int next_constraint_dd = _context.buildCanonicalXADD(next_constraint);
			constraints.add(next_constraint_dd);
			
			o = i.next(); // get endconstraint
			o = i.next(); // get constraint or discount
		}
		camdp._alConstraints = constraints;
		// Read discount and tolerance
		//o = i.next();
		if (!(o instanceof String)
				|| !((String) o).equalsIgnoreCase("discount")) {
			System.out.println("Missing discount declaration: " + o);
			System.exit(1);
		}
		discount = ((BigDecimal) i.next());
		camdp._bdDiscount = discount;
		o = i.next();
		if (!(o instanceof String)
				|| !((String) o).equalsIgnoreCase("iterations")) {
			System.out.println("Missing iterations declaration: " + o);
			System.exit(1);
		}
		iterations = (new Integer((String)i.next()));
		camdp._nIter = iterations;
	}
	
	private int buildObsticleXADD(Obsticle obsticle,int _RS) {
		//hand coded function for x,y,ax,ay 
		//according to the points, build C and D for the line intersection 
		//Each decision of C and D are inserted into a tree with true=penalty and false = 0 
		//to build the tree of the decisions, each indicator function (eg: c>=0) is build with a simple 
		//getVarNode according to values of C and D and then we multiply the penalty and all the indicator functions
		//to get the penalty of obsticles tree
		
		//first build test for checking slope equality of parallel lines: ay = (y2-y1)/(x2-x1)*ax
		int obsticleXADD=0;
		String p_lhs = "ay";
		ArithExpr arith_p_lhs = ArithExpr.parse(p_lhs);
		String p_rhs = "(("+obsticle.getYpoint2()+"-"+obsticle.getYpoint1()+")/("+obsticle.getXpoint2()+"-"+obsticle.getXpoint1()+"))*ax";
		ArithExpr arith_p_rhs = ArithExpr.parse(p_rhs);
		CompExpr comp_p1 = new CompExpr(_context.GT_EQ, arith_p_lhs, arith_p_rhs);
		ExprDec expr_p1 = _context.new ExprDec(comp_p1);
		
		obsticleXADD = _context.getVarNode(expr_p1, 0, 1);
		obsticleXADD = _context.makeCanonical(obsticleXADD);
		CompExpr comp_p2 = new CompExpr(_context.LT_EQ, arith_p_lhs, arith_p_rhs);
		ExprDec expr_p2 = _context.new ExprDec(comp_p2);
		
		obsticleXADD = _context.apply(_context.getVarNode(expr_p2,0, 1),obsticleXADD,_context.PROD);
		//switch the true and false branch
		int T_ONE = _context.getTermNode(ONE);
		int switchedBranch = _context.applyInt(T_ONE, obsticleXADD, _context.MINUS); // Note: this enforces canonicity so
		obsticleXADD = _context.makeCanonical(switchedBranch);
		//Indicator for D>=0
		
		String d0_lhs = "-ay*"+obsticle.getXpoint1()+"+(ay*x)-(ax*y)+ax*"+obsticle.getYpoint1();
		ArithExpr arith_d0_lhs = ArithExpr.parse(d0_lhs);
		CompExpr comp_d0 = new CompExpr(_context.GT_EQ, arith_d0_lhs, ArithExpr.parse("0"));
		ExprDec expr_d0 = _context.new ExprDec(comp_d0);
		
		obsticleXADD = _context.apply(_context.getVarNode(expr_d0, 0, 1),obsticleXADD,_context.PROD);
		obsticleXADD = _context.makeCanonical(obsticleXADD);
		//Indicator for D<=1
		String d1_lhs = "-ay*"+obsticle.getXpoint1()+"+(ay*x)-(ax*y)+ax*"+obsticle.getYpoint1();
		ArithExpr arith_d1_lhs = ArithExpr.parse(d1_lhs);
		String d1_rhs = "ay*("+obsticle.getXpoint2()+"-"+obsticle.getXpoint1()+")+(-ax*("+obsticle.getYpoint2()+"-"+obsticle.getYpoint1()+"))";
		ArithExpr arith_d1_rhs = ArithExpr.parse(d1_rhs);
		CompExpr comp_d1 = new CompExpr(_context.LT_EQ, arith_d1_lhs, arith_d1_rhs);
		ExprDec expr_d1 = _context.new ExprDec(comp_d1);
		//_context.getVarNode(expr_d1, 0, 1);
		obsticleXADD = _context.apply(_context.getVarNode(expr_d1, 0, 1),obsticleXADD,_context.PROD);
		obsticleXADD = _context.makeCanonical(obsticleXADD);
		//Indicator for C>=0
		String c0_lhs = "(x*("+obsticle.getYpoint2()+"-"+obsticle.getYpoint1()+")) -("+obsticle.getXpoint1()+"*("+
				obsticle.getYpoint2()+"-"+obsticle.getYpoint1()+")) - (y*("+obsticle.getXpoint2()+"-"+obsticle.getXpoint1()+"))+(("
				+obsticle.getXpoint2()+"-"+obsticle.getXpoint1()+")*"+obsticle.getYpoint1()+")";
		ArithExpr arith_c0_lhs = ArithExpr.parse(c0_lhs);
		CompExpr comp_c0 = new CompExpr(_context.GT_EQ, arith_c0_lhs, ArithExpr.parse("0"));
		ExprDec expr_c0 = _context.new ExprDec(comp_c0);
		
		//_context.getVarNode(expr_c0, 0, 1);
		obsticleXADD = _context.apply(_context.getVarNode(expr_c0, 0, 1),obsticleXADD,_context.PROD);
		obsticleXADD = _context.makeCanonical(obsticleXADD);
		//Indicator for D<=1
		String c1_lhs = "(x*("+obsticle.getYpoint2()+"-"+obsticle.getYpoint1()+")) -("+obsticle.getXpoint1()+"*("+
		obsticle.getYpoint2()+"-"+obsticle.getYpoint1()+")) - (y*("+obsticle.getXpoint2()+"-"+obsticle.getXpoint1()+"))+(("
		+obsticle.getXpoint2()+"-"+obsticle.getXpoint1()+")*"+obsticle.getYpoint1()+")";
		ArithExpr arith_c1_lhs = ArithExpr.parse(c1_lhs);
		String c1_rhs = "ay*("+obsticle.getXpoint2()+"-"+obsticle.getXpoint1()+")+(-ax*("+obsticle.getYpoint2()+"-"+obsticle.getYpoint1()+"))";
		ArithExpr arith_c1_rhs = ArithExpr.parse(c1_rhs);
		CompExpr comp_c1 = new CompExpr(_context.LT_EQ, arith_c1_lhs, arith_c1_rhs);
		ExprDec expr_c1 = _context.new ExprDec(comp_c1);
		
		//_context.getVarNode(expr_c1, 0, 1);
		obsticleXADD = _context.apply(_context.getVarNode(expr_c1, 0, 1),obsticleXADD,_context.PROD);
		obsticleXADD = _context.makeCanonical(obsticleXADD);
		int NEG_20 = _context.getTermNode(new DoubleExpr(-20d));
		obsticleXADD = _context.apply(obsticleXADD,NEG_20,_context.PROD);
		if (_RS!=0) obsticleXADD=_context.apply(obsticleXADD,_RS,_context.SUM);
		return obsticleXADD;
	}

	private void parseActionParam(ArrayList<String> params) {
		//parsing stage 1: 
		//Assume only either both numerical(not non-numerical) bounds are given or none
		int breakpoint =-1;
		for (int i=0;i<AVars.size();i++)
			if (params.contains(AVars.get(i)))
			{
				contParam.add(i*2, new Double(params.get(breakpoint+1)));
				for (int j=breakpoint+1;j<params.size();j++)
					if (params.get(j).equals("^")){
						breakpoint = j;
						break;
					}
					else breakpoint = params.size();
				contParam.add((i*2)+1, new Double(params.get(breakpoint-1)));
			}
			else if (params.size()==0) //no bounds defined, add explicitly
			{
				contParam.add(0, -1000000.0);
				contParam.add(1, 1000000.0);
			}
		
	}
	
	public void exit(String msg) {
		System.out.println(msg);
		System.exit(1);
	}

	public XADD get_context() {
		return _context;
	}

	public void set_context(XADD _context) {
		this._context = _context;
	}



	public ArrayList<String> getCVars() {
		return CVars;
	}

	public ArrayList<String> getBVars() {
		return BVars;
	}
	
	public ArrayList<String> getAVars() {
		return AVars;
	}

	public ArrayList<String> getIVars() {
		return IVars;
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

	public HashMap<String, CAction> getHashmap() {
		return hashmap;
	}

	
	
	
	
	
}
