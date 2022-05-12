package templates;

public class DirectionVector {
    double magnitude;
    double angle;

    public DirectionVector(double magnitude, double angle) {
        this.magnitude = magnitude;
        this.angle = angle;
    }

    public DirectionVector(DirectionVector v) {
        this.magnitude = v.magnitude;
        this.angle = v.angle;
    }

    public static DirectionVector fromComponents(double dx, double dy) {
        return new DirectionVector(Math.sqrt(dx * dx + dy * dy), Math.atan2(dy, dx));
    }
    
    public DirectionVector scale(double s) {
        return new DirectionVector(magnitude * s, angle);
    }

    public DirectionVector scale(double s, double t) {
        return DirectionVector.fromComponents(getDX()*s, getDY()*t);
    }
    
    public DirectionVector addVector(DirectionVector v) {
        double dx = this.magnitude * Math.cos(this.angle) + v.magnitude * Math.cos(v.angle);
        double dy = this.magnitude * Math.sin(this.angle) + v.magnitude * Math.sin(v.angle);
        return new DirectionVector(Math.sqrt(dx*dx + dy*dy), Math.atan2(dy, dx));
    }

    public DirectionVector subtractVector(DirectionVector v) {
        double dx = this.magnitude * Math.cos(this.angle) - v.magnitude * Math.cos(v.angle);
        double dy = this.magnitude * Math.sin(this.angle) - v.magnitude * Math.sin(v.angle);
        return new DirectionVector(Math.sqrt(dx*dx + dy*dy), Math.atan2(dy, dx));
    }

    public DirectionVector multiply(double scalar) {
        return new DirectionVector(this.magnitude * scalar, this.angle);
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

    public double getDX() {
        return this.magnitude * Math.cos(this.angle);
    }
    public double getDY() {
        return this.magnitude * Math.sin(this.angle);
    }

    public static DirectionVector zero() {
        return new DirectionVector(0, 0);
    }
    
    @Override
    public boolean equals(Object o) {
        if(o instanceof DirectionVector v)
            return this.magnitude == v.magnitude && this.angle == v.angle;
        return false;
    }
}
