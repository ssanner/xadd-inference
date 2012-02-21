package DPOMDP;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class SingleAlphaV {
	//this is the index J for each of the vectors
	private HashMap <Integer,double[]> _alphaVector;

	public SingleAlphaV()
	{
		_alphaVector = new HashMap<Integer, double[]>();
	}
	public HashMap<Integer, double[]> get_alphaVector() {
		return _alphaVector;
	}

	public void set_alphaVector(HashMap<Integer, double[]> _alphaVector) {
		this._alphaVector = _alphaVector;
	}

	public double[] get_alphaVector(int av)
	{
		//if (this._alphaVector.containsKey(av))
			return get_alphaVector().get(av);
		//else 
			
	}
	public void set_alphaVector(int av, double[] value)
	{
		get_alphaVector().put(av, value);
	}
	
	public int getAlphaSize()
	{
		return get_alphaVector().size();
	}
	public int getAlphaMaxIndex()
	{
		int maximum = 0;
		for (Entry<Integer, double[]> entry: get_alphaVector().entrySet())
		{
			if (entry.getKey()> maximum)
				maximum = entry.getKey();
		}
		return maximum;
	}
	
	public void addAlphaVector(double[] value)
	{
		double[] temp = new double[2];
		boolean duplicate=false;
		for (int i=0;i<get_alphaVector().size();i++)
		{
			temp = this._alphaVector.get(i);
			if (value.equals(temp))
				duplicate=true;
		}
		if (duplicate==false)
		{
			get_alphaVector().put(get_alphaVector().size(), value);
		}
	}
	
	public void removeAlphaVector(double[] value)
	{
		int key = -1;
		for (Entry<Integer, double[]> entry: get_alphaVector().entrySet())
			if (entry.getValue().equals(value))
				key = entry.getKey();
		if (key!=-1)
		{
			get_alphaVector().remove(key);
		}
	}
}
