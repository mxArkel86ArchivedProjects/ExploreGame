package templates;

import util.MathUtil;

public class CalcVector extends DirectionVector {
    double x;
    double y;
    
    public CalcVector(double x, double y, double magnitude, double angle) {
        super(magnitude, angle);
        this.x = x;
        this.y = y;
    }

    public CalcVector(Point p1, DirectionVector v) {
        super(v.getMagnitude(), v.getAngle());
        this.x = p1.getX();
        this.y = p1.getY();
    }

    public static CalcVector fromPoints(Point p1, Point p2) {
        return new CalcVector(p1.getX(), p1.getY(), Math.sqrt(Math.pow(p2.getX() - p1.getX(), 2) + Math.pow(p2.getY() - p1.getY(), 2)), Math.atan2(p2.getY() - p1.getY(), p2.getX() - p1.getX()));
    }

    public CalcVector scale(double s) {
        return new CalcVector(this.x, this.y, this.magnitude * s, this.angle);
    }

    public CalcVector scale(double s, double t) {
        return new CalcVector(new Point(x, y), super.scale(s, t));
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getDX() {
        return magnitude*Math.cos(angle);
    }

    public double getDY() {
        return magnitude*Math.sin(angle);
    }

    public Point origin() {
        return new Point(x, y);
    }

    public Point destination() {
        return new Point(x + getDX(), y + getDY());
    }

    public double getMagnitude() {
        return Math.sqrt(getDX() * getDX() + getDY() * getDY());
    }
    
    public CalcVector withMagnitude(double magnitude) {
        return new CalcVector(x, y, magnitude, angle);
    }

    public DirectionVector getDirectionVector() {
        return this;
    }

    public CalcVector invert() {
        return CalcVector.fromPoints(destination(), origin());
    }

    public CalcVector addAll(CalcVector other) {
        return new CalcVector(x+other.getX(), y + other.getY(), getDX() + other.getDX(), getDY() + other.getDY());
    }

    public CalcVector subtractAll(CalcVector other) {
        return new CalcVector(x - other.getX(), y - other.getY(), getDX() - other.getDX(), getDY() - other.getDY());
    }

    public Point PointOnLine(double percent) {
        return new Point(origin().getX()*(1-percent)+destination().getX()*percent, origin().getY()*(1-percent)+destination().getY()*percent);
    }
}
