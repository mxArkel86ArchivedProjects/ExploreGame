package util;

import org.javatuples.Pair;

import templates.CalcVector;
import templates.Point;
import templates.Size;

public class MathUtil {
	public static double dotProduct(CalcVector v1, CalcVector v2) {
		return v1.getDX() * v2.getDX() + v1.getDY() * v2.getDY();
	}

	public static double crossProduct(CalcVector v1, CalcVector v2) {
		return v1.getX() * v2.getY() - v1.getY() * v2.getX();
	}
	
	public static CalcVector parallelProjection(CalcVector U, CalcVector V) {
		double scalar = dotProduct(U, V) / (Math.pow(V.getMagnitude(), 2));
		return V.getScaledVector(scalar);
	}

	public static CalcVector perpendicularProjection(CalcVector U, CalcVector V) {
		return U.subtract(parallelProjection(U, V));
	}

	public static double round(double value, int places) {
		double factor = Math.pow(10, places);
		return Math.round(value * factor) / factor;
	}

	public static double getAngle(CalcVector v1, CalcVector v2) {
		return Math.acos(dotProduct(v1, v2) / (v1.getMagnitude() * v2.getMagnitude()));
	}

	public static Point getIntersection(CalcVector v1, CalcVector v2) {
		double x = (v2.getY() * v1.getX() - v1.getY() * v2.getX()) / (v1.getY() * v2.getX() - v2.getY() * v1.getX());
		double y = (v1.getX() * v2.getY() - v2.getX() * v1.getY()) / (v1.getY() * v2.getX() - v2.getY() * v1.getX());
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
    
}
