package sve;

import java.util.ArrayList;

import cmdp.HierarchicalParser;
import xadd.XADD;

public class SVE {

	public GraphicalModel _gm = null;
	public XADD _context      = null;
	
	public SVE(GraphicalModel gm) {
		_gm = gm;
		_context = _gm._context;
	}

	public int infer(Query q) {
		
		return -1;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//GraphicalModel gm = new GraphicalModel("./src/sve/tracking.gm");
		//Query q = new Query("./src/sve/tracking.query.1");
		GraphicalModel gm = new GraphicalModel("./src/sve/test.gm");
		Query q = new Query("./src/sve/test.query");
		SVE sve = new SVE(gm);
		gm.instantiateGMTemplate(q._hmVar2Expansion);
		int xadd = sve.infer(q);
	}

}
