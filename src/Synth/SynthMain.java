package Synth;

import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SynthMain extends PApplet
{
    static boolean drawNeighbours = false;
    private boolean twistDirection = false;
    private boolean paused = false;
    private boolean drawMesh = false;
    private boolean drawBoids = true;
    private boolean drawCurves = true;
    private boolean drawEllipse = false;
    //Whether the agents should attract/repel and follow given curves
    private boolean interactCurves = true;
    private boolean twist = false;
    private long fileID;
    float scaleFactor;


    private Boid twistBoid;

    private KDTree meshVertexTree;
    private Mesh m;
    Mesh componentA, componentB;

    private PVector[] population;
    private ArrayList<PVector> normalList;
    private KDTree boidTree;
    private HashMap<PVector, Integer> boidMap;
    private ArrayList<Boid> boids;

    private int boidCount = 15000;
    private int frameNum = 0;

    private CurveCollection curves;
    private MeshCollection meshCollection;

    public static void main(String[] args)
    {
        PApplet.main("Synth.SynthMain", args);
    }

    /*
     * returns the index of all occurrences of a specific object in a list.
     * "Object" is the generic base class of all java objects, this will work with any class
     * This is a fairly slow procedure, as it has to run through the whole list every time.
     */
    static ArrayList<Integer> indexOfAll(Object obj, ArrayList list)
    {
        ArrayList<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++)
        {
            if (obj.equals(list.get(i)))
            {
                indexList.add(i);
            }
        }
        return indexList;
    }

    /*
     * I've got my curves coming in as polylines in a text document
     * Each line of the document is one point on the curve
     * e.g.
     * 2.1 3.5 7.2
     * 29.2 1 0
     * 0 53.9 23.7
     * might be a simple 3 point polyline
     */
    private static ArrayList<PVector> readCrv(String absolutePath)
    {
        Charset charset = Charset.forName("US-ASCII");
        String p = "file://" + absolutePath;
        Path file = Paths.get(URI.create(p));

        System.out.println(file);
        ArrayList<PVector> outList = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, charset))
        {

            String line;
            while ((line = reader.readLine()) != null)
            {
                String[] vector = line.split(" ");
                PVector tempVec = new PVector(Float.parseFloat(vector[0]), Float.parseFloat(vector[1]), Float.parseFloat(vector[2]));
                outList.add(tempVec);
            }
        }
        catch (IOException e)
        {
            System.out.println("IOException: " + e);
        }
        return outList;
    }

    /*
     * This method is one that must be used when working with Processing in Intellij
     * Generally only used to declare the size of the window, and the type of renderer being used
     */
    public void settings()
    {
        size(1000, 800, P3D);
    }

    public void setup()
    {
        fileID = System.currentTimeMillis();
        PeasyCam camera = new PeasyCam(this, 500);
        ArrayList<ArrayList<PVector>> curveList = new ArrayList<>();

        //NOTE(bryn): Declaring a population array for the positions of the Boids
        population = new PVector[boidCount];
        ArrayList<Mesh> meshList;
        float cameraZ = ((height / 2.0f) / tan(PI * 60.0f / 360.0f));
        perspective(PI / 3, width / height, cameraZ / 100, cameraZ * 100);
        //NOTE(bryn): Since I don't run windows on my machine, I have to do some OS wrangling for different file paths
        //Don't worry about this if you're just using Windows
        String OS = System.getProperty("os.name");
        System.out.println(OS);
        if (OS.equals("Windows 10"))
        {
            String currentDirectory = args[0];
            curveList.add(readCrv(currentDirectory + "1.txt"));
            curveList.add(readCrv(currentDirectory + "2.txt"));
            meshList = Mesh.readMeshes(currentDirectory + "chunkbase.obj", this);
        }
        else
        {
            meshList = Mesh.readMeshes("/home/bryn/BaseMeshes/4.obj", this);
        }

        //CurveCollection is my class for a collection of curves, it includes a few helpful data structures for
        //working with large numbers of curves, probably not necessary for this project.
        curves = new CurveCollection(curveList, this);
        for (Mesh mesh : meshList)
        {
            //Since a lot of programs export objs with a null, or empty, polygroup, here I'm just setting the
            //First polygroup with vertices in it to be the active one.
            if (mesh.vertices.size() != 0)
            {
                m = mesh;
                break;
            }
        }
        //Sometimes the mesh is too large for processing to display, due to near/far clipping plane issues
        //This is especially true if the mesh was modeled to scale in rhino
        // https://stackoverflow.com/questions/4590250/what-is-near-clipping-distance-and-far-clipping-distance-in-3d-graphics
        scaleFactor = 0.5f;

        //I only deal w/ pure triangle meshes
        m = m.convQuadsToTris();

        //This moves the centre of the mesh to 0,0,0 - so I don't have to bother with it in Rhino
        PVector translationTarget = m.moveMeshCentreToWorld();
        try
        {
            PrintWriter out = new PrintWriter(fileID + "translationTarget.txt");
            out.println(translationTarget.x + ", " + translationTarget.y + ", " + translationTarget.z);
            out.close();
            out = new PrintWriter(fileID + "scaleFactor.txt");
            out.println(1 / scaleFactor);
            out.close();
        }
        catch (Exception e)
        {
            System.out.println("aaaa");
        }

        curves.move(translationTarget);
        m.scale(scaleFactor, new PVector());
        curves.scale(scaleFactor, new PVector());

        PVector[] points = new PVector[m.vertices.size()];
        for (int i = 0; i < m.vertices.size(); i++)
        {
            points[i] = m.vertices.get(i);
        }
        //Creating a KDTree for the vertices of the mesh, to make searching faster
        meshVertexTree = new KDTree(points, 0, this);

        //List of all the normal vectors of the mesh
        //Used to give the boids an initial normal. Not a great solution
        normalList = new ArrayList<>();
        population = m.populate(boidCount, normalList);

        //Initializing ALL the boids.
        boids = new ArrayList<>();
        boidMap = new HashMap<>();
        for (int i = 0; i < population.length; i++)
        {

            Boid newboid = new Boid(random(5, 10), population[i], random(1, 6),
                    random(1, 6), normalList.get(i), random(50, 150), this);
            boids.add(newboid);
            boidMap.put(boids.get(i).position, i);
        }

        //I have some perlin noise populated on the mesh for a behaviour in which the boids follow a noisefield on the mesh
        m.popNoise();
        ArrayList<Mesh> meshes = new ArrayList<>();
        meshes.add(m);
        meshCollection = new MeshCollection(meshes, this);

        //Creating a KDTree for the boids, this needs to be updated any time they move.
        //Creating a new KDTree and searching it each time they move is still faster than brute force searching.
        boidTree = new KDTree(population, 0, this);

        //The twist boid is a special type of boid that the other boids "orbit" around
        setTwistBoid();
    }

    /*
     * This method initialises or resets the twistBoid
     */
    private void setTwistBoid()
    {
        int randIndex = floor(random(m.vertices.size()));
        PVector randVertex = m.vertices.get(randIndex);
        //TODO(bryn): We can use a HashTable lookup now, no need to be using indexOf()
        int normalIndex = (m.faceVerts.indexOf(randIndex));
        normalIndex = m.faceNormals.get(normalIndex);
        PVector randNormal = m.normals.get(normalIndex).copy();
        if (random(1) < 0.5)
        {
            twistDirection = !twistDirection;
        }
        twistBoid = new Boid(1, randVertex.copy(), 5, 5, randNormal, 500, this);
    }

    public void draw()
    {
        frameNum++;
        background(255);
        if (drawMesh)
        {
            fill(255);
            m.drawWires(0, 1);
        }
        if (!paused)
        {
            boidLoop();
        }
        if (drawBoids)
        {
            boidDraw();
        }
        if (drawCurves)
        {
            curves.draw();
        }
    }


    private void boidLoop()
    {
        ArrayList<PVector> tempPop = new ArrayList<>(Arrays.asList(population));
        //The twistBoid "wanders" on the mesh, following its noise field
        twistBoid.followMeshNoiseField(m, meshVertexTree, 1, true);
        //"integration" simply means updating the velocity with the boid's acceleration, then updating its position with
        //that velocity (then setting the acceleration to 0)
        //A bunch of issues that I don't really understand can arise from this.
        //I'm using semi-implicit Euler integration.
        //https://gafferongames.com/post/integration_basics/
        //https://en.wikipedia.org/wiki/Semi-implicit_Euler_method
        twistBoid.integrate();

        //Solving behaviours for ALL the boids
        for (int i = 0; i < boids.size(); i++)
        {

            if (population.length > 1)
            {
                //This commented out method was an attempt at a draft angle behaviour.
                //boids.get(i).towardHorizontal(45);

                //Basic boid behaviours.
                boids.get(i).align(boidTree, boids, population, boidMap);
                boids.get(i).cohesionRepulsion(boidTree);

                //This method also "sticks" the boids to the base mesh.
                //There is a  "weight" variable that determines how much they are able to leave the mesh before being
                //pulled back in
                boids.get(i).attractToMesh(meshCollection, 1);
                if (interactCurves)
                {
                    boids.get(i).flowAlongCurve(curves, 1f);
                    boids.get(i).attractRepelCurves(curves, 0.2f);
                }
                if (twist)
                {
                    boids.get(i).twist(new Plane(twistBoid.position.copy(), twistBoid.normal.copy()), twistDirection);
                }
            }
            boidMap.remove(boids.get(i).position);
            boids.get(i).integrate();
            boidMap.put(boids.get(i).position, i);

            //Updating the position array with the new positions of the boids
        }
    }

    /*
     *This method goes through the display of the boids.
     */
    private void boidDraw()
    {
        for (Boid element : boids)
        {
            element.draw(10, false);
            pushMatrix();
            translate(element.position.x, element.position.y, element.position.z);
            if (drawEllipse)
            {
                ellipse(0, 0, 50, 50);
            }
            popMatrix();
        }
        if (twist)
        {
            pushMatrix();
            translate(twistBoid.position.x, twistBoid.position.y, twistBoid.position.z);
            noStroke();
            fill(127, 255, 255, 200);
            ellipse(0, 0, 100, 100);
            noFill();
            popMatrix();
        }
    }

    /*
     * These are some simple keybindings that I use to control the application
     * Pressing "enter" will save the boids positions etc to disk
     */
    public void keyPressed()
    {
        if (key == 'h')
        {
            drawMesh = !drawMesh;
        }
        else if (key == BACKSPACE)
        {
            paused = !paused;
            if (paused)
            {
                System.out.println("Paused");
            }
            else
            {
                System.out.println("Unpaused");
            }
        }
        else if (key == ENTER)
        {
            saveBoids();
//            saveMeshes();
        }
        else if (key == 'u')
        {
            setTwistBoid();
        }
        else if (key == 'm')
        {
            drawNeighbours = !drawNeighbours;
        }
        else if (key == 'b')
        {
            drawBoids = !drawBoids;
        }
        else if (key == 'c')
        {
            drawCurves = !drawCurves;
        }
        else if (key == 't')
        {
            twist = !twist;
        }
        else if (key == 'e')
        {
            drawEllipse = !drawEllipse;
        }
        else if (key == 'C')
        {
            interactCurves = !interactCurves;
        }
    }

    private void saveBoids()
    {
        assert population.length == normalList.size();
        assert normalList.size() == boids.size();
        ArrayList<Plane> plArray = planeListFromBoidList(boids);
        PrintWriter out, outX, outY, outZ;

        try
        {
            System.out.println("writing positions");
            out = new PrintWriter(fileID + "position" + ".txt");
            for (PVector position : population)
            {
                out.println(position.x + ", " + position.y + ", " + position.z);
            }
            out.close();
            System.out.println("done");

            System.out.println("writing planes");
            outX = new PrintWriter(fileID + "planeX" + ".txt");
            for (Plane p : plArray)
            {
                outX.println(p.x.x + ", " + p.x.y + ", " + p.x.z);
            }
            outX.close();

            outY = new PrintWriter(fileID + "planeY" + ".txt");
            for (Plane p : plArray)
            {
                outY.println(p.y.x + ", " + p.y.y + ", " + p.y.z);
            }
            outY.close();

            outZ = new PrintWriter(fileID + "planeZ" + ".txt");
            for (Plane p : plArray)
            {
                outZ.println(p.z.x + ", " + p.z.y + ", " + p.z.z);
            }
            outZ.close();

            PrintWriter outDist = new PrintWriter(fileID + "nearest.txt");
            for (Boid b : boids)
            {
                PVector nearest = boidTree.nearestNeighbor(b.position);
                outDist.println(nearest.x + ", " + nearest.y + ", " + nearest.z);
            }
            outDist.close();
            System.out.println("done");

        }
        catch (IOException e)
        {
            System.out.println("Unhandled IO Exception " + e);
        }
    }

    private void saveMeshes()
    {
        ArrayList<Plane> plArray = planeListFromBoidList(boids);
        long fileID = System.currentTimeMillis();
        System.out.println("Working on saving obj");
        try
        {
            PrintWriter p = new PrintWriter(fileID + "out.obj");
            p.println("# Mesh produced by custom script");
            p.println("# Bryn Murrell 2018");

            Mesh outMesh = null;
            //Loop through group
            for (int i = 0; i < boids.size(); i++)
            {
                if (i % 2500 == 0 && i != 0)
                {
                    p.close();
                    fileID = System.currentTimeMillis();
                    System.out.println(i + "/" + boids.size());
                    p = new PrintWriter(fileID + "out.obj");
                    p.println("# Mesh produced by custom script");
                    p.println("# Bryn Murrell 2018");
                }
                float t = new PVector(1, 0, 0).dot(boids.get(i).normal);
                t = map(t, -1, 1, 0, 1);
                outMesh = Mesh.tween2Meshes(componentA, componentB, t);
                outMesh.moveMeshCentreToWorld();
                //outMesh.scale(1 / scaleFactor, new PVector());
                outMesh = Mesh.orientMeshOnPlane(plArray.get(i), outMesh);
                p.println("o object_" + i);
                for (PVector vert : outMesh.vertices)
                {
                    p.println("v " + vert.x + " " + vert.y + " " + vert.z + " ");
                }
                for (PVector norm : outMesh.normals)
                {
                    p.println("vn " + norm.x + " " + norm.y + " " + norm.z + " ");
                }
                for (int j = 0; j < outMesh.faceVerts.size() / 4; j++)
                {
                    int currentIndex = 4 * j;
                    String s = "f ";
                    s += (1 + (outMesh.faceVerts.get(currentIndex))     + (i * outMesh.vertices.size())) + "//" + (1 + (outMesh.faceNormals.get(currentIndex))     + (i * outMesh.vertices.size())) + " ";
                    s += (1 + (outMesh.faceVerts.get(currentIndex + 1)) + (i * outMesh.vertices.size())) + "//" + (1 + (outMesh.faceNormals.get(currentIndex + 1)) + (i * outMesh.vertices.size())) + " ";
                    s += (1 + (outMesh.faceVerts.get(currentIndex + 2)) + (i * outMesh.vertices.size())) + "//" + (1 + (outMesh.faceNormals.get(currentIndex + 2)) + (i * outMesh.vertices.size())) + " ";
                    p.println(s);
                }
            }
            p.close();

        }

        catch (IOException e)
        {
            System.out.println("Unhandled IO Exception " + e);
        }
        finally
        {
            System.out.println("done");

        }


    }

    private ArrayList<Plane> planeListFromBoidList(ArrayList<Boid> boids)
    {
        ArrayList<Plane> plArray = new ArrayList<>();

        for (int i = 0; i < boids.size(); i++)
        {
            if (boids.get(i).velocity.mag() == 0)
            {
                boids.get(i).velocity = new PVector(1,0,0);
            }
            PVector x = boids.get(i).velocity.normalize().copy();
            PVector z = boids.get(i).normal.normalize().copy();
            PVector y = x.cross(z).mult(-1);
            Plane temp = new Plane(boids.get(i).position.copy(), x, y, z);
            plArray.add(temp);
        }
        return plArray;
    }
}

