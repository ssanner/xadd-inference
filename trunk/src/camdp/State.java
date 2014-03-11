package camdp;

import java.util.ArrayList;
import java.util.HashMap;

public class State{
	public HashMap<String, Boolean> _hmBoolVars = new HashMap<String, Boolean> ();
	public HashMap<String, Double> _hmContVars = new HashMap<String, Double> ();
	
	public State(){ //Create empty partial State
	}
	
	public State(HashMap<String, Double> CVal, HashMap<String, Boolean> BVal){
		_hmBoolVars.putAll(BVal);
		_hmContVars.putAll(CVal);
	}


	public State(ArrayList<String> BoolVars, ArrayList<Boolean> BoolValues,
				 ArrayList<String> ContVars, ArrayList<Double> ContValues){
		for(int b = 0; b < BoolVars.size(); b++){
			_hmBoolVars.put(BoolVars.get(b), BoolValues.get(b) );
		}
		for(int c = 0; c < ContVars.size(); c++){
			_hmContVars.put(ContVars.get(c), ContValues.get(c) );
		}
		
	}

	public String toString(){
		String str = "State: CVars: ";
		for(String cv : _hmContVars.keySet())
			str = str + cv + " = " + _hmContVars.get(cv) + ", ";
		str = str.concat(" BVars: ");
		for(String bv : _hmBoolVars.keySet())
			str = str + bv + " = " + _hmBoolVars.get(bv) + ", ";
		return str;
	}

	@Override public boolean equals(Object other) {
        boolean result = false;
        if (other instanceof State) {
            State that = (State) other;
            result = (that.canEqual(this) && (that._hmBoolVars.equals(_hmBoolVars)) && that._hmContVars.equals(_hmContVars));
        }
        return result;
    }

    @Override public int hashCode() {
        return mixHash(_hmBoolVars.hashCode(), _hmContVars.hashCode());
    }

    public int mixHash(int a, int b){
    	return ((a + b)*(a+b+1))/2 + a;
    }
    public boolean canEqual(Object other) {
        return (other instanceof State);
    }
}
