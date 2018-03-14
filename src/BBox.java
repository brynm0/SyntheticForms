import processing.core.PApplet;
import processing.core.PVector;

public class BBox
{
    public PVector Min;
    public PVector Max;
    public PApplet app;

    public BBox(PVector min, PVector max, PApplet _app)
    {
        Min = min;
        Max = max;
        app = _app;
    }


    public BBox(float xlow, float ylow, float zlow, float xhigh, float yhigh, float zhigh, PApplet _app)
    {
        Min = new PVector(xlow,ylow,zlow);
        Max = new PVector(xhigh,yhigh,zhigh);
        app = _app;
    }
    public BBox(PApplet _app)
    {
        Min = new PVector();
        Max = new PVector();
        app = _app;
    }

    public void draw()
    {
        app.line(Min.x, Min.y, Min.z, Max.x, Min.y, Min.z);
        app.line(Min.x, Min.y, Min.z, Min.x, Max.y, Min.z);
        app.line(Min.x, Min.y, Min.z, Min.x, Min.y, Max.z);

        app.line(Max.x, Max.y, Max.z, Min.x, Max.y, Max.z);
        app.line(Max.x, Max.y, Max.z, Max.x, Min.y, Max.z);
        app.line(Max.x, Max.y, Max.z, Max.x, Max.y, Min.z);

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
