package Synth;

import processing.core.PApplet;
import processing.core.PVector;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class KDTree
{

    public static int numSearches = 0;
    public int depth;
    public float axisLocation;
    public KDTree leftChild;
    public KDTree rightChild;
    public PApplet app;
    public PVector value;

    public KDTree(PVector[] points, int _depth, PApplet _app)
    {

        app = _app;
        depth = _depth;
        int axis = depth % 3;
        //x == 0, y == 1, z == 2
        float median;
        if (axis == 0)
        {
            Arrays.sort(points, new xComparator());

            if (points.length > 2)
            {
                median = points[(points.length / 2)].x;

            }
            else
            {
                assert points.length == 2;
                median = points[0].x + points[1].x;
                median = median / 2;

            }


        }
        else if (axis == 1)
        {
            Arrays.sort(points, new yComparator());
            if (points.length > 2)
            {

                median = points[(points.length / 2)].y;
            }
            else
            {
                assert points.length == 2;
                median = points[0].y + points[1].y;
                median = median / 2;

            }


        }
        else
        {
            Arrays.sort(points, new zComparator());
            if (points.length > 2)
            {

                median = points[(points.length / 2)].z;
            }
            else
            {
                assert points.length == 2;
                median = points[0].z + points[1].z;
                median = median / 2;

            }
        }
        this.axisLocation = median;
        if (points.length == 2)
        {
            leftChild = new KDTree(points[0], depth + 1, app);
            rightChild = new KDTree(points[1], depth + 1, app);

        }
        else if (points.length == 3)
        {
            leftChild = new KDTree(new PVector[]{points[0], points[1]}, depth + 1, app);
            rightChild = new KDTree(points[2], depth + 1, app);

        }
        else
        {
            PVector[] leftList = Arrays.copyOfRange(points, 0, points.length / 2);
            PVector[] rightList = Arrays.copyOfRange(points, points.length / 2, points.length);
            leftChild = new KDTree(leftList, depth + 1, app);
            rightChild = new KDTree(rightList, depth + 1, app);

        }
    }

    public KDTree(PVector point, int _depth, PApplet _app)
    {
        app = _app;
        value = point;
        depth = _depth;
        if (depth % 3 == 0)
        {
            axisLocation = value.x;
        }
        else if (depth % 3 == 1)
        {
            axisLocation = value.y;
        }
        else if (depth % 3 == 2)
        {
            axisLocation = value.z;
        }
        leftChild = null;
        rightChild = null;

    }

    public PVector nearestNeighbor(PVector point)
    {
        return nearestNeighbourR(point, new PVector(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE));
    }

    private PVector nearestNeighbourR(PVector point, PVector currentBest)
    {
//Move down nodes til find a leaf
        boolean left = false;
        boolean right = false;
        assert point != null;
        if (value != null && !value.equals(point) && currentBest.equals(new PVector(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)))
        {
            currentBest = value;
        }
        else if (value != null && !value.equals(point))
        {
            if (currentBest.dist(point) > value.dist(point))
            {
                currentBest = value;
            }
        }
        else
        {
            if (this.depth % 3 == 0)
            {
                //x
                if (point.x <= axisLocation && leftChild != null)
                {
                    //check left
                    currentBest = leftChild.nearestNeighbourR(point, currentBest);
                    left = true;
                    numSearches++;
                }
                else if (rightChild != null)
                {
                    //check y
                    currentBest = rightChild.nearestNeighbourR(point, currentBest);
                    right = true;
                    numSearches++;

                }
            }
            else if (this.depth % 3 == 1)
            {
                //y
                if (point.y <= axisLocation && leftChild != null)
                {
                    //check left
                    currentBest = leftChild.nearestNeighbourR(point, currentBest);
                    left = true;
                    numSearches++;

                }
                else if (rightChild != null)

                {
                    //check y
                    currentBest = rightChild.nearestNeighbourR(point, currentBest);
                    right = true;
                    numSearches++;

                }

            }
            else
            {
                //z
                if (point.z <= axisLocation && leftChild != null)
                {
                    //check left
                    currentBest = leftChild.nearestNeighbourR(point, currentBest);
                    left = true;
                    numSearches++;

                }
                else if (rightChild != null)

                {
                    //check y
                    currentBest = rightChild.nearestNeighbourR(point, currentBest);
                    right = true;
                    numSearches++;

                }

            }
        }
        if (!right && left)
        {
            //Should we check right?
            if (depth % 3 == 0)
            {
                //x
                if (new PVector(point.x, 0, 0).dist(new PVector(axisLocation, 0, 0)) < currentBest.dist(point))
                {
                    currentBest = rightChild.nearestNeighbourR(point, currentBest);
                    numSearches++;

                }
            }
            else if (depth % 3 == 1)
            {
                //y
                if (new PVector(0, point.y, 0).dist(new PVector(0, axisLocation, 0)) < currentBest.dist(point))
                {
                    currentBest = rightChild.nearestNeighbourR(point, currentBest);
                    numSearches++;

                }

            }
            else
            {
                if (new PVector(0, 0, point.z).dist(new PVector(0, 0, axisLocation)) < currentBest.dist(point))
                {
                    currentBest = rightChild.nearestNeighbourR(point, currentBest);
                    numSearches++;

                }

            }
        }
        else if (right)
        {
            //should we check left?
            if (depth % 3 == 0)
            {
                //x
                if (new PVector(point.x, 0, 0).dist(new PVector(axisLocation, 0, 0)) < currentBest.dist(point))
                {
                    currentBest = leftChild.nearestNeighbourR(point, currentBest);
                    numSearches++;

                }
            }
            else if (depth % 3 == 1)
            {
                //y
                if (new PVector(0, point.y, 0).dist(new PVector(0, axisLocation, 0)) < currentBest.dist(point))
                {
                    currentBest = leftChild.nearestNeighbourR(point, currentBest);
                    numSearches++;

                }

            }
            else
            {
                if (new PVector(0, 0, point.z).dist(new PVector(0, 0, axisLocation)) < currentBest.dist(point))
                {
                    currentBest = leftChild.nearestNeighbourR(point, currentBest);
                    numSearches++;

                }

            }

        }
        return currentBest;


    }

    ArrayList<PVector> nearestNInsertValue(PVector point, ArrayList<PVector> list, PVector value, int num)
    {
        if (list.contains(value))
        {
            return list;
        }
        for (int i = list.size() - 1; i > -1; i--)
        {
            if (point.dist(list.get(i)) < point.dist(value))
            {
                if (i == list.size() - 1 && list.size() < num)
                {
                    list.add(value);
                }

                return list;

            }
            else if (point.dist(list.get(i)) > point.dist(value))
            {
                if (i != list.size() - 1)
                {
                    PVector copy = list.get(i);
                    list.set(i+1, copy);
                }
                list.set(i, value);
            }
        }
        return list;
    }

    public ArrayList<PVector> nearestNeighbours(PVector point, int neighbourCount)
    {
        return nearestNeighbours_r(point, Float.NaN, new ArrayList<PVector>(), neighbourCount);
    }

    private ArrayList<PVector> nearestNeighbours_r(PVector point, float radius, ArrayList<PVector> neighbours, int neighbourCount)
    {
        boolean left = false;
        boolean right = false;
        float EPSILON = 0.00001f;
        if (value != null && !value.equals(point) && Float.isNaN(radius) && neighbours.size() == 0)
        {
            neighbours.add(value);
        }
        else if (value != null && neighbours.size() < neighbourCount && !value.equals(point) && Float.isNaN(radius))
        {
            neighbours = nearestNInsertValue(point, neighbours, value, neighbourCount);
        }
        else if (value != null && !value.equals(point) && neighbours.size() == neighbourCount - 1 && Float.isNaN(radius))
        {
            radius = value.dist(point);
            neighbours = nearestNInsertValue(point, neighbours, value, neighbourCount);
        }
        else if (value != null && !value.equals(point) && neighbours.size() == neighbourCount && !Float.isNaN(radius) && value.dist(point) < radius)
        {
            radius = value.dist(point);
            neighbours = nearestNInsertValue(point, neighbours, value, neighbourCount);
        }
        else
        {
            if (this.depth % 3 == 0)
            {
                //x
                if (point.x <= axisLocation)
                {
                    //check left
                    if (leftChild != null)
                    {
                        neighbours = leftChild.nearestNeighbours_r(point, radius, neighbours, neighbourCount);
                        left = true;
                        numSearches++;
                    }
                }
                else
                {
                    if (rightChild != null)
                    {
                        neighbours = rightChild.nearestNeighbours_r(point, radius, neighbours, neighbourCount);
                        right = true;
                        numSearches++;
                    }
                }
            }
            else if (this.depth % 3 == 1)
            {
                //y
                if (point.y <= axisLocation)
                {
                    //check left
                    if (leftChild != null)
                    {
                        neighbours = leftChild.nearestNeighbours_r(point, radius, neighbours, neighbourCount);
                        left = true;
                        numSearches++;
                    }
                }
                else
                {
                    if (rightChild != null)
                    {
                        neighbours = rightChild.nearestNeighbours_r(point, radius, neighbours, neighbourCount);
                        right = true;
                        numSearches++;
                    }
                }
            }
            else
            {
                //z
                if (point.z <= axisLocation)
                {
                    //check left
                    if (leftChild != null)
                    {
                        neighbours = leftChild.nearestNeighbours_r(point, radius, neighbours, neighbourCount);
                        left = true;
                        numSearches++;
                    }
                }
                else
                {
                    if (rightChild != null)
                    {

                        neighbours = rightChild.nearestNeighbours_r(point, radius, neighbours, neighbourCount);
                        right = true;
                        numSearches++;
                    }
                }
            }
        }

        if (!right && left)
        {
            //Should we check right?
            if (depth % 3 == 0)
            {
                //x
                if (Float.isNaN(radius) || new PVector(point.x, 0, 0).dist(new PVector(axisLocation, 0, 0)) < radius)
                {
                    if (rightChild != null)
                    {
                        neighbours = rightChild.nearestNeighbours_r(point, radius, neighbours, neighbourCount);
                        numSearches++;
                    }
                }
            }
            else if (depth % 3 == 1)
            {
                //y
                if (Float.isNaN(radius) || new PVector(0, point.y, 0).dist(new PVector(0, axisLocation, 0)) < radius)
                {
                    if (rightChild != null)
                    {
                        neighbours = rightChild.nearestNeighbours_r(point, radius, neighbours, neighbourCount);
                        numSearches++;
                    }
                }
            }
            else
            {
                if (Float.isNaN(radius) || new PVector(0, 0, point.z).dist(new PVector(0, 0, axisLocation)) < radius)
                {
                    if (rightChild != null)
                    {
                        neighbours = rightChild.nearestNeighbours_r(point, radius, neighbours, neighbourCount);
                        numSearches++;
                    }
                }
            }
        }
        else if (right)
        {
            //should we check left?
            if (depth % 3 == 0)
            {
                //x
                if (Float.isNaN(radius) || new PVector(point.x, 0, 0).dist(new PVector(axisLocation, 0, 0)) < radius)
                {
                    if (leftChild != null)
                    {
                        neighbours = leftChild.nearestNeighbours_r(point, radius, neighbours, neighbourCount);
                        numSearches++;
                    }
                }
            }
            else if (depth % 3 == 1)
            {
                //y
                if (Float.isNaN(radius) || new PVector(0, point.y, 0).dist(new PVector(0, axisLocation, 0)) < radius)
                {
                    if (leftChild != null)
                    {
                        neighbours = leftChild.nearestNeighbours_r(point, radius, neighbours, neighbourCount);
                        numSearches++;
                    }
                }
            }
            else
            {
                if (Float.isNaN(radius) || new PVector(0, 0, point.z).dist(new PVector(0, 0, axisLocation)) < radius)
                {
                    if (leftChild != null)
                    {
                        neighbours = leftChild.nearestNeighbours_r(point, radius, neighbours, neighbourCount);
                        numSearches++;
                    }
                }
            }
        }
        return neighbours;
    }

    public ArrayList<PVector> radiusNeighbours(PVector point, float radius)
    {

        return radiusNeighboursR(point, radius, new ArrayList<PVector>());

    }

    private ArrayList<PVector> radiusNeighboursR(PVector point, float radius, ArrayList<PVector> neighbours)
    {
        boolean left = false;
        boolean right = false;
        float EPSILON = 0.00001f;
        if (value != null && neighbours.size() == 0 && !value.equals(point) && value.dist(point) < radius)
        {
            neighbours.add(value);
        }
        else if (value != null && !value.equals(point))
        {
            float dist = value.dist(point);
            if (dist < radius && dist > EPSILON)
            {
                neighbours.add(value);
            }
        }
        else
        {
            if (this.depth % 3 == 0)
            {
                //x
                if (point.x <= axisLocation)
                {
                    //check left
                    if (leftChild != null)
                    {
                        neighbours = leftChild.radiusNeighboursR(point, radius, neighbours);

                        left = true;
                        numSearches++;
                    }
                }
                else
                {
                    if (rightChild != null)
                    {
                        neighbours = rightChild.radiusNeighboursR(point, radius, neighbours);
                        right = true;
                        numSearches++;
                    }

                }
            }
            else if (this.depth % 3 == 1)
            {
                //y
                if (point.y <= axisLocation)
                {
                    //check left
                    if (leftChild != null)
                    {
                        neighbours = leftChild.radiusNeighboursR(point, radius, neighbours);
                        left = true;
                        numSearches++;
                    }

                }
                else
                {
                    if (rightChild != null)
                    {
                        neighbours = rightChild.radiusNeighboursR(point, radius, neighbours);
                        right = true;
                        numSearches++;
                    }
                }

            }
            else
            {
                //z
                if (point.z <= axisLocation)
                {
                    //check left
                    if (leftChild != null)
                    {

                        neighbours = leftChild.radiusNeighboursR(point, radius, neighbours);
                        left = true;
                        numSearches++;

                    }
                }
                else
                {
                    if (rightChild != null)
                    {

                        neighbours = rightChild.radiusNeighboursR(point, radius, neighbours);
                        right = true;
                        numSearches++;
                    }
                }

            }
        }

        if (!right && left)
        {
            //Should we check right?
            if (depth % 3 == 0)
            {
                //x
                if (new PVector(point.x, 0, 0).dist(new PVector(axisLocation, 0, 0)) < radius)
                {
                    if (rightChild != null)
                    {
                        neighbours = rightChild.radiusNeighboursR(point, radius, neighbours);
                        numSearches++;
                    }

                }
            }
            else if (depth % 3 == 1)
            {
                //y
                if (new PVector(0, point.y, 0).dist(new PVector(0, axisLocation, 0)) < radius)
                {
                    if (rightChild != null)
                    {
                        neighbours = rightChild.radiusNeighboursR(point, radius, neighbours);
                        numSearches++;
                    }

                }

            }
            else
            {
                if (new PVector(0, 0, point.z).dist(new PVector(0, 0, axisLocation)) < radius)
                {
                    if (rightChild != null)
                    {
                        neighbours = rightChild.radiusNeighboursR(point, radius, neighbours);
                        numSearches++;
                    }

                }

            }
        }
        else if (right)
        {
            //should we check left?
            if (depth % 3 == 0)
            {
                //x
                if (new PVector(point.x, 0, 0).dist(new PVector(axisLocation, 0, 0)) < radius)
                {
                    if (leftChild != null)
                    {
                        neighbours = leftChild.radiusNeighboursR(point, radius, neighbours);
                        numSearches++;
                    }

                }
            }
            else if (depth % 3 == 1)
            {
                //y
                if (new PVector(0, point.y, 0).dist(new PVector(0, axisLocation, 0)) < radius)
                {
                    if (leftChild != null)
                    {
                        neighbours = leftChild.radiusNeighboursR(point, radius, neighbours);
                        numSearches++;
                    }

                }

            }
            else
            {
                if (new PVector(0, 0, point.z).dist(new PVector(0, 0, axisLocation)) < radius)
                {
                    if (leftChild != null)
                    {
                        neighbours = leftChild.radiusNeighboursR(point, radius, neighbours);
                        numSearches++;
                    }
                }

            }

        }
        return neighbours;


    }

    public int countNodes(int currentTotal)
    {
        currentTotal++;
        if (leftChild != null)
        {
            currentTotal = leftChild.countTotalValues(currentTotal);

        }
        if (rightChild != null)
        {
            currentTotal = rightChild.countTotalValues(currentTotal);

        }
        return currentTotal;

    }

    public PVector nearestNotInList(PVector point, ArrayList<PVector> list)
    {
        return nearestNotInListR(point, new PVector(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE), list);
    }

    public PVector nearestNotInListR(PVector point, PVector currentBest, ArrayList<PVector> list)
    {
        //Move down nodes til find a leaf
        boolean left = false;
        boolean right = false;
        assert point != null;
        if (value != null && !value.equals(point) && currentBest.equals(new PVector(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)) && !list.contains(value))
        {
            currentBest = value;
        }
        else if (value != null && !value.equals(point))
        {
            if (currentBest.dist(point) > value.dist(point) && !list.contains(value))
            {
                currentBest = value;
            }
        }
        else
        {
            if (this.depth % 3 == 0)
            {
                //x
                if (point.x <= axisLocation && leftChild != null)
                {
                    //check left
                    currentBest = leftChild.nearestNotInListR(point, currentBest, list);
                    left = true;
                    numSearches++;
                }
                else if (rightChild != null)
                {
                    //check y
                    currentBest = rightChild.nearestNotInListR(point, currentBest, list);
                    right = true;
                    numSearches++;

                }
            }
            else if (this.depth % 3 == 1)
            {
                //y
                if (point.y <= axisLocation && leftChild != null)
                {
                    //check left
                    currentBest = leftChild.nearestNotInListR(point, currentBest, list);
                    left = true;
                    numSearches++;

                }
                else if (rightChild != null)

                {
                    //check y
                    currentBest = rightChild.nearestNotInListR(point, currentBest, list);
                    right = true;
                    numSearches++;

                }

            }
            else
            {
                //z
                if (point.z <= axisLocation && leftChild != null)
                {
                    //check left
                    currentBest = leftChild.nearestNotInListR(point, currentBest, list);
                    left = true;
                    numSearches++;

                }
                else if (rightChild != null)

                {
                    //check y
                    currentBest = rightChild.nearestNotInListR(point, currentBest, list);
                    right = true;
                    numSearches++;

                }

            }
        }
        if (!right && left)
        {
            //Should we check right?
            if (depth % 3 == 0)
            {
                //x
                if (new PVector(point.x, 0, 0).dist(new PVector(axisLocation, 0, 0)) < currentBest.dist(point))
                {
                    currentBest = rightChild.nearestNotInListR(point, currentBest, list);
                    numSearches++;

                }
            }
            else if (depth % 3 == 1)
            {
                //y
                if (new PVector(0, point.y, 0).dist(new PVector(0, axisLocation, 0)) < currentBest.dist(point))
                {
                    currentBest = rightChild.nearestNotInListR(point, currentBest, list);
                    numSearches++;

                }

            }
            else
            {
                if (new PVector(0, 0, point.z).dist(new PVector(0, 0, axisLocation)) < currentBest.dist(point))
                {
                    currentBest = rightChild.nearestNotInListR(point, currentBest, list);
                    numSearches++;

                }

            }
        }
        else if (right)
        {
            //should we check left?
            if (depth % 3 == 0)
            {
                //x
                if (new PVector(point.x, 0, 0).dist(new PVector(axisLocation, 0, 0)) < currentBest.dist(point))
                {
                    currentBest = leftChild.nearestNotInListR(point, currentBest, list);
                    numSearches++;

                }
            }
            else if (depth % 3 == 1)
            {
                //y
                if (new PVector(0, point.y, 0).dist(new PVector(0, axisLocation, 0)) < currentBest.dist(point))
                {
                    currentBest = leftChild.nearestNotInListR(point, currentBest, list);
                    numSearches++;

                }

            }
            else
            {
                if (new PVector(0, 0, point.z).dist(new PVector(0, 0, axisLocation)) < currentBest.dist(point))
                {
                    currentBest = leftChild.nearestNotInListR(point, currentBest, list);
                    numSearches++;

                }

            }

        }
        return currentBest;


    }

    public int countTotalValues(int currentTotal)
    {
        if (value != null)
        {
            currentTotal++;
        }
        if (leftChild != null)
        {
            currentTotal = leftChild.countTotalValues(currentTotal);

        }
        if (rightChild != null)
        {
            currentTotal = rightChild.countTotalValues(currentTotal);

        }
        return currentTotal;
    }

    public class xComparator implements Comparator<PVector>
    {
        //@Override
        public int compare(PVector v1, PVector v2)
        {
            return Float.compare(v1.x, v2.x);
        }
    }

    public class yComparator implements Comparator<PVector>
    {
        //@Override
        public int compare(PVector v1, PVector v2)
        {
            return Float.compare(v1.y, v2.y);
        }
    }

    public class zComparator implements Comparator<PVector>
    {
        //@Override
        public int compare(PVector v1, PVector v2)
        {
            return Float.compare(v1.z, v2.z);
        }
    }

}
