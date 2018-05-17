package Synth;

import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.Hashtable;

public class CurveCollection
{
    KDTree curvePointTree;
    Hashtable<PVector, int[]> curveIndexTable;
    ArrayList<ArrayList<PVector>> curves;
    PApplet app;

    public CurveCollection(ArrayList<ArrayList<PVector>> _curves, PApplet _app)
    {
        curves = _curves;
        app = _app;
        curveIndexTable = createCurveMap();
        curvePointTree = createCurveTree();
    }

    public static ArrayList<PVector> divDist(ArrayList<PVector> inCrv, float targetDist, ArrayList<Integer> fixed)
    {
        ArrayList<PVector> outList = new ArrayList<>();
        float runningDist = 0;
        for (int i = 0; i < inCrv.size(); i++)
        {
            PVector curr = inCrv.get(i);
//            PVector next = null;
            PVector prev = null;
            if (i != 0)
            {
                prev = inCrv.get(i - 1);
            }
//            if (i != inCrv.size() - 1)
//            {
//                next = inCrv.get(i);
//            }
            else
            {

            }
            if (i == 0 || i == inCrv.size() - 1)
            {
                outList.add(curr);
            }
            else if (fixed.contains(i))
            {

                outList.add(curr);
                runningDist = 0;
            }
            else if (prev != null && prev.dist(curr) + runningDist > targetDist)
            {
                float t = (targetDist) / ((prev.dist(curr) - runningDist));
                PVector result = SynthMath.lerpVector(prev, curr, t);
                outList.add(result);
                runningDist = 0;
            }
            else
            {
                runningDist += prev.dist(curr);
            }
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
        curveIndexTable = createCurveMap();
        curvePointTree = createCurveTree();
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
}
