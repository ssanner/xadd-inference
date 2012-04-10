package ml;

public class ConvexPolytope {

	private int D;			// dimension of a a point (e.g. vertix)
	private int nv = 0;		// number of vertices (changes in time)
	private double[][] v;	// list of vertices of the polytope
	
	// test if x is an interior point of current polytope
	public boolean contains(double[] x) {
		return true;
	}
	
	// add a new vertix to the list of vertices
	public void addVertix(double[] x) {
		v[nv] = x;
		nv++;
	}
	
	// remove the lastly added vertix from list of vertices
	public void popVertix() {
		nv--;
	}
	
	public ConvexPolytope(int dim, int maxVertices) {
		D = dim;
		v = new double[dim][maxVertices];
	}
	
	static void main(String[] args) {
		
	}

}
