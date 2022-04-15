package main;

import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.ImageCapabilities;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.VolatileImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.JPanel;

import org.javatuples.*;

import gameObjects.Bullet;
import gameObjects.Collider;
import gameObjects.ColorRect;
import gameObjects.LevelTile;
import gameObjects.LevelWall;
import gameObjects.ResetBox;
import inventory.Gun;
import inventory.ItemAttributes;
import inventory.Weapon;
import util.AppAsset;
import util.CollisionReturn;
import util.CollisionUtil;
import util.ImageImport;
import util.LevelConfigUtil;
import util.MathUtil;
import util.Point;
import util.Rect;
import util.SchemUtilities;
import util.ScreenAnimation;
import util.Size;
import util.Vector;

public class Application extends JPanel {
	Rect PLAYER_SCREEN_LOC = null;
	Point TOPLEFT_BOUND = new Point(0, 0);
	Point BOTTOMRIGHT_BOUND = new Point(0, 0);
	Point location = new Point(0, 0);

	/*
	 * GAME OBJECTS
	 */
	public List<Collider> colliders = new ArrayList<Collider>();
	public List<LevelWall> walls = new ArrayList<LevelWall>();
	public List<ResetBox> resetboxes = new ArrayList<ResetBox>();
	public List<LevelWall> newWalls = new ArrayList<LevelWall>();
	public List<LevelTile> tiles = new ArrayList<LevelTile>();
	public List<LevelTile> newTiles = new ArrayList<LevelTile>();
	public List<Collider> newColliders = new ArrayList<Collider>();
	public List<Bullet> bullets = new ArrayList<Bullet>();

	/*
	 * GAME ASSETS
	 */
	public HashMap<String, Point> checkpoints = new HashMap<String, Point>();
	public HashMap<String, AppAsset> assets = new HashMap<>();
	public HashMap<String, Color> colors = new HashMap<String, Color>();

	/*
	 * GRAPHICS OBJECTS
	 */
	Font DEBUG_TEXT = new Font("Arial", Font.PLAIN, 12);
	Font AMMO_TEXT = new Font("Arial", Font.PLAIN, 28);
	Font DEBUG_SELECT_TEXT = new Font("Arial", Font.PLAIN, 22);

	/*
	 * PLAYER PARAMETERS
	 */
	int dash_count = 0;
	double looking_angle = 0;
	Weapon weapon = ItemAttributes.DevTek_Rifle();
	long last_fire_tick = 0;
	Vector velocity = new Vector(0, 0);
	Vector intent = new Vector(1, 0);

	/*
	 * SELECTOR VARIABLES
	 */
	int selection_type = 0;
	String selectasset = "";
	// String selectcolor = "black";
	Point select_point_1 = new Point(0, 0);
	boolean select_preview = true;

	int asset_menu_index = 0;

	/*
	 * MISC
	 */
	boolean typing = false;
	String typing_str = "";

	/*
	 * GAME TOGGLES
	 */
	int debug_selection = 0;
	boolean SHOW_GRID = true;
	boolean LIGHT_MODE = true;
	boolean EDIT_MODE = false;
	boolean CLIP_MODE = false;
	boolean OVERLAY_MODE = false;
	boolean SHADOWS_MODE = true;
	boolean SHOW_ASSETS_MENU = false;

	GraphicsConfiguration gconfig = null;

	List<Double> debug_vals = Arrays.asList(0.0, 0.0, 0.0);
	int debug_val_selection = 0;

	List<Pair<String, Runnable>> debug_opts = new ArrayList<>();

	int select_val = 0;

	/*
	 * INIT METHOD
	 */
	public void Init(int width, int height) {

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		gconfig = ge.getDefaultScreenDevice().getDefaultConfiguration();

		ColorModel COLORMODEL = gconfig.getColorModel(Transparency.TRANSLUCENT);

		ac.createContext(COLORMODEL, COLORMODEL, null);
		ac_def.createContext(COLORMODEL, COLORMODEL, null);

		onResize(width, height);

		HashMap<String, Size> assetSizes = new HashMap<>();

		for (final File fileEntry : new File("assets").listFiles()) {
			if (fileEntry.isFile()) {
				String extension = fileEntry.getName().substring(fileEntry.getName().indexOf(".") + 1);
				if (!extension.equalsIgnoreCase("png") && !extension.equalsIgnoreCase("jpg")) {
					continue;
				}
				String name = fileEntry.getName().substring(0, fileEntry.getName().indexOf("."));
				BufferedImage img = ImageImport.getImage(fileEntry.getPath());
				Size size = new Size(img.getWidth() / Globals.PIXELS_PER_GRID,
						img.getHeight() / Globals.PIXELS_PER_GRID);
				assetSizes.put(name,
						size);
				if (selectasset.length() == 0)
					selectasset = name;
				BufferedImage resize = ImageImport.resize(img,
						(int) (img.getWidth() * Globals.PIXELS_RESIZE * 1.0f / Globals.PIXELS_PER_GRID),
						(int) (img.getHeight() * Globals.PIXELS_RESIZE * 1.0f / Globals.PIXELS_PER_GRID));
				assets.put(name, new AppAsset(resize, size));
			}
		}

		LevelConfigUtil.loadLevel(assetSizes);

		if (checkpoints.containsKey("start"))
			setPlayerPosFromSchem(checkpoints.get("start"));

		Collections.sort(walls, new Comparator<LevelWall>() {
			@Override
			public int compare(LevelWall a, LevelWall b) {
				if (a.getZ() > b.getZ())
					return 1;
				else if (a.getZ() == b.getZ())
					return 0;
				else
					return -1;
			}
		});

		levelUpdate();

		debug_opts.add(new Pair<String, Runnable>("Show Assets Menu", () -> {
			SHOW_ASSETS_MENU = true;
			asset_menu_index = 0;
		}));
		debug_opts.add(new Pair<String, Runnable>("Toggle Grid", () -> {
			SHOW_GRID = !SHOW_GRID;
		}));
		debug_opts.add(new Pair<String, Runnable>("Toggle Light", () -> {
			LIGHT_MODE = !LIGHT_MODE;
		}));
		debug_opts.add(new Pair<String, Runnable>("Toggle Shadows", () -> {
			SHADOWS_MODE = !SHADOWS_MODE;
		}));
		debug_opts.add(new Pair<String, Runnable>("Toggle Overlay", () -> {
			OVERLAY_MODE = !OVERLAY_MODE;
		}));
		debug_opts.add(new Pair<String, Runnable>("Toggle Edit Mode", () -> {
			EDIT_MODE = !EDIT_MODE;
		}));
		debug_opts.add(new Pair<String, Runnable>("Toggle Clip", () -> {
			CLIP_MODE = !CLIP_MODE;
		}));
		debug_opts.add(new Pair<String, Runnable>("Save Level", () -> {
			LevelConfigUtil.saveLevel();
			levelUpdate();
		}));
	}

	/*
	 * BACKEND GAME CODE
	 */
	public void onTick() {
		inputUpdate(true, typing, !typing);
		if (entry.peripherals.mouse_state)
			mouseDown(entry.peripherals.mousePos());

		if (typing) {
			if (entry.peripherals.keysTypedB()) {
				typing_str += entry.peripherals.keysTyped();
			}
			return;
		}

		// if (animation) {
		// double step_x = Globals.DASH_STEP * intent.getX();
		// double step_y = Globals.DASH_STEP * intent.getY();
		// if (entry.tick - dash_tick < Globals.DASH_DURATION) {

		// CollisionReturn collided = playerCollision(step_x, -step_y);
		// if (collided.x_collision && collided.y_collision) {
		// location.x += collided.disp_x;
		// location.y -= collided.disp_y;
		// dash_tick = 0;
		// animation = false;
		// } else if (collided.x_collision && !collided.y_collision) {
		// location.x += collided.disp_x;
		// location.y -= step_y;
		// } else if (!collided.x_collision && collided.y_collision) {
		// location.x += step_x;
		// location.y -= collided.disp_y;
		// }

		// } else {
		// animation = false;
		// }
		// } else
		playerCollisionAndMovementCode();

		// for (ResetBox b : resetboxes) {
		// Rect r = SchemUtilities.schemToLocal(b, location, Globals.GRIDSIZE);
		// boolean res = CollisionUtil.staticCollision(PLAYER_SCREEN_LOC, r);
		// if (res) {
		// deathscreen = true;
		// animation_tick = entry.tick;
		// setPlayerPosFromSchem(checkpoints.get(b.checkpoint));
		// }
		// }
	}

	void RawGame(Graphics2D g) {

		g.setColor(Color.GREEN);
		g.setStroke(new BasicStroke(1));

		if (SHOW_GRID) {
			for (int x1 = (int) TOPLEFT_BOUND.x; x1 < BOTTOMRIGHT_BOUND.x; x1++) {
				for (int y1 = (int) TOPLEFT_BOUND.y; y1 < BOTTOMRIGHT_BOUND.y; y1++) {
					Point p = new Point(x1 * Globals.GRIDSIZE - location.x, y1 * Globals.GRIDSIZE - location.y);

					Rect r1 = new Rect((int) p.x, (int) p.y, (int) (Globals.GRIDSIZE), (int) (Globals.GRIDSIZE));
					if (inScreenSpace(r1)) {
						g.drawLine((int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.x),
								(int) Math.floor(p.y + Globals.GRIDSIZE));

						g.drawLine((int) Math.floor(p.x), (int) Math.floor(p.y),
								(int) Math.floor(p.x + Globals.GRIDSIZE),
								(int) Math.floor(p.y));
					}
				}
			}
		}

		for (LevelTile o : tiles) {
			// if (Math.ceil(o.getZ()) <= 0) {
			Rect r = SchemUtilities.schemToLocalZ(o, PLAYER_SCREEN_LOC, location, o.getZ(), Globals.GRIDSIZE);
			if (inScreenSpace(r))
				paintLevelTile(g, (LevelTile) o);
			// }
		}
		for (LevelTile o : newTiles) {
			// if (Math.ceil(o.getZ()) <= 0) {
			Rect r = SchemUtilities.schemToLocalZ(o, PLAYER_SCREEN_LOC, location, o.getZ(), Globals.GRIDSIZE);
			if (inScreenSpace(r))
				paintLevelTile(g, (LevelTile) o);
			// }
		}

		for (LevelWall o : walls) {
			// if (Math.ceil(o.getZ()) <= 0) {
			Rect r = SchemUtilities.schemToLocalZ(o, PLAYER_SCREEN_LOC, location, o.getZ(), Globals.GRIDSIZE);
			if (inScreenSpace(r))
				paintLevelWall(g, (LevelWall) o);
			// }
		}

		for (LevelWall o : newWalls) {
			Rect r = SchemUtilities.schemToLocalZ(o, PLAYER_SCREEN_LOC, location, o.getZ(), Globals.GRIDSIZE);
			if (inScreenSpace(r))
				paintLevelWall(g, (LevelWall) o);
		}

		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(4));

		if (OVERLAY_MODE) {
			for (Collider o : colliders) {
				Rect r = SchemUtilities.schemToLocal(o, location, Globals.GRIDSIZE);
				if (inScreenSpace(r))
					g.drawRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
			}

			g.setColor(Color.ORANGE);
			for (Collider o : newColliders) {
				Rect r = SchemUtilities.schemToLocal(o, location, Globals.GRIDSIZE);

				if (inScreenSpace(r))
					g.drawRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
			}
		}

		g.setColor(Color.YELLOW);
		g.fillOval((int) PLAYER_SCREEN_LOC.x, (int) PLAYER_SCREEN_LOC.y, (int) PLAYER_SCREEN_LOC.width,
				(int) PLAYER_SCREEN_LOC.height);

		for (Bullet b : bullets) {
			Rect r = new Rect(b.x - location.x, b.y - location.y, b.width, b.height);
			if (inScreenSpace(r))
				g.drawRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
		}

	}

	private Shape LightMask(Graphics2D dispG, Graphics g) {
		double dx = 360 * Math.cos(looking_angle);
		double dy = 360 * Math.sin(looking_angle);
		// double r = 200;
		double small_radius = 40;

		Area visibility = new Area();

		if (LIGHT_MODE) {
			Polygon light = new Polygon();

			Point center = new Point(PLAYER_SCREEN_LOC.x + PLAYER_SCREEN_LOC.width / 2,
					PLAYER_SCREEN_LOC.y + PLAYER_SCREEN_LOC.height / 2);
			double angle_1 = looking_angle - Globals.FLASHLIGHT_FOV;
			double angle_2 = looking_angle + Globals.FLASHLIGHT_FOV;
			light.addPoint((int) (center.x + small_radius * Math.cos(angle_1)),
					(int) (center.y + small_radius * Math.sin(angle_1)));

			light.addPoint((int) (center.x + dx + Globals.FLASHLIGHT_RANGE * Math.cos(angle_1)),
					(int) (center.y + dy + Globals.FLASHLIGHT_RANGE * Math.sin(angle_1)));
			light.addPoint((int) (center.x + dx + Globals.FLASHLIGHT_RANGE * Math.cos(angle_2)),
					(int) (center.y + dy + Globals.FLASHLIGHT_RANGE * Math.sin(angle_2)));

			light.addPoint((int) (center.x + small_radius * Math.cos(angle_2)),
					(int) (center.y + small_radius * Math.sin(angle_2)));

			visibility.add(new Area(light));
			Ellipse2D e2d = new Ellipse2D.Double(center.x - Globals.INNER_RADIUS, center.y - Globals.INNER_RADIUS,
					Globals.INNER_RADIUS * 2, Globals.INNER_RADIUS * 2);
			visibility.add(new Area(e2d));

			// radial gradient
			RadialGradientPaint rgp = new RadialGradientPaint(new Point2D.Double(center.x, center.y),
					(float) Globals.INNER_RADIUS,
					new float[] { 0.0f, 0.5f, 1.0f },
					new Color[] { Color.BLACK, new Color(0, 0, 0, 220), new Color(0, 0, 0, 0) });

			dispG.setPaint(rgp);
			dispG.fill(new Ellipse2D.Double(center.getX() - Globals.INNER_RADIUS, center.getY() - Globals.INNER_RADIUS,
					Globals.INNER_RADIUS * 2, Globals.INNER_RADIUS * 2));

			// flashlight
			GradientPaint gp = new GradientPaint(new Point2D.Double(center.x, center.y), Color.BLACK,
					new Point2D.Double(center.x + Globals.FLASHLIGHT_RANGE * Math.cos(looking_angle),
							center.y + Globals.FLASHLIGHT_RANGE * Math.sin(looking_angle)),
					new Color(0, 0, 0, 0));
			dispG.setPaint(gp);
			dispG.fill(light);

			dispG.setPaint(null);
		} else {
			dispG.setBackground(Color.BLACK);
			dispG.clearRect(0, 0, this.getWidth(), this.getHeight());
			visibility.add(new Area(new Rectangle2D.Float(0, 0, this.getWidth(), this.getHeight())));
		}

		if (SHADOWS_MODE) {
			Area areas[] = new Area[] { new Area(), new Area() };
					Triplet<Double, Double, Double> args[] = new Triplet[] {
							new Triplet<Double, Double, Double>(0.0, -1.0, 0.0),
							new Triplet<Double, Double, Double>(-4.0, 0.0, -7.5) };

			for (LevelWall w : walls) {
				Rect rect = SchemUtilities.schemToLocalZ(w, PLAYER_SCREEN_LOC, location, 0, Globals.GRIDSIZE);
				if (inScreenSpace(rect)) {
					Shape s = new Rectangle2D.Float((int) rect.getX(), (int) rect.getY(), (int) rect.getWidth(),
							(int) rect.getHeight());

					double d2 = 800;

					Point PLAYER_CENTER = new Point(PLAYER_SCREEN_LOC.x + PLAYER_SCREEN_LOC.width / 2,
							PLAYER_SCREEN_LOC.y + PLAYER_SCREEN_LOC.height / 2);

					double overall_angle = (Math.atan2(PLAYER_CENTER.y - (rect.getY() + rect.getHeight() / 2),
							rect.getX() + rect.getWidth() / 2 - PLAYER_CENTER.x) + Math.PI * 2) % (Math.PI * 2);
					double angle_tl = (Math.atan2(PLAYER_CENTER.y - rect.getY(),
							rect.getX() - PLAYER_CENTER.x) + Math.PI * 2) % (Math.PI * 2);
					double angle_br = (Math.atan2(PLAYER_CENTER.y - (rect.getY() + rect.getHeight()),
							rect.getX() + rect.getWidth() - PLAYER_CENTER.x) + Math.PI * 2) % (Math.PI * 2);
					double angle_tr = (Math.atan2(PLAYER_CENTER.y - rect.getY(),
							rect.getX() + rect.getWidth() - PLAYER_CENTER.x) + Math.PI * 2) % (Math.PI * 2);
					double angle_bl = (Math.atan2(PLAYER_CENTER.y - (rect.getY() + rect.getHeight()),
							rect.getX() - PLAYER_CENTER.x) + Math.PI * 2) % (Math.PI * 2);

					// start points



					for (int i = 0; i < areas.length; i++) {
						Polygon o = new Polygon();
						Double center_points_buff = args[i].getValue0();
						Double corner_buff = args[i].getValue1();
						Double end_buff = args[i].getValue2();

						Point inner_first = null;
						Pair<String, Point> outer_first = null;
						Pair<String, Point> outer_last = null;
						Point inner_last = null;
						if (angle_br >= 0 && angle_br < Math.PI / 2) {
							inner_first = new Point((int) (rect.getX() + rect.getWidth() - corner_buff),
									(int) (rect.getY() + rect.getHeight() - corner_buff));

							outer_first = PointOnScreenEdge(angle_br, new Point(rect.getX() + rect.getWidth() - end_buff,
									rect.getY() + rect.getHeight() - end_buff));

							// g.setColor(Color.RED);
							// g.fillOval((int) (rect.getX() + rect.getWidth()), (int) (rect.getY() + rect.getHeight()), 5,
							// 		5);

						}else
						if (angle_tr >= Math.PI / 2 && angle_tr < Math.PI) {
							inner_first = new Point((int) (rect.getX() + rect.getWidth() - corner_buff),
									(int) Math.ceil(rect.getY() + corner_buff));

							outer_first = PointOnScreenEdge(angle_tr,
									new Point(rect.getX() + rect.getWidth() - end_buff, rect.getY() + end_buff));

							// g.setColor(Color.YELLOW);
							// g.fillOval((int) (rect.getX() + rect.getWidth()), (int) (rect.getY()), 5, 5);

						}else
						if (angle_tl >= Math.PI && angle_tl < Math.PI * 3 / 2) {
							inner_first = new Point((int) Math.ceil(rect.getX() + corner_buff),
									(int) Math.ceil(rect.getY() + corner_buff));

							outer_first = PointOnScreenEdge(angle_tl, new Point(rect.getX()+end_buff, rect.getY()+end_buff));

							// g.setColor(Color.GREEN);
							// g.fillOval((int) (rect.getX()), (int) (rect.getY()), 5, 5);
						}else
						if (angle_bl >= Math.PI * 3 / 2 && angle_bl < Math.PI * 2) {
							inner_first = new Point((int) Math.ceil(rect.getX() + corner_buff),
									(int) (rect.getY() + rect.getHeight() - corner_buff));

							outer_first = PointOnScreenEdge(angle_bl,
									new Point(rect.getX() + end_buff, rect.getY() + rect.getHeight() - end_buff));


							// g.setColor(Color.BLUE);
							// g.fillOval((int) (rect.getX()), (int) (rect.getY() + rect.getHeight()), 5, 5);
						}

						// end points
						if (angle_tl >= 0 && angle_tl < Math.PI / 2) {
							outer_last = PointOnScreenEdge(angle_tl, new Point(rect.getX()+end_buff, rect.getY()+end_buff));
							
							inner_last = new Point((int) Math.ceil(rect.getX() + corner_buff),
									(int) Math.ceil(rect.getY() + corner_buff));

							// g.setColor(Color.CYAN);
							// g.fillRect((int) (rect.getX()), (int) (rect.getY()), 5, 5);
						}else
						if (angle_bl >= Math.PI / 2 && angle_bl < Math.PI) {
							outer_last = PointOnScreenEdge(angle_bl,
									new Point(rect.getX() + end_buff, rect.getY() + rect.getHeight() - end_buff));
							
							inner_last = new Point((int) Math.ceil(rect.getX() + corner_buff),
									(int) (rect.getY() + rect.getHeight() - corner_buff));
							// g.setColor(Color.MAGENTA);
							// g.fillRect((int) (rect.getX()), (int) (rect.getY() + rect.getHeight()), 5, 5);
						}else
						if (angle_br >= Math.PI && angle_br < Math.PI * 3 / 2) {
							outer_last = PointOnScreenEdge(angle_br, new Point(rect.getX()+rect.getWidth()-end_buff, rect.getY()+rect.getHeight()-end_buff));
							// o.addPoint((int) (rect.getX() + rect.getWidth() + d2 * Math.cos(angle_br) - end_buff),
							// 		(int) (rect.getY() + rect.getHeight() - d2 * Math.sin(angle_br) - end_buff));
							
							inner_last = new Point((int) (rect.getX() + rect.getWidth() - corner_buff),
									(int) (rect.getY() + rect.getHeight() - corner_buff));

							// g.setColor(Color.PINK);
							// g.fillRect((int) (rect.getX() + rect.getWidth()), (int) (rect.getY() + rect.getHeight()), 5,
							// 		5);
						}else
						if (angle_tr >= Math.PI * 3 / 2 && angle_tr < Math.PI * 2) {
							outer_last = PointOnScreenEdge(angle_tr,
									new Point(rect.getX() + rect.getWidth() - end_buff, rect.getY() + end_buff));
							// o.addPoint((int) (rect.getX() + rect.getWidth() + d2 * Math.cos(angle_tr) - end_buff),
							// 		(int) (rect.getY() - d2 * Math.sin(angle_tr) + end_buff));

							inner_last = new Point((int) (rect.getX() + rect.getWidth() - corner_buff),
									(int) Math.ceil(rect.getY() + corner_buff));

						}
						

						if (inner_first != null) {
							o.addPoint((int) inner_first.getX(), (int) inner_first.getY());
							if (i == 0) {
								g.setColor(Color.WHITE);
								g.fillRect((int) inner_first.x - Globals.OVERLAY_MARKER_SIZE / 2,
										(int) inner_first.y - Globals.OVERLAY_MARKER_SIZE / 2,
										Globals.OVERLAY_MARKER_SIZE, Globals.OVERLAY_MARKER_SIZE);
							}
						}

						if (outer_first != null) {
							o.addPoint((int) outer_first.getValue1().x, (int) outer_first.getValue1().y);
							if (i == 0) {
								g.setColor(Color.ORANGE);
								g.fillRect((int) outer_first.getValue1().getX() - Globals.OVERLAY_MARKER_SIZE / 2,
										(int) outer_first.getValue1().getY() - Globals.OVERLAY_MARKER_SIZE / 2,
										Globals.OVERLAY_MARKER_SIZE, Globals.OVERLAY_MARKER_SIZE);
							}
						}
						if (outer_first != null && outer_last != null) {
							if (!outer_first.getValue0().equals(outer_last.getValue0())) {
								if ((outer_first.getValue0().equals("top") && outer_last.getValue0().equals("left")) ||
										(outer_first.getValue0().equals("left")
												&& outer_last.getValue0().equals("top"))) {
									o.addPoint(0, 0);
								} else if ((outer_first.getValue0().equals("top")
										&& outer_last.getValue0().equals("right"))
										||
										(outer_first.getValue0().equals("right")
												&& outer_last.getValue0().equals("top"))) {
									o.addPoint(this.getWidth(), 0);
								} else if ((outer_first.getValue0().equals("bottom")
										&& outer_last.getValue0().equals("left")) ||
										(outer_first.getValue0().equals("left")
												&& outer_last.getValue0().equals("bottom"))) {
									o.addPoint(0, this.getHeight());
								} else if ((outer_first.getValue0().equals("bottom")
										&& outer_last.getValue0().equals("right")) ||
										(outer_first.getValue0().equals("right")
												&& outer_last.getValue0().equals("bottom"))) {
									o.addPoint(this.getWidth(), this.getHeight());
								} else if ((outer_first.getValue0().equals("top")
										&& outer_last.getValue0().equals("bottom")) ||
										(outer_first.getValue0().equals("bottom")
												&& outer_last.getValue0().equals("top"))) {
									if (overall_angle >= Math.PI / 2 && overall_angle < Math.PI * 3 / 2) {
										o.addPoint(0, 0);
										o.addPoint(0, this.getHeight());
									} else {
										o.addPoint(this.getWidth(), this.getHeight());
										o.addPoint(this.getWidth(), 0);
									}
								} else if ((outer_first.getValue0().equals("left")
										&& outer_last.getValue0().equals("right")) ||
										(outer_first.getValue0().equals("right")
												&& outer_last.getValue0().equals("left"))) {
									if (overall_angle >= 0 && overall_angle < Math.PI) {
										o.addPoint(0, 0);
										o.addPoint(this.getWidth(), 0);
									} else {
										o.addPoint(0, this.getHeight());
										o.addPoint(this.getWidth(), this.getHeight());
									}
								}
							}
						}

						if (outer_last != null) {
							o.addPoint((int) outer_last.getValue1().getX(), (int) outer_last.getValue1().getY());
							if (i == 0) {
								g.setColor(Color.BLUE);
								g.fillRect((int) outer_last.getValue1().getX() - Globals.OVERLAY_MARKER_SIZE / 2,
										(int) outer_last.getValue1().getY() - Globals.OVERLAY_MARKER_SIZE / 2,
										Globals.OVERLAY_MARKER_SIZE, Globals.OVERLAY_MARKER_SIZE);
							}
						}

						if (inner_last != null) {
							o.addPoint((int) inner_last.x, (int) inner_last.y);
							if (i == 0) {
								g.setColor(Color.GREEN);
								g.fillRect((int) inner_last.x - Globals.OVERLAY_MARKER_SIZE / 2,
										(int) inner_last.y - Globals.OVERLAY_MARKER_SIZE / 2,
										Globals.OVERLAY_MARKER_SIZE, Globals.OVERLAY_MARKER_SIZE);
							}
						}



						if (angle_bl > Math.PI / 2 && angle_bl < Math.PI && angle_br > 0 && angle_br < Math.PI / 2) {
							o.addPoint((int) (rect.getX() + center_points_buff),
									(int) (rect.getY() + rect.getHeight() + center_points_buff));
							o.addPoint((int) (rect.getX() + rect.getWidth() - center_points_buff),
									(int) (rect.getY() + rect.getHeight() + center_points_buff));
						}
						if (angle_bl > Math.PI / 2 && angle_bl < Math.PI && angle_tr > Math.PI / 2
								&& angle_tr < Math.PI) {
							o.addPoint((int) (rect.getX() + rect.getWidth() + center_points_buff),
									(int) (rect.getY() + rect.getHeight() + center_points_buff));
						}
						if (angle_tr > Math.PI / 2 && angle_tr < Math.PI && angle_br > Math.PI
								&& angle_br < Math.PI * 3 / 2) {
							o.addPoint((int) (rect.getX() + rect.getWidth() + center_points_buff),
									(int) (rect.getY() + rect.getHeight() - center_points_buff));
							o.addPoint((int) (rect.getX() + rect.getWidth() + center_points_buff),
									(int) (rect.getY() + center_points_buff));
						}
						if (angle_tr > Math.PI && angle_tr < Math.PI * 3 / 2 && angle_br > Math.PI
								&& angle_br < Math.PI * 3 / 2) {
							o.addPoint((int) (rect.getX() + rect.getWidth() + center_points_buff),
									(int) (rect.getY() - center_points_buff));
						}
						if (angle_tl > Math.PI && angle_tl < Math.PI * 3 / 2 && angle_tr > Math.PI * 3 / 2
								&& angle_tr < Math.PI * 2) {
							o.addPoint((int) (rect.getX() + rect.getWidth() - center_points_buff),
									(int) (rect.getY() - center_points_buff));
							o.addPoint((int) (rect.getX() + center_points_buff),
									(int) (rect.getY() - center_points_buff));
						}
						if (angle_tr > Math.PI * 3 / 2 && angle_tr < Math.PI * 2 && angle_bl > Math.PI * 3 / 2
								&& angle_bl < Math.PI * 2) {
							o.addPoint((int) (rect.getX() - center_points_buff),
									(int) (rect.getY() - center_points_buff));
						}
						if (angle_tl > 0 && angle_tl < Math.PI / 2 && angle_br > Math.PI * 3 / 2
								&& angle_br < Math.PI * 2) {
							o.addPoint((int) (rect.getX() - center_points_buff),
									(int) (rect.getY() + center_points_buff));
							o.addPoint((int) (rect.getX() - center_points_buff),
									(int) (rect.getY() + rect.getHeight() - center_points_buff));
						}
						if (angle_tl > 0 && angle_tl < Math.PI / 2 && angle_br > 0 && angle_br < Math.PI / 2) {
							o.addPoint((int) (rect.getX() - center_points_buff),
									(int) (rect.getY() + rect.getHeight() + center_points_buff));
						}
						areas[i].add(new Area(o));
					}

				}
			}
			visibility.subtract(new Area(areas[0]));

			Shape vis = areas[1];
			dispG.setPaint(null);
			for (LevelWall wall : walls) {
				Rect wallrect = SchemUtilities.schemToLocalZ(wall, PLAYER_SCREEN_LOC, location, wall.getZ(),
						Globals.GRIDSIZE);
				if (inScreenSpace(wallrect)) {
					Rectangle2D re = new Rectangle2D.Double((int)Math.round(wallrect.getX()), (int)Math.round(wallrect.getY()),
							wallrect.getWidth(),
							wallrect.getHeight());


					if (!vis.contains(re)) {

						visibility.add(new Area((Shape)re));
					}
				}
			}


		}
		return visibility;
	}

	void drawUI(Graphics2D g) {
		for (ResetBox b : resetboxes) {
			Rect r = SchemUtilities.schemToLocal(b, location, Globals.GRIDSIZE);
			if (inScreenSpace(r))
				g.drawRect((int) Math.floor(r.getX()), (int) Math.floor(r.getY()), (int) Math.floor(r.getWidth()),
						(int) Math.floor(r.getHeight()));
		}

		if (EDIT_MODE) {
			g.setColor(Color.RED);
			Point schem = SchemUtilities.schemPointFromFramePos(entry.peripherals.mousePos(), location,
					Globals.GRIDSIZE);
			Point newpoint1 = SchemUtilities.schemToLocalPoint(schem, location, Globals.GRIDSIZE);
			AppAsset a = assets.get(selectasset);
			if (a != null) {
				BufferedImage img = a.source;
				g.drawRect((int) newpoint1.x, (int) newpoint1.y, (int) (a.size.getWidth() * Globals.GRIDSIZE),
						(int) (a.size.getHeight() * Globals.GRIDSIZE));
			}
		}

		if (EDIT_MODE)
			g.setColor(Color.ORANGE);
		else
			g.setColor(Color.WHITE);
		Point mouse = entry.peripherals.mousePos();
		Point schem_mouse = SchemUtilities.schemPointFromFramePos(mouse, location, Globals.GRIDSIZE);
		int z = 3;
		g.fillOval((int) mouse.x - z, (int) mouse.y - z, 2 * z, 2 * z);

		g.setFont(DEBUG_TEXT);
		String focus = selectasset;
		// if (selecttype == 2)
		// focus = selectcolor;
		g.drawString(
				String.format(
						"raw=(%5.1f,%5.1f)  coord=(%5.1f,%5.1f) typing=%b focus=[%s] select_type=%d debug_val=[%s]",
						mouse.x, mouse.y, schem_mouse.x, schem_mouse.y, typing, focus, selection_type,
						debug_vals.toString()),
				20,
				g.getFontMetrics().getAscent() + 20);

		if (EDIT_MODE) {
			g.setStroke(new BasicStroke(6));
			g.setColor(Color.BLUE);
			g.drawRect((int) LEVEL_SCREEN_SPACE.getX(), (int) LEVEL_SCREEN_SPACE.getY(),
					(int) LEVEL_SCREEN_SPACE.getWidth(), (int) LEVEL_SCREEN_SPACE.getHeight());
		}

		if (typing) {
			g.setFont(AMMO_TEXT);
			g.setColor(Color.RED);
			g.drawString(typing_str, 40, 40);
		}

		g.setColor(Color.RED);
		g.fillRect(20, getHeight() - 60, 200, 10);

		g.setStroke(new BasicStroke(2));
		g.setColor(Color.BLACK);
		g.drawRect(20, getHeight() - 60, 200, 10);
		g.drawRect(20, getHeight() - 40, 200, 10);

		g.setColor(Color.WHITE);
		g.setFont(AMMO_TEXT);

		Gun gun = (Gun) weapon;
		if (gun.mag != null) {
			String bullet_str = String.format("%d/%d", gun.mag.bullet_count, gun.mag.BULLET_MAX());
			g.drawString(bullet_str,
					(int) (-g.getFontMetrics().getStringBounds(bullet_str, g).getWidth() + this.getWidth() - 20),
					(int) (-g.getFontMetrics().getAscent() - 20 + this.getHeight()));
		}

		g.setColor(Color.WHITE);
		g.setStroke(new BasicStroke(2));
		Point mousepos = entry.peripherals.mousePos();
		if (entry.tick - last_fire_tick < gun.FIRING_DELAY()) {
			int size1 = 14;
			double percent = ((entry.tick - last_fire_tick) * 1.0f / gun.FIRING_DELAY());
			g.drawArc((int) mousepos.x - size1, (int) mousepos.y - size1, 2 * size1, 2 * size1, 0,
					(int) (360 * percent));
		}
	}

	void drawDebug(Graphics2D g) {

		if (SHOW_ASSETS_MENU) {
			ArrayList<String> keys = new ArrayList<String>(assets.keySet());

			final int BUFFER = 50;
			final int BETWEEN_IMG = 10;
			int img_size = (int) (this.getWidth() - 2 * BUFFER - (Globals.ASSET_MENU_HORIZONTAL + 1) * BETWEEN_IMG)
					/ Globals.ASSET_MENU_HORIZONTAL;
			g.setColor(new Color(80, 80, 80, 200));
			g.fillRect(BUFFER, BUFFER, this.getWidth() - 2 * BUFFER, this.getHeight() - 2 * BUFFER);
			for (int y = 0; y <= (int) (assets.size() / Globals.ASSET_MENU_HORIZONTAL); y++) {
				for (int x = 0; x < Globals.ASSET_MENU_HORIZONTAL; x++) {

					int index = y * Globals.ASSET_MENU_HORIZONTAL + x;
					if (index >= assets.size())
						break;

					int posx = BUFFER + x * (img_size) + (x + 1) * BETWEEN_IMG;
					int posy = BUFFER + y * (img_size) + (y + 1) * 2 * BETWEEN_IMG;
					BufferedImage img = assets.get(keys.get(index)).source;

					if (asset_menu_index == index) {
						g.setColor(Color.ORANGE);
						g.setStroke(new BasicStroke(8));
						g.fillRect(posx - 4, posy - 4, img_size + 8, img_size + 8);
					}
					g.drawImage(img, posx, posy, img_size, img_size, null);
					g.setColor(Color.WHITE);
					String msg = keys.get(index);
					int height = g.getFontMetrics().getAscent();
					int width = (int) g.getFontMetrics().getStringBounds(msg, g).getWidth();
					g.drawString(msg, posx + img_size / 2 - width / 2, posy + img_size + 10 + height);
				}
			}
		} else {
			final int VERT_SPACING = 20;
			final int VERT_MID_SPACING = 6;
			final int SIDE_SPACING = 20;

			g.setFont(DEBUG_SELECT_TEXT);
			int text_height = g.getFontMetrics(DEBUG_SELECT_TEXT).getAscent();

			final int TOTAL_HEIGHT = text_height * debug_opts.size() + VERT_MID_SPACING * (debug_opts.size())
					+ 2 * VERT_SPACING;
			final int TOTAL_WIDTH = debug_opts.stream()
					.map(s -> g.getFontMetrics(DEBUG_SELECT_TEXT).stringWidth(s.getValue0()))
					.max(Comparator.naturalOrder()).get() + 2 * SIDE_SPACING;
			Rect bound = Rect.fromPoints(this.getWidth() - 20 - TOTAL_WIDTH, 20, this.getWidth() - 20,
					20 + TOTAL_HEIGHT);

			g.setColor(Color.DARK_GRAY);
			g.fillRect((int) bound.getX(), (int) bound.getY(), (int) bound.getWidth(), (int) bound.getHeight());

			g.setColor(Color.LIGHT_GRAY);
			g.fillRect((int) bound.x,
					(int) (bound.y + VERT_SPACING + (debug_selection) * (text_height + VERT_MID_SPACING)),
					(int) bound.width, (int) text_height + 8);

			g.setColor(Color.WHITE);

			for (int i = 0; i < debug_opts.size(); i++) {
				Pair<String, Runnable> pair = debug_opts.get(i);

				String name = pair.getValue0();

				g.drawString(name, (int) (bound.x + SIDE_SPACING),
						(int) (bound.y + (i + 1) * text_height + (i) * VERT_MID_SPACING + VERT_SPACING));
			}
		}
	}
	/*
	 * PAINT METHOD
	 */

	Rect LEVEL_SCREEN_SPACE;
	VolatileImage display = null;
	VolatileImage extra = null;

	AlphaComposite ac_def = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
	AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_IN, 1.0f);

	@Override
	public void paint(Graphics g1) {
		Graphics2D g = (Graphics2D) g1;

		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		LEVEL_SCREEN_SPACE = new Rect(
				Math.max(0, (TOPLEFT_BOUND.x) * Globals.GRIDSIZE - location.x),
				Math.max(0, (TOPLEFT_BOUND.y) * Globals.GRIDSIZE - location.y),
				MathUtil.min_(BOTTOMRIGHT_BOUND.x * Globals.GRIDSIZE - location.x,
						getWidth() - TOPLEFT_BOUND.x * Globals.GRIDSIZE + location.x,
						(BOTTOMRIGHT_BOUND.x - TOPLEFT_BOUND.x) * Globals.GRIDSIZE, getWidth()),
				MathUtil.min_(BOTTOMRIGHT_BOUND.y * Globals.GRIDSIZE - location.y,
						getHeight() - TOPLEFT_BOUND.y * Globals.GRIDSIZE + location.y,
						(BOTTOMRIGHT_BOUND.y - TOPLEFT_BOUND.y) * Globals.GRIDSIZE, getHeight()));

		Graphics2D dispG = (Graphics2D) display.getGraphics();
		Graphics2D extraG = (Graphics2D) extra.getGraphics();

		dispG.setBackground(new Color(0, 0, 0, 0));
		dispG.clearRect(0, 0, this.getWidth(), this.getHeight());
		extraG.setBackground(new Color(0, 0, 0, 0));
		extraG.clearRect(0, 0, this.getWidth(), this.getHeight());
		g.setBackground(Color.BLACK);
		g.clearRect(0, 0, this.getWidth(), this.getHeight());

		if (LIGHT_MODE || SHADOWS_MODE) {
			dispG.setComposite(ac_def);
			// DRAW MASK

			Shape mask = LightMask(dispG, extraG);
			dispG.setClip(mask);

			// dispG.setColor(new Color(0, 0, 0, 25));
			// dispG.fillRect(0, 0, this.getWidth(), this.getHeight());

			dispG.setComposite(ac);

			// DRAW ACTUAL GAME

			RawGame(dispG);

			dispG.setComposite(ac_def);

			// DRAW ANYTHING ELSE

			g.drawImage(display, 0, 0, null);
		} else {
			RawGame(g);
		}
		if (OVERLAY_MODE)
			g.drawImage(extra, 0, 0, null);
		drawUI(g);
		drawDebug(g);
	}

	/*
	 * RESIZE EVENT
	 */
	public void onResize(double width, double height) {
		PLAYER_SCREEN_LOC = new Rect((width - Globals.PLAYER_SIZE) / 2,
				(height + 0.0 * height - Globals.PLAYER_SIZE) / 2,
				Globals.PLAYER_SIZE,
				Globals.PLAYER_SIZE);

		ImageCapabilities ic = new ImageCapabilities(true);
		try {

			display = gconfig.createCompatibleVolatileImage((int) width, (int) height, ic, Transparency.TRANSLUCENT);
			extra = gconfig.createCompatibleVolatileImage((int) width, (int) height, ic, Transparency.TRANSLUCENT);
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	public void mouseDown(Point pos) {
		if (!EDIT_MODE) {
			Gun g = (Gun) weapon;
			if (g.FIRING_TYPE() == 1 && g.mag != null) {
				if (entry.tick - last_fire_tick > g.FIRING_DELAY()) {
					FireBullet();
				}
			}
		}
	}

	public Pair<String, Point> PointOnScreenEdge(double angle, Point start_point) {
		double x0 = start_point.x;
		double y0 = start_point.y;

		double slope = Math.tan(angle);
		int dx = (int) Math.copySign(1, Math.cos(angle));
		int dy = (int) Math.copySign(1, Math.sin(angle));

		double xf_0 = (x0 + (y0) / slope);
		double xf_H = (x0 + (y0-this.getHeight()) / slope);
		double yf_0 = (y0 + (x0) * slope);
		double yf_W = (y0 + (x0-this.getWidth()) * slope);

		if (xf_0 > 0 && xf_0 < this.getWidth()&& dy==1) {
			return new Pair<String, Point>("top", new Point((int) xf_0, 0));
		} else
		if (xf_H > 0 && xf_H < this.getWidth() && dy == -1) {
			return new Pair<String, Point>("bottom", new Point((int) xf_H, this.getHeight()));
		}
		if(yf_0 > 0 && yf_0 < this.getHeight() && dx == -1) {
			return new Pair<String, Point>("left", new Point(0, (int) yf_0));
		} else
		if(yf_W > 0 && yf_W < this.getHeight() && dx == 1) {
			return new Pair<String, Point>("right", new Point(this.getWidth(), (int) yf_W));
		}
		return new Pair<String, Point>("none", new Point(0, 0));
	}

	/*
	 * MOUSE CLICK EVENT
	 */
	public void mouseClick(Point pos) {
		if (EDIT_MODE) {
			if (select_preview) {
				select_point_1 = SchemUtilities.schemPointFromFramePos(pos, location, Globals.GRIDSIZE);

				if (selection_type == 1) {
					AppAsset a = assets.get(selectasset);
					// else if(selecttype == 2) img = assets.get(selectcolor);

					Rect r = new Rect(select_point_1.x, select_point_1.y, a.size.getWidth(),
							a.size.getHeight());

					LevelWall c = new LevelWall(r.getX(), r.getY(), r.getWidth(), r.getHeight(), 0.0f, selectasset);
					newWalls.add(c);
				} else if (selection_type == 2) {
					AppAsset a = assets.get(selectasset);
					// else if(selecttype == 2) img = assets.get(selectcolor);

					Rect r = new Rect(select_point_1.x, select_point_1.y, a.size.getWidth(),
							a.size.getHeight());

					LevelTile c = new LevelTile(r.getX(), r.getY(), r.getWidth(), r.getHeight(), 0.0f, selectasset);
					newTiles.add(c);
				} else if (selection_type == 0) {
					AppAsset a = assets.get(selectasset);

					Rect r = new Rect(select_point_1.x, select_point_1.y, a.size.getWidth(),
							a.size.getHeight());

					Collider c = new Collider(r.getX(), r.getY(), r.getWidth(), r.getHeight());
					newColliders.add(c);
				}
				// }
				levelUpdate();
			}
		} else {
			Gun g = (Gun) weapon;
			if (g.FIRING_TYPE() == 0 && g.mag != null) {
				if (entry.tick - last_fire_tick > g.FIRING_DELAY()) {
					FireBullet();
				}
			}
		}
	}

	void FireBullet() {
		Gun g = (Gun) weapon;
		Point pos = entry.peripherals.mousePos();

		if (g.MAG_TYPE() != g.mag.NAME()) {
			System.out.println("WRONG MAG INSERTED");
		}

		Point arm = new Point(PLAYER_SCREEN_LOC.x + location.x + Globals.PLAYER_SIZE / 2,
				PLAYER_SCREEN_LOC.y + location.y + Globals.PLAYER_SIZE / 2);
		double angle = (Math.atan2(
				-pos.y + PLAYER_SCREEN_LOC.y,
				-pos.x + PLAYER_SCREEN_LOC.x + Globals.PLAYER_SIZE / 2)) % (2 * Math.PI) + Math.PI;
		Point start = new Point(arm.x + Globals.BULLET_DEFAULT_DISTANCE * Math.cos(angle),
				arm.y + Globals.BULLET_DEFAULT_DISTANCE * Math.sin(angle));
		Bullet b = new Bullet(start.x, start.y, g.BULLET_SIZE(), angle, g.mag.BULLET_INITIAL_SPEED());
		bullets.add(b);

		last_fire_tick = entry.tick;
		g.mag.bullet_count--;
		if (g.mag.bullet_count == 0)
			g.mag = null;
	}

	/*
	 * INPUT MANAGEMENT
	 * Set local variables and toggles based on peripheral inputs
	 */
	void inputUpdate(boolean essentials, boolean typing_en, boolean game) {

		if (essentials) {
			if (entry.peripherals.KeyToggled(KeyEvent.VK_PERIOD)) {
				typing = !typing;
				if (!typing)
					typing_str = "";
				entry.peripherals.typingEnable(typing);
			}

			if (entry.peripherals.KeyToggled(KeyEvent.VK_RIGHT)) {
				if (asset_menu_index < assets.size() - 1)
					asset_menu_index++;
			}
			if (entry.peripherals.KeyToggled(KeyEvent.VK_LEFT)) {
				if (asset_menu_index > 0)
					asset_menu_index--;
			}

			if (entry.peripherals.KeyPressed(KeyEvent.VK_I)) {
				debug_vals.set(debug_val_selection, debug_vals.get(debug_val_selection) + 0.01);
			}
			if (entry.peripherals.KeyPressed(KeyEvent.VK_K)) {
				debug_vals.set(debug_val_selection, debug_vals.get(debug_val_selection) - 0.01);
			}
			if (entry.peripherals.KeyToggled(KeyEvent.VK_L)) {
				if (debug_val_selection == debug_vals.size() - 1)
					debug_val_selection = 0;
				else debug_val_selection++;
			}

			if (entry.peripherals.KeyToggled(KeyEvent.VK_UP)) {
				if (SHOW_ASSETS_MENU) {
					if (asset_menu_index - Globals.ASSET_MENU_HORIZONTAL > 0)
						asset_menu_index -= Globals.ASSET_MENU_HORIZONTAL;
				} else if (debug_selection > 0)
					debug_selection--;
			} else if (entry.peripherals.KeyToggled(KeyEvent.VK_DOWN)) {
				if (SHOW_ASSETS_MENU) {
					if (asset_menu_index + Globals.ASSET_MENU_HORIZONTAL < assets.size())
						asset_menu_index += Globals.ASSET_MENU_HORIZONTAL;
				}
				if (debug_selection < debug_opts.size() - 1)
					debug_selection++;
			}

			if (entry.peripherals.KeyToggled(KeyEvent.VK_SPACE)) {
				if (SHOW_ASSETS_MENU) {
					selectasset = new ArrayList<String>(assets.keySet()).get(asset_menu_index);
					SHOW_ASSETS_MENU = false;
				} else
					debug_opts.get(debug_selection).getValue1().run();
			}
		}

		if (typing_en) {

			if (entry.peripherals.KeyToggled(KeyEvent.VK_ENTER)) {
				if (typing_str.length() > 0) {
					typing = false;
					typing_str = typing_str.strip();
					selectasset = typing_str;

					typing_str = "";
					entry.peripherals.typingEnable(false);
				}
			}
			if (entry.peripherals.KeyToggled(KeyEvent.VK_COMMA)) {
				if (typing_str.length() > 0) {
					typing_str = typing_str.substring(0, typing_str.length() - 1);
				}
			}
		}

		if (game) {
			looking_angle = (Math.atan2(
					-entry.peripherals.mousePos().y + PLAYER_SCREEN_LOC.y,
					-entry.peripherals.mousePos().x + PLAYER_SCREEN_LOC.x + Globals.PLAYER_SIZE / 2)) % (2 * Math.PI)
					+ Math.PI;

			short intent_x = 0;
			short intent_y = 0;

			if (entry.peripherals.KeyPressed(KeyEvent.VK_W))
				intent_y++;
			if (entry.peripherals.KeyPressed(KeyEvent.VK_S))
				intent_y--;
			if (entry.peripherals.KeyPressed(KeyEvent.VK_D))
				intent_x++;
			if (entry.peripherals.KeyPressed(KeyEvent.VK_A))
				intent_x--;

			if (intent_x != 0 || intent_y != 0) {
				intent = Vector.fromComponentsUnitVector(intent_x, intent_y);
			} else {
				intent = Vector.zero();
			}

			if (entry.peripherals.KeyToggled(KeyEvent.VK_B)) {
				selection_type++;
				if (selection_type >= 3)
					selection_type = 0;
			}

			if (entry.peripherals.KeyToggled(KeyEvent.VK_U)) {
				((Gun) weapon).mag = ItemAttributes.DevTek_Mag();
			}
		}
	}

	/*
	 * PLAYER COLLISION AND MOVEMENT CODE
	 */
	public void playerCollisionAndMovementCode() {
		double component_x = 0;
		double component_y = 0;

		velocity = velocity.addVector(intent.multiply(0.14));// intent

		if (velocity.getMagnitude() > Globals.PLAYER_MAX_SPEED)
			velocity.setMagnitude(Globals.PLAYER_MAX_SPEED);
		if (intent.equals(Vector.zero())) {
			if (velocity.getMagnitude() < Globals.PLAYER_MIN_SPEED_CUTOFF)
				velocity = Vector.zero();
			else
				velocity = velocity.addVector(velocity.multiply(-0.08));// friction
		}

		component_x = velocity.getX();
		component_y = velocity.getY();

		if (!CLIP_MODE) {
			{
				CollisionReturn ret = playerCollision(component_x, component_y);
				if (ret != null) {
					if (ret.y_collision) {
						component_y = 0;
						location.y += Math.copySign(1, velocity.getY()) * -ret.disp_y;
					}
					if (ret.x_collision) {
						component_x = 0;
						location.x += Math.copySign(1, velocity.getX()) * ret.disp_x;
					}
				}
			}

			for (Collider c : colliders) {
				Rect collider = SchemUtilities.schemToLocal(c, location, Globals.GRIDSIZE);

				for (int i = 0; i < bullets.size(); i++) {
					Bullet b = bullets.get(i);
					Rect bullet = new Rect(b.x - location.x, b.y - location.y, b.width, b.height);
					double dx = b.speed * Globals.BULLET_SPEED_MULT * Math.cos(b.angle);
					double dy = b.speed * Globals.BULLET_SPEED_MULT * Math.sin(b.angle);

					double speed_x = dx;
					double speed_y = dy;
					double new_angle = Math.atan2(speed_y, speed_x);
					double new_speed = Math.sqrt(Math.pow(speed_x, 2) + Math.pow(speed_y, 2));
					b.speed = new_speed;
					b.angle = new_angle;

					boolean ret = CollisionUtil.staticCollision(bullet, collider);

					if (ret || Math.sqrt(Math.pow(bullet.x, 2) + Math.pow(bullet.y, 2)) > 4000) {
						bullets.remove(i);
						i--;
					} else {
						b.x += dx;
						b.y += dy;
					}
				}

			}
		}
		// dash_count = Globals.DASH_COUNT;
		location.x += component_x;
		location.y -= component_y;
	}

	/*
	 * CHECKS PLAYER COLLISION WITH ALL OBJECTS IN THE SCENE
	 */
	public CollisionReturn playerCollision(double x, double y) {
		CollisionReturn colret = null;
		List<Rect> objects = new ArrayList<>();
		objects.addAll((List<Rect>) colliders.stream()
				.map(c -> SchemUtilities.schemToLocal(c, location, Globals.GRIDSIZE))
				.filter(c -> inScreenSpace(c))
				.collect(Collectors.toList()));
		objects.addAll((List<Rect>) newColliders.stream()
				.map(c -> SchemUtilities.schemToLocal(c, location, Globals.GRIDSIZE))
				.filter(c -> inScreenSpace(c))
				.collect(Collectors.toList()));
		// objects.addAll((List<Rect>) walls.stream()
		// .filter(c -> c.getZ() == 0)
		// .map(c -> SchemUtilities.schemToLocal(c, location, Globals.GRIDSIZE))
		// .filter(c -> inScreenSpace(c))
		// .collect(Collectors.toList()));

		for (Rect r : objects) {
			CollisionReturn ret = CollisionUtil.DynamicCollision(PLAYER_SCREEN_LOC, r, x, y);

			if (ret.x_collision || ret.y_collision) {
				if (colret == null)
					colret = ret;
				else {
					if (ret.x_collision) {
						colret.x_collision = true;
						if (Math.abs(ret.disp_x) > Math.abs(colret.disp_x))
							colret.disp_x = ret.disp_x;
					}
					if (ret.y_collision) {
						colret.y_collision = true;
						if (Math.abs(ret.disp_y) > Math.abs(colret.disp_y))
							colret.disp_y = ret.disp_y;
					}
				}
			}

		}
		return colret;
	}

	/*
	 * UPDATE SCHEMATIC LEVEL BOUNDS BASED ON GAME OBJECTS
	 */
	void levelUpdate() {
		TOPLEFT_BOUND = new Point(Double.MAX_VALUE, Double.MAX_VALUE);
		BOTTOMRIGHT_BOUND = new Point(-Double.MAX_VALUE, -Double.MAX_VALUE);

		List<Rect> all = new ArrayList<>();
		all.addAll(walls.stream().map(c -> (Rect) c).collect(Collectors.toList()));
		all.addAll(colliders.stream().map(c -> (Rect) c).collect(Collectors.toList()));
		all.addAll(newColliders.stream().map(c -> (Rect) c).collect(Collectors.toList()));
		all.addAll(newWalls.stream().map(c -> (Rect) c).collect(Collectors.toList()));
		all.addAll(tiles.stream().map(c -> (Rect) c).collect(Collectors.toList()));
		all.addAll(newTiles.stream().map(c -> (Rect) c).collect(Collectors.toList()));

		for (Rect o : all) {
			if (o.getX() < TOPLEFT_BOUND.x)
				TOPLEFT_BOUND.x = o.getX();
			if (o.getX() + o.getWidth() > BOTTOMRIGHT_BOUND.x)
				BOTTOMRIGHT_BOUND.x = o.getX() + o.getWidth();
			if (o.getY() < TOPLEFT_BOUND.y)
				TOPLEFT_BOUND.y = o.getY();
			if (o.getY() + o.getHeight() > BOTTOMRIGHT_BOUND.y)
				BOTTOMRIGHT_BOUND.y = o.getY() + o.getHeight();
		}
	}

	/*
	 * Move player given a SCHEMATIC coordinate
	 */
	void setPlayerPosFromSchem(Point p) {
		location = new Point(p.getX() * Globals.GRIDSIZE - PLAYER_SCREEN_LOC.getX(),
				p.getY() * Globals.GRIDSIZE - PLAYER_SCREEN_LOC.getY() - PLAYER_SCREEN_LOC.getHeight() / 2
						- 1.5 * Globals.GRIDSIZE);
	}

	/*
	 * CHECK IF RECTANGLE IS INSIDE SCREEN SPACE
	 */
	boolean inScreenSpace(Rect r) {
		return CollisionUtil.staticCollision(new Rect(0, 0, this.getWidth(), this.getHeight()), r);
	}

	void paintColorRect(Graphics g, ColorRect rect, double depth) {
		Rect r = SchemUtilities.schemToLocal(rect, location, Globals.GRIDSIZE);
		float c = 0.05f;
		if (!entry.app.colors.containsKey(rect.getColor())) {
			g.setColor(Color.RED);
			g.fillRect((int) Math.floor(r.getX() - c), (int) Math.floor(r.getY() - c),
					(int) Math.ceil(r.getWidth() + c), (int) Math.ceil(r.getHeight() + c));
		} else {
			g.setColor(entry.app.colors.get(rect.getColor()));
			g.fillRect((int) Math.floor(r.getX() - c), (int) Math.floor(r.getY() - c),
					(int) Math.ceil(r.getWidth() + c), (int) Math.ceil(r.getHeight() + c));
		}
	}

	void paintLevelWall(Graphics g, LevelWall p) {
		Rect r = SchemUtilities.schemToLocalZ(p, PLAYER_SCREEN_LOC, location, p.getZ(), Globals.GRIDSIZE);
		if (!entry.app.assets.containsKey(p.getAsset())) {
			g.setColor(Color.RED);
			g.fillRect((int) Math.round(r.getX()), (int) Math.round(r.getY()), (int) r.getWidth(), (int) r.getHeight());
		} else {
			BufferedImage img = entry.app.assets.get(p.getAsset()).source;
			g.drawImage(img, (int) Math.round(r.getX()), (int) Math.round(r.getY()), (int) r.getWidth(),
					(int) r.getHeight(), null);
		}
	}

	void paintLevelTile(Graphics g, LevelTile p) {
		Rect r = SchemUtilities.schemToLocalZ(p, PLAYER_SCREEN_LOC, location, p.getZ(), Globals.GRIDSIZE);
		if (!entry.app.assets.containsKey(p.getAsset())) {
			g.setColor(Color.RED);
			g.fillRect((int) Math.round(r.getX()), (int) Math.round(r.getY()), (int) r.getWidth(), (int) r.getHeight());
		} else {
			BufferedImage img = entry.app.assets.get(p.getAsset()).source;
			g.drawImage(img, (int) Math.round(r.getX()), (int) Math.round(r.getY()), (int) r.getWidth(),
					(int) r.getHeight(), null);
		}
	}

	BufferedImage resizeToGrid(BufferedImage img, double width, double height) {
		return ImageImport.resize(img, (int) (width / Globals.GRIDSIZE * Globals.PIXELS_PER_GRID),
				(int) (height / Globals.GRIDSIZE * Globals.PIXELS_PER_GRID));
	}

}