package inventory;

public class Gun extends Weapon {
    public Magazine mag;
    private int FIRING_TYPE;
    private int FIRING_DELAY;

    public Gun(Magazine mag, int FIRING_TYPE, int FIRING_DELAY) {
        this.mag = mag;
        this.FIRING_TYPE = FIRING_TYPE;
        this.FIRING_DELAY = FIRING_DELAY;
    }

    public int FIRING_TYPE() {
        return FIRING_TYPE;
    }

    public int FIRING_DELAY() {
        return FIRING_DELAY;
    }
}
