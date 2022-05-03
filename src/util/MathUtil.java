package util;

import org.javatuples.Pair;

import templates.Point;
import templates.Size;

public class MathUtil {

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
