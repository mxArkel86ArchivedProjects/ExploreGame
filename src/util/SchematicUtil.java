package util;


import main.AppConstants;
import templates.IntPoint;
import templates.Point;
import templates.Rect;
import templates.Vector;

public class SchematicUtil {

    public static Point schemToFrame(Point r, Point location) {
    	/// multiply by GRIDSIZE, subtract camera location
    	return new Point(r.getX() * AppConstants.PIXELS_PER_GRID() - location.getX(), r.getY() * AppConstants.PIXELS_PER_GRID() - location.getY());
    }

	public static Vector schemToFrame(Vector l, Point location) {
		/// multiply by GRIDSIZE, subtract camera location
		return Vector.fromPoints(schemToFrame(l.origin(), location),
				schemToFrame(l.destination(), location));
	}

	public static Rect schemToFrame(Rect r, Point location) {
		/// multiply by GRIDSIZE, subtract camera location
		Point p_1 = schemToFrame(r.topLeft(), location);
		Point p_2 = new Point(p_1.getX()+r.getWidth()*AppConstants.PIXELS_PER_GRID(), p_1.getY()+r.getHeight()*AppConstants.PIXELS_PER_GRID());
		return new Rect(p_1, p_2);
	}
	
	public static Point roundSchemFrame(Point p, Point location) {
		IntPoint p2 = SchematicUtil.frameToSchemInt(p, location);
		return SchematicUtil.schemToFrame(p2.DPoint(), location);
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

	public static Point frameToSchem(Point p, Point location) {
		return new Point((p.getX() + location.getX()) / AppConstants.PIXELS_PER_GRID(),
				(p.getY() + location.getY()) / AppConstants.PIXELS_PER_GRID());
	}
	
	public static IntPoint frameToSchemInt(Point p, Point location) {
    	return new IntPoint((int)Math.floor((p.getX() + location.getX()) / AppConstants.PIXELS_PER_GRID()),
    			(int)Math.floor((p.getY() + location.getY()) / AppConstants.PIXELS_PER_GRID()));
    }
    
}
