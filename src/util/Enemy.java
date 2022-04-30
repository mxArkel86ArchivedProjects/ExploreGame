package util;

import java.util.function.Function;
import java.util.stream.Collectors;

import org.javatuples.Pair;

import gameObjects.Collider;
import main.Globals;
import main.entry;

public class Enemy extends Rect {
    Path p;
    
    public Enemy(double x, double y, double width, double height) {
        super(x, y, width, height);
    }

    int index;
    int t;
    final double speed = 0.03;

    public void step() {
        if (p == null || p.path==null)
            return;
        if (index >= p.path.size() - 1)
            return;

        Point p1 = p.getPathPoints().get(index);
        Point p2 = p.getPathPoints().get(index + 1);
        double dist = Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));

        double dx = p2.x==p1.x?0:(p2.x - p1.x)/dist * speed;
        double dy = p2.y==p1.y?0:(p2.y - p1.y)/dist * speed;

        if (t > dist / speed) {
            index++;
            t = 0;
            return;
        } else if (t + 1 > dist / speed) {
            this.setP1(p2);
        } else {
            this.setX(this.left() + dx);
            this.setY(this.top() + dy);
        }
        t++;
    }

    public void updatePath(Point p2, Point location, double GRIDSIZE) {
        index = 0;
        t = 0;
		Point p1 = new Point(left(), top());
		Rect r = new Rect(p1, p2);

		Path path = PathFinding.PathFind(new PathNode((int)Math.round(p1.getX()), (int)Math.round(p1.getY()), null), new PathNode((int)p2.getX(), (int)p2.getY(), null),
				new Function<Pair<Point, Point>, Boolean>() {
					@Override
					public Boolean apply(Pair<Point, Point> p) {
                        Point pstart = p.getValue0();
                        Point pend = p.getValue1();
                        
                        boolean inMap = (pend.getX() >= r.left() - Globals.PATH_BUFFER
                                && pend.getX() <= r.right() + Globals.PATH_BUFFER
                                && pend.getY() >= r.top() - Globals.PATH_BUFFER
                                && pend.getY() <= r.bottom() + Globals.PATH_BUFFER);

                        if (!inMap)
                            return false;

                        // if(!(t2.getX() > entry.app.TOPLEFT_BOUND.getX() && t2.getX()<entry.app.BOTTOMRIGHT_BOUND.getX() && t2.getY() > entry.app.TOPLEFT_BOUND.getY() && t2.getY()<entry.app.BOTTOMRIGHT_BOUND.getY())) {
                        //     return false;
                        // }

                        double dx = pend.getX() - pstart.getX();
                        double dy = pend.getY() - pstart.getY();

                        // for (Collider c : entry.app.colliders) {
                        //     Rect r1 = new Rect(pstart.getX() - 0.2, pstart.getY() - 0.2, 0.4, 0.4);
                            
                        //     CollisionReturn ret = CollisionUtil.SchemDynamicCollision(r1,
                        //             c, dx, dy);
                        //     if (ret.colliding()) {
                        //         return false;
                        //     }
                        // }
                        
						return true;
					}
                });
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
    
}
