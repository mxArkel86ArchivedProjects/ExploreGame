package util;

import java.util.ArrayList;
import java.util.List;

public class PathFindPath {
    public PathFindPath(double x, double y) {
        path = new ArrayList<>();
        path.add(new Point(x, y));
    }

    public PathFindPath(double x, double y, PathFindPath p) {
        path = p.path;
        path.add(new Point(x, y));
    }

    public List<Point> path;

    public Point getLastPoint() {
        return path.get(path.size() - 1);
    }
    
}
