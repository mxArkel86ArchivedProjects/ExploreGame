package util;

public class Rect {
	public double x;
	public double y;
	public double width;
	public double height;
	public Rect(double x, double y, double width, double height){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	public Rect(Point p1, Point p2){
		this.x = Math.min(p1.x, p2.x);
        this.y = Math.min(p1.y, p2.y);
        this.width = Math.abs(p1.x-p2.x);
        this.height = Math.abs(p1.y-p2.y);
	}
	public double getX() {
		return x;
	}
	public double getY() {
		return y;
	}
	public double getWidth() {
		return width;
	}
	public double getHeight() {
		return height;
	}

	public void setX(double x) {
		this.x = x;
	}
	public void setY(double y) {
		this.y = y;
	}
	public void setWidth(double width) {
		this.width = width;
	}
	public void setHeight(double height) {
		this.height = height;
	}
    public Rect extend(double i) {
        return new Rect(this.x-i, this.y-i, this.width+2*i, this.height+2*i);
    }

	public static Rect fromPoints(int x1, int y1, int x2, int y2) {
		int x_ = Math.min(x1, x2);
		int y_ = Math.min(y1, y2);
		return new Rect(x_, y_, Math.abs(x1 - x2), Math.abs(y1 - y2));
	}
}
