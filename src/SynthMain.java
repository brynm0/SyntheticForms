import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.Stack;

public class SynthMain extends PApplet
{
    OctTree meshVertexTree;
    OctTree boidTree;
    PeasyCam camera;
    Mesh m;
    ArrayList<PVector> population;
    ArrayList<PVector> planeList;
    ArrayList<Boid> boids;
    boolean paused = false;
    float scalar = 300;

    public static void main(String[] args)
    {
        PApplet.main("SynthMain", args);
    }

    public static float bilinearInterp(PVector bottomLeft, PVector topRight, PVector sample,
                                       float f11, float f12, float f22, float f21)
    {
        float fxy1 = f11 * ((topRight.x - sample.x) / (topRight.x - bottomLeft.x)) + f21 * ((sample.x - bottomLeft.x) / (topRight.x - bottomLeft.x));
        float fxy2 = f12 * ((topRight.x - sample.x) / (topRight.x - bottomLeft.x)) + f22 * ((sample.x - bottomLeft.x) / (topRight.x - bottomLeft.x));
        return fxy1 * ((topRight.y - sample.y) / (topRight.y - bottomLeft.y)) + fxy2 * ((sample.y - bottomLeft.y) / (topRight.y - bottomLeft.y));
    }

    public static PVector lerpVector(PVector A, PVector B, float t)
    {
        assert (t <= 1) && (t >= 0);
        return PVector.add(PVector.mult(A, t), PVector.mult(B, 1.0f - t));
    }

    public static PVector mult(PVector A, PVector B)
    {
        return (new PVector(A.x * B.x, A.y * B.y, A.z * B.z));
    }

    public void settings()
    {
        size(1000, 800, P3D);
    }

    public void setup()
    {
        camera = new PeasyCam(this, 50);
        float cameraZ = ((height / 2.0f) / tan(PI * 60.0f / 360.0f));
        population = new ArrayList<>();
        m = Mesh.readMeshes("geo", "1.obj", this).get(0);
        m.scale(100, new PVector());
        m = m.convQuadsToTris();
        Stack<PVector> p = new Stack<>();

        p.addAll(m.vertices);
        meshVertexTree = new OctTree(this, new BBox(new PVector(-50, -50, -50), new PVector(50, 50, 50)), p);
        meshVertexTree.UpdateTree();
        planeList = new ArrayList<>();
        population = m.populate(100, planeList);
        boids = new ArrayList<>();
        for (PVector pop : population)
        {
            boids.add(new Boid(random(0.5f, 2), pop, random(1.5f, 4.5f), random(0.01f, 0.1f), this));
        }
        p = new Stack<>();
        p.addAll(population);
        boidTree = new OctTree(this, new BBox(new PVector(-1000,-1000,-1000), new PVector(10000,1000,1000)), p);
        boidTree.UpdateTree();

    }

    public void draw()
    {
        background(255);
        m.drawWires(0,1);

        if (!paused)
        {

            Stack<PVector> p = new Stack<>();
            p.addAll(population);
            boidTree = new OctTree(this, new BBox(new PVector(-1000,-1000,-1000), new PVector(1000,1000,1000)), p);
            boidTree.UpdateTree();

            for (int i = 0; i < boids.size(); i++)
            {
                population.set(i, boids.get(i).position);

                boids.get(i).cohesion(boidTree, 5, 25);
                boids.get(i).repulsion(boidTree, 5, 50);
                boids.get(i).align(boidTree, boids, population, 5);
                boids.get(i).wander(3f);
                boids.get(i).stickToMesh(m);
                boids.get(i).integrate();
            }
        }
        for (Boid element : boids)
        {
            element.draw(5);

        }
    }

    public void mouseClicked()
    {
        paused = !paused;
    }
}
