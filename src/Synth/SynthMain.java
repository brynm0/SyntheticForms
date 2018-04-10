package Synth;

import peasy.PeasyCam;
import processing.core.PApplet;
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
import java.util.Collections;

public class SynthMain extends PApplet
{
    CurveCollection curveCollection;
    ArrayList<ArrayList<PVector>> dividedCurves;
    PeasyCam cam;
    ArrayList<ArrayList<PVector>> shades = new ArrayList<>();
    float scaleFactor = 0.1f;

    float cellWidthHeight = 1500 * scaleFactor;
    float zOffset = 300 * scaleFactor;
    float baseOffset = 150 * scaleFactor;
    float offsetScalar = 450 * scaleFactor;

    public static void main(String[] args)
    {
        PApplet.main("Synth.SynthMain", args);
    }

    public static ArrayList<PVector> readCrv(String absolutePath)
    {
        Charset charset = Charset.forName("US-ASCII");
        String p = "file://" + absolutePath;
        Path file = Paths.get(URI.create(p));
        System.out.println(file);
        ArrayList<PVector> outList = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, charset))
        {

            String line;
            while ((line = reader.readLine()) != null)
            {
                String[] vector = line.split(" ");
                PVector tempVec = new PVector(Float.parseFloat(vector[0]), Float.parseFloat(vector[1]), Float.parseFloat(vector[2]));
                outList.add(tempVec);
            }
        }
        catch (IOException e)
        {
            System.out.println("IOException: " + e);
        }
        return outList;
    }

    public void settings()
    {
        size(1000, 800, P3D);
    }

    public void setup()
    {
        cam = new PeasyCam(this, 500);
        CurveCollection curveCollection = new CurveCollection("/home/bryn/Downloads/shades.crv", this);
        curveCollection.scale(scaleFactor, new PVector());
        dividedCurves = new ArrayList<>();
        for (int i = 0; i < curveCollection.curves.size(); i++)
        {
            dividedCurves.add(CurveCollection.divideCurveByLength(curveCollection.curves.get(i),
                    cellWidthHeight));
        }
        int n = 0;
        n++;
        Collections.reverse(dividedCurves);
        for (int j = 0; j < dividedCurves.size() - 1; j++)
        {
            for (int i = 0; i < dividedCurves.get(j).size() - 1; i++)
            {
                if (i >= dividedCurves.get(j + 1).size())
                {
                    break;
                }
                ArrayList<PVector> tmp = new ArrayList<>();
                //vertex(divisions.get(i).x, divisions.get(i).y, divisions.get(i).z);
                tmp.add(dividedCurves.get(j).get(i));
                //vertex(divisions2.get(i).x, divisions2.get(i).y, divisions2.get(i).z);
                tmp.add(dividedCurves.get(j + 1).get(i));
                PVector point = dividedCurves.get(j).get(i).copy();
                PVector movement = PVector.sub(tmp.get(1), tmp.get(0)).cross(new PVector(0, 0, -1));
                movement.normalize();
                float length = PVector.sub(tmp.get(1), tmp.get(0)).mag();
                if (length > cellWidthHeight)
                {
                    length = cellWidthHeight;
                }
                float actualBaseOffset = (baseOffset / cellWidthHeight) * length;
                float actualOffsetScalar = (offsetScalar / cellWidthHeight) * length;


                float northVerticalOffset = movement.dot(new PVector(0, 1, 0)) * actualOffsetScalar;
                if (northVerticalOffset > 0)
                {
                    northVerticalOffset = 0;
                }
                northVerticalOffset = Math.abs(northVerticalOffset);
                float verticalOffset = actualBaseOffset + northVerticalOffset;
                float horizontalOffset = baseOffset + (Math.abs(movement.dot(new PVector(1, 0, 0)))) * offsetScalar;
                movement.setMag(zOffset);
                movement.add(new PVector(0, 0, -1).mult(verticalOffset));
                movement.add(PVector.sub(tmp.get(1), tmp.get(0)).setMag(horizontalOffset));
                point.add(movement);
                tmp.add(point);
                //vertex(divisions.get(i+1).x, divisions.get(i+1).y, divisions.get(i+1).z);
                shades.add(tmp);
                tmp = new ArrayList<>();
                tmp.add(dividedCurves.get(j).get(i));
                tmp.add(dividedCurves.get(j).get(i + 1));
                tmp.add(point);
                shades.add(tmp);

            }

        }
        saveShades();

    }

    public void draw()
    {
        background(255);
        stroke(0);
        //line(test.get(0).x, test.get(0).y, test.get(0).z, test.get(1).x, test.get(1).y, test.get(1).z);
        //line(test2.get(0).x, test2.get(0).y, test2.get(0).z, test2.get(1).x, test2.get(1).y, test2.get(1).z);
        for (ArrayList<PVector> list : shades)
        {
            beginShape();
            stroke(0);
            fill(255);
            for (PVector point : list)
            {
                vertex(point.x, point.y, point.z);
            }
            endShape(CLOSE);
        }

    }

    public void saveShades()
    {
        long fileID = System.currentTimeMillis();
        PrintWriter out;
        try
        {
            System.out.println("writing positions");
            out = new PrintWriter(fileID + "position" + ".txt");

            for (ArrayList<PVector> triangle : shades)
            {
                for (PVector point : triangle)
                {
                    out.println(point.x + ", " + point.y + ", " + point.z);
                }
            }
            out.close();
            System.out.println("done");

        }
        catch (IOException e)
        {
            System.out.println("Unhandled IO Exception " + e);
        }
    }


}

