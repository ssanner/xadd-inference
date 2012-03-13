package camdp;

public class Obstacle {
	//consider points are the coordinates of an obsticle
	private double xpoint1;
	private double xpoint2;
	private double ypoint1;
	private double ypoint2;
	//2points represent a line
	private int penalty; // reward for hitting/running over obsticle

	
	public Obstacle() {
		xpoint1=0;
		xpoint2=20;
		ypoint1=0;
		ypoint2=20;
		penalty = 0;
	}

	public double getXpoint1() {
		return xpoint1;
	}

	public void setXpoint1(double xpoint1) {
		this.xpoint1 = xpoint1;
	}

	public double getXpoint2() {
		return xpoint2;
	}

	public void setXpoint2(double xpoint2) {
		this.xpoint2 = xpoint2;
	}

	public double getYpoint1() {
		return ypoint1;
	}

	public void setYpoint1(double ypoint1) {
		this.ypoint1 = ypoint1;
	}

	public double getYpoint2() {
		return ypoint2;
	}

	public void setYpoint2(double ypoint2) {
		this.ypoint2 = ypoint2;
	}

	public int getPenalty() {
		return penalty;
	}

	public void setPenalty(int penalty) {
		this.penalty = penalty;
	}
	
	
	
}
