package util;

public class Rect {
	Point p1;
	Point p2;

	public Rect(double x, double y, double x1, double y1) {
		this.p1 = new Point(x, y);
		this.p2 = new Point(x1, y1);
	}

	public Rect(Point p1, Point p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	public void setP1(Point p) {
		this.p1 = p;
	}

	public void setP2(Point p) {
		this.p2 = p;
	}

	public double getWidth() {
		return Math.abs(p1.getX() - p2.getX());
	}

	public double getHeight() {
		return Math.abs(p1.getY() - p2.getY());
	}

	public void setX(double x) {
		double w = getWidth();
		p1.setX(x);
		p2.setX(x + w);
	}

	public void setY(double y) {
		double h = getHeight();
		p1.setY(y);
		p2.setY(y + h);
	}

	public void setWidth(double width) {
		p2.setX(p1.getX() + width);
	}

	public void setHeight(double height) {
		p2.setY(p1.getY() + height);
	}

	public Point topLeft() {
		return new Point(p1.getX(), p1.getY());
	}

	public Point topRight() {
		return new Point(p2.getX(), p1.getY());
	}

	public Point bottomLeft() {
		return new Point(p1.getX(), p2.getY());
	}

	public Point bottomRight() {
		return new Point(p2.getX(), p2.getY());
	}

	public Point center() {
		return new Point(p1.getX() + getWidth() / 2, p1.getY() + getHeight() / 2);
	}

	public double bottom() {
		return p2.getY();
	}

	public double top() {
		return p1.getY();
	}

	public double left() {
		return p1.getX();
	}

	public double right() {
		return p2.getX();
	}

	// public Rect extend(double i) {
	//     return new Rect(this.x-i, this.y-i, this.width+2*i, this.height+2*i);
	// }


	public static Rect fromPoints(double x1, double y1, double x2, double y2) {
		double x_ = Math.min(x1, x2);
		double y_ = Math.min(y1, y2);
		return new Rect(x_, y_, Math.abs(x1 - x2), Math.abs(y1 - y2));
	}
}
