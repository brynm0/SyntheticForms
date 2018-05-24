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
import java.util.HashMap;

public class SynthMain extends PApplet
{
    static boolean drawNeighbours = false;
    static int frameNum = 0;
    boolean saveFrames = true;
    private boolean drawTrails = false;
    private boolean firstPass = true;
    private boolean obnoxious = false;
    private boolean drawGraph = false;
    ArrayList<Graph> nodes;
    ArrayList<ArrayList<Graph>> visited;
    ArrayList<HashMap<PVector, Integer>> vismap;
    ArrayList<ArrayList<PVector>> prevPositions;
    KDTree graphVertexTree;
    HashMap<PVector, Integer> graphMap;
    ArrayList<Integer> lastIndexKept = new ArrayList<>();
    int totalFramesSaved = 0;
    int numTraversed = 0;
    boolean secondPassInit = false;
    ArrayList<Integer[]> boidCurveIndices;
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
    private int boidCount = 150;
    private MeshCollection columnCol, supportCol;

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

    private static ArrayList<ArrayList<PVector>> readCrvs(String absolutePath)
    {
        Charset charset = Charset.forName("US-ASCII");
        String p = "file://" + absolutePath;
        Path file = Paths.get(URI.create(p));

        System.out.println(file);
        ArrayList<ArrayList<PVector>> outList = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, charset))
        {

            String line;
            int currentIndex = -1;
            while ((line = reader.readLine()) != null)
            {
                if (line.contains("object"))
                {
                    currentIndex++;
                    outList.add(new ArrayList<>());
                }
                else
                {
                    String[] vector = line.split(" ");
                    PVector tempVec = new PVector(Float.parseFloat(vector[0]), Float.parseFloat(vector[1]), Float.parseFloat(vector[2]));
                    outList.get(currentIndex).add(tempVec);
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("IOException: " + e);
        }
        return outList;
    }

    public static boolean arrayContains(Object[] array, Object test)
    {
        for (Object o : array)
        {
            if (o.equals(test))
            {
                return true;
            }
        }
        return false;
    }

    /*
     * This method is one that must be used when working with Processing in Intellij
     * Generally only used to declare the size of the window, and the type of renderer being used
     */
    public void settings()
    {
        size(1000, 1000, P3D);
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
        Mesh column, support;
        String currentDirectory;

        if (OS.contains("Windows"))
        {
            currentDirectory = "/Users/evilg/Google%20Drive/Architecture/2018/Semester%201/Synthetic%20Forms/Week%2012/test/";
        }
        else
        {
            currentDirectory = "/home/bryn/SyntheticForms/Chunk/";

        }
        meshlist = Mesh.readMeshes(currentDirectory + "column.obj", this);
        column = meshlist.get(0);
        nodes = Graph.readGraphListFromFile(currentDirectory + "connections.txt", this);
        meshlist = Mesh.readMeshes(currentDirectory + "support.obj", this);
        support = meshlist.get(0);

        //Sometimes the mesh is too large for processing to display, due to near/far clipping plane issues
        //This is especially true if the mesh was modeled to scale in rhino
        // https://stackoverflow.com/questions/4590250/what-is-near-clipping-distance-and-far-clipping-distance-in-3d-graphics

        //I only deal w/ pure triangle meshes
        System.out.println("converting quads to tris");
        column = column.convQuadsToTris();
        support = support.convQuadsToTris();

        System.out.println("scaling meshes");
        column.scale(scaleFactor, new PVector());
        Graph.scaleGraphList(nodes, scaleFactor, new PVector());
        support.scale(scaleFactor, new PVector());

        //List of all the normal vectors of the mesh
        //Used to give the boids an initial normal. Not a great solution
        population = support.populateDownwardFaces(boidCount);

        //Initializing ALL the boids.
        boids = new ArrayList<>();
        boidMap = new HashMap<>();
        System.out.println("populating boids");
        visited = new ArrayList<>();
        vismap = new ArrayList<>();
        prevPositions = new ArrayList<>();
        for (int i = 0; i < population.length; i++)
        {

            Boid newboid = new Boid(random(5, 10), population[i], random(8, 16),
                    random(1f, 3), new PVector(1, 0, 0), random(50, 150), this);
            newboid.velocity = newboid.normal.cross(new PVector(0, 0, 1)).normalize();
            boids.add(newboid);
            boidMap.put(boids.get(i).position, i);
            visited.add(new ArrayList<>());
            vismap.add(new HashMap<PVector, Integer>());
            prevPositions.add(new ArrayList<>());
        }

        //I have some perlin noise populated on the mesh for a behaviour in which the boids follow a noisefield on the mesh
        ArrayList<Mesh> meshes = new ArrayList<>();
        meshes.add(column);
        columnCol = new MeshCollection(meshes, this);
        PVector[] graphVerts = new PVector[nodes.size()];
        graphMap = new HashMap<>();
        System.out.println("generating graph");
        for (int i = 0; i < graphVerts.length; i++)
        {
            graphVerts[i] = nodes.get(i).nodePos.copy();
            graphMap.put(nodes.get(i).nodePos.copy(), i);
        }
        graphVertexTree = new KDTree(graphVerts, 0, this);


        meshes = new ArrayList<>();
        meshes.add(support);
        supportCol = new MeshCollection(meshes, this);

        //Creating a KDTree for the boids, this needs to be updated any time they move.
        //Creating a new KDTree and searching it each time they move is still faster than brute force searching.
        boidTree = new KDTree(population, 0, this);
        System.out.println("beginning main loop");
        System.out.println(boidCount);
    }
int framesUnpaused = 0;
    public void draw()
    {
        frameNum++;
        int modulo = 200;
        if (!firstPass)
        {
            modulo = 60;
        }
        if (!paused)
        {
            framesUnpaused++;
        }
        if (framesUnpaused % modulo == 0 || framesUnpaused == 0)
        {
            if (firstPass)
            {
                drawGraph = true;
            }
            else
            {
                drawGraph = false;
            }
            drawTrails = true;
            drawBoids = false;

        }
        if (obnoxious)
        {
            background(235, 104, 65, 255);
        }
        else
        {
            background(0);
        }
        if (drawMesh)
        {
            fill(255);

            columnCol.drawAllWires(color(255,255,255, 255), 0.5f);
            //supportCol.drawAllWires(0, 1);
        }
        if (drawGraph)
        {
            for (Graph g : nodes)
            {
                g.drawAllConnections(0.5f, color(255,255,255,255));
            }
            strokeWeight(1);
        }
        if (!paused)
        {
            if (firstPass)
            {

                firstPass();
            }
            else
            {
                secondPass();
            }
        }
        if (drawBoids)
        {
            boidDraw();
        }
        if (drawTrails)
        {
            for (ArrayList<PVector> curve : prevPositions)
            {
//            ArrayList<PVector> curve = prevPositions.get(0);
                stroke(0, 160, 176, 255);
                strokeWeight(1);
                if (curve.size() < 4)
                {
                    for (int i = 0; i < curve.size() - 1; i++)
                    {
                        PVector curr = curve.get(i);
                        PVector next = curve.get(i + 1);
                        line(curr.x, curr.y, curr.z, next.x, next.y, next.z);
                    }
                }
                else
                {
                    noFill();
                    beginShape();
                    for (PVector curr : curve)
                    {
                        curveVertex(curr.x, curr.y, curr.z);
                    }
                    endShape();

                }
                strokeWeight(1);
            }
        }
        if (saveFrames && (framesUnpaused % modulo == 0  || framesUnpaused == 0) && !paused)
        {
            totalFramesSaved++;
            System.out.println("saving");
            saveFrame(Integer.toString(totalFramesSaved));
            drawGraph = false;
            drawTrails = false;
            drawBoids = true;
        }
    }

    private void firstPass()
    {
        //Solving behaviours for ALL the boids
        boidTree = new KDTree(population, 0, this);

        for (int i = 0; i < boids.size(); i++)
        {
            Boid curr = boids.get(i);
            ArrayList<PVector> neighbours = boidTree.radiusNeighbours(curr.position, curr.sightOuter);
            if (curr.moves)
            {
                numTraversed = curr.traverseGraph(nodes, vismap, visited, graphMap, i, graphVertexTree, curr.maxVel * 0.9f, numTraversed, 1);
                curr.repelMesh(columnCol, 1);
//                    curr.cohesionRepulsion(neighbours);
                //curr.align(neighbours, boids, population, boidMap);
                boidMap.remove(curr.position);
                curr.integrate();
                prevPositions.get(i).add(curr.position.copy());
                boidMap.put(curr.position, i);
            }
        }
        if (frameNum % 50 == 0)
        {
            System.out.println(numTraversed + "/" + nodes.size());
        }
        if (numTraversed == nodes.size())
        {
            System.out.println("done");
            paused = true;
            firstPass = false;
        }
    }

    private void secondPass()
    {
        if (!secondPassInit)
        {
            System.out.println("Commencing second pass");
            firstPass = false;
            ArrayList<ArrayList<Integer>> fixedPointIndices = new ArrayList<>();
            for (int i = 0; i < prevPositions.size(); i++)
            {
                fixedPointIndices.add(new ArrayList<>());
                for (int j = 0; j < prevPositions.get(i).size(); j++)
                {
                    if (prevPositions.get(i).get(j).dist(graphVertexTree.nearestNeighbor(prevPositions.get(i).get(j))) <= boids.get(i).maxVel * 0.4f)
                    {
                        fixedPointIndices.get(i).add(j);
                    }
                }
                prevPositions.set(i, CurveCollection.divDist(prevPositions.get(i), 25, fixedPointIndices.get(i)));

            }

            ArrayList<PVector> normals = new ArrayList<>();
            ArrayList<PVector> velocities = new ArrayList<>();
            ArrayList<PVector> tempPop = new ArrayList<>();
            ArrayList<Boolean> movesArray = new ArrayList<>();
            boidCurveIndices = new ArrayList<>();
            for (int i = 0; i < prevPositions.size(); i++)
            {
                for (int j = 0; j < prevPositions.get(i).size(); j++)
                {
                    PVector normal;
                    if (j == prevPositions.get(i).size() - 1)
                    {
                        PVector prev = prevPositions.get(i).get(j - 1);
                        PVector prevToCurr = PVector.sub(prevPositions.get(i).get(j), prev);
                        prevToCurr = prevToCurr.normalize();
                        velocities.add(prevToCurr);
                        normal = new PVector(1, 0, 0).cross(prevToCurr);
                    }
                    else if (j == 0)
                    {
                        PVector next = prevPositions.get(i).get(j + 1);
                        PVector currToNext = PVector.sub(next, prevPositions.get(i).get(j));
                        currToNext.normalize();
                        velocities.add(currToNext);
                        normal = new PVector(1, 0, 0).cross(currToNext);
                    }
                    else
                    {
                        PVector prev = prevPositions.get(i).get(j - 1);
                        PVector next = prevPositions.get(i).get(j + 1);
                        PVector cp = SynthMath.lineCP2(prev, next, prevPositions.get(i).get(j));
                        normal = PVector.sub(prevPositions.get(i).get(j), cp);
                        PVector prevToNext = PVector.sub(next, prev);
                        prevToNext.normalize();
                        velocities.add(prevToNext);
                    }
                    normals.add(normal);
                    tempPop.add(prevPositions.get(i).get(j));
                    boidCurveIndices.add(new Integer[]{i, j});
                    if (j == 0 || j == prevPositions.get(i).size() - 1)
                    {
                        movesArray.add(false);
                    }
                    else if (prevPositions.get(i).get(j).dist(graphVertexTree.nearestNeighbor(prevPositions.get(i).get(j))) <= boids.get(i).maxVel * 0.3f)
                    {
                        movesArray.add(false);
                    }
                    else
                    {
                        movesArray.add(true);
                    }
                }
            }
            population = tempPop.toArray(new PVector[0]);
            boids = new ArrayList<>();
            boidMap = new HashMap<>();
            for (int i = 0; i < population.length; i++)
            {
                assert population[i] != null;
                Boid newboid = new Boid(6,
                        population[i].copy(), 5, 1,
                        normals.get(i), 35, this);
                newboid.velocity = velocities.get(i);
                newboid.moves = movesArray.get(i);
                boids.add(newboid);
                boidMap.put(population[i], i);
            }
            System.out.println(boids.size());

            System.out.println("Initialised second pass");
            secondPassInit = true;
        }
        boidTree = new KDTree(population, 0, this);
        PVector[] populationCopy = new PVector[population.length];
        HashMap<PVector, Integer> boidMapCopy = (HashMap<PVector, Integer>) boidMap.clone();
        if (!paused)
        {
            for (int i = 0; i < boids.size(); i++)
            {
                boidMapCopy.remove(boids.get(i).position, i);
                if (boids.get(i).moves)
                {
                    ArrayList<PVector> neighbours = boidTree.radiusNeighbours(boids.get(i).position.copy(), boids.get(i).sightOuter);
                    boids.get(i).straighten(prevPositions.get(boidCurveIndices.get(i)[0]), boidCurveIndices.get(i)[1]);
                    try
                    {
                        boids.get(i).seekCoTanPoint(neighbours, prevPositions, boidMap, boidCurveIndices);
                    }
                    catch (NullPointerException e)
                    {
                        System.out.println("NullPointerException : " + e);
                        saveBoids();
                        paused = true;
                    }
                    boids.get(i).integrate();
                }
                boidMapCopy.put(boids.get(i).position.copy(), i);
                populationCopy[i] = boids.get(i).position.copy();
                prevPositions.get(boidCurveIndices.get(i)[0]).set(boidCurveIndices.get(i)[1], boids.get(i).position.copy());
            }
            boidMap = boidMapCopy;
            population = populationCopy;
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
            savePrevPos(System.currentTimeMillis());
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
        else if (key == 't')
        {
            drawTrails = !drawTrails;
        }
        else if (key == 's')
        {
            saveFrame(Integer.toString(frameNum));
        }
        else if (key == 'o')
        {
            obnoxious = !obnoxious;
        }
        else if (key == 'g')
        {
            drawGraph = !drawGraph;
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

    private void savePrevPos(long stamp)
    {
        ArrayList<Plane> plArray = planeListFromBoidList(boids);
        PrintWriter out, outX, outY, outZ;

        try
        {
            System.out.println("writing positions");
            out = new PrintWriter(stamp + "position" + ".txt");
            for (int i = 0; i < prevPositions.size(); i++)
            {
                out.println("object_" + i);
                for (PVector position : prevPositions.get(i))
                {
                    out.println(position.x + " " + position.y + " " + position.z);
                }
            }
            out.close();
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

