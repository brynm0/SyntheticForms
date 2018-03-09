import processing.core.PVector;
import sun.util.resources.cldr.or.CurrencyNames_or;

public class Plane
{
    PVector origin;
    PVector x, y, z;

    public Plane()
    {
        origin = new PVector();
        x = new PVector(1,0,0);
        y = new PVector(0,1,0);
        z = new PVector(0,0,1);
    }
    public Plane(PVector _origin, PVector _x, PVector _y, PVector _z)
    {
        origin = _origin;
        x = _x.normalize();
        y = _y.normalize();
        z = _z.normalize();
    }

    public Plane(PVector _origin, PVector _z)
    {
        //a x b
        //world z needs to be A
        //local z needs to be B
        PVector worldZ = new PVector(0, 0, 1);
        z = _z;

        if (z != worldZ)
        {
            x = worldZ.cross(z).mult(-1);
            y = x.cross(z).mult(-1);
            x.normalize();
            y.normalize();
            z.normalize();

        }
        else
        {
            x = new PVector(1,0,0);
            y = new PVector(0,1,0);
        }
        origin = _origin;
    }
}
