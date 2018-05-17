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
    boolean saveFrames = false;
    boolean drawTrails = true;
    boolean firstPass = false;
    ArrayList<Graph> nodes;
    ArrayList<ArrayList<Graph>> visited;
    ArrayList<HashMap<PVector, Integer>> vismap;
    ArrayList<ArrayList<PVector>> prevPositions;
    KDTree graphVertexTree;
    HashMap<PVector, Integer> graphMap;
    ArrayList<Integer> lastIndexKept = new ArrayList<>();
    int totalFramesSaved = 0;
    int numTraversed = 0;
    HashMap<PVector, Integer[]> boidMapSecondPass;
    boolean secondPassInit = false;
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
    private int frameNum = 0;
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
        size(1280, 720, P3D);
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
            currentDirectory = "/Users/evilg/Google%20Drive/Architecture/2018/Semester%201/Synthetic%20Forms/Week%2011/Chunk/test/";
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
        paused = true;
        prevPositions = readCrvs(currentDirectory + "crvs.txt");
    }

    public void draw()
    {
        frameNum++;
        background(255);
        if (drawMesh)
        {
            fill(255);
            columnCol.drawAllWires(20, 1);
            supportCol.drawAllWires(0, 1);
            for (Graph g : nodes)
            {
                g.drawAllConnections(1, 127);
            }
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
//            for (int j = 0; j < prevPositions.size(); j++)
//            {
            ArrayList<PVector> curve = prevPositions.get(0);
            stroke(0, 255, 0);
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

//                }
            }
            for (PVector vertex : curve)
            {
                pushMatrix();
                translate(vertex.x, vertex.y, vertex.z);
                noStroke();
                fill(255, 0, 255);
                ellipse(0, 0, 1, 1);

                popMatrix();
            }
            strokeWeight(1);
        }
        if (saveFrames && frameNum % 2 == 0 && !paused)
        {
            totalFramesSaved++;
            saveFrame(Integer.toString(totalFramesSaved));
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
                curr.align(neighbours, boids, population, boidMap);
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
            boidMapSecondPass = new HashMap<>();
            for (int i = 0; i < prevPositions.size(); i++)
            {
                for (int j = 0; j < prevPositions.get(i).size(); j++)
                {
                    PVector normal;
                    if (j == prevPositions.get(i).size() - 1)
                    {
                        PVector prev = prevPositions.get(i).get(j - 1);
                        PVector prevToCurr = PVector.sub(prevPositions.get(i).get(j), prevPositions.get(i).get(j-1));
                        prevToCurr = prevToCurr.normalize();
                        velocities.add(prevToCurr);
                        normal = new PVector(1,0,0).cross(prevToCurr);
                    }
                    else if (j == 0)
                    {
                        PVector next = prevPositions.get(i).get(j+1);
                        PVector currToNext = PVector.sub(next, prevPositions.get(i).get(j));
                        currToNext.normalize();
                        velocities.add(currToNext);
                        normal = new PVector(1,0,0).cross(currToNext);
                    }
                    else
                    {
                        PVector prev = prevPositions.get(i).get(j - 1);
                        PVector next = prevPositions.get(i).get(j+1);
                        PVector prevToNext = PVector.sub()
                    }
                    normals.add(normal);
                    tempPop.add(prevPositions.get(i).get(j));
                    if (fixedPointIndices.get(i).contains(j))
                    {
                        movesArray.add(false);
                    }
                    else
                    {
                        movesArray.add(true);
                    }
                    boidMapSecondPass.put(prevPositions.get(i).get(j), new Integer[]{i, j});
                }
            }
            population = tempPop.toArray(new PVector[0]);
            boids = new ArrayList<>();
            boidMap = new HashMap<>();
            for (int i = 0; i < population.length; i++)
            {
                Boid newboid = new Boid(random(5, 10),
                        population[i], 2, 0.25f,
                        new PVector(1, 0, 0),
                        random(50, 150), this);
                newboid.velocity = newboid.normal.cross(new PVector(0, 0, 1)).normalize();
                newboid.moves = movesArray.get(i);
                boids.add(newboid);
                boidMap.put(boids.get(i).position, i);
            }
            System.out.println(boids.size());

            System.out.println("Initialised second pass");
            secondPassInit = true;
//            drawTrails = false;
        }
//        boidTree = new KDTree(population, 0, this);
//        for (int i = 0; i < boids.size(); i++)
//        {
//            Boid curr = boids.get(i);
//            ArrayList<PVector> neighbours = boidTree.radiusNeighbours(curr.position, curr.sightOuter);
//            if (curr.moves)
//            {
//                curr.repelMesh(columnCol, 1);
//                curr.align(neighbours, boids, population, boidMap);
//                boidMap.remove(curr.position);
//                curr.integrate();
//                boidMap.put(curr.position, i);
//            }
//        }
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
            savePrevPos();
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

    private void savePrevPos()
    {
        ArrayList<Plane> plArray = planeListFromBoidList(boids);
        PrintWriter out, outX, outY, outZ;

        try
        {
            System.out.println("writing positions");
            out = new PrintWriter(fileID + "position" + ".txt");
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

