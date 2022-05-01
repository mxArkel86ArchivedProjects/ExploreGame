package util;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.javatuples.Pair;

import gameObjects.Collider;
import main.Globals;
import main.entry;

public class Enemy {
    Path p;
    Point pos;
    double size;
    
    public Enemy(double x, double y, double size) {
        this.pos = new Point(x, y);
        this.size = size;
    }

    int index;
    int t;
    final double speed = 0.03;

    public void step() {
        if (p == null || p.path == null)
            return;
        if (index >= p.path.size() - 1)
            return;

        IntPoint p1 = p.getPathPoints().get(index);
        IntPoint p2 = p.getPathPoints().get(index + 1);
        double dist = Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));

        double dx = p2.x == p1.x ? 0 : (p2.x - p1.x) / dist * speed;
        double dy = p2.y == p1.y ? 0 : (p2.y - p1.y) / dist * speed;

        if (t > dist / speed) {
            index++;
            t = 0;
            return;
        } else if (t + 1 > dist / speed) {
            pos.x = p2.x;
            pos.y = p2.y;
        } else {
            pos.x += dx;
            pos.y += dy;
        }
        t++;
    }
    
    public Point getPos() {
        return pos;
    }

    public IntPoint getIntPos() {
        return new IntPoint((int) pos.x, (int) pos.y);
    }
    
    public double getSize() {
        return size;
    }

    public void updatePath(IntPoint start, IntPoint end, Point location, double GRIDSIZE) {
        index = 0;
        t = 0;

        // Path path = PathFinding.PathFindByGrid(new PathNode((int) (start.getX()), (int) (start.getY()), null),
        //         new PathNode((int) end.getX(), (int) end.getY(), null),
        //         check);
        Path path = PathFinding.PathFindByWalls(new PathNode(start, null), new PathNode(end, null), 6, entry.app.colliders);
        if (path != null) {
            this.p = path;
            // p.path.add(0, p1);
            //path.optimizePath();
        } else {
            this.p = null;
        }
    }

    public Path getPath() {
        return p;
    }

    public static final Function<Pair<IntPoint, IntPoint>, Boolean> check = new Function<Pair<IntPoint, IntPoint>, Boolean>() {
        @Override
        public Boolean apply(Pair<IntPoint, IntPoint> p) {
            IntPoint pstart = p.getValue0();
            IntPoint pend = p.getValue1();

            Rect r = new Rect(pstart.DPoint(), pend.DPoint());
            
            boolean inMap = (pend.getX() >= r.left() - Globals.PATH_BUFFER
                    && pend.getX() <= r.right() + Globals.PATH_BUFFER
                    && pend.getY() >= r.top() - Globals.PATH_BUFFER
                    && pend.getY() <= r.bottom() + Globals.PATH_BUFFER);

            if (!inMap)
                return false;

            // if(!(t2.getX() > entry.app.TOPLEFT_BOUND.getX() && t2.getX()<entry.app.BOTTOMRIGHT_BOUND.getX() && t2.getY() > entry.app.TOPLEFT_BOUND.getY() && t2.getY()<entry.app.BOTTOMRIGHT_BOUND.getY())) {
            //     return false;
            // }

            // double dx = pend.getX() - pstart.getX();
            // double dy = pend.getY() - pstart.getY();

            Line cross = new Line(pstart.DPoint(), pend.DPoint());

            for (Collider c : entry.app.colliders) {
                Line c_line = new Line(c.getX1()-0.5, c.getY1()-0.5, c.getX2()-0.5, c.getY2()-0.5);
                boolean collided = CollisionUtil.LineLineIntersection(cross,c_line);
                if (collided) {
                    return false;
                }
            }
            
            return true;
        }
    };
    
}
