package ml;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Iterator;

public class DataReader {
	
	private double[][] _x;	// array of input data
	private double[] _y;	// array of targets
	private int N = 0, D = 0;	// N = number of records; D = input data dimension
	private String _fileName;
	
	// getters
	public double x(int i, int j) { return _x[i][j]; }
	public double[] x(int i) { return _x[i]; }
	public double y(int i) { return _y[i]; }
	public int nRows() { return N; }
	public int xDim() { return D; }
	public String fileName() { return _fileName; }
	
	
	public double[] wxy(int i) {
		double[] wxy_i = new double[D+1];
		wxy_i[0] = y(i);
		for (int j=1; j<D+1; j++) wxy_i[j] = x(i,j-1) * y(i);
		return wxy_i; 
	}
	
	private void readData() {
		// read all input data to linked list data
		LinkedList<double[]> data = new LinkedList<double[]>();
		try {             
			BufferedReader reader = new BufferedReader( new FileReader(_fileName));             
			String strLine = null;                                
			while((strLine = reader.readLine()) != null) {                              
				String[] nums = strLine.split(",");
				if (D == 0) D = nums.length;
				if (D != nums.length)
					System.out.println("Wrong data dimension at line " + (N+1));
				else {
					double[] row = new double[D];
					for (int j=0; j<nums.length; j++)
						row[j] = Double.parseDouble(nums[j]);
					data.add(row);
					N++;
				}
			}
		}
		catch (IOException e) { 
			N = 0;
		} 
		
		if (N>0)
		{
			// assign data to array x,y
			D = D - 1;
			_x = new double[N][D];
			_y = new double[N];
			int i = 0;
			Iterator<double[]> itr = data.iterator();
			while (itr.hasNext()) {
				double[] row = itr.next();
				for (int j=0; j<D; j++) 
					_x[i][j] = row[j];
				_y[i] = row[D];
				if (_y[i] <= 0) _y[i] = -1;	//forces y[i] = -1 or 1
				if (_y[i] > 0) _y[i] = 1;
				i++;
			}
		}
	}
	
	public DataReader(String dataFileName) {
		_fileName = dataFileName;
		readData();
	}

	public static void main(String[] args) {
		DataReader dr = new DataReader("./src/ml/data2.txt");
		System.out.println("Successfully read " + dr.nRows() + " rows from: " + dr.fileName());
	}

}
