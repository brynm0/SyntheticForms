package Synth;

import peasy.PeasyCam;
import processing.core.PApplet;
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

public class SynthMain extends PApplet
{
    public static boolean drawNeighbours = true;
    Boid twistBoid;
    boolean drawEllipse = false;
    private boolean twistDirection = false;
    private boolean paused = false;
    private boolean drawMesh = false;
    private boolean drawBoids = true;
    boolean followCurves = true;
    //Whether the agents should attract/repel and follow given curves
    private boolean twist = false;
    boolean bifurcates = true;
    private KDTree meshVertexTree;
    private KDTree boidTree;
    private PeasyCam camera;
    private Mesh m;
    private ArrayList<PVector> population;
    private ArrayList<PVector> normalList;
    Synth.CurveCollection curveCollection;
    private ArrayList<Boid> boids;
    private int boidCount = 3;
    private int frameNum = 0;
    KDTree prevPositionTree;
    ArrayList<ArrayList<PVector>> prevPositions;
    ArrayList<PVector> prevPositionsFlattened;
    ArrayList<PVector> seekAxes;
    ArrayList<Integer> parents;
    int numFrozen = 0;

    public static void main(String[] args)
    {
        PApplet.main("Synth.SynthMain", args);
    }

    /*
     * returns the index of all occurrences of a specific object in a list.
     * "Object" is the generic base class of all java objects, this will work with any class
     * This is a fairly slow procedure, as it has to run through the whole list every time.
     */
    public static ArrayList<Integer> indexOfAll(Object obj, ArrayList list)
    {
        ArrayList<Integer> indexList = new ArrayList<Integer>();
        for (int i = 0; i < list.size(); i++)
            if (obj.equals(list.get(i)))
            {
                indexList.add(i);
            }
        return indexList;
    }

    public static ArrayList<PVector> readCrv(String absolutePath)
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
        } catch (IOException e)
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
        camera = new PeasyCam(this, 500);
        ArrayList<Mesh> meshList;
        prevPositions = new ArrayList<>();

        //NOTE(bryn): Since I don't run windows on my machine, I have to do some OS wrangling for different file paths
        //Don't worry about this if you're just using Windows
        String OS = System.getProperty("os.name");
        System.out.println(OS);
        if (OS.equals("Windows 10"))
        {
            String currentDirectory = "/Users/evilg/Google%20Drive/Architecture/2018/Semester%201/Synthetic%20Forms/Week%2010/pre/1/";
            ArrayList<ArrayList<PVector>> crvs = new ArrayList<>();
            crvs.add(readCrv(currentDirectory + "1.txt"));
            crvs.add(readCrv(currentDirectory + "2.txt"));
            curveCollection = new CurveCollection(crvs, this);
            meshList = Mesh.readMeshes(currentDirectory + "1.obj", this);
        }
        else
        {
            meshList = Mesh.readMeshes("/home/bryn/Downloads/post124.OBJ", this);
        }

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
        // https://stackoverflow.com/questions/4590250/what-is-near-clipping-distance-and-far-clipping-distance-in-3d-graphics
        float scaleFactor = 0.05f;

        //I only deal w/ pure triangle meshes
        m = m.convQuadsToTris();

        //This moves the centre of the mesh to 0,0,0 - so I don't have to bother with it in Rhino
        PVector translationTarget = m.moveMeshCentreToWorld();
        System.out.println(translationTarget);
        m.scale(scaleFactor, new PVector());
        curveCollection.move(translationTarget);
        curveCollection.scale(scaleFactor, new PVector());

        PVector[] points = new PVector[m.vertices.size()];
        for (int i = 0; i < m.vertices.size(); i++)
        {
            points[i] = m.vertices.get(i);
        }
        //Creating a KDTree for the vertices of the mesh, to make searching faster
        meshVertexTree = new KDTree(points, 0, this);

        population = new ArrayList<>();
        population.add(curveCollection.curves.get(0).get(0).copy());
        population.add(curveCollection.curves.get(1).get(0).copy());
        //Initializing ALL the boids.
        boids = new ArrayList<>();
        seekAxes = new ArrayList<>();
        prevPositionsFlattened = new ArrayList<>();
        parents = new ArrayList<>();
        for (int i = 0; i < population.size(); i++)
        {
            Boid newboid = new Boid(random(5, 10), population.get(i), random(1, 2),
                    random(5, 10), new PVector(1, 0, 0), 850, this);
            boids.add(newboid);
            ArrayList<PVector> temp = new ArrayList<>();
            temp.add(population.get(i).copy());
            prevPositions.add(temp);
            prevPositionsFlattened.add(population.get(i).copy());
            seekAxes.add(new PVector(0, 0, 1));
            parents.add(-1);
        }

        //Creating a KDTree for the boids, this needs to be updated any time they move.
        //Creating a new KDTree and searching it each time they move is still faster than brute force searching.
        boidTree = new KDTree(population.toArray(new PVector[population.size()]), 0, this);
        prevPositionTree = new KDTree(prevPositionsFlattened.toArray(new PVector[prevPositionsFlattened.size()]), 0, this);
        m.popNoise();
    }

    public void draw()
    {

        frameNum++;
        background(255);
        stroke(255, 0, 0);
        line(0, 0, 0, 0, 0, -1000);

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
            curveCollection.draw();
        }
    }

    void boidLoop()
    {
        boidTree = new KDTree(population.toArray(new PVector[population.size()]), 0, this);
        KDTree prevPositionTree = new KDTree(prevPositionsFlattened.toArray(new PVector[prevPositionsFlattened.size()]), 0, this);
        //Solving behaviours for ALL the boids
        int currentBoidsLength = boids.size();
        for (int i = 0; i < currentBoidsLength; i++)
        {
            if (population.size() > 1)
            {
                if (boids.get(i).moves)
                {
                    //This commented out method was an attempt at a draft angle behaviour.
                    //boids.get(i).towardHorizontal(45);

                    //Basic boid behaviours.
                    boids.get(i).cohesionRepulsion(boidTree, boids, population);
                    if (followCurves)
                    {
                        boids.get(i).attractRepelCurves(curveCollection, 0.20f);
                        boids.get(i).flowAlongCurve(curveCollection, 1);
                    }
                    boids.get(i).followMeshNoiseField(m, meshVertexTree, 0.5f, false);
                    boids.get(i).moveInAxis(seekAxes.get(i), 0.5f);
                    boids.get(i).moveInAxis(new PVector(0,0,1), 0.25f);
                    if (random(125) < 1 && bifurcates)
                    {
                        boids.get(i).bifurcate(boidTree, boids, population, prevPositions, prevPositionsFlattened, seekAxes, i, parents);
                    }
                }
            }
            if (boids.get(i).moves)
            {
                boids.get(i).integrate();
                prevPositions.get(i).add(boids.get(i).position.copy());
                prevPositionsFlattened.add(boids.get(i).position.copy());
                //Updating the position array with the new positions of the boids
                population.set(i, boids.get(i).position);
            }
        }
    }

    /*
     *This method goes through the display of the boids.
     */
    void boidDraw()
    {
        for (ArrayList<PVector> curve : prevPositions)
        {
            for (int i = 1; i < curve.size(); i++)
            {
                stroke(0, 255, 0);
                line(curve.get(i).x, curve.get(i).y, curve.get(i).z, curve.get(i - 1).x, curve.get(i - 1).y, curve.get(i - 1).z);
            }
        }
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
        }
        else if (key == ENTER)
        {
            saveBoids();
        }
        else if (key == 'm')
        {
            drawNeighbours = !drawNeighbours;
        }
        else if (key == 'b')
        {
            drawBoids = !drawBoids;
        }
        else if (key == 'B')
        {
            bifurcates = !bifurcates;
        }
        else if (key == 't')
        {
            twist = !twist;
        }
        else if (key == 'e')
        {
            drawEllipse = !drawEllipse;
        }
        else if (key == 'c')
        {
            followCurves = !followCurves;
        }
    }

    public void saveBoids()
    {
        long fileID = System.currentTimeMillis();
        ArrayList<Plane> plArray = new ArrayList<>();
        PrintWriter out;
        try
        {
            System.out.println("writing positions");
            out = new PrintWriter(fileID + "position" + ".txt");
            for (ArrayList<PVector> curve : prevPositions)
            {
                for (int i = 0; i < curve.size(); i++)
                {
                    out.println(curve.get(i).x + " " + curve.get(i).y + " " + curve.get(i).z);
                    if (i == curve.size() - 1)
                    {
                        out.println(" ");
                    }
                }
            }
            out.close();
            System.out.println("done");
        } catch (IOException e)
        {
            System.out.println("Unhandled IO Exception " + e);
        }
    }


}

