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

    public static void DrawPathfindingPoints(Graphics2D g, Point location, List<Collider> subdivided_colliders) {
        final int pointsize = 3;
        
        for (Collider c : subdivided_colliders) {
            Point pt3 = SchematicUtil.schemToFrame(c.getCenter(), location);
            for (Point pt : PathfindingUtil.getAdjacentPoints(c)) {
                Point pt2 = SchematicUtil.schemToFrame(pt, location);
                g.setColor(new Color(184, 123, 77));
                g.drawLine((int) pt3.getX(), (int) pt3.getY(), (int) pt2.getX(), (int) pt2.getY());
                
                g.setColor(Color.ORANGE);
                g.fillOval((int) pt2.getX() - pointsize, (int) pt2.getY() - pointsize, pointsize * 2, pointsize * 2);
                
            }
        }
    }
    
    public static void DrawPathfindingCorners(Graphics2D g, Point location, List<Collider> subdivided_colliders) {
        final int pointsize = 3;
        
        g.setStroke(new BasicStroke(3));
		g.setColor(Color.RED);
		for (Vector l2 : PathfindingUtil.getCornerLines(subdivided_colliders)) {
			Point p0 = SchematicUtil.schemToFrame(l2.origin(), location);
            Point p1 = SchematicUtil.schemToFrame(l2.destination(), location);
            
            Point pt3 = SchematicUtil.schemToFrame(l2.getCenter(), location);
            for (Point pt : PathfindingUtil.getAdjacentPoints(l2)) {
                Point pt2 = SchematicUtil.schemToFrame(pt, location);
                g.setColor(Color.PINK);
                g.drawLine((int) pt3.getX(), (int) pt3.getY(), (int) pt2.getX(), (int) pt2.getY());
                
                g.setColor(Color.ORANGE);
                g.fillOval((int) pt2.getX() - pointsize, (int) pt2.getY() - pointsize, pointsize * 2, pointsize * 2);
                
            }

            g.setColor(Color.BLUE);
            g.drawLine((int) p0.getX(), (int) p0.getY(), (int) p1.getX(), (int) p1.getY());
		}
    }

    public static void DrawEnemyPaths(Graphics2D g, Point location, List<Enemy> enemies) {
        final int psize = 3;
        g.setStroke(new BasicStroke(4));

        for (Enemy enemy : enemies) {
			if (enemy.getPath() != null) {
				List<Point> points = enemy.getPath();
				

                for (Point point : points) {
                    Point p2 = SchematicUtil.schemToFrame(
                            new Point(point.getX() + POINT_OFFSET, point.getY() + POINT_OFFSET), location);
                    g.drawOval((int) p2.getX() - psize, (int) p2.getY() - psize, psize * 2, psize * 2);
                }
				
				for (int i = 0; i < points.size() - 1; i++) {
					Point current = points.get(i);
					Point next = points.get(i + 1);

					Point p1 = SchematicUtil.schemToFrame(
							new Point(current.getX() + POINT_OFFSET, current.getY() + POINT_OFFSET), location);
					Point p2 = SchematicUtil.schemToFrame(
							new Point(next.getX() + POINT_OFFSET, next.getY() + POINT_OFFSET), location);
					g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
				}
			}
		}
    }

    public static void DrawNewGameObjectOverlay(Graphics2D g, Point location) {
        g.setColor(Color.BLUE);
        g.setStroke(new BasicStroke(4));

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
                g.drawLine((int) r.origin().getX(), (int) r.origin().getY(), (int) r.destination().getX(), (int) r.destination().getY());
        }
    }
    
    public static void DrawColliders(Graphics2D g, Point location, List<Collider> subdivided_colliders) {
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(3));

        for (Collider o : subdivided_colliders) {
            Vector r = SchematicUtil.schemToFrame(o, location);//.constrict(0.1)
            if (entry.app.inScreenSpace(r))
                g.drawLine((int) r.origin().getX(), (int) r.origin().getY(), (int) r.destination().getX(),
                        (int) r.destination().getY());
        }
    }

    public static void DrawGrid(Graphics2D g, Point location, Rect level_schem_space) {
        g.setColor(Color.GREEN);
        g.setStroke(new BasicStroke(2));
        
        for (int x1 = (int) level_schem_space.left(); x1 < level_schem_space.right(); x1++) {
            for (int y1 = (int) level_schem_space.top(); y1 < level_schem_space.bottom(); y1++) {
                Point p = SchematicUtil.schemToFrame(new Point(x1, y1), location);
                if (entry.app.inScreenSpace(p) || entry.app.inScreenSpace(SchematicUtil.schemToFrame(new Point(x1, y1).shift(1, 1), location))) {

                    g.drawLine((int) p.getX(), (int) p.getY(), (int) p.getX(),
                            (int) (p.getY() + AppConstants.PIXELS_PER_GRID()));

                    g.drawLine((int) p.getX(), (int) p.getY(),
                            (int) (p.getX() + AppConstants.PIXELS_PER_GRID()),
                            (int) p.getY());
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

    public static void DrawEditModeOverlays(Graphics2D g, Point location, Point store_point, Point new_point, int selection_type, HashMap<String, ImageAsset> assets, String selected_asset) {
			if (selection_type == 0) {
				if (store_point != null) {
					Point newpoint2 = SchematicUtil.schemToFrame(store_point, location);

					g.drawLine((int) new_point.getX(), (int) new_point.getY(), (int) newpoint2.getX(),
							(int) newpoint2.getY());
				}
			} else {
				ImageAsset a = assets.get(selected_asset);
				if (a != null) {
					g.drawRect((int) new_point.getX(), (int) new_point.getY(),
							(int) (a.size.getWidth() * AppConstants.PIXELS_PER_GRID()),
							(int) (a.size.getHeight() * AppConstants.PIXELS_PER_GRID()));
				}
			}
    }

    public static void DrawDebugDropdown(Graphics2D g, List<Triplet<String, Runnable, Callable<String>>> options,
            int selection) {
        int WIDTH = entry.app.getWidth();
        int HEIGHT = entry.app.getHeight();

        final int VERT_SPACING = 20;
        final int VERT_MID_SPACING = 6;
        final int SIDE_SPACING = 20;

        g.setFont(DEBUG_FONT);
        int text_height = g.getFontMetrics(DEBUG_FONT).getAscent();

        final int TOTAL_HEIGHT = text_height * options.size() + VERT_MID_SPACING * (options.size())
                + 2 * VERT_SPACING;
        final int TOTAL_WIDTH = options.stream()
                .map(s -> g.getFontMetrics(DEBUG_FONT).stringWidth(s.getValue0()))
                .max(Comparator.naturalOrder()).get() + 2 * SIDE_SPACING;
        Rect bound = new Rect(WIDTH - 20 - TOTAL_WIDTH, 20, WIDTH - 20,
                20 + TOTAL_HEIGHT);

        g.setColor(Color.DARK_GRAY);
        g.fillRect((int) bound.left(), (int) bound.top(), (int) bound.getWidth(), (int) bound.getHeight());

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect((int) bound.left(),
                (int) (bound.top() + VERT_SPACING + (selection) * (text_height + VERT_MID_SPACING)),
                (int) bound.getWidth(), (int) text_height + 8);

        g.setColor(Color.WHITE);

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
                    (int) (bound.top() + (i + 1) * text_height + (i) * VERT_MID_SPACING + VERT_SPACING));
        }
    }
    
    public static void DrawAssetLibrary(Graphics2D g, HashMap<String, ImageAsset> assets, int selection) {
        int WIDTH = entry.app.getWidth();
        int HEIGHT = entry.app.getHeight();
        final int BUFFER = 50;
        final int BETWEEN_IMG = 10;
        

        ArrayList<String> keys = new ArrayList<String>(assets.keySet());
        g.setColor(new Color(80, 80, 80, 200));
        g.fillRect(BUFFER, BUFFER, WIDTH - 2 * BUFFER, HEIGHT - 2 * BUFFER);
			
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
    }
}
