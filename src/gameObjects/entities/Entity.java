package gameObjects.entities;

import templates.DirectionVector;
import templates.Point;
import templates.Vector;

public class Entity {
    double radius;
    Point position;
    DirectionVector directionVector;

    public Entity(double radius, Point position, DirectionVector directionVector) {
        this.radius = radius;
        this.position = position;
        this.directionVector = directionVector;
    }

    public Vector getVector() {
        return new Vector(position, directionVector);
    }

    public DirectionVector getDirectionVector() {
        return directionVector;
    }

    public Point getPosition() {
        return position;
    }

    public double getRadius() {
        return radius;
    }
}
