package gameObjects;

import java.util.ArrayList;
import java.util.List;

import templates.Point;
import templates.Vector;

public class Collider extends Vector {
	public Collider(double x, double y, double x2, double y2) {
		super(x, y, Math.sqrt(Math.pow(x2 - x, 2) + Math.pow(y2 - y, 2)), Math.atan2(y2 - y, x2 - x));
	}
	public Collider(Point p1, Point p2) {
		super(p1.getX(), p1.getY(), Math.sqrt(Math.pow(p2.getX() - p1.getX(), 2) + Math.pow(p2.getY() - p1.getY(), 2)), Math.atan2(p2.getY() - p1.getY(), p2.getX() - p1.getX()));
	}
	

	public Collider shift(double d, double e) {
		Collider c = new Collider(this.origin().getX() + d, this.origin().getY() + e, this.destination().getX() + d, this.destination().getY() + e);
		return c;
	}
}
