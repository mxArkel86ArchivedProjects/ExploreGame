package util;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.javatuples.Pair;

import gameObjects.Collider;
import main.Globals;
import main.entry;

public class Enemy {
    List<Point> path;
    Point pos;
    double size;
    
    public Enemy(double x, double y, double size) {
        this.pos = new Point(x, y);
        this.size = size;
    }

    int index;
    int t;
    final double speed = 0.03;
    double totalDist = 0;

    public void step() {
        if (path == null)
            return;
        if (index >= path.size() - 1)
            return;

       // while(pos.distance(p.get(p.size()-1)) > 0.04) {
            Point p1 = path.get(index);
            Point p2 = path.get(index + 1);
            double angle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
            //double dist = p1.distance(p2);

            double dx = Math.cos(angle) * speed;
            double dy = Math.sin(angle) * speed;
            
            pos.x += dx;
            pos.y += dy;

            if(pos.distance(p2) < 0.04) {
                index++;
            }
       // }
        

        // if (t > dist / speed) {
        //     index++;
        //     t = 0;
        //     return;
        // } else if (t + 1 > dist / speed) {
        //     pos.x = p2.x;
        //     pos.y = p2.y;
        // } else {
        //     pos.x += dx;
        //     pos.y += dy;
        // }
        // t++;
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

        List<Point> p1 = PathFinding.PathFindByWalls(new PathNode(start, null), new PathNode(end, null), 6,
                entry.app.colliders);
        
        if (p1 != null) {
            Line l = new Line(pos, p1.get(1));
            if (!CollisionUtil.LineIntersectsWithColliders(l, entry.app.colliders)) {
                p1.set(0, pos);
            }
            this.path = p1;

        } else {
            this.path = null;
        }
    }

    public List<Point> getPath() {
        return path;
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
