package templates;

public class PathNode {
    private IntPoint pt;
    private PathNode parent = null;
    
    public PathNode(int i, int y, PathNode node) {
        pt = new IntPoint(i, y);
        parent = node;
    }

    public PathNode(IntPoint pt, PathNode node) {
        this.pt = pt;
        parent = node;
    }

    public IntPoint getPoint() {
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

    public PathNode getParent() {
        return parent;
    }
}