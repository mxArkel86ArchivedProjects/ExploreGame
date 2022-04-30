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

        Point p1 = p.getPathPoints().get(index);
        Point p2 = p.getPathPoints().get(index + 1);
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
    
    public double getSize() {
        return size;
    }

    public void updatePath(Point p2, Point location, double GRIDSIZE) {
        index = 0;
        t = 0;
		Point p1 = new Point(pos.x, pos.y);

		Path path = PathFinding.PathFind(new PathNode((int)(p1.getX()), (int)(p1.getY()), null), new PathNode((int)p2.getX(), (int)p2.getY(), null),
				check);
        if (path != null) {
            this.p = path;
            p.path.add(0, p1);
            //path.optimizePath();
        } else {
            this.p = null;
        }
	}

    public Path getPath() {
        return p;
    }

    public static final Function<Pair<Point, Point>, Boolean> check = new Function<Pair<Point, Point>, Boolean>() {
        @Override
        public Boolean apply(Pair<Point, Point> p) {
            Point pstart = p.getValue0();
            Point pend = p.getValue1();

            Rect r = new Rect(pstart, pend);
            
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

            Point p1 = new Point(Math.round(pstart.getX()), Math.round(pstart.getY()));
            Point p2 = new Point(Math.round(pend.getX()), Math.round(pend.getY()));

            Line cross = new Line(p1, p2);

            for (Collider c : entry.app.colliders) {
                
                boolean collided = CollisionUtil.LineLineIntersection(cross,c);
                if (collided) {
                    return false;
                }
            }
            
            return true;
        }
    };
    
}
