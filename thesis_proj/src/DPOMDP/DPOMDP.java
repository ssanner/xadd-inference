//////////////////////////////////////////////////////////////////////////
//
// File:     MDP.java
// Author:   Scott Sanner, University of Toronto (ssanner@cs.toronto.edu)
// Date:     9/1/2003
//
// Description:
//
//   An MDP inference package that uses both Tables, ADDs, AADDs as the
//   underlying computational mechanism.  (All via the logic.add.FBR
//   interface.)  See SPUDD (Hoey et al, UAI 1999) for more details on the
//   algorithm.
//
//////////////////////////////////////////////////////////////////////////

// Package definition
package DPOMDP;

// Packages to import
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

// DD & FBR interfaces

/**
 * Main CPOMDP inference class
 * 
 * @version 1.0
 * @author Zahra Zamani
 **/
public class DPOMDP{

	
	/* Constants */
	public final  int[] state = new int[2]; //false = low, true = high, //0=flow, 1=pressure
	public final  int[] obs= new int[2]; //false = low, true = high 
	public final int[] action = new int[2]; //0=continue plant, 1 = stop action
	//public final static boolean actionp=false;
	
	public static double[][][] transition = new double[2][2][2];
	public static double[][][] observation = new double[2][2][2];
	public static int[][] reward = new int[2][2];
	public static double[] belief = new double[2];
	public final static boolean REDUCE_LP = true;
	public final static double discount=1.0;
	public ArrayList<GammaSet> unionGamma; // for each horizon, keep xadd_id  
	
	
	////////////////////////////////////////////////////////////////////////////
	// Constructors
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Constructor - filename
	 **/
	public DPOMDP() {
		belief[0]= 0.5;//start with equal belief state  
		belief[1] = 0.5;
		action[0] = 0;//stop plant
		action[1] = 1;//start plant
		state[0] = 0;//low levels
		state[1] = 1;//high levels
		obs[0] = 0;//low level observation
		obs[1] = 1;//high level observation
		//transition s,a,s'
		transition[0][0][0] = 0.9;transition[0][0][1] = 0.1; //when plant is shut down, levels don't go up
		transition[0][1][1] = 0.9;transition[0][1][0] = 0.1; // when plant is working, the levels go up
		transition[1][0][0] = 0.5;transition[1][0][1] = 0.5; //stoping a plant on high levels can decrease levels, or not
		transition[1][1][0] = 0.1;transition[1][1][1] = 0.9; // working on high levels, does not cause decrease in levels
		//observation o,s,a
		observation[0][0][0] = 0.8;observation[0][0][1] = 0.2; //noisy state when plant is shut down
		observation[0][1][0] = 0.6;observation[0][1][1] = 0.4; // much more noise when plant is working (obs=low means actually state=low)
		observation[1][0][0] = 0.2;observation[1][0][1] = 0.8;
		observation[1][1][1] = 0.6;observation[1][1][0] = 0.4;
		//reward s,a
		//reward of shutting down is big, but continuing with high pressure is worst
		reward[0][0] = -10;reward[0][1] = 0 ;reward[1][0] = 0; reward[1][1] = -50;
		unionGamma = new ArrayList<GammaSet>();
		int maxIter = 4;
		int totalNumberAlpha =0;
			//compute alpha-vector for state s
		for(int h=0;h<=maxIter;h++)
		{
			computeGammaSet(h);
			if (getTotalAlphaHorizon(h)>1)
			{
				//before pruning, set total number of alpha vectors 
				totalNumberAlpha = unionGamma.get(h).get_VectorForAction_Observation(0, 0).getAlphaSize();
				//SingleAlphaV allalpha = new SingleAlphaV();
				//int allalphaSize = unionGamma.get(h).get_VectorForAction_Observation(0, 0).getAlphaSize();
				//SingleAlphaV compare = new SingleAlphaV();
				//int compareSize = unionGamma.get(h).get_VectorForAction_Observation(0, 0).getAlphaSize();
				boolean endLoop = false,breakIter = false;
				while (!endLoop)
				{
					for (int j=0; j<=unionGamma.get(h).get_VectorForAction_Observation(0, 0).getAlphaMaxIndex();j++)
					{
					//just check for vector dominance here
						if (unionGamma.get(h).get_VectorForAction_Observation(0, 0).get_alphaVector(j) == null) 
							continue;
						breakIter = false;
						double[] tempj = unionGamma.get(h).get_VectorForAction_Observation(0, 0).get_alphaVector(j); 
						for (int k=0; k<=unionGamma.get(h).get_VectorForAction_Observation(0, 0).getAlphaMaxIndex();k++)
						{
							if (unionGamma.get(h).get_VectorForAction_Observation(0, 0).get_alphaVector(k) == null) 
									continue;

							double[] temp = unionGamma.get(h).get_VectorForAction_Observation(0, 0).get_alphaVector(k); 
							if ((tempj[0] >= temp[0])
								&&(tempj[1] >= temp[1])
									&& (k!=j))
							{
								unionGamma.get(h).remove_alpha(temp);
								breakIter = true;
								
								break;
							}
						}
						
						if (breakIter) break;
						else if (j==unionGamma.get(h).get_VectorForAction_Observation(0, 0).getAlphaMaxIndex()) endLoop = true;
					}
				}
			}
			
			//output set of alpha-vectors of result + their policy
				SingleAlphaV allalpha  = unionGamma.get(h).get_VectorForAction_Observation(0, 0);
				System.out.println("End of iteration "+ h + " - List of dominated alpha-vectors:");
				double[] policy = new double[2];
				double[] alphas = new double[4];
				for (Entry<Integer, double[]> entry: unionGamma.get(h).get_VectorForAction_Observation(0, 0).get_alphaVector().entrySet())
				{
					policy = allalpha.get_alphaVector(entry.getKey());
					alphas = traceObservationAlpha(h, entry.getKey(), tracePolicy(h, entry.getKey(),totalNumberAlpha));
					System.out.println("["+policy[0]+","+ policy[1]+"]");
					System.out.println("The policy tree for this vector: action " + tracePolicy(h, entry.getKey(), totalNumberAlpha)+ " crossSum of alpha's" + 
						"["+alphas[0]+","+ alphas[1]+"] of Observation 1 and ["
						+alphas[2]+","+ alphas[3]+"] of Observation 2 \n");
				}
			}
		}
	
	//alpha= set of value functions over each state: alpha_p = <V_p(s_1),...V_p(s_n)> for a certain policy p
	//each V_p(s_1) is a scalar , and |V_p| depends on the following:
	//V_p(b) = b.alpha_p which can be tested for domination
	//then we take the maximum policy for each horizon V(b) = max_p alpha_p.b
	private void computeGammaSet(int iter) 
	{
		SingleAlphaV currentAlphas = new SingleAlphaV();
		AlphaVector alpha = new AlphaVector();
		GammaSet tempGamma = new GammaSet();
		GammaSet crossSumGamma = new GammaSet();
		if (iter==0)
		{ 
			//put reward(s,a=0) as the first alpha
			//no observation in first horizon
			// maximum of two action values
			double[] temp_rwd = new double[2];
			temp_rwd[0]=reward[0][0];
			temp_rwd[1]=reward[1][0];
			currentAlphas.addAlphaVector(temp_rwd);
			alpha.set_hmObsAlphaSet(currentAlphas, 0);
			
			tempGamma.set_AlphasForAction(1, alpha);
			temp_rwd = new double[2];
			temp_rwd[0]=reward[0][1];
			temp_rwd[1]=reward[1][1];
			currentAlphas = new SingleAlphaV();
			currentAlphas.addAlphaVector(temp_rwd);
			alpha = new AlphaVector();
			//this is the cross sum so put it in the cross sum
			alpha.set_hmObsAlphaSet(currentAlphas,0);
			tempGamma.set_AlphasForAction(2, alpha);
			unionGamma.add(0, tempGamma.union());
			
		}
		else 
		{

			tempGamma = new GammaSet();
			double[] tempRstate= new double[2];
			for (int a=0;a<action.length;a++) 
			{
				alpha = new AlphaVector();
				for (int o=0;o<state.length;o++) 
				{
					//j comes from alpha_j^h which is <1...|HV^h|> and |HV^h|=Result of union G over action
					currentAlphas = new SingleAlphaV();
					double[] tempj = new double[2];
					for (Entry<Integer, double[]> entry: unionGamma.get(iter-1).get_VectorForAction_Observation(0, 0).get_alphaVector().entrySet())
					{
						tempj = unionGamma.get(iter-1).get_VectorForAction_Observation(0, 0).get_alphaVector(entry.getKey());
						tempRstate= new double[2];
						for (int s=0;s<state.length;s++)
						{
							int sum=0;
							for (int ss=0;ss<state.length;ss++)
							{
								sum += transition[s][a][ss] * observation[o][ss][a]* tempj[ss] ;
							
							}
							tempRstate[s] =  0.5 *reward[s][a] + sum;
						}
						//doesn't add to hashset??
						currentAlphas.set_alphaVector(entry.getKey(), tempRstate);
						//j.remove();
					}
					//for each observation keep a separate set of gamma's
					alpha.set_hmObsAlphaSet(currentAlphas, o+1);
				}
			
		
			//take cross-sum of all currentGamma objects
			//assume here for 2 observations only
			SingleAlphaV o1 = alpha.get_hmObsAlphaSet(1);
			SingleAlphaV o2= alpha.get_hmObsAlphaSet(2);
			SingleAlphaV o0 = new SingleAlphaV();
			double[] tempi,tempk = new double[2];
			for (Entry<Integer, double[]> i: o1.get_alphaVector().entrySet())
			{
				tempi = o1.get_alphaVector(i.getKey());
				for (Entry<Integer, double[]> k: o2.get_alphaVector().entrySet())
				{
					tempk = o2.get_alphaVector(k.getKey());
					tempRstate= new double[2];
					for (int s=0;s<state.length;s++)
						tempRstate[s]  =  tempi[s]+ tempk[s];
						//keep sums in obs-0, why do we do a cross-sum here? It ruins the obs. number
					o0.addAlphaVector(tempRstate);
				}
			}
			alpha.set_hmObsAlphaSet(o0, 0);
			crossSumGamma.set_AlphasForAction(a+1,alpha);
		}
			//Union of all _alpha's just add it to the set?
			unionGamma.add(iter, crossSumGamma.union());
		}	
		
	}


	public int getTotalAlphaHorizon(int h)
	{
		int counter=0;
		//if (h==0)
			counter = unionGamma.get(h).getAlphaSizeForAction(0);
		//else
			//for (int a=0;a<unionGamma.get(h).get_hmActionAlphas().size();a++)
			//counter+= unionGamma.get(h).getTotalAlphaActionObs(a);
		return counter;
	}
	
	public int tracePolicy(int h,int index,int total)
	{
		int action1=0;
		//half of the array is for action1, the rest for action 2
		if (index>=total/2)
			action1 = 2;
		else action1 =1;
		return action1;
	}
	
	public double[] traceObservationAlpha(int h,int index,int a)
	{
		SingleAlphaV alpha = new SingleAlphaV();
		//half of the array is for action1, the rest for action 2
		if (a==2)
			index = (index%2);
		if (h==0) alpha = unionGamma.get(h).get_VectorForAction_Observation(0, 0);
		else alpha = unionGamma.get(h).get_VectorForAction_Observation(a, 1);
		if (a==2)
			index = (index%(unionGamma.get(h).getAlphaSizeForAction(a)));
		//now find which crossSum lead to this: index/J = first element of o1 and indexMODJ = second element from o2
		int jIndex= alpha.getAlphaSize();
		int[] obs = new int[2];
		obs[0] =	index / jIndex;
		obs[1] = index % jIndex;
		double[] alphas = new double[4];
		if (h==0)
		{
			for (int i=0;i<2;i++)
				alphas[i] = alpha.get_alphaVector(0)[i];
			for (int i=0;i<2;i++)
				alphas[i+2] = alpha.get_alphaVector(1)[i];
		}
		else 
		{
			int[] map = new int[jIndex];
			int counter=0;
			for (Entry<Integer, double[]> k: unionGamma.get(h).get_VectorForAction_Observation(a, 1).get_alphaVector().entrySet())
				map[counter++] = k.getKey();
				
			for (int i=0;i<2;i++)
				alphas[i] = unionGamma.get(h).get_VectorForAction_Observation(a, 1).get_alphaVector(map[obs[0]])[i];
			for (int i=0;i<2;i++)
				alphas[i+2] = unionGamma.get(h).get_VectorForAction_Observation(a, 2).get_alphaVector(map[obs[1]])[i];
			
		}
			return alphas;
	}

	public static void main(String args[]) {
		DPOMDP mdp1 = new DPOMDP();

	}
}
