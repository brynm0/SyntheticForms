import processing.core.PApplet;
import processing.core.PVector;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Mesh
{

    public ArrayList<PVector> vertices;
    public ArrayList<PVector> texCoords;
    public ArrayList<PVector> normals;
    public ArrayList<PVector> parameters;
    public ArrayList<Integer> faceVerts;
    public ArrayList<Integer> faceTexCoords;
    public ArrayList<Integer> faceNormals;
    public PApplet app;
    int numFaces = 0;

    public Mesh(PApplet _ap)
    {
        vertices = new ArrayList<>();
        texCoords = new ArrayList<>();
        normals = new ArrayList<>();
        parameters = new ArrayList<>();

        faceVerts = new ArrayList<>();
        faceTexCoords = new ArrayList<>();
        faceNormals = new ArrayList<>();
        app = _ap;
    }

    public Mesh(ArrayList<PVector> _vertices,
                ArrayList<PVector> _texCoords,
                ArrayList<PVector> _normals,
                ArrayList<PVector> _parameters,
                ArrayList<Integer> _faces,
                ArrayList<Integer> _faceTex,
                ArrayList<Integer> _faceNormals,
                PApplet _app)
    {
        vertices = _vertices;
        texCoords = _texCoords;
        normals = _normals;
        parameters = _parameters;
        faceVerts = _faces;
        faceTexCoords = _faceTex;
        faceNormals = _faceNormals;

        app = _app;
    }

    public static ArrayList<Mesh> readMeshes(String fileFolder, String fileName, PApplet app)
    {
        Charset charset = Charset.forName("US-ASCII");

        Path file = Paths.get(URI.create("file:///home/bryn/1.obj"));

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
        System.out.println("Successfully loaded mesh " + fileFolder + " " + fileName);
        System.out.println("OBJ file contains " + outputMeshList.size() + " mesh group(s)");
        for (Mesh m : outputMeshList)
        {
            System.out.println("V: " + m.vertices.size() + " VN: " + m.normals.size()
                    + " VT: " + m.texCoords.size() + " F: " + m.faceVerts.size());

        }

        return outputMeshList;
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
        for (PVector inVec : vertices)
        {
            inVec.sub(scaleOrigin);
            inVec.mult(scaleFactor);
        }
    }

    public PVector meshNoise(PVector meshParam)
    {
        //get 4 neighbours based on meshparam
        //get noise values based on neighbour positions
        ArrayList<PVector> outVectors = new ArrayList<>();
        for (int i = 0; i < faceVerts.size(); i++)
        {
            if (faceVerts.get(i) != -1)
            {
            }
        }

        //determine basis vectors for meshParam plane


        return new PVector();
    }


    public ArrayList<PVector> populate(int count, ArrayList<PVector> outNormals)
    {


        ArrayList<PVector> out = new ArrayList<>();

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
            out.add(f);

            // calculate vectors from point f to vertices p1, p2 and p3:

            PVector f1 = PVector.sub(p1,f);
            PVector f2 = PVector.sub(p2,f);
            PVector f3 = PVector.sub(p3,f);

            // calculate the areas and factors (order of parameters doesn't matter):
            float a = (PVector.sub(p1,p2).cross(PVector.sub(p1,p3))).mag();

            float a1 = PVector.sub(f2,f3).mag() / a;

            float a2 = PVector.sub(f3,f1).mag() / a;

           float a3 = PVector.sub(f1,f2).mag() / a;

            // find the uv corresponding to point f (uv1/uv2/uv3 are associated to p1/p2/p3):
            PVector term1 = PVector.mult(normals.get(normalIndex1), a1);
            PVector term2 = PVector.mult(normals.get(normalIndex2), a2);
            PVector term3 = PVector.mult(normals.get(normalIndex3), a3);

            PVector normalVector = PVector.add(PVector.add(term1, term2), term3);
            outNormals.add(normalVector);

        }
        return out;
    }

    public PVector closestPointOnMesh(PVector point, OctTree vertexTree)
    {
        vertexTree.findClosestN(point, 1);
    }


    public Mesh convQuadsToTris()
    {
        int newNumFaces = 0;

        ArrayList<Integer> newFaceList = new ArrayList<>();
        ArrayList<Integer> newFaceTexCoords = new ArrayList<>();
        ArrayList<Integer> newFaceNormals = new ArrayList<>();


        for (int startIndex = 0; startIndex < faceVerts.size(); startIndex++)
        {
            if (startIndex == 0 || faceVerts.get(startIndex - 1) == -1)
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

        }
        return new Mesh(vertices, texCoords, normals, parameters, newFaceList, newFaceTexCoords, newFaceNormals, app);

    }


}
