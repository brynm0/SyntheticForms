import processing.core.PVector;

public class Matrix2D
{
    PVector i;
    PVector j;

    public Matrix2D(PVector _i, PVector _j)
    {
        i = _i;
        j = _j;
    }

    public Matrix2D(float a, float b, float c, float d)
    {
        i.x = a;
        i.y = c;
        j.x = b;
        j.y = d;
    }


    public Matrix2D()
    {
        i = new PVector();
        j = new PVector();
    }

    public float determinant()
    {
        return (1.0f / (i.x * j.y - i.x * j.y));

    }

    public static Matrix2D scale (float scalar, Matrix2D in)
    {
        Matrix2D outMat = new Matrix2D();
        outMat.i = PVector.mult(in.i, scalar);
        outMat.j = PVector.mult(in.j, scalar);
        return outMat;
    }

    public static PVector changeOfBasis2D(Matrix2D newBasis, PVector vecToTransform)
    {

        //CHANGE OF BASIS:
        //                [i'.x j'.x][vec.x] i'&j' are the transformed basis vectors in WORLD coordinate system
        //                [i'.y j'.y][vec.y] TO TRANSLATE FORM WORLD TO MESH SPACE i' & j' MUST BE THE WORLD BASIS VECTORS IN
        //                                 MESH SPACE
        //                vec.x*i' + vec.y * j'

        PVector inverseI = new PVector();
        PVector inverseJ = new PVector();
        float detBasis = newBasis.determinant();
        inverseI.x = newBasis.j.y * detBasis;
        inverseI.y = -1 * newBasis.i.x * detBasis;
        inverseJ.x = -1 * newBasis.j.x * detBasis;
        inverseJ.y = newBasis.i.x * detBasis;
        PVector outputVec = new PVector();
        outputVec.x = vecToTransform.x * inverseI.x + vecToTransform.x * inverseI.y;
        outputVec.y = vecToTransform.y * inverseJ.x + vecToTransform.y * inverseJ.y;
        return outputVec;

    }

}
