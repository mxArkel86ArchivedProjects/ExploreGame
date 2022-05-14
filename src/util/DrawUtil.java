package util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gameObjects.Collider;
import gameObjects.LevelTile;
import gameObjects.LevelWall;
import main.entry;
import templates.Point;
import templates.Rect;
import templates.Vector;

public class DrawUtil {
	public static void DrawInvertedPolygon(List<Polygon> polygons, Graphics2D g, int width, int height) {
		List<Integer> x = new ArrayList<Integer>();
		List<Integer> y = new ArrayList<Integer>();
		for (Polygon p : polygons) {
			for (int i : p.xpoints)
				x.add(i);
			for (int i : p.ypoints)
				y.add(i);
			x.add(width / 2);
			y.add(height / 2);
		}
		x.addAll(Arrays.asList(0, width, width, 0, 0));
		y.addAll(Arrays.asList(0, 0, height, height, 0));
		int x_[] = x.stream()
				.mapToInt(Integer::intValue)
				.toArray();
		int y_[] = y.stream()
				.mapToInt(Integer::intValue)
				.toArray();

		g.fillPolygon(x_, y_, x.size());
	}

	public static void paintLevelTile(Graphics g, Point location, LevelTile p) {
		Rect r = SchematicUtil.schemToFrame(p, location);
		if (!entry.app.assets.containsKey(p.getAsset())) {
			g.setColor(Color.RED);
			g.fillRect((int) Math.round(r.left()), (int) Math.round(r.top()), (int) r.getWidth(), (int) r.getHeight());
		} else {
			BufferedImage img = entry.app.assets.get(p.getAsset()).source;
			g.drawImage(img, (int) Math.round(r.left()), (int) Math.round(r.top()), (int) r.getWidth(),
					(int) r.getHeight(), null);
		}
	}

    public static void paintLevelWall(Graphics g, Point location, LevelWall p) {
    	Rect r = SchematicUtil.schemToFrame(p, location);
    	if (!entry.app.assets.containsKey(p.getAsset())) {
    		g.setColor(Color.RED);
    		g.fillRect((int) Math.round(r.left()), (int) Math.round(r.top()), (int) r.getWidth(), (int) r.getHeight());
    	} else {
    		BufferedImage img = entry.app.assets.get(p.getAsset()).source;
    		g.drawImage(img, (int) Math.round(r.left()), (int) Math.round(r.top()), (int) r.getWidth(),
    				(int) r.getHeight(), null);
    	}
    }

    public static void drawVector(Graphics2D g, Point location, Vector v1, Color red) {
    	// CalcVector vector = v1;
    	g.setStroke(new BasicStroke(2));
    	g.setColor(red);
    	Vector g_line = SchematicUtil.schemToFrame(v1, location);
    	g.drawLine((int) g_line.origin().getX(), (int) g_line.origin().getY(), (int) g_line.destination().getX(), (int) g_line.destination().getY());
    
    	Point dst = v1.destination();
    
    	double dangle = 5 * Math.PI / 6;
    	Vector left = new Vector(dst.getX(), dst.getY(), 0.2, v1.getAngle() + dangle);
    	Vector right = new Vector(dst.getX(), dst.getY(), 0.2, v1.getAngle() - dangle);
    
    	Vector l_line = SchematicUtil.schemToFrame(Vector.fromPoints(left.origin(), left.destination()), location);
    	Vector r_line = SchematicUtil.schemToFrame(Vector.fromPoints(right.origin(), right.destination()), location);
    
    	g.drawLine((int) l_line.origin().getX(), (int) l_line.origin().getY(), (int) l_line.destination().getX(), (int) l_line.destination().getY());
    	g.drawLine((int) r_line.origin().getX(), (int) r_line.origin().getY(), (int) r_line.destination().getX(), (int) r_line.destination().getY());
    }
}
