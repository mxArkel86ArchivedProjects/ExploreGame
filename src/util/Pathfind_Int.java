package util;

public class Pathfind_Int {
    int x;
    int y;
    
    public Pathfind_Int(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
    @Override
    public boolean equals(Object o) {
        if (o instanceof Pathfind_Int pt) {
            if (pt.x == x && pt.y == y) {
                return true;
            }
        }
        return false;
    }
}