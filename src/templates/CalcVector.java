package templates;

import util.MathUtil;

public class CalcVector {
    double x;
    double y;
    double dx;
    double dy;
    
    public CalcVector(double x, double y, double dx, double dy) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
    }

    public CalcVector(Point p, double dx, double dy) {
        this.x = p.getX();
        this.y = p.getY();
        this.dx = dx;
        this.dy = dy;
    }

    public static CalcVector fromAngleMag(Point start, double mag, double angle) {
        CalcVector cvec = new CalcVector(start.getX(), start.getY(), mag * Math.cos(angle), mag * Math.sin(angle));
        return cvec;
    }

    public CalcVector(Point p1, Point p2) {
        this.x = p1.getX();
        this.y = p1.getY();
        this.dx = p2.getX() - p1.getX();
        this.dy = p2.getY() - p1.getY();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getDX() {
        return dx;
    }

    public double getDY() {
        return dy;
    }

    public Point origin() {
        return new Point(x, y);
    }

    public Point destination() {
        return new Point(x + dx, y + dy);
    }

    public double getMagnitude() {
        return Math.sqrt(dx * dx + dy * dy);
    }

    public CalcVector setMagnitude(double mult) {
        double mag = getMagnitude();
        return new CalcVector(x, y, dx * mult / mag, dy * mult / mag);
    }

    public double getAngle() {
        return Math.atan2(dy, dx);
    }

    public CalcVector getUnitVector() {
        double magnitude = getMagnitude();
        return new CalcVector(0,0,dx / magnitude, dy / magnitude);
    }

    public CalcVector getScaledVector(double scale) {
        return new CalcVector(x, y, dx * scale, dy * scale);
    }

    public CalcVector invert() {
        return new CalcVector(destination(), origin());
    }

    public CalcVector localize(CalcVector c) {
        double angle = c.getAngle()-this.getAngle();
        double mag = this.getMagnitude();
        double x_ = mag * Math.cos(angle);
        double y_ = mag * Math.sin(angle);
        return new CalcVector(this.origin(), x_, y_);
    }

    public CalcVector getScaledVector(double sx, double sy) {
        return new CalcVector(x, y, dx * sx, dy * sy);
    }

    public CalcVector add(CalcVector other) {
        return new CalcVector(x+other.getX(), y + other.getY(), dx + other.getDX(), dy + other.getDY());
    }

    public CalcVector subtract(CalcVector other) {
        return new CalcVector(x - other.getX(), y - other.getY(), dx - other.getDX(), dy - other.getDY());
    }
}
