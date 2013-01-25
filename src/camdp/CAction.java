package camdp;

import java.util.*;

import xadd.XADD;
import xadd.XADD.BoolDec;
import xadd.XADD.DeltaFunctionSubstitution;


public class CAction {

	public CAMDP _camdp; // MDP of which this action is a part
	public String _sName; // Name of this action
	//public ArrayList<Double> _actionVarBounds = new ArrayList<Double>(2);
	public HashMap<String, Double> _hmAVar2LB;
	public HashMap<String, Double> _hmAVar2UB;
	public HashMap<String, Integer> _hmVar2DD;
	public HashMap<String, Integer> _hmNoise2DD;
	public Integer _reward;
	//public HashSet<String> _hsRewardIandNSVars;
	public ArrayList<String> _actionParams;
	public ArrayList<String> _contStateVars;
	public ArrayList<String> _boolStateVars;
	public ArrayList<String> _contNextStateVars;
	public ArrayList<String> _boolNextStateVars;
	public ArrayList<String> _contIntermVars;
	public ArrayList<String> _boolIntermVars;
	public ArrayList<String> _noiseVars;

	/**
	 * Constructor
	 * @param aVars 
	 * @param bVars 
	 **/
	public CAction(CAMDP mdp, String name, ArrayList<Double> params, HashMap<String, ArrayList> cpt_desc, Integer reward, 
			ArrayList<String> cVars, ArrayList<String> bVars, ArrayList<String> iCVars, ArrayList<String> iBVars, ArrayList<String> aVars,
			ArrayList<String> nsCVars, ArrayList<String> nsBVars, HashMap<String, ArrayList> noise_desc, ArrayList<String> noiseVars) {

		_camdp = mdp;
		_sName = name;
		_hmVar2DD = new HashMap<String, Integer>();
		_hmNoise2DD = new HashMap<String, Integer>();
		_reward = reward;

		_contStateVars = cVars;
		_boolStateVars = bVars;
		_contIntermVars = iCVars;
		_boolIntermVars = iBVars;
		_contNextStateVars = nsCVars;
		_boolNextStateVars = nsBVars;

		_noiseVars = noiseVars;

		_actionParams = aVars;

		_hmAVar2LB = new HashMap<String, Double>();
		_hmAVar2UB = new HashMap<String, Double>();
		for (int i = 0; i < _actionParams.size(); i++) {
			String var = _actionParams.get(i);
			_hmAVar2LB.put(var, params.get(2*i));
			_hmAVar2UB.put(var, params.get(2*i+1));
		}

		buildAction(cpt_desc, noise_desc);
	}


	/**
	 * Build action description DDs
	 **/
	public void buildAction(HashMap<String,ArrayList> cpt_desc, HashMap<String,ArrayList> noise_desc) {

		// Head will be for current next-state
		for (Map.Entry<String,ArrayList> e : cpt_desc.entrySet()) {
			
			String var = e.getKey();
			int xadd = _camdp._context.buildCanonicalXADD(e.getValue());
					
			// If a boolean variable, need to construct dual action diagram
			if (_boolNextStateVars.contains(var) || _boolIntermVars.contains(var)) {
				int var_index = _camdp._context.getVarIndex( _camdp._context.new BoolDec(var/* + "'"*/), false);
				int high_branch = xadd;
				int low_branch = _camdp._context.apply(
						_camdp._context.ONE, high_branch, XADD.MINUS);
				xadd = _camdp._context.getINode(var_index, low_branch, high_branch);
				xadd = _camdp._context.makeCanonical(xadd);
				_hmVar2DD.put(var, xadd);
			} else if (_contNextStateVars.contains(var) || _contIntermVars.contains(var)) {
				_hmVar2DD.put(var, xadd);
			} else {
				System.err.println("Unexpected cpf for " + var + " in action '" + _sName + "'");
				System.exit(1);
			}
		}

		// Check that all expected cpfs are defined
		HashSet<String> all_cpf_vars = new HashSet<String>();
		all_cpf_vars.addAll(_boolNextStateVars);
		all_cpf_vars.addAll(_contNextStateVars);
		all_cpf_vars.addAll(_boolIntermVars);
		all_cpf_vars.addAll(_contIntermVars);
		if (!all_cpf_vars.containsAll(cpt_desc.keySet())) {
			System.err.println("Expected cpf vars " + cpt_desc.keySet() + " to be a strict subset of interm, next-state vars: " + all_cpf_vars);
			System.exit(1);
		}
		
		// Head will be for current next-state
		HashSet<String> all_state_vars = new HashSet<String>();
		all_state_vars.addAll(_boolStateVars);
		all_state_vars.addAll(_contStateVars);
		for (Map.Entry<String,ArrayList> e : noise_desc.entrySet()) {
			
			String nvar = e.getKey();
			int xadd = _camdp._context.buildCanonicalXADD(e.getValue());
			_hmNoise2DD.put(nvar, xadd);
			
			// Check that factor only references state variables
			all_state_vars.add(nvar);
			if (!all_state_vars.containsAll(_camdp._context.collectVars(xadd))) {
				System.err.println("Noise factors can only condition on themselves and state vars: " + all_state_vars + 
						",\nbut the factor for " + nvar + " conditioned on: " + _camdp._context.collectVars(xadd));
				System.exit(1);			
			}
			all_state_vars.remove(nvar);
		}
		
		// Check that all expected noise factors are defined
		if (!new HashSet<String>(_noiseVars).equals(noise_desc.keySet())) {
			System.err.println("Expected noise vars " + _noiseVars + ", but received " + noise_desc.keySet());
			System.exit(1);
		}
		
		// Check the reward for next-state variables... if present, pre-compute
		// the regression of this reward -- or just rely on cache?
		//_hsRewardIandNSVars = filterIandNSVars(_camdp._context.collectVars(_reward));
	}

	public HashSet<String> filterIandNSVars(HashSet<String> vars) {
		HashSet<String> filter_vars = new HashSet<String>();
		for (String var : vars)
			if (var.endsWith("'") || _contIntermVars.contains(var) || _boolIntermVars.contains(var)) 
				filter_vars.add(var);
		return filter_vars;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(_sName + " ( ");
		for (int i = 0; i < _actionParams.size(); i++) {
			String var = _actionParams.get(i);
			Double lb  = _hmAVar2LB.get(var);
			Double ub  = _hmAVar2UB.get(var);
			sb.append( var + " [" + lb + ", " + ub + "] ");
		}
		sb.append("):\n");
		for (Map.Entry<String,Integer> me : _hmVar2DD.entrySet()) {
			XADD.XADDNode n = _camdp._context.getNode(me.getValue());
			sb.append("*****\nCPF: " + me.getKey() + " " + n.collectVars() + ":\n" + 
					  _camdp._context.getString(me.getValue()) + "\n");
		}
		for (Map.Entry<String,Integer> me : _hmNoise2DD.entrySet()) {
			XADD.XADDNode n = _camdp._context.getNode(me.getValue());
			sb.append("*****\nNoise: " + me.getKey() + " " + n.collectVars() + ":\n" + 
					  _camdp._context.getString(me.getValue()) + "\n");
		}
		
		XADD.XADDNode r = _camdp._context.getNode(_reward);
		sb.append("*****\nReward: " + r.collectVars() + 
				  /*" (primed vars: " + _hsRewardIandNSVars + ")" +*/ ":\n" +  
				  _camdp._context.getString(_reward) + "\n");
		
		sb.append("*****\n");

		return sb.toString();
	}
}