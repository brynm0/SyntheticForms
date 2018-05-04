package Synth;

import com.sun.istack.internal.Nullable;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Mesh
{

    ArrayList<PVector> vertices;
    ArrayList<PVector> normals;
    ArrayList<Integer> faceVerts;
    ArrayList<Integer> faceNormals;
    ArrayList<PVector> texCoords;
    ArrayList<Integer> faceTexCoords;
    PApplet app;
    int numFaces = 0;

    private Mesh(PApplet _ap)
    {
        vertices = new ArrayList<>();
        texCoords = new ArrayList<>();
        normals = new ArrayList<>();

        faceVerts = new ArrayList<>();
        faceTexCoords = new ArrayList<>();
        faceNormals = new ArrayList<>();
        app = _ap;
    }

    private Mesh(ArrayList<PVector> _vertices,
                 ArrayList<PVector> _texCoords,
                 ArrayList<PVector> _normals,
                 ArrayList<Integer> _faces,
                 ArrayList<Integer> _faceTex,
                 ArrayList<Integer> _faceNormals,
                 PApplet _app)
    {
        vertices = _vertices;
        texCoords = _texCoords;
        normals = _normals;
        faceVerts = _faces;
        faceTexCoords = _faceTex;
        faceNormals = _faceNormals;
        app = _app;
    }

    public Mesh(Mesh another)
    {
        vertices = new ArrayList<>(another.vertices);
        texCoords = new ArrayList<>(another.texCoords);
        normals = new ArrayList<>(another.normals);
        faceVerts = new ArrayList<>(another.faceVerts);
        faceTexCoords = new ArrayList<>(another.faceTexCoords);
        faceNormals = new ArrayList<>(another.faceNormals);
        app = another.app;
    }

    public static ArrayList<Mesh> readMeshes(String absolutePath, PApplet app)
    {
        Charset charset = Charset.forName("US-ASCII");
        String p = "file://" + absolutePath;
        Path file = Paths.get(URI.create(p));

        System.out.println(file);
        ArrayList<Mesh> outputMeshList = new ArrayList<>();
        int groupCurrentIndex = -1;
        try (BufferedReader reader = Files.newBufferedReader(file, charset))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                //TODO(bryn): If new obj group is started, finish old mesh, add it to the list, start new one.
                if (line.startsWith("g ") || groupCurrentIndex == -1)
                {
                    groupCurrentIndex++;
                    outputMeshList.add(new Mesh(app));
                }
                if (line.startsWith("v "))
                {
                    String[] split = line.split(" ");
                    outputMeshList.get(groupCurrentIndex).vertices.add(new PVector(Float.parseFloat(split[1]), Float.parseFloat(split[2]), Float.parseFloat(split[3])));

                }
                else if (line.startsWith("vt "))
                {
                    if (line.contains("["))
                    {
                        String[] split = line.split(" ");
                        float uComponent = Float.parseFloat(split[1]);
                        float vComponent = Float.parseFloat(split[2]);

                        String wComponentAsString = split[3].substring(1, split[3].length() - 1);
                        float wComponent = Float.parseFloat(wComponentAsString);
                        outputMeshList.get(groupCurrentIndex).texCoords.add(new PVector(uComponent, vComponent, wComponent));
                    }
                    else
                    {
                        String[] split = line.split(" ");
                        outputMeshList.get(groupCurrentIndex).texCoords.add(new PVector(Float.parseFloat(split[1]), Float.parseFloat(split[2]), 0f));
                    }
                    String[] split = line.split(" ");
                }
                else if (line.startsWith("vn "))
                {
                    String[] split = line.split(" ");
                    outputMeshList.get(groupCurrentIndex).normals.add(new PVector(Float.parseFloat(split[1]), Float.parseFloat(split[2]), Float.parseFloat(split[3])));
                }
                else if (line.startsWith("f "))
                {
                    if (line.contains("/"))
                    {

                        String[] splitSpace = line.split(" ");
                        for (int i = 1; i < splitSpace.length; i++)
                        {
                            String[] splitSlash = splitSpace[i].split("/");
                            if (splitSlash.length == 2)
                            {
                                //Add index to face vertex list
                                if (splitSlash[0].length() == 0)
                                {
                                    outputMeshList.get(groupCurrentIndex).faceVerts.add(Integer.parseInt(splitSlash[0]) - 1);
                                }
                                else
                                {
                                    outputMeshList.get(groupCurrentIndex).faceVerts.add(-1);

                                }
                                if (splitSlash[1].length() == 0)
                                {
                                    //Add index to face texCoord list
                                    outputMeshList.get(groupCurrentIndex).faceTexCoords.add(Integer.parseInt(splitSlash[1]) - 1);
                                }
                                else
                                {
                                    outputMeshList.get(groupCurrentIndex).faceTexCoords.add(-1);
                                }
                            }
                            else if (splitSlash.length == 3)
                            {
                                //Add index to face vertex list
                                if (splitSlash[0].length() != 0)
                                {
                                    outputMeshList.get(groupCurrentIndex).faceVerts.add(Integer.parseInt(splitSlash[0]) - 1);
                                }
                                else
                                {
                                    outputMeshList.get(groupCurrentIndex).faceVerts.add(-1);

                                }
                                if (splitSlash[1].length() != 0)
                                {
                                    //Add index to face texCoord list
                                    outputMeshList.get(groupCurrentIndex).faceTexCoords.add(Integer.parseInt(splitSlash[1]) - 1);
                                }
                                else
                                {
                                    outputMeshList.get(groupCurrentIndex).faceTexCoords.add(-1);
                                }
                                if (splitSlash[2].length() != 0)
                                {
                                    //Add index to face normal list
                                    outputMeshList.get(groupCurrentIndex).faceNormals.add(Integer.parseInt(splitSlash[2]) - 1);
                                }
                                else
                                {
                                    outputMeshList.get(groupCurrentIndex).faceNormals.add(-1);
                                }
                            }

                        }
                        outputMeshList.get(groupCurrentIndex).numFaces++;
                        outputMeshList.get(groupCurrentIndex).faceVerts.add(-1);
                        outputMeshList.get(groupCurrentIndex).faceNormals.add(-1);
                        outputMeshList.get(groupCurrentIndex).faceTexCoords.add(-1);

                    }
                    else
                    {
                        String[] split = line.split(" ");
                        for (int i = 1; i < split.length; i++)
                        {
                            outputMeshList.get(groupCurrentIndex).faceVerts.add(Integer.parseInt(split[i]) - 1);
                            outputMeshList.get(groupCurrentIndex).faceNormals.add(-1);
                            outputMeshList.get(groupCurrentIndex).faceTexCoords.add(-1);

                        }
                        outputMeshList.get(groupCurrentIndex).numFaces++;
                        outputMeshList.get(groupCurrentIndex).faceVerts.add(-1);
                        outputMeshList.get(groupCurrentIndex).faceNormals.add(-1);
                        outputMeshList.get(groupCurrentIndex).faceTexCoords.add(-1);
                    }
                }
            }
        }
        catch (IOException e)
        {
            System.err.format("IOException: %s%n", e);
        }
        System.out.println("Successfully loaded mesh " + absolutePath);
        System.out.println("OBJ file contains " + outputMeshList.size() + " mesh group(s)");
        for (Mesh m : outputMeshList)
        {
            System.out.println("V: " + m.vertices.size() + " VN: " + m.normals.size()
                    + " VT: " + m.texCoords.size() + " F: " + m.faceVerts.size() / 4);

        }

        return outputMeshList;
    }

    public static void writeObj(ArrayList<Mesh> meshes)
    {
        long fileID = System.currentTimeMillis();
        System.out.println("Working on saving obj");
        try
        {
            PrintWriter p = new PrintWriter(fileID + "out.obj");
            p.println("# Synth.Mesh produced by custom script");
            p.println("# Bryn Murrell 2018");


            //Loop through group
            int runningTotalSize = 0;
            for (int meshIndex = 0; meshIndex < meshes.size(); meshIndex++)
            {
                if (meshes.get(meshIndex).faceVerts.size() != 0)
                {
                    Mesh curr = meshes.get(meshIndex);

                    p.println("g object_" + meshIndex);
                    for (PVector v : curr.vertices)
                    {
                        p.println("v " + v.x + " " + v.y + " " + v.z);
                    }
                    for (PVector vt : curr.texCoords)
                    {
                        p.println("vt " + vt.x + " " + vt.y + " " + vt.z);
                    }
                    for (PVector vn : curr.normals)
                    {
                        p.println("vn " + vn.x + " " + vn.y + " " + vn.z);
                    }
                    curr.countFaces();
                    //TODO(bryn): Fill out printwriting for quadmeshes & trimeshes
                    //could be a pure quad mesh or a mix of quads and tris
                    //will probably be the latter, so need to account for that
                    String currface = "";

                    for (int i = 0; i < curr.faceVerts.size(); i++)
                    {

                        if (i == 0)
                        {
                            currface = "";
                            //start of face, insert f
                            currface += "f ";
                        }
                        else if (curr.faceVerts.get(i) == -1)
                        {
                            p.println(currface);
                            currface = "";
                            //start of face, insert f
                            currface += "f ";
                            continue;
                        }
                        currface += (curr.faceVerts.get(i) + 1 + runningTotalSize);
                        currface += "/";
                        if (curr.faceTexCoords.get(i) != -1)
                        {
                            currface += (1 + curr.faceTexCoords.get(i) + runningTotalSize);
                        }
                        currface += "/";
                        currface += (1 + curr.faceNormals.get(i) + runningTotalSize);
                        currface += " ";
                    }
                    runningTotalSize += meshes.get(meshIndex).vertices.size();
                }
            }
            p.close();
        }
        catch (IOException e)
        {
            System.out.println("Unhandled IO Exception " + e);
        }
        finally
        {
            System.out.println("done");

        }

    }

    public static Mesh tween2Meshes(Mesh A, Mesh B, float t)
    {
        assert (A.faceVerts.size() == B.faceVerts.size() && A.vertices.size() == B.vertices.size());
        Mesh out = new Mesh(A);
        for (int i = 0; i < A.vertices.size(); i++)
        {
            out.vertices.set(i, SynthMath.lerpVector(A.vertices.get(i), B.vertices.get(i), t));
            out.normals.set(i, SynthMath.lerpVector(A.normals.get(i), B.normals.get(i), t));
            out.texCoords.set(i, SynthMath.lerpVector(A.texCoords.get(i), B.normals.get(i), t));
        }
        return out;
    }

    void countFaces()
    {
        int total = 0;
        for (int i = 0; i < faceVerts.size(); i++)
        {
            if (faceVerts.get(i) == -1)
            {
                total++;
            }
        }
        numFaces = total;
    }

    public PVector getMeshAverage()
    {
        int vertexCount = vertices.size();
        PVector average = new PVector();
        for (PVector vertex : vertices)
        {
            PVector temp = vertex.copy();
            temp.mult(1.0f / vertexCount);
            average.add(temp);
            assert !Float.isNaN(average.x);
            assert !Float.isNaN(average.y);
            assert !Float.isNaN(average.z);
        }
        return average;
    }

    public PVector moveMeshCentreToWorld()
    {
        PVector origin = new PVector();
        PVector centre = this.getMeshAverage();
        PVector transform = PVector.sub(origin, centre);
        for (PVector vertex : vertices)
        {
            vertex.add(transform);
        }
        return transform;
    }


    public void drawWires(int strokeCol, int strokeWeight)
    {
        app.beginShape();
        app.stroke(strokeCol);
        app.strokeWeight(strokeWeight);
        for (int i = 0; i < faceVerts.size(); i++)
        {
            if (faceVerts.get(i) == -1)
            {
                app.endShape(app.CLOSE);
                app.beginShape();
            }
            else
            {
                PVector vertex = vertices.get(faceVerts.get(i));
                app.vertex(vertex.x, vertex.y, vertex.z);
            }
        }
        app.endShape(app.CLOSE);
    }

    public void scale(float scaleFactor, PVector scaleOrigin)
    {
        for (int i = 0; i < vertices.size(); i++)
        {
            PVector scl = PVector.sub(vertices.get(i), scaleOrigin);
            scl.mult(scaleFactor);
            vertices.set(i, PVector.add(scl, scaleOrigin));
        }
    }

    public PVector[] populate(int count, @Nullable ArrayList<PVector> outNormals)
    {
        PVector[] out = new PVector[count];
        int currIndex = 0;
        for (int i = 0; i < count; i++)
        {
            //Pick a random face
            assert (faceVerts.size() % 4 == 0);
            int faceIndex = PApplet.floor(app.random(faceVerts.size()));
            faceIndex -= faceIndex % 4;
            int index1 = faceVerts.get(faceIndex);
            int normalIndex1 = faceNormals.get(faceIndex);

            int index2 = faceVerts.get(faceIndex + 1);
            int normalIndex2 = faceNormals.get(faceIndex + 1);

            int index3 = faceVerts.get(faceIndex + 2);
            int normalIndex3 = faceNormals.get(faceIndex + 2);

            PVector p1 = vertices.get(index1);
            PVector p2 = vertices.get(index2);
            PVector p3 = vertices.get(index3);

            PVector param = new PVector(app.random(1), app.random(1));

            if (param.x + param.y > 1)
            {
                //Transform so that it fits on the triangle
                param.x = 1 - param.x;
                param.y = 1 - param.y;
            }

            //x = a1 * v1 + a2 * v2;
            PVector v1 = PVector.sub(p2, p1);
            PVector v2 = PVector.sub(p3, p1);
            PVector f = PVector.add(PVector.mult(v1, param.x), PVector.mult(v2, param.y));
            f.add(p1);
            out[currIndex] = f;
            currIndex++;

            // calculate vectors from point f to vertices p1, p2 and p3:

            PVector f1 = PVector.sub(p1, f);
            PVector f2 = PVector.sub(p2, f);
            PVector f3 = PVector.sub(p3, f);

            // calculate the areas and factors (order of parameters doesn't matter):
            float a = (PVector.sub(p1, p2).cross(PVector.sub(p1, p3))).mag();

            float a1 = PVector.sub(f2, f3).mag() / a;

            float a2 = PVector.sub(f3, f1).mag() / a;

            float a3 = PVector.sub(f1, f2).mag() / a;

            // find the uv corresponding to point f (uv1/uv2/uv3 are associated to p1/p2/p3):
            PVector term1 = PVector.mult(normals.get(normalIndex1), a1);
            PVector term2 = PVector.mult(normals.get(normalIndex2), a2);
            PVector term3 = PVector.mult(normals.get(normalIndex3), a3);

            PVector normalVector = PVector.add(PVector.add(term1, term2), term3);
            if (outNormals != null)
            {
                outNormals.add(normalVector);
            }
        }
        return out;
    }


    public void popNoise()
    {
        ArrayList<PVector> outTex = new ArrayList<>();
        for (int index = 0; index < vertices.size(); ++index)
        {
            float noiseVal = app.noise(vertices.get(index).x, vertices.get(index).y, vertices.get(index).z);
            noiseVal = PApplet.map(noiseVal, 0, 1, 0, PConstants.TWO_PI);
            PVector noiseVec = PVector.fromAngle(noiseVal);
            Plane currentPlane = new Plane(vertices.get(index).copy(), normals.get(index).copy());
            //xi + yj
            //Using the plane's X and Y axes as new basis vectors for noiseVec
            PVector newX = PVector.mult(currentPlane.x, noiseVec.x);
            PVector newY = PVector.mult(currentPlane.y, noiseVec.y);
            PVector newZ = PVector.mult(currentPlane.z, noiseVec.z);
            PVector outNoise = PVector.add(PVector.add(newX, newY), newZ);
            outTex.add(outNoise);
        }
        texCoords = outTex;
    }

    public PVector[] closestPointOnMesh(PVector point, KDTree vertexTree)
    {
        PVector closestVert = vertexTree.nearestNeighbor(point);
        //TODO(bryn): Rework this so it's v1, v2 v3 - or closest vert & second closest can change.
        int closestVertIndex = vertices.indexOf(closestVert);
        assert faceVerts.contains(closestVertIndex);
        PVector closestVertNormal = normals.get(faceNormals.get(faceVerts.indexOf(closestVertIndex)));

        ArrayList<Integer> indices = SynthMain.indexOfAll(closestVertIndex, faceVerts);
        int index1;
        int index2;
        int index3;
        PVector normal2;
        PVector normal3;

        PVector tex1;
        PVector tex2;
        PVector tex3;

        PVector result = null;
        PVector averageNormal = new PVector();
        PVector averageTex = new PVector();

        for (int i = 0; i < indices.size(); i++)
        {
            if (indices.get(i) % 4 == 0)
            {
                //Start of face
                index1 = faceVerts.get(indices.get(i));
                index2 = faceVerts.get(indices.get(i) + 1);
                index3 = faceVerts.get(indices.get(i) + 2);
                int normalIndex1 = faceNormals.get(indices.get(i) + 1);
                int normalIndex2 = faceNormals.get(indices.get(i) + 2);
                tex1 = texCoords.get(index1);
                tex2 = texCoords.get(index2);
                tex3 = texCoords.get(index3);

                normal2 = normals.get(normalIndex1);

                normal3 = normals.get(normalIndex2);

                PVector[] temparray = {vertices.get(index1), vertices.get(index2), vertices.get(index3)};
                averageNormal = PVector.add(PVector.add(normal2, normal3), closestVertNormal);
                averageNormal = PVector.div(averageNormal, 3);

                averageTex = PVector.add(PVector.add(tex1, tex3), tex2);
                averageTex = PVector.div(averageTex, 3);


                PVector tempResult = lineIntersectsTriangle(point, averageNormal, temparray);
                if ((tempResult != null && (result == null || point.dist(result) > point.dist(tempResult))))
                {
                    result = tempResult;
                }


            }
            else if (indices.get(i) % 4 == 1)
            {
                //Middle of face
                index2 = faceVerts.get(indices.get(i));
                index1 = faceVerts.get(indices.get(i) - 1);
                index3 = faceVerts.get(indices.get(i) + 1);

                int normalIndex1 = faceNormals.get(indices.get(i) - 1);
                int normalIndex2 = faceNormals.get(indices.get(i) + 1);
                normal2 = normals.get(normalIndex1);

                normal3 = normals.get(normalIndex2);

                tex1 = texCoords.get(index1);
                tex2 = texCoords.get(index2);
                tex3 = texCoords.get(index3);

                PVector[] temparray = {vertices.get(index1), vertices.get(index2), vertices.get(index3)};
                averageNormal = PVector.add(PVector.add(normal2, normal3), closestVertNormal);
                averageNormal = PVector.div(averageNormal, 3);

                averageTex = PVector.add(PVector.add(tex1, tex3), tex2);
                averageTex = PVector.div(averageTex, 3);


                PVector tempResult = lineIntersectsTriangle(point, averageNormal, temparray);
                if ((tempResult != null && (result == null || point.dist(result) > point.dist(tempResult))))
                {
                    result = tempResult;
                }


            }
            else if (indices.get(i) % 4 == 2)
            {
                //End of face
                index3 = faceVerts.get(indices.get(i));
                index2 = faceVerts.get(indices.get(i) - 1);
                index1 = faceVerts.get(indices.get(i) - 2);

                int normalIndex1 = faceNormals.get(indices.get(i) - 1);
                int normalIndex2 = faceNormals.get(indices.get(i) - 2);
                normal2 = normals.get(normalIndex1);

                normal3 = normals.get(normalIndex2);
                tex1 = texCoords.get(index1);
                tex2 = texCoords.get(index2);
                tex3 = texCoords.get(index3);

                PVector[] temparray = {vertices.get(index1), vertices.get(index2), vertices.get(index3)};
                averageNormal = PVector.add(PVector.add(normal2, normal3), closestVertNormal);
                averageNormal = PVector.div(averageNormal, 3);

                averageTex = PVector.add(PVector.add(tex1, tex3), tex2);
                averageTex = PVector.div(averageTex, 3);

                PVector tempResult = lineIntersectsTriangle(point, averageNormal, temparray);
                if ((tempResult != null && (result == null || point.dist(result) > point.dist(tempResult))))
                {
                    result = tempResult;
                }

            }


        }
        if (result != null)
        {
            return new PVector[]{result, averageNormal, averageTex};
        }

        //NEED TO DO THE SAME AS ABOVE FOR ALL EDGES
        //For each vertex connected to the closest one, try to project onto the edge they make. return the closest one
        PVector closestInterpolatedPoint = null;
        PVector closestInterpolatedNormal = new PVector();
        PVector closestIntTex = new PVector();
        for (int i = 0; i < indices.size(); i++)
        {
            if (indices.get(i) % 4 == 0)
            {
                //Start of face
                index1 = faceVerts.get(indices.get(i));
                index2 = faceVerts.get(indices.get(i) + 1);
                index3 = faceVerts.get(indices.get(i) + 2);
                int normalIndex1 = faceNormals.get(indices.get(i) + 1);
                int normalIndex2 = faceNormals.get(indices.get(i) + 2);

                tex1 = texCoords.get(index1);
                tex2 = texCoords.get(index2);
                tex3 = texCoords.get(index3);


                PVector[] temp = lineCP2(vertices.get(index1), vertices.get(index2), point,
                        closestVertNormal, normals.get(normalIndex1),
                        tex1, tex2);
                if (closestInterpolatedPoint == null)
                {
                    closestInterpolatedPoint = temp[0];
                    closestInterpolatedNormal = temp[1];
                    closestIntTex = temp[2];

                }
                temp = lineCP2(vertices.get(index1), vertices.get(index3), point, closestVertNormal, normals.get(normalIndex2),
                        tex1, tex3);
                if (temp[0].dist(point) < closestInterpolatedPoint.dist(point))
                {
                    closestInterpolatedPoint = temp[0];
                    closestInterpolatedNormal = temp[1];
                    closestIntTex = temp[2];
                }
            }
            else if (indices.get(i) % 4 == 1)
            {
                //Middle of face
                index2 = faceVerts.get(indices.get(i));
                index1 = faceVerts.get(indices.get(i) - 1);
                index3 = faceVerts.get(indices.get(i) + 1);

                int normalIndex1 = faceNormals.get(indices.get(i) - 1);
                int normalIndex2 = faceNormals.get(indices.get(i) + 1);
                tex1 = texCoords.get(index1);
                tex2 = texCoords.get(index2);
                tex3 = texCoords.get(index3);

                PVector[] temp = lineCP2(vertices.get(index2), vertices.get(index1), point,
                        closestVertNormal, normals.get(normalIndex1),
                        tex2, tex1);
                if (closestInterpolatedPoint == null)
                {
                    closestInterpolatedPoint = temp[0];
                    closestInterpolatedNormal = temp[1];
                    closestIntTex = temp[2];

                }
                temp = lineCP2(vertices.get(index2), vertices.get(index3), point, closestVertNormal, normals.get(normalIndex2), tex2, tex3);
                if (temp[0].dist(point) < closestInterpolatedPoint.dist(point))
                {
                    closestInterpolatedPoint = temp[0];
                    closestInterpolatedNormal = temp[1];
                    closestIntTex = temp[2];

                }

            }
            else if (indices.get(i) % 4 == 2)
            {
                //End of face
                index3 = faceVerts.get(indices.get(i));
                index2 = faceVerts.get(indices.get(i) - 1);
                index1 = faceVerts.get(indices.get(i) - 2);

                int normalIndex2 = faceNormals.get(indices.get(i) - 1);
                int normalIndex1 = faceNormals.get(indices.get(i) - 2);
                normal2 = normals.get(normalIndex1);
                normal3 = normals.get(normalIndex2);

                tex1 = texCoords.get(index1);
                tex2 = texCoords.get(index2);
                tex3 = texCoords.get(index3);

                PVector[] temp = lineCP2(vertices.get(index3), vertices.get(index2), point, closestVertNormal, normals.get(normalIndex2),
                        tex3, tex2);
                if (closestInterpolatedPoint == null)
                {
                    closestInterpolatedPoint = temp[0];
                    closestInterpolatedNormal = temp[1];
                    closestIntTex = temp[2];

                }
                temp = lineCP2(vertices.get(index3), vertices.get(index1), point, closestVertNormal, normals.get(normalIndex1), tex3, tex1);
                if (temp[0].dist(point) < closestInterpolatedPoint.dist(point))
                {
                    closestInterpolatedPoint = temp[0];
                    closestInterpolatedNormal = temp[1];
                    closestIntTex = temp[2];

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
        return new PVector[]{closestInterpolatedPoint, closestInterpolatedNormal, closestIntTex};
    }

    private PVector lineIntersectsTriangle(PVector rayOrigin,
                                           PVector rayVector,
                                           PVector[] inTriangle)
    {
        float EPSILON = 0.0000001f;
        PVector vertex0 = inTriangle[0];
        PVector vertex1 = inTriangle[1];
        PVector vertex2 = inTriangle[2];
        PVector edge1, edge2, h, s, q;
        float a, f, u, v;
        edge1 = PVector.sub(vertex1, vertex0);
        edge2 = PVector.sub(vertex2, vertex0);
        h = rayVector.cross(edge2);
        a = edge1.dot(h);
        if (a > -EPSILON && a < EPSILON)
        {
            return null;
        }
        f = 1.0f / a;
        s = PVector.sub(rayOrigin, vertex0);
        u = f * (s.dot(h));
        if (u < 0.0f || u > 1.0f)
        {
            return null;
        }
        q = s.cross(edge1);
        v = f * rayVector.dot(q);
        if (v < 0.0f || u + v > 1.0f)
        {
            return null;
        }
        // At this stage we can compute t to find out where the intersection point is on the line.
        float t = f * edge2.dot(q);
        if (t > EPSILON) // ray intersection
        {
            return PVector.add(rayOrigin, PVector.mult(rayVector, t));
        }
        else
        {
            // This means that there is a line intersection but not a ray intersection.
            return PVector.add(rayOrigin, PVector.mult(rayVector, t));
        }
    }

    private PVector raycastTriangle(PVector rayOrigin,
                                    PVector rayVector,
                                    PVector[] inTriangle)
    {
        float EPSILON = 0.0000001f;
        PVector vertex0 = inTriangle[0];
        PVector vertex1 = inTriangle[1];
        PVector vertex2 = inTriangle[2];
        PVector edge1, edge2, h, s, q;
        float a, f, u, v;
        edge1 = PVector.sub(vertex1, vertex0);
        edge2 = PVector.sub(vertex2, vertex0);
        h = rayVector.cross(edge2);
        a = edge1.dot(h);
        if (a > -EPSILON && a < EPSILON)
        {
            return null;
        }
        f = 1.0f / a;
        s = PVector.sub(rayOrigin, vertex0);
        u = f * (s.dot(h));
        if (u < 0.0f || u > 1.0f)
        {
            return null;
        }
        q = s.cross(edge1);
        v = f * rayVector.dot(q);
        if (v < 0.0f || u + v > 1.0f)
        {
            return null;
        }
        // At this stage we can compute t to find out where the intersection point is on the line.
        float t = f * edge2.dot(q);
        if (t > EPSILON) // ray intersection
        {
            return PVector.add(rayOrigin, PVector.mult(rayVector, t));
        }
        else
        {
            // This means that there is a line intersection but not a ray intersection.
            return null;
        }
    }

    public PVector[] populateDownwardFaces(int num)
    {
        PVector[] out = new PVector[num];
        ArrayList<Integer[]> triangleList = new ArrayList<>();
        for (int i = 0; i < faceVerts.size() / 4; i++)
        {
            int index = i * 4;
            PVector averageNormal = new PVector();
            averageNormal.add(normals.get(faceNormals.get(index)));
            averageNormal.add(normals.get(faceNormals.get(index + 1)));
            averageNormal.add(normals.get(faceNormals.get(index + 2)));
            averageNormal.div(3);
            averageNormal.normalize();
            if (averageNormal.z < 0)
            {
                Integer[] templist = new Integer[3];
                templist[0] = faceVerts.get(index);
                templist[1] = faceVerts.get(index + 1);
                templist[2] = faceVerts.get(index + 2);
                triangleList.add(templist);
            }
        }
        for (int outIndex = 0; outIndex < num; outIndex++)
        {
            int randFaceIndex = (int) app.random(triangleList.size());
            PVector p1 = vertices.get(triangleList.get(randFaceIndex)[0]);
            PVector p2 = vertices.get(triangleList.get(randFaceIndex)[1]);
            PVector p3 = vertices.get(triangleList.get(randFaceIndex)[2]);

            PVector param = new PVector(app.random(1), app.random(1));

            if (param.x + param.y > 1)
            {
                //Transform so that it fits on the triangle
                param.x = 1 - param.x;
                param.y = 1 - param.y;
            }

            //x = a1 * v1 + a2 * v2;
            PVector v1 = PVector.sub(p2, p1);
            PVector v2 = PVector.sub(p3, p1);
            PVector f = PVector.add(PVector.mult(v1, param.x), PVector.mult(v2, param.y));
            f.add(p1);
            out[outIndex] = f;
        }
        return out;
    }

    public boolean insideMesh(PVector rayOrigin,
                              PVector rayVector)
    {
        int hitnum = 0;
        for (int i = 0; i < faceVerts.size() / 4; i++)
        {
            int index = i * 4;
            PVector[] currentTriangle = new PVector[3];
            currentTriangle[0] = vertices.get(faceVerts.get(index));
            currentTriangle[1] = vertices.get(faceVerts.get(index + 1));
            currentTriangle[2] = vertices.get(faceVerts.get(index + 2));
            if (raycastTriangle(rayOrigin, rayVector, currentTriangle) == null)
            {
                hitnum++;
            }
        }
        if (hitnum % 2 != 0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private PVector[] lineCP2(PVector A, PVector B, PVector P, PVector normalA, PVector normalB, PVector texA, PVector texB)
    {
//        auto AB = B - A;
        PVector AB = PVector.sub(B, A);
//        auto AP = P - A;
        PVector AP = PVector.sub(P, A);
//        float lengthSqrAB = AB.x * AB.x + AB.y * AB.y;
        float lengthSqAB = AB.magSq();
//        float t = (AP.x * AB.x + AP.y * AB.y) / lengthSqrAB;
        float t = (AP.dot(AB)) / lengthSqAB;
        if (t > 1)
        {
            t = 1;
        }
        if (t < 0)
        {
            t = 0;
        }
        if (t == 0)
        {
            return new PVector[]{B, normalB, texB};

        }
        if (t == 1)
        {
            return new PVector[]{A, normalA, texA};

        }
        else
        {
            assert t < 1 && t > 0;
            PVector temp = SynthMath.lerpVector(B, A, t);
            PVector tempNormal = SynthMath.lerpVector(normalB, normalA, t);
            PVector tempTex = SynthMath.lerpVector(texB, texA, t);
            return new PVector[]{temp, tempNormal, tempTex};
        }
    }

    public Mesh convQuadsToTris()
    {
        ArrayList<Integer> newFaceList = new ArrayList<>();
        ArrayList<Integer> newFaceTexCoords = new ArrayList<>();
        ArrayList<Integer> newFaceNormals = new ArrayList<>();


        for (int startIndex = 0; startIndex < faceVerts.size(); startIndex++)
        {
            //TODO(bryn): Add check that face is not already triangular
            if (startIndex == 0 || faceVerts.get(startIndex - 1) == -1)
            {
                if (faceVerts.get(startIndex + 3) != -1)
                {
                    newFaceList.add(faceVerts.get(startIndex));
                    newFaceList.add(faceVerts.get(startIndex + 1));
                    newFaceList.add(faceVerts.get(startIndex + 2));
                    newFaceList.add(-1);
                    newFaceList.add(faceVerts.get(startIndex + 2));
                    newFaceList.add(faceVerts.get(startIndex + 3));
                    newFaceList.add(faceVerts.get(startIndex));
                    newFaceList.add(-1);

                    newFaceTexCoords.add(faceTexCoords.get(startIndex));
                    newFaceTexCoords.add(faceTexCoords.get(startIndex + 1));
                    newFaceTexCoords.add(faceTexCoords.get(startIndex + 2));
                    newFaceTexCoords.add(-1);
                    newFaceTexCoords.add(faceTexCoords.get(startIndex + 2));
                    newFaceTexCoords.add(faceTexCoords.get(startIndex + 3));
                    newFaceTexCoords.add(faceTexCoords.get(startIndex));
                    newFaceTexCoords.add(-1);

                    newFaceNormals.add(faceNormals.get(startIndex));
                    newFaceNormals.add(faceNormals.get(startIndex + 1));
                    newFaceNormals.add(faceNormals.get(startIndex + 2));
                    newFaceNormals.add(-1);
                    newFaceNormals.add(faceNormals.get(startIndex + 2));
                    newFaceNormals.add(faceNormals.get(startIndex + 3));
                    newFaceNormals.add(faceNormals.get(startIndex));
                    newFaceNormals.add(-1);
                }
                else
                {
                    newFaceList.add(faceVerts.get(startIndex));
                    newFaceList.add(faceVerts.get(startIndex + 1));
                    newFaceList.add(faceVerts.get(startIndex + 2));
                    newFaceList.add(-1);
                    newFaceTexCoords.add(faceTexCoords.get(startIndex));
                    newFaceTexCoords.add(faceTexCoords.get(startIndex + 1));
                    newFaceTexCoords.add(faceTexCoords.get(startIndex + 2));
                    newFaceTexCoords.add(-1);
                    newFaceNormals.add(faceNormals.get(startIndex));
                    newFaceNormals.add(faceNormals.get(startIndex + 1));
                    newFaceNormals.add(faceNormals.get(startIndex + 2));
                    newFaceNormals.add(-1);
                }
            }

        }
        return new Mesh(vertices, texCoords, normals, newFaceList, newFaceTexCoords, newFaceNormals, app);
    }


}
