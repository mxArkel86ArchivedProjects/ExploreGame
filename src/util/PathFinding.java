package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Color;

import org.javatuples.Pair;

public class PathFinding {
    public static Path PathFind(PathNode start, PathNode end, Function<Pair<Point, Point>, Boolean> inMap) {
        Queue<PathNode> queue = new LinkedList<>();
        queue.add(start);
        List<Point> visited = new ArrayList<>();

        List<PathNode> add_node = new ArrayList<>();
        System.out.println("start: " + start.getPoint().getX() + "," + start.getPoint().getY() + " end: "
                + end.getPoint().getX() + "," + end.getPoint().getY());
        while (queue.size() > 0) {
            if (queue.size() > 10000)
                return null;
            System.out.println("queue size: " + queue.size());
            while (!queue.isEmpty()) {
                PathNode node = queue.poll();

                if (node.equals(end)) {
                    Path p = new Path(getPath(node, start));
                    return p;
                }

                visited.add(node.getPoint());
                for (PathNode neighbor : getNeighbors(node)) {
                    if (!visited.contains(neighbor.getPoint()) && !queue.contains(neighbor)) {
                        Pair<Point, Point> points = Pair.with(node.getPoint(), neighbor.getPoint());
                        if (inMap == null || inMap.apply(points)) {
                            add_node.add(neighbor);
                        }
                    }
                }
            }
            queue.addAll(add_node);
            add_node.clear();
        }
        return null;
    }

    public static void displayPath(Graphics2D g, List<List<PathNode>> layers, Point location, double GRIDSIZE, Function<Pair<Point, Point>, Boolean> inMap) {
        g.setStroke(new BasicStroke(3));
        for (int i = 0; i < layers.size(); i++) {
            List<PathNode> layer = layers.get(i);
            int z = (int) (255 / layers.size() * i);
            Color c = new Color(z, z, z);
            g.setColor(c);
            for (PathNode node : layer) {
                if(node.parent==null)
                    continue;
                Point p1 = SchemUtilities.schemToFrame(node.getPoint(), location, GRIDSIZE);
                Point p2 = SchemUtilities.schemToFrame(node.parent.getPoint(), location, GRIDSIZE);
                g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
            }
        }
    }

    public static List<List<PathNode>> PathFindDebug(PathNode start, int max_iterations, Function<Pair<Point, Point>, Boolean> inMap) {
        Queue<PathNode> queue = new LinkedList<>();
        queue.add(start);
        List<Point> visited = new ArrayList<>();

        List<PathNode> add_node = new ArrayList<>();
        
        List<List<PathNode>> layers = new ArrayList<>();
        

        int iterations = 0;
        while (queue.size() > 0 || iterations > max_iterations) {
            if (queue.size() > 1000)
                break;
            
            List<PathNode> layer = new ArrayList<>();
            while (!queue.isEmpty()) {
                PathNode node = queue.poll();
                layer.add(node);

                visited.add(node.getPoint());
                for (PathNode neighbor : getNeighbors(node)) {
                    if (!visited.contains(neighbor.getPoint()) && !queue.contains(neighbor)) {
                        Pair<Point, Point> points = Pair.with(node.getPoint(), neighbor.getPoint());
                        if (inMap == null || inMap.apply(points)) {
                            add_node.add(neighbor);
                        }
                    }
                }
            }
            queue.addAll(add_node);
            add_node.clear();
            layers.add(layer);
        }
        return layers;
    }

    private static List<Point> getPath(PathNode node, PathNode start) {
        List<Point> path = new ArrayList<>();
        final double z = 0.5f;
        while (node.parent != null) {

            path.add(new Point(node.getPoint().getX() + z, node.getPoint().getY() + z));
            node = node.parent;
        }
        path.add(new Point(start.getPoint().getX() + z, start.getPoint().getY() + z));
        Collections.reverse(path);
        return path;
    }

    private static List<PathNode> getNeighbors(PathNode node) {
        List<PathNode> neighbors = Arrays.asList(
                new PathNode(node.getPoint().x + 1, node.getPoint().y, node), // right
                new PathNode(node.getPoint().x - 1, node.getPoint().y, node), // left
                new PathNode(node.getPoint().x, node.getPoint().y - 1, node), // down
                new PathNode(node.getPoint().x, node.getPoint().y + 1, node)// ,//,//up
        // new PathNode(node.getPoint().x+1, node.getPoint().y+1, node),//right-up
        // new PathNode(node.getPoint().x-1, node.getPoint().y-1, node),//left-down
        // new PathNode(node.getPoint().x+1, node.getPoint().y-1, node),//right-down
        // new PathNode(node.getPoint().x-1, node.getPoint().y+1, node)//left-up
        );
        return neighbors;
    }
}