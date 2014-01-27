package camdp.solver;

import java.util.HashMap;

import camdp.CAction;

public class ParametrizedAction {
	public CAction _action;
	public HashMap<String, Double> _params;

	public ParametrizedAction(CAction action, HashMap<String, Double> params){
		_action = action;
		_params = new HashMap<String, Double>();
		_params.putAll(params);
		if (!_action._actionParams.containsAll(params.keySet())
			|| !params.keySet().containsAll(_action._actionParams) ){
			System.err.println("Creating Invalid parametrized action: CAction:"+ _action._sName);
			System.err.println("Action params:"+ _action._actionParams);
			System.err.println("Passed params:"+ params.keySet());
		}
	}


	public String toString(){
		String str = " Params: ";
		for(String cv : _params.keySet())
			str = str + cv + " = " + _params.get(cv) + ", ";
		return _action._sName + str;
	}
}