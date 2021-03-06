package edu.cg.algebra;

public class Point {
	public double x, y, z;
	
	public Point(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Point(double val) {
		this(val, val, val);
	}
	
	public Point() {
		this(0);
	}
	
	public Point add(double t, Vec v) {
		return Ops.add(this, t, v);
	}
	
	public Point add(Vec v) {
		return Ops.add(this, v);
	}
	
	public Point mult(double a) {
		return Ops.mult(a, this);
	}
	
	public Point mult(Point p) {
		return Ops.mult(this, p);
	}
	
	public Point add(Point p) {
		return Ops.add(this, p);
	}
	
	public double dist(Point other) {
		return Ops.dist(this, other);
	}
	
	public double distSqr(Point other) {
		return Ops.distSqr(this, other);
	}
	
	public Vec sub(Point other) {
		return Ops.sub(this, other);
	}
	
	public Vec toVec() {
		return sub(new Point());
	}
	
	public boolean isFinite() {
		return Ops.isFinite(this);
	}

	// Returns an array representing the point
	public double[] asArray() {
		double ret[] = new double[3];
		ret[0] = x;
		ret[1] = y;
		ret[2] = z;
		return ret;
	}

	// Returns the value in the given coordinates. The value of coordinate is 0,1 or 2 where x=0, y=1 and z=1.
	public double getCoordinate(int coordinate){
		switch (coordinate){
			case 0: return x;
			case 1: return y;
			case 2: return z;
		}
		return Double.POSITIVE_INFINITY;
	}


	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}
}
