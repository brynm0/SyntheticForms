import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PVector;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
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
    BufferedImage img;
    private boolean followNoise = false;
    private boolean paused = false;
    private boolean drawCurves = false;
    private boolean reset = false;
    private boolean attractToMap = true;
    private KDTree boidTree;
    private PeasyCam camera;
    private PVector[] population;
    private float[] lateralSep, roadSep;
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

        String OS = System.getProperty("os.name");
        System.out.println(OS);
        camera = new PeasyCam(this, 500);

        if (OS.equals("Linux"))
        {
            String housePath = "/home/bryn/ResearchElective/";
            population = readPopulationFromFile(housePath + "houselocations.txt", this).toArray(new PVector[0]);
            lateralSep = readFloatArrayFromFile(housePath + "latsep.txt");
            roadSep = readFloatArrayFromFile(housePath + "roadsep.txt");
        }
        else
        {
            String housePath = "/Users/evilg/Google%20Drive/Architecture/2018/Semester%201/Research/txts/";
            population = readPopulationFromFile(housePath + "houselocations.txt", this).toArray(new PVector[0]);
            lateralSep = readFloatArrayFromFile(housePath + "latsep.txt");
            roadSep = readFloatArrayFromFile(housePath + "roadsep.txt");
        }
        for (int i = 0; i < population.length; i++)
        {
            population[i].mult(scaleFactor);
            lateralSep[i] *= scaleFactor;
            roadSep[i] *= scaleFactor;
        }
        boids = new ArrayList<>();
        for (int i = 0; i < population.length; i++)
        {
            float offset = 0;
            if (floor(random(100)) == 0)
            {
                offset = random(-15000, 0);
            }
            offset *= scaleFactor;
            boids.add(new Boid(1, population[i], 0.75f, 0.1f, new PVector(0, 0, 1),
                    75000 * scaleFactor, offset + lateralSep[i], this));
        }
        boidCount = population.length;
        if (OS.equals("Linux"))
        {
            roads = new CurveCollection("/home/bryn/ResearchElective/1.crv", scaleFactor, this);
        }
        else
        {
            roads = new CurveCollection("/Users/evilg/Google%20Drive/Architecture/2018/Semester%201/Research/txts/1.crv", scaleFactor, this);
        }


        img = null;
        try
        {
            img = ImageIO.read(new File("1.png"));

        }
        catch (IOException e)
        {
            System.out.println(e);
        }
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

    float[] readFloatArrayFromFile(String absolutePath)
    {
        Charset charset = Charset.forName("US-ASCII");
        String p = "file://" + absolutePath;
        Path file = Paths.get(URI.create(p));

        System.out.println(file);
        ArrayList<Float> outList = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, charset))
        {

            String line;
            while ((line = reader.readLine()) != null)
            {
                outList.add(Float.parseFloat(line));
            }
        }
        catch (IOException e)
        {
            System.err.format("IOException: %s%n", e);
        }
        float[] outArray = new float[outList.size()];
        for (int i = 0; i < outList.size(); i++)
        {
            outArray[i] = outList.get(i);
        }
        return outArray;

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
                    if (attractToMap)
                    {
                        boids.get(i).attractToImage(img);
                    }
                    else if (!followNoise)
                    {
                        boids.get(i).flowAlongCurve(roads);
                        boidCount = boids.get(i).attractToCurve(boidCount, roads, boidTree, boids, tempPop, roadSep[i]);
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
            element.draw(element.sightInner, false);
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
        else if (key == 'q')
        {
            attractToMap = !attractToMap;
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