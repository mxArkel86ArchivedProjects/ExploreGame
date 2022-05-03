package templates;

public class PolarLine {
    double magnitude;
    double angle;

    public PolarLine(double magnitude, double angle) {
        this.magnitude = magnitude;
        this.angle = angle;
    }

    public static PolarLine fromComponents(double x, double y) {
        double m = Math.sqrt(x * x + y * y);
        double a = Math.atan2(y, x);
        return new PolarLine(m, a);
    }
    public static PolarLine fromComponentsUnitVector(double x, double y) {
        double a = Math.atan2(y, x);
        return new PolarLine(1, a);
    }
    
    public PolarLine addVector(PolarLine v) {
        double dx = this.magnitude * Math.cos(this.angle) + v.magnitude * Math.cos(v.angle);
        double dy = this.magnitude * Math.sin(this.angle) + v.magnitude * Math.sin(v.angle);
        return PolarLine.fromComponents(dx, dy);
    }

    public PolarLine subtractVector(PolarLine v) {
        double dx = this.magnitude * Math.cos(this.angle) - v.magnitude * Math.cos(v.angle);
        double dy = this.magnitude * Math.sin(this.angle) - v.magnitude * Math.sin(v.angle);
        return PolarLine.fromComponents(dx, dy);
    }

    public PolarLine multiply(double scalar) {
        return new PolarLine(this.magnitude * scalar, this.angle);
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

    public static PolarLine zero() {
        return new PolarLine(0, 0);
    }
    
    @Override
    public boolean equals(Object o) {
        if(o instanceof PolarLine v)
            return this.magnitude == v.magnitude && this.angle == v.angle;
        return false;
    }
}
