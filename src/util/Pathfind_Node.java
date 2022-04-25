package util;

public class Pathfind_Node {
    Pathfind_Int pt;
    Pathfind_Node parent = null;
    
    public Pathfind_Node(int i, int y, Pathfind_Node node) {
        pt = new Pathfind_Int(i, y);
        parent = node;
    }

    Pathfind_Int getPoint() {
        return pt;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pathfind_Node) {
            Pathfind_Node node = (Pathfind_Node) obj;
            return pt.equals(node.getPoint());
        }
        return false;
    }
}