package Synth;

import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PVector;
import sun.management.BaseOperatingSystemImpl;

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
    private boolean paused = false;
    private boolean drawMesh = false;
    private boolean drawBoids = true;
    private boolean drawEllipse = false;
    //Whether the agents should attract/repel and follow given curves
    private long fileID;
    private float scaleFactor;



    private PVector[] population;
    private KDTree boidTree;
    private HashMap<PVector, Integer> boidMap;
    private ArrayList<Boid> boids;

    private int boidCount = 50;
    private int frameNum = 0;

    private MeshCollection coreCol, columnCol, supportCol;

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
        scaleFactor = 0.225f;

        fileID = System.currentTimeMillis();
        PeasyCam camera = new PeasyCam(this, 500);

        //NOTE(bryn): Declaring a population array for the positions of the Boids
        population = new PVector[boidCount];
        float cameraZ = ((height / 2.0f) / tan(PI * 60.0f / 360.0f));
        perspective(PI / 3, width / height, cameraZ / 100, cameraZ * 100);
        //NOTE(bryn): Since I don't run windows on my machine, I have to do some OS wrangling for different file paths
        //Don't worry about this if you're just using Windows
        String OS = System.getProperty("os.name");
        System.out.println(OS);
        ArrayList<Mesh> meshlist;
        Mesh column, core, support;
        if (OS.equals("Windows 10"))
        {
            String currentDirectory = "/Users/evilg/Google%20Drive/Architecture/2018/Semester%201/Synthetic%20Forms/Week%2011/Chunk/test/";
            meshlist= Mesh.readMeshes(currentDirectory + "column.obj", this);
            column = meshlist.get(0);
            meshlist= Mesh.readMeshes(currentDirectory + "core.obj", this);
            core = meshlist.get(0);
            meshlist= Mesh.readMeshes(currentDirectory + "support.obj", this);
            support = meshlist.get(0);

        }
        else
        {
            String currentDirectory = "/Users/evilg/Google%20Drive/Architecture/2018/Semester%201/Synthetic%20Forms/Week%2011/Chunk/test/";
            meshlist= Mesh.readMeshes(currentDirectory + "column.obj", this);
            column = meshlist.get(0);
            meshlist= Mesh.readMeshes(currentDirectory + "core.obj", this);
            core = meshlist.get(0);
            meshlist= Mesh.readMeshes(currentDirectory + "support.obj", this);
            support = meshlist.get(0);
        }

        //Sometimes the mesh is too large for processing to display, due to near/far clipping plane issues
        //This is especially true if the mesh was modeled to scale in rhino
        // https://stackoverflow.com/questions/4590250/what-is-near-clipping-distance-and-far-clipping-distance-in-3d-graphics

        //I only deal w/ pure triangle meshes
        System.out.println("converting quads to tris");
        column  = column.convQuadsToTris();
        core    = core.convQuadsToTris();
        support = support.convQuadsToTris();

        System.out.println("scaling meshes");
        column.scale(scaleFactor, new PVector());
        core.scale(scaleFactor, new PVector());
        support.scale(scaleFactor, new PVector());

        //List of all the normal vectors of the mesh
        //Used to give the boids an initial normal. Not a great solution
        population = support.populateDownwardFaces(boidCount);

        //Initializing ALL the boids.
        boids = new ArrayList<>();
        boidMap = new HashMap<>();
        System.out.println("populating boids");

        for (int i = 0; i < population.length; i++)
        {

            Boid newboid = new Boid(random(5, 10), population[i], random(1, 6),
                    random(1, 6), new PVector(1, 0, 0), random(50, 150), this);
            newboid.velocity = newboid.normal.cross(new PVector(0,0,1)).normalize();
            boids.add(newboid);
            boidMap.put(boids.get(i).position, i);
        }

        //I have some perlin noise populated on the mesh for a behaviour in which the boids follow a noisefield on the mesh
        ArrayList<Mesh> meshes = new ArrayList<>();
        meshes.add(column);
        columnCol = new MeshCollection(meshes, this);
//        ArrayList<Graph> nodes = Graph.initGraphListFromMeshes(meshCollection, this);

        meshes = new ArrayList<>();
        meshes.add(core);
        coreCol = new MeshCollection(meshes, this);
        meshes = new ArrayList<>();
        meshes.add(support);
        supportCol = new MeshCollection(meshes, this);

        //Creating a KDTree for the boids, this needs to be updated any time they move.
        //Creating a new KDTree and searching it each time they move is still faster than brute force searching.
        boidTree = new KDTree(population, 0, this);

        System.out.println("beginning main loop");
        paused = true;

    }

    public void draw()
    {
        frameNum++;
        background(255);
        if (drawMesh)
        {
            fill(255);
            coreCol.drawAllWires(0, 1);
            supportCol.drawAllWires(0, 1);
        }
        if (!paused)
        {
            boidLoop();
        }
        if (drawBoids)
        {
            boidDraw();
        }
    }


    private void boidLoop()
    {
//        //The twistBoid "wanders" on the mesh, following its noise field
//        twistBoid.followMeshNoiseField(m, meshVertexTree, 1, true);
//        //"integration" simply means updating the velocity with the boid's acceleration, then updating its position with
//        //that velocity (then setting the acceleration to 0)
//        //A bunch of issues that I don't really understand can arise from this.
//        //I'm using semi-implicit Euler integration.
//        //https://gafferongames.com/post/integration_basics/
//        //https://en.wikipedia.org/wiki/Semi-implicit_Euler_method
//        twistBoid.integrate();
//
        //Solving behaviours for ALL the boids
        boidTree = new KDTree(population, 0, this);
        for (int i = 0; i < boids.size(); i++)
        {

            if (population.length > 1)
            {

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
            //saveBoids();
        }
        else if (key == 'm')
        {
            drawNeighbours = !drawNeighbours;
        }
        else if (key == 'b')
        {
            drawBoids = !drawBoids;
        }
        else if (key == 'e')
        {
            drawEllipse = !drawEllipse;
        }
    }

    private void saveBoids()
    {
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

    private ArrayList<Plane> planeListFromBoidList(ArrayList<Boid> boids)
    {
        ArrayList<Plane> plArray = new ArrayList<>();

        for (Boid element : boids)
        {
            PVector x = element.velocity.copy();
            x = x.setMag(1);
            PVector z = element.normal.copy();
            z = z.setMag(1);
            PVector y = x.cross(z).mult(-1);
            y = y.setMag(1);
            Plane temp = new Plane(element.position.copy(), x, y, z);
            plArray.add(temp);
        }
        return plArray;
    }
}

