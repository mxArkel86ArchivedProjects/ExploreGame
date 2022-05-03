package gameObjects;

import main.Globals;
import templates.Point;
import templates.Rect;

public class Bullet {
    public double angle;
    public double speed;
    private double size;
    private Point topleft;

    public Bullet(double x, double y, double size, double angle, double speed) {
        this.angle = angle;
        this.speed = speed;
        this.size = size;
        this.topleft = new Point(x, y);
    }
    
    public void moveBullet() {
        double dx = speed * Math.cos(angle);
        double dy = speed * Math.sin(angle);
        topleft = topleft.shift(dx, dy);
    }

    public double getSize() {
        return size;
    }
    
    public Point getPos() {
        return topleft;
    }
    
}
