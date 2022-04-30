package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Color;

import org.javatuples.Pair;

public class PathFinding {
    public static Path PathFind(PathNode start, PathNode end, Function<Pair<IntPoint, IntPoint>, Boolean> inMap) {
        Queue<PathNode> queue = new LinkedList<>();
        queue.add(start);
        List<IntPoint> visited = new ArrayList<>();

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
                        Pair<IntPoint, IntPoint> points = Pair.with(node.getPoint(), neighbor.getPoint());
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

    private static int pathGetDepth(List<PathNode> layer) {
        List<PathNode> nodes = new ArrayList<>();
        nodes.addAll(layer);

        int depth = 0;
        while (nodes.size() > 0) {
            depth++;
            for (int i = 0; i < nodes.size(); i++) {
                PathNode node = nodes.get(i);
                if (node.parent != null) {
                    nodes.remove(i);
                    nodes.add(i, node.parent);
                } else {
                    nodes.remove(i);
                    i--;
                }
            }
        }
        return depth;
    }

    public static void displayPath(Graphics2D g, List<PathNode> layers, Point location, double GRIDSIZE,
            Function<Pair<IntPoint, IntPoint>, Boolean> inMap) {
        g.setStroke(new BasicStroke(3));

        List<PathNode> nodes = new ArrayList<>();
        nodes.addAll(layers);

        int depth = pathGetDepth(nodes);
        
        int i = 0;
        while (nodes.size() > 0) {
            Color c = Color.getHSBColor(i * 0.4f / depth, 1, 1);
            g.setColor(c);
            i++;
            for (int j = 0; j < nodes.size(); j++) {
                PathNode node = nodes.get(j);

                if (node.parent == null)
                    nodes.remove(j);
                else {
                    nodes.remove(j);
                    nodes.add(j, node.parent);
                    Point p1 = SchemUtilities.schemToFrame(new Point(node.getPoint().getX()+0.5, node.getPoint().getY()+0.5), location, GRIDSIZE);
                    Point p2 = SchemUtilities.schemToFrame(new Point(node.parent.getPoint().getX()+0.5, node.parent.getPoint().getY()+0.5), location, GRIDSIZE);
                    g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
                }
                
                
            }
        }
    }
    

    public static List<PathNode> PathFindDebug(PathNode start, int max_iterations, Function<Pair<IntPoint, IntPoint>, Boolean> inMap) {
        Queue<PathNode> queue = new LinkedList<>();
        queue.add(start);
        List<IntPoint> visited = new ArrayList<>();

        List<PathNode> add_node = new ArrayList<>();

        List<PathNode> finalNodes = new ArrayList<>();
        
    
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
                        Pair<IntPoint, IntPoint> points = Pair.with(node.getPoint(), neighbor.getPoint());
                        if (inMap == null || inMap.apply(points)) {
                            add_node.add(neighbor);
                        }
                    }
                }
            }
            queue.addAll(add_node);
            add_node.clear();
            finalNodes = queue.stream().collect(Collectors.toList());
        }
        return finalNodes;
    }

    private static List<IntPoint> getPath(PathNode node, PathNode start) {
        List<IntPoint> path = new ArrayList<>();
        while (node.parent != null) {

            path.add(node.getPoint());
            node = node.parent;
        }
        path.add(start.getPoint());
        Collections.reverse(path);
        return path;
    }

    private static List<PathNode> getNeighbors(PathNode node) {
        List<PathNode> neighbors = Arrays.asList(
                new PathNode(node.getPoint().x + 1, node.getPoint().y, node), // right
                new PathNode(node.getPoint().x - 1, node.getPoint().y, node), // left
                new PathNode(node.getPoint().x, node.getPoint().y - 1, node), // down
                new PathNode(node.getPoint().x, node.getPoint().y + 1, node),// ,//,//up
        new PathNode(node.getPoint().x+1, node.getPoint().y+1, node),//right-up
        new PathNode(node.getPoint().x-1, node.getPoint().y-1, node),//left-down
        new PathNode(node.getPoint().x+1, node.getPoint().y-1, node),//right-down
        new PathNode(node.getPoint().x-1, node.getPoint().y+1, node)//left-up
        );
        return neighbors;
    }
}