package main;

import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.ImageCapabilities;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
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
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JPanel;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import gameObjects.Bullet;
import gameObjects.Collider;
import gameObjects.ColorRect;
import gameObjects.Enemy;
import gameObjects.LevelTile;
import gameObjects.LevelWall;
import gameObjects.ResetBox;
import inventory.Gun;
import inventory.Magazine;
import inventory.Weapon;
import templates.CalcVector;
import templates.ImageAsset;
import templates.IntPoint;
import templates.Line;
import templates.PathNode;
import templates.Point;
import templates.DirectionVector;
import templates.Rect;
import templates.Size;
import util.CollisionUtil;
import util.ImageUtil;
import util.LevelConfigUtil;
import util.MathUtil;
import util.PathfindingUtil;
import util.SchemUtilities;
import util.ShaderUtil;

public class Application extends JPanel {
	public Point player_screen_pos = new Point(0, 0);
	Rect LEVEL_BOUND = new Rect(0, 0, 0, 0);
	public Point location = new Point(0, 0);

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
	public HashMap<String, ImageAsset> assets = new HashMap<>();
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
	public double looking_angle = 0;
	Weapon weapon = null;
	long last_fire_tick = 0;
	DirectionVector velocity = new DirectionVector(0, 0);
	DirectionVector intent = new DirectionVector(1, 0);

	List<Enemy> enemies = new ArrayList<Enemy>();

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
	boolean DEBUG_MODE = false;
	boolean SHADOWS_MODE = false;
	boolean SHOW_ASSETS_MENU = false;
	boolean FLASHLIGHT_ENABLED = false;
	boolean ENEMY_FOLLOW = false;

	GraphicsConfiguration gconfig = null;

	List<Double> debug_vals = Arrays.asList(0.0, 0.0, 0.0);
	int debug_val_selection = 0;

	List<Triplet<String, Runnable, Callable<String>>> debug_opts = new ArrayList<>();

	int select_val = 0;

	List<PathNode> layers = null;

	List<Collider> subdivided_colliders = null;

	boolean colliding = false;

	CalcVector perpendicular;

	long enemy_tick = 0;

	long enemy_tick_main = 0;

	double health = 100;

	/*
	 * INIT METHOD
	 */
	public void Init(int width, int height) {
		System.out.println("Initializing Application");

		weapon = new Gun(null, 1, 100);

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		gconfig = ge.getDefaultScreenDevice().getDefaultConfiguration();

		ColorModel COLORMODEL = gconfig.getColorModel(Transparency.TRANSLUCENT);

		ac.createContext(COLORMODEL, COLORMODEL, null);
		ac_def.createContext(COLORMODEL, COLORMODEL, null);

		updateRenderResolution();

		for (int x = 0; x < 2; x++) {
			for (int y = 0; y < 2; y++) {
				Enemy enemy1 = new Enemy(31+x, 40 + y, 0.3, pair -> {
					IntPoint start = pair.getValue0();
					IntPoint end = pair.getValue1();
					List<Point> p1 = PathfindingUtil.PathFindByWalls(new PathNode(start, null), new PathNode(end, null), 3, 6,
							entry.app.colliders);
					// List<Point> p1 = PathfindingUtil.directPath(new PathNode(start, null), new PathNode(end, null),
					// 		entry.app.colliders);
					return p1;
				});
				enemies.add(enemy1);
			}
		}

		HashMap<String, Size> assetSizes = new HashMap<>();

		for (final File fileEntry : new File("assets").listFiles()) {
			if (fileEntry.isFile()) {
				String extension = fileEntry.getName().substring(fileEntry.getName().indexOf(".") + 1);
				if (!extension.equalsIgnoreCase("png") && !extension.equalsIgnoreCase("jpg")) {
					continue;
				}
				String name = fileEntry.getName().substring(0, fileEntry.getName().indexOf("."));
				BufferedImage img = ImageUtil.getImage(fileEntry.getPath());
				Size size = new Size(img.getWidth() / Globals.PIXELS_PER_GRID_IMPORT(),
						img.getHeight() / Globals.PIXELS_PER_GRID_IMPORT());
				assetSizes.put(name,
						size);
				if (selectasset.length() == 0)
					selectasset = name;
				BufferedImage resize = ImageUtil.resize(img,
						(int) (img.getWidth() * Globals.PIXELS_RESIZE * 1.0f / Globals.PIXELS_PER_GRID_IMPORT()),
						(int) (img.getHeight() * Globals.PIXELS_RESIZE * 1.0f / Globals.PIXELS_PER_GRID_IMPORT()));
				assets.put(name, new ImageAsset(resize, size));
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
		debug_opts.add(new Triplet<String, Runnable, Callable<String>>("Toggle Debug [%s]", () -> {
			DEBUG_MODE = !DEBUG_MODE;
		}, () -> {
			return String.valueOf(DEBUG_MODE);
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
		debug_opts.add(new Triplet<String, Runnable, Callable<String>>("Enemy Follow [%s]", () -> {
			ENEMY_FOLLOW = !ENEMY_FOLLOW;
		}, () -> {
			return String.valueOf(ENEMY_FOLLOW);
		}));
		debug_opts.add(new Triplet<String, Runnable, Callable<String>>("Toggle Flashlight [%s]", () -> {
			FLASHLIGHT_ENABLED = !FLASHLIGHT_ENABLED;
		}, () -> {
			return String.valueOf(FLASHLIGHT_ENABLED);
		}));
		debug_opts.add(new Triplet<String, Runnable, Callable<String>>("Save Level", () -> {
			LevelConfigUtil.saveLevel();
			levelUpdate();
		}, () -> {
			return "";
		}));

		subdivided_colliders = colliders.stream().map(c -> CollisionUtil.subdivideCollider(c)).flatMap(List::stream)
				.collect(Collectors.toList());
	}
	
	public void updateEnemyAI() {
		for (int i = 0; i < enemies.size();i++) {
			Enemy enemy = enemies.get(i);
			if (!enemy.isAlive()) {
				enemies.remove(i);
				i--;
			}

			if (ENEMY_FOLLOW && entry.tick > reassign_tick + 500) {
				IntPoint end = playerSchemRoundedPos();
				IntPoint start = new IntPoint((int) Math.round(enemy.getPos().getX()),
						(int) Math.round(enemy.getPos().getY()));
		
				enemy.updateRoutine(start, end, false);
			}
			enemy.getIntent();
		}
		for (Enemy enemy : enemies) {
			enemy.step(enemies);

			Point pos1 = playerSchemPos().shift(-0.5,-0.5);

			boolean collision = CollisionUtil.sphereCollision(pos1, Globals.PLAYER_SIZE / Globals.PIXELS_PER_GRID()/2,
					enemy.getPos(), enemy.getRadius());
			if (collision) {
				health -= 0.1;
				Line ln = new Line(pos1, enemy.getPos());
                Line newline = MathUtil.extendLineFromFirstPoint(ln, (Globals.PLAYER_SIZE/Globals.PIXELS_PER_GRID()/2 + enemy.getRadius()) + 0.1);
				//setPlayerPosFromSchem(newline.getP1());
                enemy.setPos(newline.getP2());
			}
		
		}

		if (entry.tick > reassign_tick + 500) {
			reassign_tick = entry.tick;
		}

		for (int i = 0; i < bullets.size(); i++) {
			for (Enemy e : enemies) {
				Pair<Point, Point> collisionline = CollisionUtil.sphereCollision(e.getPos(), e.getRadius(), bullets.get(i).getPos(), bullets.get(i).getRadius(),
						e.getIntent()!=null?e.getIntent():DirectionVector.zero(), bullets.get(i).getDirectionVector());
				if(collisionline!=null)
				{
					e.removeHealth(14);
					bullets.remove(i);
					i--;
					break;
				}
			}
		}
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

		Graphics2D g2 = (Graphics2D) debug_tick.getGraphics();
		g2.setBackground(new Color(0, 0, 0, 0));
		g2.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());

		playerCollisionAndMovementCode((Graphics2D) debug_tick.getGraphics());

		updateEnemyAI();
	
		for (int i = 0; i < bullets.size(); i++) {
			Bullet b = bullets.get(i);
			b.moveBullet();
			boolean colliding = objectCollision(b.getPos(), velocity, b.getRadius());
			if (colliding) {
				bullets.remove(i);
				i--;
			}
		}

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

		g.setColor(Color.YELLOW);
		g.fillOval((int) (player_screen_pos.getX() - Globals.PLAYER_SIZE / 2),
				(int) (player_screen_pos.getY() - Globals.PLAYER_SIZE / 2), (int) Globals.PLAYER_SIZE,
				(int) Globals.PLAYER_SIZE);

		g.setColor(Color.YELLOW);
		for (Enemy enemy : enemies) {
			Point pnew = SchemUtilities.schemToFrame(
					new Point(enemy.getPos().getX() + 0.5, enemy.getPos().getY() + 0.5),
					location, Globals.PIXELS_PER_GRID());
			g.fillOval((int) (pnew.getX() - enemy.getRadius() * Globals.PIXELS_PER_GRID()), (int) (pnew.getY() - enemy.getRadius()* Globals.PIXELS_PER_GRID()),
					(int) (enemy.getRadius()*2* Globals.PIXELS_PER_GRID()), (int) (enemy.getRadius()*2* Globals.PIXELS_PER_GRID()));
		}
		g.setColor(Color.RED);
		for (Bullet b : bullets) {
			Rect r = SchemUtilities.schemToFrame(
					new Rect(b.getPos().shift(-b.getRadius() / 2, -b.getRadius() / 2),
							new Size(b.getRadius(), b.getRadius())),
					location,
					Globals.PIXELS_PER_GRID());
			if (inScreenSpace(r))
				g.fillOval((int) r.left(), (int) r.top(), (int) r.getWidth(), (int) r.getHeight());
		}
		
		for(Enemy e : enemies) {
			Point pos = SchemUtilities.schemToFrame(e.getPos().shift(0.5, 0.5), location, Globals.PIXELS_PER_GRID());
			g.setColor(Color.RED);
			g.fillRect((int)(pos.getX()-e.getRadius()*Globals.PIXELS_PER_GRID()), (int)(pos.getY()-e.getRadius()*Globals.PIXELS_PER_GRID()-10), (int)((e.getRadius()*2*Globals.PIXELS_PER_GRID())*e.getHealth()/100), 6);
		}
	}

	void drawUI(Graphics2D g) {
		for (ResetBox b : resetboxes) {
			Rect r = SchemUtilities.schemToFrame(b, location, Globals.PIXELS_PER_GRID());
			// if (inScreenSpace(r))
			g.drawRect((int) Math.floor(r.left()), (int) Math.floor(r.top()), (int) Math.floor(r.getWidth()),
					(int) Math.floor(r.getHeight()));
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
				ImageAsset a = assets.get(selectasset);
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
				"raw=(%5.1f,%5.1f)  coord=(%5.1f,%5.1f) typing=%b focus=[%s] select_type=%d debug_val=[%s] colliding=%b tick=%d",
				mouse.getX(),
				mouse.getY(), schem_mouse.getX(), schem_mouse.getY(), typing, focus, selection_type,
				debug_vals.toString(), colliding, entry.tick), 20,
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
		g.fillRect(20, this.getHeight() - 60, (int)(200*(health/100)), 10);

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

		if (health <= 0) {
			g.setFont(AMMO_TEXT);
			g.drawString("DEAD", 10, g.getFontMetrics().getAscent()+10);
		}
	}

	void drawOverlay(Graphics2D g) {
		final float point_offset = 0.5f;

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

		g.setColor(Color.BLUE);
		g.setStroke(new BasicStroke(4));

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

		for (Collider s : subdivided_colliders) {
			Line o = s.constrict(0.1);
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

		for (Enemy enemy : enemies) {
			if (enemy.getPath() != null) {
				List<Point> points = enemy.getPath();
				final int psize = 3;

				for (Point point : points) {
					Point p2 = SchemUtilities.schemToFrame(
							new Point(point.getX() + point_offset, point.getY() + point_offset), location,
							Globals.PIXELS_PER_GRID());
					g.drawOval((int) p2.getX() - psize, (int) p2.getY() - psize, psize * 2, psize * 2);
				}
				g.setStroke(new BasicStroke(4));
				for (int i = 0; i < points.size() - 1; i++) {
					Point current = points.get(i);
					Point next = points.get(i + 1);

					Point p1 = SchemUtilities.schemToFrame(
							new Point(current.getX() + point_offset, current.getY() + point_offset), location,
							Globals.PIXELS_PER_GRID());
					Point p2 = SchemUtilities.schemToFrame(
							new Point(next.getX() + point_offset, next.getY() + point_offset), location,
							Globals.PIXELS_PER_GRID());
					g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
				}
			}
		}

		int z2 = 4;
		g.setColor(Color.ORANGE);
		for (Collider c : subdivided_colliders) {
			for (Point pt : PathfindingUtil.getAdjacentPoints(c)) {
				Point pt2 = SchemUtilities.schemToFrame(pt, location, Globals.PIXELS_PER_GRID());
				g.fillOval((int) pt2.getX() - z2, (int) pt2.getY() - z2, z2 * 2, z2 * 2);
			}
		}

		List<Collider> closeColliders = subdivided_colliders.stream()
				.filter(c -> c.center().distance(playerSchemRoundedPos().DPoint()) < 2)
				.collect(Collectors.toList());

		g.setColor(Color.BLUE);
		for (Point p3 : PathfindingUtil.getCorners(closeColliders)) {
		Point p4 = SchemUtilities.schemToFrame(p3.shift(-point_offset,
		-point_offset), location, Globals.PIXELS_PER_GRID());
		g.fillOval((int) p4.getX() - z2, (int) p4.getY() - z2, z2 * 2, z2 * 2);
		}
		// //lines

		g.setStroke(new BasicStroke(3));
		g.setColor(Color.RED);
		for (Line l2 : PathfindingUtil.getCornerLines(closeColliders)) {
			Point p1 = SchemUtilities.schemToFrame(l2.getP1().shift(-point_offset, -point_offset), location,
					Globals.PIXELS_PER_GRID());
			Point p2 = SchemUtilities.schemToFrame(l2.getP2().shift(-point_offset, -point_offset), location,
					Globals.PIXELS_PER_GRID());
			g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
		}

		if (layers != null) {
			PathfindingUtil.displayPath(g, layers, location, (double) Globals.PIXELS_PER_GRID(), Enemy.check);
		}
	}
	/*
	 * PAINT METHOD
	 */

	public Rect LEVEL_SCREEN_SPACE;
	VolatileImage display = null;
	VolatileImage overlay = null;
	VolatileImage debug_tick = null;

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
				Math.min(this.getWidth(), LEVEL_BOUND.right() * Globals.PIXELS_PER_GRID() - location.getX()),
				Math.min(this.getHeight(), LEVEL_BOUND.bottom() * Globals.PIXELS_PER_GRID() - location.getY()));

		Graphics2D dispG = (Graphics2D) display.getGraphics();
		Graphics2D extraG = (Graphics2D) overlay.getGraphics();

		dispG.setBackground(new Color(0, 0, 0, 0));
		dispG.clearRect(0, 0, display.getWidth(), display.getHeight());
		extraG.setBackground(new Color(0, 0, 0, 0));
		extraG.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());
		g.setBackground(Color.BLACK);
		g.clearRect(0, 0, this.getWidth(), this.getHeight());

		if (LIGHT_MODE || SHADOWS_MODE) {
			dispG.setComposite(ac_def);

			Shape mask = ShaderUtil.LightMask(dispG, extraG, SHADOWS_MODE, LIGHT_MODE, FLASHLIGHT_ENABLED);
			dispG.setClip(mask);

			dispG.setComposite(ac);

			RawGame(dispG);

			dispG.setComposite(ac_def);
			dispG.setClip(null);

		} else {
			RawGame(dispG);
		}

		if (OVERLAY_MODE) {
			dispG.drawImage(overlay, 0, 0, null);
			drawOverlay(dispG);
		}
		if (DEBUG_MODE) {
			dispG.drawImage(debug_tick, 0, 0, null);
		}

		drawUI(dispG);
		drawDebugMenu(dispG);

		g.drawImage(display, 0, 0, this.getWidth(), this.getHeight(), null);

	}

	private void drawDebugMenu(Graphics2D g) {
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
			Rect bound = new Rect(this.getWidth() - 20 - TOTAL_WIDTH, 20, this.getWidth() - 20,
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

		player_screen_pos = new Point(width / 2, height / 2);

		ImageCapabilities ic = new ImageCapabilities(true);
		try {

			display = gconfig.createCompatibleVolatileImage((int) width, (int) height, ic, Transparency.TRANSLUCENT);
			overlay = gconfig.createCompatibleVolatileImage((int) width, (int) height, ic, Transparency.TRANSLUCENT);
			debug_tick = gconfig.createCompatibleVolatileImage((int) width, (int) height, ic, Transparency.TRANSLUCENT);
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
	Point select_point_store = null;

	public void mouseClick(Point pos) {
		Point select_point_1 = SchemUtilities.frameToSchemInt(pos, location, Globals.PIXELS_PER_GRID()).DPoint();

		if (EDIT_MODE) {
			if (selection_type == 0) {
				if (select_point_store == null)
					select_point_store = SchemUtilities.frameToSchemInt(pos, location,
							Globals.PIXELS_PER_GRID()).DPoint();
				else {
					Collider c = new Collider(select_point_store, select_point_1);
					newColliders.add(c);
					select_point_store = null;
				}

			} else if (selection_type == 1) {
				ImageAsset a = assets.get(selectasset);

				Rect r = new Rect(select_point_1.getX(), select_point_1.getY(), a.size.getWidth(),
						a.size.getHeight());

				LevelWall c = new LevelWall(r.left(), r.top(), r.getWidth(), r.getHeight(), 0.0f, selectasset);
				newWalls.add(c);
			} else if (selection_type == 2) {
				ImageAsset a = assets.get(selectasset);

				Rect r = new Rect(select_point_1.getX(), select_point_1.getY(), a.size.getWidth(),
						a.size.getHeight());

				LevelTile c = new LevelTile(r.left(), r.top(), r.getWidth(), r.getHeight(), 0.0f, selectasset);
				newTiles.add(c);
			} else if (selection_type == 0) {
				ImageAsset a = assets.get(selectasset);

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

		// if (g.MAG_TYPE() != g.mag.NAME()) {
		// System.out.println("WRONG MAG INSERTED");
		// }

		Point arm = new Point(player_screen_pos.getX() + location.getX(),
				player_screen_pos.getY() + location.getY());
		Point start = new Point(arm.getX() + Globals.BULLET_DEFAULT_DISTANCE * Math.cos(looking_angle),
				arm.getY() + Globals.BULLET_DEFAULT_DISTANCE * Math.sin(looking_angle));

		Point end = new Point(start.getX() / Globals.PIXELS_PER_GRID(),
				start.getY() / Globals.PIXELS_PER_GRID());

		double bullet_size = g.mag.BULLET_SIZE();
		Bullet b = new Bullet(end.getX(), end.getY(), bullet_size, looking_angle,
				Globals.BULLET_SPEED);

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
					(player_screen_pos.getY()) - entry.peripherals.mousePos().getY(),
					(player_screen_pos.getX()) - entry.peripherals.mousePos().getX())) % (2 * Math.PI)
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
				intent = DirectionVector.fromComponents(intent_x, intent_y);
			} else {
				intent = DirectionVector.zero();
			}

			if (entry.peripherals.KeyToggled(KeyEvent.VK_U)) {
				((Gun) weapon).mag = new Magazine(45, "GUN TEST NAME", 0.22);
			}

			// if (entry.peripherals.KeyToggled(KeyEvent.VK_O)) {
			// 	IntPoint end = playerSchemRoundedPos();
			// 	IntPoint start = new IntPoint((int) Math.round(enemy.getPos().getX()),
			// 			(int) Math.round(enemy.getPos().getY()));

				
			// 	enemy.updateRoutine(start, end, true);
			// }

			// if (entry.peripherals.KeyToggled(KeyEvent.VK_P)) {
			// 	IntPoint pos = enemy.getIntPos();
			// 	layers = PathfindingUtil.PathFindByWallsDebug(new PathNode(pos, null), 4, colliders);
			// }

		}
	}

	/*
	 * PLAYER COLLISION AND MOVEMENT CODE
	 */

	private boolean objectCollision(Point objSchemPos, DirectionVector velocity, double objRadius) {
		List<Collider> eligible_colliders = colliders.stream().filter(c -> {
			Point linept = MathUtil.ClosestPointOnLine(c, objSchemPos);
			boolean b = linept.distance(objSchemPos) < objRadius*1;
			boolean b2 = CollisionUtil.LineIntersectsWithColliders(new Line(objSchemPos, linept), colliders);
			return b && !b2;
		}).collect(Collectors.toList());

		CalcVector OMV = new CalcVector(objSchemPos, velocity).scale(1, -1);

		Point newPt = OMV.destination();

		for (Collider c : eligible_colliders) {
			Point linept = MathUtil.ClosestPointOnLine(c, newPt);

			CalcVector colliderV = CalcVector.fromPoints(c.getP1(), c.getP2());

			CalcVector PPosV = CalcVector.fromPoints(linept, objSchemPos);

			// double angle = MathUtil.getAngle(PMV, colliderV);
			CalcVector perpendicular = MathUtil.perpendicularProjection(OMV, colliderV);
			CalcVector parallel = MathUtil.parallelProjection(OMV, colliderV);

			if (MathUtil.dotProduct(OMV, PPosV) <= 0) {
				return true;
			}
		}
		return false;
	}

	public DirectionVector playerNextPosition(Graphics2D g, DirectionVector velocity, Point pos) {
		double r = Globals.PLAYER_SIZE / Globals.PIXELS_PER_GRID() / 2;
		double detection_radius = 1.0 * r;

		Point schemPt = SchemUtilities.frameToSchem(player_screen_pos, location, Globals.PIXELS_PER_GRID());

		DirectionVector speed = new DirectionVector(0, 0);
		// player vector on screen
		CalcVector PMV = new CalcVector(schemPt, velocity).scale(1, -1);

		Point newPt = PMV.scale(1 / Globals.PIXELS_PER_GRID()).destination();

		List<Collider> eligible_colliders = colliders.stream().filter(c -> {
			Point linept = MathUtil.ClosestPointOnLine(c, schemPt);
			boolean b = linept.distance(schemPt) < detection_radius;
			boolean b2 = CollisionUtil.LineIntersectsWithColliders(new Line(schemPt, linept), colliders);
			return b && !b2;
		}).collect(Collectors.toList());

		if (eligible_colliders.size() == 0)
			return velocity;

		for (Collider c : eligible_colliders) {

			Line screenLine = SchemUtilities.schemToFrame(c, location, Globals.PIXELS_PER_GRID());

			g.setColor(Color.MAGENTA);
			g.setStroke(new BasicStroke(2));

			g.drawLine((int) screenLine.getP1().getX(), (int) screenLine.getP1().getY(),
					(int) screenLine.getP2().getX(), (int) screenLine.getP2().getY());

		}

		boolean colliding = false;

		for (Collider c : eligible_colliders) {
			//Collider c = eligible_colliders.get(0);

			Point linept = MathUtil.ClosestPointOnLine(c, newPt);

			CalcVector colliderV = CalcVector.fromPoints(c.getP1(), c.getP2());

			CalcVector PPosV = CalcVector.fromPoints(linept, schemPt);

			// double angle = MathUtil.getAngle(PMV, colliderV);
			CalcVector perpendicular = MathUtil.perpendicularProjection(PMV, colliderV);
			CalcVector parallel = MathUtil.parallelProjection(PMV, colliderV);

			// drawVector(g, PMV, Color.RED);
			// drawVector(g, perpendicular, Color.BLUE);
			// drawVector(g, parallel, Color.ORANGE);

			Point cornerpoint = null;
			if (linept.equals(colliderV.origin()))
				cornerpoint = colliderV.origin();
			else if (linept.equals(colliderV.destination()))
				cornerpoint = colliderV.destination();

			if (cornerpoint != null) {
				CalcVector cvec = CalcVector.fromPoints(cornerpoint, schemPt);
				cvec.setMagnitude(r * 1);

				drawVector(g, cvec, Color.GREEN);

				setPlayerPosFromSchem(cvec.destination());
				return velocity;
			} else {
				if (MathUtil.dotProduct(PMV, PPosV) <= 0) {
					speed = speed.addVector(parallel.scale(1, -1));
					speed = speed.subtractVector(perpendicular.scale(1, -1));
					colliding = true;
				}
				//setPlayerPosFromSchem(perpendicular.withMagnitude(r).scale(-1,-1).origin());
			}
		}

		if (colliding)
			return speed;
		else
			return velocity;
	}

	public void playerCollisionAndMovementCode(Graphics2D g) {
		g.setStroke(new BasicStroke(4));

		velocity = velocity.addVector(intent.multiply(0.14));

		if (velocity.getMagnitude() > Globals.PLAYER_MAX_SPEED)
			velocity.setMagnitude(Globals.PLAYER_MAX_SPEED);
		if (intent.equals(DirectionVector.zero())) {
			if (velocity.getMagnitude() < Globals.PLAYER_MIN_SPEED_CUTOFF)
				velocity = DirectionVector.zero();
			else
				velocity = velocity.addVector(velocity.multiply(-0.08));
		}

		if (CLIP_MODE) {
			location = location.shift(velocity.getDX(), -velocity.getDY());
			return;
		}

		DirectionVector c1 = playerNextPosition(g, velocity, playerSchemPos());

		location = location.shift(c1.getDX(), -c1.getDY());
	}

	public Point playerSchemPos() {
		return SchemUtilities.frameToSchem(new Point((player_screen_pos.getX()), (player_screen_pos.getY())), location,
				Globals.PIXELS_PER_GRID());
	}

	private void drawVector(Graphics2D g, CalcVector v1, Color red) {
		// CalcVector vector = v1;
		g.setStroke(new BasicStroke(2));
		g.setColor(red);
		Line g_line = SchemUtilities.schemToFrame(MathUtil.vectorToLine(v1), location,
				Globals.PIXELS_PER_GRID());
		g.drawLine((int) g_line.getX1(), (int) g_line.getY1(), (int) g_line.getX2(), (int) g_line.getY2());

		Point dst = v1.destination();

		double dangle = 5 * Math.PI / 6;
		CalcVector left = new CalcVector(dst.getX(), dst.getY(), 0.2, v1.getAngle() + dangle);
		CalcVector right = new CalcVector(dst.getX(), dst.getY(), 0.2, v1.getAngle() - dangle);

		Line l_line = SchemUtilities.schemToFrame(new Line(left.origin(), left.destination()), location,
				Globals.PIXELS_PER_GRID());
		Line r_line = SchemUtilities.schemToFrame(new Line(right.origin(), right.destination()), location,
				Globals.PIXELS_PER_GRID());

		g.drawLine((int) l_line.getX1(), (int) l_line.getY1(), (int) l_line.getX2(), (int) l_line.getY2());
		g.drawLine((int) r_line.getX1(), (int) r_line.getY1(), (int) r_line.getX2(), (int) r_line.getY2());
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
		if (p != null && p.getX() != Double.NaN && p.getY() != Double.NaN) {
			location = new Point(p.getX() * Globals.PIXELS_PER_GRID() - player_screen_pos.getX(),
					p.getY() * Globals.PIXELS_PER_GRID() - player_screen_pos.getY());
		}
	}

	/*
	 * CHECK IF RECTANGLE IS INSIDE SCREEN SPACE
	 */
	public boolean inScreenSpace(Rect r) {
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
		Point pos = playerSchemPos();
		return new IntPoint(pos);
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
		return ImageUtil.resize(img, (int) (width / Globals.PIXELS_PER_GRID() * Globals.PIXELS_PER_GRID()),
				(int) (height / Globals.PIXELS_PER_GRID() * Globals.PIXELS_PER_GRID()));
	}

}