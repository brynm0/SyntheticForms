import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.Stack;

import peasy.PeasyCam;

public class SynthMain extends PApplet {
    public static void main(String[] args) {
        PApplet.main("SynthMain", args);
    }

    public void settings() {
        size(1000, 800, P3D);
    }

    Stack<PVector> p;
    ArrayList<PVector> pointsArray;
    OctTree tree;
    ArrayList<ArrayList<PVector>> neighbours;

    PeasyCam camera;

    public void setup() {
        camera = new PeasyCam(this, 500);
        p = new Stack<>();
        pointsArray = new ArrayList<>();
        randomSeed(1);
        while (p.size() < 100) {
            PVector temp = new PVector(random(-300, 300), random(-300, 300), random(-300, 300));
            p.push(temp);
            pointsArray.add(temp);
        }
        tree = new OctTree(this, new BBox(new PVector(-500, -500, -500), new PVector(500, 500, 500)), p);
        int n;
    }

    public void draw() {
        tree.UpdateTree();
        neighbours = new ArrayList<>();
        for (int j = 0; j < pointsArray.size(); j++) {
            int searchDistance = 300;
            ArrayList<PVector> findings = tree.Search(searchDistance, pointsArray.get(j), 0);
            neighbours.add(findings);
        }

        background(255);
        translate(width / 2, height / 2, 0);
        for (PVector element : pointsArray) {
            pushMatrix();
            translate(element.x, element.y, element.z);
            stroke(0);
            ellipse(0,0,10,10);
            popMatrix();
        }

        for (int i = 0; i < pointsArray.size(); i++) {
            for (PVector element : neighbours.get(i)) {
                stroke(0);
                strokeWeight(1);
                line(pointsArray.get(i).x, pointsArray.get(i).y, pointsArray.get(i).z, element.x, element.y, element.z);

            }
        }
    }



}
