package util;

public class PathNode {
    Point pt;
    PathNode parent = null;
    
    public PathNode(double i, double y, PathNode node) {
        pt = new Point(i, y);
        parent = node;
    }

    Point getPoint() {
        return pt;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PathNode) {
            PathNode node = (PathNode) obj;
            return pt.equals(node.getPoint());
        }
        return false;
    }
}