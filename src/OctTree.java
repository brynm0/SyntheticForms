import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Stack;

public class OctTree
{
    private static boolean treeBuilt = false;       //there is no pre-existing tree yet.
    //
    //OctTree usage: construct tree with a stack of points "p," then run updateTree as desired.
    //
    public PApplet app;
    int maxLifeSpan = 8;
    private BBox region;
    private ArrayList<PVector> values;
    private Stack<PVector> pendingInsertion = new Stack<>();
    private OctTree[] m_childNode = new OctTree[8];
    private byte activeChildren = 0;
    private int currLife = -1;
    private OctTree parent;

    public OctTree(PApplet _app)
    {
        values = new ArrayList<>();
        region = new BBox(_app);
        currLife = -1;
    }


    OctTree(PApplet _ap, BBox _region, ArrayList<PVector> _values)
    {
        app = _ap;
        region = _region;
        values = _values;
        currLife = -1;
    }

    OctTree(PApplet _ap, BBox _region, Stack<PVector> _pending)
    {
        app = _ap;
        region = _region;
        pendingInsertion = _pending;
        values = new ArrayList<>();
        currLife = -1;
    }


    public OctTree(BBox _region)
    {
        region = _region;
        values = new ArrayList<>();
        currLife = -1;
    }

    public static BBox getBBox(ArrayList<PVector> positions, PApplet app)
    {
        float xmin = Float.MAX_VALUE;
        float xmax = Float.MIN_VALUE;
        float ymin = Float.MAX_VALUE;
        float ymax = Float.MIN_VALUE;
        float zmin = Float.MAX_VALUE;
        float zmax = Float.MIN_VALUE;
        for (PVector element : positions)
        {
            float oldXMax = xmax;
            if (element.x < xmin)
            {
                xmin = element.x;
            }
            if (element.x > xmax)
            {
                xmax = element.x;
            }

            if (element.y < ymin)
            {
                ymin = element.y;
            }
            if (element.y > ymax)
            {
                ymax = element.y;
            }

            if (element.z < zmin)
            {
                zmin = element.z;
            }
            if (element.z > zmax)
            {
                zmax = element.z;
            }

        }
        float xDist = Math.abs(xmax - xmin);
        float yDist = Math.abs(ymax - ymin);
        float zDist = Math.abs(zmax - zmin);
        int longest = 0;
        if (xDist > yDist && xDist > zDist)
        {
            longest = (int) Math.ceil(xDist);
        }
        else if (yDist > xDist && yDist > zDist)
        {
            longest = (int) Math.ceil(yDist);
        }
        else if (zDist > xDist && zDist > yDist)
        {
            longest = (int) Math.ceil(zDist);
        }
        longest = longest - 1;
        longest |= longest >> 1;
        longest |= longest >> 2;
        longest |= longest >> 4;
        longest |= longest >> 8;
        longest |= longest >> 16;
        longest = longest + 1;
        BBox region = new BBox((xmax + xmin) / 2 - (longest / 2), (ymin + ymax) / 2 - (longest / 2), (zmin + zmax) / 2 - (longest / 2),
                (xmin + xmax) / 2 + longest / 2, (ymin + ymax) / 2 + longest / 2, (zmin + zmax) / 2 + longest / 2, app);

        return region;
    }

    void draw()
    {
        region.draw();
        for (OctTree child : m_childNode)
        {
            if (child != null)
            {
                child.draw();
            }
        }
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
            FindEnclosingCube();
            dimensions = PVector.sub(region.Max, region.Min);
        }

        PVector half = dimensions.div(2.0f);
        PVector centre = PVector.add(region.Min, half);

        BBox[] octant = new BBox[8];
        octant[0] = new BBox(region.Min, centre, app);
        octant[1] = new BBox(new PVector(centre.x, region.Min.y, region.Min.z), new PVector(region.Max.x, centre.y, centre.z), app);
        octant[2] = new BBox(new PVector(centre.x, region.Min.y, centre.z), new PVector(region.Max.x, centre.y, region.Max.z), app);
        octant[3] = new BBox(new PVector(region.Min.x, region.Min.y, centre.z), new PVector(centre.x, centre.y, region.Max.z), app);
        octant[4] = new BBox(new PVector(region.Min.x, centre.y, region.Min.z), new PVector(centre.x, region.Max.y, centre.z), app);
        octant[5] = new BBox(new PVector(centre.x, centre.y, region.Min.z), new PVector(region.Max.x, region.Max.y, centre.z), app);
        octant[6] = new BBox(centre, region.Max, app);
        octant[7] = new BBox(new PVector(region.Min.x, centre.y, centre.z), new PVector(centre.x, region.Max.y, region.Max.z), app);

        ArrayList<PVector>[] octList = new ArrayList[8];
        for (int i = 0; i < 8; i++)
        {
            octList[i] = new ArrayList<>();
        }
        ArrayList<PVector> delist = new ArrayList<>();

        for (PVector point : values)
        {
            for (int a = 0; a < 8; a++)
            {
                if (octant[a].Contains(point))
                {
                    octList[a].add(point);
                    delist.add(point);
                    break;
                }
            }
        }

        values.removeAll(delist);
        for (int i = 0; i < 8; i++)
        {
            if (octList[i].size() != 0)
            {
                m_childNode[i] = CreateNode(octant[i], octList[i]);
                activeChildren |= (byte) (1 << i);
                m_childNode[i].BuildTree();
            }
        }
        treeBuilt = true;
    }

    private OctTree CreateNode(BBox region, ArrayList<PVector> objList)  //complete & tested
    {
        if (objList.size() == 0)
            return null;

        OctTree ret = new OctTree(app, region, objList);
        ret.parent = this;

        return ret;
    }

    private OctTree CreateNode(BBox region, PVector Item)
    {
        ArrayList<PVector> objList = new ArrayList<>(1); //sacrifice potential CPU time for a smaller memory footprint
        objList.add(Item);
        OctTree ret = new OctTree(app, region, objList);
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
        if (dimensions.y <= minSize && dimensions.y <= minSize && dimensions.z <= minSize)
        {
            values.add(item);
            return;
        }

        PVector half = dimensions.div(2.0f);
        PVector centre = PVector.add(region.Min, half);
        BBox[] childOctant = new BBox[8];
        childOctant[0] = (m_childNode[0] != null) ? m_childNode[0].region : new BBox(region.Min, centre, app);
        childOctant[1] = (m_childNode[1] != null) ? m_childNode[1].region : new BBox(new PVector(centre.x, region.Min.y, region.Min.z), new PVector(region.Max.x, centre.y, centre.z), app);
        childOctant[2] = (m_childNode[2] != null) ? m_childNode[2].region : new BBox(new PVector(centre.x, region.Min.y, centre.z), new PVector(region.Max.x, centre.y, region.Max.z), app);
        childOctant[3] = (m_childNode[3] != null) ? m_childNode[3].region : new BBox(new PVector(region.Min.x, region.Min.y, centre.z), new PVector(centre.x, centre.y, region.Max.z), app);
        childOctant[4] = (m_childNode[4] != null) ? m_childNode[4].region : new BBox(new PVector(region.Min.x, centre.y, region.Min.z), new PVector(centre.x, region.Max.y, centre.z), app);
        childOctant[5] = (m_childNode[5] != null) ? m_childNode[5].region : new BBox(new PVector(centre.x, centre.y, region.Min.z), new PVector(region.Max.x, region.Max.y, centre.z), app);
        childOctant[6] = (m_childNode[6] != null) ? m_childNode[6].region : new BBox(centre, region.Max, app);
        childOctant[7] = (m_childNode[7] != null) ? m_childNode[7].region : new BBox(new PVector(region.Min.x, centre.y, centre.z), new PVector(centre.x, region.Max.y, region.Max.z), app);

        if (region.Contains(item))
        {
            boolean found = false;
            for (int a = 0; a < 8; a++)
            {
                if (childOctant[a].Contains(item))
                {
                    if (m_childNode[a] != null)
                    {
                        m_childNode[a].Insert(item);
                    }
                    else
                    {
                        m_childNode[a] = CreateNode(childOctant[a], item);
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

    private void FindEnclosingCube()
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
        if (region.BoxSphereCollision(fromPoint, radius) || depth == 0)
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
            for (OctTree element : m_childNode)
            {
                if (element != null)
                {
                    findings.addAll(element.SearchRadius(radius, fromPoint, depth + 1));
                }
            }
        }
        return findings;
    }
}
