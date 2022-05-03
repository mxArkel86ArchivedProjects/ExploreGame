package templates;

public class IntPoint {
    private int x;
    private int y;

    public IntPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public IntPoint(Point p) {
        this.x = (int) p.getX();
        this.y = (int) p.getY();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntPoint) {
            IntPoint p = (IntPoint) obj;
            return x == p.getX() && y == p.getY();
        }
        return false;
    }

    public Point DPoint() {
        return new Point(x, y);
    }
}
