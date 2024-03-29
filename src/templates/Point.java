package templates;

import java.util.function.Function;

public class Point {
	private double x;
	private double y;

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

	public Point scale(double scale) {
		return new Point(x * scale, y * scale);
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public Point shift(double x, double y) {
		Point p = new Point(this.x + x, this.y + y);
		return p;
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

    public double distance(Point p2) {
        return Math.sqrt(Math.pow(p2.getX() - this.getX(), 2) + Math.pow(p2.getY() - this.getY(), 2));
    }

}
