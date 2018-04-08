package Synth;

import processing.core.PVector;

public class Plane
{
    PVector origin;
    PVector x, y, z;

    public Plane()
    {
        origin = new PVector();
        x = new PVector(1, 0, 0);
        y = new PVector(0, 1, 0);
        z = new PVector(0, 0, 1);
    }
    public Plane(PVector _origin, PVector _z, PVector _x)
    {
        //a x b
        //world z needs to be A
        //local z needs to be B
        PVector worldZ = new PVector(0, 0, 1);
        z = _z.copy();
        x = _x.copy();

        if (z != worldZ)
        {
            y = x.cross(z).mult(-1);


        }
        else
        {
            y = x.cross(z).mult(-1);
        }
        origin = _origin;
    }


    public Plane(PVector _origin, PVector _x, PVector _y, PVector _z)
    {
        origin = _origin;
        x = _x.copy();
        y = _y.copy();
        z = _z.copy();

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


        }
        else
        {
            x = new PVector(1, 0, 0);
            y = new PVector(0, 1, 0);
        }
        origin = _origin;
    }

    public PVector changeBasis(PVector v)
    {
        Matrix3D planeMatrix = new Matrix3D(this);
        planeMatrix = planeMatrix.invert();
        PVector result = Matrix3D.mult(planeMatrix, v);
        return result;
    }

    public PVector orient(PVector v)
    {
        PVector out = v.copy();
        out.x = this.x.x * v.x + this.x.y * v.x + this.x.z * v.x;
        out.y = this.y.x * v.y + this.y.y * v.y + this.y.z * v.y;
        out.z = this.z.x * v.z + this.z.y * v.z + this.z.z * v.z;
        out.add(this.origin);
        assert out.x != Float.NaN;


        return out;
    }


    public PVector cpOnPlane(PVector p)
    {
        // p' = p - (n ⋅ (p - o)) * n
        return PVector.sub(p, PVector.mult( z, PVector.dot(z, PVector.sub(p, origin))));
    }


}
