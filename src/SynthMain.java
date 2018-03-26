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
    public static boolean drawNeighbours = false;
    private boolean twistDirection = false;
    private boolean paused = false;
    private boolean drawMesh = true;
    private boolean drawBoids = true;
    private boolean drawCurves = true;
    private boolean twist = true;
    Boid twistBoid;
    private KDTree meshVertexTree;
    private KDTree boidTree;
    private PeasyCam camera;
    private Mesh m;
    private PVector[] population;
    private ArrayList<PVector> normalList;
    private ArrayList<Boid> boids;
    private int boidCount = 1250;
    private int frameNum = 0;
   // CurveCollection curves;
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
        ArrayList<ArrayList<PVector>> curveList = new ArrayList<>();
        population = new PVector[boidCount];
        ArrayList<Mesh> meshList;

        String OS = System.getProperty("os.name");
        System.out.println(OS);
        if (OS.equals("Windows 10"))
        {
            String currentDirectory = "/Users/evilg/Google%20Drive/Architecture/2018/Semester%201/Synthetic%20Forms/Week%204b/Textural/Base/5/";
            curveList.add(readCrv(currentDirectory + "1.txt"));
            curveList.add(readCrv(currentDirectory + "2.txt"));
            meshList = Mesh.readMeshes(currentDirectory + "5l.obj", this);
        }
        else
        {
            meshList = Mesh.readMeshes("/home/bryn/BaseMeshes/4.obj", this);
        }
        //curves = new CurveCollection(curveList, this);
        for (Mesh mesh : meshList)
        {
            if (mesh.vertices.size() != 0)
            {
                m = mesh;
                break;
            }
        }
        float scaleFactor = 0.02f;
        m.moveMeshCentreToWorld();
        m.scale(scaleFactor, new PVector());
        m = m.convQuadsToTris();
//        curves.scale(scaleFactor, new PVector());

        PVector[] points = new PVector[m.vertices.size()];
        for (int i = 0; i < m.vertices.size(); i++)
        {
            points[i] = m.vertices.get(i);
        }
        meshVertexTree = new KDTree(points, 0, this);
        normalList = new ArrayList<>();
        population = m.populate(boidCount, normalList);
        boids = new ArrayList<>();
        for (int i = 0; i < population.length; i++)
        {
            Boid newboid = new Boid(random(5, 10), population[i], random(1, 6),
                    random(1, 6), normalList.get(i), random(250, 500), this);
            boids.add(newboid);
        }
        m.popNoise();
        boidTree = new KDTree(population, 0, this);
        setTwistBoid();
        Plane p = new Plane(new PVector(), new PVector(0.5f, 0, 0), new PVector(0, 0.5f, 0), new PVector(0, 0, 0.5f));
        PVector vToOrient = new PVector(1, 1, 1);
        System.out.println(p.changeBasis(vToOrient));
    }

    public void setTwistBoid()
    {
        int randIndex = floor(random(m.vertices.size()));
        PVector randVertex = m.vertices.get(randIndex);
        int normalIndex = (m.faceVerts.indexOf(randIndex));
        normalIndex = m.faceNormals.get(normalIndex);
        float rand = random(0, 1);
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
//            curves.draw();
        }
    }


    void boidLoop()
    {

        ArrayList<PVector> tempPop = new ArrayList<>(Arrays.asList(population));
        boidTree = new KDTree(population, 0, this);

        //twistBoid.position = new PVector();
        twistBoid.followMeshNoiseField(m, meshVertexTree, 1, true);
        twistBoid.integrate();
        //twistBoid.draw(500);

        for (int i = 0; i < boids.size(); i++)
        {
            if (population.length > 1)
            {
                boids.get(i).align(boidTree, boids, tempPop);
                boids.get(i).cohesionRepulsion(boidTree);
                boids.get(i).followMeshNoiseField(m, meshVertexTree, 0.05f, false);
//                boids.get(i).flowAlongCurve(curves, 1f);
//                boids.get(i).attractRepelCurves(curves,0.2f);
                if (twist)
                {
                    boids.get(i).twist(new Plane(twistBoid.position.copy(), twistBoid.normal.copy()), twistDirection);
                }
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


    }

    public void saveBoids()
    {
        long fileID = System.currentTimeMillis();
        assert population.length == normalList.size();
        assert normalList.size() == boids.size();
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

        } catch (IOException e)
        {
            System.out.println("Unhandled IO Exception " + e);
        }
    }

//    void meshCPDebug()
//    {
//        PVector position = new PVector(x, y, z);
//        PVector[] cp = m.closestPointOnMesh(position, meshVertexTree);
//
//        pushMatrix();
//        translate(position.x, position.y, position.z);
//        fill(255, 0, 0);
//        ellipse(0, 0, 25, 25);
//        popMatrix();
//
//        pushMatrix();
//        translate(cp[0].x, cp[0].y, cp[0].z);
//        fill(0, 255, 0);
//        ellipse(0, 0, 25, 25);
//        popMatrix();
//        line(position.x, position.y, position.z, cp[0].x, cp[0].y, cp[0].z);
//    }


}

