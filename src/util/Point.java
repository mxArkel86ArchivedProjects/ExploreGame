package util;

import java.util.function.Function;

public class Point {
	double x;
	double y;

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public void modify(Function<Point, Point> a) {
		Point p = a.apply(this);
		this.x = p.getX();
		this.y = p.getY();
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Point p) {
			return p.getX() == this.getX() && p.getY() == this.getY();
		}
		return false;
	}

}
