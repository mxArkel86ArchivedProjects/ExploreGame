package inventory;

public class Magazine extends Item {
    public int bullet_count;
    private int BULLET_MAX;
    private String NAME;
    private double BULLET_SPEED;
    private double BULLET_SIZE;

    public Magazine(int BULLET_MAX) {
        this.BULLET_MAX = BULLET_MAX;
        bullet_count = BULLET_MAX;
    }

    public Magazine(int BULLET_MAX, String NAME, double BULLET_SPEED, double BULLET_SIZE) {
        this.BULLET_MAX = BULLET_MAX;
        this.NAME = NAME;
        this.BULLET_SPEED = BULLET_SPEED;
        this.BULLET_SIZE = BULLET_SIZE;
        bullet_count = BULLET_MAX;
    }

    public int getBulletCount() {
        return bullet_count;
    }

    public void setBulletCount(int bullet_count) {
        this.bullet_count = bullet_count;
    }

    public int BULLET_MAX() {
        return BULLET_MAX;
    }

    public String NAME() {
        return NAME;
    }

    public double BULLET_SPEED() {
        return BULLET_SPEED;
    }

    public double BULLET_SIZE() {
        return BULLET_SIZE;
    }
}
