package main;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import org.javatuples.Triplet;

import gameObjects.Collider;
import gameObjects.LevelTile;
import gameObjects.LevelWall;
import gameObjects.entities.Enemy;
import templates.ImageAsset;
import templates.Point;
import templates.Rect;
import templates.Vector;
import util.DrawUtil;
import util.PathfindingUtil;
import util.SchematicUtil;

import java.awt.Font;
import java.awt.Color;

import java.awt.image.BufferedImage;
import java.awt.BasicStroke;

public class DebugOverlay {
    static final Font DEBUG_FONT = new Font("Arial", Font.PLAIN, 24);
    public static final int ASSET_MENU_HORIZONTAL = 10;
    static final float POINT_OFFSET = 0.5f;

    public static void DrawStatsOverlay(Graphics2D g, String... args) {
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(4));
        g.setFont(DEBUG_FONT);

        for (int i = 0; i < args.length; i++) {
            g.drawString(args[i], 20, g.getFontMetrics(DEBUG_FONT).getAscent() * (1 + i) + 20);
        }
    }

    public static void DrawPathfindingPoints(GraphicsContext g, Point location, List<Collider> subdivided_colliders) {
        final int pointsize = 3;
        
        for (Collider c : subdivided_colliders) {
            Point pt3 = SchematicUtil.schemToFrame(c.getCenter(), location);
            for (Point pt : PathfindingUtil.getAdjacentPoints(c)) {
                Point pt2 = SchematicUtil.schemToFrame(pt, location);
                g.drawLine((int) pt3.getX(), (int) pt3.getY(), (int) pt2.getX(), (int) pt2.getY(), new Color(184, 123, 77), 2);
                
                g.fillCircle((int) pt2.getX(), (int) pt2.getY(), pointsize, Color.ORANGE);
                
            }
        }
    }
    
    public static void DrawPathfindingCorners(GraphicsContext g, Point location, List<Collider> subdivided_colliders) {
        final int pointsize = 3;
        
		for (Vector l2 : PathfindingUtil.getCornerLines(subdivided_colliders)) {
			Point p0 = SchematicUtil.schemToFrame(l2.origin(), location);
            Point p1 = SchematicUtil.schemToFrame(l2.destination(), location);
            
            Point pt3 = SchematicUtil.schemToFrame(l2.getCenter(), location);
            for (Point pt : PathfindingUtil.getAdjacentPoints(l2)) {
                Point pt2 = SchematicUtil.schemToFrame(pt, location);
                g.drawLine((int) pt3.getX(), (int) pt3.getY(), (int) pt2.getX(), (int) pt2.getY(), Color.PINK, 3);
                
                g.fillCircle(pt2.getX(), pt2.getY(), pointsize, Color.ORANGE);
                
            }

            g.drawLine((int) p0.getX(), (int) p0.getY(), (int) p1.getX(), (int) p1.getY(), Color.BLUE, 3);
		}
    }

    public static void DrawEnemyPaths(GraphicsContext g, Point location, List<Enemy> enemies) {
        final int psize = 3;

        for (Enemy enemy : enemies) {
			if (enemy.getPath() != null) {
				List<Point> points = enemy.getPath();
				

                for (Point point : points) {
                    Point p2 = SchematicUtil.schemToFrame(
                            new Point(point.getX() + POINT_OFFSET, point.getY() + POINT_OFFSET), location);
                    g.drawCircle(p2.getX(),  p2.getY(), psize * 2, Color.ORANGE, 4);
                }
				
				for (int i = 0; i < points.size() - 1; i++) {
					Point current = points.get(i);
					Point next = points.get(i + 1);

					Point p1 = SchematicUtil.schemToFrame(
							new Point(current.getX() + POINT_OFFSET, current.getY() + POINT_OFFSET), location);
					Point p2 = SchematicUtil.schemToFrame(
							new Point(next.getX() + POINT_OFFSET, next.getY() + POINT_OFFSET), location);
					g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY(), Color.YELLOW, 2);
				}
			}
		}
    }

    public static void DrawNewGameObjectOverlay(GraphicsContext g, Point location) {
        for (LevelTile o : entry.app.newTiles) {

            Rect r = SchematicUtil.schemToFrame(o, location);
            if (entry.app.inScreenSpace(r))
                DrawUtil.paintLevelTile(g, location, (LevelTile) o);

        }
        for (LevelWall o : entry.app.newWalls) {
            Rect r = SchematicUtil.schemToFrame(o, location);
            if (entry.app.inScreenSpace(r))
                DrawUtil.paintLevelWall(g, location, (LevelWall) o);
        }

        for (Collider o : entry.app.newColliders) {
            Vector r = SchematicUtil.schemToFrame(o, location);
            if (entry.app.inScreenSpace(r))
                g.drawLine(r.origin().getX(),  r.origin().getY(),  r.destination().getX(), r.destination().getY(), Color.BLUE, 2);
        }
    }
    
    public static void DrawColliders(GraphicsContext g, Point location, List<Collider> subdivided_colliders) {

        for (Collider o : subdivided_colliders) {
            Vector r = SchematicUtil.schemToFrame(o, location);//.constrict(0.1)
            if (entry.app.inScreenSpace(r))
                g.drawLine((int) r.origin().getX(), (int) r.origin().getY(), (int) r.destination().getX(),
                        (int) r.destination().getY(), Color.RED, 3);
        }
    }

    public static void DrawGrid(GraphicsContext gc, Point location, Rect level_schem_space) {
        for (int x1 = (int) level_schem_space.left(); x1 < level_schem_space.right(); x1++) {
            for (int y1 = (int) level_schem_space.top(); y1 < level_schem_space.bottom(); y1++) {
                Point p = SchematicUtil.schemToFrame(new Point(x1, y1), location);
                if (entry.app.inScreenSpace(p) || entry.app.inScreenSpace(SchematicUtil.schemToFrame(new Point(x1, y1).shift(1, 1), location))) {

                    gc.drawLine(p.getX(), p.getY(), p.getX(),
                            p.getY() + AppConstants.PIXELS_PER_GRID(), Color.GREEN, 2);

                    gc.drawLine(p.getX(),p.getY(),
                            p.getX() + AppConstants.PIXELS_PER_GRID(),
                            p.getY(), Color.GREEN, 2);
                }
            
                
            }
        }
    }
    
    public static void OutlineScreenSpace(Graphics2D g, Rect screenSpace) {
        g.setStroke(new BasicStroke(6));
			g.setColor(Color.BLUE);
			g.drawRect((int) screenSpace.left(), (int) screenSpace.top(),
					(int) screenSpace.getWidth(), (int) screenSpace.getHeight());
    }

    public static void DrawEditModeOverlays(GraphicsContext c, Point location, Point store_point,
            int selection_type, HashMap<String, ImageAsset> assets, String selected_asset) {
        Point new_point = SchematicUtil.roundSchemFrame(entry.peripherals.mousePos(), location);	
        if (selection_type == 0) {
				if (store_point != null) {
					Point newpoint2 = SchematicUtil.schemToFrame(store_point, location);

					c.drawLine(new_point.getX(), new_point.getY(), newpoint2.getX(),
							 newpoint2.getY(), Color.RED, 4);
				}
			} else if(selection_type==1) {
				ImageAsset a = assets.get(selected_asset);
				if (a != null) {
					c.drawRect(new_point.getX(), new_point.getY(),
							(a.size.getWidth() * AppConstants.PIXELS_PER_GRID()),
                             (a.size.getHeight() * AppConstants.PIXELS_PER_GRID()), Color.RED, 2);
                    c.drawImage(a.source, new_point.getX(), new_point.getY(),
                    (a.size.getWidth() * AppConstants.PIXELS_PER_GRID()),
                     (a.size.getHeight() * AppConstants.PIXELS_PER_GRID()));
				}
			}
    }

    public static void DrawDebugDropdown(GraphicsContext g, List<Triplet<String, Runnable, Callable<String>>> options,
            int selection) {
        int WIDTH = entry.app.getWidth();
        int HEIGHT = entry.app.getHeight();

        final int VERT_SPACING = 20;
        final int VERT_MID_SPACING = 6;
        final int SIDE_SPACING = 20;

        int text_height = g.getGraphics().getFontMetrics(DEBUG_FONT).getAscent();

        final int TOTAL_HEIGHT = text_height * options.size() + VERT_MID_SPACING * (options.size())
                + 2 * VERT_SPACING;
        final int TOTAL_WIDTH = options.stream()
                .map(s -> g.getGraphics().getFontMetrics(DEBUG_FONT).stringWidth(s.getValue0()))
                .max(Comparator.naturalOrder()).get() + 2 * SIDE_SPACING;
        Rect bound = new Rect(WIDTH - 20 - TOTAL_WIDTH, 20, WIDTH - 20,
                20 + TOTAL_HEIGHT);

        g.fillRect((int) bound.left(), (int) bound.top(), (int) bound.getWidth(), (int) bound.getHeight(), Color.DARK_GRAY);

        g.fillRect((int) bound.left(),
                (int) (bound.top() + VERT_SPACING + (selection) * (text_height + VERT_MID_SPACING)),
                (int) bound.getWidth(), (int) text_height + 8, Color.GRAY);

        for (int i = 0; i < options.size(); i++) {
            Triplet<String, Runnable, Callable<String>> triplet = options.get(i);

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
                    (int) (bound.top() + (i) * text_height + (i) * VERT_MID_SPACING + VERT_SPACING), Color.WHITE, DEBUG_FONT);
        }
    }
    
    public static void DrawAssetLibrary(GraphicsContext g, HashMap<String, ImageAsset> assets, int selection) {
        int WIDTH = entry.app.getWidth();
        int HEIGHT = entry.app.getHeight();
        final int BUFFER = 50;
        final int BETWEEN_IMG = 10;
        

        ArrayList<String> keys = new ArrayList<String>(assets.keySet());
        g.fillRect(BUFFER, BUFFER, WIDTH - 2 * BUFFER, HEIGHT - 2 * BUFFER, new Color(80, 80, 80, 200));
			
			int img_size = (int) (WIDTH- 2 * BUFFER - (ASSET_MENU_HORIZONTAL + 1) * BETWEEN_IMG)
					/ ASSET_MENU_HORIZONTAL;
			
			for (int y = 0; y <= (int) (assets.size() / ASSET_MENU_HORIZONTAL); y++) {
				for (int x = 0; x < ASSET_MENU_HORIZONTAL; x++) {

					int index = y * ASSET_MENU_HORIZONTAL + x;
					if (index >= assets.size())
						break;

					int posx = BUFFER + x * (img_size) + (x + 1) * BETWEEN_IMG;
					int posy = BUFFER + y * (img_size) + (y + 1) * 2 * BETWEEN_IMG;
					BufferedImage img = assets.get(keys.get(index)).source;

					if (selection == index) {
                        g.fillRect(posx - 4, posy - 4, img_size + 8, img_size + 8, Color.ORANGE);
                        
					}
					g.drawImage(img, posx, posy, img_size, img_size);


					String msg = keys.get(index);
					int height = g.getGraphics().getFontMetrics().getAscent();
					int width = (int) g.getGraphics().getFontMetrics().getStringBounds(msg, g.getGraphics()).getWidth();
					g.drawString(msg, posx + img_size / 2 - width / 2, posy + img_size + 10 + height, Color.WHITE, DEBUG_FONT);
				}
			}
    }
}
