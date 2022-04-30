package util;

public class Line {
    Point p1;
    Point p2;

    public Line(Point p1, Point p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public Line(double x1, double y1, double x2, double y2) {
        this.p1 = new Point(x1, y1);
        this.p2 = new Point(x2, y2);
    }

    public Point getP1() {
        return p1;
    }

    public Point getP2() {
        return p2;
    }

    public double getX1() {
        return p1.getX();
    }

    public double getY1() {
        return p1.getY();
    }

    public double getX2() {
        return p2.getX();
    }

    public double getY2() {
        return p2.getY();
    }

    public double dY() {
        return p2.getY() - p1.getY();
    }

    public double dX() {
        return p2.getX() - p1.getX();
    }

    public double slope() {
        return dY() / dX();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Line l) {
            return l.p1.equals(p1) && l.p2.equals(p2);
        }
        return false;
    }
}
