package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

public class PathFinding {
    public static List<Pathfind_Int> PathFind(Pathfind_Node start, Pathfind_Node end, Function<Pathfind_Int, Boolean> inMap) {
        Queue<Pathfind_Node> queue = new LinkedList<>();
        queue.add(start);
        List<Pathfind_Int> visited = new ArrayList<>();

        List<Pathfind_Node> add_node = new ArrayList<>();

        while(queue.size() > 0){
            while (!queue.isEmpty()) {
                Pathfind_Node node = queue.poll();

                if (node.equals(end)) {
                    return getPath(node, start);
                }

                visited.add(node.getPoint());
                for (Pathfind_Node neighbor : getNeighbors(node)) {
                    if (!visited.contains(neighbor.getPoint()) && !queue.contains(neighbor)) {
                        if (inMap == null || inMap.apply(neighbor.getPoint())) {
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
    
    private static List<Pathfind_Int> getPath(Pathfind_Node node, Pathfind_Node start) {
        List<Pathfind_Int> path = new ArrayList<>();

        while (node.parent != null) {
            path.add(node.getPoint());
            node = node.parent;
        }
        path.add(start.getPoint());
        Collections.reverse(path);
        return path;
    }

    private static List<Pathfind_Node> getNeighbors(Pathfind_Node node) {
        List<Pathfind_Node> neighbors = Arrays.asList(
                new Pathfind_Node(node.getPoint().x + 1, node.getPoint().y, node),//right
                new Pathfind_Node(node.getPoint().x - 1, node.getPoint().y, node),//left
                new Pathfind_Node(node.getPoint().x, node.getPoint().y - 1, node),//down
                new Pathfind_Node(node.getPoint().x, node.getPoint().y + 1, node),//up
                new Pathfind_Node(node.getPoint().x+1, node.getPoint().y+1, node),//right-up
                new Pathfind_Node(node.getPoint().x-1, node.getPoint().y-1, node),//left-down
                new Pathfind_Node(node.getPoint().x+1, node.getPoint().y-1, node),//right-down
                new Pathfind_Node(node.getPoint().x-1, node.getPoint().y+1, node)//left-up
        );
        return neighbors;
    }
}