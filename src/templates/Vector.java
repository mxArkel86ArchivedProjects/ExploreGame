package templates;

import util.MathUtil;

public class Vector extends DirectionVector {
    double x;
    double y;
    
    public Vector(double x, double y, double magnitude, double angle) {
        super(magnitude, angle);
        this.x = x;
        this.y = y;
    }

    public Vector(Point p1, DirectionVector v) {
        super(v.getMagnitude(), v.getAngle());
        this.x = p1.getX();
        this.y = p1.getY();
    }


    public static Vector fromPoints(Point p1, Point p2) {
        return new Vector(p1.getX(), p1.getY(), Math.sqrt(Math.pow(p2.getX() - p1.getX(), 2) + Math.pow(p2.getY() - p1.getY(), 2)), Math.atan2(p2.getY() - p1.getY(), p2.getX() - p1.getX()));
    }

    public Vector scale(double s) {
        return new Vector(this.x, this.y, this.magnitude * s, this.angle);
    }

    public Vector shift(double dx, double dy) {
        return new Vector(this.x + dx, this.y + dy, this.magnitude, this.angle);
    }

    public Point getCenter() {
        return new Point(x + getDX()/2, y + getDY()/2);
    }

    public Vector scale(double s, double t) {
        return new Vector(new Point(x, y), super.scale(s, t));
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Point positionComponent() {
        return new Point(x, y);
    }

    public Point directionComponent() {
        return new Point(magnitude * Math.cos(angle), magnitude * Math.sin(angle));
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
    
    public Vector withMagnitude(double magnitude) {
        return new Vector(x, y, magnitude, angle);
    }

    public DirectionVector getDirectionVector() {
        return this;
    }

    public Vector invert() {
        return Vector.fromPoints(destination(), origin());
    }

    public Vector addAll(Vector other) {
        return new Vector(x+other.getX(), y + other.getY(), getDX() + other.getDX(), getDY() + other.getDY());
    }

    public Vector subtractAll(Vector other) {
        return new Vector(x - other.getX(), y - other.getY(), getDX() - other.getDX(), getDY() - other.getDY());
    }

    public Point PointOnLine(double percent) {
        return new Point(origin().getX() * (1 - percent) + destination().getX() * percent,
                origin().getY() * (1 - percent) + destination().getY() * percent);
    }
    
    public double getSlope() {
        return this.getDY()/this.getDX();
    }

    public boolean isParallel(Vector l) {
        if(l.getSlope() - getSlope() < 0.00001 && l.getSlope() - getSlope() > -0.00001)
            return true;
        return false;
    }

    public boolean isPerpendicular(Vector l) {
        if(l.getSlope() + 1 / getSlope()< 1.00001 && l.getSlope() + 1 / getSlope() > 0.99999)
            return false;
        return true;
    }
}
