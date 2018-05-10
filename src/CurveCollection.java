import processing.core.PApplet;
import processing.core.PVector;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;

public class CurveCollection
{
    KDTree curvePointTree;
    Hashtable<PVector, int[]> curveIndexTable;
    ArrayList<ArrayList<PVector>> curves;
    PApplet app;
    float scaleFactor;
    public CurveCollection(String absolutePath, float _scaleFactor, PApplet _app)
    {
        app = _app;
        scaleFactor = _scaleFactor;
        curves = readCrvFile(absolutePath, app);
        scaleCurves(scaleFactor);
        removeBadSegments(scaleFactor);
        PVector[] curveVertexList = explodeAllCurves();
        curvePointTree = new KDTree(curveVertexList, 0, app);
        curveIndexTable = createCurveMap(curves);


    }
    void scaleCurves(float scaleFactor)
    {
        for (ArrayList<PVector> list : curves)
        {
            for (PVector vec : list)
            {
                vec.mult(scaleFactor);
            }
        }
    }

    private static ArrayList<ArrayList<PVector>> readCrvFile(String absolutePath, PApplet app)
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

    private static Hashtable<PVector, int[]> createCurveMap(ArrayList<ArrayList<PVector>> curveList)
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
    public PVector[] explodeAllCurves()
    {
        int totalSize = 0;
        for (ArrayList<PVector> p : curves)
        {
            totalSize += p.size();
        }
        PVector[] out = new PVector[totalSize];
        int index = 0;
        for (ArrayList<PVector> p : curves)
        {
            for (PVector vec : p)
            {
                out[index] = vec;
                index++;
            }
        }
        return out;
    }

    public void removeBadSegments(float scaleFactor)
    {
        for (ArrayList<PVector> b : curves)
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

    public void draw()
    {
        for (ArrayList<PVector> b : curves)
        {
            for (int i = 0; i < b.size(); i++)
            {
                if (i != b.size() - 1)
                {
                    if (PVector.dist(b.get(i), b.get(i + 1)) > scaleFactor * 7000)
                    {
                        app.stroke(255, 0, 0);
                    }
                    app.line(b.get(i).x, b.get(i).y, b.get(i + 1).x, b.get(i + 1).y);
                    app.stroke(0);

                }
            }
        }
    }

    public static PVector SegmentClosestPoint(PVector A, PVector B, PVector P)
    {
//        auto AB = B - A;
        PVector AB = PVector.sub(B, A);
//        auto AP = P - A;
        PVector AP = PVector.sub(P, A);
//        float lengthSqrAB = AB.x * AB.x + AB.y * AB.y;
        float lengthSqAB = AB.magSq();
//        float t = (AP.x * AB.x + AP.y * AB.y) / lengthSqrAB;
        float t = (AP.dot(AB)) / lengthSqAB;
        if (t > 1) {t = 1;}
        else if (t < 0) {t = 0;}
        if (t == 0)
        {
            return A;

        }
        if (t == 1)
        {
            return B;

        }
        else
        {
            assert t <= 1 && t >= 0;
            PVector temp = SynthMath.lerpVector(B, A, t);
            return temp;
        }
    }





}
