package util;

public class Vector {
    double magnitude;
    double angle;

    public Vector(double magnitude, double angle) {
        this.magnitude = magnitude;
        this.angle = angle;
    }

    public static Vector fromComponents(double x, double y) {
        double m = Math.sqrt(x * x + y * y);
        double a = Math.atan2(y, x);
        return new Vector(m, a);
    }
    public static Vector fromComponentsUnitVector(double x, double y) {
        double a = Math.atan2(y, x);
        return new Vector(1, a);
    }
    
    public Vector addVector(Vector v) {
        double dx = this.magnitude * Math.cos(this.angle) + v.magnitude * Math.cos(v.angle);
        double dy = this.magnitude * Math.sin(this.angle) + v.magnitude * Math.sin(v.angle);
        return Vector.fromComponents(dx, dy);
    }

    public Vector subtractVector(Vector v) {
        double dx = this.magnitude * Math.cos(this.angle) - v.magnitude * Math.cos(v.angle);
        double dy = this.magnitude * Math.sin(this.angle) - v.magnitude * Math.sin(v.angle);
        return Vector.fromComponents(dx, dy);
    }

    public Vector multiply(double scalar) {
        return new Vector(this.magnitude * scalar, this.angle);
    }
    
    public double getMagnitude() {
        return this.magnitude;
    }

    public double getAngle() {
        return this.angle;
    }

    public void setMagnitude(double magnitude) {
        this.magnitude = magnitude;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public double getX() {
        return this.magnitude * Math.cos(this.angle);
    }
    public double getY() {
        return this.magnitude * Math.sin(this.angle);
    }

    public static Vector zero() {
        return new Vector(0, 0);
    }
    
    @Override
    public boolean equals(Object o) {
        if(o instanceof Vector v)
            return this.magnitude == v.magnitude && this.angle == v.angle;
        return false;
    }
}
