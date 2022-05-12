package gameObjects;

import main.Globals;
import templates.DirectionVector;
import templates.Point;
import templates.Rect;

public class Bullet {
    public double angle;
    public double speed;
    private double radius;
    private Point center;

    public DirectionVector getDirectionVector() {
        return new DirectionVector(speed, angle);
    }

    public Bullet(double x, double y, double radius, double angle, double speed) {
        this.angle = angle;
        this.speed = speed;
        this.radius = radius;
        this.center = new Point(x, y);
    }
    
    public void moveBullet() {
        double dx = speed * Math.cos(angle);
        double dy = speed * Math.sin(angle);
        center = center.shift(dx, dy);
    }

    public double getRadius() {
        return radius;
    }
    
    public Point getPos() {
        return center;
    }
    
}
