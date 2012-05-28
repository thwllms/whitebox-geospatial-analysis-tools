/*
 * Copyright (C) 2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whitebox.geospatialfiles.shapefile;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import whitebox.structures.BoundingBox;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class PolygonZ implements Geometry {
    //private double[] box = new double[4];
    private BoundingBox bb;
    private int numParts;
    private int numPoints;
    private int[] parts;
    private double[][] points;
    private double mMin;
    private double mMax;
    private double[] mArray;
    private double zMin;
    private double zMax;
    private double[] zArray;
    private boolean[] isHole;
    private boolean[] isConvex;
    private double maxExtent;
    
    public PolygonZ(byte[] rawData) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(rawData);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();
            
            bb = new BoundingBox(buf.getDouble(0), buf.getDouble(8), 
                    buf.getDouble(16), buf.getDouble(24));
            maxExtent = bb.getMaxExtent();
            numParts = buf.getInt(32);
            numPoints = buf.getInt(36);
            parts = new int[numParts];
            for (int i = 0; i < numParts; i++) {
                parts[i] = buf.getInt(40 + i * 4);
            }
            int pos = 40 + numParts * 4;
            points = new double[numPoints][2];
            for (int i = 0; i < numPoints; i++) {
                points[i][0] = buf.getDouble(pos + i * 16); // x value
                points[i][1] = buf.getDouble(pos + i * 16 + 8); // y value
            }
            
            pos += numPoints * 16;
            zMin = buf.getDouble(pos);
            zMax = buf.getDouble(pos + 8);
            
            zArray = new double[numPoints];
            pos += 16;
            for (int i = 0; i < numPoints; i++) {
                zArray[i] = buf.getDouble(pos + i * 8); // z value
            }
            
            pos += numPoints * 8;
            mMin = buf.getDouble(pos);
            mMax = buf.getDouble(pos + 8);
            
            mArray = new double[numPoints];
            pos += 16;
            for (int i = 0; i < numPoints; i++) {
                mArray[i] = buf.getDouble(pos + i * 8); // m value
            }
            
            isHole = new boolean[numParts];
            isConvex = new boolean[numParts];
            checkForHoles();
            buf.clear();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    
    // properties
    public BoundingBox getBox() {
        return bb;
    }
    
    public double getXMin() {
        return bb.getMinX();
    }
    
    public double getYMin() {
        return bb.getMinY();
    }
    
    public double getXMax() {
        return bb.getMaxX();
    }
    
    public double getYMax() {
        return bb.getMaxY();
    }
    
    public int getNumPoints() {
        return numPoints;
    }

    public double[][] getPoints() {
        return points;
    }

    public int getNumParts() {
        return numParts;
    }

    public int[] getParts() {
        return parts;
    }

    public double[] getzArray() {
        return zArray;
    }

    public double getzMax() {
        return zMax;
    }

    public double getzMin() {
        return zMin;
    }
    
    public double[] getmArray() {
        return mArray;
    }

    public double getmMax() {
        return mMax;
    }

    public double getmMin() {
        return mMin;
    }
    
    // methods
    private void checkForHoles() {
        // Note: holes are polygons that have verticies in counter-clockwise order
        
        // This approach is based on the method described by Paul Bourke, March 1998
        // http://paulbourke.net/geometry/clockwise/index.html
        
        int stPoint, endPoint, numPointsInPart;
        double x0, y0, x1, y1, x2, y2;
        int n1 = 0, n2 = 0, n3 = 0;
        for (int i = 0; i < numParts; i++) {
            stPoint = parts[i];
            if (i < numParts - 1) {
                // remember, the last point in each part is the same as the first...it's not a legitamate point.
                endPoint = parts[i + 1] - 2;
            } else {
                endPoint = numPoints - 2;
            }
            numPointsInPart = endPoint - stPoint + 1;
            if (numPointsInPart < 3) { return; } // something's wrong! 
            // first see if it is a convex or concave polygon
            // calculate the cros product for each adjacent edge.
            double[] crossproducts = new double[numPointsInPart];
            for (int j = 0; j < numPointsInPart; j++) {
                n2 = stPoint + j;
                if (j == 0) {
                    n1 = stPoint + numPointsInPart - 1;
                    n3 = stPoint + j + 1;
                } else if (j == numPointsInPart - 1) {
                    n1 = stPoint + j - 1;
                    n3 = stPoint ;
                } else {
                    n1 = stPoint + j - 1;
                    n3 = stPoint + j + 1;
                }
                x0 = points[n1][0];
                y0 = points[n1][1];
                x1 = points[n2][0];
                y1 = points[n2][1];
                x2 = points[n3][0];
                y2 = points[n3][1];
                crossproducts[j] = (x1 - x0) * (y2 - y1) - (y1 - y0) * (x2 - x1);
            }
            boolean testSign;
            if (crossproducts[0] >= 0) { 
                testSign = true; // positive
            } else { 
                testSign = false; // negative
            }
            isConvex[i] = true;
            for (int j = 1; j < numPointsInPart; j++) {
                if (crossproducts[j] >= 0 && !testSign) { 
                    isConvex[i] = false;
                    break;
                } else if (crossproducts[j] < 0 && testSign) { 
                    isConvex[i] = false;
                    break; 
                }
            }
            
            // now see if it is clockwise or counter-clockwise
            if (isConvex[i]) {
                if (testSign) { // positive means counter-clockwise
                    isHole[i] = true;
                } else {
                    isHole[i] = false;
                }
            } else {
                // calculate the polygon area. If it is positive is is in clockwise order, else counter-clockwise.
                double area = 0;
                for (int j = 0; j < numPointsInPart; j++) {
                    n1 = stPoint + j;
                    if (j < numPointsInPart - 1) {
                        n2 = stPoint + j + 1;
                    } else {
                        n2 = stPoint;
                    }
                    x1 = points[n1][0];
                    y1 = points[n1][1];
                    x2 = points[n2][0];
                    y2 = points[n2][1];
                
                    area += (x1 * y2) - (x2 * y1);
                } 
                // if this were the true area, we'd half it, but we're only interested in the sign.
                if (area < 0) { // a positive area indicates counter-clockwise order
                    isHole[i] = false;
                } else {
                    isHole[i] = true;
                }
            }
        }
    }
    
    public boolean isPartAHole(int partNum) {
        if (partNum < 0) { return false; }
        if (partNum >= numParts) { return false; }
        return isHole[partNum];
    }
    
    public boolean isPartConvex(int partNum) {
        if (partNum < 0) { return false; }
        if (partNum >= numParts) { return false; }
        return isConvex[partNum];
    }
    
    @Override
    public int getLength() {
        return 32 + 8 + numParts * 4 + numPoints * 16 + 16 + numPoints * 8
                + 16 + numPoints * 8;
    }
    
    @Override
    public ByteBuffer toByteBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(getLength());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.rewind();
        // put the bounding box data in.
        buf.putDouble(bb.getMinX());
        buf.putDouble(bb.getMinY());
        buf.putDouble(bb.getMaxX());
        buf.putDouble(bb.getMaxY());
        // put the numParts and numPoints in.
        buf.putInt(numParts);
        buf.putInt(numPoints);
        // put the part data in.
        for (int i = 0; i < numParts; i++) {
            buf.putInt(parts[i]);
        }
        // put the point data in.
        for (int i = 0; i < numPoints; i++) {
            buf.putDouble(points[i][0]);
            buf.putDouble(points[i][1]);
        }
        // put the min and max z values in
        buf.putDouble(zMin);
        buf.putDouble(zMax);
        // put the z values in
        for (int i = 0; i < numPoints; i++) {
            buf.putDouble(zArray[i]);
        }
        // put the min and max M values in
        buf.putDouble(mMin);
        buf.putDouble(mMax);
        // put the m values in
        for (int i = 0; i < numPoints; i++) {
            buf.putDouble(mArray[i]);
        }
        return buf;
    }

    @Override
    public ShapeType getShapeType() {
        return ShapeType.POLYGONZ;
    }
    
    @Override
    public boolean isMappable(BoundingBox box, double minSize) {
        if (box.doesIntersect(bb) && maxExtent > minSize) {
            return true;
        } else {
            return false;
        }
    }
}
