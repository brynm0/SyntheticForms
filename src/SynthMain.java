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
import java.util.Hashtable;

public class SynthMain extends PApplet
{
    public static boolean drawneighbours = false;
    public float scaleFactor = 0.0001f;
    ArrayList<ArrayList<PVector>> curves = new ArrayList<>();
    private boolean paused = false;
    private boolean drawCurves = true;
    private boolean reset = false;
    private float x = 0;
    private float y = 0;
    private float z = 0;
    private KDTree curveVertexTree;
    private KDTree boidTree;
    private PeasyCam camera;
    private PVector[] population;
    private ArrayList<Boid> boids;
    private int boidCount = 1;
    private int frameNum = 0;
    Hashtable<PVector, int[]> curveHashTable;

    public static ArrayList<ArrayList<PVector>> readCrvFile(String absolutePath, PApplet app)
    {
        Charset charset = Charset.forName("US-ASCII");
        String p = "file://" + absolutePath;
        Path file = Paths.get(URI.create(p));

        System.out.println(file);
        ArrayList<ArrayList<PVector>> outList = new ArrayList<>();
        ArrayList<ArrayList<PVector>> realOutList = new ArrayList<>();
        int groupCurrentIndex = -1;
        try (BufferedReader reader = Files.newBufferedReader(file, charset))
        {

            boolean zeroHasOccurred = false;
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.contains("object_0"))
                {
                    if (!zeroHasOccurred)
                    {
                        zeroHasOccurred = true;
                        groupCurrentIndex++;
                        ArrayList<PVector> extremelyTempList = new ArrayList<>();
                        outList.add(extremelyTempList);
                    }
                    else
                    {
                        break;
                    }
                }
                else if (line.contains("object_"))
                {
                    groupCurrentIndex++;
                    ArrayList<PVector> extremelyTempList = new ArrayList<>();
                    outList.add(extremelyTempList);

                }
                else
                {
                    String[] s = line.split(" ");
                    if (!s[0].equals("") && !s[1].equals(""))
                    {
                        PVector vec = new PVector(Float.parseFloat(s[0]), Float.parseFloat(s[1]));
                        outList.get(groupCurrentIndex).add(vec);

                    }
                }
            }
        }
        catch (IOException e)
        {
            System.err.format("IOException: %s%n", e);
        }
        finally
        {
            for (int i = 0; i < outList.size(); i++)
            {

                for (int j = 0; j < outList.size(); j++)
                {
                    if (i != j && outList.get(j).size() == 2)
                    {
                        assert !outList.get(i).contains(new PVector());
                        boolean b = outList.get(i).get(outList.get(i).size() - 1).equals(outList.get(j).get(0));
                        if (b)
                        {
                            outList.get(i).addAll(outList.get(j));
                        }
                    }
                }
                if (outList.get(i).size() != 2)
                {
                    realOutList.add(outList.get(i));
                }
            }
        }
        return realOutList;
    }

    public static void main(String[] args)
    {
        PApplet.main("SynthMain", args);
    }

    public static Hashtable<PVector, int[]> createCurveMap(ArrayList<ArrayList<PVector>> curveList)
    {
        Hashtable<PVector, int[]> curveMap = new Hashtable<>();
        for (int i = 0; i < curveList.size(); i++)
        {
            for (int j = 0; j < curveList.get(i).size(); j++)
            {
                PVector key = curveList.get(i).get(j);
                int[] value = new int[2];
                value[0] = i;
                value[1] = j;
                curveMap.put(key, value);
            }
        }
        return curveMap;
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

    public void settings()
    {
        size(1000, 800, P3D);
    }

    public void setup()
    {
        camera = new PeasyCam(this, 500);

        population = new PVector[boidCount];
        population[0] = new PVector(1000,0,0);
        boids = new ArrayList<>();
        for (PVector point : population             )
        {
            boids.add(new Boid(1, point, 5, 0.01f, new PVector(0,0,1), 500, this));
        }
        String OS = System.getProperty("os.name");
        System.out.println(OS);
        if (OS.equals("Windows 10"))
        {

        }
        else
        {
            curves = readCrvFile("/home/bryn/Downloads/1.crv", this);
        }
        scaleCurves(curves, scaleFactor);
        removeBadSegments(curves);
        PVector[] curveVertexList = explodeAllCurves(curves);
        curveVertexTree = new KDTree(curveVertexList, 0, this);
        curveHashTable = createCurveMap(curves);

//        boids.get(0).escapeCurves(curves, curveVertexTree, curveHashTable, 50);

    }

    public PVector[] explodeAllCurves(ArrayList<ArrayList<PVector>> list)
    {
        int totalSize = 0;
        for (ArrayList<PVector> p : list)
        {
            totalSize += p.size();
        }
        PVector[] out = new PVector[totalSize];
        int index = 0;
        for (ArrayList<PVector> p : list)
        {
            for (PVector vec : p)
            {
                out[index] = vec;
                index++;
            }
        }
        return out;
    }

    public void removeBadSegments(ArrayList<ArrayList<PVector>> A)
    {
        for (ArrayList<PVector> b : A)
        {
            for (int i = 0; i < b.size(); i++)
            {
                if (i != b.size() - 1)
                {
                    if (PVector.dist(b.get(i), b.get(i + 1)) > scaleFactor * 7000)
                    {
                        b.remove(i + 1);
                        b.remove(i);
                        i--;
                        i--;
                    }
                }
            }
        }

    }

    public void drawAllCurves(ArrayList<ArrayList<PVector>> A)
    {
        for (ArrayList<PVector> b : A)
        {
            for (int i = 0; i < b.size(); i++)
            {
                if (i != b.size() - 1)
                {
                    if (PVector.dist(b.get(i), b.get(i + 1)) > scaleFactor * 7000)
                    {
                        stroke(255, 0, 0);
                    }
                    line(b.get(i).x, b.get(i).y, b.get(i + 1).x, b.get(i + 1).y);
                    stroke(0);

                }
            }
        }
    }

    public void draw()
    {
        frameNum++;
        background(255);
        if (!paused)
        {
//            boidLoop();
            boids.get(0).flowAlongCurve(curveVertexTree, curveHashTable, curves);
            boids.get(0).integrate();
        }

        //drawAllCurves(curves);
        ellipse(0,0,5,5);
        boidDraw();
    }

    void scaleCurves(ArrayList<ArrayList<PVector>> A, float scaleFactor)
    {
        for (ArrayList<PVector> list : A)
        {
            for (PVector vec : list)
            {
                vec.mult(scaleFactor);
            }
        }
    }


    void boidLoop()
    {

        ArrayList<PVector> tempPop = new ArrayList<>(Arrays.asList(population));
        boidTree = new KDTree(population, 0, this);


        for (int i = 0; i < boids.size(); i++)
        {
            if (population.length > 1)
            {
                boids.get(i).align(boidTree, boids, tempPop);
                boids.get(i).cohesionRepulsion(boidTree);
            }
            boids.get(i).integrate();

            population[i] = boids.get(i).position;
        }
    }

    void boidDraw()
    {
        for (Boid element : boids)
        {
            element.draw(50, false);
            pushMatrix();
            translate(element.position.x, element.position.y, element.position.z);
            ellipse(0, 0, 50, 50);
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

