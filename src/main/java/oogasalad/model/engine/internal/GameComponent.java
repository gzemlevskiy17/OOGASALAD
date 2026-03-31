package oogasalad.model.engine.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Pose;
import oogasalad.model.engine.api.Vector;

public abstract class GameComponent implements GameObject {
    private Pose pos;
    private Vector velocity;
    private double mass;
    private List<Object> hitboxes;
    private Vector force;

    public GameComponent(Pose pos, double mass) {
      this.pos = pos;
      this.velocity = new Vector(0, 0);
      this.force = new Vector(0, 0);
      this.mass = mass;
      this.hitboxes = new ArrayList<>();
    }
    @Override
    public Pose getPose() {return this.pos;}

    public void setPosition(Pose position) {this.pos = position;}

    @Override
    public Vector getVelocity() {return velocity;}

    public void setVelocity(Vector velocity) {this.velocity = velocity;}

    @Override
    public double getMass() {return mass;}

    public void setMass(double mass) {this.mass = mass;}

    @Override
    public List<Object> getHitboxes() {return Collections.unmodifiableList(hitboxes);}

    protected void addHitbox(Object hitbox) {hitboxes.add(hitbox);}

    @Override
    public Vector getForce(){return force;}

    @Override
    public void addForce(Vector force){this.force = this.force.add(force);}
}
