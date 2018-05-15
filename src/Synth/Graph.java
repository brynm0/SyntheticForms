package Synth;

import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;

public class Graph
{
    PVector nodePos;
    ArrayList<Graph> connections;
    PApplet app;

    public Graph(PVector position, PApplet _app)
    {
        connections = new ArrayList<>();
        nodePos = position;
        app = _app;
    }

    public static ArrayList<Graph> initGraphListFromMeshes(MeshCollection m, PApplet _app)
    {
        ArrayList<Graph> graphList = new ArrayList<>();
        Mesh currMesh = m.meshes.get(0);
        for (PVector vertex : m.meshes.get(0).vertices)
        {
            graphList.add(new Graph(vertex, _app));
        }
        for (int i = 0; i < graphList.size(); i++)
        {
            Graph curr = graphList.get(i);
            ArrayList<Graph> currConnections = new ArrayList<>();
            ArrayList<Integer[]> connectionIndices = m.faceListIndexMap.get(curr.nodePos);
            for (Integer[] indices : connectionIndices                 )
            {
                int vertexIndex = currMesh.faceVerts.get(indices[1] + 1);
                if (vertexIndex != -1)
                {
                    currConnections.add( graphList.get(vertexIndex));
                }
                if (indices[1] != 0)
                {
                    vertexIndex = currMesh.faceVerts.get(indices[1] - 1);
                    if (vertexIndex != -1)
                    {
                        currConnections.add( graphList.get(vertexIndex));
                    }
                }

            }
            curr.connections = currConnections;
        }
        return graphList;

    }
}
