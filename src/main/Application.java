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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.Function;
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
import util.Enemy;
import util.ImageImport;
import util.IntPoint;
import util.LevelConfigUtil;
import util.Line;
import util.MathUtil;
import util.Path;
import util.PathFinding;
import util.PathNode;
import util.Point;
import util.Rect;
import util.SchemUtilities;
import util.ScreenAnimation;
import util.Size;
import util.Vector;

public class Application extends JPanel {
	Rect PLAYER_SCREEN_LOC = null;
	Rect LEVEL_BOUND = new Rect(0, 0, 0, 0);
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
	double looking_angle = 0;
	Weapon weapon = ItemAttributes.DevTek_Rifle();
	long last_fire_tick = 0;
	Vector velocity = new Vector(0, 0);
	Vector intent = new Vector(1, 0);

	Enemy enemy = new Enemy(38,45,40);

	/*
	 * SELECTOR VARIABLES
	 */
	int selection_type = 0;
	String selectasset = "";

	int asset_menu_index = 0;

	/*
	 * MISC
	 */
	boolean typing = false;
	String typing_str = "";
	long reassign_tick = 0;

	/*
	 * GAME TOGGLES
	 */
	int debug_selection = 0;
	boolean SHOW_GRID = true;
	boolean LIGHT_MODE = false;
	boolean EDIT_MODE = false;
	boolean CLIP_MODE = false;
	boolean OVERLAY_MODE = false;
	boolean SHADOWS_MODE = false;
	boolean SHOW_ASSETS_MENU = false;
	boolean WALL_VISIBILITY = true;

	GraphicsConfiguration gconfig = null;

	List<Double> debug_vals = Arrays.asList(0.0, 0.0, 0.0);
	int debug_val_selection = 0;

	List<Triplet<String, Runnable, Callable<String>>> debug_opts = new ArrayList<>();

	int select_val = 0;

	List<PathNode> layers = null;

	/*
	 * INIT METHOD
	 */
	public void Init(int width, int height) {
		System.out.println("Initializing Application");

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		gconfig = ge.getDefaultScreenDevice().getDefaultConfiguration();

		ColorModel COLORMODEL = gconfig.getColorModel(Transparency.TRANSLUCENT);

		ac.createContext(COLORMODEL, COLORMODEL, null);
		ac_def.createContext(COLORMODEL, COLORMODEL, null);

		updateRenderResolution();

		HashMap<String, Size> assetSizes = new HashMap<>();

		for (final File fileEntry : new File("assets").listFiles()) {
			if (fileEntry.isFile()) {
				String extension = fileEntry.getName().substring(fileEntry.getName().indexOf(".") + 1);
				if (!extension.equalsIgnoreCase("png") && !extension.equalsIgnoreCase("jpg")) {
					continue;
				}
				String name = fileEntry.getName().substring(0, fileEntry.getName().indexOf("."));
				BufferedImage img = ImageImport.getImage(fileEntry.getPath());
				Size size = new Size(img.getWidth() / Globals.PIXELS_PER_GRID_IMPORT(),
						img.getHeight() / Globals.PIXELS_PER_GRID_IMPORT());
				assetSizes.put(name,
						size);
				if (selectasset.length() == 0)
					selectasset = name;
				BufferedImage resize = ImageImport.resize(img,
						(int) (img.getWidth() * Globals.PIXELS_RESIZE * 1.0f / Globals.PIXELS_PER_GRID_IMPORT()),
						(int) (img.getHeight() * Globals.PIXELS_RESIZE * 1.0f / Globals.PIXELS_PER_GRID_IMPORT()));
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

		debug_opts.add(new Triplet<String, Runnable, Callable<String>>("Show Assets Menu", () -> {
			SHOW_ASSETS_MENU = true;
			asset_menu_index = 0;
		}, () -> {
			return "";
		}));
		debug_opts.add(new Triplet<String, Runnable, Callable<String>>("Draw Mode [%s]", () -> {
			selection_type++;
			if (selection_type >= 3)
				selection_type = 0;
		}, () -> {
			return String.valueOf(selection_type);
		}));
		debug_opts.add(new Triplet<String, Runnable, Callable<String>>("Toggle Grid [%s]", () -> {
			SHOW_GRID = !SHOW_GRID;
		}, () -> {
			return String.valueOf(SHOW_GRID);
		}));
		debug_opts.add(new Triplet<String, Runnable, Callable<String>>("Toggle Light [%s]", () -> {
			LIGHT_MODE = !LIGHT_MODE;
		}, () -> {
			return String.valueOf(LIGHT_MODE);
		}));
		debug_opts.add(new Triplet<String, Runnable, Callable<String>>("Toggle Shadows [%s]", () -> {
			SHADOWS_MODE = !SHADOWS_MODE;
		}, () -> {
			return String.valueOf(SHADOWS_MODE);
		}));
		debug_opts.add(new Triplet<String, Runnable, Callable<String>>("Toggle Overlay [%s]", () -> {
			OVERLAY_MODE = !OVERLAY_MODE;
		}, () -> {
			return String.valueOf(OVERLAY_MODE);
		}));
		debug_opts.add(new Triplet<String, Runnable, Callable<String>>("Toggle Edit Mode [%s]", () -> {
			EDIT_MODE = !EDIT_MODE;
		}, () -> {
			return String.valueOf(EDIT_MODE);
		}));
		debug_opts.add(new Triplet<String, Runnable, Callable<String>>("Toggle Clip [%s]", () -> {
			CLIP_MODE = !CLIP_MODE;
		}, () -> {
			return String.valueOf(CLIP_MODE);
		}));
		debug_opts.add(new Triplet<String, Runnable, Callable<String>>("Wall Visibility [%s]", () -> {
			WALL_VISIBILITY = !WALL_VISIBILITY;
		}, () -> {
			return String.valueOf(WALL_VISIBILITY);
		}));
		debug_opts.add(new Triplet<String, Runnable, Callable<String>>("Save Level", () -> {
			LevelConfigUtil.saveLevel();
			levelUpdate();
		}, () -> {
			return "";
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

		enemy.step();

		// if (entry.tick>reassign_tick+5000) {
			
		// 	reassign_tick = entry.tick;
		// }
		

		/*
		 * if (animation) {
		 * double step_x = Globals.DASH_STEP * intent.getX();
		 * double step_y = Globals.DASH_STEP * intent.getY();
		 * if (entry.tick - dash_tick < Globals.DASH_DURATION) {
		 * 
		 * CollisionReturn collided = playerCollision(step_x, -step_y);
		 * if (collided.getX()_collision && collided.getY()_collision) {
		 * location.getX() += collided.disp_x;
		 * location.getY() -= collided.disp_y;
		 * dash_tick = 0;
		 * animation = false;
		 * } else if (collided.getX()_collision && !collided.getY()_collision) {
		 * location.getX() += collided.disp_x;
		 * location.getY() -= step_y;
		 * } else if (!collided.getX()_collision && collided.getY()_collision) {
		 * location.getX() += step_x;
		 * location.getY() -= collided.disp_y;
		 * }
		 * 
		 * } else {
		 * animation = false;
		 * }
		 * } else
		 */
		playerCollisionAndMovementCode();

		/*
		 * for (ResetBox b : resetboxes) {
		 * Rect r = SchemUtilities.schemToFrame(b, location, Globals.PIXELS_PER_GRID());
		 * boolean res = CollisionUtil.staticCollision(PLAYER_SCREEN_LOC, r);
		 * if (res) {
		 * deathscreen = true;
		 * animation_tick = entry.tick;
		 * setPlayerPosFromSchem(checkpoints.get(b.checkpoint));
		 * }
		 * }
		 */
	}

	void RawGame(Graphics2D g) {
		for (LevelTile o : tiles) {

			Rect r = SchemUtilities.schemToFrame(o, location, Globals.PIXELS_PER_GRID());
			if (inScreenSpace(r))
				paintLevelTile(g, (LevelTile) o);

		}

		for (LevelWall o : walls) {

			Rect r = SchemUtilities.schemToFrame(o, location, Globals.PIXELS_PER_GRID());
			if (inScreenSpace(r))
				paintLevelWall(g, (LevelWall) o);

		}

		g.setColor(Color.BLUE);
		g.setStroke(new BasicStroke(4));

		if (OVERLAY_MODE) {
			for (LevelTile o : newTiles) {

				Rect r = SchemUtilities.schemToFrame(o, location, Globals.PIXELS_PER_GRID());
				if (inScreenSpace(r))
					paintLevelTile(g, (LevelTile) o);
	
			}
			for (LevelWall o : newWalls) {
				Rect r = SchemUtilities.schemToFrame(o, location,
						Globals.PIXELS_PER_GRID());
				if (inScreenSpace(r))
					paintLevelWall(g, (LevelWall) o);
			}
			
			for (Collider o : colliders) {
				Line r = SchemUtilities.schemToFrame(o, location, Globals.PIXELS_PER_GRID());
				if (inScreenSpace(r))
					g.drawLine((int) r.getX1(), (int) r.getY1(), (int) r.getX2(), (int) r.getY2());
			}

			g.setColor(Color.ORANGE);
			for (Collider o : newColliders) {
				Line l = SchemUtilities.schemToFrame(o, location, Globals.PIXELS_PER_GRID());

				if (inScreenSpace(l))
					g.drawLine((int) l.getX1(), (int) l.getY1(), (int) l.getX2(), (int) l.getY2());
			}
		}

		g.setColor(Color.YELLOW);
		g.fillOval((int) PLAYER_SCREEN_LOC.left(), (int) PLAYER_SCREEN_LOC.top(), (int) PLAYER_SCREEN_LOC.getWidth(),
				(int) PLAYER_SCREEN_LOC.getHeight());

		g.setColor(Color.GREEN);
		for (Bullet b : bullets) {
			Rect r = SchemUtilities.schemToFrame(b, location, Globals.PIXELS_PER_GRID());
			if (inScreenSpace(r))
				g.drawRect((int) r.left(), (int) r.top(), (int) r.getWidth(), (int) r.getHeight());
		}
	}

	private Shape LightMask(Graphics2D dispG, Graphics g) {
		double dx = 360 * Math.cos(looking_angle);
		double dy = 360 * Math.sin(looking_angle);

		double small_radius = 40;
		Point center = PLAYER_SCREEN_LOC.center();

		Area areas[] = new Area[] { new Area(), new Area() };
		Triplet<Double, Double, Double> args[] = new Triplet[] {
				new Triplet<Double, Double, Double>(0.0, -1.0, 0.0),
				new Triplet<Double, Double, Double>(-6.0, 0.0, -10.0) };

		Area visibility = new Area();

		if (!LIGHT_MODE) {
			dispG.setBackground(Color.BLACK);
			dispG.clearRect(0, 0, this.getWidth(), this.getHeight());
		}
		if (!SHADOWS_MODE) {
			visibility.add(new Area(new Rectangle2D.Float(0, 0, this.getWidth(), this.getHeight())));
		}

		if (!LIGHT_MODE && !SHADOWS_MODE)
			return visibility;

		Polygon light = new Polygon();
		double angle_1 = looking_angle - Globals.FLASHLIGHT_FOV;
		double angle_2 = looking_angle + Globals.FLASHLIGHT_FOV;
		light.addPoint((int) (center.getX() + small_radius * Math.cos(angle_1)),
				(int) (center.getY() + small_radius * Math.sin(angle_1)));

		light.addPoint((int) (center.getX() + dx + Globals.FLASHLIGHT_RANGE * Math.cos(angle_1)),
				(int) (center.getY() + dy + Globals.FLASHLIGHT_RANGE * Math.sin(angle_1)));
		light.addPoint((int) (center.getX() + dx + Globals.FLASHLIGHT_RANGE * Math.cos(angle_2)),
				(int) (center.getY() + dy + Globals.FLASHLIGHT_RANGE * Math.sin(angle_2)));

		light.addPoint((int) (center.getX() + small_radius * Math.cos(angle_2)),
				(int) (center.getY() + small_radius * Math.sin(angle_2)));

		Ellipse2D e2d = new Ellipse2D.Double(center.getX() - Globals.INNER_RADIUS, center.getY() - Globals.INNER_RADIUS,
				Globals.INNER_RADIUS * 2, Globals.INNER_RADIUS * 2);

		if (LIGHT_MODE) {

			RadialGradientPaint rgp = new RadialGradientPaint(new Point2D.Double(center.getX(), center.getY()),
					(float) Globals.INNER_RADIUS,
					new float[] { 0.0f, 0.5f, 1.0f },
					new Color[] { Color.BLACK, new Color(0, 0, 0, 220), new Color(0, 0, 0, 0) });

			dispG.setPaint(rgp);
			dispG.fill(
					new Ellipse2D.Double(center.getX() - Globals.INNER_RADIUS, center.getY() - Globals.INNER_RADIUS,
							Globals.INNER_RADIUS * 2, Globals.INNER_RADIUS * 2));

			GradientPaint gp = new GradientPaint(new Point2D.Double(center.getX(), center.getY()),
			Color.BLACK,
			new Point2D.Double(center.getX() + Globals.FLASHLIGHT_RANGE *
			Math.cos(looking_angle),
			center.getY() + Globals.FLASHLIGHT_RANGE * Math.sin(looking_angle)),
			new Color(0, 0, 0, 0));
			dispG.setPaint(gp);
			dispG.fill(light);

			dispG.setPaint(null);

		}
		if (SHADOWS_MODE) {
			visibility.add(new Area(e2d));
			visibility.add(new Area(light));
		}

		if (SHADOWS_MODE) {

			for (LevelWall w : walls) {
				Rect rect = SchemUtilities.schemToFrame(w, location, Globals.PIXELS_PER_GRID());
				if (inScreenSpace(rect)) {
					Point PLAYER_CENTER = PLAYER_SCREEN_LOC.center();
					Point WALL_CENTER = rect.center();
					double overall_angle = (Math.atan2(PLAYER_CENTER.getY() - WALL_CENTER.getY(),
							WALL_CENTER.getX() - PLAYER_CENTER.getX()) + Math.PI * 2) % (Math.PI * 2);
					double angle_tl = (Math.atan2(PLAYER_CENTER.getY() - rect.left(),
							rect.left() - PLAYER_CENTER.getX()) + Math.PI * 2) % (Math.PI * 2);
					double angle_br = (Math.atan2(PLAYER_CENTER.getY() - rect.bottomLeft().getY(),
							rect.topRight().getX() - PLAYER_CENTER.getX()) + Math.PI * 2) % (Math.PI * 2);
					double angle_tr = (Math.atan2(PLAYER_CENTER.getY() - rect.top(),
							rect.topRight().getX() - PLAYER_CENTER.getX()) + Math.PI * 2) % (Math.PI * 2);
					double angle_bl = (Math.atan2(PLAYER_CENTER.getY() - rect.bottomRight().getY(),
							rect.left() - PLAYER_CENTER.getX()) + Math.PI * 2) % (Math.PI * 2);

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
							inner_first = new Point((int) (rect.right() - corner_buff),
									(int) (rect.bottom() - corner_buff));

							outer_first = PointOnScreenEdge(angle_br,
									new Point(rect.right() - end_buff,
											rect.bottom() - end_buff));

						} else if (angle_tr >= Math.PI / 2 && angle_tr < Math.PI) {
							inner_first = new Point((int) (rect.right() - corner_buff),
									(int) Math.ceil(rect.top() + corner_buff));

							outer_first = PointOnScreenEdge(angle_tr,
									new Point(rect.right() - end_buff, rect.top() + end_buff));

						} else if (angle_tl >= Math.PI && angle_tl < Math.PI * 3 / 2) {
							inner_first = new Point((int) Math.ceil(rect.left() + corner_buff),
									(int) Math.ceil(rect.top() + corner_buff));

							outer_first = PointOnScreenEdge(angle_tl,
									new Point(rect.left() + end_buff, rect.top() + end_buff));

						} else if (angle_bl >= Math.PI * 3 / 2 && angle_bl < Math.PI * 2) {
							inner_first = new Point((int) Math.ceil(rect.left() + corner_buff),
									(int) (rect.bottom() - corner_buff));

							outer_first = PointOnScreenEdge(angle_bl,
									new Point(rect.left() + end_buff, rect.bottom() - end_buff));

						}

						if (angle_tl >= 0 && angle_tl < Math.PI / 2) {
							outer_last = PointOnScreenEdge(angle_tl,
									new Point(rect.left() + end_buff, rect.top() + end_buff));

							inner_last = new Point((int) Math.ceil(rect.left() + corner_buff),
									(int) Math.ceil(rect.top() + corner_buff));

						} else if (angle_bl >= Math.PI / 2 && angle_bl < Math.PI) {
							outer_last = PointOnScreenEdge(angle_bl,
									new Point(rect.left() + end_buff, rect.bottom() - end_buff));

							inner_last = new Point((int) Math.ceil(rect.left() + corner_buff),
									(int) (rect.bottom() - corner_buff));

						} else if (angle_br >= Math.PI && angle_br < Math.PI * 3 / 2) {
							outer_last = PointOnScreenEdge(angle_br,
									new Point(rect.right() - end_buff,
											rect.bottom() - end_buff));

							inner_last = new Point((int) (rect.right() - corner_buff),
									(int) (rect.bottom() - corner_buff));

						} else if (angle_tr >= Math.PI * 3 / 2 && angle_tr < Math.PI * 2) {
							outer_last = PointOnScreenEdge(angle_tr,
									new Point(rect.right() - end_buff, rect.top() + end_buff));

							inner_last = new Point((int) (rect.right() - corner_buff),
									(int) Math.ceil(rect.top() + corner_buff));

						}

						if (inner_first != null) {
							o.addPoint((int) inner_first.getX(), (int) inner_first.getY());
							if (i == 0) {
								g.setColor(Color.WHITE);
								g.fillRect((int) inner_first.getX() - Globals.OVERLAY_MARKER_SIZE / 2,
										(int) inner_first.getY() - Globals.OVERLAY_MARKER_SIZE / 2,
										Globals.OVERLAY_MARKER_SIZE, Globals.OVERLAY_MARKER_SIZE);
							}
						}

						if (outer_first != null) {
							o.addPoint((int) outer_first.getValue1().getX(), (int) outer_first.getValue1().getY());
							if (i == 0) {
								g.setColor(Color.ORANGE);
								g.fillRect((int) outer_first.getValue1().getX() - Globals.OVERLAY_MARKER_SIZE / 2,
										(int) outer_first.getValue1().getY() - Globals.OVERLAY_MARKER_SIZE / 2,
										Globals.OVERLAY_MARKER_SIZE, Globals.OVERLAY_MARKER_SIZE);
							}
						}
						if (outer_first != null && outer_last != null) {
							if (!outer_first.getValue0().equals(outer_last.getValue0())) {
								if ((outer_first.getValue0().equals("top") && outer_last.getValue0().equals("left"))
										||
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
							o.addPoint((int) inner_last.getX(), (int) inner_last.getY());
							if (i == 0) {
								g.setColor(Color.GREEN);
								g.fillRect((int) inner_last.getX() - Globals.OVERLAY_MARKER_SIZE / 2,
										(int) inner_last.getY() - Globals.OVERLAY_MARKER_SIZE / 2,
										Globals.OVERLAY_MARKER_SIZE, Globals.OVERLAY_MARKER_SIZE);
							}
						}

						if (angle_bl > Math.PI / 2 && angle_bl < Math.PI && angle_br > 0
								&& angle_br < Math.PI / 2) {
							o.addPoint((int) (rect.left() + center_points_buff),
									(int) (rect.bottom() + center_points_buff));
							o.addPoint((int) (rect.right() - center_points_buff),
									(int) (rect.bottom() + center_points_buff));
						}
						if (angle_bl > Math.PI / 2 && angle_bl < Math.PI && angle_tr > Math.PI / 2
								&& angle_tr < Math.PI) {
							o.addPoint((int) (rect.right() + center_points_buff),
									(int) (rect.bottom() + center_points_buff));
						}
						if (angle_tr > Math.PI / 2 && angle_tr < Math.PI && angle_br > Math.PI
								&& angle_br < Math.PI * 3 / 2) {
							o.addPoint((int) (rect.right() + center_points_buff),
									(int) (rect.bottom() - center_points_buff));
							o.addPoint((int) (rect.right() + center_points_buff),
									(int) (rect.top() + center_points_buff));
						}
						if (angle_tr > Math.PI && angle_tr < Math.PI * 3 / 2 && angle_br > Math.PI
								&& angle_br < Math.PI * 3 / 2) {
							o.addPoint((int) (rect.right() + center_points_buff),
									(int) (rect.top() - center_points_buff));
						}
						if (angle_tl > Math.PI && angle_tl < Math.PI * 3 / 2 && angle_tr > Math.PI * 3 / 2
								&& angle_tr < Math.PI * 2) {
							o.addPoint((int) (rect.right() - center_points_buff),
									(int) (rect.top() - center_points_buff));
							o.addPoint((int) (rect.left() + center_points_buff),
									(int) (rect.top() - center_points_buff));
						}
						if (angle_tr > Math.PI * 3 / 2 && angle_tr < Math.PI * 2 && angle_bl > Math.PI * 3 / 2
								&& angle_bl < Math.PI * 2) {
							o.addPoint((int) (rect.left() - center_points_buff),
									(int) (rect.top() - center_points_buff));
						}
						if (angle_tl > 0 && angle_tl < Math.PI / 2 && angle_br > Math.PI * 3 / 2
								&& angle_br < Math.PI * 2) {
							o.addPoint((int) (rect.left() - center_points_buff),
									(int) (rect.top() + center_points_buff));
							o.addPoint((int) (rect.left() - center_points_buff),
									(int) (rect.bottom() - center_points_buff));
						}
						if (angle_tl > 0 && angle_tl < Math.PI / 2 && angle_br > 0 && angle_br < Math.PI / 2) {
							o.addPoint((int) (rect.left() - center_points_buff),
									(int) (rect.bottom() + center_points_buff));
						}
						areas[i].add(new Area(o));
					}

				}
			}

			visibility.subtract(new Area(areas[0]));
		}
		dispG.setPaint(null);

		for (LevelWall wall : walls) {
			if (inScreenSpace(wall) && wall.getAsset().equals("wood")) {
				Rect r = SchemUtilities.schemToFrame(wall, location, Globals.PIXELS_PER_GRID());
				Point obj = r.center();

				Ellipse2D e2d2 = new Ellipse2D.Double(obj.getX() - Globals.LAMP_RADIUS, obj.getY() - Globals.LAMP_RADIUS,
						Globals.LAMP_RADIUS * 2, Globals.LAMP_RADIUS * 2);

				if (SHADOWS_MODE)
					visibility.add(new Area(e2d2));

				if (LIGHT_MODE) {
					RadialGradientPaint rgp = new RadialGradientPaint(new Point2D.Double(obj.getX(), obj.getY()),
							(float) Globals.LAMP_RADIUS,
							new float[] { 0.0f, 0.5f, 1.0f },
							new Color[] { Color.BLACK, new Color(0, 0, 0, 220), new Color(0, 0, 0, 0) });

					dispG.setPaint(rgp);
					dispG.fill(new Ellipse2D.Double(obj.getX() - Globals.LAMP_RADIUS, obj.getY() - Globals.LAMP_RADIUS,
							Globals.LAMP_RADIUS * 2, Globals.LAMP_RADIUS * 2));
				}
			}
		}

		Shape vis = areas[1];

		if (SHADOWS_MODE) {
			for (LevelWall wall : walls) {
				Rect wallrect = SchemUtilities.schemToFrame(wall, location,
						Globals.PIXELS_PER_GRID());
				if (inScreenSpace(wallrect)) {
					Rectangle2D re = new Rectangle2D.Double((int) Math.round(wallrect.left()),
							(int) Math.round(wallrect.top()),
							wallrect.getWidth(),
							wallrect.getHeight());

					if (WALL_VISIBILITY) {// || !vis.contains(re)

						visibility.add(new Area((Shape) re));
					}
				}
			}
		}

		return visibility;
	}

	void drawUI(Graphics2D g) {
		for (ResetBox b : resetboxes) {
			Rect r = SchemUtilities.schemToFrame(b, location, Globals.PIXELS_PER_GRID());
			//if (inScreenSpace(r))
				g.drawRect((int) Math.floor(r.left()), (int) Math.floor(r.top()), (int) Math.floor(r.getWidth()),
						(int) Math.floor(r.getHeight()));
		}

		if (enemy.getPath() != null) {
			Path p = enemy.getPath();
			final int psize = 3;
			List<IntPoint> points = p.getPathPoints();
			for (IntPoint point : points) {
				Point p2 = SchemUtilities.schemToFrame(new Point(point.getX()+0.5, point.getY()+0.5), location,
						Globals.PIXELS_PER_GRID());
				g.drawOval((int) p2.getX() - psize, (int) p2.getY() - psize, psize * 2, psize * 2);
			}
			g.setStroke(new BasicStroke(4));
			for (int i = 0; i < points.size() - 1; i++) {
				IntPoint current = points.get(i);
				IntPoint next = points.get(i + 1);
				
				Point p1 = SchemUtilities.schemToFrame(new Point(current.getX()+0.5, current.getY()+0.5), location,
						Globals.PIXELS_PER_GRID());
				Point p2 = SchemUtilities.schemToFrame(new Point(next.getX()+0.5, next.getY()+0.5), location,
						Globals.PIXELS_PER_GRID());
				g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
			}
		}

		g.setColor(Color.RED);
		g.setStroke(new BasicStroke(4));
		if (EDIT_MODE) {
			Point newpoint1 = SchemUtilities.roundSchemFrame(entry.peripherals.mousePos(), location,
					Globals.PIXELS_PER_GRID());

			if (selection_type == 0) {
				if (select_point_store != null) {
					Point newpoint2 = SchemUtilities.schemToFrame(select_point_store, location,
							Globals.PIXELS_PER_GRID());

					g.drawLine((int) newpoint1.getX(), (int) newpoint1.getY(), (int) newpoint2.getX(),
							(int) newpoint2.getY());
				}
			} else {
				AppAsset a = assets.get(selectasset);
				if (a != null) {
					g.drawRect((int) newpoint1.getX(), (int) newpoint1.getY(),
							(int) (a.size.getWidth() * Globals.PIXELS_PER_GRID()),
							(int) (a.size.getHeight() * Globals.PIXELS_PER_GRID()));
				}
			}

		}

		if (EDIT_MODE)
			g.setColor(Color.ORANGE);
		else
			g.setColor(Color.WHITE);

		Point mouse = entry.peripherals.mousePos();
		Point schem_mouse = SchemUtilities.frameToSchem(mouse, location, Globals.PIXELS_PER_GRID());
		int z = 3;
		g.fillOval((int) mouse.getX() - z, (int) mouse.getY() - z, 2 * z, 2 * z);

		g.setFont(DEBUG_TEXT);
		String focus = selectasset;

		g.drawString(String.format(
				"raw=(%5.1f,%5.1f)  coord=(%5.1f,%5.1f) typing=%b focus=[%s] select_type=%d debug_val=[%s] tick=%d", mouse.getX(),
				mouse.getY(), schem_mouse.getX(), schem_mouse.getY(), typing, focus, selection_type, debug_vals.toString(), entry.tick), 20,
				g.getFontMetrics().getAscent() + 20);

		if (EDIT_MODE) {
			g.setStroke(new BasicStroke(6));
			g.setColor(Color.BLUE);
			g.drawRect((int) LEVEL_SCREEN_SPACE.left(), (int) LEVEL_SCREEN_SPACE.top(),
					(int) LEVEL_SCREEN_SPACE.getWidth(), (int) LEVEL_SCREEN_SPACE.getHeight());
		}

		if (typing) {
			g.setFont(AMMO_TEXT);
			g.setColor(Color.RED);
			g.drawString(typing_str, 40, 40);
		}

		g.setColor(Color.RED);
		g.fillRect(20, this.getHeight() - 60, 200, 10);

		g.setStroke(new BasicStroke(2));
		g.setColor(Color.BLACK);
		g.drawRect(20, this.getHeight() - 60, 200, 10);
		g.drawRect(20, this.getHeight() - 40, 200, 10);

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
			g.drawArc((int) mousepos.getX() - size1, (int) mousepos.getY() - size1, 2 * size1, 2 * size1, 0,
					(int) (360 * percent));
		}
	}

	void drawDebug(Graphics2D g) {
		g.setColor(Color.GREEN);
		g.setStroke(new BasicStroke(1));

		if (SHOW_GRID) {
			for (int x1 = (int) LEVEL_BOUND.left(); x1 < LEVEL_BOUND.right(); x1++) {
				for (int y1 = (int) LEVEL_BOUND.top(); y1 < LEVEL_BOUND.bottom(); y1++) {
					Point p = new Point(x1 * Globals.PIXELS_PER_GRID() - location.getX(),
							y1 * Globals.PIXELS_PER_GRID() - location.getY());

					Rect r1 = new Rect((int) p.getX(), (int) p.getY(), (int) (Globals.PIXELS_PER_GRID()),
							(int) (Globals.PIXELS_PER_GRID()));
					if (inScreenSpace(r1)) {
						g.drawLine((int) Math.floor(p.getX()), (int) Math.floor(p.getY()), (int) Math.floor(p.getX()),
								(int) Math.floor(p.getY() + Globals.PIXELS_PER_GRID()));

						g.drawLine((int) Math.floor(p.getX()), (int) Math.floor(p.getY()),
								(int) Math.floor(p.getX() + Globals.PIXELS_PER_GRID()),
								(int) Math.floor(p.getY()));
					}
				}
			}
		}

		if (layers != null) {
			PathFinding.displayPath(g, layers, location, (double)Globals.PIXELS_PER_GRID(), Enemy.check);
		}

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
			g.fillRect((int) bound.left(), (int) bound.top(), (int) bound.getWidth(), (int) bound.getHeight());

			g.setColor(Color.LIGHT_GRAY);
			g.fillRect((int) bound.left(),
					(int) (bound.top() + VERT_SPACING + (debug_selection) * (text_height + VERT_MID_SPACING)),
					(int) bound.getWidth(), (int) text_height + 8);

			g.setColor(Color.WHITE);

			for (int i = 0; i < debug_opts.size(); i++) {
				Triplet<String, Runnable, Callable<String>> triplet = debug_opts.get(i);

				String name = "null";
				try {
					String str = triplet.getValue2().call();
					if (str.length() > 0)
						name = String.format(triplet.getValue0(), triplet.getValue2().call());
					else
						name = triplet.getValue0();
				} catch (Exception e) {
					e.printStackTrace();
				}

				g.drawString(name, (int) (bound.left() + SIDE_SPACING),
						(int) (bound.top() + (i + 1) * text_height + (i) * VERT_MID_SPACING + VERT_SPACING));
			}
		}

		int c = 4;
		g.fillRect((int)PLAYER_SCREEN_LOC.left()-c, (int)PLAYER_SCREEN_LOC.top()-c, 2*c, 2*c);
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
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

		LEVEL_SCREEN_SPACE = new Rect(
				Math.max(0, LEVEL_BOUND.left() * Globals.PIXELS_PER_GRID() - location.getX()),
				Math.max(0, LEVEL_BOUND.top() * Globals.PIXELS_PER_GRID() - location.getY()),
				Math.min(this.getWidth(), LEVEL_BOUND.right() * Globals.PIXELS_PER_GRID()-location.getX()),
				Math.min(this.getHeight(), LEVEL_BOUND.bottom() * Globals.PIXELS_PER_GRID()-location.getY()));

		Graphics2D dispG = (Graphics2D) display.getGraphics();
		Graphics2D extraG = (Graphics2D) extra.getGraphics();

		dispG.setBackground(new Color(0, 0, 0, 0));
		dispG.clearRect(0, 0, display.getWidth(), display.getHeight());
		extraG.setBackground(new Color(0, 0, 0, 0));
		extraG.clearRect(0, 0, extra.getWidth(), extra.getHeight());
		g.setBackground(Color.BLACK);
		g.clearRect(0, 0, this.getWidth(), this.getHeight());

		if (LIGHT_MODE || SHADOWS_MODE) {
			dispG.setComposite(ac_def);

			Shape mask = LightMask(dispG, extraG);
			dispG.setClip(mask);

			/*
			 * dispG.setColor(new Color(0, 0, 0, 25));
			 * dispG.fillRect(0, 0, this.getWidth(), this.getHeight());
			 */

			dispG.setComposite(ac);

			RawGame(dispG);

			dispG.setComposite(ac_def);
			dispG.setClip(null);

		} else {
			RawGame(dispG);
		}

		drawUI(dispG);
		drawDebug(dispG);

		if (OVERLAY_MODE)
			dispG.drawImage(extra, 0, 0, null);

		g.drawImage(display, 0, 0, this.getWidth(), this.getHeight(), null);

		g.setColor(Color.GREEN);
		g.setStroke(new BasicStroke(4));
		g.drawRect(0, 0, this.getWidth(), this.getHeight());


		int z = 5;
		g.setColor(Color.RED);
		Point enemyP = SchemUtilities.schemToFrame(new Point(enemy.getPos().getX(), enemy.getPos().getY()), location, Globals.PIXELS_PER_GRID());
		g.fillOval((int)enemyP.getX()-z, (int)enemyP.getY()-z, z*2, z*2);

		g.setColor(Color.YELLOW);
		Point pnew = SchemUtilities.schemToFrame(new Point(enemy.getPos().getX()+0.5, enemy.getPos().getY()+0.5), location, Globals.PIXELS_PER_GRID());
		g.fillOval((int) (pnew.getX()-enemy.getSize()/2), (int) (pnew.getY()-enemy.getSize()/2), (int) enemy.getSize(), (int) enemy.getSize());

	}

	/*
	 * RESIZE EVENT
	 */
	public void updateRenderResolution() {
		int width = this.getWidth();
		int height = this.getHeight();

		Point topleft = new Point((width - Globals.PLAYER_SIZE) / 2,
		(height - Globals.PLAYER_SIZE) / 2);

		Point bottomright = new Point((width + Globals.PLAYER_SIZE) / 2,
		(height + Globals.PLAYER_SIZE) / 2);

		PLAYER_SCREEN_LOC = new Rect(topleft, bottomright);

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
		double x0 = start_point.getX();
		double y0 = start_point.getY();

		double slope = Math.tan(angle);
		int dx = (int) Math.copySign(1, Math.cos(angle));
		int dy = (int) Math.copySign(1, Math.sin(angle));

		double xf_0 = (x0 + (y0) / slope);
		double xf_H = (x0 + (y0 - this.getHeight()) / slope);
		double yf_0 = (y0 + (x0) * slope);
		double yf_W = (y0 + (x0 - this.getWidth()) * slope);

		if (xf_0 > 0 && xf_0 < this.getWidth() && dy == 1) {
			return new Pair<String, Point>("top", new Point((int) xf_0, 0));
		} else if (xf_H > 0 && xf_H < this.getWidth() && dy == -1) {
			return new Pair<String, Point>("bottom", new Point((int) xf_H, this.getHeight()));
		}
		if (yf_0 > 0 && yf_0 < this.getHeight() && dx == -1) {
			return new Pair<String, Point>("left", new Point(0, (int) yf_0));
		} else if (yf_W > 0 && yf_W < this.getHeight() && dx == 1) {
			return new Pair<String, Point>("right", new Point(this.getWidth(), (int) yf_W));
		}
		return new Pair<String, Point>("none", new Point(0, 0));
	}

	/*
	 * MOUSE CLICK EVENT
	 */
	Point select_point_store = null;

	public void mouseClick(Point pos) {
		Point select_point_1 = SchemUtilities.frameToSchem(pos, location, Globals.PIXELS_PER_GRID());

		if (EDIT_MODE) {
			if (selection_type == 0) {
				if (select_point_store == null)
					select_point_store = SchemUtilities.frameToSchem(pos, location,
							Globals.PIXELS_PER_GRID());
				else {
					Collider c = new Collider(select_point_store, select_point_1);
					newColliders.add(c);
					select_point_store = null;
				}

			} else if (selection_type == 1) {
				AppAsset a = assets.get(selectasset);

				Rect r = new Rect(select_point_1.getX(), select_point_1.getY(), a.size.getWidth(),
						a.size.getHeight());

				LevelWall c = new LevelWall(r.left(), r.top(), r.getWidth(), r.getHeight(), 0.0f, selectasset);
				newWalls.add(c);
			} else if (selection_type == 2) {
				AppAsset a = assets.get(selectasset);

				Rect r = new Rect(select_point_1.getX(), select_point_1.getY(), a.size.getWidth(),
						a.size.getHeight());

				LevelTile c = new LevelTile(r.left(), r.top(), r.getWidth(), r.getHeight(), 0.0f, selectasset);
				newTiles.add(c);
			} else if (selection_type == 0) {
				AppAsset a = assets.get(selectasset);

				Rect r = new Rect(select_point_1.getX(), select_point_1.getY(), a.size.getWidth(),
						a.size.getHeight());

				Collider c = new Collider(r.left(), r.top(), r.getWidth(), r.getHeight());
				newColliders.add(c);
			}

			levelUpdate();
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

		Point arm = new Point(PLAYER_SCREEN_LOC.left() + location.getX() + Globals.PLAYER_SIZE / 2,
				PLAYER_SCREEN_LOC.top() + location.getY() + Globals.PLAYER_SIZE / 2);
		Point start = new Point(arm.getX() + Globals.BULLET_DEFAULT_DISTANCE * Math.cos(looking_angle),
				arm.getY() + Globals.BULLET_DEFAULT_DISTANCE * Math.sin(looking_angle));

		Point end = new Point(start.getX() / Globals.PIXELS_PER_GRID(),
				start.getY()/ Globals.PIXELS_PER_GRID());
		Bullet b = new Bullet(end.getX(), end.getY(), g.BULLET_SIZE()*Globals.BULLET_SIZE_MULT, looking_angle,
				g.mag.BULLET_INITIAL_SPEED() * Globals.BULLET_SPEED_MULT);
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
				else
					debug_val_selection++;
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
					(PLAYER_SCREEN_LOC.center().getY())-entry.peripherals.mousePos().getY(),
					(PLAYER_SCREEN_LOC.center().getX())-entry.peripherals.mousePos().getX())) % (2 * Math.PI)
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

			if (entry.peripherals.KeyToggled(KeyEvent.VK_U)) {
				((Gun) weapon).mag = ItemAttributes.DevTek_Mag();
			}

			if (entry.peripherals.KeyToggled(KeyEvent.VK_O)) {
				IntPoint end = playerSchemRoundedPos();
				IntPoint start = enemy.getIntPos();
				enemy.updatePath(start, end, location, Globals.PIXELS_PER_GRID());
				//enemy.updatePath(layers, start, end);
			}

			if (entry.peripherals.KeyToggled(KeyEvent.VK_P)) {
				IntPoint pos = enemy.getIntPos();
				layers = PathFinding.PathFindDebug(new PathNode(pos.getX(), pos.getY(), null), 10,
				Enemy.check);
			}

			
		}
	}

	/*
	 * PLAYER COLLISION AND MOVEMENT CODE
	 */
	public void playerCollisionAndMovementCode() {
		double component_x = 0;
		double component_y = 0;

		velocity = velocity.addVector(intent.multiply(0.14));

		if (velocity.getMagnitude() > Globals.PLAYER_MAX_SPEED)
			velocity.setMagnitude(Globals.PLAYER_MAX_SPEED);
		if (intent.equals(Vector.zero())) {
			if (velocity.getMagnitude() < Globals.PLAYER_MIN_SPEED_CUTOFF)
				velocity = Vector.zero();
			else
				velocity = velocity.addVector(velocity.multiply(-0.08));
		}

		component_x = velocity.getX();
		component_y = velocity.getY();

		if (!CLIP_MODE) {
			{
				CollisionReturn ret = objectCollision(PLAYER_SCREEN_LOC, component_x, component_y, true);
				if (ret != null) {
					if (ret.y_collision) {
						component_y = 0;
						location.setY(location.getY()+ Math.copySign(1, velocity.getY()) * -ret.disp_y);
					}
					if (ret.x_collision) {
						component_x = 0;
						location.setX(location.getX()+ Math.copySign(1, velocity.getX()) * ret.disp_x);
					}
				}
			}

			for (Collider c : colliders) {
				Line collider = SchemUtilities.schemToFrame(c, location, Globals.PIXELS_PER_GRID());

				for (int i = 0; i < bullets.size(); i++) {
					Bullet b = bullets.get(i);
					Rect bullet = new Rect(b.left(), b.top(), b.getWidth(), b.getHeight());
					double dx = b.speed * Math.cos(b.angle);
					double dy = b.speed * Math.sin(b.angle);

					double speed_x = dx;
					double speed_y = dy;
					double new_angle = Math.atan2(speed_y, speed_x);
					double new_speed = Math.sqrt(Math.pow(speed_x, 2) + Math.pow(speed_y, 2));
					b.speed = new_speed;
					b.angle = new_angle;

					Rect br = SchemUtilities.schemToFrame(bullet, location, Globals.PIXELS_PER_GRID());
					//boolean ret = CollisionUtil.staticCollision(br, collider);

					// if (ret || Math.sqrt(Math.pow(bullet.getX(), 2) + Math.pow(bullet.getY(), 2)) > 4000) {
					// 	bullets.remove(i);
					// 	i--;
					// } else {
					// 	b.setX(b.getX() + dx);
					// 	b.setY(b.getY() + dy);
					// }
				}

			}
		}

		location.setX(location.getX()+component_x);
		location.setY(location.getY()- component_y);
	}

	/*
	 * CHECKS PLAYER COLLISION WITH ALL OBJECTS IN THE SCENE
	 */
	public CollisionReturn objectCollision(Rect a, double dx, double dy, boolean transform_coord) {
		CollisionReturn colret = null;
		List<Line> objects = new ArrayList<>();
		objects.addAll((List<Line>) colliders.stream()
				.map(c -> (Line) c)
				.collect(Collectors.toList()));
		objects.addAll((List<Line>) newColliders.stream()
				.map(c -> (Line) c)
				.collect(Collectors.toList()));

		for (Line c : objects) {
			Line r = c;
			if (transform_coord)
				r = SchemUtilities.schemToFrame(c, location, Globals.PIXELS_PER_GRID());
			if (!inScreenSpace(r))
				continue;
			// //CollisionReturn ret = CollisionUtil.DynamicCollision(a, r, dx, dy);

			// if (ret.x_collision || ret.y_collision) {
			// 	if (colret == null)
			// 		colret = ret;
			// 	else {
			// 		if (ret.x_collision) {
			// 			colret.x_collision = true;
			// 			if (Math.abs(ret.disp_x) > Math.abs(colret.disp_x))
			// 				colret.disp_x = ret.disp_x;
			// 		}
			// 		if (ret.y_collision) {
			// 			colret.y_collision = true;
			// 			if (Math.abs(ret.disp_y) > Math.abs(colret.disp_y))
			// 				colret.disp_y = ret.disp_y;
			// 		}
			// 	}
			// }

		}
		return colret;
	}

	/*
	 * UPDATE SCHEMATIC LEVEL BOUNDS BASED ON GAME OBJECTS
	 */
	void levelUpdate() {
		LEVEL_BOUND = new Rect(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);

		List<Rect> all = new ArrayList<>();
		all.addAll(walls.stream().map(c -> (Rect) c).collect(Collectors.toList()));
		all.addAll(colliders.stream().map(c -> new Rect(c.getP1(), c.getP2())).collect(Collectors.toList()));
		all.addAll(newColliders.stream().map(c -> new Rect(c.getP1(), c.getP2())).collect(Collectors.toList()));
		all.addAll(newWalls.stream().map(c -> (Rect) c).collect(Collectors.toList()));
		all.addAll(tiles.stream().map(c -> (Rect) c).collect(Collectors.toList()));
		all.addAll(newTiles.stream().map(c -> (Rect) c).collect(Collectors.toList()));

		for (Rect o : all) {
			if (o.left() < LEVEL_BOUND.left())
				LEVEL_BOUND.setP1(new Point(o.left(), LEVEL_BOUND.top()));
			if (o.right() > LEVEL_BOUND.right())
				LEVEL_BOUND.setP2(new Point(o.right(), LEVEL_BOUND.bottom()));
			if (o.top() < LEVEL_BOUND.top())
				LEVEL_BOUND.setP1(new Point(LEVEL_BOUND.left(), o.top()));
			if (o.bottom() > LEVEL_BOUND.bottom())
				LEVEL_BOUND.setP2(new Point(LEVEL_BOUND.right(), o.bottom()));
		}
	}

	/*
	 * Move player given a SCHEMATIC coordinate
	 */
	void setPlayerPosFromSchem(Point p) {
		location = new Point(p.getX() * Globals.PIXELS_PER_GRID() - PLAYER_SCREEN_LOC.left(),
				p.getY() * Globals.PIXELS_PER_GRID() - PLAYER_SCREEN_LOC.top() - PLAYER_SCREEN_LOC.getHeight() / 2
						- 1.5 * Globals.PIXELS_PER_GRID());
	}

	/*
	 * CHECK IF RECTANGLE IS INSIDE SCREEN SPACE
	 */
	boolean inScreenSpace(Rect r) {
		return CollisionUtil.RectRectIntersection(LEVEL_SCREEN_SPACE, r);
	}

	boolean inScreenSpace(Line l) {
		return inScreenSpace(l.getP1()) && inScreenSpace(l.getP2());
	}

	boolean inScreenSpace(Point p) {
		Rect screen = new Rect(0, 0, this.getWidth(), this.getHeight());
		return p.getX() > screen.left() && p.getX() < screen.right()
				&& p.getY() > screen.top() && p.getY() < screen.bottom();
	}

	IntPoint playerSchemRoundedPos() {
		return SchemUtilities.frameToSchemInt(new Point((PLAYER_SCREEN_LOC.left()),(PLAYER_SCREEN_LOC.top())), location, Globals.PIXELS_PER_GRID());
	}

	void paintColorRect(Graphics g, ColorRect rect, double depth) {
		Rect r = SchemUtilities.schemToFrame(rect, location, Globals.PIXELS_PER_GRID());
		float c = 0.05f;
		if (!entry.app.colors.containsKey(rect.getColor())) {
			g.setColor(Color.RED);
			g.fillRect((int) Math.floor(r.left() - c), (int) Math.floor(r.top() - c),
					(int) Math.ceil(r.getWidth() + c), (int) Math.ceil(r.getHeight() + c));
		} else {
			g.setColor(entry.app.colors.get(rect.getColor()));
			g.fillRect((int) Math.floor(r.left() - c), (int) Math.floor(r.top() - c),
					(int) Math.ceil(r.getWidth() + c), (int) Math.ceil(r.getHeight() + c));
		}
	}

	void paintLevelWall(Graphics g, LevelWall p) {
		Rect r = SchemUtilities.schemToFrame(p, location, Globals.PIXELS_PER_GRID());
		if (!entry.app.assets.containsKey(p.getAsset())) {
			g.setColor(Color.RED);
			g.fillRect((int) Math.round(r.left()), (int) Math.round(r.top()), (int) r.getWidth(), (int) r.getHeight());
		} else {
			BufferedImage img = entry.app.assets.get(p.getAsset()).source;
			g.drawImage(img, (int) Math.round(r.left()), (int) Math.round(r.top()), (int) r.getWidth(),
					(int) r.getHeight(), null);
		}
	}

	void paintLevelTile(Graphics g, LevelTile p) {
		Rect r = SchemUtilities.schemToFrame(p, location, Globals.PIXELS_PER_GRID());
		if (!entry.app.assets.containsKey(p.getAsset())) {
			g.setColor(Color.RED);
			g.fillRect((int) Math.round(r.left()), (int) Math.round(r.top()), (int) r.getWidth(), (int) r.getHeight());
		} else {
			BufferedImage img = entry.app.assets.get(p.getAsset()).source;
			g.drawImage(img, (int) Math.round(r.left()), (int) Math.round(r.top()), (int) r.getWidth(),
					(int) r.getHeight(), null);
		}
	}

	BufferedImage resizeToGrid(BufferedImage img, double width, double height) {
		return ImageImport.resize(img, (int) (width / Globals.PIXELS_PER_GRID() * Globals.PIXELS_PER_GRID()),
				(int) (height / Globals.PIXELS_PER_GRID() * Globals.PIXELS_PER_GRID()));
	}

}