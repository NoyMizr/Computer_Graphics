package edu.cg.scene.objects;

import edu.cg.UnimplementedMethodException;
import edu.cg.algebra.Hit;
import edu.cg.algebra.Ops;
import edu.cg.algebra.Point;
import edu.cg.algebra.Ray;

// TODO Implement this class which represents a sphere
public class Sphere extends Shape {
	private Point center;
	private double radius;

	public Sphere(Point center, double radius) {
		this.center = center;
		this.radius = radius;
	}

	public Sphere() {
		this(new Point(0, -0.5, -6), 0.5);
	}

	@Override
	public String toString() {
		String endl = System.lineSeparator();
		return "Sphere:" + endl +
				"Center: " + center + endl +
				"Radius: " + radius + endl;
	}


	public Sphere initCenter(Point center) {
		this.center = center;
		return this;
	}

	public Sphere initRadius(double radius) {
		this.radius = radius;
		return this;
	}

	@Override
	public Hit intersect(Ray ray) {
		// TODO Implement:
//		throw new UnimplementedMethodException("edu.cg.scene.object.Sphere.intersect()");
		Boolean thereTwoSolution = false;
		Boolean thereOneSolution = false;
		Hit solution = null;
		double a = ray.direction().lengthSqr(); // get (||v||)
		double b = 2 * ray.direction().dot(ray.source().sub(center));
		double c = ray.source().sub(center).lengthSqr() - (radius * radius);
		double solutions = (b*b) - (4 * a * c);
		if (solutions > 0) {
			double firstSolution = (-b - Math.sqrt(solutions)) / (2 * a);
			double secondSolution = (-b + Math.sqrt(solutions)) / (2 * a);
			if (firstSolution == secondSolution) {
				thereOneSolution = true;
			} else {
				thereTwoSolution = true;
			}
			if (thereOneSolution) {
				if (firstSolution < Ops.epsilon) {
					solution =  null;
				} else {
					solution = new Hit(firstSolution, ray.add(firstSolution).sub(center).normalize()).setWithin();
				}
			} else if (thereTwoSolution) {
				if (secondSolution > Ops.epsilon) {
					if (firstSolution <= Ops.epsilon) {
						solution = new Hit(secondSolution, ray.add(secondSolution).sub(center).normalize()).setWithin();
					} else {
						solution = new Hit(firstSolution, ray.add(firstSolution).sub(center).normalize());
					}
				}
			}
		}
		return solution;
	}
}
