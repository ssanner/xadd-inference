package DPOMDP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class GammaSet {
		
		//for each action, keep a hashmap of observation-alphavectors
		private HashMap<Integer,AlphaVector> _hmActionAlphas;		
		public GammaSet(){
			_hmActionAlphas = new HashMap<Integer, AlphaVector>();
			//keep element 0 for union
			AlphaVector temp = new AlphaVector();
			_hmActionAlphas.put(0,temp);
		}
		
		public GammaSet( HashMap<Integer, AlphaVector> a)
		{	
			_hmActionAlphas = a;
		}

		public HashMap<Integer, AlphaVector> get_hmActionAlphas() {
			return _hmActionAlphas;
		}

		public void set_hmActionAlphas(
				HashMap<Integer, AlphaVector> _hmActionAlphas) {
			this._hmActionAlphas = _hmActionAlphas;
		}
		//get and set for specific action
		public void set_AlphasForAction(int a,
				AlphaVector _Alphas) {
			this._hmActionAlphas.put(a, _Alphas);
		}
		public AlphaVector get_AlphasForAction(int a) {
			return _hmActionAlphas.get(a);
		}
		//get number of alphaVectors for each action
		public int getAlphaSizeForAction(int a) {
			AlphaVector temp = _hmActionAlphas.get(a);
			int counter=0;
			for (int i=0;i<temp.get_hmObsAlphaSet().size();i++)
				counter+= temp.get_hmObsAlphaSet(i).getAlphaSize();
			return counter;
		}
		//get for specific action and specific observation
		public HashSet<double[]> get_AlphaSetForAction_Observation(int a,int o) {
			return _hmActionAlphas.get(a).getAlphaVectorSetForObs(o);
		}
		//get for specific action and specific observation
		public SingleAlphaV get_VectorForAction_Observation(int a,int o) {
			return _hmActionAlphas.get(a).get_hmObsAlphaSet(o);
		}
		//get for specific action and specific observation,specific J
		public double[] get_VectorForAction_Observation(int a,int o,int j) {
			return _hmActionAlphas.get(a).getAlphaVectorForObs_j(o, j);
		}

		public GammaSet union() {
			// TODO Auto-generated method stub
			double[]temp = new double[2];
			AlphaVector newVector = new AlphaVector();
			SingleAlphaV eachAlpha = new SingleAlphaV();
			for (int a=1;a<_hmActionAlphas.size();a++)
				for (int j=0;j<_hmActionAlphas.get(a).getTotalAlphaNumbersObs(0);j++)
				{
					temp = _hmActionAlphas.get(a).getAlphaVectorForObs_j(0, j);
					eachAlpha.addAlphaVector(temp);
				}
			newVector.set_hmObsAlphaSet(eachAlpha, 0);
			_hmActionAlphas.put(0, newVector);
			return this;
		}
		
		public void remove_alpha(double[] value) {
			// TODO Auto-generated method stub
			_hmActionAlphas.get(0).get_hmObsAlphaSet(0).removeAlphaVector(value);
		}
		
}
