package util;

import org.javatuples.Pair;

import templates.Vector;
import templates.Point;
import templates.Size;

public class MathUtil {
	public static double dotProduct(Point v1, Point v2) {
		return v1.getX() * v2.getX() + v1.getY() * v2.getY();
	}

	public static Vector getPerpendicularVector(Point p1, Vector v) {
		return new Vector(p1.getX(), p1.getY(), v.getMagnitude(), v.getAngle() + Math.PI / 2);
	}

	//check if a vector is facing the same direction as another vector
	public static boolean isFacing(Vector v1, Vector v2) {
		return dotProduct(v1.directionComponent(), v2.directionComponent()) > 0;
	}

	//check if a vector is facing the opposite direction as another vector
	public static boolean isNotFacing(Vector v1, Vector v2) {
		return dotProduct(v1.directionComponent(), v2.directionComponent()) < 0;
	}

	public static Vector getAverageVector(Vector a, Vector b) {
		double angle1 = MathUtil.clipAngle(a.getAngle());
		double angle2 = MathUtil.clipAngle(b.getAngle());
		double angle = ((angle1 + angle2) / 2) % Math.PI + Math.PI;

		double mag1 = a.getMagnitude();
		double mag2 = b.getMagnitude();

		Point pavg = new Point((a.origin().getX() + b.origin().getX()) / 2,
				(a.origin().getY() + b.origin().getY()) / 2);
		return new Vector(pavg.getX(), pavg.getY(), (mag1 + mag2) / 2, angle);
	}
	
	//get point where a sphere is tangent to two lines
	public static Point getTangentPoint(Point p1, double r, Vector v1, Vector v2) {
		Vector avgv = getAverageVector(v1, v2);

		double A = avgv.getMagnitude();
		Point Bz = MathUtil.ClosestPointOnLine(v1, avgv.destination());
		double CBz = avgv.destination().distance(Bz);
		double dist = (A * r) / CBz;
		Point newpt = avgv.PointOnLine(dist / avgv.getMagnitude());
		return newpt;
	}

	public static double crossProduct(Point v1, Point v2) {
		return v1.getX() * v2.getY() - v1.getY() * v2.getX();
	}
	
	public static Vector parallelProjection(Vector U, Vector V) {
		double dot = dotProduct(U.directionComponent(), V.directionComponent());
		double mag = V.getMagnitude();
		double angle = V.getAngle();
		return new Vector(U.getX(), U.getY(), dot / mag, angle);
	}

	public static boolean isParallel(Vector v1, Vector v2) {
		return Math.abs(dotProduct(v1.directionComponent(), v2.directionComponent())) == v1.getMagnitude() * v2.getMagnitude();
	}

	public static boolean isAntiParallel(Vector v1, Vector v2) {
		return Math.abs(dotProduct(v1.directionComponent(), v2.directionComponent())) == -v1.getMagnitude() * v2.getMagnitude();
	}

	public static Vector perpendicularProjection(Vector U, Vector V) {
		return new Vector(new Point(U.getX(), U.getY()), U.subtractVector(parallelProjection(U, V).getDirectionVector()));
	}

	public static double round(double value, int places) {
		double factor = Math.pow(10, places);
		return Math.round(value * factor) / factor;
	}

	public static double getAngle(Vector v1, Vector v2) {
		return Math.acos(dotProduct(v1.directionComponent(), v2.directionComponent()) / (v1.getMagnitude() * v2.getMagnitude()));
	}

	public static Point getIntersection(Vector v1, Vector v2) {
		double dx1 = v1.getDX();
		double dy1 = v1.getDY();
		double dx2 = v2.getDX();
		double dy2 = v2.getDY();

		double x1 = v1.getX();
		double y1 = v1.getY();
		double x2 = v2.getX();
		double y2 = v2.getY();

		double angle1 = v1.getAngle();
		double angle2 = v2.getAngle();

		//check if lines are perpendicular
		if (Math.abs(Math.cos(angle1 - angle2)) < 0.00001) {
			return null;
		}

		double denom = (dy1 * dx2 - dx1 * dy2);
		if (denom == 0) {
			return null;
		}
		double x = (dx1 * (y2 - y1) + dy1 * (x1 - x2)) / denom;
		double y = (dy2 * (x1 - x2) - dx2 * (y1 - y2)) / denom;
		
		return new Point(x, y);
	}

    public static double clipAngle(double r) {
    	while (r > 2 * Math.PI) {
    		r -= Math.PI * 2;
    	}
    	while (r < 0) {
    		r += Math.PI * 2;
    	}
    	return r;	
    }

	public static Pair<String, Point> PointOnScreenEdge(double angle, Point start_point, Size screen) {
		double x0 = start_point.getX();
		double y0 = start_point.getY();

		double slope = Math.tan(angle);
		int dx = (int) Math.copySign(1, Math.cos(angle));
		int dy = (int) Math.copySign(1, Math.sin(angle));

		double xf_0 = (x0 + (y0) / slope);
		double xf_H = (x0 + (y0 - (int) screen.getHeight()) / slope);
		double yf_0 = (y0 + (x0) * slope);
		double yf_W = (y0 + (x0 - (int) screen.getWidth()) * slope);

		if (xf_0 > 0 && xf_0 < (int) screen.getWidth() && dy == 1) {
			return new Pair<String, Point>("top", new Point((int) xf_0, 0));
		} else if (xf_H > 0 && xf_H < (int) screen.getWidth() && dy == -1) {
			return new Pair<String, Point>("bottom", new Point((int) xf_H, (int) screen.getHeight()));
		}
		if (yf_0 > 0 && yf_0 < (int) screen.getHeight() && dx == -1) {
			return new Pair<String, Point>("left", new Point(0, (int) yf_0));
		} else if (yf_W > 0 && yf_W < (int) screen.getHeight() && dx == 1) {
			return new Pair<String, Point>("right", new Point((int) screen.getWidth(), (int) yf_W));
		}
		return new Pair<String, Point>("none", new Point(0, 0));
	}
	
	public static double shortestDistanceBetweenTwoLines(Vector l1, Vector l2) {
		Point a = l1.origin();
		Point b = l1.destination();
		Point c = l2.origin();
		Point d = l2.destination();

		double d1 = Double.MAX_VALUE;
		double d2 = Double.MAX_VALUE;
		double d3 = Double.MAX_VALUE;
		double d4 = Double.MAX_VALUE;

		if (l1.getMagnitude() != 0) {
			d1 = ClosestPointOnLine(l1, c).distance(c);
			d2 = ClosestPointOnLine(l1, d).distance(d);
		}
		if (l2.getMagnitude() != 0) {
			d3 = ClosestPointOnLine(l2, a).distance(a);
			d4 = ClosestPointOnLine(l2, b).distance(b);
		}
		double dist = Math.min(Math.min(d1, d2), Math.min(d3, d4));
		return dist==Double.NaN?0:dist;
	}

	public static Point ClosestPointOnLine(Vector line, Point sphereCenter) {
		double x1 = line.origin().getX();
		double y1 = line.origin().getY();
		double x2 = line.destination().getX();
		double y2 = line.destination().getY();
		double x3 = sphereCenter.getX();
		double y3 = sphereCenter.getY();
		double x4 = x2 - x1;
		double y4 = y2 - y1;
		double t = ((x3 - x1) * x4 + (y3 - y1) * y4) / (x4 * x4 + y4 * y4);
		if (t < 0) {
			t = 0;
		} else if (t > 1) {
			t = 1;
		}
		double closestX = x1 + t * x4;
		double closestY = y1 + t * y4;
		return new Point(closestX, closestY);
	}

	public static Vector extendLine(Vector line, double d) {
		//extend both sides of the line into length d
		Point center = line.getCenter();
		double length = line.getMagnitude();
		double scale = d / length / 4;
		double angle1 = Math.atan2(line.getCenter().getY() - line.origin().getY(), line.getCenter().getX() - line.origin().getX());

		Point p1 = new Point(center.getX() + scale * Math.cos(angle1), center.getY() + scale * Math.sin(angle1));
		Point p2 = new Point(center.getX() - scale * Math.cos(angle1), center.getY() - scale * Math.sin(angle1));
		return Vector.fromPoints(p2, p1);
	}

	public static Vector extendLineFromFirstPoint(Vector line, double d) {
		//extend both sides of the line into length d
		Point first = line.origin();
		double length = line.getMagnitude();
		double angle1 = line.getAngle();

		Point last = new Point(first.getX() + d * Math.cos(angle1), first.getY() + d * Math.sin(angle1));
		return Vector.fromPoints(first, last);
	}
}
