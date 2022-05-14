package main;

public class AppConstants {
	// WINDOW SETTINGS
	public static final double REFRESH_RATE = 60;
	public static int WINDOW_WIDTH_INITIAL = 1920;
	public static int WINDOW_HEIGHT_INITIAL = 1080;

	// IMAGE SETTINGS
	public static final int PIXELS_PER_GRID() {
		return (int) (64.0);
	}

	public static final int PIXELS_PER_GRID_IMPORT() {
		return (int) (50);
	}

	public static final int PATH_BUFFER = 6;
	public static final int PIXELS_RESIZE = 64;
	// APPLICATION PARAMS
	public static final double PLAYER_SIZE = 50;
	public static final double PLAYER_ACCELERATION = 0.1;
	public static final double PLAYER_MAX_SPEED = 2.8;
	public static final double PLAYER_FRICTION = 0.1;
	public static final double PLAYER_MIN_SPEED_CUTOFF = 0.1;
	public static final double SPRINT_DRAIN = 0.003;
	public static final double SPRINT_REGEN = 0.0015;
	public static final double SPRINT_DELAY = 1000;
	public static final double SPRINT_MULT = 1.4;
	public static final double BULLET_DEFAULT_DISTANCE = 40;
	public static final double BULLET_MAX_DISTANCE = 80;
	public static final int DASH_COUNT = 2;
	public static final double DASH_STEP = 20;
	public static final int DASH_DURATION = 180;
	public static final double DASH_PERCENT_FALLOFF_SPEED = 0.04;
	public static final int DASH_DELAY = 100;
	public static final double BULLET_SPEED = 0.4;
	// LIGHTING
	public static final double FLASHLIGHT_FOV = 0.7;
	public static final int INNER_RADIUS = 250;
	public static final int FLASHLIGHT_RANGE = 540;
	public static final int OVERLAY_MARKER_SIZE = 10;
	public static final int LAMP_RADIUS = 180;
}
