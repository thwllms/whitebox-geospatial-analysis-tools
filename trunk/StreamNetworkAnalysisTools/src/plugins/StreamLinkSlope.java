/*
 * Copyright (C) 2011 Dr. John Lindsay <jlindsay@uoguelph.ca>
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

import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class StreamLinkSlope implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;
    
    // Constants
    private static final double LnOf2 = 0.693147180559945;

    @Override
    public String getName() {
        return "StreamLinkSlope";
    }

    @Override
    public String getDescriptiveName() {
    	return "Stream Link Slope";
    }

    @Override
    public String getToolDescription() {
    	return "Estimates the slope of each link in a stream network.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "StreamAnalysis" };
    	return ret;
    }

    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }

    private void showFeedback(String message) {
        if (myHost != null) {
            myHost.showFeedback(message);
        } else {
            System.out.println(message);
        }
    }

    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }

    private int previousProgress = 0;
    private String previousProgressLabel = "";
    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null && ((progress != previousProgress) || 
                (!progressLabel.equals(previousProgressLabel)))) {
            myHost.updateProgress(progressLabel, progress);
        }
        previousProgress = progress;
        previousProgressLabel = progressLabel;
    }

    private void updateProgress(int progress) {
        if (myHost != null && progress != previousProgress) {
            myHost.updateProgress(progress);
        }
        previousProgress = progress;
    }
    
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    
    private boolean cancelOp = false;
    @Override
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }
    
    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    
    private boolean amIActive = false;
    @Override
    public boolean isActive() {
        return amIActive;
    }

    @Override
    public void run() {
        amIActive = true;
        
        String streamsLinkIDHeader = null;
        String pointerHeader = null;
        String DEMHeader = null;
        String outputHeader = null;
        int row, col, x, y;
        float progress = 0;
        int i, c;
        double z;
        int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
        double[] inflowingVals = new double[]{16, 32, 64, 128, 1, 2, 4, 8};
        boolean flag = false;
        double flowDir = 0;
                
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                streamsLinkIDHeader = args[i];
            } else if (i == 1) {
                pointerHeader = args[i];
            } else if (i == 2) {
                DEMHeader = args[i];
            } else if (i == 3) {
                outputHeader = args[i];
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((streamsLinkIDHeader == null) || (pointerHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster streamID = new WhiteboxRaster(streamsLinkIDHeader, "r");
            int rows = streamID.getNumberRows();
            int cols = streamID.getNumberColumns();
            double noData = streamID.getNoDataValue();
            double gridResX = streamID.getCellSizeX();
            double gridResY = streamID.getCellSizeY();
            double diagGridRes = Math.sqrt(gridResX * gridResX + gridResY * gridResY);
            double[] gridLengths = new double[]{diagGridRes, gridResX, diagGridRes, gridResY, diagGridRes, gridResX, diagGridRes, gridResY};
            
            double maxLinkID = streamID.getMaximumValue();
            double[] linkLengths = new double[(int)maxLinkID + 1];
            
            WhiteboxRaster pntr = new WhiteboxRaster(pointerHeader, "r");
            if (pntr.getNumberRows() != rows || pntr.getNumberColumns() != cols) {
                showFeedback("The input images must be of the same dimensions.");
                return;
            }
            
            WhiteboxRaster DEM = new WhiteboxRaster(DEMHeader, "r");
            if (DEM.getNumberRows() != rows || DEM.getNumberColumns() != cols) {
                showFeedback("The input images must be of the same dimensions.");
                return;
            }
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", 
                    streamsLinkIDHeader, WhiteboxRaster.DataType.FLOAT, 0);
            output.setPreferredPalette("spectrum.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);
            
            double[][] linkRelief = new double[3][(int)maxLinkID + 1]; //first entry holds 
            //the minimum elevation, the second holds the maximimum, and the third holds the slope
            //initialize the LinkRelief array so that the min and max values can be found
            
            for (c = 0; c <= maxLinkID; c++) {
                linkRelief[0][c] = 99999999;
                linkRelief[1][c] = -99999999;
            }
                    
            byte numNeighbouringStreamCells = 0;
            double currentID = 0;
            double linkLength = 0;
            
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    if (streamID.getValue(row, col) > 0) {
                        // see if it is a headwater location
                        numNeighbouringStreamCells = 0;
                        for (c = 0; c < 8; c++) {
                            x = col + dX[c];
                            y = row + dY[c];
                            if (streamID.getValue(y, x) > 0 && 
                                    pntr.getValue(y, x) == inflowingVals[c]) { 
                                numNeighbouringStreamCells++; 
                            }
                        }
                        if (numNeighbouringStreamCells == 0) {
                            //it's a headwater location so start a downstream flowpath
                            x = col;
                            y = row;
                            linkLength = 0;
                            currentID = streamID.getValue(y, x);
                            flag = true;
                            do {
                                z = DEM.getValue(y, x);
                                if (z < linkRelief[0][(int)currentID]) { linkRelief[0][(int)currentID] = z; }
                                if (z > linkRelief[1][(int)currentID]) { linkRelief[1][(int)currentID] = z; }
                                
                                //find the downslope neighbour
                                flowDir = pntr.getValue(y, x);
                                if (flowDir > 0) {
                                    c = (int)(Math.log(flowDir) / LnOf2);
                                    if (c > 7) {
                                        showFeedback("An unexpected value has "
                                                + "been identified in the pointer "
                                                + "image. This tool requires a "
                                                + "pointer grid that has been "
                                                + "created using either the D8 "
                                                + "or Rho8 tools.");
                                        return;
                                    }
                                    linkLength += gridLengths[c];
                                    
                                    x += dX[c];
                                    y += dY[c];
                                    
                                    if (streamID.getValue(y, x) != currentID) {
                                        linkLengths[(int)(currentID)] = linkLength;
                                        currentID = streamID.getValue(y, x);
                                        if (currentID != noData) {
                                            linkLength = 0;
                                            if (linkLengths[(int) currentID] > 0) {
                                                flag = false;
                                            }
                                        } else {
                                            flag = false;
                                        }
                                    }

                                } else {
                                    linkLengths[(int)currentID] = linkLength;
                                    flag = false;
                                }
                            } while (flag);
                        }
                    } else {
                        output.setValue(row, col, noData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress((int) progress);
            }
            
            for (i = 1; i <= maxLinkID; i++) {
                linkRelief[2][i] = (linkRelief[1][i] - linkRelief[0][i]) / linkLengths[i] * 100;
            }
            
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    if (streamID.getValue(row, col) > 0) {
                        currentID = streamID.getValue(row, col);
                        output.setValue(row, col, linkRelief[2][(int)currentID]);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress((int) progress);
            }
            
            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            
            pntr.close();
            streamID.close();
            output.close();

            // returning a header file string displays the image.
            returnData(outputHeader);

            String retData = "Stream Link Slope Output\n";
            for (i = 1; i <= maxLinkID; i++) {
                retData = retData + i + "\t" + linkRelief[2][i] + "\n";
            }
            
            returnData(retData);
            
        } catch (Exception e) {
            showFeedback(e.getMessage());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }
}