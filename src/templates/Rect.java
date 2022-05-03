package templates;

public class Rect {
	private Point topleft;
	private Size size;

	public Rect(double x, double y, double x1, double y1) {
		this.topleft = new Point(x, y);
		this.size = new Size(x1 - x, y1 - y);
	}
	
	public Rect(Point topleft, Size s) {
		this.topleft = topleft;
		this.size = s;
	}

	public static Rect fromXYWH(double x, double y, double width, double height) {
		return new Rect(x, y, x + width, y + height);
	}

	public Rect(Point p1, Point p2) {
		this.topleft = new Point(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()));
		this.size = new Size(Math.abs(p1.getX() - p2.getX()), Math.abs(p1.getY() - p2.getY()));
	}

	public void setP1(Point p) {
		this.topleft = p;
	}

	public void setP2(Point p) {
		this.size = new Size(p.getX() - topleft.getX(), p.getY() - topleft.getY());
	}

	public double getWidth() {
		return size.getWidth();
	}

	public double getHeight() {
		return size.getHeight();
	}

	public void setX(double x) {
		this.topleft.setX(x);
	}

	public void setY(double y) {
		this.topleft.setY(y);
	}

	public void setWidth(double width) {
		this.size.setWidth(width);
	}

	public void setHeight(double height) {
		this.size.setHeight(height);
	}

	public Point topLeft() {
		return topleft;
	}

	public Point topRight() {
		return new Point(topleft.getX() + size.getWidth(), topleft.getY());
	}

	public Point bottomLeft() {
		return new Point(topleft.getX(), topleft.getY()+ size.getHeight());
	}

	public Point bottomRight() {
		return new Point(topleft.getX() + size.getWidth(), topleft.getY() + size.getHeight());
	}

	public Point center() {
		return new Point(topleft.getX() + size.getWidth() / 2, topleft.getY() + size.getHeight() / 2);
	}

	public double bottom() {
		return topleft.getY() + size.getHeight();
	}

	public double top() {
		return topleft.getY();
	}

	public double left() {
		return topleft.getX();
	}

	public double right() {
		return topleft.getX() + size.getWidth();
	}

    public Size getSize() {
        return size;
    }
}
