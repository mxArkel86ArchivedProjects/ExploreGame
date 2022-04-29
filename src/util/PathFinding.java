package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

import org.javatuples.Pair;

public class PathFinding {
    public static Path PathFind(PathNode start, PathNode end, Function<Pair<Point, Point>, Boolean> inMap) {
        Queue<PathNode> queue = new LinkedList<>();
        queue.add(start);
        List<Point> visited = new ArrayList<>();

        List<PathNode> add_node = new ArrayList<>();
        System.out.println("start: " + start.getPoint().getX() + "," + start.getPoint().getY() + " end: "
                + end.getPoint().getX() + "," + end.getPoint().getY());
        // while (queue.size() > 0) {
        //     if (queue.size() > 10000)
        //         return null;
        //     System.out.println("queue size: " + queue.size());
        //     while (!queue.isEmpty()) {
        //         PathNode node = queue.poll();

        //         if (node.equals(end)) {
        //             Path p = new Path(getPath(node, start));
        //             return p;
        //         }

        //         visited.add(node.getPoint());
        //         for (PathNode neighbor : getNeighbors(node)) {
        //             if (!visited.contains(neighbor.getPoint()) && !queue.contains(neighbor)) {
        //                 Pair<Point, Point> points = Pair.with(node.getPoint(), neighbor.getPoint());
        //                 if (inMap == null || inMap.apply(points)) {
        //                     add_node.add(neighbor);
        //                 }
        //             }
        //         }
        //     }
        //     queue.addAll(add_node);
        //     add_node.clear();
        // }
        return null;
    }
    
    private static List<Point> getPath(PathNode node, PathNode start) {
        List<Point> path = new ArrayList<>();

        while (node.parent != null) {
            path.add(new Point(node.getPoint().getX()+0.5, node.getPoint().getY()+0.5));
            node = node.parent;
        }
        path.add(new Point(start.getPoint().getX()+0.5, start.getPoint().getY()+0.5));
        Collections.reverse(path);
        return path;
    }

    private static List<PathNode> getNeighbors(PathNode node) {
        List<PathNode> neighbors = Arrays.asList(
                new PathNode(node.getPoint().x + 1, node.getPoint().y, node),//right
                new PathNode(node.getPoint().x - 1, node.getPoint().y, node),//left
                new PathNode(node.getPoint().x, node.getPoint().y - 1, node),//down
                new PathNode(node.getPoint().x, node.getPoint().y + 1, node),//,//up
                new PathNode(node.getPoint().x+1, node.getPoint().y+1, node),//right-up
                new PathNode(node.getPoint().x-1, node.getPoint().y-1, node),//left-down
                new PathNode(node.getPoint().x+1, node.getPoint().y-1, node),//right-down
                new PathNode(node.getPoint().x-1, node.getPoint().y+1, node)//left-up
        );
        return neighbors;
    }
}