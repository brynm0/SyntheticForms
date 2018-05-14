import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PVector;

import javax.imageio.ImageIO;
import java.awt.*;
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
    public static int boidCount;
    CurveCollection roads;
    BufferedImage img;
    ArrayList<PVector> attractionLocations;
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
            attractionLocations = readPopulationFromFile(housePath + "attractiontargets.txt", this);
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
            attractionLocations.get(i).mult(scaleFactor);
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
            boids.add(new Boid(1, population[i], 1.5f, .5f, new PVector(0, 0, 1),
                    random(115000, 150000)* scaleFactor, offset + lateralSep[i], this));
        }
        boidCount = boids.size();
        if (OS.equals("Linux"))
        {
            roads = new CurveCollection("/home/bryn/ResearchElective/roads.crv", scaleFactor, this);
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
            System.out.println("Image file failed to load" + e);
        }
//        attractionLocations = new ArrayList<>();
//        System.out.println("Generating attractions");
//        PrintWriter out;
//        try
//        {
//            out = new PrintWriter("attractiontargets.txt");
//            for (Boid b : boids)
//            {
//                PVector temp = (generateAttractionLocation(img));
//                out.println(temp.x + " " + temp.y + " " + temp.z);
//
//            }
//            out.close();
//
//        }
//        catch (IOException e)
//        {
//            System.out.println("Unhandled IO Exception " + e);
//        }
//
//        System.out.println("Done!");
//        paused = true;
    }


    public PVector generateAttractionLocation(BufferedImage image)
    {
        float randomTarget = random(115329.46f);
        float runningCounter = 0;
        for (int i = 0; i < image.getWidth(); i++)
        {
            for (int j = 0; j < image.getHeight(); j++)
            {
                Color col = new Color(img.getRGB(i, j));
                float val = map(255 - col.getRed(), 0, 255, 0, 1);
                runningCounter += val;
                if (runningCounter > randomTarget)
                {
                    PVector target = new PVector(i, j);
                    PVector trans = new PVector(8.0974E6f, -842101.947f);
                    float newScaleFactor = 13323.6957f;
                    target.mult(newScaleFactor);
                    target.add(trans);
                    return target;
                }
            }
        }
        return null;
    }

    public void draw()
    {
        frameNum++;
        background(255);
        strokeWeight(1);
        stroke(255, 0, 0);
        line(0, 0, 0, 150, 0, 0);
        stroke(0, 255, 0);
        line(0, 0, 0, 0, 150, 0);


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
        boidTree = new KDTree(population, 0, this);
        ArrayList<PVector> tempPop = new ArrayList<>(Arrays.asList(population));
        for (int i = 0; i < boids.size(); i++)
        {
            if (!boids.get(i).isFrozen)
            {
                if (population.length > 1)
                {
                    if (attractToMap)
                    {
                        PVector p = new PVector(attractionLocations.get(i).x, -attractionLocations.get(i).y);
                        boids.get(i).seek(p, 200);
                        pushMatrix();
                        translate(p.x, p.y);
                        ellipse(0, 0, 5, 5);
                        popMatrix();
                    }
                    else if (!followNoise)
                    {
                        boids.get(i).flowAlongCurve(roads);
                        boidCount = boids.get(i).attractToCurve(boidCount, roads, roadSep[i], 500 * scaleFactor, 10000*scaleFactor);
                        boids.get(i).cohesionRepulsion(boidTree, boids, tempPop, 0.5f);
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
            //element.draw(10, false);
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