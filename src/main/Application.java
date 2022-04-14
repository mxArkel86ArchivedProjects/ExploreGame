package main;

import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.awt.image.ColorModel;
import java.io.File;

import util.Rect;
import util.SchemUtilities;
import util.ScreenAnimation;
import util.Size;
import util.Vector;
import util.CollisionReturn;
import util.CollisionUtil;
import util.LevelConfigUtil;
import util.MathUtil;
import util.DrawUtil;
import util.ImageImport;
import util.Point;
import java.awt.geom.*;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.JPanel;

import java.awt.*;

import gameObjects.Bullet;
import gameObjects.Collider;
import gameObjects.ColorRect;
import gameObjects.GameObject;
import gameObjects.LevelTile;
import gameObjects.LevelWall;
import gameObjects.ResetBox;
import inventory.Gun;
import inventory.ItemAttributes;
import inventory.Weapon;

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
	public HashMap<String, BufferedImage> assets = new HashMap<>();
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
	String selectasset = "grass";
	// String selectcolor = "black";
	Point select_point_1 = new Point(0, 0);
	boolean select_preview = true;

	/*
	 * MISC
	 */
	boolean deathscreen = false;
	boolean selectstage = false;
	boolean typing = false;
	String typing_str = "";

	/*
	 * ANIMATION VARIABLES
	 */
	boolean animation = false;
	long animation_tick = 0;
	long animation_time = 3000;
	long sprint_tick = 0;
	long dash_tick = 0;
	long walk_tick = 0;
	long dash_delay_tick = 0;
	int player_anim = 0;

	/*
	* GAME TOGGLES
	*/
	int debug_selection = 0;
	double GRIDSIZE = Globals.GRIDSIZE;
	boolean SHOW_GRID = true;
	boolean LIGHT_MODE = true;
	boolean EDIT_MODE = false;

	GraphicsConfiguration gconfig = null;

	List<Double> debug_vals = Arrays.asList(0.0, 0.0, 0.0);

	HashMap<String, Runnable> debug_opts = new HashMap<String, Runnable>();

	int select_val = 0;

	/*
	 * INIT METHOD
	 */
	public void Init(int width, int height) {

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		gconfig = ge.getDefaultScreenDevice().getDefaultConfiguration();

		ColorModel COLORMODEL = gconfig.getColorModel();

		ac.createContext(COLORMODEL, COLORMODEL, null);
		ac_def.createContext(COLORMODEL, COLORMODEL, null);

		onResize(width, height);

		HashMap<String, Size> assetSizes = new HashMap<>();

		for (final File fileEntry : new File("assets").listFiles()) {
			if (fileEntry.isFile()) {
				if (!fileEntry.getName().substring(fileEntry.getName().indexOf(".") + 1).equalsIgnoreCase("png")) {
					continue;
				}
				String name = fileEntry.getName().substring(0, fileEntry.getName().indexOf("."));
				BufferedImage img = ImageImport.getImage(fileEntry.getPath());

				assetSizes.put(name,
						new Size(img.getWidth() / Globals.PIXELS_PER_GRID, img.getHeight() / Globals.PIXELS_PER_GRID));
				assets.put(name, img);
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

		debug_opts.put("Toggle Grid", () -> {
			SHOW_GRID = !SHOW_GRID;
		});
		debug_opts.put("Toggle Light", () -> {
			LIGHT_MODE = !LIGHT_MODE;
		});
		debug_opts.put("Toggle Edit Mode", () -> {
			EDIT_MODE = !EDIT_MODE;
		});
	}

	/*
	 * BACKEND GAME CODE
	 */
	public void onTick() {
		inputUpdate(true, typing, (!animation && !typing));
		if (entry.peripherals.mouse_state)
			mouseDown(entry.peripherals.mousePos());

		deathscreen = ScreenAnimation.DeathScreen_Enabled(animation_tick);

		if (deathscreen)
			return;

		if (typing) {
			if (entry.peripherals.keysTypedB()) {
				typing_str += entry.peripherals.keysTyped();
			}
			return;
		}

		if (animation) {
			double step_x = Globals.DASH_STEP * intent.getX();
			double step_y = Globals.DASH_STEP * intent.getY();
			if (entry.tick - dash_tick < Globals.DASH_DURATION) {

				CollisionReturn collided = playerCollision(step_x, -step_y);
				if (collided.x_collision && collided.y_collision) {
					location.x += collided.disp_x;
					location.y -= collided.disp_y;
					dash_tick = 0;
					animation = false;
				} else if (collided.x_collision && !collided.y_collision) {
					location.x += collided.disp_x;
					location.y -= step_y;
				} else if (!collided.x_collision && collided.y_collision) {
					location.x += step_x;
					location.y -= collided.disp_y;
				}

			} else {
				animation = false;
			}
		} else
			playerCollisionAndMovementCode();

		for (ResetBox b : resetboxes) {
			Rect r = SchemUtilities.schemToLocal(b, location, GRIDSIZE);
			boolean res = CollisionUtil.staticCollision(PLAYER_SCREEN_LOC, r);
			if (res) {
				deathscreen = true;
				animation_tick = entry.tick;
				setPlayerPosFromSchem(checkpoints.get(b.checkpoint));
			}
		}
	}

	void RawGame(Graphics2D g) {

		g.setColor(Color.GREEN);
		g.setStroke(new BasicStroke(1));

		if (SHOW_GRID) {
			for (int x1 = (int) TOPLEFT_BOUND.x; x1 < BOTTOMRIGHT_BOUND.x; x1++) {
				for (int y1 = (int) TOPLEFT_BOUND.y; y1 < BOTTOMRIGHT_BOUND.y; y1++) {
					Point p = new Point(x1 * GRIDSIZE - location.x, y1 * GRIDSIZE - location.y);

					Rect r1 = new Rect((int) p.x, (int) p.y, (int) (GRIDSIZE), (int) (GRIDSIZE));
					if (inScreenSpace(r1)) {
						g.drawLine((int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.x),
								(int) Math.floor(p.y + GRIDSIZE));

						g.drawLine((int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.x + GRIDSIZE),
								(int) Math.floor(p.y));
					}
				}
			}
		}

		for (LevelTile o : tiles) {
			//if (Math.ceil(o.getZ()) <= 0) {
			Rect r = SchemUtilities.schemToLocalZ(o, PLAYER_SCREEN_LOC, location, o.getZ(), GRIDSIZE);
			if (inScreenSpace(r))
				paintLevelTile(g, (LevelTile) o);
			//}
		}

		for (LevelWall o : walls) {
			//if (Math.ceil(o.getZ()) <= 0) {
			Rect r = SchemUtilities.schemToLocalZ(o, PLAYER_SCREEN_LOC, location, o.getZ(), GRIDSIZE);
			if (inScreenSpace(r))
				paintLevelWall(g, (LevelWall) o);
			//}
		}

		for (LevelWall o : newWalls) {
			Rect r = SchemUtilities.schemToLocalZ(o, PLAYER_SCREEN_LOC, location, o.getZ(), GRIDSIZE);
			if (inScreenSpace(r))
				paintLevelWall(g, (LevelWall) o);
		}

		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(4));

		for (Collider o : colliders) {
			Rect r = SchemUtilities.schemToLocal(o, location, GRIDSIZE);
			if (inScreenSpace(r))
				g.drawRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
		}

		g.setColor(Color.ORANGE);
		for (Collider o : newColliders) {
			Rect r = SchemUtilities.schemToLocal(o, location, GRIDSIZE);

			if (inScreenSpace(r))
				g.drawRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
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

	private Shape LightMask(Graphics g) {
		double fov = 0.6;
		double dx = 360 * Math.cos(looking_angle);
		double dy = 360 * Math.sin(looking_angle);
		double STEP = Math.PI / 24;
		double dist = 400;
		//double r = 200;
		double radius = 200;
		double lamp_dist = 140;

		Area shape = new Area();
		Polygon circle = new Polygon();

		{
			Point center = new Point(PLAYER_SCREEN_LOC.x + PLAYER_SCREEN_LOC.width / 2,
					PLAYER_SCREEN_LOC.y + PLAYER_SCREEN_LOC.height / 2);
			double angle_1 = looking_angle - fov;
			double angle_2 = looking_angle + fov;
			circle.addPoint((int) (center.x + radius * Math.cos(angle_1)),
					(int) (center.y + radius * Math.sin(angle_1)));

			circle.addPoint((int) (center.x + dx + dist * Math.cos(angle_1)),
					(int) (center.y + dy + dist * Math.sin(angle_1)));
			circle.addPoint((int) (center.x + dx + dist * Math.cos(angle_2)),
					(int) (center.y + dy + dist * Math.sin(angle_2)));

			circle.addPoint((int) (center.x + radius * Math.cos(angle_2)),
					(int) (center.y + radius * Math.sin(angle_2)));

			for (double a = angle_2; a + STEP < angle_1 + 2 * Math.PI; a += STEP) {
				circle.addPoint((int) (center.x + radius * Math.cos(a)),
						(int) (center.y + radius * Math.sin(a)));
			}
			circle.addPoint((int) (center.x + radius * Math.cos(angle_1)),
					(int) (center.y + radius * Math.sin(angle_1)));
		}
		shape.add(new Area(circle));
		{
			for (LevelWall o : walls) {
				LevelWall p = (LevelWall) o;
				if (p.getAsset().equals("lamp")) {
					Rect rect = SchemUtilities.schemToLocalZ(o, PLAYER_SCREEN_LOC, location, o.getZ(), GRIDSIZE);
					if (inScreenSpace(rect.extend(Math.exp(7)))) {
						Shape ellipse = new Ellipse2D.Float((int) (rect.getX() - lamp_dist),
								(int) (rect.getY() - lamp_dist),
								(int) (rect.getWidth() + 2 * lamp_dist), (int) (rect.getWidth() + 2 * lamp_dist));
						shape.add(new Area(ellipse));
					}

				}

			}
		}
		// List<LevelWall> tempwall = new ArrayList<LevelWall>(walls.size());
		// for (LevelWall w2 : walls) {
		// 	tempwall.add(w2);
		// }
		// List<List<LevelWall>> wall_groups = new ArrayList<List<LevelWall>>();

		// for (int i = 0; i < tempwall.size(); i++) {
		// 	LevelWall lw = tempwall.get(i);
		// 	List<LevelWall> wallgroup = new ArrayList<LevelWall>();
			
		// 	boolean lock_y = false;
		// 	boolean lock_x = false;
		// 	for (int d = 1; d < 5; d++) {
		// 		for (int j = 0; j < tempwall.size(); j++) {
		// 			LevelWall lw2 = tempwall.get(j);

		// 			if ((lw2.x == lw.x || lock_x) && !lock_y) {
		// 				if (lw2.y == lw.y + d) {
		// 					wallgroup.add(lw2);
		// 					tempwall.remove(j);
		// 					j--;
		// 					lock_x = true;
		// 				} else if (lw2.y == lw.y - d) {
		// 					wallgroup.add(lw2);
		// 					tempwall.remove(j);
		// 					j--;
		// 					lock_x = true;
		// 				}

		// 			}
		// 			if ((lw2.y == lw.y || lock_y) && !lock_x) {
		// 				if (lw2.x == lw.x + 1) {
		// 					wallgroup.add(lw2);
		// 					tempwall.remove(j);
		// 					j--;
		// 					lock_y = true;
		// 				} else if (lw2.x == lw.x - 1) {
		// 					wallgroup.add(lw2);
		// 					tempwall.remove(j);
		// 					j--;
		// 					lock_y = true;
		// 				}

		// 			}
		// 		}

		// 	}
		// 	wallgroup.add(lw);
		// 	wall_groups.add(wallgroup);
		// }
		// for (List<LevelWall> wallgroup : wall_groups) {
		// 	double l = SchemUtilities.schemToLocalZ(wallgroup.get(0), PLAYER_SCREEN_LOC, location, 0, GRIDSIZE).x;
		// 	double r = SchemUtilities.schemToLocalZ(wallgroup.get(0), PLAYER_SCREEN_LOC, location, 0, GRIDSIZE).x;
		// 	double t = SchemUtilities.schemToLocalZ(wallgroup.get(0), PLAYER_SCREEN_LOC, location, 0, GRIDSIZE).y;
		// 	double b = SchemUtilities.schemToLocalZ(wallgroup.get(0), PLAYER_SCREEN_LOC, location, 0, GRIDSIZE).y;
		// 	for (LevelWall w : wallgroup) {
		// 		Rect r2 = SchemUtilities.schemToLocalZ(w, PLAYER_SCREEN_LOC, location, 0, GRIDSIZE);
		// 		if (r2.getX() < l) {
		// 			l = r2.getX();
		// 		}
		// 		if (r2.getX() + r2.getWidth() > r) {
		// 			r = r2.getX() + r2.getWidth();
		// 		}
		// 		if (r2.getY() < t) {
		// 			t = r2.getY();
		// 		}
				
		// 	}

		// 	Rect rect = Rect.fromPoints(l, t, r, b);
		for (LevelWall w : walls) {
			Rect rect = SchemUtilities.schemToLocalZ(w, PLAYER_SCREEN_LOC, location, 0, GRIDSIZE);
			if (inScreenSpace(rect)) {
				Shape s = new Rectangle2D.Float((int) rect.getX(), (int) rect.getY(), (int) rect.getWidth(),
						(int) rect.getHeight());
				Polygon o = new Polygon();

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

				debug_vals.set(0, overall_angle);

				//start points
				if (angle_br >= 0 && angle_br < Math.PI / 2) {
					o.addPoint((int) (rect.getX() + rect.getWidth()), (int) (rect.getY() + rect.getHeight()));

					o.addPoint((int) (rect.getX() + rect.getWidth() + d2 * Math.cos(angle_br)),
							(int) (rect.getY() + rect.getHeight() - d2 * Math.sin(angle_br)));

					g.setColor(Color.RED);
					g.fillOval((int) (rect.getX() + rect.getWidth()), (int) (rect.getY() + rect.getHeight()), 5, 5);

				}
				if (angle_tr >= Math.PI / 2 && angle_tr < Math.PI) {
					o.addPoint((int) (rect.getX() + rect.getWidth()), (int) (rect.getY()));

					o.addPoint((int) (rect.getX() + rect.getWidth() + d2 * Math.cos(angle_tr)),
							(int) (rect.getY() - d2 * Math.sin(angle_tr)));

					g.setColor(Color.YELLOW);
					g.fillOval((int) (rect.getX() + rect.getWidth()), (int) (rect.getY()), 5, 5);

				}
				if (angle_tl >= Math.PI && angle_tl < Math.PI * 3 / 2) {
					o.addPoint((int) (rect.getX()), (int) (rect.getY()));

					o.addPoint((int) (rect.getX() + d2 * Math.cos(angle_tl)),
							(int) (rect.getY() - d2 * Math.sin(angle_tl)));

					g.setColor(Color.GREEN);
					g.fillOval((int) (rect.getX()), (int) (rect.getY()), 5, 5);
				}
				if (angle_bl >= Math.PI * 3 / 2 && angle_bl < Math.PI * 2) {
					o.addPoint((int) (rect.getX()), (int) (rect.getY() + rect.getHeight()));

					o.addPoint((int) (rect.getX() + d2 * Math.cos(angle_bl)),
							(int) (rect.getY() + rect.getHeight() - d2 * Math.sin(angle_bl)));

					g.setColor(Color.BLUE);
					g.fillOval((int) (rect.getX()), (int) (rect.getY() + rect.getHeight()), 5, 5);
				}

				//end points
				if (angle_tl >= 0 && angle_tl < Math.PI / 2) {
					o.addPoint((int) (rect.getX() + d2 * Math.cos(angle_tl)),
							(int) (rect.getY() - d2 * Math.sin(angle_tl)));

					o.addPoint((int) (rect.getX()), (int) (rect.getY()));

					g.setColor(Color.CYAN);
					g.fillRect((int) (rect.getX()), (int) (rect.getY()), 5, 5);
				}
				if (angle_bl >= Math.PI / 2 && angle_bl < Math.PI) {
					o.addPoint((int) (rect.getX() + d2 * Math.cos(angle_bl)),
							(int) (rect.getY() + rect.getHeight() - d2 * Math.sin(angle_bl)));

					o.addPoint((int) (rect.getX()), (int) (rect.getY() + rect.getHeight()));
					g.setColor(Color.MAGENTA);
					g.fillRect((int) (rect.getX()), (int) (rect.getY() + rect.getHeight()), 5, 5);
				}
				if (angle_br >= Math.PI && angle_br < Math.PI * 3 / 2) {
					o.addPoint((int) (rect.getX() + rect.getWidth() + d2 * Math.cos(angle_br)),
							(int) (rect.getY() + rect.getHeight() - d2 * Math.sin(angle_br)));

					o.addPoint((int) (rect.getX() + rect.getWidth()), (int) (rect.getY() + rect.getHeight()));

					g.setColor(Color.PINK);
					g.fillRect((int) (rect.getX() + rect.getWidth()), (int) (rect.getY() + rect.getHeight()), 5, 5);
				}
				if (angle_tr >= Math.PI * 3 / 2 && angle_tr < Math.PI * 2) {
					o.addPoint((int) (rect.getX() + rect.getWidth() + d2 * Math.cos(angle_tr)),
							(int) (rect.getY() - d2 * Math.sin(angle_tr)));

					o.addPoint((int) (rect.getX() + rect.getWidth()), (int) (rect.getY()));

					g.setColor(Color.WHITE);
					g.fillRect((int) (rect.getX() + rect.getWidth()), (int) (rect.getY()), 5, 5);
				}
				// Center points
				if (angle_bl > Math.PI / 2 && angle_bl < Math.PI && angle_br > 0 && angle_br < Math.PI / 2) {
					o.addPoint((int) (rect.getX()), (int) (rect.getY()));
					o.addPoint((int) (rect.getX() + rect.getWidth()),
							(int) (rect.getY()));
				}
				if (angle_bl > Math.PI / 2 && angle_bl < Math.PI && angle_tr > Math.PI / 2 && angle_tr < Math.PI) {
					o.addPoint((int) (rect.getX()), (int) (rect.getY()));
				}
				if (angle_tr > Math.PI / 2 && angle_tr < Math.PI && angle_br > Math.PI && angle_br < Math.PI * 3 / 2) {
					o.addPoint((int) (rect.getX()), (int) (rect.getY() + rect.getHeight()));
					o.addPoint((int) (rect.getX()),
							(int) (rect.getY()));
				}
				if (angle_tr > Math.PI && angle_tr < Math.PI * 3 / 2 && angle_br > Math.PI
						&& angle_br < Math.PI * 3 / 2) {
					o.addPoint((int) (rect.getX()), (int) (rect.getY() + rect.getHeight()));
				}
				if (angle_tl > Math.PI && angle_tl < Math.PI * 3 / 2 && angle_tr > Math.PI * 3 / 2
						&& angle_tr < Math.PI * 2) {
					o.addPoint((int) (rect.getX() + rect.getWidth()), (int) (rect.getY() + rect.getHeight()));
					o.addPoint((int) (rect.getX()),
							(int) (rect.getY() + rect.getHeight()));
				}
				if (angle_tr > Math.PI * 3 / 2 && angle_tr < Math.PI * 2 && angle_bl > Math.PI * 3 / 2
						&& angle_bl < Math.PI * 2) {
					o.addPoint((int) (rect.getX() + rect.getWidth()), (int) (rect.getY() + rect.getHeight()));
				}
				if (angle_tl > 0 && angle_tl < Math.PI / 2 && angle_br > Math.PI * 3 / 2 && angle_br < Math.PI * 2) {
					o.addPoint((int) (rect.getX() + rect.getWidth()),
							(int) (rect.getY()));
					o.addPoint((int) (rect.getX() + rect.getWidth()), (int) (rect.getY() + rect.getHeight()));
				}
				if (angle_tl > 0 && angle_tl < Math.PI / 2 && angle_br > 0 && angle_br < Math.PI / 2) {
					o.addPoint((int) (rect.getX() + rect.getWidth()),
							(int) (rect.getY()));
				}

				shape.subtract(new Area(o));
			}
		}
		// for (LevelWall wall : walls) {
		// 	Rect rect = SchemUtilities.schemToLocalZ(wall, PLAYER_SCREEN_LOC, location, wall.getZ(), GRIDSIZE);
		// 	if (inScreenSpace(rect)) {
		// 		Rectangle2D re = new Rectangle2D.Double(rect.getX(), rect.getY(), rect.getWidth(),
		// 				rect.getHeight());
		// 		Shape s = (Shape) re;
		// 		if(shape.intersects(r));
		// 			shape.add(new Area(s));
		// 	}
		// }
		return shape;
	}

	void drawUI(Graphics2D g) {
		for (ResetBox b : resetboxes) {
			Rect r = SchemUtilities.schemToLocal(b, location, GRIDSIZE);
			if (inScreenSpace(r))
				g.drawRect((int) Math.floor(r.getX()), (int) Math.floor(r.getY()), (int) Math.floor(r.getWidth()),
						(int) Math.floor(r.getHeight()));
		}

		if (EDIT_MODE) {
			g.setColor(Color.RED);
			Point schem = SchemUtilities.schemPointFromFramePos(entry.peripherals.mousePos(), location, GRIDSIZE);
			Point newpoint1 = SchemUtilities.schemToLocalPoint(schem, location, GRIDSIZE);
			BufferedImage img = assets.get(selectasset);
			g.drawRect((int) newpoint1.x, (int) newpoint1.y, (int) img.getWidth(), (int) img.getHeight());
		}

		if(EDIT_MODE)
			g.setColor(Color.ORANGE);
		else
			g.setColor(Color.WHITE);
		Point mouse = entry.peripherals.mousePos();
		Point schem_mouse = SchemUtilities.schemPointFromFramePos(mouse, location, GRIDSIZE);
		int z = 3;
		g.fillOval((int) mouse.x - z, (int) mouse.y - z, 2 * z, 2 * z);

		g.setFont(DEBUG_TEXT);
		String focus = selectasset;
		// if (selecttype == 2)
		// 	focus = selectcolor;
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

		// if (selectstage == true) {
		// 	Point p1 = SchemUtilities.schemToLocalPoint(select_point_1, location, GRIDSIZE);
		// 	Point p2 = SchemUtilities.schemToLocalPoint(
		// 			SchemUtilities.schemPointFromFramePos(entry.peripherals.mousePos(), location, GRIDSIZE),
		// 			location, GRIDSIZE);
		// 	Rect r = new Rect(p1, p2);

		// 	if (selecttype == 0) {
		// 		g.setColor(Color.RED);
		// 		g.setStroke(new BasicStroke(4));
		// 		g.drawRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
		// 	} else if (selecttype == 1) {
		// 		if (assets.containsKey(selectasset))
		// 			g.drawImage(assets.get(selectasset), (int) r.getX(), (int) r.getY(),
		// 					(int) r.getWidth(),
		// 					(int) r.getHeight(), null);
		// 		else {
		// 			g.setColor(Color.RED);
		// 			g.fillRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
		// 		}
		// 	} else if (selecttype == 2) {
		// 		if (colors.containsKey(selectcolor)) {
		// 			g.setColor(colors.get(selectcolor));
		// 			g.fillRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(),
		// 					(int) r.getHeight());
		// 		} else {
		// 			g.setColor(Color.RED);
		// 			g.fillRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
		// 		}
		// 	}
		// }

		if (typing) {
			g.setFont(AMMO_TEXT);
			g.setColor(Color.RED);
			g.drawString(typing_str, 40, 40);
		}

		if (deathscreen) {
			ScreenAnimation.DeathScreen_Graphics(g, animation_tick, this.getWidth(), this.getHeight());
		}

		g.setColor(Color.RED);
		g.fillRect(20, getHeight() - 60, 200, 10);
		// g.setColor(new Color(255, (int) (100 + (120) * sprint_val), 0));
		// g.fillRect(20, getHeight() - 40, (int) (200 * sprint_val), 10);

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
		final int VERT_SPACING = 20;
		final int VERT_MID_SPACING = 6;
		final int SIDE_SPACING = 20;

		g.setFont(DEBUG_SELECT_TEXT);
		int text_height = g.getFontMetrics(DEBUG_SELECT_TEXT).getAscent();

		final int TOTAL_HEIGHT = text_height * debug_vals.size() + VERT_MID_SPACING * (debug_vals.size() - 1)+2*VERT_SPACING;
		final int TOTAL_WIDTH = debug_opts.keySet().stream().map(s -> g.getFontMetrics(DEBUG_SELECT_TEXT).stringWidth(s))
				.max(Comparator.naturalOrder()).get() + 2 * SIDE_SPACING;
		Rect bound = Rect.fromPoints(this.getWidth() - 20 - TOTAL_WIDTH, 20, this.getWidth() - 20,
				20 + TOTAL_HEIGHT);

		g.setColor(Color.DARK_GRAY);
		g.fillRect((int) bound.getX(), (int) bound.getY(), (int) bound.getWidth(), (int) bound.getHeight());

		g.setColor(Color.LIGHT_GRAY);
		g.fillRect((int) bound.x, (int) (bound.y + VERT_SPACING + (debug_selection) * (text_height + VERT_MID_SPACING)),
				(int) bound.width, (int) text_height + 8);

		g.setColor(Color.WHITE);

		for (int i = 0; i < debug_opts.size(); i++) {
			var set = (Entry<String, Runnable>) debug_opts.entrySet().toArray()[i];

			String name = set.getKey();
			Runnable func = set.getValue();
			g.drawString(name, (int) (bound.x + SIDE_SPACING),
					(int) (bound.y + (i + 1) * text_height+(i)*VERT_MID_SPACING+VERT_SPACING));
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
				Math.max(0, (TOPLEFT_BOUND.x) * GRIDSIZE - location.x),
				Math.max(0, (TOPLEFT_BOUND.y) * GRIDSIZE - location.y),
				MathUtil.min_(BOTTOMRIGHT_BOUND.x * GRIDSIZE - location.x,
						getWidth() - TOPLEFT_BOUND.x * GRIDSIZE + location.x,
						(BOTTOMRIGHT_BOUND.x - TOPLEFT_BOUND.x) * GRIDSIZE, getWidth()),
				MathUtil.min_(BOTTOMRIGHT_BOUND.y * GRIDSIZE - location.y,
						getHeight() - TOPLEFT_BOUND.y * GRIDSIZE + location.y,
						(BOTTOMRIGHT_BOUND.y - TOPLEFT_BOUND.y) * GRIDSIZE, getHeight()));

		Graphics2D dispG = (Graphics2D) display.getGraphics();
		Graphics2D extraG = (Graphics2D) extra.getGraphics();

		dispG.setBackground(new Color(0, 0, 0, 0));
		dispG.clearRect(0, 0, this.getWidth(), this.getHeight());
		extraG.setBackground(new Color(0, 0, 0, 0));
		extraG.clearRect(0, 0, this.getWidth(), this.getHeight());
		g.setBackground(Color.BLACK);
		g.clearRect(0, 0, this.getWidth(), this.getHeight());

		if (LIGHT_MODE) {
			dispG.setComposite(ac_def);
			// DRAW MASK

			Shape mask = LightMask(extraG);
			dispG.setColor(Color.BLACK);
			dispG.fill(mask);

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
		g.drawImage(extra, 0,0, null);
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

	/*
	 * MOUSE CLICK EVENT
	 */
	public void mouseClick(Point pos) {
		if (EDIT_MODE) {
			if (select_preview) {
				select_point_1 = SchemUtilities.schemPointFromFramePos(pos, location, GRIDSIZE);

				if (selection_type == 1) {
					BufferedImage img = assets.get(selectasset);
					// else if(selecttype == 2) img = assets.get(selectcolor);

					Rect r = new Rect(select_point_1.x, select_point_1.y, img.getWidth() / Globals.PIXELS_PER_GRID,
							img.getHeight() / Globals.PIXELS_PER_GRID);

					LevelWall c = new LevelWall(r.getX(), r.getY(), r.getWidth(), r.getHeight(), 0.0f, selectasset);
					newWalls.add(c);
				} else if (selection_type == 0) {
					BufferedImage img = assets.get(selectasset);

					Rect r = new Rect(select_point_1.x, select_point_1.y, img.getWidth() / Globals.PIXELS_PER_GRID,
							img.getHeight() / Globals.PIXELS_PER_GRID);

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
				PLAYER_SCREEN_LOC.y + location.y+Globals.PLAYER_SIZE / 2);
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

			if (entry.peripherals.KeyToggled(KeyEvent.VK_I)) {
				if (select_val == debug_vals.size() - 1)
					select_val = 0;
				else
					select_val++;
			}
			if (entry.peripherals.KeyPressed(KeyEvent.VK_L)) {
				double val = debug_vals.get(select_val);
				debug_vals.set(select_val, val + 0.04);
			} else if (entry.peripherals.KeyPressed(KeyEvent.VK_K)) {
				double val = debug_vals.get(select_val);
				debug_vals.set(select_val, val - 0.04);
			}

			if (entry.peripherals.KeyToggled(KeyEvent.VK_UP)) {
				if (debug_selection > 0)
					debug_selection--;
			} else if (entry.peripherals.KeyToggled(KeyEvent.VK_DOWN)) {
				if (debug_selection < debug_opts.size() - 1)
					debug_selection++;
			}

			if (entry.peripherals.KeyToggled(KeyEvent.VK_SPACE)) {
				((Entry<String, Runnable>) debug_opts.entrySet().toArray()[debug_selection]).getValue().run();
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

			if (entry.peripherals.KeyToggled(KeyEvent.VK_X)) {
				selectstage = false;
			}

			if (entry.peripherals.KeyToggled(KeyEvent.VK_B)) {
				selection_type++;
				if (selection_type == 2)
					selection_type = 0;
			}

			if (entry.peripherals.KeyToggled(KeyEvent.VK_SHIFT)) {

				if (dash_count > 0 && entry.tick - dash_delay_tick > Globals.DASH_DELAY) {
					dash_delay_tick = entry.tick;
					dash_count--;
					dash_tick = entry.tick;
					animation = true;
				}
			}

			// if (entry.peripherals.KeyPressed(KeyEvent.VK_Z)) {
			// sprint = true;
			// if (sprint_val > 0)
			// sprint_val -= Globals.SPRINT_DRAIN;
			// sprint_tick = entry.tick;
			// } else {
			// sprint = false;
			// if (sprint_tick + Globals.SPRINT_DELAY < entry.tick && sprint_val != 1) {
			// if (sprint_val + Globals.SPRINT_REGEN > 1)
			// sprint_val = 1;
			// else
			// sprint_val += Globals.SPRINT_REGEN;
			// }
			// }

			if (entry.peripherals.KeyToggled(KeyEvent.VK_P)) {
				LevelConfigUtil.saveLevel();
				levelUpdate();
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
			Rect collider = SchemUtilities.schemToLocal(c, location, GRIDSIZE);

			for (int i = 0; i < bullets.size(); i++) {
				Bullet b = bullets.get(i);
				Rect bullet = new Rect(b.x - location.x, b.y - location.y, b.width, b.height);
				double dx = b.speed * Math.cos(b.angle);
				double dy = b.speed * Math.sin(b.angle);

				double speed_x = dx;
				double speed_y = dy + Globals.BULLET_GRAV_CONST;
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

		dash_count = Globals.DASH_COUNT;
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
				.map(c -> SchemUtilities.schemToLocal(c, location, GRIDSIZE))
				.filter(c -> inScreenSpace(c))
				.collect(Collectors.toList()));
		objects.addAll((List<Rect>) newColliders.stream()
				.map(c -> SchemUtilities.schemToLocal(c, location, GRIDSIZE))
				.filter(c -> inScreenSpace(c))
				.collect(Collectors.toList()));
		// objects.addAll((List<Rect>) walls.stream()
		// 		.filter(c -> c.getZ() == 0)
		// 		.map(c -> SchemUtilities.schemToLocal(c, location, GRIDSIZE))
		// 		.filter(c -> inScreenSpace(c))
		// 		.collect(Collectors.toList()));

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
		location = new Point(p.getX() * GRIDSIZE - PLAYER_SCREEN_LOC.getX(),
				p.getY() * GRIDSIZE - PLAYER_SCREEN_LOC.getY() - PLAYER_SCREEN_LOC.getHeight() / 2 - 1.5 * GRIDSIZE);
	}

	/*
	 * CHECK IF RECTANGLE IS INSIDE SCREEN SPACE
	 */
	boolean inScreenSpace(Rect r) {
		return CollisionUtil.staticCollision(new Rect(0, 0, this.getWidth(), this.getHeight()), r);
	}

	void paintColorRect(Graphics g, ColorRect rect, double depth) {
		Rect r = SchemUtilities.schemToLocal(rect, location, GRIDSIZE);
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
		Rect r = SchemUtilities.schemToLocalZ(p, PLAYER_SCREEN_LOC, location, p.getZ(), GRIDSIZE);
		if (!entry.app.assets.containsKey(p.getAsset())) {
			g.setColor(Color.RED);
			g.fillRect((int) Math.round(r.getX()), (int) Math.round(r.getY()), (int) r.getWidth(), (int) r.getHeight());
		} else {
			BufferedImage img = entry.app.assets.get(p.getAsset());
			g.drawImage(img, (int) Math.round(r.getX()), (int) Math.round(r.getY()), (int) r.getWidth(),
					(int) r.getHeight(), null);
		}
	}

	void paintLevelTile(Graphics g, LevelTile p) {
		Rect r = SchemUtilities.schemToLocalZ(p, PLAYER_SCREEN_LOC, location, p.getZ(), GRIDSIZE);
		if (!entry.app.assets.containsKey(p.getAsset())) {
			g.setColor(Color.RED);
			g.fillRect((int) Math.round(r.getX()), (int) Math.round(r.getY()), (int) r.getWidth(), (int) r.getHeight());
		} else {
			BufferedImage img = entry.app.assets.get(p.getAsset());
			g.drawImage(img, (int) Math.round(r.getX()), (int) Math.round(r.getY()), (int) r.getWidth(),
					(int) r.getHeight(), null);
		}
	}

	BufferedImage resizeToGrid(BufferedImage img, double width, double height) {
		return ImageImport.resize(img, (int) (width / GRIDSIZE * Globals.PIXELS_PER_GRID),
				(int) (height / GRIDSIZE * Globals.PIXELS_PER_GRID));
	}

}