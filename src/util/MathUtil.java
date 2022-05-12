package util;

import org.javatuples.Pair;

import templates.CalcVector;
import templates.Line;
import templates.Point;
import templates.Size;

public class MathUtil {
	public static double dotProduct(CalcVector v1, CalcVector v2) {
		return v1.getDX() * v2.getDX() + v1.getDY() * v2.getDY();
	}

	public static CalcVector getPerpendicularVector(Point p1, CalcVector v) {
		return new CalcVector(p1.getX(), p1.getY(), v.getMagnitude(), v.getAngle() + Math.PI / 2);
	}

	//check if a vector is facing the same direction as another vector
	public static boolean isFacing(CalcVector v1, CalcVector v2) {
		return dotProduct(v1, v2) > 0;
	}

	//check if a vector is facing the opposite direction as another vector
	public static boolean isNotFacing(CalcVector v1, CalcVector v2) {
		return dotProduct(v1, v2) < 0;
	}

	public static CalcVector getAverageVector(CalcVector a, CalcVector b) {
		double angle1 = MathUtil.clipAngle(a.getAngle());
		double angle2 = MathUtil.clipAngle(b.getAngle());
		double angle = ((angle1 + angle2) / 2) % Math.PI + Math.PI;

		double mag1 = a.getMagnitude();
		double mag2 = b.getMagnitude();

		Point pavg = new Point((a.origin().getX() + b.origin().getX()) / 2,
				(a.origin().getY() + b.origin().getY()) / 2);
		return new CalcVector(pavg.getX(), pavg.getY(), (mag1 + mag2) / 2, angle);
	}

	public static Line vectorToLine(CalcVector v) {
		return new Line(v.origin(), v.destination());
	}
	
	//get point where a sphere is tangent to two lines
	public static Point getTangentPoint(Point p1, double r, CalcVector v1, CalcVector v2) {
		CalcVector avgv = getAverageVector(v1, v2);

		double A = avgv.getMagnitude();
		Point Bz = MathUtil.ClosestPointOnLine(new Line(v1.origin(), v1.destination()), avgv.destination());
		double CBz = avgv.destination().distance(Bz);
		double dist = (A * r) / CBz;
		Point newpt = avgv.PointOnLine(dist / avgv.getMagnitude());
		return newpt;
	}

	public static double crossProduct(CalcVector v1, CalcVector v2) {
		return v1.getX() * v2.getY() - v1.getY() * v2.getX();
	}
	
	public static CalcVector parallelProjection(CalcVector U, CalcVector V) {
		double dot = dotProduct(U, V);
		double mag = V.getMagnitude();
		double angle = V.getAngle();
		return new CalcVector(U.getX(), U.getY(), dot / mag, angle);
	}

	public static boolean isParallel(CalcVector v1, CalcVector v2) {
		return Math.abs(dotProduct(v1, v2)) == v1.getMagnitude() * v2.getMagnitude();
	}

	public static boolean isAntiParallel(CalcVector v1, CalcVector v2) {
		return Math.abs(dotProduct(v1, v2)) == -v1.getMagnitude() * v2.getMagnitude();
	}

	public static CalcVector perpendicularProjection(CalcVector U, CalcVector V) {
		return new CalcVector(new Point(U.getX(), U.getY()), U.subtractVector(parallelProjection(U, V).getDirectionVector()));
	}

	public static double round(double value, int places) {
		double factor = Math.pow(10, places);
		return Math.round(value * factor) / factor;
	}

	public static double getAngle(CalcVector v1, CalcVector v2) {
		return Math.acos(dotProduct(v1, v2) / (v1.getMagnitude() * v2.getMagnitude()));
	}

	public static Point getIntersection(CalcVector v1, CalcVector v2) {
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
    	double xf_H = (x0 + (y0 - (int)screen.getHeight()) / slope);
    	double yf_0 = (y0 + (x0) * slope);
    	double yf_W = (y0 + (x0 - (int)screen.getWidth()) * slope);
    
    	if (xf_0 > 0 && xf_0 < (int)screen.getWidth() && dy == 1) {
    		return new Pair<String, Point>("top", new Point((int) xf_0, 0));
    	} else if (xf_H > 0 && xf_H < (int)screen.getWidth() && dy == -1) {
    		return new Pair<String, Point>("bottom", new Point((int) xf_H, (int)screen.getHeight()));
    	}
    	if (yf_0 > 0 && yf_0 < (int)screen.getHeight() && dx == -1) {
    		return new Pair<String, Point>("left", new Point(0, (int) yf_0));
    	} else if (yf_W > 0 && yf_W < (int)screen.getHeight() && dx == 1) {
    		return new Pair<String, Point>("right", new Point((int)screen.getWidth(), (int) yf_W));
    	}
    	return new Pair<String, Point>("none", new Point(0, 0));
    }

	public static Point ClosestPointOnLine(Line line, Point sphereCenter) {
		double x1 = line.getX1();
		double y1 = line.getY1();
		double x2 = line.getX2();
		double y2 = line.getY2();
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
    
}
