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

    public void scale(float scaleFactor, PVector scaleOrigin)
    {
        for (ArrayList<PVector> curve : curves             )
        {
            for (int i = 0; i < curve.size(); i++                 )
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
