package camdp;

import graph.Graph;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import xadd.XADD;
import xadd.XADD.ArithExpr;
import xadd.XADD.CompExpr;
import xadd.XADD.DoubleExpr;
import xadd.XADD.ExprDec;

public class ParseCAMDP {
	
	CAMDP _camdp=null;
	
	public final static ArithExpr ZERO = new DoubleExpr(0d);
	public final static ArithExpr ONE = new DoubleExpr(1d);
	ArrayList<String> CVars = new ArrayList<String>();
	public HashMap<String, Double> _minCVal = new HashMap<String, Double>();
	public HashMap<String, Double> _maxCVal = new HashMap<String, Double>();
	ArrayList<String> BVars = new ArrayList<String>();
	ArrayList<String> IVars = new ArrayList<String>();
	ArrayList<String> AVars = new ArrayList<String>();
	ArrayList<Double> contParam = new ArrayList<Double>(2);
	ArrayList<Integer> constraints = new ArrayList<Integer>();
	BigDecimal discount ;
	Integer iterations ;
	HashMap<String, CAction> hashmap = new HashMap<String, CAction>();
	
	public ParseCAMDP(CAMDP camdp)
	{
		_camdp = camdp;

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
			_camdp._context.getVarIndex(_camdp._context.new BoolDec(var), true);
			_camdp._context.getVarIndex(_camdp._context.new BoolDec(var + "'"), true);
		}

		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("ivariables")) {
			exit("Missing ivariable declarations: " + o);
		}
		o = i.next();
		IVars = (ArrayList<String>) ((ArrayList) o).clone();

		
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("avariables")) {
			exit("Missing avariable declarations: " + o);
		}
		o = i.next();
		AVars = (ArrayList<String>) ((ArrayList) o).clone();

		ArrayList<String> contVars = new ArrayList<String>();
		contVars.addAll(CVars);
		contVars.addAll(AVars);
			
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
			//new parser format : has + for ANDing rewards
			o=i.next();
			ArrayList reward = (ArrayList) o;
			int _runningSum=0,reward_dd = 0;
			int reward_toGoal = _camdp._context.buildCanonicalXADD(reward);
			//new parser format : has + for ANDing rewards
			 o = i.next();
			 while (!((String) o).equalsIgnoreCase("endaction"))
			{
				int reward_2=0;
				if (((String) o).equalsIgnoreCase("+"))
				{
					o = i.next();
					reward = (ArrayList) o; 
					reward_2 = _camdp._context.buildCanonicalXADD(reward);
					int T_ZERO = _camdp._context.getTermNode(ZERO);
					int T_ONE = _camdp._context.getTermNode(ONE);
					int var = _camdp._context.getVarIndex(_camdp._context.new BoolDec(BVars.get(0)), false);
					int ind_true = _camdp._context.getINode(var,  T_ZERO,  T_ONE);
					int ind_false = _camdp._context.getINode(var,  T_ONE,  T_ZERO);
					//int true_half = _context.applyInt(ind_true, reward_toGoal, _context.PROD,-1); // Note: this enforces canonicity so
					//int reward_d = _context.apply(reward_2,reward_toGoal, _context.SUM,-1);
					int false_half = _camdp._context.applyInt(ind_false, reward_2, _camdp._context.PROD); // can use applyInt rather than apply
					//reward_dd = _context.applyInt(true_half, false_half, _context.SUM,-1);
					reward_dd = _camdp._context.applyInt(reward_2, reward_toGoal, _camdp._context.SUM);
				}
				o=i.next();
			}
				
		
			////////////////////////////////////////////
			//OBSTACLE AVOIDANCE SECTION
			/*o=i.next();
			ArrayList reward = (ArrayList) o;
			int _runningSum=0,reward_dd = 0;
			ArrayList<Obstacle> obstacle = new ArrayList<Obstacle>();
			int nextObsPos = -1;
			for (int s=0;s<reward.size();s++) 
			{
				if (reward.get(s).equals("+"))
				{
					//all the paranthesis on s+1 is the obsticle
					Obstacle obs = new Obstacle();
					int counter =0;
					ArrayList obstacle1 = (ArrayList) reward.get(s+1);
					for (int t=0;t<obstacle1.size();t++)
					{
						ArrayList obsElement = (ArrayList) obstacle1.get(t);
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
					obstacle.add(obs);
				}
			}
			
			 //define total reward = reward_toGoal + penalty
			int reward_toGoal = _context.buildCanonicalXADD(reward);
			if (obstacle.size()>0)
			{
				for (int t=0;t<obstacle.size();t++)
					_runningSum = buildObstacleXADD(obstacle.get(t),_runningSum);
				int T_ZERO = _context.getTermNode(ZERO);
				int T_ONE = _context.getTermNode(ONE);
				int var = _context.getVarIndex(_context.new BoolDec(BVars.get(0)), false);
				int ind_true = _context.getINode(var,  T_ZERO,  T_ONE);
				int ind_false = _context.getINode(var,  T_ONE,  T_ZERO);
				int true_half = _context.applyInt(ind_true, T_ZERO, _context.PROD,-1); // Note: this enforces canonicity so
				int reward_d = _context.apply(_runningSum,reward_toGoal, _context.SUM);
				int false_half = _context.applyInt(ind_false, reward_d, _context.PROD,-1); // can use applyInt rather than apply
				int reward_dd1 = _context.applyInt(true_half, false_half, _context.SUM,-1);
				//int reward_dd = _context.apply(_runningSum,reward_toGoal, _context.SUM);
				Graph g = _context.getGraph(reward_dd1);
				g.addNode("_temp_");
				g.addNodeLabel("_temp_", "Q reward_dd");
				g.addNodeShape("_temp_", "square");
				g.addNodeStyle("_temp_", "filled");
				g.addNodeColor("_temp_", "lightblue");
				g.launchViewer(1300, 770);
				reward_dd1 = _context.makeCanonical(reward_dd1);
				//can't perform LP-reduce because of x*ay (bi-linear constraints)
				
				contVars.addAll(CVars);
				contVars.addAll(AVars);
				reward_dd1 = _context.reduceLP(reward_dd1, contVars);
				hashmap.put(aname, new CAction(camdp, aname, contParam, cpt_map, reward_dd1,CVars,AVars));
			}
			//else*/
			///////////////////////////////////////////////
			if (reward_dd>0)
				hashmap.put(aname, new CAction(_camdp, aname, contParam, cpt_map, reward_dd,CVars,AVars,BVars));
			else hashmap.put(aname, new CAction(_camdp, aname, contParam, cpt_map, reward_toGoal,CVars,AVars,BVars));
			
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
			int next_constraint_dd = _camdp._context.buildCanonicalXADD(next_constraint);
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

	private int buildObstacleXADD(Obstacle obstacle,int _RS) {
		
		//hand coded function for x,y,ax,ay 
				//according to the points, build C and D for the line intersection 
				//Each decision of C and D are inserted into a tree with true=penalty and false = 0 
				//to build the tree of the decisions, each indicator function (eg: c>=0) is build with a simple 
				//getVarNode according to values of C and D and then we multiply the penalty and all the indicator functions
				//to get the penalty of obsticles tree
				
				//first build test for checking slope equality of parallel lines: ay = (y2-y1)/(x2-x1)*ax
				int obstacleXADD=0;
				/*String p_lhs = "ay";
				ArithExpr arith_p_lhs = ArithExpr.parse(p_lhs);
				String p_rhs = "(("+obstacle.getYpoint2()+"-"+obstacle.getYpoint1()+")/("+obstacle.getXpoint2()+"-"+obstacle.getXpoint1()+"))*ax";
				ArithExpr arith_p_rhs = ArithExpr.parse(p_rhs);
				CompExpr comp_p1 = new CompExpr(_context.GT_EQ, arith_p_lhs, arith_p_rhs);
				ExprDec expr_p1 = _context.new ExprDec(comp_p1);
				
				obstacleXADD = _context.getVarNode(expr_p1, 0, 1);
				obstacleXADD = _context.makeCanonical(obstacleXADD);
				CompExpr comp_p2 = new CompExpr(_context.LT_EQ, arith_p_lhs, arith_p_rhs);
				ExprDec expr_p2 = _context.new ExprDec(comp_p2);
				
				obstacleXADD = _context.apply(_context.getVarNode(expr_p2,0, 1),obstacleXADD,_context.PROD);
				//switch the true and false branch
				int T_ONE = _context.getTermNode(ONE);
				int switchedBranch = _context.applyInt(T_ONE, obstacleXADD, _context.MINUS); // Note: this enforces canonicity so
				obstacleXADD = _context.makeCanonical(switchedBranch);*/
				//Indicator for D>=0
				
				String d0_lhs = "-ay*"+obstacle.getXpoint1()+"+(ay*x)-(ax*y)+ax*"+obstacle.getYpoint1();
				ArithExpr arith_d0_lhs = ArithExpr.parse(d0_lhs);
				CompExpr comp_d0 = new CompExpr(_camdp._context.GT_EQ, arith_d0_lhs, ArithExpr.parse("0"));
				ExprDec expr_d0 = _camdp._context.new ExprDec(comp_d0);
				obstacleXADD = _camdp._context.getVarNode(expr_d0, 0, 1);
				obstacleXADD = _camdp._context.makeCanonical(obstacleXADD);
				
				//obstacleXADD = _context.apply(_context.getVarNode(expr_d0, 0, 1),obstacleXADD,_context.PROD);
				//obstacleXADD = _context.makeCanonical(obstacleXADD);
				//Indicator for D<=1
				String d1_lhs = "-ay*"+obstacle.getXpoint1()+"+(ay*x)-(ax*y)+ax*"+obstacle.getYpoint1();
				ArithExpr arith_d1_lhs = ArithExpr.parse(d1_lhs);
				String d1_rhs = "ay*("+obstacle.getXpoint2()+"-"+obstacle.getXpoint1()+")+(-ax*("+obstacle.getYpoint2()+"-"+obstacle.getYpoint1()+"))";
				ArithExpr arith_d1_rhs = ArithExpr.parse(d1_rhs);
				CompExpr comp_d1 = new CompExpr(_camdp._context.LT_EQ, arith_d1_lhs, arith_d1_rhs);
				ExprDec expr_d1 = _camdp._context.new ExprDec(comp_d1);
				//_context.getVarNode(expr_d1, 0, 1);
				obstacleXADD = _camdp._context.apply(_camdp._context.getVarNode(expr_d1, 0, 1),obstacleXADD,_camdp._context.PROD);
				obstacleXADD = _camdp._context.makeCanonical(obstacleXADD);
				//Indicator for C>=0
				String c0_lhs = "(x*("+obstacle.getYpoint2()+"-"+obstacle.getYpoint1()+")) -("+obstacle.getXpoint1()+"*("+
						obstacle.getYpoint2()+"-"+obstacle.getYpoint1()+")) - (y*("+obstacle.getXpoint2()+"-"+obstacle.getXpoint1()+"))+(("
						+obstacle.getXpoint2()+"-"+obstacle.getXpoint1()+")*"+obstacle.getYpoint1()+")";
				ArithExpr arith_c0_lhs = ArithExpr.parse(c0_lhs);
				CompExpr comp_c0 = new CompExpr(_camdp._context.GT_EQ, arith_c0_lhs, ArithExpr.parse("0"));
				ExprDec expr_c0 = _camdp._context.new ExprDec(comp_c0);
				
				//_context.getVarNode(expr_c0, 0, 1);
				obstacleXADD = _camdp._context.apply(_camdp._context.getVarNode(expr_c0, 0, 1),obstacleXADD,_camdp._context.PROD);
				obstacleXADD = _camdp._context.makeCanonical(obstacleXADD);
				//Indicator for D<=1
				String c1_lhs = "(x*("+obstacle.getYpoint2()+"-"+obstacle.getYpoint1()+")) -("+obstacle.getXpoint1()+"*("+
						obstacle.getYpoint2()+"-"+obstacle.getYpoint1()+")) - (y*("+obstacle.getXpoint2()+"-"+obstacle.getXpoint1()+"))+(("
				+obstacle.getXpoint2()+"-"+obstacle.getXpoint1()+")*"+obstacle.getYpoint1()+")";
				ArithExpr arith_c1_lhs = ArithExpr.parse(c1_lhs);
				String c1_rhs = "ay*("+obstacle.getXpoint2()+"-"+obstacle.getXpoint1()+")+(-ax*("+obstacle.getYpoint2()+"-"+obstacle.getYpoint1()+"))";
				ArithExpr arith_c1_rhs = ArithExpr.parse(c1_rhs);
				CompExpr comp_c1 = new CompExpr(_camdp._context.LT_EQ, arith_c1_lhs, arith_c1_rhs);
				ExprDec expr_c1 = _camdp._context.new ExprDec(comp_c1);
				
				//_context.getVarNode(expr_c1, 0, 1);
				obstacleXADD = _camdp._context.apply(_camdp._context.getVarNode(expr_c1, 0, 1),obstacleXADD,_camdp._context.PROD);
				obstacleXADD = _camdp._context.makeCanonical(obstacleXADD);
				int NEG_penalty = _camdp._context.getTermNode(new DoubleExpr(obstacle.getPenalty()));
				obstacleXADD = _camdp._context.apply(obstacleXADD,NEG_penalty,_camdp._context.PROD);
				if (_RS!=0) obstacleXADD=_camdp._context.apply(obstacleXADD,_RS,_camdp._context.SUM);
				return obstacleXADD;
	}
	
	private int buildObstacleXADD_no_ax(Obstacle obstacle,int _RS) {
		
		//no ax - to find the problem
		//hand coded function for x,y,ay 
				//according to the points, build C and D for the line intersection 
				//Each decision of C and D are inserted into a tree with true=penalty and false = 0 
				//to build the tree of the decisions, each indicator function (eg: c>=0) is build with a simple 
				//getVarNode according to values of C and D and then we multiply the penalty and all the indicator functions
				//to get the penalty of obsticles tree
				
				//first build test for checking slope equality of parallel lines: ay = (y2-y1)/(x2-x1)*ax
				int obstacleXADD=0;
				/*String p_lhs = "ay";
				ArithExpr arith_p_lhs = ArithExpr.parse(p_lhs);
				CompExpr comp_p1 = new CompExpr(_context.GT_EQ, arith_p_lhs, ArithExpr.parse("0"));
				ExprDec expr_p1 = _context.new ExprDec(comp_p1);
				
				obstacleXADD = _context.getVarNode(expr_p1, 0, 1);
				obstacleXADD = _context.makeCanonical(obstacleXADD);
				CompExpr comp_p2 = new CompExpr(_context.LT_EQ, arith_p_lhs, ArithExpr.parse("0"));
				ExprDec expr_p2 = _context.new ExprDec(comp_p2);
				
				obstacleXADD = _context.apply(_context.getVarNode(expr_p2,0, 1),obstacleXADD,_context.PROD);
				//switch the true and false branch
				int T_ONE = _context.getTermNode(ONE);
				int switchedBranch = _context.applyInt(T_ONE, obstacleXADD, _context.MINUS); // Note: this enforces canonicity so
				obstacleXADD = _context.makeCanonical(switchedBranch);*/
				//Indicator for D>=0
				
				String d0_lhs = "x +" + (obstacle.getXpoint1()*(-1));
				ArithExpr arith_d0_lhs = ArithExpr.parse(d0_lhs);
				CompExpr comp_d0 = new CompExpr(_camdp._context.GT_EQ, arith_d0_lhs, ArithExpr.parse("0"));
				ExprDec expr_d0 = _camdp._context.new ExprDec(comp_d0);
				obstacleXADD = _camdp._context.getVarNode(expr_d0, 0, 1);
				obstacleXADD = _camdp._context.makeCanonical(obstacleXADD);
				
				//obstacleXADD = _context.apply(_context.getVarNode(expr_d0, 0, 1),obstacleXADD,_context.PROD);
				//obstacleXADD = _context.makeCanonical(obstacleXADD);
				//Indicator for D<=1
				//String d1_lhs = "x";
				ArithExpr arith_d1_lhs = ArithExpr.parse(d0_lhs);
				String d1_rhs = obstacle.getXpoint2()+"-"+obstacle.getXpoint1();
				ArithExpr arith_d1_rhs = ArithExpr.parse(d1_rhs);
				CompExpr comp_d1 = new CompExpr(_camdp._context.LT_EQ, arith_d1_lhs, arith_d1_rhs);
				ExprDec expr_d1 =_camdp._context.new ExprDec(comp_d1);
				//_context.getVarNode(expr_d1, 0, 1);
				obstacleXADD = _camdp._context.apply(_camdp._context.getVarNode(expr_d1, 0, 1),obstacleXADD,_camdp._context.PROD);
				obstacleXADD = _camdp._context.makeCanonical(obstacleXADD);
				//Indicator for C>=0
				String d = "("+d0_lhs +") /("+ d1_rhs+")";
				//String c0_lhs = obstacle.getYpoint2()+"-y";
				String c0_lhs = obstacle.getYpoint1()+"+ ((" + d + ") *"+ "("+obstacle.getYpoint2()+"-"+obstacle.getYpoint1()+")) - y"; 
				ArithExpr arith_c0_lhs = ArithExpr.parse(c0_lhs);
				CompExpr comp_c0 = new CompExpr(_camdp._context.GT_EQ, arith_c0_lhs, ArithExpr.parse("0"));
				ExprDec expr_c0 = _camdp._context.new ExprDec(comp_c0);
				
				//_context.getVarNode(expr_c0, 0, 1);
				obstacleXADD = _camdp._context.apply(_camdp._context.getVarNode(expr_c0, 0, 1),obstacleXADD,_camdp._context.PROD);
				obstacleXADD = _camdp._context.makeCanonical(obstacleXADD);
				//Indicator for C<=1
				//String c1_lhs = obstacle.getYpoint2()+"-y";
				
				ArithExpr arith_c1_lhs = ArithExpr.parse(c0_lhs);
				String c1_rhs = "ay";
				ArithExpr arith_c1_rhs = ArithExpr.parse(c1_rhs);
				CompExpr comp_c1 = new CompExpr(_camdp._context.LT_EQ, arith_c1_lhs, arith_c1_rhs);
				ExprDec expr_c1 = _camdp._context.new ExprDec(comp_c1);
				
				//_context.getVarNode(expr_c1, 0, 1);
				obstacleXADD = _camdp._context.apply(_camdp._context.getVarNode(expr_c1, 0, 1),obstacleXADD,_camdp._context.PROD);
				obstacleXADD = _camdp._context.makeCanonical(obstacleXADD);
				int NEG_penalty = _camdp._context.getTermNode(new DoubleExpr(obstacle.getPenalty()));
				obstacleXADD = _camdp._context.apply(obstacleXADD,NEG_penalty,_camdp._context.PROD);
				if (_RS!=0) obstacleXADD=_camdp._context.apply(obstacleXADD,_RS,_camdp._context.SUM);
				return obstacleXADD;
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
		return _camdp._context;
	}

	public void set_context(XADD _context) {
		this._camdp._context = _context;
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
