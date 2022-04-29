package util;

import java.util.ArrayList;
import java.util.List;

public class Path {
    List<Point> path;
    boolean optimized;

    public Path(List<Point> path) {
        this.path = path;
        this.optimized = false;
    }
    
    public List<Point> getPathPoints() {
        return path;
    }

    public void optimizePath() {
        if (optimized) {
            return;
        }
        List<Point> optimizedPath = new ArrayList<>();
        
        optimizedPath.add(path.get(0));
        double prevdirection = directionFromPoints(path.get(1), path.get(0)); // 0 = right, 1 = top-right, 2 = top, 3 = top-left, 4 = left, 5 = bottom-left, 6 = bottom, 7 = bottom-right
        
        for (int i = 1; i < path.size() - 1; i++) {
            Point current = path.get(i);
            Point prev = null;

            
            prev = path.get(i - 1);
            double currentdirection = -1;
            boolean included = false;
            if (prevdirection == -1) {//if there is no direction yet
                prevdirection = directionFromPoints(current, prev);
                optimizedPath.add(current);
                included = true;
            } else {//a given direction has been decided previously
                currentdirection = directionFromPoints(current, prev);
                if (prevdirection != currentdirection) {//if the direction is different, end the current run
                    optimizedPath.add(current);
                    prevdirection = currentdirection;
                    included = true;
                } else {
                    //continue;
                }
            }
            System.out.println("prev=" + prev + " next=" + current + " pdirection=" + prevdirection + " ndirection2=" + currentdirection + " included=" + included);
        }
        optimizedPath.add(path.get(path.size() - 1));
        optimized = true;
        path = optimizedPath;
    }

    private static double directionFromPoints(Point current, Point prev) {
            // if (current.x > prev.x && current.y == prev.y)
            //     return 0;
            // else if (current.x > prev.x && current.y > prev.y)
            //     return 1;
            // else if (current.x == prev.x && current.y > prev.y)
            //     return 2;
            // else if (current.x < prev.x && current.y > prev.y)
            //     return 3;
            // else if (current.x < prev.x && current.y == prev.y)
            //     return 4;
            // else if (current.x < prev.x && current.y < prev.y)
            //     return 5;
            // else if (current.x == prev.x && current.y < prev.y)
            //     return 6;
            // else if (current.x > prev.x && current.y < prev.y)
            //     return 7;
            // else
            //     return -1;
        return Math.atan2(current.y - prev.y, current.x - prev.x);
    }
}
