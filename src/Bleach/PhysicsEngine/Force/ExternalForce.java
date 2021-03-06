package Bleach.PhysicsEngine.Force;

import Bleach.PhysicsEngine.CollisionEngine.CollisionListener;

public class ExternalForce {

    private CollisionListener collisionListener = null;

    private Force force;
    private boolean isExhausted = false;

    public ExternalForce(double vectorAngle, double deltaVelocity) {
	this.force = new Force(vectorAngle, deltaVelocity);
    }

    public CollisionListener getCollisionListener() {
	return collisionListener;
    }

    public double getMagnitude(double deltaTime) {
	if (isExhausted)
	    return Double.MIN_NORMAL;

	double magnitude = force.getMagnitude(deltaTime);

	// Added this feature
	if (force.getVelocity() == Double.MAX_VALUE)
	    return magnitude;

	double newVelocity = force.getVelocity() - magnitude;

	if (newVelocity <= Double.MIN_NORMAL) {
	    magnitude = magnitude - newVelocity;
	    isExhausted = true;
	    force.setVelocity(Double.MIN_NORMAL);
	}

	force.setVelocity(newVelocity);

	return magnitude;
    }

    public double getVectorAngle() {
	return force.getVectorAngle();
    }

    public boolean hasCollisionListener() {
	return this.collisionListener != null ? true : false;
    }

    public boolean isExhaused() {
	return isExhausted;
    }

    public void kill() {
	this.isExhausted = true;
    }

    public void setOnCollision(CollisionListener onCollision) {
	this.collisionListener = onCollision;
    }

    public static enum ForceIdentifier {
	GRAVITY, JUMP, WIND;
    }
}