package templates;

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

    public Line shift(double x, double y) {
        Point p1_ = new Point(p1.getX() + x, p1.getY() + y);
        Point p2_ = new Point(p2.getX() + x, p2.getY() + y);
        Line l = new Line(p1_, p2_);
        return l;
    }

    public Point getP1() {
        return p1;
    }

    public Point getP2() {
        return p2;
    }

    public Line constrict(double percent) {
        Point p1_ = new Point(p1.getX() * (1 - percent) + p2.getX() * percent,
                p1.getY() * (1 - percent) + p2.getY() * percent);
        
        Point p2_ = new Point(p2.getX() * (1 - percent) + p1.getX() * percent,
                p2.getY() * (1 - percent) + p1.getY() * percent);
        
        Line l = new Line(p1_, p2_);
        return l; 
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

    public double getSlope() {
        if (dX() == 0)
            return Double.POSITIVE_INFINITY;
        return dY() / dX();
    }

    public double getIntercept() {
        return p1.getY() - getSlope() * p1.getX();
    }

    public Point center() {
        return new Point((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2);
    }

    public double length() {
        return Math.sqrt(Math.pow(dX(), 2) + Math.pow(dY(), 2));
    }

    public double angle() {
        return Math.atan2(dY(), dX());
    }

    public boolean parallel(Line l) {
        if(l.getSlope() - getSlope() < 0.00001 && l.getSlope() - getSlope() > -0.00001)
            return true;
        return false;
    }

    public boolean perpendicular(Line l) {
        if(l.getSlope() + 1 / getSlope()< 1.00001 && l.getSlope() + 1 / getSlope() > 0.99999)
            return false;
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Line l) {
            return l.p1.equals(p1) && l.p2.equals(p2);
        }
        return false;
    }
}
