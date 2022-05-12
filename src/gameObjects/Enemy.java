package gameObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.javatuples.Pair;

import main.Globals;
import main.entry;
import templates.DirectionVector;
import templates.IntPoint;
import templates.Line;
import templates.PathNode;
import templates.Point;
import templates.Rect;
import util.CollisionUtil;
import util.MathUtil;
import util.PathfindingUtil;

public class Enemy {
    List<Point> path;
    Point pos;
    double radius;
    Function<Pair<IntPoint, IntPoint>, List<Point>> follow_routine;
    double health = 100;

    public Enemy(double x, double y, double radius, Function<Pair<IntPoint, IntPoint>, List<Point>> follow_routine) {
        this.pos = new Point(x, y);
        this.radius = radius;
        this.follow_routine = follow_routine;
    }

    int index;
    final double DEF_SPEED = 0.03;

    public void updateRoutine(IntPoint start, IntPoint end, boolean override) {
        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
                List<Point> path2 = new ArrayList<>();
                List<Point> newpoints = follow_routine.apply(Pair.with(start, end));
                

                if (newpoints != null) {
                    path2.addAll(newpoints);
                    Line l = new Line(pos, path2.get(1));
                    if (!CollisionUtil.LineIntersectsWithColliders(l, entry.app.colliders)) {
                        path2.set(0, pos);
                    }
                    path2.set(path2.size()-1, entry.app.playerSchemPos().shift(-0.5, -0.5));
                    path = path2;
                    index = 0;

                } else {
                    if (override == true)
                        path = null;
                }
            }
        });
        thr.start();

    }

    public DirectionVector getIntent() {
        if (path == null)
            return null;
        if (index >= path.size() - 1)
            return null;

        // Point p1 = path.get(index);
        Point p2 = path.get(index + 1);
        double angle = Math.atan2(p2.getY() - pos.getY(), p2.getX() - pos.getX());

        return new DirectionVector(DEF_SPEED, angle);
    }

    public void step(List<Enemy> enemies) {
        if (path == null)
            return;
        if (index >= path.size() - 1)
            return;

        Point p2 = path.get(index + 1);

        DirectionVector intent = getIntent();
        for (Enemy e : enemies) {
            if (e == this)
                continue;
            Pair<Point,Point> collision = CollisionUtil.sphereCollision(pos, radius, e.pos, e.radius, intent, e.getIntent());
            if (collision != null) {
                Line ln = new Line(collision.getValue0(), collision.getValue1());
                Line newline = MathUtil.extendLine(ln, (radius + e.radius) + 0.2);
                this.pos = newline.getP1();
                e.pos = newline.getP2();
                return;
            }

        }

        pos = pos.shift(intent.getDX(), intent.getDY());

        if (p2.distance(pos) < 0.1) {
            index++;
            pos = p2;
        }
    }

    public void removeHealth(double rm) {
        health -= rm;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public Point getPos() {
        return pos;
    }

    public IntPoint getIntPos() {
        return new IntPoint((int) pos.getX(), (int) pos.getY());
    }

    public double getRadius() {
        return radius;
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

            // if(!(t2.getX() > entry.app.TOPLEFT_BOUND.getX() &&
            // t2.getX()<entry.app.BOTTOMRIGHT_BOUND.getX() && t2.getY() >
            // entry.app.TOPLEFT_BOUND.getY() &&
            // t2.getY()<entry.app.BOTTOMRIGHT_BOUND.getY())) {
            // return false;
            // }

            // double dx = pend.getX() - pstart.getX();
            // double dy = pend.getY() - pstart.getY();

            Line cross = new Line(pstart.DPoint(), pend.DPoint());

            for (Collider c : entry.app.colliders) {
                Line c_line = new Line(c.getX1() - 0.5, c.getY1() - 0.5, c.getX2() - 0.5, c.getY2() - 0.5);
                boolean collided = CollisionUtil.LineLineIntersection(cross, c_line);
                if (collided) {
                    return false;
                }
            }

            return true;
        }
    };

    public double getHealth() {
        return health;
    }

    public void setPos(Point p2) {
        pos = p2;
    }

}
