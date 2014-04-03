/*
 * Copyright (C) 2011-2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package plugins;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import java.util.ArrayList;
import java.util.List;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.geospatialfiles.shapefile.PointsList;
import whitebox.geospatialfiles.shapefile.Polygon;
import whitebox.geospatialfiles.shapefile.ShapeType;
import whitebox.geospatialfiles.shapefile.ShapefilePoint;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.utilities.BitOps;
import whitebox.utilities.Topology;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class RasterToVectorPolygons implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name
     * containing no spaces.
     *
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "RasterToVectorPolygons";
    }

    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer
     * name (containing spaces) and is used in the interface to list the tool.
     *
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
        return "Raster To Vector Polygons";
    }

    /**
     * Used to retrieve a short description of what the plugin tool does.
     *
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
        return "Converts a raster containing polygons into a vector.";
    }

    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     *
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
        String[] ret = {"RasterVectorConversions"};
        return ret;
    }

    /**
     * Sets the WhiteboxPluginHost to which the plugin tool is tied. This is the
     * class that the plugin will send all feedback messages, progress updates,
     * and return objects.
     *
     * @param host The WhiteboxPluginHost that called the plugin tool.
     */
    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }

    /**
     * Used to communicate feedback pop-up messages between a plugin tool and
     * the main Whitebox user-interface.
     *
     * @param feedback String containing the text to display.
     */
    private void showFeedback(String message) {
        if (myHost != null) {
            myHost.showFeedback(message);
        } else {
            System.out.println(message);
        }
    }

    /**
     * Used to communicate a return object from a plugin tool to the main
     * Whitebox user-interface.
     *
     * @return Object, such as an output WhiteboxRaster.
     */
    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }
    private int previousProgress = 0;
    private String previousProgressLabel = "";

    /**
     * Used to communicate a progress update between a plugin tool and the main
     * Whitebox user interface.
     *
     * @param progressLabel A String to use for the progress label.
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null && ((progress != previousProgress)
                || (!progressLabel.equals(previousProgressLabel)))) {
            myHost.updateProgress(progressLabel, progress);
        } else {
            System.out.println(progressLabel + String.valueOf(progress) + "%");
        }
        previousProgress = progress;
        previousProgressLabel = progressLabel;
    }

    /**
     * Used to communicate a progress update between a plugin tool and the main
     * Whitebox user interface.
     *
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(int progress) {
        if (myHost != null && progress != previousProgress) {
            myHost.updateProgress(progress);
        } else {
            System.out.println(String.valueOf(progress) + "%");
        }
        previousProgress = progress;
    }

    /**
     * Sets the arguments (parameters) used by the plugin.
     *
     * @param args
     */
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    private boolean cancelOp = false;

    /**
     * Used to communicate a cancel operation from the Whitebox GUI.
     *
     * @param cancel Set to true if the plugin should be canceled.
     */
    @Override
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }

    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    private boolean amIActive = false;

    /**
     * Used by the Whitebox GUI to tell if this plugin is still running.
     *
     * @return a boolean describing whether or not the plugin is actively being
     * used.
     */
    @Override
    public boolean isActive() {
        return amIActive;
    }

    @Override
    public void run() {

        amIActive = true;
        String inputFile;
        String outputFile;
        boolean flag;
        int row, col;
        double xCoord, yCoord;
        int progress, oldProgress;
        int i;
        double value, z, zN1, zN2;
        int FID = 0;
        int[] rowVals = new int[2];
        int[] colVals = new int[2];
        int traceDirection = 0;
        int previousTraceDirection = 0;
        double currentHalfRow = 0, currentHalfCol = 0;
        double[] inputValueData = new double[4];
        long numPoints;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        inputFile = args[0];
        outputFile = args[1];
        boolean polygonsContainHoles = true;
        if (args.length > 2) {
            polygonsContainHoles = Boolean.parseBoolean(args[2]);
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputFile == null) || (outputFile == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster input = new WhiteboxRaster(inputFile, "r");
            int rows = input.getNumberRows();
            int cols = input.getNumberColumns();
            double noData = input.getNoDataValue();
            double gridResX = input.getCellSizeX();
            double gridResY = input.getCellSizeY();

            double east = input.getEast() - gridResX / 2.0;
            double west = input.getWest() + gridResX / 2.0;
            double EWRange = east - west;
            double north = input.getNorth() - gridResY / 2.0;
            double south = input.getSouth() + gridResY / 2.0;
            double NSRange = north - south;

            // create a temporary raster image.
            String tempHeader1 = inputFile.replace(".dep", "_temp1.dep");
            WhiteboxRaster temp1 = new WhiteboxRaster(tempHeader1, "rw", inputFile, WhiteboxRaster.DataType.INTEGER, 0);
            temp1.isTemporaryFile = true;

            GeometryFactory factory = new GeometryFactory();
            List<com.vividsolutions.jts.geom.Polygon> polyList = new ArrayList<>();
            List<Double> zVals = new ArrayList<>();

            // set up the output files of the shapefile and the dbf
            DBFField fields[] = new DBFField[2];

            fields[0] = new DBFField();
            fields[0].setName("FID");
            fields[0].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[0].setFieldLength(10);
            fields[0].setDecimalCount(0);

            fields[1] = new DBFField();
            fields[1].setName("VALUE");
            fields[1].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[1].setFieldLength(10);
            fields[1].setDecimalCount(2);

            ShapeFile output = new ShapeFile(outputFile, ShapeType.POLYGON, fields);
//
//            String DBFName = output.getDatabaseFile();
//            DBFWriter writer = new DBFWriter(new File(DBFName)); /*
//             * this DBFWriter object is now in Syc Mode
//             */
//
//            writer.setFields(fields);

            int[] parts = {0};

            oldProgress = -1;
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = input.getValue(row, col);
                    if (z > 0 && z != noData) {
                        zN1 = input.getValue(row - 1, col);
                        zN2 = input.getValue(row, col - 1);

                        if (zN1 != z || zN2 != z) {
                            flag = false;
                            if (zN1 != z) {
                                i = (int) temp1.getValue(row, col);
                                if (!BitOps.checkBit(i, 0)) {
                                    flag = true;
                                }
                            }
                            if (zN2 != z) {
                                i = (int) temp1.getValue(row, col);
                                if (!BitOps.checkBit(i, 3)) {
                                    flag = true;
                                }
                            }
                            if (flag) {

                                currentHalfRow = row - 0.5;
                                currentHalfCol = col - 0.5;

                                traceDirection = -1;

                                numPoints = 0;
                                FID++;
                                PointsList points = new PointsList();

                                //flag = true;
                                do {

                                    // Get the data for the 2 x 2 
                                    // window, i.e. the window in Diagram 1 above.
                                    rowVals[0] = (int) Math.floor(currentHalfRow);
                                    rowVals[1] = (int) Math.ceil(currentHalfRow);
                                    colVals[0] = (int) Math.floor(currentHalfCol);
                                    colVals[1] = (int) Math.ceil(currentHalfCol);

                                    inputValueData[0] = input.getValue(rowVals[0], colVals[0]);
                                    inputValueData[1] = input.getValue(rowVals[0], colVals[1]);
                                    inputValueData[2] = input.getValue(rowVals[1], colVals[0]);
                                    inputValueData[3] = input.getValue(rowVals[1], colVals[1]);

                                    previousTraceDirection = traceDirection;
                                    traceDirection = -1;

                                    // traceDirection 0
                                    if (inputValueData[1] != inputValueData[3]
                                            && inputValueData[1] == z) {
                                        // has the bottom edge of the top-right cell been traversed?
                                        i = (int) temp1.getValue(rowVals[0], colVals[1]);
                                        if (!BitOps.checkBit(i, 2)) {
                                            temp1.setValue(rowVals[0], colVals[1], BitOps.setBit(i, 2));
                                            traceDirection = 0;
                                        }
                                    }

                                    if (inputValueData[1] != inputValueData[3]
                                            && inputValueData[3] == z) {
                                        // has the top edge of the bottom-right cell been traversed?
                                        i = (int) temp1.getValue(rowVals[1], colVals[1]);
                                        if (!BitOps.checkBit(i, 0)) {
                                            temp1.setValue(rowVals[1], colVals[1], BitOps.setBit(i, 0));
                                            traceDirection = 0;
                                        }
                                    }

                                    if (traceDirection == -1) {
                                        // traceDirection 1
                                        if (inputValueData[2] != inputValueData[3]
                                                && inputValueData[2] == z) {
                                            // has the right edge of the bottom-left cell been traversed?
                                            i = (int) temp1.getValue(rowVals[1], colVals[0]);
                                            if (!BitOps.checkBit(i, 1)) {
                                                temp1.setValue(rowVals[1], colVals[0], BitOps.setBit(i, 1));
                                                traceDirection = 1;
                                            }
                                        }

                                        if (inputValueData[2] != inputValueData[3]
                                                && inputValueData[3] == z) {
                                            // has the left edge of the bottom-right cell been traversed?
                                            i = (int) temp1.getValue(rowVals[1], colVals[1]);
                                            if (!BitOps.checkBit(i, 3)) {
                                                temp1.setValue(rowVals[1], colVals[1], BitOps.setBit(i, 3));
                                                traceDirection = 1;
                                            }
                                        }
                                    }

                                    if (traceDirection == -1) {
                                        // traceDirection 2
                                        if (inputValueData[0] != inputValueData[2]
                                                && inputValueData[0] == z) {
                                            // has the bottom edge of the top-left cell been traversed?
                                            i = (int) temp1.getValue(rowVals[0], colVals[0]);
                                            if (!BitOps.checkBit(i, 2)) {
                                                temp1.setValue(rowVals[0], colVals[0], BitOps.setBit(i, 2));
                                                traceDirection = 2;
                                            }
                                        }

                                        if (inputValueData[0] != inputValueData[2]
                                                && inputValueData[2] == z) {
                                            // has the top edge of the bottom-left cell been traversed?
                                            i = (int) temp1.getValue(rowVals[1], colVals[0]);
                                            if (!BitOps.checkBit(i, 0)) {
                                                temp1.setValue(rowVals[1], colVals[0], BitOps.setBit(i, 0));
                                                traceDirection = 2;
                                            }
                                        }
                                    }

                                    if (traceDirection == -1) {
                                        // traceDirection 3
                                        if (inputValueData[0] != inputValueData[1]
                                                && inputValueData[0] == z) {
                                            // has the right edge of the top-left cell been traversed?
                                            i = (int) temp1.getValue(rowVals[0], colVals[0]);
                                            if (!BitOps.checkBit(i, 1)) {
                                                temp1.setValue(rowVals[0], colVals[0], BitOps.setBit(i, 1));
                                                traceDirection = 3;
                                            }
                                        }

                                        if (inputValueData[0] != inputValueData[1]
                                                && inputValueData[1] == z) {
                                            // has the left edge of the top-right cell been traversed?
                                            i = (int) temp1.getValue(rowVals[0], colVals[1]);
                                            if (!BitOps.checkBit(i, 3)) {
                                                temp1.setValue(rowVals[0], colVals[1], BitOps.setBit(i, 3));
                                                traceDirection = 3;
                                            }
                                        }
                                    }

                                    if (previousTraceDirection != traceDirection) {
                                        xCoord = west + (currentHalfCol / cols) * EWRange + gridResX;
                                        yCoord = north - (currentHalfRow / rows) * NSRange - gridResY;
                                        points.addPoint(xCoord, yCoord);
                                    }

                                    switch (traceDirection) {
                                        case 0:
                                            currentHalfCol += 1.0;
                                            break;
                                        case 1:
                                            currentHalfRow += 1.0;
                                            break;
                                        case 2:
                                            currentHalfCol -= 1.0;
                                            break;
                                        case 3:
                                            currentHalfRow -= 1.0;
                                            break;
                                        default:
                                            flag = false;
                                            break;
                                    }
                                    numPoints++;
                                    if (numPoints > 1000000) {
                                        flag = false;
                                    }
                                } while (flag);

                                if (numPoints > 1) {
//                                    // add the line to the shapefile.
//                                    Object[] rowData = new Object[2];
//                                    rowData[0] = (double) FID;
//                                    rowData[1] = z;
//                                    Polygon poly = new Polygon(parts, points.getPointsArray());
//                                    output.addRecord(poly, rowData);
                                    com.vividsolutions.jts.geom.Polygon poly = factory.createPolygon(points.getCoordinateArraySequence());
                                    if (!poly.isValid()) {
                                        // fix the geometry with a buffer(0) as recommended in JTS docs
                                        com.vividsolutions.jts.geom.Geometry jtsGeom2 = poly.buffer(0d);
                                        for (int a = 0; a < jtsGeom2.getNumGeometries(); a++) {
                                            com.vividsolutions.jts.geom.Geometry gN = jtsGeom2.getGeometryN(a);
                                            if (gN instanceof com.vividsolutions.jts.geom.Polygon) {
                                                poly = (com.vividsolutions.jts.geom.Polygon) gN.clone();
                                                polyList.add(poly);
                                                zVals.add(z);
                                            }
                                        }
                                    } else {
                                        polyList.add(poly); //factory.createPolygon(points.getCoordinateArraySequence()));
                                        zVals.add(z);
                                    }
                                }
                            }
                        }
                    }
                }

                progress = (int) (100f * row / (rows - 1));
                if (progress != oldProgress) {
                    updateProgress(progress);
                    oldProgress = progress;
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                }
            }

            // find holes
            int numPoly = polyList.size();
            boolean[] isHole = new boolean[numPoly];
            boolean[] containsHoles = new boolean[numPoly];
            List<Integer>[] myHoles = new List[numPoly];

            if (polygonsContainHoles) {
                oldProgress = -1;
                for (i = 0; i < numPoly; i++) {
                    com.vividsolutions.jts.geom.Polygon item1 = polyList.get(i);
                    double myZ = zVals.get(i);
                    for (int j = i + 1; j < numPoly; j++) {
                        if (zVals.get(j) == myZ) {
                            com.vividsolutions.jts.geom.Polygon item2 = polyList.get(j);
                            if (item1.contains(item2)) {
                                containsHoles[i] = true;
                                isHole[j] = true;
                                if (myHoles[i] == null) {
                                    myHoles[i] = new ArrayList<>();
                                }
                                myHoles[i].add(j);
                            }
                        }
                    }

                    progress = (int) (100f * i / (numPoly - 1));
                    if (progress != oldProgress) {
                        updateProgress("Searching for polygon holes:", progress);
                        oldProgress = progress;
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                    }
                }
            }

            oldProgress = -1;
            FID = 0;
            for (i = 0; i < numPoly; i++) {
                if (!isHole[i] && containsHoles[i]) {
                    FID++;
                    int numHoles = myHoles[i].size();

                    parts = new int[numHoles + 1];

                    Object[] rowData = new Object[2];
                    rowData[0] = (double) FID;
                    rowData[1] = zVals.get(i);

                    com.vividsolutions.jts.geom.Polygon p = polyList.get(i);
                    PointsList points = new PointsList();
                    Coordinate[] coords = p.getExteriorRing().getCoordinates();
                    if (!Topology.isClockwisePolygon(coords)) {
                        for (int j = coords.length - 1; j >= 0; j--) {
                            points.addPoint(coords[j].x, coords[j].y);
                        }
                    } else {
                        for (Coordinate coord : coords) {
                            points.addPoint(coord.x, coord.y);
                        }
                    }

                    for (int k = 0; k < numHoles; k++) {
                        parts[k + 1] = points.size();

                        int holeID = myHoles[i].get(k);
                        p = polyList.get(holeID);
                        coords = p.getExteriorRing().getCoordinates();
                        if (Topology.isClockwisePolygon(coords)) {
                            for (int j = coords.length - 1; j >= 0; j--) {
                                points.addPoint(coords[j].x, coords[j].y);
                            }
                        } else {
                            for (Coordinate coord : coords) {
                                points.addPoint(coord.x, coord.y);
                            }
                        }

                    }

                    Polygon poly = new Polygon(parts, points.getPointsArray());
                    output.addRecord(poly, rowData);
                }
                if (!isHole[i] && !containsHoles[i]) {
                    FID++;
                    parts = new int[]{0};
                    Object[] rowData = new Object[2];
                    rowData[0] = (double) FID;
                    rowData[1] = zVals.get(i);
                    com.vividsolutions.jts.geom.Polygon p = polyList.get(i);
                    PointsList points = new PointsList();
                    Coordinate[] coords = p.getExteriorRing().getCoordinates();
                    if (!Topology.isClockwisePolygon(coords)) {
                        for (int j = coords.length - 1; j >= 0; j--) {
                            points.addPoint(coords[j].x, coords[j].y);
                        }
                    } else {
                        for (int j = 0; j < coords.length; j++) {
                            points.addPoint(coords[j].x, coords[j].y);
                        }
                    }

                    Polygon poly = new Polygon(parts, points.getPointsArray());
                    output.addRecord(poly, rowData);
                }
                progress = (int) (100f * i / (numPoly - 1));
                if (progress != oldProgress) {
                    updateProgress("Outputing data:", progress);
                    oldProgress = progress;
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                }
            }

            input.close();
            output.write();
            temp1.close();

            // returning a header file string displays the image.
            returnData(outputFile);

        } catch (OutOfMemoryError oe) {
            myHost.showFeedback("An out-of-memory error has occurred during operation.");
        } catch (Exception e) {
            myHost.showFeedback("An error has occurred during operation. See log file for details.");
            myHost.logException("Error in " + getDescriptiveName(), e);
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }

    // This method is only used during testing.
    public static void main(String[] args) {
        args = new String[3];
        //args[0] = "/Users/johnlindsay/Documents/Research/Contracts/NRCan 2012/Data/tmp7.dep";
        //args[1] = "/Users/johnlindsay/Documents/Research/Contracts/NRCan 2012/Data/tmp7.shp";
        //args[0] = "/Users/johnlindsay/Documents/Data/Beau's Data/Waterloo deps.dep";
        //args[1] = "/Users/johnlindsay/Documents/Data/Beau's Data/tmp1.shp";
        //args[0] = "/Users/johnlindsay/Documents/Data/Beau's Data/ParisGalt deps.dep";
        //args[1] = "/Users/johnlindsay/Documents/Data/Beau's Data/ParisGalt deps.shp";
        args[0] = "/Users/johnlindsay/Documents/Data/Beau's Data/landuse.dep";
        args[1] = "/Users/johnlindsay/Documents/Data/Beau's Data/tmp1.shp";

        RasterToVectorPolygons rtvp = new RasterToVectorPolygons();
        rtvp.setArgs(args);
        rtvp.run();
    }
}
