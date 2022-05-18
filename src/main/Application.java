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

import gameObjects.Collider;
import gameObjects.LevelTile;
import gameObjects.LevelWall;
import gameObjects.entities.Bullet;
import gameObjects.entities.Enemy;
import gameObjects.entities.Entity;
import inventory.Gun;
import inventory.Magazine;
import inventory.Weapon;
import templates.Vector;
import templates.ImageAsset;
import templates.IntPoint;
import templates.PathNode;
import templates.Point;
import templates.DirectionVector;
import templates.Rect;
import templates.Size;
import util.CollisionUtil;
import util.DrawUtil;
import util.ImageUtil;
import util.LevelConfigUtil;
import util.MathUtil;
import util.PathfindingUtil;
import util.SchematicUtil;
import util.ShaderUtil;

public class Application extends JPanel {
	public Point player_screen_pos = new Point(0, 0);
	Rect LEVEL_SCHEM_SPACE = new Rect(0, 0, 0, 0);
	public Point location = new Point(0, 0);

	/*
	 * GAME OBJECTS
	 */
	public List<Collider> colliders = new ArrayList<Collider>();
	public List<LevelWall> walls = new ArrayList<LevelWall>();
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

	int asset_library_selection = 0;

	/*
	 * MISC
	 */
	boolean typing = false;
	String typing_str = "";
	long reassign_tick = 0;

	/*
	 * GAME TOGGLES
	 */
	int debug_dropdown_selection = 0;
	boolean SHOW_GRID = true;
	boolean LIGHT_MODE = false;
	boolean EDIT_MODE = false;
	boolean CLIP_MODE = false;
	boolean OVERLAY_MODE = false;
	boolean SHOW_ASSETS_MENU = false;
	boolean FLASHLIGHT_ENABLED = false;
	boolean ENEMY_FOLLOW = false;

	GraphicsConfiguration gconfig = null;

	public List<Double> debug_vals = Arrays.asList(0.0, 0.0, 0.0);
	int debug_val_selection = 0;

	List<Triplet<String, Runnable, Callable<String>>> debug_opts = new ArrayList<>();

	int select_val = 0;

	List<PathNode> layers = null;

	List<Collider> subdivided_colliders = null;

	boolean colliding = false;

	Vector perpendicular;

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

		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				Enemy enemy1 = new Enemy(31 + x, 40 + y, 0.3, pair -> {
					IntPoint start = pair.getValue0();
					IntPoint end = pair.getValue1();
					List<Point> p1 = PathfindingUtil.PathFindByWalls(new PathNode(start, null), new PathNode(end, null),
							2, 3,
							entry.app.colliders);
					// List<Point> p1 = PathfindingUtil.directPath(new PathNode(start, null), new
					// PathNode(end, null),
					// entry.app.colliders);
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
				Size size = new Size(img.getWidth() / AppConstants.PIXELS_PER_GRID_IMPORT(),
						img.getHeight() / AppConstants.PIXELS_PER_GRID_IMPORT());
				assetSizes.put(name,
						size);
				if (selectasset.length() == 0)
					selectasset = name;
				BufferedImage resize = ImageUtil.resize(img,
						(int) (img.getWidth() * AppConstants.PIXELS_RESIZE * 1.0f
								/ AppConstants.PIXELS_PER_GRID_IMPORT()),
						(int) (img.getHeight() * AppConstants.PIXELS_RESIZE * 1.0f
								/ AppConstants.PIXELS_PER_GRID_IMPORT()));
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
			asset_library_selection = 0;
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
		for (int i = 0; i < enemies.size(); i++) {
			Enemy enemy = enemies.get(i);
			if (!enemy.isAlive()) {
				enemies.remove(i);
				i--;
			}

			enemy.doStep();

			if (ENEMY_FOLLOW && entry.tick > reassign_tick + 500) {
				IntPoint end = playerSchemRoundedPos();
				IntPoint start = new IntPoint((int) Math.round(enemy.getPos().getX()),
						(int) Math.round(enemy.getPos().getY()));

				enemy.updateRoutine(start, end, false);
			}
		}
		for (Enemy enemy : enemies) {
			Point pos1 = playerSchemPos().shift(-0.5, -0.5);

			boolean collision = CollisionUtil.sphereCollision(pos1,
					AppConstants.PLAYER_SIZE / AppConstants.PIXELS_PER_GRID() / 2,
					enemy.getPos(), enemy.getRadius());
			if (collision) {
				health -= 0.1;
				Vector ln = Vector.fromPoints(pos1, enemy.getPos());
				Vector newline = MathUtil.extendLineFromFirstPoint(ln,
						(AppConstants.PLAYER_SIZE / AppConstants.PIXELS_PER_GRID() / 2 + enemy.getRadius()) + 0.1);
				// setPlayerPosFromSchem(newline.getP1());
				enemy.setPos(newline.destination());
			}

		}

		if (entry.tick > reassign_tick + 500) {
			reassign_tick = entry.tick;
		}

		for (int i = 0; i < bullets.size(); i++) {
			for (Enemy e : enemies) {
				boolean collides = CollisionUtil.sphereCollision(new Entity(e.getRadius(), e.getPos().shift(0.5, 0.5), e.getDirectionVector()), bullets.get(i));
				if (collides) {
					e.removeHealth(8);
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

		Graphics2D g2 = (Graphics2D) overlay.getGraphics();
		g2.setBackground(new Color(0, 0, 0, 0));
		g2.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());

		playerCollisionAndMovementCode();

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

	void RawGame(GraphicsContext g) {
		for (LevelTile o : tiles) {

			Rect r = SchematicUtil.schemToFrame(o, location);
			if (inScreenSpace(r))
				DrawUtil.paintLevelTile(g, location, (LevelTile) o);

		}

		for (LevelWall o : walls) {

			Rect r = SchematicUtil.schemToFrame(o, location);
			if (inScreenSpace(r))
				DrawUtil.paintLevelWall(g, location, (LevelWall) o);

		}

		g.fillCircle(player_screen_pos.getX(), player_screen_pos.getY(), AppConstants.PLAYER_SIZE/2, Color.YELLOW);

		for (Enemy enemy : enemies) {
			Point pnew = SchematicUtil.schemToFrame(
					new Point(enemy.getPos().getX() + 0.5, enemy.getPos().getY() + 0.5),
					location);
			g.fillCircle(pnew.getX(), pnew.getY(), enemy.getRadius()*AppConstants.PIXELS_PER_GRID(), Color.YELLOW);
		}
		
		for (Bullet b : bullets) {
			Rect r = SchematicUtil.schemToFrame(
					new Rect(b.getPos().shift(-b.getRadius() / 2, -b.getRadius() / 2),
							new Size(b.getRadius(), b.getRadius())),
					location);
			if (inScreenSpace(r))
				g.fillCircle(r.center().getX(), r.center().getY(), r.getWidth()/2, Color.RED);
		}

		for (Enemy e : enemies) {
			Point pos = SchematicUtil.schemToFrame(e.getPos().shift(0.5, 0.5), location);
			
			g.fillRect((int) (pos.getX() - e.getRadius() * AppConstants.PIXELS_PER_GRID()),
					(int) (pos.getY() - e.getRadius() * AppConstants.PIXELS_PER_GRID() - 10),
					(int) ((e.getRadius() * 2 * AppConstants.PIXELS_PER_GRID()) * e.getHealth() / 100), 6, Color.RED);
		}
	}

	void drawPlayerUI(GraphicsContext g) {
		// HEALTH BARS
		g.fillRect(20, this.getHeight() - 60, (int) (200 * (health / 100)), 10, Color.RED);

		g.drawRect(20, this.getHeight() - 60, 200, 10, Color.BLACK, 2);
		g.drawRect(20, this.getHeight() - 40, 200, 10, Color.BLACK, 2);


		Gun gun = (Gun) weapon;
		if (gun.mag != null) {
			String bullet_str = String.format("%d/%d", gun.mag.bullet_count, gun.mag.BULLET_MAX());
			g.drawString(bullet_str,
					(int) (-g.getGraphics().getFontMetrics().getStringBounds(bullet_str, g.getGraphics()).getWidth() + this.getWidth() - 20),
					(int) (-20 + this.getHeight()), Color.WHITE, AMMO_TEXT);
		}

		// FIRING DELAY INDICATOR
		Point mousepos = entry.peripherals.mousePos();
		if (entry.tick - last_fire_tick < gun.FIRING_DELAY()) {
			int size1 = 14;
			double percent = ((entry.tick - last_fire_tick) * 1.0f / gun.FIRING_DELAY());
			g.drawArc(mousepos.getX() - size1, mousepos.getY() - size1, 2 * size1, 2 * size1, 0,
					(360 * percent), Color.WHITE, 2);
		}
	}
	/*
	 * PAINT METHOD
	 */

	public Rect LEVEL_SCREEN_SPACE;
	VolatileImage display = null;
	VolatileImage overlay = null;
	VolatileImage light_mask = null;

	AlphaComposite ac_def = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
	AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_IN, 1.0f);
	

	@Override
	public void paint(Graphics g1) {
		Graphics2D panelGraphics = (Graphics2D) g1;

		panelGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		panelGraphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
				RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
		panelGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

		LEVEL_SCREEN_SPACE = new Rect(
				Math.max(0, LEVEL_SCHEM_SPACE.left() * AppConstants.PIXELS_PER_GRID() - location.getX()),
				Math.max(0, LEVEL_SCHEM_SPACE.top() * AppConstants.PIXELS_PER_GRID() - location.getY()),
				Math.min(this.getWidth(), LEVEL_SCHEM_SPACE.right() * AppConstants.PIXELS_PER_GRID() - location.getX()),
				Math.min(this.getHeight(),
						LEVEL_SCHEM_SPACE.bottom() * AppConstants.PIXELS_PER_GRID() - location.getY()));

		GraphicsContext gc = new GraphicsContext((Graphics2D) display.getGraphics(), display.getWidth(), display.getHeight());
		GraphicsContext lightMaskContext = new GraphicsContext((Graphics2D) light_mask.getGraphics(), light_mask.getWidth(), light_mask.getHeight());
		GraphicsContext overlayContext = new GraphicsContext((Graphics2D) overlay.getGraphics(), overlay.getWidth(), overlay.getHeight());

		

		Graphics2D g = (Graphics2D) gc.getGraphics();
		//g.setComposite(ac_def);

		gc.clear();
		lightMaskContext.clear();

		panelGraphics.setBackground(Color.BLACK);
		panelGraphics.clearRect(0, 0, this.getWidth(), this.getHeight());
		

		if (LIGHT_MODE) {
			Shape mask = ShaderUtil.LightMask(lightMaskContext, overlayContext, FLASHLIGHT_ENABLED);
			
			g.setClip(mask);
			g.drawImage(light_mask, 0, 0, null);
			

			g.setComposite(ac);

			RawGame(gc);

			g.setComposite(ac_def);
			g.setClip(null);
		} else {
			RawGame(gc);
		}

		if (SHOW_GRID)
			DebugOverlay.DrawGrid(gc, location, LEVEL_SCHEM_SPACE);

		if (layers != null) {
			PathfindingUtil.displayPath(gc, layers, location, Enemy.check);
		}

		drawPlayerUI(gc);

		if (OVERLAY_MODE) {
			DebugOverlay.DrawPathfindingPoints(gc, location, subdivided_colliders);
			DebugOverlay.DrawPathfindingCorners(gc, location, subdivided_colliders);

			DebugOverlay.DrawEnemyPaths(gc, location, enemies);

			DebugOverlay.DrawColliders(gc, location, subdivided_colliders);

			gc.drawImage(overlay, 0, 0, this.getWidth(), this.getHeight());

		}

		DebugOverlay.DrawStatsOverlay(g, playerSchemPos().toString(), debug_vals.stream().map(x->x.toString()).collect(Collectors.joining(", ")));

		if (SHOW_ASSETS_MENU) {
			DebugOverlay.DrawAssetLibrary(gc, assets, asset_library_selection);
		} else {
			DebugOverlay.DrawDebugDropdown(gc, debug_opts, debug_dropdown_selection);
		}

		drawCursor(gc);
		

		panelGraphics.drawImage(display, 0, 0, this.getWidth(), this.getHeight(), null);
	}

	private void drawCursor(GraphicsContext g) {
		Point p2 = entry.peripherals.mousePos();
		g.fillCircle(p2.getX(), p2.getY(), 3, Color.WHITE);
	}

	/*
	 * RESIZE EVENT
	 */
	public void updateRenderResolution() {
		int width = this.getWidth();
		int height = this.getHeight();

		player_screen_pos = new Point(width / 2, height / 2);

		ImageCapabilities ic = new ImageCapabilities(true);
		try {

			display = gconfig.createCompatibleVolatileImage((int) width, (int) height, ic, Transparency.TRANSLUCENT);
			overlay = gconfig.createCompatibleVolatileImage((int) width, (int) height, ic, Transparency.TRANSLUCENT);
			light_mask = gconfig.createCompatibleVolatileImage((int) width, (int) height, ic, Transparency.TRANSLUCENT);
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
		Point select_point_1 = SchematicUtil.frameToSchemInt(pos, location).DPoint();

		if (EDIT_MODE) {
			if (selection_type == 0) {
				if (select_point_store == null)
					select_point_store = SchematicUtil.frameToSchemInt(pos, location).DPoint();
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
		Point start = new Point(arm.getX() + AppConstants.BULLET_DEFAULT_DISTANCE * Math.cos(looking_angle),
				arm.getY() + AppConstants.BULLET_DEFAULT_DISTANCE * Math.sin(looking_angle));

		Point end = new Point(start.getX() / AppConstants.PIXELS_PER_GRID(),
				start.getY() / AppConstants.PIXELS_PER_GRID());

		double bullet_size = g.mag.BULLET_SIZE();
		Bullet b = new Bullet(end.getX(), end.getY(), bullet_size, looking_angle,
				AppConstants.BULLET_SPEED);

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
				if (asset_library_selection < assets.size() - 1)
					asset_library_selection++;
			}
			if (entry.peripherals.KeyToggled(KeyEvent.VK_LEFT)) {
				if (asset_library_selection > 0)
					asset_library_selection--;
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
					if (asset_library_selection - DebugOverlay.ASSET_MENU_HORIZONTAL > 0)
						asset_library_selection -= DebugOverlay.ASSET_MENU_HORIZONTAL;
				} else if (debug_dropdown_selection > 0)
					debug_dropdown_selection--;
			} else if (entry.peripherals.KeyToggled(KeyEvent.VK_DOWN)) {
				if (SHOW_ASSETS_MENU) {
					if (asset_library_selection + DebugOverlay.ASSET_MENU_HORIZONTAL < assets.size())
						asset_library_selection += DebugOverlay.ASSET_MENU_HORIZONTAL;
				}
				if (debug_dropdown_selection < debug_opts.size() - 1)
					debug_dropdown_selection++;
			}

			if (entry.peripherals.KeyToggled(KeyEvent.VK_SPACE)) {
				if (SHOW_ASSETS_MENU) {
					selectasset = new ArrayList<String>(assets.keySet()).get(asset_library_selection);
					SHOW_ASSETS_MENU = false;
				} else
					debug_opts.get(debug_dropdown_selection).getValue1().run();
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
		}
	}

	/*
	 * PLAYER COLLISION AND MOVEMENT CODE
	 */

	private boolean objectCollision(Point objSchemPos, DirectionVector velocity, double objRadius) {
		List<Collider> eligible_colliders = colliders.stream().filter(c -> {
			Point linept = MathUtil.ClosestPointOnLine(c, objSchemPos);
			boolean b = linept.distance(objSchemPos) < objRadius * 1;
			boolean b2 = CollisionUtil.LineIntersectsWithColliders(Vector.fromPoints(objSchemPos, linept), colliders);
			return b && !b2;
		}).collect(Collectors.toList());

		Vector OMV = new Vector(objSchemPos, velocity).scale(1, -1);

		Point newPt = OMV.destination();

		for (Collider c : eligible_colliders) {
			Point linept = MathUtil.ClosestPointOnLine(c, newPt);


			Vector PPosV = Vector.fromPoints(linept, objSchemPos);

			// double angle = MathUtil.getAngle(PMV, c);
			Vector perpendicular = MathUtil.perpendicularProjection(OMV, c);
			Vector parallel = MathUtil.parallelProjection(OMV, c);

			if (MathUtil.dotProduct(OMV.directionComponent(), PPosV.directionComponent()) <= 0) {
				return true;
			}
		}
		return false;
	}

	public DirectionVector playerNextPosition(DirectionVector velocity, Point pos) {
		double r = AppConstants.PLAYER_SIZE / AppConstants.PIXELS_PER_GRID() / 2;
		double detection_radius = 1.15 * r;

		Point schemPt = SchematicUtil.frameToSchem(player_screen_pos, location);

		DirectionVector speed = new DirectionVector(0, 0);
		// player vector on screen
		Vector PMV = new Vector(schemPt, velocity).scale(1, -1);

		Point newPt = PMV.scale(1 / AppConstants.PIXELS_PER_GRID()).destination();

		List<Collider> eligible_colliders = colliders.stream().filter(c -> {
			Point linept = MathUtil.ClosestPointOnLine(c, schemPt);
			boolean b = linept.distance(schemPt) < detection_radius;
			boolean b2 = CollisionUtil.LineIntersectsWithColliders(Vector.fromPoints(schemPt, linept), colliders);
			return b && !b2;
		}).collect(Collectors.toList());

		if (eligible_colliders.size() == 0)
			return null;
		
		Graphics2D g = (Graphics2D) overlay.getGraphics();

		for (Collider c : eligible_colliders) {

			Vector screenLine = SchematicUtil.schemToFrame(c, location);

			g.setColor(Color.MAGENTA);
			g.setStroke(new BasicStroke(2));

			g.drawLine((int) screenLine.origin().getX(), (int) screenLine.origin().getY(),
					(int) screenLine.destination().getX(), (int) screenLine.destination().getY());

		}

		boolean colliding = false;
	
		
		for (Collider c : eligible_colliders) {
			// Collider c = eligible_colliders.get(0);

			Point linept = MathUtil.ClosestPointOnLine(c, newPt);

			Vector PPosV = Vector.fromPoints(linept, schemPt);

			// double angle = MathUtil.getAngle(PMV, c);
			// double factor = 1/Math.sin(angle==0?0.00001:angle);
			//Vector modPMV = PMV.scale(factor);
			// double angle = MathUtil.getAngle(PMV, c);
			Vector perpendicular = MathUtil.perpendicularProjection(PMV, c);
			Vector parallel = MathUtil.parallelProjection(PMV, c);

			// drawVector(g, PMV, Color.RED);
			// drawVector(g, perpendicular, Color.BLUE);
			// drawVector(g, parallel, Color.ORANGE);
			

			Point cornerpoint = null;
			if (linept.equals(c.origin()))
				cornerpoint = c.origin();
			else if (linept.equals(c.destination()))
				cornerpoint = c.destination();

			if (cornerpoint != null) {
				Vector cvec = Vector.fromPoints(cornerpoint, schemPt);
				cvec.setMagnitude(r * 1);
				
				
				setPlayerPosFromSchem(cvec.destination());
				
				return null;
			} else {
				if (MathUtil.dotProduct(PMV.directionComponent(), PPosV.directionComponent()) <= 0) {
					speed = speed.addVector(parallel.scale(1, -1));
					speed = speed.subtractVector(perpendicular.scale(1, -1));
					colliding = true;
					setPlayerPosFromSchem(perpendicular.withMagnitude(r).scale(-1, -1).origin());
				}
			}
		}

		if (colliding)
			return speed;
		else
			return null;
	}

	public void playerCollisionAndMovementCode() {
		velocity = velocity.addVector(intent.multiply(0.14));

		if (velocity.getMagnitude() > AppConstants.PLAYER_MAX_SPEED)
			velocity.setMagnitude(AppConstants.PLAYER_MAX_SPEED);
		if (intent.equals(DirectionVector.zero())) {
			if (velocity.getMagnitude() < AppConstants.PLAYER_MIN_SPEED_CUTOFF)
				velocity = DirectionVector.zero();
			else
				velocity = velocity.addVector(velocity.multiply(-0.08));
		}

		if (CLIP_MODE) {
			location = location.shift(velocity.getDX(), -velocity.getDY());
			return;
		}

		DirectionVector c1 = playerNextPosition(velocity, playerSchemPos());

		if (c1 != null) {
			location = location.shift(c1.getDX(), -c1.getDY());
			//velocity = c1;
		}
		else
			location = location.shift(velocity.getDX(), -velocity.getDY());
	}

	public Point playerSchemPos() {
		return SchematicUtil.frameToSchem(new Point((player_screen_pos.getX()), (player_screen_pos.getY())), location);
	}

	/*
	 * UPDATE SCHEMATIC LEVEL BOUNDS BASED ON GAME OBJECTS
	 */
	void levelUpdate() {
		LEVEL_SCHEM_SPACE = new Rect(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);

		List<Rect> all = new ArrayList<>();
		all.addAll(walls.stream().map(c -> (Rect) c).collect(Collectors.toList()));
		all.addAll(colliders.stream().map(c -> new Rect(c.origin(), c.destination())).collect(Collectors.toList()));
		all.addAll(newColliders.stream().map(c -> new Rect(c.origin(), c.destination())).collect(Collectors.toList()));
		all.addAll(newWalls.stream().map(c -> (Rect) c).collect(Collectors.toList()));
		all.addAll(tiles.stream().map(c -> (Rect) c).collect(Collectors.toList()));
		all.addAll(newTiles.stream().map(c -> (Rect) c).collect(Collectors.toList()));

		for (Rect o : all) {
			if (o.left() < LEVEL_SCHEM_SPACE.left())
				LEVEL_SCHEM_SPACE.setP1(new Point(o.left(), LEVEL_SCHEM_SPACE.top()));
			if (o.right() > LEVEL_SCHEM_SPACE.right())
				LEVEL_SCHEM_SPACE.setP2(new Point(o.right(), LEVEL_SCHEM_SPACE.bottom()));
			if (o.top() < LEVEL_SCHEM_SPACE.top())
				LEVEL_SCHEM_SPACE.setP1(new Point(LEVEL_SCHEM_SPACE.left(), o.top()));
			if (o.bottom() > LEVEL_SCHEM_SPACE.bottom())
				LEVEL_SCHEM_SPACE.setP2(new Point(LEVEL_SCHEM_SPACE.right(), o.bottom()));
		}
	}

	/*
	 * Move player given a SCHEMATIC coordinate
	 */
	void setPlayerPosFromSchem(Point p) {
		if (p != null && p.getX() != Double.NaN && p.getY() != Double.NaN) {
			location = new Point(p.getX() * AppConstants.PIXELS_PER_GRID() - player_screen_pos.getX(),
					p.getY() * AppConstants.PIXELS_PER_GRID() - player_screen_pos.getY());
		}
	}

	/*
	 * CHECK IF RECTANGLE IS INSIDE SCREEN SPACE
	 */
	public boolean inScreenSpace(Rect r) {
		return CollisionUtil.RectRectIntersection(LEVEL_SCREEN_SPACE, r);
	}

	boolean inScreenSpace(Vector l) {
		return inScreenSpace(l.origin()) && inScreenSpace(l.destination());
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
}