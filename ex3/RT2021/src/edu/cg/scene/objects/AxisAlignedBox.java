package edu.cg.scene.objects;

import edu.cg.algebra.*;


// TODO Implement this class which represents an axis aligned box
public class AxisAlignedBox extends Shape{
    private final static int NDIM=3; // Number of dimensions
    private Point a = null;
    private Point b = null;
    private double[] aAsArray;
    private double[] bAsArray;

    public AxisAlignedBox(Point a, Point b){
        this.a = a;
        this.b = b;
        // We store the points as Arrays - this could be helpful for more elegant implementation.
        aAsArray = a.asArray();
        bAsArray = b.asArray();
        assert (a.x <= b.x && a.y<=b.y && a.z<=b.z);

    }

    @Override
    public String toString() {
        String endl = System.lineSeparator();
        return "AxisAlignedBox:" + endl +
                "a: " + a + endl +
                "b: " + b + endl;
    }

    public AxisAlignedBox initA(Point a){
        this.a = a;
        aAsArray = a.asArray();
        return this;
    }

    public AxisAlignedBox initB(Point b){
        this.b = b;
        bAsArray = b.asArray();
        return this;
    }

    @Override
    public Hit intersect(Ray ray) {
        Vec normal;
        boolean insideBox;
        boolean[] toNegNormal = new boolean[3];

        Point sourcePoint = ray.source();
        Vec rayDirection = ray.direction();
        double[] sourcePointCoordinate = new double[3];
        double[] rayDirectionCoordinate = new double[3];
        // fill coordinate for sourcePoint and rayDirection
        for (int i = 0; i < 3; i++) {
            sourcePointCoordinate[i] = sourcePoint.getCoordinate(i);
            rayDirectionCoordinate[i] = rayDirection.getCoordinate(i);
        }

        double[] minElementsIntersections = new double[3];
        double[] maxElementsIntersections = new double[3];
        // fill the intersection point where intervals:
        // Ix = [minElementsIntersections[0],maxElementsIntersections[0]]
        // Iy = [minElementsIntersections[1],maxElementsIntersections[1]]
        // Iz = [minElementsIntersections[2],maxElementsIntersections[2]]
        for (int i = 0; i < 3; ++i) {
            if (isLineDirectionIsZeroInOneDirection(rayDirectionCoordinate[i])) {
                if (!isInsideBoxRange(sourcePointCoordinate[i],i)) {
                    return null;
                } else {
                    minElementsIntersections[i] = Double.NEGATIVE_INFINITY;
                    maxElementsIntersections[i] = Double.POSITIVE_INFINITY;
                }
            }
            double t0 = (this.aAsArray[i] - sourcePointCoordinate[i]) / rayDirectionCoordinate[i];
            double t1 = (this.bAsArray[i] - sourcePointCoordinate[i]) / rayDirectionCoordinate[i];

            minElementsIntersections[i] = Math.min(t0, t1);
            maxElementsIntersections[i] = Math.max(t0, t1);
            toNegNormal[i] = checkIfNeedToNegateNormal(t0,t1);
        }

        int indexOfMaximumInMinElements = this.getIndexOfMaxElementInArray(minElementsIntersections);
        double maxElementInMinElements = minElementsIntersections[indexOfMaximumInMinElements];
        int indexOfTheMinimumInMaxElements = this.getIndexOfMinElementInArray(maxElementsIntersections);
        double minElementInMaxElements = maxElementsIntersections[indexOfTheMinimumInMaxElements];

        if (maxElementInMinElements > minElementInMaxElements || minElementInMaxElements <= Ops.epsilon) {
            return null;
        }

        // find the correct normal
        if (maxElementInMinElements > Ops.epsilon) {
            insideBox = false;
            normal = this.getNormal(indexOfMaximumInMinElements).neg();
            if (toNegNormal[indexOfMaximumInMinElements]) {
                normal = normal.neg();
            }
        } else {
            maxElementInMinElements = minElementInMaxElements;
            insideBox = true;
            normal = this.getNormal(indexOfTheMinimumInMaxElements);
            if (toNegNormal[indexOfTheMinimumInMaxElements]) {
                normal = normal.neg();
            }
        }
        Hit hit = new Hit(maxElementInMinElements, normal).setIsWithin(insideBox);
        return hit;
    }

    private boolean isLineDirectionIsZeroInOneDirection(double rayDirectionCoordinate) {
        if (Math.abs(rayDirectionCoordinate) <= Ops.epsilon){
            return true;
        }
        return false;
    }

    private boolean isInsideBoxRange(double pointCoordinate, int dimension) {
        if ((pointCoordinate >= this.aAsArray[dimension]) && (pointCoordinate <= this.bAsArray[dimension])) {
            return true;
        }
        return false;
    }

    private boolean checkIfNeedToNegateNormal(double t0, double t1) {
        if (t0 <= Ops.epsilon) {
            return true;
        }
        if(t1 < t0 && t1 > Ops.epsilon) {
            return true;
        }
        return false;
    }

    private int getIndexOfMinElementInArray(double[] arr) {
        double minElement = Double.POSITIVE_INFINITY;
        int minIndex = 0;
        for (int i = 0; i < 3; ++i) {
            if (arr[i] < minElement){
                minIndex = i;
                minElement = arr[i];
            }
        }
        return minIndex;
    }

    private int getIndexOfMaxElementInArray(double[] arr) {
        double maxElement = Double.NEGATIVE_INFINITY;
        int maxIndex = 0;
        for (int i = 0; i < 3; ++i) {
            if (arr[i] > maxElement){
                maxIndex = i;
                maxElement = arr[i];
            }
        }
        return maxIndex;
    }

    private Vec getNormal(int coordinateIndex) {
        Vec normalizeNormal = null;
        if (coordinateIndex == 0) {
            normalizeNormal = new Vec(1,0,0);
        } else if (coordinateIndex == 1) {
            normalizeNormal = new Vec(0,1,0);
        } else if (coordinateIndex == 2) {
            normalizeNormal = new Vec(0,0,1);
        }
        return normalizeNormal;
    }
}

