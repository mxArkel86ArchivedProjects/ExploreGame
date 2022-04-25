package gameObjects;

import util.Rect;

public class Collider extends Rect {
	public Collider(double x, double y, double width, double height) {
		super(x, y, width, height);
	}
	public Collider(Rect r){
		super(r.x, r.y, r.width, r.height);
	}

}
