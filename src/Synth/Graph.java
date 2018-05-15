package Synth;

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

public class Graph
{
    PVector nodePos;
    ArrayList<Graph> connections;
    PApplet app;
    PVector normal;
    int timesVisited;

    public Graph(PVector position, PVector _normal, PApplet _app)
    {
        connections = new ArrayList<>();
        nodePos = position;
        normal = _normal;
        app = _app;
        timesVisited = 0;
    }

    public static ArrayList<Graph> initGraphListFromMeshes(MeshCollection m, PApplet _app)
    {
        ArrayList<Graph> graphList = new ArrayList<>();
        Mesh currMesh = m.meshes.get(0);
        for (int i = 0; i < currMesh.vertices.size(); i++)
        {
            PVector vertex = currMesh.vertices.get(i);
            PVector norm = currMesh.normals.get(i);
            graphList.add(new Graph(vertex, norm, _app));
        }
        for (int i = 0; i < graphList.size(); i++)
        {
            Graph curr = graphList.get(i);
            ArrayList<Graph> currConnections = new ArrayList<>();
            ArrayList<Integer[]> connectionIndices = m.faceListIndexMap.get(curr.nodePos);
            for (Integer[] indices : connectionIndices)
            {
                int vertexIndex = currMesh.faceVerts.get(indices[1] + 1);
                if (vertexIndex != -1)
                {
                    currConnections.add(graphList.get(vertexIndex));
                }
                if (indices[1] != 0)
                {
                    vertexIndex = currMesh.faceVerts.get(indices[1] - 1);
                    if (vertexIndex != -1)
                    {
                        currConnections.add(graphList.get(vertexIndex));
                    }
                }

            }
            curr.connections = currConnections;
        }
        return graphList;

    }

    void drawAllConnections(float strokeWeight, float stroke)
    {
        for (Graph g : connections)
        {
            app.strokeWeight(strokeWeight);
            app.stroke(0,0,0,stroke);
            app.line(nodePos.x, nodePos.y, nodePos.z, g.nodePos.x, g.nodePos.y, g.nodePos.z);
        }
    }

    public static ArrayList<Graph> readGraphListFromFile(String absolutePath, PApplet _app)
    {
        ArrayList<Graph> graphList = new ArrayList<>();
        Charset charset = Charset.forName("US-ASCII");
        String p = "file://" + absolutePath;
        Path file = Paths.get(URI.create(p));

        System.out.println(file);

        try (BufferedReader reader = Files.newBufferedReader(file, charset))
        {
            boolean readingVerts = false;
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.contains("v"))
                {
                    readingVerts = true;
                }
                else if (line.contains("i"))
                {
                    readingVerts = false;
                }
                else if (readingVerts)
                {
                    String[] points = line.split(" ");
                    PVector position = new PVector(Float.parseFloat(points[0]), Float.parseFloat(points[1]), Float.parseFloat(points[2]));
                    PVector norm = new PVector(Float.parseFloat(points[3]), Float.parseFloat(points[4]), Float.parseFloat(points[5]));
                    Graph temp = new Graph(position, norm, _app);
                    graphList.add(temp);
                }
                else
                {
                    String[] indices = line.split(" ");
                    Graph curr = graphList.get(Integer.parseInt(indices[0]));
                    curr.connections.add(graphList.get(Integer.parseInt(indices[1])));
                }

            }
        }
        catch (IOException e)
        {
            System.err.format("IOException: %s%n", e);
        }
        return graphList;
    }

    public static void scaleGraphList(ArrayList<Graph> list, float factor, PVector origin)
    {
        for (Graph g : list)
        {
            g.nodePos.sub(origin);
            g.nodePos.mult(factor);
            g.nodePos.add(origin);
        }
    }
}
