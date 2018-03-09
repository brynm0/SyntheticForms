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
        float term1 = i.x * j.y * k.z;
        float term2 = j.x * k.y * i.z;
        float term3 = k.x * i.y * j.z;
        float term4 = k.x * j.y * i.z;
        float term5 = j.x * i.y * k.z;
        float term6 = i.x * k.y * j.z;
        return (1.0f / (term1 + term2 + term3 - term4 - term5 - term6));

    }

    public static Matrix3D scale(float scalar, Matrix3D in)
    {
        Matrix3D outMat = new Matrix3D();
        outMat.i = PVector.mult(in.i, scalar);
        outMat.j = PVector.mult(in.j, scalar);
        outMat.k = PVector.mult(in.k, scalar);
        return outMat;
    }

    public static PVector changeOfBasis3D(Matrix3D newBasis, PVector vecToTransform)
    {
        Matrix3D invertedBasis = newBasis.invert();
        PVector outputVec = new PVector();
        outputVec.x = vecToTransform.x * invertedBasis.i.x + vecToTransform.y * invertedBasis.j.x + vecToTransform.z * invertedBasis.k.x;
        outputVec.y = vecToTransform.x * invertedBasis.i.y + vecToTransform.y * invertedBasis.j.y + vecToTransform.z * invertedBasis.k.y;
        outputVec.z = vecToTransform.x * invertedBasis.i.z + vecToTransform.y * invertedBasis.j.z + vecToTransform.z * invertedBasis.k.y;
        return outputVec;
    }

    public Matrix3D invert()
    {
        float det = determinant();
        Matrix3D adj = adjugate();
        adj = scale(det, adj);
        return adj;
    }

    public Matrix3D adjugate()
    {
        return(cofactor().transpose());
    }

    public Matrix3D cofactor()
    {
        Matrix2D[] temp = new Matrix2D[9];
        temp[0] = new Matrix2D(j.y, k.y, j.z, k.z);
        temp[1] = new Matrix2D(i.y, k.y, i.z, k.z);
        temp[2] = new Matrix2D(i.y, j.y, i.z, j.z);
        temp[3] = new Matrix2D(j.x, k.x, j.z, k.z);
        temp[4] = new Matrix2D(i.x, k.x, i.z, j.z);
        temp[5] = new Matrix2D(i.x, j.x, i.z, j.z);
        temp[6] = new Matrix2D(j.x, k.x, j.y, k.y);
        temp[7] = new Matrix2D(i.x, k.x, i.y, k.y);
        temp[8] = new Matrix2D(i.x, j.x, i.z, j.y);

        Matrix3D out = new Matrix3D();
        out.i.x = temp[0].determinant();
        out.j.x = temp[1].determinant();
        out.k.x = temp[2].determinant();

        out.i.y = temp[3].determinant();
        out.j.y = temp[4].determinant();
        out.k.y = temp[5].determinant();

        out.i.z = temp[6].determinant();
        out.j.z = temp[7].determinant();
        out.k.z = temp[8].determinant();

        return out;
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
