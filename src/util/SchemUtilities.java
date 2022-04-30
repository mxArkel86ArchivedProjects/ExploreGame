package util;

public class SchemUtilities {

    public static Point schemToFrame(Point r, Point location, double GRIDSIZE) {
    	/// multiply by GRIDSIZE, subtract camera location
    	return new Point(r.getX() * GRIDSIZE - location.x, r.getY() * GRIDSIZE - location.y);
    }

	public static Line schemToFrame(Line l, Point location, double GRIDSIZE) {
		/// multiply by GRIDSIZE, subtract camera location
		return new Line(schemToFrame(l.getP1(), location, GRIDSIZE),
				schemToFrame(l.getP2(), location, GRIDSIZE));
	}

	public static Rect schemToFrame(Rect r, Point location, double GRIDSIZE) {
		/// multiply by GRIDSIZE, subtract camera location
		Point p_1 = schemToFrame(r.topLeft(), location, GRIDSIZE);
		Point p_2 = new Point(p_1.getX()+r.getWidth()*GRIDSIZE, p_1.getY()+r.getHeight()*GRIDSIZE);
		return new Rect(p_1, p_2);
	}
	
	public static Point roundSchemFrame(Point p, Point location, double GRIDSIZE) {
		Point p2 = SchemUtilities.frameToSchem(p, location, GRIDSIZE);
		return SchemUtilities.schemToFrame(p2, location, GRIDSIZE);
	}

	// public static Rect schemToFrameZ(Rect r, Rect PLAYER_SCREEN_LOC, Point location, double depth, double GRIDSIZE) {
	// 	double z = Math.exp(depth);
    // 	Point p = new Point(
    // 			r.getX() * GRIDSIZE * z - location.x * z - (PLAYER_SCREEN_LOC.getX() * z - PLAYER_SCREEN_LOC.getX())
    // 					+ (r.getWidth() * GRIDSIZE / 2 * z - r.getWidth() * GRIDSIZE / 2)
    // 					- (PLAYER_SCREEN_LOC.getWidth() / 2 * z - PLAYER_SCREEN_LOC.getWidth() / 2),
    // 			r.getY() * GRIDSIZE * z - location.y * z - PLAYER_SCREEN_LOC.getY() * z + PLAYER_SCREEN_LOC.getY()
    // 					+ r.getHeight() * GRIDSIZE * z - r.getHeight() * GRIDSIZE - PLAYER_SCREEN_LOC.getHeight() * z
    // 					+ PLAYER_SCREEN_LOC.getHeight());
    // 	return new Rect(p.x, p.y, r.getWidth() * GRIDSIZE, r.getHeight() * GRIDSIZE);
    // }

	public static Point frameToSchem(Point p, Point location, double GRIDSIZE) {
		return new Point((p.getX() + location.getX()) / GRIDSIZE,
				(p.getY() + location.getY()) / GRIDSIZE);
	}
	
	public static IntPoint frameToSchemInt(Point p, Point location, double GRIDSIZE) {
    	return new IntPoint((int)Math.floor((p.getX() + location.getX()) / GRIDSIZE),
    			(int)Math.floor((p.getY() + location.getY()) / GRIDSIZE));
    }
    
}
