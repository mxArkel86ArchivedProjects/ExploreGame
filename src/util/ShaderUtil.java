package util;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.util.List;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import gameObjects.LevelWall;
import main.AppConstants;
import main.GraphicsContext;
import main.entry;
import templates.Point;
import templates.Rect;
import templates.Size;

public class ShaderUtil {
    
    
    public static Shape LightMask(GraphicsContext dispG, GraphicsContext debug, boolean shading, boolean clipping, boolean flashlight_enabled) {
        double looking_angle = entry.app.looking_angle;
        Point location = entry.app.location;
		Point player_pos = entry.app.player_screen_pos;
		double player_size = AppConstants.PLAYER_SIZE;
        Rect screen = entry.app.LEVEL_SCREEN_SPACE;

    	double dx = 360 * Math.cos(looking_angle);
    	double dy = 360 * Math.sin(looking_angle);
    
    	double small_radius = 40;
		Point center = player_pos;
    
    	
    	Triplet<Double, Double, Double> args[] = new Triplet[] {
    			new Triplet<Double, Double, Double>(0.0, -1.0, 0.0),
    			new Triplet<Double, Double, Double>(-6.0, 0.0, -10.0) };
    
		Area visibility = new Area();
		Area wall_detection = new Area();

		Area areas[] = new Area[] { visibility, wall_detection};
    
        // set clipping or shading default background
    	if (!shading) {
			dispG.fillRect(0,0,screen.getWidth(),screen.getHeight(), Color.BLACK);
    	}
    	if (!clipping) {
    		visibility.add(new Area(new Rectangle2D.Float(0, 0, (int)screen.getWidth(), (int)screen.getHeight())));
    	}
    
    	if (!shading && !clipping)
    		return visibility;
    
    	// --------
        Polygon light = new Polygon();
    	double angle_1 = looking_angle - AppConstants.FLASHLIGHT_FOV;
    	double angle_2 = looking_angle + AppConstants.FLASHLIGHT_FOV;
    	light.addPoint((int) (center.getX() + small_radius * Math.cos(angle_1)),
    			(int) (center.getY() + small_radius * Math.sin(angle_1)));
    
    	light.addPoint((int) (center.getX() + dx + AppConstants.FLASHLIGHT_RANGE * Math.cos(angle_1)),
    			(int) (center.getY() + dy + AppConstants.FLASHLIGHT_RANGE * Math.sin(angle_1)));
    	light.addPoint((int) (center.getX() + dx + AppConstants.FLASHLIGHT_RANGE * Math.cos(angle_2)),
    			(int) (center.getY() + dy + AppConstants.FLASHLIGHT_RANGE * Math.sin(angle_2)));
    
    	light.addPoint((int) (center.getX() + small_radius * Math.cos(angle_2)),
    			(int) (center.getY() + small_radius * Math.sin(angle_2)));
    
    	Ellipse2D e2d = new Ellipse2D.Double(center.getX() - AppConstants.INNER_RADIUS, center.getY() - AppConstants.INNER_RADIUS,
    			AppConstants.INNER_RADIUS * 2, AppConstants.INNER_RADIUS * 2);
    
    	if (shading) {
    
    		RadialGradientPaint rgp = new RadialGradientPaint(new Point2D.Double(center.getX(), center.getY()),
    				(float) AppConstants.INNER_RADIUS,
    				new float[] { 0.0f, 0.5f, 1.0f },
    				new Color[] { Color.BLACK, new Color(0, 0, 0, 220), new Color(0, 0, 0, 0) });
    
    		dispG.getGraphics().setPaint(rgp);
    		dispG.getGraphics().fill(
    				new Ellipse2D.Double(center.getX() - AppConstants.INNER_RADIUS, center.getY() - AppConstants.INNER_RADIUS,
    						AppConstants.INNER_RADIUS * 2, AppConstants.INNER_RADIUS * 2));
    
    		GradientPaint gp = new GradientPaint(new Point2D.Double(center.getX(), center.getY()),
    		Color.BLACK,
    		new Point2D.Double(center.getX() + AppConstants.FLASHLIGHT_RANGE *
    		Math.cos(looking_angle),
    		center.getY() + AppConstants.FLASHLIGHT_RANGE * Math.sin(looking_angle)),
					new Color(0, 0, 0, 0));
			if (flashlight_enabled) {
				dispG.getGraphics().setPaint(gp);
				dispG.getGraphics().fill(light);
			}
    
    		dispG.getGraphics().setPaint(null);
    
    	}
    	if (clipping) {
    		visibility.add(new Area(e2d));
			wall_detection.add(new Area(e2d));

			if (flashlight_enabled) {
				wall_detection.add(new Area(light));
				visibility.add(new Area(light));
			}
    	}
    
    	if (clipping) {
    
    		for (LevelWall w : entry.app.walls) {
    			Rect rect = SchematicUtil.schemToFrame(w, location);
    			if (entry.app.inScreenSpace(rect)) {
    				Point PLAYER_CENTER = player_pos;
    				Point WALL_CENTER = rect.center();
    				double overall_angle = MathUtil.clipAngle(Math.atan2(PLAYER_CENTER.getY() - WALL_CENTER.getY(),
    						WALL_CENTER.getX() - PLAYER_CENTER.getX()));
    				double angle_tl = MathUtil.clipAngle(Math.atan2(PLAYER_CENTER.getY() - rect.top(),
    						rect.left() - PLAYER_CENTER.getX()));
    				double angle_tr = MathUtil.clipAngle(Math.atan2(PLAYER_CENTER.getY() - rect.top(),
    						rect.right() - PLAYER_CENTER.getX()));
    				double angle_bl = MathUtil.clipAngle(Math.atan2(PLAYER_CENTER.getY() - rect.bottom(),
    						rect.left() - PLAYER_CENTER.getX()));
    				double angle_br = MathUtil.clipAngle(Math.atan2(PLAYER_CENTER.getY() - rect.bottom(),
    						rect.right() - PLAYER_CENTER.getX()));
    
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
    
    						outer_first = MathUtil.PointOnScreenEdge(angle_br,
    								new Point(rect.right() - end_buff,
    										rect.bottom() - end_buff), screen.getSize());
    
    					} else if (angle_tr >= Math.PI / 2 && angle_tr < Math.PI) {
    						inner_first = new Point((int) (rect.right() - corner_buff),
    								(int) Math.ceil(rect.top() + corner_buff));
    
    						outer_first = MathUtil.PointOnScreenEdge(angle_tr,
    								new Point(rect.right() - end_buff, rect.top() + end_buff), screen.getSize());
    
    					} else if (angle_tl >= Math.PI && angle_tl < Math.PI * 3 / 2) {
    						inner_first = new Point((int) Math.ceil(rect.left() + corner_buff),
    								(int) Math.ceil(rect.top() + corner_buff));
    
    						outer_first = MathUtil.PointOnScreenEdge(angle_tl,
    								new Point(rect.left() + end_buff, rect.top() + end_buff), screen.getSize());
    
    					} else if (angle_bl >= Math.PI * 3 / 2 && angle_bl < Math.PI * 2) {
    						inner_first = new Point((int) Math.ceil(rect.left() + corner_buff),
    								(int) (rect.bottom() - corner_buff));
    
    						outer_first = MathUtil.PointOnScreenEdge(angle_bl,
    								new Point(rect.left() + end_buff, rect.bottom() + end_buff), screen.getSize());
    
    					}

    
    					if (angle_tl >= 0 && angle_tl < Math.PI / 2) {//0-1.57  4.8
    						outer_last = MathUtil.PointOnScreenEdge(angle_tl,
    								new Point(rect.left() + end_buff, rect.top() + end_buff), screen.getSize());
    
    						inner_last = new Point((int) Math.ceil(rect.left() + corner_buff),
    								(int) Math.ceil(rect.top() + corner_buff));
    
    					} else if (angle_bl >= Math.PI / 2 && angle_bl < Math.PI) {//1.57-3.14
    						outer_last = MathUtil.PointOnScreenEdge(angle_bl,
    								new Point(rect.left() + end_buff, rect.bottom() - end_buff), screen.getSize());
    
    						inner_last = new Point((int) Math.ceil(rect.left() + corner_buff),
    								(int) (rect.bottom() - corner_buff));
    
    					} else if (angle_br >= Math.PI && angle_br < Math.PI * 3 / 2) {//3.14-4.71
    						outer_last = MathUtil.PointOnScreenEdge(angle_br,
    								new Point(rect.right() - end_buff, rect.bottom() - end_buff), screen.getSize());
    
    						inner_last = new Point((int) (rect.right() - corner_buff),
    								(int) (rect.bottom() - corner_buff));
    
    					} else if (angle_tr >= Math.PI * 3 / 2 && angle_tr < Math.PI * 2) {//4.71-6.28
    						outer_last = MathUtil.PointOnScreenEdge(angle_tr,
    								new Point(rect.right() - end_buff, rect.top() + end_buff), screen.getSize());
    
    						inner_last = new Point((int) (rect.right() - corner_buff),
    								(int) Math.ceil(rect.top() + corner_buff));
    
    					}
    
    					if (inner_first != null) {
    						o.addPoint((int) inner_first.getX(), (int) inner_first.getY());
    						if (i == 0) {
    							debug.fillRect((int) inner_first.getX() - AppConstants.OVERLAY_MARKER_SIZE / 2,
    									(int) inner_first.getY() - AppConstants.OVERLAY_MARKER_SIZE / 2,
										AppConstants.OVERLAY_MARKER_SIZE, AppConstants.OVERLAY_MARKER_SIZE, Color.WHITE);
    						}
    					}
    
    					if (outer_first != null) {
    						o.addPoint((int) outer_first.getValue1().getX(), (int) outer_first.getValue1().getY());
    						if (i == 0) {
    							debug.fillRect((int) outer_first.getValue1().getX() - AppConstants.OVERLAY_MARKER_SIZE / 2,
    									(int) outer_first.getValue1().getY() - AppConstants.OVERLAY_MARKER_SIZE / 2,
    									AppConstants.OVERLAY_MARKER_SIZE, AppConstants.OVERLAY_MARKER_SIZE, Color.ORANGE);
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
    								o.addPoint((int)screen.getWidth(), 0);
    							} else if ((outer_first.getValue0().equals("bottom")
    									&& outer_last.getValue0().equals("left")) ||
    									(outer_first.getValue0().equals("left")
    											&& outer_last.getValue0().equals("bottom"))) {
    								o.addPoint(0, (int)screen.getHeight());
    							} else if ((outer_first.getValue0().equals("bottom")
    									&& outer_last.getValue0().equals("right")) ||
    									(outer_first.getValue0().equals("right")
    											&& outer_last.getValue0().equals("bottom"))) {
    								o.addPoint((int)screen.getWidth(), (int)screen.getHeight());
    							} else if ((outer_first.getValue0().equals("top")
    									&& outer_last.getValue0().equals("bottom")) ||
    									(outer_first.getValue0().equals("bottom")
    											&& outer_last.getValue0().equals("top"))) {
    								if (overall_angle >= Math.PI / 2 && overall_angle < Math.PI * 3 / 2) {
    									o.addPoint(0, 0);
    									o.addPoint(0, (int)screen.getHeight());
    								} else {
    									o.addPoint((int)screen.getWidth(), (int)screen.getHeight());
    									o.addPoint((int)screen.getWidth(), 0);
    								}
    							} else if ((outer_first.getValue0().equals("left")
    									&& outer_last.getValue0().equals("right")) ||
    									(outer_first.getValue0().equals("right")
    											&& outer_last.getValue0().equals("left"))) {
    								if (overall_angle >= 0 && overall_angle < Math.PI) {
    									o.addPoint(0, 0);
    									o.addPoint((int)screen.getWidth(), 0);
    								} else {
    									o.addPoint(0, (int)screen.getHeight());
    									o.addPoint((int)screen.getWidth(), (int)screen.getHeight());
    								}
    							}
    						}
    					}
    
    					if (outer_last != null) {
    						o.addPoint((int) outer_last.getValue1().getX(), (int) outer_last.getValue1().getY());
    						if (i == 0) {
    							debug.fillRect((int) outer_last.getValue1().getX() - AppConstants.OVERLAY_MARKER_SIZE / 2,
    									(int) outer_last.getValue1().getY() - AppConstants.OVERLAY_MARKER_SIZE / 2,
    									AppConstants.OVERLAY_MARKER_SIZE, AppConstants.OVERLAY_MARKER_SIZE, Color.BLUE);
    						}
    					}
    
    					if (inner_last != null) {
    						o.addPoint((int) inner_last.getX(), (int) inner_last.getY());
    						if (i == 0) {
    							debug.fillRect((int) inner_last.getX() - AppConstants.OVERLAY_MARKER_SIZE / 2,
    									(int) inner_last.getY() - AppConstants.OVERLAY_MARKER_SIZE / 2,
    									AppConstants.OVERLAY_MARKER_SIZE, AppConstants.OVERLAY_MARKER_SIZE, Color.GREEN);
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
    					areas[i].subtract(new Area(o));
    				}
    
    			}
    		}
    
    		//visibility.subtract(new Area(areas[0]));
    	}
    	dispG.getGraphics().setPaint(null);
    
    	for (LevelWall wall : entry.app.walls) {
    		if (wall.getAsset().equals("wood")) {
                Rect r = SchematicUtil.schemToFrame(wall, location);
                
                // if(!entry.app.inScreenSpace(r))
                //     continue;
    			Point obj = r.center();
    
    			Ellipse2D e2d2 = new Ellipse2D.Double(obj.getX() - AppConstants.LAMP_RADIUS, obj.getY() - AppConstants.LAMP_RADIUS,
    					AppConstants.LAMP_RADIUS * 2, AppConstants.LAMP_RADIUS * 2);
    
    			if (clipping)
    				visibility.add(new Area(e2d2));
    
    			if (shading) {
    				RadialGradientPaint rgp = new RadialGradientPaint(new Point2D.Double(obj.getX(), obj.getY()),
    						(float) AppConstants.LAMP_RADIUS,
    						new float[] { 0.0f, 0.5f, 1.0f },
    						new Color[] { Color.BLACK, new Color(0, 0, 0, 220), new Color(0, 0, 0, 0) });
    
    				dispG.getGraphics().setPaint(rgp);
    				dispG.getGraphics().fill(new Ellipse2D.Double(obj.getX() - AppConstants.LAMP_RADIUS, obj.getY() - AppConstants.LAMP_RADIUS,
    						AppConstants.LAMP_RADIUS * 2, AppConstants.LAMP_RADIUS * 2));
    			}
    		}
    	}
		
		// debug.setBackground(new Color(0, 0, 0, 0));
		// debug.clearRect(0, 0, (int)screen.getWidth(), (int)screen.getHeight());
		
		
		Rectangle2D screenrect = new Rectangle2D.Double(0, 0, screen.getWidth(), screen.getHeight());
    	
		for (LevelWall wall : entry.app.walls) {
			Rect wallrect = SchematicUtil.schemToFrame(wall, location);
			
				Rectangle2D re = new Rectangle2D.Double((int) Math.round(wallrect.left()),
						(int) Math.round(wallrect.top()),
						wallrect.getWidth(),
						wallrect.getHeight());

				boolean b1 = wall_detection.intersects(re);
				boolean b2 = wall_detection.contains(re);
				boolean b3 = screenrect.intersects(re);
				boolean b4 = screenrect.contains(re);
				// if(!screenrect.contains(re))
				// 	continue;
				if(b1&&b4)//normal wall
					visibility.add(new Area((Shape) re));
				else if(!b1 && b4)//wall behind cover
				{
						
				} else if (b1 && !b4) {//wall outside of bounds

				}
				
		}
		
		// debug.setColor(Color.RED);
		// debug.fill(vis);
    	
		// Area a = new Area();
		// a.add(new Area(new Rectangle2D.Float(0,0,(int)screen.getWidth(), (int)screen.getHeight())));
		// return a;
    	return visibility;
    }
    
}
