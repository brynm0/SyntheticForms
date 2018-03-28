package Synth;

import processing.core.PVector;

public class Matrix3D
{
    PVector i;
    PVector j;
    PVector k;

    public Matrix3D(PVector _i, PVector _j, PVector _k)
    {
        i = _i;
        j = _j;
        k = _k;
    }

    public Matrix3D(Plane p)
    {
        i = p.x;
        j = p.y;
        k = p.z;
    }

    public Matrix3D()
    {
        i = new PVector();
        j = new PVector();
        k = new PVector();
    }

    public float determinant()
    {
        Matrix2D temp = new Matrix2D(j.y, k.y, j.z, k.z);
        float term1 = i.x * temp.determinant();

        temp = new Matrix2D(i.x, k.x, i.z, k.z);
        float term2 = j.x * temp.determinant();

        temp = new Matrix2D(i.x, j.x, i.y, j.y);
        float term3 = k.x * temp.determinant();

        return (term1 - term2 + term3);

    }

    public static Matrix3D scale(float scalar, Matrix3D in)
    {
        Matrix3D outMat = new Matrix3D();
        outMat.i = PVector.mult(in.i, scalar);
        outMat.j = PVector.mult(in.j, scalar);
        outMat.k = PVector.mult(in.k, scalar);
        return outMat;
    }

    public Matrix3D invert()
    {
        float det = determinant();
        Matrix3D adj = adjugate();
        adj = scale(1 / det, adj);
        return adj;
    }

    public Matrix3D adjugate()
    {
        return (matrixOfMinors().cofactor().transpose());
    }
    public Matrix3D cofactor()
    {
        Matrix3D out = new Matrix3D();
        out.i.x = i.x;
        out.i.y = i.y * -1;
        out.i.z = i.z;
        out.j.x = j.x * -1;
        out.j.y = j.y;
        out.j.z = j.z * -1;
        out.k.x = k.x;
        out.k.y = k.y * -1;
        out.k.z = k.z;
        return out;
    }

    public Matrix3D matrixOfMinors()
    {
        Matrix2D[] temp = new Matrix2D[9];
        float a = i.x;
        float b = j.x;
        float c = k.x;
        float d = i.y;
        float e = j.y;
        float f = k.y;
        float g = i.z;
        float h = j.z;
        float ii = k.z;

        temp[0] = new Matrix2D(e, f, h, ii);
        temp[1] = new Matrix2D(b, c, h, ii);
        temp[2] = new Matrix2D(b, c, e, f);
        temp[3] = new Matrix2D(d, f, g, ii);
        temp[4] = new Matrix2D(a, c, g, ii);
        temp[5] = new Matrix2D(a, c, d, f);
        temp[6] = new Matrix2D(d, e, g, h);
        temp[7] = new Matrix2D(a, b, g, h);
        temp[8] = new Matrix2D(a, b, d, e);

        Matrix3D out = new Matrix3D();
        out.i.x = temp[0].determinant();
        out.i.y = temp[1].determinant();
        out.i.z = temp[2].determinant();

        out.j.x = temp[3].determinant();
        out.j.y = temp[4].determinant();
        out.j.z = temp[5].determinant();

        out.k.x = temp[6].determinant();
        out.k.y = temp[7].determinant();
        out.k.z = temp[8].determinant();

        return out;
    }

    public static PVector mult(Matrix3D m, PVector v)
    {
        float x = m.i.x * v.x + m.j.x * v.x + m.k.x * v.x;
        float y = m.i.y * v.y + m.j.y * v.y + m.k.y * v.y;
        float z = m.i.z * v.z + m.j.z * v.z + m.k.z * v.z;

        return new PVector(x,y,z);
    }

    public Matrix3D transpose()
    {
        Matrix3D outMat = new Matrix3D();
        outMat.i.x = i.x;
        outMat.i.y = j.x;
        outMat.i.z = k.x;

        outMat.j.x = i.y;
        outMat.j.y = j.y;
        outMat.j.z = k.y;

        outMat.k.x = i.z;
        outMat.k.y = j.z;
        outMat.k.z = k.z;
        return outMat;
    }


}
