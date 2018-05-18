package Synth;

import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.Hashtable;

public class MeshCollection
{
    KDTree meshVertexTree;
    Hashtable<PVector, int[]> meshVertexMap;
    Hashtable<PVector, ArrayList<Integer[]>> faceListIndexMap;
    ArrayList<Mesh> meshes;
    PApplet app;

    public MeshCollection(ArrayList<Mesh> _meshes, PApplet _app)
    {
        meshes = _meshes;
        app = _app;
        meshVertexTree = createMeshTree();
        meshVertexMap = createMeshMap();
        faceListIndexMap = createFaceIndexMap();
    }

    private Hashtable<PVector, int[]> createMeshMap()
    {
        Hashtable<PVector, int[]> vertMap = new Hashtable<>();
        for (int i = 0; i < meshes.size(); i++)
        {
            for (int j = 0; j < meshes.get(i).vertices.size(); j++)
            {
                PVector key = meshes.get(i).vertices.get(j);
                int[] value = new int[2];
                value[0] = i;
                value[1] = j;
                vertMap.put(key, value);
            }
        }
        return vertMap;
    }

    private Hashtable<PVector, ArrayList<Integer[]>> createFaceIndexMap()
    {
        Hashtable<PVector, ArrayList<Integer[]>> faceMap = new Hashtable<>();
        for (int i = 0; i < meshes.size(); i++)
        {
            for (int j = 0; j < meshes.get(i).faceVerts.size(); j++)
            {
                int keyIndex = (meshes.get(i).faceVerts.get(j));
                if (keyIndex == -1)
                {
                    continue;
                }
                PVector key = meshes.get(i).vertices.get(keyIndex);
                Integer[] value = new Integer[2];
                value[0] = i;
                value[1] = j;
                ArrayList<Integer[]> temp = faceMap.get(key);
                if (temp == null)
                {
                    temp = new ArrayList<>();
                    temp.add(value);
                    faceMap.put(key, temp);
                }
                else
                {
                    temp.add(value);
                }
            }
        }
        return faceMap;
    }

    private PVector[] explodeAllMeshes()
    {
        int totalSize = 0;
        for (Mesh m : meshes)
        {
            totalSize += m.vertices.size();
        }
        PVector[] out = new PVector[totalSize];
        int index = 0;
        for (Mesh m : meshes)
        {
            for (PVector vec : m.vertices)
            {
                out[index] = vec;
                index++;
            }
        }
        return out;
    }


    private KDTree createMeshTree()
    {
        PVector[] tempArray = explodeAllMeshes();
        return new KDTree(tempArray, 0, app);
    }

    //TODO(bryn):Keep working on this mesh thing when you can
    public PVector closestPointOnMesh(PVector point)
    {
        //TODO(bryn): Currently this only works if MeshCollection contains only ONE mesh, not full functionality yet
        PVector closestVert = meshVertexTree.nearestNeighbor(point);
        int[] closestVertIndices = meshVertexMap.get(closestVert);
        PVector key = meshes.get(closestVertIndices[0]).vertices.get(closestVertIndices[1]);
        ArrayList<Integer[]> faceVertsIndices = faceListIndexMap.get(key);

        Mesh current = meshes.get(closestVertIndices[0]);
        //PVector closestVertNormal = normals.get(faceNormals.get(faceVerts.indexOf(closestVertIndices)));

        PVector closestVertNormal = current.normals.get(current.faceNormals.get(faceVertsIndices.get(0)[1]));
        int index1;
        int index2;
        int index3;
        PVector normal2;
        PVector normal3;

        PVector result = null;
        PVector averageNormal = new PVector();

        for (int i = 0; i < faceVertsIndices.size(); i++)
        {
            int currentIndex = faceVertsIndices.get(i)[1];
            if (currentIndex % 4 == 0)
            {
                //Start of face
                index1 = current.faceVerts.get(currentIndex);
                index2 = current.faceVerts.get(currentIndex + 1);
                index3 = current.faceVerts.get(currentIndex + 2);
                PVector[] temparray = {current.vertices.get(index1), current.vertices.get(index2), current.vertices.get(index3)};

                int normalIndex1 = current.faceNormals.get(currentIndex + 1);
                int normalIndex2 = current.faceNormals.get(currentIndex + 2);

                normal2 = current.normals.get(normalIndex1);
                normal3 = current.normals.get(normalIndex2);

                averageNormal = PVector.add(PVector.add(normal2, normal3), closestVertNormal);
                averageNormal = PVector.div(averageNormal, 3);

                PVector tempResult = current.lineIntersectsTriangle(point, averageNormal, temparray);
                if ((tempResult != null && (result == null || point.dist(result) > point.dist(tempResult))))
                {
                    result = tempResult;
                }


            }
            else if (currentIndex % 4 == 1)
            {
                //Middle of face
                index2 = current.faceVerts.get(currentIndex);
                index1 = current.faceVerts.get(currentIndex - 1);
                index3 = current.faceVerts.get(currentIndex + 1);
                PVector[] temparray = {current.vertices.get(index1), current.vertices.get(index2), current.vertices.get(index3)};

                int normalIndex1 = current.faceNormals.get(currentIndex - 1);
                int normalIndex2 = current.faceNormals.get(currentIndex + 1);

                normal2 = current.normals.get(normalIndex1);
                normal3 = current.normals.get(normalIndex2);

                averageNormal = PVector.add(PVector.add(normal2, normal3), closestVertNormal);
                averageNormal = PVector.div(averageNormal, 3);

                PVector tempResult = current.lineIntersectsTriangle(point, averageNormal, temparray);
                if ((tempResult != null && (result == null || point.dist(result) > point.dist(tempResult))))
                {
                    result = tempResult;
                }


            }
            else if (currentIndex % 4 == 2)
            {
                //End of face
                index3 = current.faceVerts.get(currentIndex);
                index2 = current.faceVerts.get(currentIndex - 1);
                index1 = current.faceVerts.get(currentIndex - 2);

                int normalIndex1 = current.faceNormals.get(currentIndex - 1);
                int normalIndex2 = current.faceNormals.get(currentIndex - 2);
                normal2 = current.normals.get(normalIndex1);

                normal3 = current.normals.get(normalIndex2);
                PVector[] temparray = {current.vertices.get(index1), current.vertices.get(index2), current.vertices.get(index3)};
                averageNormal = PVector.add(PVector.add(normal2, normal3), closestVertNormal);
                averageNormal = PVector.div(averageNormal, 3);

                PVector tempResult = current.lineIntersectsTriangle(point, averageNormal, temparray);
                if ((tempResult != null && (result == null || point.dist(result) > point.dist(tempResult))))
                {
                    result = tempResult;
                }

            }


        }
        if (result != null)
        {
            return result.copy();
        }

        //NEED TO DO THE SAME AS ABOVE FOR ALL EDGES
        //For each vertex connected to the closest one, try to project onto the edge they make. return the closest one
        PVector closestInterpolatedPoint = null;
        PVector closestInterpolatedNormal = new PVector();
        for (Integer[] var : faceVertsIndices)
        {

            int currentIndex = var[1];
            if (currentIndex % 4 == 0)
            {
                //Start of face
                index1 = current.faceVerts.get(currentIndex);
                index2 = current.faceVerts.get(currentIndex + 1);
                index3 = current.faceVerts.get(currentIndex + 2);
                int normalIndex1 = current.faceNormals.get(currentIndex + 1);
                int normalIndex2 = current.faceNormals.get(currentIndex + 2);

                PVector[] temp = current.lineCP2(current.vertices.get(index1), current.vertices.get(index2), point,
                        closestVertNormal, current.normals.get(normalIndex1),
                        null, null);
                if (closestInterpolatedPoint == null)
                {
                    closestInterpolatedPoint = temp[0];
                    closestInterpolatedNormal = temp[1];

                }
                temp = current.lineCP2(current.vertices.get(index1), current.vertices.get(index3), point, closestVertNormal, current.normals.get(normalIndex2),
                        null, null);
                if (temp[0].dist(point) < closestInterpolatedPoint.dist(point))
                {
                    closestInterpolatedPoint = temp[0];
                    closestInterpolatedNormal = temp[1];
                }
            }
            else if (currentIndex % 4 == 1)
            {
                //Middle of face
                index2 = current.faceVerts.get(currentIndex);
                index1 = current.faceVerts.get(currentIndex - 1);
                index3 = current.faceVerts.get(currentIndex + 1);

                int normalIndex1 = current.faceNormals.get(currentIndex - 1);
                int normalIndex2 = current.faceNormals.get(currentIndex + 1);

                PVector[] temp = current.lineCP2(current.vertices.get(index2), current.vertices.get(index1), point, closestVertNormal, current.normals.get(normalIndex1), null, null);
                if (closestInterpolatedPoint == null)
                {
                    closestInterpolatedPoint = temp[0];
                    closestInterpolatedNormal = temp[1];

                }
                temp = current.lineCP2(current.vertices.get(index2), current.vertices.get(index3), point, closestVertNormal, current.normals.get(normalIndex2), null, null);
                if (temp[0].dist(point) < closestInterpolatedPoint.dist(point))
                {
                    closestInterpolatedPoint = temp[0];
                    closestInterpolatedNormal = temp[1];
                }

            }
            else if (currentIndex % 4 == 2)
            {
                //End of face
                index3 = current.faceVerts.get(currentIndex);
                index2 = current.faceVerts.get(currentIndex - 1);
                index1 = current.faceVerts.get(currentIndex - 2);

                int normalIndex2 = current.faceNormals.get(currentIndex - 1);
                int normalIndex1 = current.faceNormals.get(currentIndex - 2);
                normal2 = current.normals.get(normalIndex1);
                normal3 = current.normals.get(normalIndex2);

                PVector[] temp = current.lineCP2(current.vertices.get(index3), current.vertices.get(index2), point, closestVertNormal, current.normals.get(normalIndex2),
                        null, null);
                if (closestInterpolatedPoint == null)
                {
                    closestInterpolatedPoint = temp[0];
                    closestInterpolatedNormal = temp[1];

                }
                temp = current.lineCP2(current.vertices.get(index3), current.vertices.get(index1), point, closestVertNormal, current.normals.get(normalIndex1), null, null);
                if (temp[0].dist(point) < closestInterpolatedPoint.dist(point))
                {
                    closestInterpolatedPoint = temp[0];
                    closestInterpolatedNormal = temp[1];
                }
            }
        }
        if (SynthMain.drawNeighbours)
        {
            app.strokeWeight(5);
            app.stroke(0, 255, 255);
            app.line(point.x, point.y, point.z, closestInterpolatedPoint.x, closestInterpolatedPoint.y, closestInterpolatedPoint.z);
            app.strokeWeight(1);
        }
        return closestInterpolatedPoint;
    }

    public void moveMeshCentreToWorld()
    {
        for (Mesh m : meshes)
        {
            m.moveMeshCentreToWorld();
        }
        createMeshTree();
        createMeshMap();
    }

    public void drawAllWires(int strokeCol, float strokeWeight)
    {
        for (Mesh m : meshes)
        {
            m.drawWires(strokeCol, strokeWeight);
        }
        createMeshTree();
        createMeshMap();
    }

    public void scaleAllMeshes(float scaleFactor, PVector scaleOrigin)
    {
        for (Mesh m : meshes)
        {
            m.scale(scaleFactor, scaleOrigin);
        }
        createMeshTree();
        createMeshMap();
    }
}
