package templates;

public class CalcVector {
    double x;
    double y;
    
    public CalcVector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getMagnitude() {
        return Math.sqrt(x * x + y * y);
    }

    public double getAngle() {
        return Math.atan2(y, x);
    }

    public CalcVector getUnitVector() {
        double magnitude = getMagnitude();
        return new CalcVector(x / magnitude, y / magnitude);
    }

    public CalcVector getScaledVector(double scale) {
        return new CalcVector(x * scale, y * scale);
    }

    public CalcVector add(CalcVector other) {
        return new CalcVector(x + other.getX(), y + other.getY());
    }

    public CalcVector subtract(CalcVector other) {
        return new CalcVector(x - other.getX(), y - other.getY());
    }
}
