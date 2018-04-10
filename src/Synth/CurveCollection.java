package Synth;

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

    public CurveCollection(String absolutePath, PApplet _app)
    {

        app = _app;
        curves = readCrvFile(absolutePath, app);
        curveIndexTable = null;
        curvePointTree = null;
    }

    private static ArrayList<ArrayList<PVector>> readCrvFile(String absolutePath, PApplet app)
    {
        Charset charset = Charset.forName("US-ASCII");
        String p = "file://" + absolutePath;
        Path file = Paths.get(URI.create(p));

        System.out.println(file);
        ArrayList<ArrayList<PVector>> outList = new ArrayList<>();
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
                    if (!s[0].equals("") && !s[1].equals("") && !s[2].equals(""))
                    {
                        PVector vec = new PVector(Float.parseFloat(s[0]), Float.parseFloat(s[1]), Float.parseFloat(s[2]));
                        outList.get(groupCurrentIndex).add(vec);
                    }
                }
            }
        }
        catch (IOException e)
        {
            System.err.format("IOException: %s%n", e);
        }
        return outList;
    }


    private Hashtable<PVector, int[]> createCurveMap()
    {
        Hashtable<PVector, int[]> curveMap = new Hashtable<>();
        for (int i = 0; i < curves.size(); i++)
        {
            for (int j = 0; j < curves.get(i).size(); j++)
            {
                PVector key = curves.get(i).get(j);
                int[] value = new int[2];
                value[0] = i;
                value[1] = j;
                curveMap.put(key, value);
            }
        }
        return curveMap;
    }

    private PVector[] explodeAllCurves()
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


    private KDTree createCurveTree()
    {
        PVector[] tempArray = explodeAllCurves();
        return new KDTree(tempArray, 0, app);
    }

    public void move(PVector transform)
    {
        for (ArrayList<PVector> list : curves)
        {
            for (PVector vertex : list)
            {
                vertex.add(transform);
            }
        }
        curveIndexTable = createCurveMap();
        curvePointTree = createCurveTree();
    }

    public void scale(float scaleFactor, PVector scaleOrigin)
    {
        for (ArrayList<PVector> curve : curves)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                PVector scl = PVector.sub(curve.get(i), scaleOrigin);
                scl.mult(scaleFactor);
                curve.set(i, scl);
            }
        }
    }

    public void draw()
    {
        app.noFill();
        app.stroke(0);
        app.strokeWeight(1);
        for (ArrayList<PVector> curveList : curves)
        {
            app.beginShape();
            for (PVector vertex : curveList)
            {
                app.vertex(vertex.x, vertex.y, vertex.z);
            }
            app.endShape();

        }

    }

    public static ArrayList<PVector> divideCurveByLength(ArrayList<PVector> curve, float divisionLength)
    {
        float curveLength = PVector.sub(curve.get(0), curve.get(1)).mag();
        int numDivisions = (int) (curveLength / divisionLength);
        ArrayList<PVector> outlist = new ArrayList<>();
        for (int i = 1; i < numDivisions; i++)
        {
            float param  =  (i * divisionLength) / curveLength;
            assert param >= 0 && param <= 1;
            PVector lerp = PVector.sub(curve.get(0), curve.get(1)).mult(param);
            lerp.add(curve.get(1));
            outlist.add(lerp);
        }
        ArrayList<PVector> temp = new ArrayList<>();
        temp.add(curve.get(1));
        temp.addAll(outlist);
        outlist = temp;
        outlist.add(curve.get(0));
        return outlist;
    }
}
