package util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gameObjects.Collider;
import gameObjects.LevelTile;
import gameObjects.LevelWall;
import main.GraphicsContext;
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

	public static void paintLevelTile(GraphicsContext g, Point location, LevelTile p) {
		Rect r = SchematicUtil.schemToFrame(p, location);
		if (!entry.app.assets.containsKey(p.getAsset())) {
			g.fillRect(r.left(), r.top(), r.getWidth(), r.getHeight(), Color.RED);
		} else {
			BufferedImage img = entry.app.assets.get(p.getAsset()).source;
			g.drawImage(img, r.left(), r.top(), r.getWidth(),
					r.getHeight());
		}
	}

    public static void paintLevelWall(GraphicsContext g, Point location, LevelWall p) {
    	Rect r = SchematicUtil.schemToFrame(p, location);
    	if (!entry.app.assets.containsKey(p.getAsset())) {
    		g.fillRect(r.left(), r.top(), r.getWidth(), r.getHeight(), Color.RED);
    	} else {
    		BufferedImage img = entry.app.assets.get(p.getAsset()).source;
    		g.drawImage(img, r.left(), r.top(), r.getWidth(),
					r.getHeight());
    	}
    }

    public static void drawVector(GraphicsContext c, Point location, Vector v1, Color red) {
    	// CalcVector vector = v1;
    	Vector g_line = SchematicUtil.schemToFrame(v1, location);
    	c.drawLine(g_line.origin().getX(), g_line.origin().getY(),  g_line.destination().getX(), g_line.destination().getY(), red, 3);
    
    	Point dst = v1.destination();
    
    	double dangle = 5 * Math.PI / 6;
    	Vector left = new Vector(dst.getX(), dst.getY(), 0.2, v1.getAngle() + dangle);
    	Vector right = new Vector(dst.getX(), dst.getY(), 0.2, v1.getAngle() - dangle);
    
    	Vector l_line = SchematicUtil.schemToFrame(Vector.fromPoints(left.origin(), left.destination()), location);
    	Vector r_line = SchematicUtil.schemToFrame(Vector.fromPoints(right.origin(), right.destination()), location);
    
    	c.drawLine(l_line.origin().getX(), l_line.origin().getY(), l_line.destination().getX(), l_line.destination().getY(), red, 3);
    	c.drawLine(r_line.origin().getX(), r_line.origin().getY(), r_line.destination().getX(),  r_line.destination().getY(), red, 3);
    }
}
