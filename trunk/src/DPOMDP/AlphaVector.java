package DPOMDP;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class AlphaVector{
	
	//for each observation, keep a class of vectors (hashmap with numbers)
	private HashMap <Integer,SingleAlphaV> _hmObsAlphaSet;
	
	public AlphaVector()
	{
		_hmObsAlphaSet = new HashMap<Integer, SingleAlphaV>();
		//keep element 0 for the crossSum
		double[] alpha= new double[2];
		alpha[0]=0.0;
		alpha[1]=0.0;
		SingleAlphaV alphas = new SingleAlphaV();
		alphas.addAlphaVector(alpha);
		_hmObsAlphaSet.put(0, alphas);
	}
	public HashMap<Integer, SingleAlphaV> get_hmObsAlphaSet() {
		return _hmObsAlphaSet;
	}
	public void set_hmObsAlphaSet(HashMap<Integer, SingleAlphaV> _hmObsAlphaSet) {
		this._hmObsAlphaSet = _hmObsAlphaSet;
	}
	
	public SingleAlphaV get_hmObsAlphaSet(int o) {
		return _hmObsAlphaSet.get(o);
	}
	public void set_hmObsAlphaSet(SingleAlphaV alphaSet, int o) {
		this._hmObsAlphaSet.put(o, alphaSet);
	}
	//add alphavector to set of alphaVectors, check for duplicates
	public void setAlphaVectorForObs(int o,double[] value) {
			SingleAlphaV alphaHM = _hmObsAlphaSet.get(o);
			alphaHM.addAlphaVector(value);
	}
	
	public double[] getAlphaVectorForObs_j(int o,int j) {
		SingleAlphaV alphaHM = _hmObsAlphaSet.get(o);
		return alphaHM.get_alphaVector(j);
}
		
	public void setAlphaVectorSetForObs(int o,HashSet<double[]> value) {
		SingleAlphaV alphaHM = _hmObsAlphaSet.get(o);
		double[] temp = new double[2];
		for (Iterator<double[]> i=value.iterator();i.hasNext();)
		{
			temp = i.next();
			alphaHM.addAlphaVector(temp);
		}
	}
	
	public HashSet<double[]> getAlphaVectorSetForObs(int o) {
		SingleAlphaV alphaHM = _hmObsAlphaSet.get(o);
		HashSet<double[]> temp = new HashSet<double[]>();
		for (int i=0;i<alphaHM.getAlphaSize();i++)
			temp.add(alphaHM.get_alphaVector(i));

		return temp;
	}
	
	public int getTotalAlphaNumbersObs(int o)
	{
		if (this._hmObsAlphaSet.get(o)!=null)
			return this._hmObsAlphaSet.get(o).getAlphaSize();
		else return 0;
	}
	
}

