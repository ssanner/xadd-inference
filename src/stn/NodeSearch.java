package stn;

import java.util.ArrayList;
import java.util.HashMap;

public class NodeSearch {

	HashMap<String,Double> partialAssignment;
	private int g;  //XADD
	private int H;  //XADD
	private double f_val;
	NodeSearch(HashMap<String,Double> partialAssignment, int g, int H, double f_val){
		this.partialAssignment=partialAssignment;
		this.g=g;
		this.H=H;
		this.f_val=f_val;
	}
	public int getG() {
		return g;
	}
	
	public int getH() {
		return H;
	}
	
	public double getF_val() {
		return f_val;
	}
	
	
	public HashMap<String,Double> getPartialAssignment() {
		return partialAssignment;
	}
	
	public String toString(){
		return "Assign: "+partialAssignment+" gXADD: "+g+" HXADD: "+ H+" f_val: "+f_val;
	}
	
	
	
	
}