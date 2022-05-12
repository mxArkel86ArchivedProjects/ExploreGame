package util;

import java.util.ArrayList;
import java.util.List;

import gameObjects.Collider;
import main.Globals;
import templates.CollisionProps;
import templates.Line;
import templates.Point;
import templates.Rect;

public class CollisionUtil {

	public static boolean LineSphereCollision(Line line, Point sphereCenter, double sphereRadius) {
		Point closestPoint = MathUtil.ClosestPointOnLine(line, sphereCenter);
		double distance = closestPoint.distance(sphereCenter);
		return distance <= sphereRadius;
	}

	public static List<Integer> playerCollisionWithColliders(Point pos, double r, List<Collider> colliders) {
		// Point nextPt = new Point(schemPt.getX() + dx + player_pos_on_screen.getWidth() / 2,
		// 		schemPt.getY() + dy + player_pos_on_screen.getHeight() / 2);
		// Line line = new Line(schemPt, nextPt);

		List<Integer> indeces = new ArrayList<Integer>();
		for (int i = 0;i<colliders.size();i++) {
			Collider c = colliders.get(i);
			boolean collision = LineSphereCollision(c, pos,
					r);
			if(collision)
				indeces.add(i);
		}
		return indeces;
	}

	public static List<Collider> subdivideCollider(Collider c) {
		Point p1 = c.getP1();
		Point p2 = c.getP2();

		double dist = Math.max(Math.abs(p1.getX() - p2.getX()), Math.abs(p1.getY() - p2.getY()));

		List<Collider> new_colliders = new ArrayList<>();
		for (int i = 0; i < dist; i++) {
			int j = i + 1;
			Point o1 = new Point(p1.getX() + (p2.getX() - p1.getX()) * i / dist, p1.getY() + (p2.getY() - p1.getY()) * i / dist);
			Point o2 = new Point(p1.getX() + (p2.getX() - p1.getX()) * j / dist, p1.getY() + (p2.getY() - p1.getY()) * j / dist);
			new_colliders.add(new Collider(o1, o2));
		}
		return new_colliders;
	}

	public static boolean LineIntersectsWithColliders(Line line, List<Collider> colliders) {
		for (Collider collider : colliders) {
			if (LineLineIntersection(line, collider)) {
				return true;
			}
		}
		return false;
	}
	//check if two line segments intersect
	public static boolean LineLineIntersection(Line l1, Line l2) {
		double x1 = l1.getP1().getX();
		double y1 = l1.getP1().getY();
		double x2 = l1.getP2().getX();
		double y2 = l1.getP2().getY();
		double x3 = l2.getP1().getX();
		double y3 = l2.getP1().getY();
		double x4 = l2.getP2().getX();
		double y4 = l2.getP2().getY();
		
		double denom = (x1-x2)*(y3-y4) - (y1-y2)*(x3-x4);
		if (denom == 0) {
			return false;
		}
		
		double xi = ((x3-x4)*(x1*y2-y1*x2)-(x1-x2)*(x3*y4-y3*x4))/denom;
		double yi = ((y3-y4)*(x1*y2-y1*x2)-(y1-y2)*(x3*y4-y3*x4))/denom;
		
		if (xi < Math.min(x1, x2) || xi > Math.max(x1, x2)) {
			return false;
		}
		if (xi < Math.min(x3, x4) || xi > Math.max(x3, x4)) {
			return false;
		}
		if (yi < Math.min(y1, y2) || yi > Math.max(y1, y2)) {
			return false;
		}
		if (yi < Math.min(y3, y4) || yi > Math.max(y3, y4)) {
			return false;
		}
		
		return true;
	}

	
	public static boolean RectRectIntersection(Rect a, Rect b) {
		boolean inline_y_axis = a.left() < b.right() && a.right() > b.left();
		boolean inline_x_axis = a.top() < b.bottom() && a.bottom() > b.top();

		boolean clip_on_left = a.left() < b.right() && a.right() > b.right();
		boolean clip_on_right = a.left() < b.left() && a.right() > b.left();

		boolean clip_on_top = a.top() < b.bottom() && a.bottom() > b.bottom();
		boolean clip_on_bottom = a.top() < b.top() && a.bottom() > b.top();

		boolean collide_x = inline_x_axis && (clip_on_left || clip_on_right || inline_y_axis);
		boolean collide_y = inline_y_axis && (clip_on_top || clip_on_bottom || inline_x_axis);
		return collide_x || collide_y;
	}

	// public static CollisionReturn SchemDynamicCollision(Rect a, Rect b, double dx, double dy) {
	// 	return CollisionRaw(a, b, dx, dy);
	// }

	// private static CollisionReturn CollisionRaw(Rect a, Rect b, double dx, double dy){
	// 	CollisionReturn ret = new CollisionReturn();

	// 	int intent_x = (int)Math.copySign(1, dx);
	// 	if(dx==0)
	// 		intent_x = 0;
	// 	int intent_y = (int)Math.copySign(1, dy);
	// 	if(dy==0)
	// 		intent_y = 0;

	// 	ret.intent_x = intent_x;
	// 	ret.intent_y = intent_y;

	// 	Point top_left = new Point(a.getX(), a.getY());
	// 	Point top_right =  new Point((a.getX()+a.getWidth()), a.getY());
	// 	Point bottom_right =  new Point((a.getX()+a.getWidth()), (a.getY()+a.getHeight()));
	// 	//Point bottom_left =  new Point(a.getX(), (a.getY()+a.getHeight()));

	// 	Point object_bottom_left = new Point(b.getX(), (b.getY()+b.getHeight()));
	// 	Point object_bottom_right = new Point((b.getX()+b.getWidth()), (b.getY()+b.getHeight()));
	// 	Point object_top_left = new Point(b.getX(), b.getY());
	// 	//Point object_top_right =  new Point((b.getX()+b.getWidth()), b.getY());

	// 	boolean left_intersect = top_right.getX() > object_bottom_left.getX() && top_left.getX() < object_bottom_left.getX();
	// 	boolean right_intersect = top_left.getX() < object_bottom_right.getX() && top_right.getX() > object_bottom_right.getX();
	// 	boolean center_intersect_x = top_left.getX() >= object_bottom_left.getX() && top_right.getX() <= object_bottom_right.getX();
	// 	boolean pass_by_x = top_right.getX() <= object_bottom_left.getX() && top_left.getX() + dx >= object_bottom_right.getX();

	// 	boolean top_intersect = bottom_right.getY() > object_top_left.getY() && top_left.getY() < object_top_left.getY();
	// 	boolean bottom_intersect = top_left.getY() < object_bottom_right.getY() && bottom_right.getY() > object_bottom_right.getY();
	// 	boolean center_intersect_y = bottom_right.getY() <= object_bottom_right.getY() && top_left.getY() >= object_top_left.getY();
	// 	boolean pass_by_y = bottom_right.getY() <= object_top_left.getY() && top_left.getY() - dy >= object_bottom_right.getY();

	// 	boolean inline_y = left_intersect || right_intersect || center_intersect_x;
	// 	boolean inline_x = top_intersect || bottom_intersect || center_intersect_y;

	// 	boolean collide_up = (inline_y || pass_by_x) && a.getY() >= (b.getY()+b.getHeight()) && a.getY() - dy < (b.getY()+b.getHeight());
	// 	boolean collide_right = (inline_x || pass_by_y) && (a.getX()+a.getWidth()) <= b.getX() && (a.getX()+a.getWidth()) + dx > b.getX();
	// 	boolean collide_left = (inline_x || pass_by_y) && a.getX() >= (b.getX()+b.getWidth()) && a.getX() + dx < (b.getX()+b.getWidth());
	// 	boolean collide_bottom = (inline_y || pass_by_x) && (a.getY() + a.getHeight()) <= b.getY()
	// 			&& (a.getY() + a.getHeight()) - dy > b.getY();

		
	// 	if(intent_x==1 && collide_right){
	// 		ret.x_collision = true;
	// 		ret.disp_x = Math.abs(b.getX() - (a.getX()+a.getWidth()));
	// 	}else
	// 	if(intent_x==-1 && collide_left){
	// 		ret.x_collision = true;
	// 		ret.disp_x = Math.abs((b.getX()+b.getWidth()) - a.getX());
	// 	}
	// 	if(intent_y==-1 && collide_bottom){
	// 		ret.y_collision = true;
	// 		ret.disp_y = Math.abs((a.getY()+a.getHeight())-b.getY());
	// 	}else
	// 	if(intent_y==1 && collide_up){
	// 		ret.y_collision = true;
	// 		ret.disp_y = Math.abs(a.getY() - (b.getY()+b.getHeight()));
	// 	}
	// 	return ret;
	// }

	// public static CollisionReturn DynamicCollision(Rect a, Rect b, double dx, double dy) {
	// 	return CollisionRaw(a, b, dx, dy);
	// }

	// public static boolean staticCollision(Rect a, Rect b) {
	// 	boolean inline_x = (a.getY() <= (b.getY() + b.getHeight()) && a.getY() >= b.getY())
	// 			|| ((a.getY() + a.getHeight()) <= (b.getY() + b.getHeight()) && (a.getY() + a.getHeight()) >= b.getY())
	// 			|| (a.getY() <= b.getY() && (a.getY() + a.getHeight()) >= (b.getY() + b.getHeight()));
	// 	boolean inline_y = (a.getX() <= (b.getX() + b.getWidth()) && a.getX() >= b.getX())
	// 			|| ((a.getX() + a.getWidth()) <= (b.getX() + b.getWidth()) && (a.getX() + a.getWidth()) >= b.getX())
	// 			|| (a.getX() <= b.getX() && (a.getX() + a.getWidth()) >= (b.getX() + b.getWidth()));
	// 	if (inline_x && inline_y) {
	// 		return true;
	// 	}
	// 	return false;
	// }
}
