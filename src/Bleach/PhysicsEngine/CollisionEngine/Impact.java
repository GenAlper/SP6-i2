package Bleach.PhysicsEngine.CollisionEngine;

import java.awt.geom.Point2D;

import Bleach.Entity.Entity;
import Bleach.Entity.EntityTranslatable;

public class Impact {

	public static boolean checkCollision(EntityTranslatable first, EntityTranslatable second) {

		if (((Entity) first).hasRectangularCollisionModel() || ((Entity) second).hasRectangularCollisionModel())
			return rectangularCollisionDetection(first, second);
		return circularCollisionDetection(first, second);
	}

	public static boolean circularCollisionDetection(EntityTranslatable first, EntityTranslatable second) {
		// Closest point on collision box
		Point2D.Double closestPoint = new Point2D.Double(0, 0);

		// Find closest x offset
		if (first.getPosition().x < second.getPosition().x) {
			closestPoint.x = second.getPosition().x;
		} else if (first.getPosition().x > second.getPosition().x + second.getRadius()) {
			closestPoint.x = second.getPosition().x + second.getRadius();
		} else {
			closestPoint.x = first.getPosition().x;
		}

		// Find closest y offset
		if (first.getPosition().y < second.getPosition().y) {
			closestPoint.y = second.getPosition().y;
		} else if (first.getPosition().y > second.getPosition().y + second.getRadius()) {
			closestPoint.y = second.getPosition().y + second.getRadius();
		} else {
			closestPoint.y = first.getPosition().y;
		}

		// If the closest point is inside the circle, the circles have collided
		if (distanceSquared(first.getPosition().x, first.getPosition().y, closestPoint.x, closestPoint.y) < first.getRadius() * 2) {
			return true;
		}
		return false;
	}

	public static boolean collide(EntityTranslatable first, EntityTranslatable second) {
		if (((Entity) first).hasRectangularCollisionModel() || ((Entity) second).hasRectangularCollisionModel()) {
			return collideRectangles(first, second);
		}
		return collideCircles(first, second);
	}

	public static boolean collideCircles(EntityTranslatable first, EntityTranslatable second) {
		Point2D.Double pos = first.getPosition();

		// double distanceBeforeCollision =
		// distanceSquared(first.getPrevPosition().x, first.getPrevPosition().y,
		// second.getPosition().x, second.getPosition().y);
		double distanceBeforeCollision = (first.getRadius() + second.getRadius());

		double reverseAngle = Math.atan2(second.getPosition().y - first.getPrevPosition().y, second.getPosition().x - first.getPrevPosition().x) + Math.PI;
		pos.x += Math.cos(reverseAngle) * distanceBeforeCollision;
		pos.y += Math.sin(reverseAngle) * distanceBeforeCollision;

		first.setPosition(pos);

		return true;
	}

	public static boolean collideRectangles(EntityTranslatable first, EntityTranslatable second) {
		/*
		 * Handle collision with rectangles.
		 */

		boolean didMove = false;

		Point2D.Double prevPos = first.getPrevPosition();
		Point2D.Double newPos = first.getPosition();

		double deltaDistanceXold = prevPos.x - (second.getBoundary().x + second.getBoundary().width / 2.0);
		double deltaDistanceYold = prevPos.y - (second.getBoundary().y + second.getBoundary().height / 2.0);

		if (Math.abs(deltaDistanceYold) >= Math.abs(deltaDistanceXold)) {
			if (deltaDistanceYold < 0) {
				newPos.y = second.getBoundary().y - first.getBoundary().height / 2.0;
				first.startFalling();
				didMove = true;
			} else {
				newPos.y = second.getBoundary().y + second.getBoundary().height + first.getBoundary().height / 2.0;
				didMove = true;
			}
		} else {
			if (deltaDistanceXold < 0) {
				newPos.x = second.getBoundary().x - first.getBoundary().width / 2.0;
				didMove = true;
			} else {
				newPos.x = second.getBoundary().x + second.getBoundary().width + first.getBoundary().width / 2.0;
				didMove = true;
			}
		}

		first.setPosition(newPos);
		return didMove;
	}

	public static double distanceSquared(double x1, double y1, double x2, double y2) {
		double dX = x2 - x1;
		double dY = y2 - y1;
		return dX * dX + dY * dY;
	}

	public static boolean rectangularCollisionDetection(EntityTranslatable first, EntityTranslatable second) {
		// The sides of the rectangles
		double leftA, leftB;
		double rightA, rightB;
		double topA, topB;
		double bottomA, bottomB;

		// Calculate the sides of rect A
		leftA = first.getBoundary().x;
		rightA = first.getBoundary().x + first.getBoundary().width;
		topA = first.getBoundary().y;
		bottomA = first.getBoundary().y + first.getBoundary().height;

		// Calculate the sides of rect B
		leftB = second.getBoundary().x;
		rightB = second.getBoundary().x + second.getBoundary().width;
		topB = second.getBoundary().y;
		bottomB = second.getBoundary().y + second.getBoundary().height;

		// If any of the sides from A are outside of B
		if (bottomA <= topB) {
			return false;
		}

		if (topA >= bottomB) {
			return false;
		}

		if (rightA <= leftB) {
			return false;
		}

		if (leftA >= rightB) {
			return false;
		}

		// If none of the sides from A are outside B
		return true;
	}
}
