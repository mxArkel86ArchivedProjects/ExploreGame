package templates;

public class CollisionProps {
    private boolean collision_y;
    private boolean collision_x;
    private double displacement_x;
    private double displacement_y;

    public CollisionProps(boolean collision_y, boolean collision_x, double displacement_x, double displacement_y) {
        this.collision_y = collision_y;
        this.collision_x = collision_x;
        this.displacement_x = displacement_x;
        this.displacement_y = displacement_y;
    }

    public boolean isCollisionY() {
        return collision_y;
    }

    public boolean isCollisionX() {
        return collision_x;
    }

    public double getDisplacementX() {
        return displacement_x;
    }

    public double getDisplacementY() {
        return displacement_y;
    }

    public boolean isColliding() {
        return collision_x || collision_y;
    }
}
