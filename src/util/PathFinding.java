package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import main.entry;

public class PathFinding {
    public static List<Point> PathFindByWalls(PathNode start, PathNode end, int MAX_TRAVEL_DIST, List<Collider> allWallsRaw) {

        List<Collider> allWalls = allWallsRaw.stream().map(c -> CollisionUtil.subdivideCollider(c)).flatMap(List::stream)
                .map(x->new Collider(x.shift(-0.5, -0.5))).collect(Collectors.toList());

        Queue<PathNode> queue = new LinkedList<>();
        List<PathNode> visited = new ArrayList<>();
        List<PathNode> add_node = new ArrayList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            while (!queue.isEmpty()) {
                PathNode current = queue.poll();

                if (queue.size() > 20000)
                    return null;


                Line walk = new Line(current.getPoint().DPoint(), end.getPoint().DPoint());
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

        List<Collider> allWalls = allWallsRaw.stream().map(c -> CollisionUtil.subdivideCollider(c)).flatMap(List::stream)
                .map(x->new Collider(x.shift(-0.5, -0.5))).collect(Collectors.toList());
        
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
            boolean b = Math.sqrt(Math.pow(p.getPoint().x - c.center().x, 2)
                    + Math.pow(p.getPoint().y - c.center().y, 2)) < MAX_TRAVEL_DIST;
            return b;
        }).collect(Collectors.toList());

        List<Point> corners = getCorners(allWalls).stream().filter(x->x.distance(p.getPoint().DPoint()) < MAX_TRAVEL_DIST)
                .collect(Collectors.toList());

        List<PathNode> nextNodes = new ArrayList<>();
        for (Collider c : collidersInRange) {
            for (Point pt : getAdjacentPoints(c)) {
                Line walk = new Line(p.getPoint().DPoint(), pt);
                boolean intersects = CollisionUtil.LineIntersectsWithColliders(walk, collidersInRange);
                if (!intersects) {
                    nextNodes.add(new PathNode(new IntPoint((int) pt.x, (int) pt.y), p));
                }
            }
        }
        for (Point pt : corners) {
            Line walk = new Line(p.getPoint().DPoint(), pt);
            boolean intersects = CollisionUtil.LineIntersectsWithColliders(walk, collidersInRange);
            if (!intersects) {
                nextNodes.add(new PathNode(new IntPoint((int) pt.x, (int) pt.y), p));
            }
        }
        return nextNodes;
    }
    
    public static List<Line> getCornerLines(List<Collider> colliders) {
        //check if two lines make a corner
        List<Line> cornerLines = new ArrayList<>();
        
        for (int i = 0; i < colliders.size(); i++) {
            for (int j = i + 1; j < colliders.size(); j++) {
                    IntPoint p1 = new IntPoint(colliders.get(i).getP1());
                    IntPoint p2 = new IntPoint(colliders.get(i).getP2());

                    IntPoint p3 = new IntPoint(colliders.get(j).getP1());
                    IntPoint p4 = new IntPoint(colliders.get(j).getP2());

                    Line l1 = colliders.get(i);
                    Line l2 = colliders.get(j);

                    IntPoint d1 = null;
                    IntPoint d2 = null;
                    IntPoint d3 = null;
                    IntPoint d4 = null;
                    if (p1.equals(p3)) {
                        d1 = p2;
                        d2 = p4;
                        d3 = p1;
                        d4 = p3;
                    } else if (p1.equals(p4)) {
                        d1 = p2;
                        d2 = p3;
                        d3 = p1;
                        d4 = p4;
                    } else if (p2.equals(p3)) {
                        d1 = p1;
                        d2 = p4;
                        d3 = p2;
                        d4 = p3;
                    } else if (p2.equals(p4)) {
                        d1 = p1;
                        d2 = p3;
                        d3 = p2;
                        d4 = p4;
                    } else {
                        continue;
                    }
                    Line across = new Line(d1.DPoint(), d2.DPoint());

                    Line l3 = new Line(across.center(), d3.DPoint());

                    // check if the lines are parallel
                    int slope = (int)(l1.dX()==0?Integer.MAX_VALUE:l1.dY()/l1.dX());
                    int slope2 = (int)(l2.dX() == 0 ? Integer.MAX_VALUE : l2.dY() / l2.dX());
                    
                    if(slope - slope2 < 0.00001 && slope - slope2 > -0.00001)
                        continue;
                    
                
                    
                    // if((slope + 1 / slope2< 1.00001 && slope + 1 / slope2 > 0.99999))
                    //     continue;
                    
                    
                    
                    // double shift_1 = (-MaxWithSign(d1.getX() - d3.getX(), d1.getY() - d3.getY())) / 2;
                    // double shift_2 = (-MaxWithSign(d2.getX() - d4.getX(), d2.getY() - d4.getY())) / 2;
                    
                    cornerLines.add(across.shift(l3.dX(), l3.dY()).shift(0.5, 0.5));
                    //cornerLines.add(across);

            }
        }
        return cornerLines;
    }
    
    public static Point[] getAdjacentPoints(Line c) {
        Point[] points = new Point[2];
        
        int[] sides = new int[] { -1, 1 };
        for (int z = 0; z < 2;z++) {
            double angle = c.angle();
            double iangle = angle + Math.PI / 2;
            int side = sides[z];
            //get point perpendicular to the collider and a distance of length/2
            double x = c.center().x + Math.cos(iangle) * c.length() / 2 * side;
            double y = c.center().y + Math.sin(iangle) * c.length() / 2 * side;
            points[z] = new Point(x, y);
            // Point p1 = new Point(c.center().x + side * Math.cos(iangle) * c.length() / 2,
            //         c.center().y + side * Math.sin(iangle) * c.length() / 2);
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
        while (node.parent != null) {

            path.add(node.getPoint());
            node = node.parent;
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
        List<Line> lines = getCornerLines(colliders);

        List<Point> corners = new ArrayList<>();
        for (Line l : lines) {
            corners.addAll(Arrays.stream(getAdjacentPoints(l)).toList());
        }
        return corners;
    }
}