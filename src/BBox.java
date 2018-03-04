import processing.core.PApplet;
import processing.core.PVector;

public class BBox
{
    public PVector Min;
    public PVector Max;

    public BBox(PVector min, PVector max)
    {
        Min = min;
        Max = max;
    }


    public BBox(float xlow, float ylow, float zlow, float xhigh, float yhigh, float zhigh)
    {
        Min = new PVector(xlow,ylow,zlow);
        Max = new PVector(xhigh,yhigh,xhigh);
    }
    public BBox()
    {
        Min = new PVector();
        Max = new PVector();
    }

    public boolean Contains(PVector point)
    {
        if (point.x < Max.x && point.x > Min.x && point.y < Max.y && point.y > Min.y && point.z < Max.z && point.z > Min.z)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public static BBox FindEnclosingCube(PApplet app, BBox region)
    {
        //we can't guarantee that all bounding regions will be relative to the origin, so to keep the math
        //simple, we're going to translate the existing region to be oriented off the origin and remember the translation.
        //find the min offset from (0,0,0) and translate by it for a short while
        PVector offset = region.Min.mult(-1f);
        region.Min = region.Min.add(offset);
        region.Max = region.Max.add(offset);

        //A 3D rectangle has a length, height, and width. Of those three dimensions, we want to find the largest dimension.
        //the largest dimension will be the minimum dimensions of the cube we're creating.
        int highX = (int)Math.ceil(Math.max(Math.max(region.Max.x, region.Max.y), region.Max.z));

        //see if our cube dimension is already at a power of 2. If it is, we don't have to do any work.
        for (int bit = 0; bit < 32; bit++)
        {
            if (highX == 1 << bit)
            {
                region.Max = new PVector(highX, highX, highX);

                region.Min = region.Min.sub(offset);
                region.Max = region.Min.sub(offset);
                return new BBox(region.Min, region.Max);
            }
        }

        //We have a cube with non-power of two dimensions. We want to find the next highest power of two.
        //example: 63 -> 64; 65 -> 128;
        int x = (highX & 0xff) >> 7;
        region.Max = new PVector(x, x, x);

        region.Min.sub(offset);
        region.Max.sub(offset);

        return new BBox(region.Min, region.Max);
    }

    public boolean BoxSphereCollision(PVector centre, float radius)
    {
        PVector closest = new PVector();
        if (centre.x < Min.x)
        {
            closest.x = Min.x;
        }
        else if (centre.x > Max.x)
        {
            closest.x = Max.x;
        }
        else
        {
            closest.x = centre.x;
        }

        if (centre.y < Min.y)
        {
            closest.y = Min.y;
        }
        else if (centre.y > Max.y)
        {
            closest.y = Max.y;
        }
        else
        {
            closest.y = centre.y;
        }

        if (centre.z < Min.z)
        {
            closest.z = Min.z;
        }
        else if (centre.z > Max.z)
        {
            closest.z = Max.z;
        }
        else
        {
            closest.z = centre.z;
        }

        if (PVector.dist(closest, centre) < radius)
        {
            return true;
        }
        else
        {
            return false;
        }
    }



}
