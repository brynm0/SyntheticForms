import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.Stack;

public class QuadTree
{
    private static boolean treeBuilt = false;       //there is no pre-existing tree yet.
    int maxLifeSpan = 8;
    //
    //QuadTree usage: construct tree with a stack of points "p," then run updateTree as desired.
    //
    private PApplet app;
    private BSquare region;
    private ArrayList<PVector> values;
    private Stack<PVector> pendingInsertion = new Stack<>();
    private QuadTree[] m_childNode = new QuadTree[4];
    private byte activeChildren = 0;
    private int currLife = -1;
    private QuadTree parent;

    public QuadTree()
    {
        values = new ArrayList<>();
        region = new BSquare();
        currLife = -1;
    }


    QuadTree(PApplet _ap, BSquare _region, ArrayList<PVector> _values)
    {
        app = _ap;
        region = _region;
        values = _values;
        currLife = -1;
    }

    QuadTree(PApplet _ap, BSquare _region, Stack<PVector> _pending)
    {
        app = _ap;
        region = _region;
        pendingInsertion = _pending;
        values = new ArrayList<>();
        currLife = -1;
    }


    public QuadTree(BSquare _region)
    {
        region = _region;
        values = new ArrayList<>();
        currLife = -1;
    }

    void UpdateTree()
    {
        if (!treeBuilt)
        {
            while (pendingInsertion.size() != 0)
            {
                values.add(pendingInsertion.pop());
            }
            BuildTree();
        }
        else
        {
            while (pendingInsertion.size() != 0)
            {
                Insert(pendingInsertion.pop());
            }
        }
    }

    private void BuildTree()
    {
        if (values.size() <= 1)
        {
            return;
        }
        PVector dimensions = PVector.sub(region.Max, region.Min);
        if (dimensions == new PVector())
        {
            FindEnclosingSquare();
            dimensions = PVector.sub(region.Max, region.Min);
        }

        PVector half = dimensions.div(2.0f);
        PVector centre = PVector.add(region.Min, half);

        BSquare[] quadrant = new BSquare[4];
        quadrant[0] = new BSquare(region.Min, centre);
        quadrant[1] = new BSquare(new PVector(centre.x, region.Min.y), new PVector(region.Max.x, centre.y));
        quadrant[2] = new BSquare(new PVector(region.Min.x, centre.y), new PVector(centre.x, region.Max.y));
        quadrant[3] = new BSquare(centre, region.Max);

        ArrayList<PVector>[] quadList = new ArrayList[4];
        for (int i = 0; i < 4; i++)
        {
            quadList[i] = new ArrayList<>();
        }
        ArrayList<PVector> delist = new ArrayList<>();

        for (PVector point : values)
        {
            for (int a = 0; a < 4; a++)
            {
                if (quadrant[a].Contains(point))
                {
                    quadList[a].add(point);
                    delist.add(point);
                    break;
                }
            }
        }

        values.removeAll(delist);
        for (int i = 0; i < 4; i++)
        {
            if (quadList[i].size() != 0)
            {
                m_childNode[i] = CreateNode(quadrant[i], quadList[i]);
                activeChildren |= (byte) (1 << i);
                m_childNode[i].BuildTree();
            }
        }
        treeBuilt = true;
    }

    private QuadTree CreateNode(BSquare region, ArrayList<PVector> objList)  //complete & tested
    {
        if (objList.size() == 0)
            return null;

        QuadTree ret = new QuadTree(app, region, objList);
        ret.parent = this;

        return ret;
    }

    private QuadTree CreateNode(BSquare region, PVector Item)
    {
        ArrayList<PVector> objList = new ArrayList<>(1); //sacrifice potential CPU time for a smaller memory footprint
        objList.add(Item);
        QuadTree ret = new QuadTree(app, region, objList);
        ret.parent = this;
        return ret;
    }

    private void Insert(PVector item)
    {
        if (values.size() <= 1 && activeChildren == 0)
        {
            values.add(item);
            return;
        }
        PVector dimensions = region.Max.sub(region.Min);
        int minSize = 1;
        if (dimensions.y <= minSize && dimensions.y <= minSize)
        {
            values.add(item);
            return;
        }

        PVector half = dimensions.div(2.0f);
        PVector centre = PVector.add(region.Min, half);
        BSquare[] childQuadrant = new BSquare[4];
        childQuadrant[0] = (m_childNode[0] != null) ? m_childNode[0].region : new BSquare(region.Min, centre);
        childQuadrant[1] = (m_childNode[1] != null) ? m_childNode[1].region : new BSquare(new PVector(centre.x, region.Min.y), new PVector(region.Max.x, centre.y));
        childQuadrant[2] = (m_childNode[2] != null) ? m_childNode[2].region : new BSquare(new PVector(region.Min.x, centre.y), new PVector(centre.x, region.Max.y));
        childQuadrant[3] = (m_childNode[3] != null) ? m_childNode[3].region : new BSquare(centre, region.Max);

        if (region.Contains(item))
        {
            boolean found = false;
            for (int a = 0; a < 4; a++)
            {
                if (childQuadrant[a].Contains(item))
                {
                    if (m_childNode[a] != null)
                    {
                        m_childNode[a].Insert(item);
                    }
                    else
                    {
                        m_childNode[a] = CreateNode(childQuadrant[a], item);
                        activeChildren |= (byte) (1 << a);
                    }
                    found = true;
                }
            }
            if (!found)
            {
                values.add(item);
            }
        }
        else
        {
            BuildTree();
        }

    }

    private void FindEnclosingSquare()
    {
        PVector min = new PVector(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        PVector max = new PVector(Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE);
        for (PVector element : values)
        {
            if (element.x < min.x)
            {
                min.x = element.x;
            }
            else if (element.x > max.x)
            {
                max.x = element.x;
            }
            if (element.y < min.y)
            {
                min.y = element.y;
            }
            else if (element.y > max.y)
            {
                max.y = element.y;
            }
            if (element.z < min.z)
            {
                min.z = element.z;
            }
            else if (element.z > max.z)
            {
                max.z = element.z;
            }

        }
        region.Min = min;
        region.Max = max;
    }

    ArrayList<PVector> SearchRadius(float radius, PVector fromPoint, int depth)
    {
        ArrayList<PVector> findings = new ArrayList<>();
        if (region.SquareCircleCollision(fromPoint, radius) || depth == 0)
        {
            if (values.size() != 0)
            {
                for (PVector element : values)
                {
                    if (PVector.dist(fromPoint, element) < radius && PVector.dist(fromPoint, element) > 0.01f)
                    {
                        findings.add(element);
                    }
                }
            }
        }
        if (activeChildren != 0)
        {
            for (QuadTree element : m_childNode)
            {
                if (element != null)
                {
                    findings.addAll(element.SearchRadius(radius, fromPoint, depth + 1));
                }
            }
        }
        return findings;
    }

    public int totalValueCount(int CurrCount)
    {
        CurrCount += values.size();
        if (activeChildren != 0)
        {
            for (QuadTree element : m_childNode)
            {
                if (element != null)
                {
                    CurrCount = element.totalValueCount(CurrCount);
                }
            }
        }

        return CurrCount;
    }

    private ArrayList<PVector> nearestNRecursion(PVector point, int depth, ArrayList<PVector> currentClosestPts)
    {
        if (depth == 0 || region.SquareCircleCollision(point, PVector.dist(point, currentClosestPts.get(currentClosestPts.size() - 1))))
        {

            if (values.size() != 0)
            {
                for (PVector element : values)
                {
                    if (element != point && !currentClosestPts.contains(element))
                    {
                        for (int i = currentClosestPts.size() - 1; i > -1; i--)
                        {
                            float distanceFromPointToCurrent = PVector.dist(point, currentClosestPts.get(i));
                            if (PVector.dist(point, element) < distanceFromPointToCurrent && (i == 0))
                            {
                                if (!(currentClosestPts.size() == 1))
                                {
                                    currentClosestPts.set(i + 1, currentClosestPts.get(i));
                                }
                                currentClosestPts.set(i, element);
                                break;
                            }
                            else if (PVector.dist(point, element) < distanceFromPointToCurrent
                                    && currentClosestPts.size() > 1 && !(PVector.dist(point, element) < PVector.dist(point, currentClosestPts.get(i - 1))))
                            {
                                if (i != currentClosestPts.size() - 1)
                                {
                                    currentClosestPts.set(i + 1, currentClosestPts.get(i));
                                }
                                currentClosestPts.set(i, element);
                                break;
                            }
                            if (PVector.dist(point, element) < distanceFromPointToCurrent && i != currentClosestPts.size() - 1 && currentClosestPts.size() > 1)
                            {
                                currentClosestPts.set(i + 1, currentClosestPts.get(i));
                                currentClosestPts.set(i, new PVector(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE));
                            }
                        }
                    }
                }
            }
        }
        if (activeChildren != 0)
        {
            for (QuadTree element : m_childNode)
            {
                if (element != null)
                {
                    currentClosestPts = (element.nearestNRecursion(point, depth + 1, currentClosestPts));
                }
            }
        }
        return currentClosestPts;

    }


    ArrayList<PVector> findClosestN(PVector point, int count)
    {
        ArrayList<PVector> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            out.add(new PVector(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE));
        }
        return nearestNRecursion(point, 0, out);
    }

}
