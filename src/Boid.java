import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;

import java.util.ArrayList;

public class Boid
{
    public float mass;
    public float maxVel;
    public float maxForce;
    public PVector acceleration;
    public PVector velocity;
    public PVector position;
    public PApplet app;

    public Boid(PApplet _app)
    {
        mass = 1f;
        maxVel = -1f;
        acceleration = new PVector();
        velocity = new PVector();
        position = new PVector();
        app = _app;
    }

    public Boid(float _mass, float _maxVel, float _maxForce, PVector _acceleration, PVector _velocity, PVector _position, PApplet _app)
    {
        mass = _mass;
        maxVel = _maxVel;
        maxForce = _maxForce;
        acceleration = _acceleration;
        velocity = _velocity;
        position = _position;
        app = _app;
    }

    public Boid(float _mass, PVector _position, float _maxVel, float _maxForce, PApplet _app)
    {
        maxVel = _maxVel;
        maxForce = _maxForce;
        mass = _mass;
        position = _position;
        acceleration = new PVector();
        velocity = new PVector();
        app = _app;
    }

    public void addForce(PVector force)
    {
        if (force.mag() > maxForce) force.setMag(maxForce);
        acceleration.add(PVector.div(force, mass));
    }

    public void attract(PVector target)
    {
        PVector desired = PVector.sub(target, position);
        PVector steer = PVector.sub(desired, velocity);
        addForce(steer);
    }

    public void cohesion(OctTree pointsTree, int numNeighbours, float minDist)
    {
        ArrayList<PVector> neighbours = pointsTree.findClosestN(position, numNeighbours);
        //get sum of neighbours pos
        PVector sum = new PVector();
        for (PVector element : neighbours)
        {
            sum.add(element);
        }
        sum.div(neighbours.size());
        if (PVector.dist(sum, position) > minDist) attract(sum);

    }

    public void stickToMesh(Mesh m)
    {

    }

    public void align(OctTree pointsTree, ArrayList<Boid> boidsList, ArrayList<PVector> positionList, int numNeighbours)
    {
        ArrayList<PVector> neighbours = pointsTree.findClosestN(position, numNeighbours);

        //get sum of neighbours pos
        PVector sum = new PVector();
        for (PVector element : neighbours)
        {
            int index  = positionList.indexOf(element);

            sum.add(boidsList.get(index).velocity);
        }
        sum.div(neighbours.size());
        PVector desired = sum.limit(maxVel);

        PVector steer = PVector.sub(desired, velocity);
        steer.mult(0.3f);
        addForce(steer);
    }

    public void wander(float length)
    {
        PVector randVector = PVector.fromAngle(app.random(PConstants.TWO_PI)).setMag(length);
        PVector desired = (PVector.add(randVector, velocity)).limit(maxVel);
        PVector steer = PVector.sub(desired, velocity);
        addForce(steer);
    }

    public void repulsion(OctTree pointsTree, int numNeighbours, float minDist)
    {
        ArrayList<PVector> neighbours = pointsTree.findClosestN(position, numNeighbours);
        //get sum of neighbours pos
        PVector sum = new PVector();
        for (PVector element : neighbours)
        {
            float weight = 100 / PVector.dist(position, element);
            sum.add(PVector.mult(element, weight));
        }
        sum.div(neighbours.size());
        PVector target = PVector.add(position, PVector.sub(position, sum));
        if (PVector.dist(target, position) < minDist)
        {
            PVector desired = PVector.sub(target, position);
            PVector steer = PVector.sub(desired, velocity);
            addForce(steer);
        }


    }


    public void integrate()
    {
        velocity.add(acceleration);
        if (maxVel != -1 && velocity.mag() > maxVel)
        {
            velocity.setMag(maxVel);
        }
        position.add(velocity);
        acceleration = new PVector();
    }

    public void draw(float scalar)
    {
        app.pushMatrix();
        app.translate(position.x, position.y, position.z);
        app.noStroke();
        app.fill(0);
        app.ellipse(0, 0, 5, 5);
        app.stroke(0, 255, 0);
        app.noFill();

        app.line(0, 0, 0, scalar * velocity.x, scalar * velocity.y, scalar * velocity.z);
        app.popMatrix();
    }

}
