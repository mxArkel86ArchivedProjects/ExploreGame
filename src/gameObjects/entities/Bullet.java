package gameObjects.entities;

import templates.DirectionVector;
import templates.Point;

public class Bullet extends Entity {


    public DirectionVector getDirectionVector() {
        return directionVector;
    }

    public Bullet(double x, double y, double radius, double angle, double speed) {
        super(radius, new Point(x, y), new DirectionVector(speed, angle));
    }
    
    public void moveBullet() {
        double dx = directionVector.getDX();
        double dy = directionVector.getDY();
        position = position.shift(dx, dy);
    }

    public double getRadius() {
        return radius;
    }
    
    public Point getPos() {
        return position;
    }
    
}
