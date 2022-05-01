package util;

public class Rect {
	Point topleft;
	Size size;

	public Rect(double x, double y, double x1, double y1) {
		this.topleft = new Point(x, y);
		this.size = new Size(x1 - x, y1 - y);
	}

	public static Rect fromXYWH(double x, double y, double width, double height) {
		return new Rect(x, y, x + width, y + height);
	}

	public Rect(Point p1, Point p2) {
		this.topleft = new Point(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y));
		this.size = new Size(Math.abs(p1.x - p2.x), Math.abs(p1.y - p2.y));
	}

	public void setP1(Point p) {
		this.topleft = p;
	}

	public void setP2(Point p) {
		this.size = new Size(p.x - topleft.x, p.y - topleft.y);
	}

	public double getWidth() {
		return size.width;
	}

	public double getHeight() {
		return size.height;
	}

	public void setX(double x) {
		this.topleft.x = x;
	}

	public void setY(double y) {
		this.topleft.y = y;
	}

	public void setWidth(double width) {
		this.size.width = width;
	}

	public void setHeight(double height) {
		this.size.height = height;
	}

	public Point topLeft() {
		return topleft;
	}

	public Point topRight() {
		return new Point(topleft.x + size.width, topleft.y);
	}

	public Point bottomLeft() {
		return new Point(topleft.x, topleft.y + size.height);
	}

	public Point bottomRight() {
		return new Point(topleft.x + size.width, topleft.y + size.height);
	}

	public Point center() {
		return new Point(topleft.x + size.width / 2, topleft.y + size.height / 2);
	}

	public double bottom() {
		return topleft.y + size.height;
	}

	public double top() {
		return topleft.y;
	}

	public double left() {
		return topleft.x;
	}

	public double right() {
		return topleft.x + size.width;
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
