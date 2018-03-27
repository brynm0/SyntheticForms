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
    public static boolean drawneighbours = false;
    public float scaleFactor = 0.0001f;
    public int boidCount;
    CurveCollection roads;
    private boolean followNoise = false;
    private boolean paused = false;
    private boolean drawCurves = false;
    private boolean reset = false;
    private float x = 0;
    private float y = 0;
    private float z = 0;
    private KDTree boidTree;
    private PeasyCam camera;
    private PVector[] population;
    private ArrayList<Boid> boids;
    private int frameNum = 0;

    public static void main(String[] args)
    {
        PApplet.main("SynthMain", args);
    }

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

    public ArrayList<PVector> readPopulationFromFile(String absolutePath, PApplet app)
    {
        Charset charset = Charset.forName("US-ASCII");
        String p = "file://" + absolutePath;
        Path file = Paths.get(URI.create(p));

        System.out.println(file);
        ArrayList<PVector> outList = new ArrayList<>();
        int currentNum = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, charset))
        {
            String line = "";
            while ((line = reader.readLine()) != null)
            {
                if (line.length() != 0)
                {
                    String[] substrings = line.split(" ");
                    float x = Float.parseFloat(substrings[0]);
                    float y = Float.parseFloat(substrings[1]);
//                    if (currentNum <= boidCount)
//                    {
//                    }
//                    else
//                    {
//                        break;
//                    }
                    outList.add(new PVector(x, y, 0));
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("IOException: " + e);
        }
        return outList;
    }

    public void settings()
    {
        size(1000, 800, P3D);
    }

    public void setup()
    {
        camera = new PeasyCam(this, 500);

        population = readPopulationFromFile("/home/bryn/Downloads/houselocationsinitial.txt", this).toArray(new PVector[0]);

        for (int i = 0; i < population.length; i++)
        {
            population[i].mult(scaleFactor);
        }
        boids = new ArrayList<>();
//        for (PVector point : population)
//        {
//            boids.add(new Boid(1, point, 5, 0.01f, new PVector(0, 0, 1), 500, this));
//        }
        for (int i = 0; i < population.length; i++)
        {
            boids.add(new Boid(1, population[i], 0.75f, 0.1f, new PVector(0, 0, 1),
                    75000 * scaleFactor, 22500 * scaleFactor, this));
        }
        boidCount = population.length;

        String OS = System.getProperty("os.name");
        System.out.println(OS);
        if (OS.equals("Windows 10"))
        {

        }
        else
        {
            roads = new CurveCollection("/home/bryn/Downloads/1.crv", scaleFactor, this);

        }

//        boids.get(0).escapeCurves(curves, curveVertexTree, curveHashTable, 50);

    }


    public void draw()
    {
        frameNum++;
        background(255);
        if (!paused)
        {
            boidLoop();
        }

        if (drawCurves)
        {
            roads.draw();

        }
        ellipse(0, 0, 5, 5);
        boidDraw();
    }


    void boidLoop()
    {

        ArrayList<PVector> tempPop = new ArrayList<>(Arrays.asList(population));
        boidTree = new KDTree(population, 0, this);


        for (int i = 0; i < boids.size(); i++)
        {
            if (!boids.get(i).isFrozen)
            {
                assert boids.get(i).isFrozen == false;
                if (population.length > 1)
                {
                    if (!followNoise)
                    {
                        boids.get(i).flowAlongCurve(roads);
                        //boids.get(i).align(boidTree, boids, tempPop);
                        //boids.get(i).cohesionRepulsion(boidTree, boids, tempPop);
                        boidCount = boids.get(i).attractToCurve(boidCount, roads, boidTree, boids, tempPop);
                    }
                    else
                    {
                        noiseSeed(floor(random(20000)));
                        boids.get(i).wanderNoise();
                    }
                }
                if (!boids.get(i).isFrozen)
                {
                    boids.get(i).integrate();
                }
            }
            population[i] = boids.get(i).position;
        }
        if (frameNum % 10 == 0)
        {
            System.out.println(boidCount);
        }
        if (boidCount == 0)
        {
            paused = true;
            saveBoids();
        }
    }

    void boidDraw()
    {
        for (Boid element : boids)
        {
            element.draw(3, false);
            pushMatrix();
            translate(element.position.x, element.position.y, element.position.z);
            //ellipse(0, 0, 5, 5);
            popMatrix();
        }
    }


    public void keyPressed()
    {
        if (key == 'h')
        {
            drawCurves = !drawCurves;
        }
        else if (key == 'r')
        {
            reset = !reset;
        }
        else if (key == BACKSPACE)
        {
            paused = !paused;
        }
        else if (key == 'w')
        {
            x += 10;
        }
        else if (key == 's')
        {
            x -= 10;
        }
        else if (key == 'a')
        {
            y += 10;
        }
        else if (key == 'd')
        {
            y -= 10;
        }
        else if (key == 'q')
        {
            z += 10;
        }
        else if (key == 'e')
        {
            z -= 10;
        }
        else if (key == ENTER)
        {
            saveBoids();
        }
        else if (key == 'm')
        {
            drawneighbours = !drawneighbours;
        }
        else if (key == 'n')
        {
            followNoise = !followNoise;
        }


    }

    public void saveBoids()
    {
        long fileID = System.currentTimeMillis();
        assert population.length == boids.size();
        ArrayList<Plane> plArray = new ArrayList<>();
        PrintWriter out, outX, outY, outZ;
        for (int i = 0; i < boids.size(); i++)
        {
            PVector x = new PVector();
            boids.get(i).velocity.normalize(x);
            PVector z = new PVector();
            boids.get(i).normal.normalize(z);
            PVector y = x.cross(z).mult(-1);
            Plane temp = new Plane(population[i], x, y, z);
            plArray.add(temp);
        }


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

            PrintWriter outDist = new PrintWriter(fileID + "distance.txt");
            for (Boid b : boids)
            {
                float dist = boidTree.nearestNeighbor(b.position).dist(b.position);
                outDist.println(dist);
            }
            outDist.close();
            System.out.println("done");

        }
        catch (IOException e)
        {
            System.out.println("Unhandled IO Exception " + e);
        }
    }
}

