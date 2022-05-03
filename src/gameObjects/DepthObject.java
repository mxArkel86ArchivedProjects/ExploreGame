package gameObjects;

import templates.Rect;

public abstract class DepthObject extends Rect {
	public DepthObject(double x, double y, double width, double height, double depth) {
		super(x, y, x+width, y+height);
		
		this.depth = depth;
	}
	double depth;

	public double getZ() {
		return depth;
	}
}
