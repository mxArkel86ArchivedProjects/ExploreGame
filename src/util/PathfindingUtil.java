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

import gameObjects.Collider;
import templates.IntPoint;
import templates.PathNode;
import templates.Point;
import templates.Vector;

public class PathfindingUtil {
    public static List<Point> directPath(PathNode start, PathNode end, List<Collider> allWallsRaw) {
        List<Collider> allWalls = allWallsRaw.stream().map(c -> CollisionUtil.subdivideCollider(c)).flatMap(List::stream).filter(x->x!=null)
                .map(x -> x.shift(-0.5, -0.5)).map(x->new Collider(x.origin(), x.destination())).collect(Collectors.toList());
            
        if (CollisionUtil.LineIntersectsWithColliders(Vector.fromPoints(start.getPoint().DPoint(), end.getPoint().DPoint()), allWalls)) {
            return null;
        }
        return Arrays.asList(start.getPoint().DPoint(), end.getPoint().DPoint());
    }

    public static List<Point> PathFindByWalls(PathNode start, PathNode end, int MAX_TRAVEL_DIST, int MAX_LEVELS, List<Collider> allWallsRaw) {

        List<Collider> allWalls = allWallsRaw.stream().map(c -> CollisionUtil.subdivideCollider(c)).flatMap(List::stream).filter(x->x!=null)
                .map(x -> x.shift(-0.5,-0.5)).collect(Collectors.toList());

        Queue<PathNode> queue = new LinkedList<>();
        List<PathNode> visited = new ArrayList<>();
        List<PathNode> add_node = new ArrayList<>();
        queue.add(start);

        int level = 0;

        while (!queue.isEmpty()) {
            if(level > MAX_LEVELS) {
                return null;
            }
            level++;
            while (!queue.isEmpty()) {
                PathNode current = queue.poll();

                if (queue.size() > 20000)
                    return null;


                Vector walk = Vector.fromPoints(current.getPoint().DPoint(), end.getPoint().DPoint());
                boolean intersects = CollisionUtil.LineIntersectsWithColliders(walk, allWalls);
                if(!intersects) {
                    List<Point> path = getPath(current, start).stream().map(p -> p.DPoint()).collect(Collectors.toList());
                    path.add(end.getPoint().DPoint());
                    return path;
                }

                visited.add(current);
                List<PathNode> neighbors = getNextNodes(current, MAX_TRAVEL_DIST, allWalls);
                for (PathNode neighbor : neighbors) {
                    if (!visited.contains(neighbor)) {
                        add_node.add(neighbor);
                    }
                }
            }
            queue.addAll(add_node);
            add_node.clear();
        }
        return null;
    }
    
    public static List<PathNode> PathFindByWallsDebug(PathNode start, int MAX_TRAVEL_DIST, List<Collider> allWallsRaw) {

        List<Collider> allWalls = allWallsRaw.stream().map(c -> CollisionUtil.subdivideCollider(c)).flatMap(List::stream).filter(x->x!=null)
                .map(x -> x.shift(-0.5, -0.5)).map(x->new Collider(x.origin(), x.destination())).collect(Collectors.toList());
        
        List<PathNode> nodes = new ArrayList<>();
        List<PathNode> visited = new ArrayList<>();
        List<PathNode> add_node = new ArrayList<>();
        nodes.add(start);

        int iteration = 0;
        while (true) {
            if (iteration > 2)
                return nodes;
            iteration++;
            System.out.println("iteration=" + iteration);
            
            for (int i = 0; i < nodes.size(); i++) {
                PathNode current = nodes.get(i);

                visited.add(current);
                List<PathNode> neighbors = getNextNodes(current, MAX_TRAVEL_DIST, allWalls);
                for (PathNode neighbor : neighbors) {
                    if (!visited.contains(neighbor)) {
                        add_node.add(neighbor);
                    }
                }
            }
            
            if (add_node.size() == 0)
                return nodes;
            
            nodes = add_node.stream().toList();
        }
    }
    
    public static List<PathNode> getNextNodes(PathNode p, int MAX_TRAVEL_DIST, List<Collider> allWalls) {

        List<Collider> collidersInRange = allWalls.stream().filter(c -> {
            boolean b = Math.sqrt(Math.pow(p.getPoint().getX() - c.getCenter().getX(), 2)
                    + Math.pow(p.getPoint().getY() - c.getCenter().getY(), 2)) < MAX_TRAVEL_DIST;
            return b;
        }).collect(Collectors.toList());

        List<Point> corners = getCorners(allWalls).stream().map(x->x.shift(1, 1)).filter(x->x.distance(p.getPoint().DPoint()) < MAX_TRAVEL_DIST)
                .collect(Collectors.toList());

        List<PathNode> nextNodes = new ArrayList<>();
        for (Collider c : collidersInRange) {
            for (Point pt : getAdjacentPoints(c)) {
                Vector walk = Vector.fromPoints(p.getPoint().DPoint(), pt);
                boolean intersects = CollisionUtil.LineIntersectsWithColliders(walk, collidersInRange);
                if (!intersects) {
                    nextNodes.add(new PathNode(new IntPoint((int) pt.getX(), (int) pt.getY()), p));
                }
            }
        }
        for (Point pt : corners) {
            Vector walk = Vector.fromPoints(p.getPoint().DPoint(), pt);
            boolean intersects = CollisionUtil.LineIntersectsWithColliders(walk, collidersInRange);
            if (!intersects) {
                nextNodes.add(new PathNode(new IntPoint((int) pt.getX(), (int) pt.getY()), p));
            }
        }
        return nextNodes;
    }
    
    public static List<Vector> getCornerLines(List<Collider> colliders) {
        //check if two lines make a corner
        List<Vector> cornerLines = new ArrayList<>();
        
        for (int i = 0; i < colliders.size(); i++) {
            for (int j = i + 1; j < colliders.size(); j++) {
                    IntPoint p1 = new IntPoint(colliders.get(i).origin());
                    IntPoint p2 = new IntPoint(colliders.get(i).destination());

                    IntPoint p3 = new IntPoint(colliders.get(j).origin());
                    IntPoint p4 = new IntPoint(colliders.get(j).destination());

                    Vector l1 = colliders.get(i);
                    Vector l2 = colliders.get(j);

                    IntPoint d1 = null;
                    IntPoint d2 = null;
                    IntPoint d3 = null;
                    // IntPoint d4 = null;
                    if (p1.equals(p3)) {
                        d1 = p2;
                        d2 = p4;
                        d3 = p1;
                        //d4 = p3;
                    } else if (p1.equals(p4)) {
                        d1 = p2;
                        d2 = p3;
                        d3 = p1;
                        //d4 = p4;
                    } else if (p2.equals(p3)) {
                        d1 = p1;
                        d2 = p4;
                        d3 = p2;
                        //d4 = p3;
                    } else if (p2.equals(p4)) {
                        d1 = p1;
                        d2 = p3;
                        d3 = p2;
                        //d4 = p4;
                    } else {
                        continue;
                    }
                    Vector across = Vector.fromPoints(d1.DPoint(), d2.DPoint());

                    Vector l3 = Vector.fromPoints(across.getCenter(), d3.DPoint());

                    // check if the lines are parallel
                    int slope = (int)(l1.getDX()==0?Integer.MAX_VALUE:l1.getDY()/l1.getDX());
                    int slope2 = (int)(l2.getDX() == 0 ? Integer.MAX_VALUE : l2.getDY() / l2.getDX());
                    
                    if(slope - slope2 < 0.00001 && slope - slope2 > -0.00001)
                        continue;
                    
                
                    
                    // if((slope + 1 / slope2< 1.00001 && slope + 1 / slope2 > 0.99999))
                    //     continue;
                    
                    
                    
                    // double shift_1 = (-MaxWithSign(d1.getX() - d3.getX(), d1.getY() - d3.getY())) / 2;
                    // double shift_2 = (-MaxWithSign(d2.getX() - d4.getX(), d2.getY() - d4.getY())) / 2;
                    
                    cornerLines.add(across.shift(l3.getDX(), l3.getDY()));
                    //cornerLines.add(across);

            }
        }
        return cornerLines;
    }
    
    public static Point[] getAdjacentPoints(Vector c) {
        Point[] points = new Point[2];
        
        int[] sides = new int[] { -1, 1 };
        for (int z = 0; z < 2;z++) {
            double angle = c.getAngle();
            double iangle = angle + Math.PI / 2;
            int side = sides[z];
            //get point perpendicular to the collider and a distance of length/2
            double x = c.getCenter().getX() + Math.cos(iangle) * c.getMagnitude() / 2 * side;
            double y = c.getCenter().getY() + Math.sin(iangle) * c.getMagnitude() / 2 * side;
            points[z] = new Point(x, y);
            // Point p1 = new Point(c.getCenter().x + side * Math.cos(iangle) * c.length() / 2,
            //         c.getCenter().y + side * Math.sin(iangle) * c.length() / 2);
            // points[z] = p1;
        }
        return points;
    }
    
    
    
    // public static Path PathFindByGrid(PathNode start, PathNode end, Function<Pair<IntPoint, IntPoint>, Boolean> inMap) {
    //     Queue<PathNode> queue = new LinkedList<>();
    //     queue.add(start);
    //     List<IntPoint> visited = new ArrayList<>();

    //     List<PathNode> add_node = new ArrayList<>();
    //     System.out.println("start: " + start.getPoint().getX() + "," + start.getPoint().getY() + " end: "
    //             + end.getPoint().getX() + "," + end.getPoint().getY());
    //     while (queue.size() > 0) {
    //         if (queue.size() > 10000)
    //             return null;
    //         System.out.println("queue size: " + queue.size());
    //         while (!queue.isEmpty()) {
    //             PathNode node = queue.poll();

    //             if (node.equals(end)) {
    //                 Path p = new Path(getPath(node, start));
    //                 return p;
    //             }

    //             visited.add(node.getPoint());
    //             for (PathNode neighbor : getNeighbors(node)) {
    //                 if (!visited.contains(neighbor.getPoint()) && !queue.contains(neighbor)) {
    //                     Pair<IntPoint, IntPoint> points = Pair.with(node.getPoint(), neighbor.getPoint());
    //                     if (inMap == null || inMap.apply(points)) {
    //                         add_node.add(neighbor);
    //                     }
    //                 }
    //             }
    //         }
    //         queue.addAll(add_node);
    //         add_node.clear();
    //     }
    //     return null;
    // }

    private static int pathGetDepth(List<PathNode> layer) {
        List<PathNode> nodes = new ArrayList<>();
        nodes.addAll(layer);

        int depth = 0;
        while (nodes.size() > 0) {
            depth++;
            for (int i = 0; i < nodes.size(); i++) {
                PathNode node = nodes.get(i);
                if (node.getParent() != null) {
                    nodes.remove(i);
                    nodes.add(i, node.getParent());
                } else {
                    nodes.remove(i);
                    i--;
                }
            }
        }
        return depth;
    }

    public static void displayPath(Graphics2D g, List<PathNode> layers, Point location,
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

                if (node.getParent() == null)
                    nodes.remove(j);
                else {
                    nodes.remove(j);
                    nodes.add(j, node.getParent());
                    Point p1 = SchematicUtil.schemToFrame(new Point(node.getPoint().getX()+0.5, node.getPoint().getY()+0.5), location);
                    Point p2 = SchematicUtil.schemToFrame(new Point(node.getParent().getPoint().getX()+0.5, node.getParent().getPoint().getY()+0.5), location);
                    g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
                }
                
                
            }
        }
    }
    

    // public static List<PathNode> PathFindDebug(PathNode start, int max_iterations, Function<Pair<IntPoint, IntPoint>, Boolean> inMap) {
    //     Queue<PathNode> queue = new LinkedList<>();
    //     queue.add(start);
    //     List<IntPoint> visited = new ArrayList<>();

    //     List<PathNode> add_node = new ArrayList<>();

    //     List<PathNode> finalNodes = new ArrayList<>();
        
    
    //     int iterations = 0;
    //     while (queue.size() > 0 || iterations > max_iterations) {
    //         if (queue.size() > 1000)
    //             break;
            
    //         List<PathNode> layer = new ArrayList<>();
    //         while (!queue.isEmpty()) {
    //             PathNode node = queue.poll();
    //             layer.add(node);

    //             visited.add(node.getPoint());
    //             for (PathNode neighbor : getNeighbors(node)) {
    //                 if (!visited.contains(neighbor.getPoint()) && !queue.contains(neighbor)) {
    //                     Pair<IntPoint, IntPoint> points = Pair.with(node.getPoint(), neighbor.getPoint());
    //                     if (inMap == null || inMap.apply(points)) {
    //                         add_node.add(neighbor);
    //                     }
    //                 }
    //             }
    //         }
    //         queue.addAll(add_node);
    //         add_node.clear();
    //         finalNodes = queue.stream().collect(Collectors.toList());
    //     }
    //     return finalNodes;
    // }

    private static List<IntPoint> getPath(PathNode node, PathNode start) {
        List<IntPoint> path = new ArrayList<>();
        while (node.getParent() != null) {

            path.add(node.getPoint());
            node = node.getParent();
        }
        path.add(start.getPoint());
        Collections.reverse(path);
        return path;
    }

    // private static List<PathNode> getNeighbors(PathNode node) {
    //     List<PathNode> neighbors = Arrays.asList(
    //             new PathNode(node.getPoint().x + 1, node.getPoint().y, node), // right
    //             new PathNode(node.getPoint().x - 1, node.getPoint().y, node), // left
    //             new PathNode(node.getPoint().x, node.getPoint().y - 1, node), // down
    //             new PathNode(node.getPoint().x, node.getPoint().y + 1, node),// ,//,//up
    //     new PathNode(node.getPoint().x+1, node.getPoint().y+1, node),//right-up
    //     new PathNode(node.getPoint().x-1, node.getPoint().y-1, node),//left-down
    //     new PathNode(node.getPoint().x+1, node.getPoint().y-1, node),//right-down
    //     new PathNode(node.getPoint().x-1, node.getPoint().y+1, node)//left-up
    //     );
    //     return neighbors;
    // }

    public static List<Point> getCorners(List<Collider> colliders) {
        List<Vector> lines = getCornerLines(colliders);

        List<Point> corners = new ArrayList<>();
        for (Vector l : lines) {
            corners.addAll(Arrays.stream(getAdjacentPoints(l)).toList());
        }
        return corners;
    }
}