package gameObjects;

import java.util.ArrayList;
import java.util.List;

import util.Line;
import util.Point;

public class Collider extends Line {
	public Collider(double x, double y, double x2, double y2) {
		super(x, y, x2, y2);
	}
	public Collider(Point p1, Point p2) {
		super(p1, p2);
	}

	public Collider(Line l) {
		super(l.getP1(), l.getP2());
	}
}
