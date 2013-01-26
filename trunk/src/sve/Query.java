/* Query for Symbolic Variable Elimination
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @author Ehsan Abbasnejad
 */
package sve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import camdp.HierarchicalParser;

public class Query {

	public HashMap<String,Boolean> _hmBVarAssign = null;
	public HashMap<String,Double>  _hmCVarAssign = null;
	public ArrayList<String>       _alQueryVars = null;
	public String                  _sFilename = null;
	public HashMap<String,ArrayList<Integer>>   _hmVar2Expansion = null;
	
	public Query(String filename) {
		_sFilename = filename;
		_hmBVarAssign = new HashMap<String,Boolean>();
		_hmCVarAssign = new HashMap<String,Double>();
		_alQueryVars  = new ArrayList<String>();
		_hmVar2Expansion = new HashMap<String,ArrayList<Integer>>(); 
		ArrayList l = HierarchicalParser.ParseFile(filename);
		//System.out.println(l);
		parseQuery(l);
	}
	
	public void parseQuery(ArrayList l) {
		
		// Set up variable index lists (for template expansion)
		Iterator i = l.iterator();
		Object o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("indices")) {
			exit("Missing 'indices' declaration: " + o);
		}
		o = i.next();
		ArrayList indices = (ArrayList<String>) ((ArrayList) o).clone();
		for (Object o2 : indices) {
			ArrayList a2 = (ArrayList)o2;
			String var = (String)a2.get(0);
			Integer low = Integer.parseInt((String)a2.get(2));
			Integer high = Integer.parseInt((String)a2.get(4));
			ArrayList<Integer> indices_array = new ArrayList<Integer>();
			for (int j = low; j <= high; j++)
				indices_array.add(j);
			_hmVar2Expansion.put(var, indices_array);
		}
		
		// Set up continuous variable assignments
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("cevidence")) {
			exit("Missing 'cevidence' declaration: " + o);
		}
		o = i.next();
		ArrayList evidence = (ArrayList<String>) ((ArrayList) o).clone();
		for (Object o2 : evidence) {
			ArrayList a2 = (ArrayList)o2;
			String var = (String)a2.get(0);
			Double assign = Double.parseDouble((String)a2.get(2));
			_hmCVarAssign.put(var, assign);
		}
		
		// Set up boolean variable assignments
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("bevidence")) {
			exit("Missing 'bevidence' declaration: " + o);
		}
		o = i.next();
		evidence = (ArrayList<String>) ((ArrayList) o).clone();
		for (Object o2 : evidence) {
			ArrayList a2 = (ArrayList)o2;
			String var = (String)a2.get(0);
			Boolean assign = Boolean.parseBoolean((String)a2.get(2));
			_hmBVarAssign.put(var, assign);
		}
	
		// Set up query variables
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("query")) {
			exit("Missing query var list (...) declaration: " + o);
		}
		o = i.next();
		_alQueryVars = (ArrayList<String>) ((ArrayList) o).clone();
	}
	
	public void exit(String msg) {
		System.out.println(msg);
		System.exit(1);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Query:\n======\n");
		sb.append("Query variables:     "  + _alQueryVars + "\n");
		sb.append("Boolean evidence:    "  + _hmBVarAssign + "\n");
		sb.append("Continuous evidence: "  + _hmCVarAssign + "\n");
		sb.append("Index Expansions:\n");
		for (Map.Entry<String, ArrayList<Integer>> e : _hmVar2Expansion.entrySet())
			sb.append(" - " + e.getKey() + ": " + e.getValue() + "\n");
		return sb.toString();		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Query q = new Query("./src/sve/test.query");
		System.out.println(q);
	}

}
