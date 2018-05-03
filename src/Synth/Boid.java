package Synth;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;

import java.util.ArrayList;

public class Boid
{
    public PVector velocity;
    public PVector position;
    public PVector normal = new PVector(0, 0, 1);
    boolean moves = true;
    float sightInner;
    float sightOuter;
    private float mass;
    private float maxVel;
    private float maxForce;
    private PVector acceleration;
    private PApplet app;

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

    public Boid(float _mass, PVector _position, float _maxVel, float _maxForce, PVector n, float _sightOuter, PApplet _app)
    {
        maxVel = _maxVel;
        maxForce = _maxForce;
        mass = _mass;
        position = _position;
        acceleration = new PVector();
        velocity = new PVector();
        app = _app;
        normal = n;
        sightOuter = _sightOuter;
        sightInner = app.random(0.4f * sightOuter, 0.75f * sightOuter);
    }

    public void addForce(PVector force)
    {
        if (force.mag() > maxForce && maxForce != -1)
        {
            force.setMag(maxForce);
        }
        acceleration.add(PVector.div(force, mass));
    }

    private void addSteer(PVector desired)
    {
        PVector steer = PVector.sub(desired, velocity);
        addForce(steer);
    }

    public void wander(float weight)
    {
        PVector rand = new PVector(app.random(-1, 1), app.random(-1, 1), app.random(0, 1));
        rand.setMag(maxForce);
        PVector desired = PVector.add(rand, velocity);
        addSteer(desired);
    }

    public void moveInAxis(PVector axis, float weight)
    {
        PVector desired = axis.copy();
        desired.setMag(maxVel);
        addSteer(desired);
    }

    public void keepInsideMesh(Mesh m)
    {
        if (!m.insideMesh(position, new PVector(1, 0, 0)))
        {
            moves = false;
        }
    }

    public void flowAlongCurve(CurveCollection c, float weight)
    {
        PVector cp = c.curvePointTree.nearestNeighbor(position);
        int[] indices = c.curveIndexTable.get(cp);
        PVector next;
        if (c.curves.get(indices[0]).size() != indices[1] + 1)
        {
            next = c.curves.get(indices[0]).get(indices[1] + 1);
            PVector desired = PVector.sub(next, cp);
            desired.setMag(maxVel);
            addSteer(PVector.mult(desired, weight));
        }
    }

    public void attractRepelCurves(CurveCollection c, float weight)
    {
        float curveSightOuter = sightOuter * weight;
        float curveSightInner = sightInner * weight;
        ArrayList<PVector> closestPoints = c.curvePointTree.radiusNeighbours(position, curveSightOuter);
        int attractioncount = 0;
        PVector desiredRepulsion = new PVector();
        PVector desiredAttraction = new PVector();
        for (PVector element : closestPoints)
        {
            if (element.dist(position) < curveSightInner)
            {
                if (SynthMain.drawNeighbours)
                {
                    app.stroke(255, 0, 0);
                    app.line(position.x, position.y, position.z, element.x, element.y, element.z);
                }
                //Contribute to repulsion total
                PVector desired = PVector.sub(position, element);
                desired.mult(1 / position.dist(element));
                desiredRepulsion.add(desired);
            }
            else
            {
                if (SynthMain.drawNeighbours)
                {

                    app.stroke(0, 0, 255);
                    app.line(position.x, position.y, position.z, element.x, element.y, element.z);
                }
                //Contribute to attraction total;
                desiredAttraction.add(element);
                attractioncount++;
            }
        }
        if (attractioncount != 0)
        {
            desiredAttraction.div(attractioncount);

        }
        if (!desiredRepulsion.equals(new PVector()))
        {
            PVector steerAway = PVector.sub(desiredRepulsion, velocity);

            addForce(steerAway);
        }
        if (!desiredAttraction.equals(new PVector()))
        {
            desiredAttraction = PVector.sub(desiredAttraction, position);

            PVector steerToward = PVector.sub(desiredAttraction, velocity);
            addForce(steerToward);

        }
    }

    public void seekCommonPlane(KDTree pointsTree, ArrayList<PVector> population, ArrayList<Boid> boids)
    {
        ArrayList<PVector> neighbours = pointsTree.radiusNeighbours(position, sightOuter);
        if (neighbours.size() == 0)
        {
            return;
        }
        PVector originAverage = new PVector();
        PVector normalAverage = new PVector();
        for (PVector neighbour : neighbours)
        {
            Boid b = boids.get(population.indexOf(neighbour));
            float t = app.map(position.dist(b.position), 0, sightOuter, 1, 0);
            originAverage.add(SynthMath.lerpVector(position, b.position, t));
            normalAverage.add(SynthMath.lerpVector(normal, b.normal, t));
        }
        originAverage.div(neighbours.size());
        normalAverage.div(neighbours.size());
        if (!originAverage.equals(new PVector()))
        {
            Plane p = new Plane(originAverage, normalAverage);
            PVector planeCp = p.cpOnPlane(position);
            app.stroke(0, 255, 0);
            app.line(position.x, position.y, position.z, planeCp.x, planeCp.y, planeCp.z);
            PVector desired = PVector.sub(p.cpOnPlane(position), position);
            desired.limit(maxVel);
            addSteer(desired);
        }
    }

    public void twist(Plane twistingPlane, boolean direction)
    {
        //cross the vector that points to the plane w the planes normal
        PVector agentToPlane = PVector.sub(twistingPlane.origin, position);
        PVector twist = agentToPlane.cross(twistingPlane.z);
        if (direction)
        {
            twist.mult(-1);
        }

        twist.setMag(maxVel);
        float dist = PVector.dist(twistingPlane.origin, position);
        twist.mult(1000 / (dist));
        addSteer(twist);
    }

    public void repelMesh(Mesh m, KDTree meshVertexTree, float weight)
    {
        PVector[] cp = m.closestPointOnMesh(position, meshVertexTree);
        PVector tempAccel = PVector.sub(position, cp[0]);
        tempAccel.normalize();
        tempAccel.mult(weight);
        acceleration.add(tempAccel);
    }

    public void followMeshNoiseField(Mesh m, KDTree meshVertexTree, float weight, boolean follow)
    {

        final PVector[] cp = m.closestPointOnMesh(position, meshVertexTree);
        this.normal = cp[1].copy();
        //Directly add some acceleration that is in the direction of the target, proportional to the distance
        PVector tempAccel = PVector.sub(cp[0], position);
        tempAccel.normalize();
        tempAccel.mult(weight);
        acceleration.add(tempAccel);

        if (follow)
        {
            PVector tex = cp[2].copy();

            tex.setMag(maxVel);
            PVector steer = PVector.sub(tex, velocity);

            addForce(steer);
        }


    }

    public void towardHorizontal(float degreesAngle)
    {
        float floor = (float) Math.cos(Math.toRadians(degreesAngle));
        float dot = new PVector(0, 0, 1).dot((velocity.copy().normalize()));

        if (dot < floor)
        {
            float t = app.map(dot, -1, floor, 1, 0);
            PVector scaledTarget = new PVector(0, 1, 1).normalize().mult(velocity.mag());
            scaledTarget.mult(t);
            addSteer(scaledTarget);
        }
    }


    public void align(KDTree pointsTree, ArrayList<Boid> boidsList, ArrayList<PVector> positionList)
    {
        ArrayList<PVector> neighbours = pointsTree.radiusNeighbours(position, sightOuter);
        if (neighbours.size() != 0)
        {
            //get sum of neighbours velocity
            PVector sum = new PVector();
            for (PVector element : neighbours)
            {
                int index = positionList.indexOf(element);
                sum.add(boidsList.get(index).velocity);
            }
            if (!sum.equals(new PVector()))
            {
                sum.div(neighbours.size());
                addSteer(sum);
            }
        }

    }

    public void wanderOnMesh(float r, Mesh m)
    {
        PVector randVector = PVector.fromAngle((app.random(PConstants.TWO_PI)));
        randVector.setMag(r);
        Plane currentPlane = new Plane(position, normal);
        PVector transformedRandVector = (PVector.mult(currentPlane.x, randVector.x));
        transformedRandVector.add(PVector.mult(currentPlane.y, randVector.y));
        transformedRandVector.add(PVector.mult(currentPlane.z, randVector.z));
        PVector desired = (PVector.add(randVector, velocity));
        PVector steer = PVector.sub(desired, velocity);
        addForce(steer);
    }

    public void cohesionRepulsion(KDTree pointsTree, ArrayList<Boid> boids, ArrayList<PVector> population)
    {
        int attractioncount = 0;
        ArrayList<PVector> neighbours = pointsTree.radiusNeighbours(position, this.sightOuter);
        PVector desiredRepulsion = new PVector();
        PVector desiredAttraction = new PVector();
        for (PVector element : neighbours)
        {
            if (element.dist(position) < this.sightInner && population.indexOf(element) != -1 && boids.get(population.indexOf(element)).moves)
            {
                if (SynthMain.drawNeighbours)
                {
                    app.stroke(255, 0, 0);
                    app.line(position.x, position.y, position.z, element.x, element.y, element.z);
                }
                //Contribute to repulsion total
                PVector desired = PVector.sub(position, element);
                desired.mult(1 / position.dist(element));
                desiredRepulsion.add(desired);
            }
            else if (population.indexOf(element) != -1 && boids.get(population.indexOf(element)).moves)
            {
                if (SynthMain.drawNeighbours)
                {

                    app.stroke(0, 0, 255);
                    app.line(position.x, position.y, position.z, element.x, element.y, element.z);
                }

                //Contribute to attraction total;
                desiredAttraction.add(element);
                attractioncount++;
            }
        }
        if (attractioncount != 0)
        {
            desiredAttraction.div(attractioncount);

        }
        if (!desiredAttraction.equals(new PVector()))
        {
            desiredAttraction = PVector.sub(desiredAttraction, position);
            PVector steerToward = PVector.sub(desiredAttraction, velocity);
            addForce(steerToward);

        }
        if (!desiredRepulsion.equals(new PVector()))
        {
            PVector steerAway = PVector.sub(desiredRepulsion, velocity);
            addForce(steerAway);
        }
    }

    public void bifurcate(KDTree boidTree, ArrayList<Boid> boids, ArrayList<PVector> prevPop,
                          ArrayList<ArrayList<PVector>> prevPositions, ArrayList<PVector> prevPositionsFlattened,
                          ArrayList<PVector> seekAxes, int currentIndex,
                          ArrayList<Integer> parents)
    {
        if (boidTree.nearestNeighbor(position).dist(this.position) > sightOuter * 0.8f && moves) ;
        {
            Boid tempBoid = new Boid(mass, position.copy(), maxVel, maxForce, normal.copy(), sightOuter, app);
            boids.add(tempBoid);
            prevPop.add(tempBoid.position.copy());
            ArrayList<PVector> templist = new ArrayList<>();
            templist.add(this.position.copy());
            prevPositionsFlattened.add(this.position.copy());
            prevPositions.add(templist);
            parents.add(currentIndex);
            PVector rotateAround = PVector.fromAngle(app.random(PConstants.TWO_PI));
            float angle = PConstants.PI - app.random(11 * PConstants.PI) / 36;
            PVector axisToSeek = SynthMath.rotate(seekAxes.get(currentIndex).copy(), rotateAround, angle);
            if (axisToSeek.z < 0)
            {
                axisToSeek = SynthMath.rotate(seekAxes.get(currentIndex).copy(), rotateAround, -angle);
            }
            seekAxes.add(axisToSeek);
        }
    }

    public void join(KDTree prevPositionTree, ArrayList<ArrayList<PVector>> prevPositions, int index, ArrayList<Integer> parents)
    {
        ArrayList<PVector> tmp = new ArrayList<>();
        tmp.addAll(prevPositions.get(index));
        int parentIndex = parents.get(index);
        if (parentIndex != -1)
        {
            tmp.addAll(prevPositions.get(parents.get(index)));
        }
        PVector nearest = prevPositionTree.nearestNotInList(position, tmp);
        if (nearest.dist(position) < 0.5)
        {
            prevPositions.get(index).add(nearest);
            moves = false;
        }
    }

    public void repulsion(KDTree pointsTree, float radius)
    {
        ArrayList<PVector> neighbours = pointsTree.radiusNeighbours(position, radius);

        if (neighbours.size() != 0)
        {
//            for (PVector neighbour : neighbours)
//            {
//                app.line(position.x, position.y, position.z, neighbour.x, neighbour.y, neighbour.z);
//            }
//

            //get sum of neighbours pos
            PVector steer;
            PVector desired = new PVector();
            for (PVector element : neighbours)
            {
                desired.add(PVector.mult(PVector.sub(element, position), radius / element.dist(position)));

            }
            desired.div(neighbours.size());
            desired.mult(-1);

            steer = PVector.sub(desired, velocity);

            addForce(steer);

            // if something is closer, its contribution to the sum ought to be greater

        }

    }


    public void integrate()
    {
        PVector oldvel = velocity.copy();
        acceleration.limit(maxForce);
        velocity.add(acceleration);
        if (maxVel > 0)
        {
            velocity.limit(maxVel);
        }
        normal.normalize();
        normal.add(PVector.sub(velocity, oldvel));
        position.add(velocity);
        acceleration = new PVector();
    }

    public void draw(float scalar, boolean drawSight)
    {
        app.pushMatrix();
        app.translate(position.x, position.y, position.z);

        app.stroke(255, 0, 0);
        app.noFill();
        //Synth.Plane pl = new Synth.Plane(new PVector(0, 0, 0), normal, velocity);
        PVector v = velocity.copy();
        v.normalize();
        app.line(0, 0, 0, scalar * v.x, scalar * v.y, scalar * v.z);

        app.popMatrix();
        if (drawSight)
        {
            app.pushMatrix();
            app.translate(position.x, position.y, position.z);
            app.ellipse(0, 0, sightOuter, sightOuter);
            app.stroke(0, 255, 0);
            app.ellipse(0, 0, sightInner, sightInner);
            app.popMatrix();
        }

    }

}
