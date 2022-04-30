package gameObjects;

import util.Line;

public class Collider extends Line {
	public Collider(double x, double y, double x2, double y2) {
		super(x, y, x2, y2);
	}
	public Collider(Line l){
		super(l.getP1(), l.getP2());
	}

}
