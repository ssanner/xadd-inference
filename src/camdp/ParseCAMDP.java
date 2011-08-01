package camdp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import xadd.XADD;
import xadd.XADD.BoolDec;

public class ParseCAMDP {
	
	XADD _context=null;
	CAMDP camdp = null;
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

			int reward_dd = _context.buildCanonicalXADD(reward);
			//Graph g = _context.getGraph(_rewardDD);
			//g.launchViewer(1300, 770);

			hashmap.put(aname, new CAction(camdp, aname, contParam, cpt_map, reward_dd,CVars,AVars));
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
