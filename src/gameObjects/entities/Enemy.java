package gameObjects.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import org.javatuples.Pair;

import gameObjects.Collider;
import main.AppConstants;
import main.entry;
import templates.DirectionVector;
import templates.IntPoint;
import templates.PathNode;
import templates.Point;
import templates.Rect;
import templates.Vector;
import util.CollisionUtil;
import util.MathUtil;
import util.PathfindingUtil;

public class Enemy extends Entity {
    List<Point> path;
    Function<Pair<IntPoint, IntPoint>, List<Point>> follow_routine;
    double health = 100;

    public Enemy(double x, double y, double radius, Function<Pair<IntPoint, IntPoint>, List<Point>> follow_routine) {
        super(radius, new Point(x, y), DirectionVector.zero());
        this.follow_routine = follow_routine;
    }

    int index;
    final double DEF_SPEED = 0.03;

    public void updateRoutine(IntPoint start, IntPoint end, boolean override) {
        List<Point> path2 = new ArrayList<>();
        List<Point> newpoints = follow_routine.apply(Pair.with(start, end));

        if (newpoints != null) {
            path2.addAll(newpoints);
            Vector l = Vector.fromPoints(position, path2.get(1));
            if (!CollisionUtil.LineIntersectsWithColliders(l, entry.app.colliders)) {
                path2.set(0, position);
            }
            path2.set(path2.size() - 1, entry.app.playerSchemPos().shift(-0.5, -0.5));
            path = path2;
            index = 0;

        } else {
            if (override == true)
                path = null;
        }
    }

    // public void updateStep() {
        
    // }

    public void doStep() {
        if (path == null)
            return;
        if (index >= path.size() - 1)
            return;

        Point p2 = path.get(index + 1);

        double angle = Math.atan2(p2.getY() - position.getY(), p2.getX() - position.getX());
        directionVector = new DirectionVector(DEF_SPEED, angle);

        position = position.shift(directionVector.getDX(), directionVector.getDY());

        if (p2.distance(position) < 0.1) {
            index++;
            position = p2;
        }
    }

    public void removeHealth(double rm) {
        health -= rm;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public Point getPos() {
        return position;
    }

    public IntPoint getIntPos() {
        return new IntPoint((int) position.getX(), (int) position.getY());
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

            boolean inMap = (pend.getX() >= r.left() - AppConstants.PATH_BUFFER
                    && pend.getX() <= r.right() + AppConstants.PATH_BUFFER
                    && pend.getY() >= r.top() - AppConstants.PATH_BUFFER
                    && pend.getY() <= r.bottom() + AppConstants.PATH_BUFFER);

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

            Vector cross = Vector.fromPoints(pstart.DPoint(), pend.DPoint());

            for (Collider c : entry.app.colliders) {
                Vector c_line = Vector.fromPoints(c.origin(), c.destination()).shift(-0.5, -0.5);
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
        position = p2;
    }

}
