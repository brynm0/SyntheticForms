import processing.core.PVector;

public class BSquare
{
    public PVector Min;
    public PVector Max;

    public BSquare(PVector min, PVector max)
    {
        Min = min;
        Max = max;
    }


    public BSquare(float xlow, float ylow, float xhigh, float yhigh)
    {
        Min = new PVector(xlow, ylow, 0);
        Max = new PVector(xhigh, yhigh, 0);
    }

    public BSquare()
    {
        Min = new PVector();
        Max = new PVector();
    }

    public boolean Contains(PVector point)
    {
        if (point.x < Max.x && point.x > Min.x && point.y < Max.y && point.y > Min.y)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean SquareCircleCollision(PVector centre, float radius)
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
